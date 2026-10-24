"""API REST de práctica con SQLite. Python 3.10+; no requiere paquetes externos."""
import argparse
import json
import re
import sqlite3
from contextlib import closing
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlsplit


def crear_servidor(host, port, database):
    database = Path(database)
    database.parent.mkdir(parents=True, exist_ok=True)
    with closing(sqlite3.connect(database)) as connection:
        for table in ('alumno', 'profesor'):
            connection.execute(f'''CREATE TABLE IF NOT EXISTS {table} (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL, apellido TEXT NOT NULL,
                edad INTEGER NOT NULL CHECK(edad BETWEEN 1 AND 120))''')
        connection.commit()

    class Handler(BaseHTTPRequestHandler):
        protocol_version = 'HTTP/1.1'

        def responder(self, status, data=None):
            raw = b'' if data is None else json.dumps(data, ensure_ascii=False).encode('utf-8')
            self.send_response(status)
            if raw:
                self.send_header('Content-Type', 'application/json; charset=utf-8')
            self.send_header('Content-Length', str(len(raw)))
            # Cierre explícito: evita reutilizar sockets cerrados en Android 7/OkHttp.
            self.send_header('Connection', 'close')
            self.close_connection = True
            self.end_headers()
            if raw:
                self.wfile.write(raw)

        def procesar(self):
            if self.command == 'GET' and urlsplit(self.path).path == '/health':
                return self.responder(200, {'status': 'ok'})
            route = re.fullmatch(r'/api/escuela/(alumno|profesor)(?:/([1-9][0-9]*))?/?', urlsplit(self.path).path)
            if not route:
                return self.responder(404, {'message': 'Recurso no encontrado'})
            table, identifier = route.groups()
            payload = None
            if self.command in ('POST', 'PUT'):
                try:
                    length = int(self.headers.get('Content-Length', '0'))
                    if not 0 < length <= 65536:
                        raise ValueError('Tamaño de solicitud inválido')
                    payload = json.loads(self.rfile.read(length))
                    if not isinstance(payload, dict):
                        raise ValueError('Se requiere un objeto JSON')
                    nombre = payload.get('nombre', '')
                    apellido = payload.get('apellido', '')
                    edad = payload.get('edad')
                    if not isinstance(nombre, str) or not 1 <= len(nombre.strip()) <= 100:
                        raise ValueError('Nombre obligatorio, máximo 100 caracteres')
                    if not isinstance(apellido, str) or not 1 <= len(apellido.strip()) <= 100:
                        raise ValueError('Apellido obligatorio, máximo 100 caracteres')
                    if type(edad) is not int or not 1 <= edad <= 120:
                        raise ValueError('Edad entera entre 1 y 120')
                    payload = (nombre.strip(), apellido.strip(), edad)
                except (ValueError, TypeError, UnicodeDecodeError) as error:
                    return self.responder(400, {'message': str(error)})
            with closing(sqlite3.connect(database)) as connection:
                connection.row_factory = sqlite3.Row
                def serializar(row):
                    result = dict(row)
                    result['id'] = str(result['id'])
                    return result
                if self.command == 'GET':
                    if identifier:
                        row = connection.execute(f'SELECT * FROM {table} WHERE id=?', (identifier,)).fetchone()
                        return self.responder(200, serializar(row)) if row else self.responder(404, {'message': 'Registro no encontrado'})
                    rows = connection.execute(f'SELECT * FROM {table} ORDER BY id').fetchall()
                    return self.responder(200, [serializar(row) for row in rows])
                if self.command == 'POST' and identifier is None:
                    cursor = connection.execute(f'INSERT INTO {table} (nombre,apellido,edad) VALUES (?,?,?)', payload)
                    connection.commit()
                    row = connection.execute(f'SELECT * FROM {table} WHERE id=?', (cursor.lastrowid,)).fetchone()
                    return self.responder(201, serializar(row))
                if self.command in ('PUT', 'DELETE') and identifier:
                    row = connection.execute(f'SELECT * FROM {table} WHERE id=?', (identifier,)).fetchone()
                    if row is None:
                        return self.responder(404, {'message': 'Registro no encontrado'})
                    if self.command == 'PUT':
                        connection.execute(f'UPDATE {table} SET nombre=?,apellido=?,edad=? WHERE id=?', (*payload, identifier))
                        connection.commit()
                        updated = connection.execute(f'SELECT * FROM {table} WHERE id=?', (identifier,)).fetchone()
                        return self.responder(200, serializar(updated))
                    connection.execute(f'DELETE FROM {table} WHERE id=?', (identifier,))
                    connection.commit()
                    return self.responder(204)
                return self.responder(405, {'message': 'Método no permitido para esta ruta'})

        do_GET = procesar
        do_POST = procesar
        do_PUT = procesar
        do_DELETE = procesar

    return ThreadingHTTPServer((host, port), Handler)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--host', default='127.0.0.1')
    parser.add_argument('--port', type=int, default=8765)
    parser.add_argument('--db', default=str(Path(__file__).with_name('escuela.sqlite3')))
    args = parser.parse_args()
    server = crear_servidor(args.host, args.port, args.db)
    print(f'API lista: http://{args.host}:{server.server_port}/api/escuela/alumno', flush=True)
    print('En Android Emulator: http://10.0.2.2:8765/api/ | Ctrl+C para cerrar', flush=True)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()

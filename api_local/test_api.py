import json
import tempfile
import threading
import unittest
from pathlib import Path
from urllib.error import HTTPError
from urllib.request import Request, urlopen
from servidor import crear_servidor


class CrudTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.server = crear_servidor('127.0.0.1', 0, Path(self.temp.name) / 'test.sqlite3')
        self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)
        self.thread.start()
        self.base = f'http://127.0.0.1:{self.server.server_port}/api/escuela/'

    def tearDown(self):
        self.server.shutdown()
        self.server.server_close()
        self.thread.join()
        self.temp.cleanup()

    def request(self, method, path, payload=None):
        raw = json.dumps(payload).encode() if payload is not None else None
        request = Request(self.base + path, data=raw, method=method, headers={'Content-Type': 'application/json'})
        try:
            with urlopen(request, timeout=5) as response:
                body = response.read()
                return response.status, json.loads(body) if body else None
        except HTTPError as error:
            with error:
                return error.code, json.loads(error.read())

    def cycle(self, resource):
        self.assertEqual(self.request('GET', resource), (200, []))
        status, created = self.request('POST', resource, {'nombre': 'Ana', 'apellido': 'Pérez', 'edad': 24})
        self.assertEqual(status, 201)
        self.assertEqual(created['nombre'], 'Ana')
        path = resource + '/' + created['id']
        self.assertEqual(self.request('GET', path), (200, created))
        status, updated = self.request('PUT', path, {'nombre': 'Ana María', 'apellido': 'Pérez', 'edad': 25})
        self.assertEqual((status, updated['id'], updated['edad']), (200, created['id'], 25))
        self.assertEqual(self.request('GET', resource)[1], [updated])
        self.assertEqual(self.request('DELETE', path), (204, None))
        self.assertEqual(self.request('GET', path)[0], 404)
        self.assertEqual(self.request('DELETE', path)[0], 404)
        self.assertEqual(self.request('GET', resource), (200, []))

    def test_alumnos_crud(self):
        self.cycle('alumno')

    def test_profesores_crud(self):
        self.cycle('profesor')

    def test_invalid_payload_and_routes(self):
        for edad in ('edad 1', 0, 121, True):
            self.assertEqual(self.request('POST', 'alumno', {'nombre': 'Ana', 'apellido': 'Pérez', 'edad': edad})[0], 400)
        self.assertEqual(self.request('POST', 'profesor', {'nombre': '', 'apellido': 'Pérez', 'edad': 25})[0], 400)
        self.assertEqual(self.request('GET', 'desconocido')[0], 404)


if __name__ == '__main__':
    unittest.main(verbosity=2)

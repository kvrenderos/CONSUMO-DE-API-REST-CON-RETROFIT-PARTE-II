# RetrofitCrudApp - Guía 11, Retrofit parte II

Actualización 1.1 (23/10/2026): se corrigió la confianza HTTPS de las API proporcionadas en Android antiguo. Consultar `CORRECCION_CONEXION_API.md` para la causa, instalación y verificación. Si quedó seleccionada la API local, usar **Configurar API → Usar URLs proporcionadas → Guardar configuración**.

Continuación del proyecto corregido de la guía 10. Incluye gestión de alumnos y profesores con GET, POST, PUT y DELETE, y conserva las búsquedas de perros y repositorios de GitHub en el menú.



## Usar la aplicación

En **Alumnos** o **Profesores** se puede consultar el listado, agregar un registro y tocarlo para modificarlo o eliminarlo. La eliminación requiere confirmación. Al regresar del formulario se recarga el listado desde la API.

Los nombres y apellidos son obligatorios (máximo 100 caracteres) y la edad debe ser un entero entre 1 y 120. Se usa el ID asignado por el servidor; el cuerpo de POST y PUT contiene solamente `nombre`, `apellido` y `edad`.

Las URL iniciales son las proporcionadas por el usuario:

Alumnos: `https://66240d4504457d4aaf9b8530.mockapi.io/api/escuela/alumno`

Profesores: `https://6ad813a395e98f36aef2ea16.mockapi.io/profesor`

Cada campo contiene la URL completa de la colección, sin ID. Retrofit usa `@Url` para admitir servidores y rutas independientes. La pantalla **Configurar API** permite cambiar ambas direcciones sin recompilar; no se agrega automáticamente ningún prefijo. La disponibilidad y los datos de las API públicas dependen de sus propietarios; las pruebas de escritura de esta entrega se hicieron en la API local incluida.

## API local incluida: alumnos y profesores

Requiere Python 3.10 o superior y no necesita instalar bibliotecas. Tiene dos tablas SQLite con el esquema `id`, `nombre`, `apellido`, `edad`.

1. Ejecutar `api_local/iniciar_api.bat`, o usar:

```powershell
python api_local/servidor.py
```

2. Mantener esa terminal abierta.
3. En un emulador Android Studio, abrir **Configurar API**, pulsar **Usar API local del emulador** y **Guardar configuración**. Se guardarán `http://10.0.2.2:8765/api/escuela/alumno` y `http://10.0.2.2:8765/api/escuela/profesor`.
4. Abrir Alumnos o Profesores y realizar el CRUD.

En un teléfono conectado por USB, ejecutar `adb reverse tcp:8765 tcp:8765` desde platform-tools y configurar las dos colecciones con `localhost` en lugar de `10.0.2.2`. Si `adb` no está en PATH, usar su ruta completa dentro del SDK.

La base se crea vacía en `api_local/escuela.sqlite3` y conserva los datos entre ejecuciones. El servidor escucha únicamente en `127.0.0.1`; es una API de práctica local. Las URL externas de la app requieren HTTPS. HTTP se admite únicamente para `localhost`, `127.0.0.1` y `10.0.2.2`.

| Método | Ruta | Resultado |
| --- | --- | --- |
| GET | `/api/escuela/alumno` o `/api/escuela/profesor` | Lista JSON |
| GET | Ruta anterior + `/{id}` | Registro o HTTP 404 |
| POST | Ruta de la colección | Registro creado, HTTP 201 |
| PUT | Ruta de un registro | Registro actualizado, HTTP 200 |
| DELETE | Ruta de un registro | HTTP 204 sin contenido |

Las solicitudes POST/PUT usan JSON, por ejemplo: `{"nombre":"Ana","apellido":"Pérez","edad":24}`. Los ID se generan en la API y se devuelven como texto, igual que en MockAPI.

## Usar un proyecto propio en MockAPI

Crear los recursos `alumno` y `profesor`, cada uno con `nombre` y `apellido` de tipo String y `edad` de tipo Number. Conservar el campo `id` automático de MockAPI. En la app guardar la URL completa de cada colección. Pueden pertenecer a proyectos distintos, como las direcciones proporcionadas por el usuario.

No es necesario crear una cuenta para usar el servidor local. Esta entrega no crea ni administra un proyecto de MockAPI del usuario.

## Estructura

- `EscuelaApi`: métodos HTTP con `@Url` para las dos colecciones; `Entidad` selecciona la URL que corresponde a cada pantalla.
- `Persona`: modelo común de respuesta. `Alumno` y `Profesor` son alias del mismo esquema.
- `PersonaDatos`: cuerpo validado para crear y actualizar.
- `PersonasActivity`: listado y eliminación; `EditarPersonaActivity`: alta y edición con consulta por ID.
- `EscuelaClient`: Retrofit, conversor Gson, tiempos de espera y logging BASIC en depuración.
- `ConfiguracionActivity`: dos URL persistidas en preferencias.
- `api_local/servidor.py`: implementación HTTP con tablas SQLite separadas para alumnos y profesores.

Se comparten el formulario y el adaptador porque las dos entidades tienen los mismos campos. Esto implementa las pantallas de la discusión de resultados sin duplicar la lógica. Se incluye un avatar vectorial propio, ya que la imagen del aula virtual no fue proporcionada.

## Pruebas

```powershell
python api_local/test_api.py
.\gradlew.bat testDebugUnitTest lintDebug
# Con servidor local activo y emulador Android Studio conectado:
.\gradlew.bat connectedDebugAndroidTest
```

Las pruebas instrumentadas `CrudFlowTest` usan exclusivamente `http://10.0.2.2:8765/api/`; crean registros con nombres únicos y eliminan solamente esos registros al terminar. Incluyen creación, consulta, edición, cancelación de eliminación, eliminación confirmada y validación de campos. Los casos de la guía 10 se mantienen.

## Ajustes frente al ejemplo de la guía

- Eliminar utiliza `@DELETE`; el ejemplo de la página 6 repite `@PUT` por error.
- No se envía un ID ficticio `0` al crear.
- La edad se valida sin `toInt()` inseguro. Si un servidor entrega edades no numéricas, se muestran como no válidas y se solicitan corregidas al editar.
- Se comparte un cliente y se usan View Binding y recursos de texto.
- Al guardar se cierra el formulario con `finish()`; no se acumulan copias de la pantalla de listado.
- Se contemplan listas vacías, errores HTTP, errores de conexión y cancelación de llamadas al destruir la actividad. Los botones se deshabilitan mientras se guarda.
- Los formularios se desplazan y se ajustan al teclado sin descontar dos veces su altura en Android 7; el teclado se mantiene oculto al abrir un formulario hasta seleccionar un campo.
- No se reintentan automáticamente las solicitudes de escritura. Si se pierde la respuesta se indica consultar la lista antes de repetir la operación.
- Se mantiene la exclusión específica de Lint para el código de notificaciones no utilizado de Picasso, procedente de la guía 10.

No se implementan autenticación, sincronización sin conexión ni resolución de ediciones simultáneas. Si se cierra o recrea la app durante una escritura, consultar de nuevo el listado para confirmar su estado antes de repetirla.

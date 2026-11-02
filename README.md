# Backend Clínica PetSalud – Autenticación JWT y roles

API REST en Spring Boot 3.3 protegida con **Spring Security + JWT (jjwt 0.12.6)**.
Autenticación sin estado (stateless) y autorización por roles **CLIENTE**, **EMPLEADO** y **GERENTE**.

## Requisitos
- Java 17 o superior
- MySQL 8 en ejecución
- Maven (o el wrapper `./mvnw` incluido)

## Variables de entorno
| Variable | Obligatoria | Descripción |
|---|---|---|
| `JWT_SECRET` | Sí | Clave en Base64 de al menos 32 bytes. Generar con `openssl rand -base64 32` |
| `DB_USERNAME` / `DB_PASSWORD` | Sí | Credenciales de MySQL (por defecto `root` / vacío) |
| `DB_URL` | No | Por defecto `jdbc:mysql://localhost:3306/petsalud_db` (se crea si no existe) |
| `JWT_EXPIRATION_MS` | No | Vigencia del token, por defecto 3600000 (1 hora) |
| `GERENTE_EMAIL` / `GERENTE_PASSWORD` | No | Si se definen, al iniciar se crea la primera cuenta GERENTE |
| `CORS_ORIGINS` | No | Orígenes del frontend permitidos, por defecto `http://localhost:3000` |

En IntelliJ: *Run > Edit Configurations > Environment variables*.

```bash
export JWT_SECRET=$(openssl rand -base64 32)
export DB_USERNAME=root DB_PASSWORD=tu_clave
export GERENTE_EMAIL=gerente@petsalud.cl GERENTE_PASSWORD=ClaveGerente123
./mvnw spring-boot:run
```

## Endpoints de autenticación (públicos)
| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/auth/registro` | Crea una cuenta con rol CLIENTE (contraseña con BCrypt, mínimo 8 caracteres) |
| POST | `/api/auth/login` | Devuelve `{ token, tipo: "Bearer", expiraEnMs, email, rol }` |

Todas las demás rutas `/api/**` exigen el encabezado `Authorization: Bearer <token>`.

## Matriz de roles
| Recurso | GERENTE | EMPLEADO | CLIENTE |
|---|---|---|---|
| Usuarios | Listar, eliminar, cambiar rol | Sin acceso | Ver y editar su perfil |
| Mascotas / Citas | Completo | Toda la clínica | Solo las suyas |
| Expedientes médicos | Completo | Crear, ver, eliminar | Sin acceso |
| Productos | Completo | Consulta y stock bajo | Consulta |
| Ventas | Completo | Consultar y registrar | Solo las suyas |
| Notificaciones / Recompensas | Completo | Sin acceso | Solo las suyas |

Para dar el rol EMPLEADO o GERENTE: `PUT /api/usuarios/{id}/rol` con `{"rol":"EMPLEADO"}` (solo GERENTE).

## Pruebas rápidas en Postman
1. `POST /api/auth/registro` con `{"email":"ana@petsalud.cl","password":"Clave1234","nombre":"Ana"}` → 201
2. `POST /api/auth/login` con `{"email":"ana@petsalud.cl","password":"Clave1234"}` → 200 y token
3. `GET /api/productos` sin token → **401**
4. `GET /api/productos` con `Authorization: Bearer <token>` → **200**
5. `POST /api/productos` con el token de CLIENTE → **403**
6. Iniciar sesión como GERENTE y repetir el paso 5 → **200**

También se incluye la colección `PetSalud-JWT.postman_collection.json`: el login guarda el token automáticamente.

## Pruebas automatizadas
```bash
./mvnw test
```
- `JwtUtilTest`: claims, token alterado, otra clave, token expirado, `alg: none` y clave débil.
- `AutorizacionPorRolTest`: 401 sin token, 403 por rol y 200 con el rol correcto.
- `ResourceAccessGuardTest` y `UsuarioServiceImplTest`: titularidad de recursos y BCrypt.

## Migración desde la versión anterior
Los roles cambiaron de nombre. Si la base ya tiene usuarios:
```sql
UPDATE usuarios SET rol = 'EMPLEADO' WHERE rol = 'VETERINARIO';
UPDATE usuarios SET rol = 'GERENTE'  WHERE rol = 'ADMIN';
```
Las cuentas con contraseña en texto claro deben restablecerse, porque el login solo acepta hashes BCrypt.

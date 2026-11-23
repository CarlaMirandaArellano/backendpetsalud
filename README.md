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
| Proveedores | Completo | Consulta | Sin acceso |
| Ventas | Completo | Consultar y registrar | Consultar solo las suyas; sin registrar |
| Notificaciones / Recompensas | Completo | Sin acceso | Solo las suyas |

Para dar el rol EMPLEADO o GERENTE: `PUT /api/usuarios/{id}/rol` con `{"rol":"EMPLEADO"}` (solo GERENTE).

## Pruebas rápidas en Postman
1. `POST /api/auth/registro` con `{"email":"ana@petsalud.cl","password":"Clave1234","nombre":"Ana","apellido":"Pérez","direccion":"Calle 123"}` → 201
2. `POST /api/auth/login` con `{"email":"ana@petsalud.cl","password":"Clave1234"}` → 200 y token
3. `GET /api/productos` sin token → **401**
4. `GET /api/productos` con `Authorization: Bearer <token>` → **200**
5. `POST /api/productos` con el token de CLIENTE → **403**
6. Iniciar sesión como GERENTE y repetir el paso 5 → **200**

También se incluye la colección `PetSalud-JWT.postman_collection.json`: el login guarda el token automáticamente.

## Pruebas automatizadas
```bash
./mvnw clean verify
# En Windows: .\mvnw.cmd clean verify
```
- `JwtUtilTest`: claims, token alterado, otra clave, token expirado, `alg: none` y clave débil.
- `AutorizacionPorRolTest`: acceso a productos y generación de ventas por rol.
- `UsuarioServiceImplTest` y `VentaServiceImplTest`: validaciones, cálculos, stock y relaciones entre usuario, venta y productos.
- `ProveedorServiceImplTest`: datos obligatorios del proveedor, RUT duplicado, relación Producto–Proveedor y regla que impide eliminar un proveedor con productos.
- `CitaServiceImplTest` (semana 5): cita agendada con estado inicial "Agendada", rechazo de horario ocupado, veterinario correcto (rol EMPLEADO) y mascota con propietario registrado.
- `MascotaServiceImplTest`, `UsuarioTest` y `CitaTest` (semana 5): datos obligatorios del usuario, roles válidos, relación CLIENTE–mascotas y relaciones de la cita.
- JUnit 5 y Mockito vienen incluidos con `spring-boot-starter-test`. JaCoCo genera el reporte en `target/site/jacoco/` y exige al menos 80% de cobertura de líneas en `UsuarioServiceImpl`, `VentaServiceImpl`, `ProveedorServiceImpl`, `CitaServiceImpl` y `MascotaServiceImpl`.

## Migración desde la versión anterior
Los roles cambiaron de nombre. Si la base ya tiene usuarios:
```sql
UPDATE usuarios SET rol = 'EMPLEADO' WHERE rol = 'VETERINARIO';
UPDATE usuarios SET rol = 'GERENTE'  WHERE rol = 'ADMIN';
```
Las cuentas con contraseña en texto claro deben restablecerse, porque el login solo acepta hashes BCrypt.

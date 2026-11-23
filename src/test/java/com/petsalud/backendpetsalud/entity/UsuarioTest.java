package com.petsalud.backendpetsalud.entity;

import jakarta.persistence.Column;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Semana 5 - Pruebas de la entidad Usuario: datos obligatorios, roles y relacion con mascotas.
 * Roles del sistema: CLIENTE, EMPLEADO (veterinarios y atencion) y GERENTE (administracion).
 */
@DisplayName("Entidad Usuario")
class UsuarioTest {

    private Usuario cliente;

    @BeforeAll
    static void inicioSuite() {
        System.out.println(">> Inicio de pruebas de la entidad Usuario");
    }

    @AfterAll
    static void finSuite() {
        System.out.println(">> Fin de pruebas de la entidad Usuario");
    }

    @BeforeEach
    void setUp() {
        cliente = Usuario.builder()
                .id(1L)
                .email("ana.rojas@petsalud.cl")
                .password("hash")
                .nombre("Ana")
                .apellido("Rojas")
                .direccion("Calle 123")
                .rol(Rol.CLIENTE)
                .mascotas(new ArrayList<>())
                .build();
    }

    @AfterEach
    void tearDown() {
        cliente = null;
    }

    // ---------- Datos del usuario ----------

    @Test
    @DisplayName("Un usuario valido tiene email, nombre y rol no nulos ni vacios")
    void usuarioValidoTieneCamposObligatorios() {
        // Assert
        assertAll("campos obligatorios",
                () -> assertNotNull(cliente.getEmail()),
                () -> assertFalse(cliente.getEmail().isBlank()),
                () -> assertNotNull(cliente.getNombre()),
                () -> assertFalse(cliente.getNombre().isBlank()),
                () -> assertNotNull(cliente.getRol()),
                () -> assertTrue(cliente.tieneDatosObligatoriosCompletos()));
    }

    @ParameterizedTest(name = "email = \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Un email nulo o vacio deja al usuario con datos incompletos")
    void emailNuloOVacioEsInvalido(String email) {
        // Arrange
        cliente.setEmail(email);

        // Act + Assert
        assertFalse(cliente.tieneDatosObligatoriosCompletos());
    }

    @ParameterizedTest(name = "nombre = \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("Un nombre nulo o vacio deja al usuario con datos incompletos")
    void nombreNuloOVacioEsInvalido(String nombre) {
        // Arrange
        cliente.setNombre(nombre);

        // Act + Assert
        assertFalse(cliente.tieneDatosObligatoriosCompletos());
    }

    @Test
    @DisplayName("email y rol estan declarados como NOT NULL en la base de datos")
    void emailYRolSonNotNullEnLaBaseDeDatos() throws NoSuchFieldException {
        // Arrange
        Column email = Usuario.class.getDeclaredField("email").getAnnotation(Column.class);
        Column rol = Usuario.class.getDeclaredField("rol").getAnnotation(Column.class);

        // Assert
        assertFalse(email.nullable(), "email debe ser NOT NULL");
        assertTrue(email.unique(), "email debe ser UNIQUE");
        assertFalse(rol.nullable(), "rol debe ser NOT NULL");
    }

    // ---------- Roles ----------

    @ParameterizedTest(name = "Rol valido: {0}")
    @EnumSource(Rol.class)
    @DisplayName("El usuario acepta los roles del sistema")
    void aceptaRolesDelSistema(Rol rol) {
        // Act
        cliente.setRol(rol);

        // Assert
        assertEquals(rol, cliente.getRol());
        assertEquals("ROLE_" + rol.name(), rol.authority());
    }

    @Test
    @DisplayName("El sistema tiene exactamente los roles CLIENTE, EMPLEADO y GERENTE")
    void existenSoloTresRoles() {
        assertEquals(List.of(Rol.CLIENTE, Rol.EMPLEADO, Rol.GERENTE), List.of(Rol.values()));
    }

    @ParameterizedTest(name = "Rol invalido: \"{0}\"")
    @ValueSource(strings = {"VETERINARIO", "ADMIN", "SUPERADMIN", "cliente", ""})
    @DisplayName("Un rol fuera de la lista es rechazado")
    void rechazaRolInvalido(String rolTexto) {
        assertThrows(IllegalArgumentException.class, () -> Rol.valueOf(rolTexto));
    }

    // ---------- Relacion usuario - mascota ----------

    @Test
    @DisplayName("Un usuario CLIENTE puede tener una lista de mascotas asociadas")
    void clienteTieneListaDeMascotas() {
        // Arrange
        Mascota perro = Mascota.builder().nombre("Firulais").especie("Perro").propietario(cliente).build();
        Mascota gato = Mascota.builder().nombre("Michi").especie("Gato").propietario(cliente).build();

        // Act
        cliente.getMascotas().add(perro);
        cliente.getMascotas().add(gato);

        // Assert
        assertEquals(Rol.CLIENTE, cliente.getRol());
        assertEquals(2, cliente.getMascotas().size());
        assertTrue(cliente.getMascotas().stream().allMatch(m -> m.getPropietario() == cliente),
                "Cada mascota apunta a su propietario");
    }

    @Test
    @DisplayName("Las mascotas del cliente tienen nombre y especie validos")
    void mascotasTienenNombreYEspecieValidos() {
        // Arrange
        cliente.getMascotas().add(Mascota.builder().nombre("Firulais").especie("Perro").build());
        cliente.getMascotas().add(Mascota.builder().nombre("Michi").especie("Gato").build());

        // Assert
        for (Mascota m : cliente.getMascotas()) {
            assertAll("mascota " + m.getNombre(),
                    () -> assertNotNull(m.getNombre()),
                    () -> assertFalse(m.getNombre().isBlank()),
                    () -> assertNotNull(m.getEspecie()),
                    () -> assertFalse(m.getEspecie().isBlank()));
        }
    }
}

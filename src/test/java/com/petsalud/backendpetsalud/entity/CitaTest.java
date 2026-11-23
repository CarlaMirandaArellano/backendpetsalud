package com.petsalud.backendpetsalud.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Semana 5 - Pruebas de la entidad Cita y sus relaciones con Mascota y Usuario.
 */
@DisplayName("Entidad Cita")
class CitaTest {

    private Usuario propietario;
    private Usuario veterinario;
    private Mascota mascota;

    @BeforeEach
    void setUp() {
        propietario = Usuario.builder().id(1L).email("ana@petsalud.cl")
                .nombre("Ana").rol(Rol.CLIENTE).build();
        veterinario = Usuario.builder().id(2L).email("dr.soto@petsalud.cl")
                .nombre("Pedro").rol(Rol.EMPLEADO).build();
        mascota = Mascota.builder().id(10L).nombre("Firulais").especie("Perro")
                .propietario(propietario).build();
    }

    @Test
    @DisplayName("Se construye una cita con sus datos y relaciones")
    void construyeCitaConRelaciones() {
        // Arrange
        LocalDateTime fecha = LocalDateTime.of(2026, 12, 1, 10, 30);

        // Act
        Cita cita = Cita.builder()
                .id(100L).fechaHora(fecha).estado("Agendada").motivo("Control")
                .mascota(mascota).veterinario(veterinario)
                .build();

        // Assert
        assertEquals(fecha, cita.getFechaHora());
        assertEquals("Agendada", cita.getEstado());
        assertEquals("Control", cita.getMotivo());
        assertSame(mascota, cita.getMascota());
        assertSame(veterinario, cita.getVeterinario());
    }

    @Test
    @DisplayName("El veterinario de la cita es EMPLEADO y distinto del propietario")
    void veterinarioEsEmpleadoYDistintoDelPropietario() {
        // Act
        Cita cita = Cita.builder().mascota(mascota).veterinario(veterinario).build();

        // Assert
        assertEquals(Rol.EMPLEADO, cita.getVeterinario().getRol());
        assertNotEquals(cita.getMascota().getPropietario(), cita.getVeterinario());
    }

    @Test
    @DisplayName("La mascota de la cita tiene un propietario CLIENTE")
    void mascotaDeLaCitaTienePropietarioCliente() {
        // Act
        Cita cita = Cita.builder().mascota(mascota).veterinario(veterinario).build();

        // Assert
        assertNotNull(cita.getMascota().getPropietario());
        assertEquals(Rol.CLIENTE, cita.getMascota().getPropietario().getRol());
    }
}

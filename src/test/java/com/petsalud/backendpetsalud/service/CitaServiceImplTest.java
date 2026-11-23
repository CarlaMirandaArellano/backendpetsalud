package com.petsalud.backendpetsalud.service;

import com.petsalud.backendpetsalud.entity.Cita;
import com.petsalud.backendpetsalud.entity.Mascota;
import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.repository.CitaRepository;
import com.petsalud.backendpetsalud.repository.MascotaRepository;
import com.petsalud.backendpetsalud.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Semana 5 - Pruebas de la logica de negocio de Cita (CitaServiceImpl).
 * Los repositorios se simulan con Mockito; no se usa base de datos.
 * En este sistema el veterinario es un usuario con rol EMPLEADO (ver README).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CitaServiceImpl")
class CitaServiceImplTest {

    @Mock
    private CitaRepository citaRepository;

    @Mock
    private MascotaRepository mascotaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private CitaServiceImpl citaService;

    private Usuario propietario;
    private Usuario veterinario;
    private Mascota mascota;
    private LocalDateTime fechaHora;

    @BeforeEach
    void setUp() {
        propietario = Usuario.builder()
                .id(1L).email("ana.rojas@petsalud.cl").password("hash")
                .nombre("Ana").apellido("Rojas").direccion("Calle 123")
                .rol(Rol.CLIENTE)
                .build();

        veterinario = Usuario.builder()
                .id(2L).email("dr.soto@petsalud.cl").password("hash")
                .nombre("Pedro").apellido("Soto").direccion("Clinica Central")
                .rol(Rol.EMPLEADO)
                .build();

        mascota = Mascota.builder()
                .id(10L).nombre("Firulais").especie("Perro").raza("Quiltro")
                .propietario(propietario)
                .build();

        fechaHora = LocalDateTime.of(2026, 12, 1, 10, 30);
    }

    private void mascotaYVeterinarioExisten() {
        when(mascotaRepository.findById(10L)).thenReturn(Optional.of(mascota));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(veterinario));
    }

    /** Stub de la agenda del veterinario (lenient: no todas las pruebas llegan a consultarla). */
    private void agendaDelVeterinario(List<Cita> citas) {
        lenient().when(citaRepository.findByVeterinarioId(2L)).thenReturn(citas);
    }

    @Nested
    @DisplayName("Creacion de citas")
    class CreacionDeCitas {

        @Test
        @DisplayName("Agenda una cita valida y su estado inicial es 'Agendada'")
        void agendaCitaValidaConEstadoInicialAgendada() {
            // Arrange
            mascotaYVeterinarioExisten();
            agendaDelVeterinario(List.of());
            when(citaRepository.save(any(Cita.class))).thenAnswer(invocation -> {
                Cita c = invocation.getArgument(0);
                c.setId(100L);
                return c;
            });

            // Act
            Cita cita = citaService.agendarCita(10L, 2L, fechaHora, "Control anual");

            // Assert
            assertNotNull(cita);
            assertEquals(100L, cita.getId());
            assertEquals("Agendada", cita.getEstado());
            assertEquals(fechaHora, cita.getFechaHora());
            assertEquals("Control anual", cita.getMotivo());
            verify(citaRepository).save(any(Cita.class));
        }

        @Test
        @DisplayName("No permite agendar en una fecha y hora ya ocupada por el veterinario")
        void rechazaCitaEnHorarioOcupado() {
            // Arrange
            mascotaYVeterinarioExisten();
            Cita citaExistente = Cita.builder()
                    .id(50L).fechaHora(fechaHora).estado("Agendada")
                    .veterinario(veterinario)
                    .build();
            agendaDelVeterinario(List.of(citaExistente));

            // Act
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> citaService.agendarCita(10L, 2L, fechaHora, "Vacuna"));

            // Assert
            assertTrue(ex.getMessage().contains("horario"));
            verify(citaRepository, never()).save(any(Cita.class));
        }

        @Test
        @DisplayName("Permite usar un horario cuya cita anterior fue cancelada")
        void permiteHorarioDeCitaCancelada() {
            // Arrange
            mascotaYVeterinarioExisten();
            Cita citaCancelada = Cita.builder()
                    .id(51L).fechaHora(fechaHora).estado("Cancelada")
                    .veterinario(veterinario)
                    .build();
            agendaDelVeterinario(List.of(citaCancelada));
            when(citaRepository.save(any(Cita.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Cita cita = citaService.agendarCita(10L, 2L, fechaHora, "Vacuna");

            // Assert
            assertEquals("Agendada", cita.getEstado());
            verify(citaRepository).save(any(Cita.class));
        }

        @Test
        @DisplayName("Rechaza la cita si la mascota no existe")
        void rechazaCitaConMascotaInexistente() {
            // Arrange
            when(mascotaRepository.findById(99L)).thenReturn(Optional.empty());

            // Act
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> citaService.agendarCita(99L, 2L, fechaHora, "Control"));

            // Assert
            assertEquals("Mascota no encontrada", ex.getMessage());
            verify(citaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Rechaza la cita si el veterinario no existe")
        void rechazaCitaConVeterinarioInexistente() {
            // Arrange
            when(mascotaRepository.findById(10L)).thenReturn(Optional.of(mascota));
            when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

            // Act
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> citaService.agendarCita(10L, 99L, fechaHora, "Control"));

            // Assert
            assertEquals("Veterinario no encontrado", ex.getMessage());
            verify(citaRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Relacion mascota - veterinario")
    class RelacionMascotaVeterinario {

        @Test
        @DisplayName("La cita queda asignada al veterinario correcto")
        void citaQuedaAsignadaAlVeterinarioCorrecto() {
            // Arrange
            mascotaYVeterinarioExisten();
            agendaDelVeterinario(List.of());
            when(citaRepository.save(any(Cita.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            citaService.agendarCita(10L, 2L, fechaHora, "Control");

            // Assert
            ArgumentCaptor<Cita> captor = ArgumentCaptor.forClass(Cita.class);
            verify(citaRepository).save(captor.capture());
            Cita guardada = captor.getValue();
            assertSame(veterinario, guardada.getVeterinario());
            assertEquals(2L, guardada.getVeterinario().getId());
            assertEquals(Rol.EMPLEADO, guardada.getVeterinario().getRol());
            assertSame(mascota, guardada.getMascota());
        }

        @Test
        @DisplayName("No permite asignar como veterinario a un usuario CLIENTE")
        void rechazaVeterinarioQueNoEsEmpleado() {
            // Arrange
            Usuario otroCliente = Usuario.builder().id(3L).email("otro@petsalud.cl")
                    .nombre("Luis").rol(Rol.CLIENTE).build();
            when(mascotaRepository.findById(10L)).thenReturn(Optional.of(mascota));
            when(usuarioRepository.findById(3L)).thenReturn(Optional.of(otroCliente));

            // Act
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> citaService.agendarCita(10L, 3L, fechaHora, "Control"));

            // Assert
            assertTrue(ex.getMessage().contains("EMPLEADO"));
            verify(citaRepository, never()).save(any());
        }

        @Test
        @DisplayName("La mascota de la cita tiene un propietario CLIENTE registrado en el sistema")
        void mascotaDeLaCitaTienePropietarioRegistrado() {
            // Arrange
            mascotaYVeterinarioExisten();
            agendaDelVeterinario(List.of());
            when(citaRepository.save(any(Cita.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(usuarioRepository.existsById(1L)).thenReturn(true);

            // Act
            Cita cita = citaService.agendarCita(10L, 2L, fechaHora, "Control");

            // Assert
            Usuario duenio = cita.getMascota().getPropietario();
            assertNotNull(duenio, "La mascota debe tener propietario");
            assertEquals(Rol.CLIENTE, duenio.getRol());
            assertTrue(usuarioRepository.existsById(duenio.getId()),
                    "El propietario debe estar registrado en el sistema");
        }
    }

    @Nested
    @DisplayName("Gestion de estado y consultas")
    class GestionDeCitas {

        @Test
        @DisplayName("Actualiza el estado de una cita existente")
        void actualizaEstadoDeCitaExistente() {
            // Arrange
            Cita cita = Cita.builder().id(100L).estado("Agendada").build();
            when(citaRepository.findById(100L)).thenReturn(Optional.of(cita));
            when(citaRepository.save(cita)).thenReturn(cita);

            // Act
            Cita actualizada = citaService.actualizarEstadoCita(100L, "Completada");

            // Assert
            assertEquals("Completada", actualizada.getEstado());
            verify(citaRepository).save(cita);
        }

        @Test
        @DisplayName("Falla al actualizar el estado de una cita inexistente")
        void actualizarEstadoDeCitaInexistenteFalla() {
            // Arrange
            when(citaRepository.findById(404L)).thenReturn(Optional.empty());

            // Act + Assert
            assertThrows(IllegalArgumentException.class,
                    () -> citaService.actualizarEstadoCita(404L, "Completada"));
            verify(citaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Obtiene una cita por id")
        void obtieneCitaPorId() {
            // Arrange
            Cita cita = Cita.builder().id(100L).build();
            when(citaRepository.findById(100L)).thenReturn(Optional.of(cita));

            // Act
            Optional<Cita> resultado = citaService.obtenerCitaPorId(100L);

            // Assert
            assertTrue(resultado.isPresent());
            assertEquals(100L, resultado.get().getId());
        }

        @Test
        @DisplayName("Cancelar una cita la elimina del repositorio")
        void cancelarCitaEliminaPorId() {
            // Act
            citaService.cancelarCita(100L);

            // Assert
            verify(citaRepository).deleteById(100L);
        }

        @Test
        @DisplayName("Lista citas por mascota, por veterinario y por rango de fechas")
        void listaCitasDesdeRepositorio() {
            // Arrange
            Cita cita = Cita.builder().id(100L).fechaHora(fechaHora).build();
            LocalDateTime inicio = fechaHora.minusDays(1);
            LocalDateTime fin = fechaHora.plusDays(1);
            when(citaRepository.findByMascotaId(10L)).thenReturn(List.of(cita));
            when(citaRepository.findByVeterinarioId(2L)).thenReturn(List.of(cita));
            when(citaRepository.findByFechaHoraBetween(inicio, fin)).thenReturn(List.of(cita));

            // Act + Assert
            assertEquals(1, citaService.listarCitasPorMascota(10L).size());
            assertEquals(1, citaService.listarCitasPorVeterinario(2L).size());
            assertEquals(1, citaService.listarCitasEntreFechas(inicio, fin).size());
        }
    }
}

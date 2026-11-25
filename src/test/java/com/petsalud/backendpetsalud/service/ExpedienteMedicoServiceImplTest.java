package com.petsalud.backendpetsalud.service;

import com.petsalud.backendpetsalud.entity.ExpedienteMedico;
import com.petsalud.backendpetsalud.entity.Mascota;
import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.repository.ExpedienteMedicoRepository;
import com.petsalud.backendpetsalud.repository.MascotaRepository;
import com.petsalud.backendpetsalud.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExpedienteMedicoServiceImpl")
class ExpedienteMedicoServiceImplTest {

    @Mock
    private ExpedienteMedicoRepository expedienteMedicoRepository;

    @Mock
    private MascotaRepository mascotaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private ExpedienteMedicoServiceImpl expedienteMedicoService;

    @Test
    @DisplayName("Registra un expediente completo vinculado a la mascota y al veterinario")
    void registraExpedienteCompletoConMascotaYVeterinario() {
        // Arrange
        Mascota mascota = Mascota.builder().id(10L).nombre("Firulais").build();
        Usuario veterinario = Usuario.builder().id(2L).rol(Rol.EMPLEADO).build();
        ExpedienteMedico registro = ExpedienteMedico.builder()
                .descripcion("Control anual")
                .diagnostico("Estado saludable")
                .tratamiento("Vacunación")
                .build();
        when(mascotaRepository.findById(10L)).thenReturn(Optional.of(mascota));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(veterinario));
        when(expedienteMedicoRepository.save(any(ExpedienteMedico.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ExpedienteMedico guardado = expedienteMedicoService.agregarRegistro(10L, 2L, registro);

        // Assert
        assertEquals("Control anual", guardado.getDescripcion());
        assertEquals("Estado saludable", guardado.getDiagnostico());
        assertEquals("Vacunación", guardado.getTratamiento());
        assertSame(mascota, guardado.getMascota());
        assertSame(veterinario, guardado.getVeterinario());
        assertNotNull(guardado.getFecha());
        verify(expedienteMedicoRepository).save(registro);
    }

    @Test
    @DisplayName("Rechaza un expediente si falta algún dato clínico requerido")
    void rechazaExpedienteIncompleto() {
        // Arrange
        ExpedienteMedico sinDescripcion = ExpedienteMedico.builder()
                .diagnostico("Diagnóstico")
                .tratamiento("Tratamiento")
                .build();
        ExpedienteMedico sinDiagnostico = ExpedienteMedico.builder()
                .descripcion("Descripción")
                .tratamiento("Tratamiento")
                .build();
        ExpedienteMedico sinTratamiento = ExpedienteMedico.builder()
                .descripcion("Descripción")
                .diagnostico("Diagnóstico")
                .tratamiento(" ")
                .build();

        // Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> expedienteMedicoService.agregarRegistro(10L, 2L, sinDescripcion));
        assertThrows(IllegalArgumentException.class,
                () -> expedienteMedicoService.agregarRegistro(10L, 2L, sinDiagnostico));
        assertThrows(IllegalArgumentException.class,
                () -> expedienteMedicoService.agregarRegistro(10L, 2L, sinTratamiento));

        verify(mascotaRepository, never()).findById(any());
        verify(usuarioRepository, never()).findById(any());
        verify(expedienteMedicoRepository, never()).save(any(ExpedienteMedico.class));
    }

    @Test
    @DisplayName("Rechaza mascotas inexistentes al registrar un expediente")
    void rechazaMascotaInexistente() {
        // Arrange
        ExpedienteMedico registro = ExpedienteMedico.builder()
                .descripcion("Control")
                .diagnostico("Saludable")
                .tratamiento("Ninguno")
                .build();
        when(mascotaRepository.findById(10L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> expedienteMedicoService.agregarRegistro(10L, 2L, registro));
        verify(expedienteMedicoRepository, never()).save(any(ExpedienteMedico.class));
    }

    @Test
    @DisplayName("Rechaza veterinarios inexistentes al registrar un expediente")
    void rechazaVeterinarioInexistente() {
        // Arrange
        ExpedienteMedico registro = ExpedienteMedico.builder()
                .descripcion("Control")
                .diagnostico("Saludable")
                .tratamiento("Ninguno")
                .build();
        when(mascotaRepository.findById(10L))
                .thenReturn(Optional.of(Mascota.builder().id(10L).build()));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> expedienteMedicoService.agregarRegistro(10L, 2L, registro));
        verify(expedienteMedicoRepository, never()).save(any(ExpedienteMedico.class));
    }
}

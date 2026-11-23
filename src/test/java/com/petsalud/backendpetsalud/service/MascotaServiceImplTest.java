package com.petsalud.backendpetsalud.service;

import com.petsalud.backendpetsalud.entity.Mascota;
import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.repository.MascotaRepository;
import com.petsalud.backendpetsalud.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Semana 5 - Relacion usuario CLIENTE - mascotas (MascotaServiceImpl).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MascotaServiceImpl - relacion usuario-mascota")
class MascotaServiceImplTest {

    @Mock
    private MascotaRepository mascotaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private MascotaServiceImpl mascotaService;

    private Usuario cliente;

    @BeforeEach
    void setUp() {
        cliente = Usuario.builder()
                .id(1L).email("ana.rojas@petsalud.cl").password("hash")
                .nombre("Ana").apellido("Rojas").direccion("Calle 123")
                .rol(Rol.CLIENTE)
                .build();
    }

    @Test
    @DisplayName("Un CLIENTE registrado puede asociar una mascota a su cuenta")
    void registraMascotaAsociadaAlCliente() {
        // Arrange
        Mascota mascota = Mascota.builder().nombre("Michi").especie("Gato").build();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(mascotaRepository.save(mascota)).thenReturn(mascota);

        // Act
        Mascota registrada = mascotaService.registrarMascota(mascota, 1L);

        // Assert
        assertSame(cliente, registrada.getPropietario());
        assertEquals(Rol.CLIENTE, registrada.getPropietario().getRol());
        verify(mascotaRepository).save(mascota);
    }

    @Test
    @DisplayName("No registra una mascota si el propietario no existe")
    void rechazaMascotaConPropietarioInexistente() {
        // Arrange
        Mascota mascota = Mascota.builder().nombre("Michi").especie("Gato").build();
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> mascotaService.registrarMascota(mascota, 99L));
        assertEquals("Propietario no encontrado", ex.getMessage());
        verify(mascotaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Lista las mascotas asociadas a un CLIENTE")
    void listaMascotasDelCliente() {
        // Arrange
        Mascota perro = Mascota.builder().nombre("Firulais").especie("Perro").propietario(cliente).build();
        Mascota gato = Mascota.builder().nombre("Michi").especie("Gato").propietario(cliente).build();
        when(mascotaRepository.findByPropietarioId(1L)).thenReturn(List.of(perro, gato));

        // Act
        List<Mascota> mascotas = mascotaService.listarMascotasPorPropietario(1L);

        // Assert
        assertEquals(2, mascotas.size());
        assertTrue(mascotas.stream().allMatch(m -> m.getPropietario().getRol() == Rol.CLIENTE));
    }

    @Test
    @DisplayName("Actualiza los datos de una mascota existente")
    void actualizaMascotaExistente() {
        // Arrange
        Mascota actual = Mascota.builder().id(10L).nombre("Firu").especie("Perro").build();
        Mascota cambios = Mascota.builder().nombre("Firulais").especie("Perro").raza("Quiltro")
                .fechaNacimiento(LocalDate.of(2022, 5, 1)).sexo("Macho").build();
        when(mascotaRepository.findById(10L)).thenReturn(Optional.of(actual));
        when(mascotaRepository.save(actual)).thenReturn(actual);

        // Act
        Mascota actualizada = mascotaService.actualizarMascota(10L, cambios);

        // Assert
        assertEquals("Firulais", actualizada.getNombre());
        assertEquals("Quiltro", actualizada.getRaza());
        assertEquals("Macho", actualizada.getSexo());
    }

    @Test
    @DisplayName("Falla al actualizar una mascota inexistente")
    void actualizarMascotaInexistenteFalla() {
        // Arrange
        when(mascotaRepository.findById(404L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(IllegalArgumentException.class,
                () -> mascotaService.actualizarMascota(404L, Mascota.builder().build()));
        verify(mascotaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Obtiene y elimina una mascota por id")
    void obtieneYEliminaMascota() {
        // Arrange
        Mascota mascota = Mascota.builder().id(10L).nombre("Firulais").build();
        when(mascotaRepository.findById(10L)).thenReturn(Optional.of(mascota));

        // Act
        Optional<Mascota> encontrada = mascotaService.obtenerMascotaPorId(10L);
        mascotaService.eliminarMascota(10L);

        // Assert
        assertTrue(encontrada.isPresent());
        verify(mascotaRepository).deleteById(10L);
    }
}

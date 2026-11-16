package com.petsalud.backendpetsalud.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petsalud.backendpetsalud.dto.UsuarioResponse;
import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.repository.UsuarioRepository;
import com.petsalud.backendpetsalud.security.UsuarioDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioServiceImpl usuarioService;

    @Test
    void registraConPasswordCodificadaYRolClienteAunqueSeSoliciteOtroRol() {
        // Arrange
        when(usuarioRepository.findByEmail("cliente@petsalud.cl")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("una-clave-segura")).thenReturn("hash-seguro");
        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Usuario usuario = usuarioCompleto();
        usuario.setPassword("una-clave-segura");
        usuario.setRol(Rol.GERENTE);

        // Act
        Usuario registrado = usuarioService.registrarUsuario(usuario);

        // Assert
        assertEquals("hash-seguro", registrado.getPassword());
        assertNotEquals("una-clave-segura", registrado.getPassword());
        assertEquals(Rol.CLIENTE, registrado.getRol());
        verify(usuarioRepository).findByEmail("cliente@petsalud.cl");
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void rechazaUsuarioSinNombreAntesDeConsultarRepositorio() {
        // Arrange
        Usuario usuario = usuarioCompleto();
        usuario.setNombre(" ");

        // Act
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> usuarioService.registrarUsuario(usuario));

        // Assert
        assertEquals("Nombre, apellido, email y dirección son obligatorios", error.getMessage());
        verifyNoInteractions(usuarioRepository, passwordEncoder);
    }

    @Test
    void rechazaUsuarioSinApellidoAntesDeConsultarRepositorio() {
        // Arrange
        Usuario usuario = usuarioCompleto();
        usuario.setApellido(null);

        // Act
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> usuarioService.registrarUsuario(usuario));

        // Assert
        assertEquals("Nombre, apellido, email y dirección son obligatorios", error.getMessage());
        verifyNoInteractions(usuarioRepository, passwordEncoder);
    }

    @Test
    void rechazaUsuarioSinEmailAntesDeConsultarRepositorio() {
        // Arrange
        Usuario usuario = usuarioCompleto();
        usuario.setEmail("");

        // Act
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> usuarioService.registrarUsuario(usuario));

        // Assert
        assertEquals("Nombre, apellido, email y dirección son obligatorios", error.getMessage());
        verifyNoInteractions(usuarioRepository, passwordEncoder);
    }

    @Test
    void rechazaUsuarioSinDireccionAntesDeConsultarRepositorio() {
        // Arrange
        Usuario usuario = usuarioCompleto();
        usuario.setDireccion(" ");

        // Act
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> usuarioService.registrarUsuario(usuario));

        // Assert
        assertEquals("Nombre, apellido, email y dirección son obligatorios", error.getMessage());
        verifyNoInteractions(usuarioRepository, passwordEncoder);
    }

    @Test
    void rechazaEmailConFormatoInvalido() {
        // Arrange
        Usuario usuario = usuarioCompleto();
        usuario.setEmail("correo-invalido");

        // Act
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> usuarioService.registrarUsuario(usuario));

        // Assert
        assertEquals("Debe indicar un correo válido", error.getMessage());
        verifyNoInteractions(usuarioRepository, passwordEncoder);
    }

    @Test
    void rechazaPasswordMenorAlMinimo() {
        // Arrange
        Usuario usuario = usuarioCompleto();
        usuario.setPassword("corta");

        // Act
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> usuarioService.registrarUsuario(usuario));

        // Assert
        assertEquals("La contraseña debe tener al menos 8 caracteres", error.getMessage());
        verifyNoInteractions(usuarioRepository, passwordEncoder);
    }

    @Test
    void rechazaRegistroConEmailExistente() {
        // Arrange
        Usuario usuario = usuarioCompleto();
        when(usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));

        // Act
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> usuarioService.registrarUsuario(usuario));

        // Assert
        assertEquals("El email ya está registrado", error.getMessage());
        verify(usuarioRepository).findByEmail(usuario.getEmail());
        verify(usuarioRepository, never()).save(any(Usuario.class));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void obtieneUsuarioPorId() {
        // Arrange
        Usuario usuario = usuarioCompleto();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        // Act
        Optional<Usuario> resultado = usuarioService.obtenerUsuarioPorId(1L);

        // Assert
        assertSame(usuario, resultado.orElseThrow());
    }

    @Test
    void obtieneUsuarioPorEmail() {
        // Arrange
        Usuario usuario = usuarioCompleto();
        when(usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));

        // Act
        Optional<Usuario> resultado = usuarioService.obtenerUsuarioPorEmail(usuario.getEmail());

        // Assert
        assertSame(usuario, resultado.orElseThrow());
    }

    @Test
    void actualizaDatosPersonalesSinCambiarEmailNiPassword() {
        // Arrange
        Usuario existente = usuarioCompleto();
        Usuario cambios = Usuario.builder()
                .nombre("María")
                .apellido("López")
                .telefono("222")
                .direccion("Calle nueva")
                .email("otro@petsalud.cl")
                .password("otra-clave")
                .build();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(usuarioRepository.save(existente)).thenReturn(existente);

        // Act
        Usuario actualizado = usuarioService.actualizarUsuario(1L, cambios);

        // Assert
        assertEquals("María", actualizado.getNombre());
        assertEquals("López", actualizado.getApellido());
        assertEquals("222", actualizado.getTelefono());
        assertEquals("Calle nueva", actualizado.getDireccion());
        assertEquals("cliente@petsalud.cl", actualizado.getEmail());
        assertEquals("hash-original", actualizado.getPassword());
        verify(usuarioRepository).save(existente);
    }

    @Test
    void actualizarUsuarioInexistenteFalla() {
        // Arrange
        when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());

        // Act
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> usuarioService.actualizarUsuario(1L, usuarioCompleto()));

        // Assert
        assertEquals("Usuario no encontrado", error.getMessage());
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void eliminaUsuarioPorId() {
        // Arrange
        Long usuarioId = 1L;

        // Act
        usuarioService.eliminarUsuario(usuarioId);

        // Assert
        verify(usuarioRepository).deleteById(usuarioId);
    }

    @Test
    void listaUsuariosDesdeRepositorio() {
        // Arrange
        List<Usuario> usuarios = List.of(usuarioCompleto());
        when(usuarioRepository.findAll()).thenReturn(usuarios);

        // Act
        List<Usuario> resultado = usuarioService.listarUsuarios();

        // Assert
        assertSame(usuarios, resultado);
    }

    @Test
    void cambiaRolDelUsuario() {
        // Arrange
        Usuario usuario = usuarioCompleto();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(usuario)).thenReturn(usuario);

        // Act
        Usuario actualizado = usuarioService.cambiarRol(1L, Rol.GERENTE);

        // Assert
        assertEquals(Rol.GERENTE, actualizado.getRol());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void rechazaCambioDeRolNuloSinConsultarRepositorio() {
        // Arrange
        Long usuarioId = 1L;

        // Act
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> usuarioService.cambiarRol(usuarioId, null));

        // Assert
        assertEquals("Debe indicar un rol: CLIENTE, EMPLEADO o GERENTE", error.getMessage());
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void cambiarRolDeUsuarioInexistenteFalla() {
        // Arrange
        when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());

        // Act
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> usuarioService.cambiarRol(1L, Rol.GERENTE));

        // Assert
        assertEquals("Usuario no encontrado", error.getMessage());
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void usuarioIndicaSiTieneTodosLosDatosObligatorios() {
        // Arrange
        Usuario completo = usuarioCompleto();
        Usuario incompleto = usuarioCompleto();
        incompleto.setDireccion(null);

        // Act
        boolean completoTieneDatos = completo.tieneDatosObligatoriosCompletos();
        boolean incompletoTieneDatos = incompleto.tieneDatosObligatoriosCompletos();

        // Assert
        assertTrue(completoTieneDatos);
        assertFalse(incompletoTieneDatos);
    }

    @Test
    void respuestaDeUsuarioNoIncluyeLaPassword() throws Exception {
        // Arrange
        UsuarioResponse response = new UsuarioResponse(
                1L,
                "cliente@petsalud.cl",
                "Ana",
                "Pérez",
                null,
                null,
                Rol.CLIENTE
        );

        // Act
        String json = new ObjectMapper().writeValueAsString(response);

        // Assert
        assertTrue(json.contains("\"email\":\"cliente@petsalud.cl\""));
        assertFalse(json.contains("password"));
    }

    @Test
    void authoritiesDeUsuarioCoincidenConSusRoles() {
        // Arrange
        for (Rol rol : Rol.values()) {
            Usuario usuario = Usuario.builder()
                    .email("usuario@petsalud.cl")
                    .password("hash")
                    .rol(rol)
                    .build();

            // Act
            Set<String> authorities = new UsuarioDetails(usuario).getAuthorities().stream()
                    .map(authority -> authority.getAuthority())
                    .collect(java.util.stream.Collectors.toSet());

            // Assert
            assertEquals(Set.of("ROLE_" + rol.name()), authorities);
        }
    }

    private Usuario usuarioCompleto() {
        return Usuario.builder()
                .id(1L)
                .email("cliente@petsalud.cl")
                .password("hash-original")
                .nombre("Ana")
                .apellido("Pérez")
                .direccion("Calle 123")
                .rol(Rol.CLIENTE)
                .build();
    }
}

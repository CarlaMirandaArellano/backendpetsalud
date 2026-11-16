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
        when(usuarioRepository.findByEmail("cliente@petsalud.cl")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("una-clave-segura")).thenReturn("hash-seguro");
        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Usuario usuario = usuarioCompleto();
        usuario.setPassword("una-clave-segura");
        usuario.setRol(Rol.GERENTE);

        Usuario registrado = usuarioService.registrarUsuario(usuario);

        assertEquals("hash-seguro", registrado.getPassword());
        assertNotEquals("una-clave-segura", registrado.getPassword());
        assertEquals(Rol.CLIENTE, registrado.getRol());
        verify(usuarioRepository).findByEmail("cliente@petsalud.cl");
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void rechazaUsuarioSinNombreAntesDeConsultarRepositorio() {
        Usuario usuario = usuarioCompleto();
        usuario.setNombre(" ");

        assertThrows(IllegalArgumentException.class, () -> usuarioService.registrarUsuario(usuario));

        verifyNoInteractions(usuarioRepository, passwordEncoder);
    }

    @Test
    void rechazaUsuarioSinApellidoAntesDeConsultarRepositorio() {
        Usuario usuario = usuarioCompleto();
        usuario.setApellido(null);

        assertThrows(IllegalArgumentException.class, () -> usuarioService.registrarUsuario(usuario));

        verifyNoInteractions(usuarioRepository, passwordEncoder);
    }

    @Test
    void rechazaUsuarioSinEmailAntesDeConsultarRepositorio() {
        Usuario usuario = usuarioCompleto();
        usuario.setEmail("");

        assertThrows(IllegalArgumentException.class, () -> usuarioService.registrarUsuario(usuario));

        verifyNoInteractions(usuarioRepository, passwordEncoder);
    }

    @Test
    void rechazaUsuarioSinDireccionAntesDeConsultarRepositorio() {
        Usuario usuario = usuarioCompleto();
        usuario.setDireccion(" ");

        assertThrows(IllegalArgumentException.class, () -> usuarioService.registrarUsuario(usuario));

        verifyNoInteractions(usuarioRepository, passwordEncoder);
    }

    @Test
    void rechazaEmailConFormatoInvalido() {
        Usuario usuario = usuarioCompleto();
        usuario.setEmail("correo-invalido");

        assertThrows(IllegalArgumentException.class, () -> usuarioService.registrarUsuario(usuario));

        verifyNoInteractions(usuarioRepository, passwordEncoder);
    }

    @Test
    void rechazaPasswordMenorAlMinimo() {
        Usuario usuario = usuarioCompleto();
        usuario.setPassword("corta");

        assertThrows(IllegalArgumentException.class, () -> usuarioService.registrarUsuario(usuario));

        verifyNoInteractions(usuarioRepository, passwordEncoder);
    }

    @Test
    void rechazaRegistroConEmailExistente() {
        Usuario usuario = usuarioCompleto();
        when(usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));

        assertThrows(IllegalArgumentException.class, () -> usuarioService.registrarUsuario(usuario));

        verify(usuarioRepository).findByEmail(usuario.getEmail());
        verify(usuarioRepository, never()).save(any(Usuario.class));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void obtieneUsuarioPorId() {
        Usuario usuario = usuarioCompleto();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        assertSame(usuario, usuarioService.obtenerUsuarioPorId(1L).orElseThrow());
    }

    @Test
    void obtieneUsuarioPorEmail() {
        Usuario usuario = usuarioCompleto();
        when(usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));

        assertSame(usuario, usuarioService.obtenerUsuarioPorEmail(usuario.getEmail()).orElseThrow());
    }

    @Test
    void actualizaDatosPersonalesSinCambiarEmailNiPassword() {
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

        Usuario actualizado = usuarioService.actualizarUsuario(1L, cambios);

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
        when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> usuarioService.actualizarUsuario(1L, usuarioCompleto()));

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void eliminaUsuarioPorId() {
        usuarioService.eliminarUsuario(1L);

        verify(usuarioRepository).deleteById(1L);
    }

    @Test
    void listaUsuariosDesdeRepositorio() {
        List<Usuario> usuarios = List.of(usuarioCompleto());
        when(usuarioRepository.findAll()).thenReturn(usuarios);

        assertSame(usuarios, usuarioService.listarUsuarios());
    }

    @Test
    void cambiaRolDelUsuario() {
        Usuario usuario = usuarioCompleto();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(usuario)).thenReturn(usuario);

        Usuario actualizado = usuarioService.cambiarRol(1L, Rol.GERENTE);

        assertEquals(Rol.GERENTE, actualizado.getRol());
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void rechazaCambioDeRolNuloSinConsultarRepositorio() {
        assertThrows(IllegalArgumentException.class, () -> usuarioService.cambiarRol(1L, null));

        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void cambiarRolDeUsuarioInexistenteFalla() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> usuarioService.cambiarRol(1L, Rol.GERENTE));

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void usuarioIndicaSiTieneTodosLosDatosObligatorios() {
        assertTrue(usuarioCompleto().tieneDatosObligatoriosCompletos());

        Usuario incompleto = usuarioCompleto();
        incompleto.setDireccion(null);
        assertFalse(incompleto.tieneDatosObligatoriosCompletos());
    }

    @Test
    void respuestaDeUsuarioNoIncluyeLaPassword() throws Exception {
        UsuarioResponse response = new UsuarioResponse(
                1L,
                "cliente@petsalud.cl",
                "Ana",
                "Pérez",
                null,
                null,
                Rol.CLIENTE
        );

        String json = new ObjectMapper().writeValueAsString(response);

        assertTrue(json.contains("\"email\":\"cliente@petsalud.cl\""));
        assertFalse(json.contains("password"));
    }

    @Test
    void authoritiesDeUsuarioCoincidenConSusRoles() {
        for (Rol rol : Rol.values()) {
            Usuario usuario = Usuario.builder()
                    .email("usuario@petsalud.cl")
                    .password("hash")
                    .rol(rol)
                    .build();

            Set<String> authorities = new UsuarioDetails(usuario).getAuthorities().stream()
                    .map(authority -> authority.getAuthority())
                    .collect(java.util.stream.Collectors.toSet());

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

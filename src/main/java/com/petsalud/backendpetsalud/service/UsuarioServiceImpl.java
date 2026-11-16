package com.petsalud.backendpetsalud.service;

import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioServiceImpl implements UsuarioService {

    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioServiceImpl(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Usuario registrarUsuario(Usuario usuario) {
        if (!usuario.tieneDatosObligatoriosCompletos()) {
            throw new IllegalArgumentException("Nombre, apellido, email y dirección son obligatorios");
        }
        if (!usuario.getEmail().contains("@")) {
            throw new IllegalArgumentException("Debe indicar un correo válido");
        }
        if (usuario.getPassword() == null || usuario.getPassword().length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException(
                    "La contraseña debe tener al menos " + MIN_PASSWORD_LENGTH + " caracteres");
        }
        usuario.setEmail(usuario.getEmail().trim().toLowerCase());
        Optional<Usuario> usuarioExistente = usuarioRepository.findByEmail(usuario.getEmail());
        if (usuarioExistente.isPresent()) {
            throw new IllegalArgumentException("El email ya está registrado");
        }
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        usuario.setRol(Rol.CLIENTE);
        return usuarioRepository.save(usuario);
    }

    @Override
    public Optional<Usuario> obtenerUsuarioPorId(Long id) {
        return usuarioRepository.findById(id);
    }

    @Override
    public Optional<Usuario> obtenerUsuarioPorEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }

    @Override
    public Usuario actualizarUsuario(Long id, Usuario usuarioActualizado) {
        return usuarioRepository.findById(id).map(usuario -> {
            usuario.setNombre(usuarioActualizado.getNombre());
            usuario.setApellido(usuarioActualizado.getApellido());
            usuario.setTelefono(usuarioActualizado.getTelefono());
            usuario.setDireccion(usuarioActualizado.getDireccion());
            // No actualizar email ni password aquí por seguridad
            return usuarioRepository.save(usuario);
        }).orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    }

    @Override
    public void eliminarUsuario(Long id) {
        usuarioRepository.deleteById(id);
    }

    @Override
    public List<Usuario> listarUsuarios() {
        return usuarioRepository.findAll();
    }

    @Override
    public Usuario cambiarRol(Long id, Rol rol) {
        if (rol == null) {
            throw new IllegalArgumentException("Debe indicar un rol: CLIENTE, EMPLEADO o GERENTE");
        }
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        usuario.setRol(rol);
        return usuarioRepository.save(usuario);
    }
}

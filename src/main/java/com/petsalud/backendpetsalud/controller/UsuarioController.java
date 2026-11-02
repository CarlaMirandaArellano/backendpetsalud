package com.petsalud.backendpetsalud.controller;

import com.petsalud.backendpetsalud.dto.CambioRolRequest;
import com.petsalud.backendpetsalud.dto.UsuarioResponse;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.security.ResourceAccessGuard;
import com.petsalud.backendpetsalud.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private ResourceAccessGuard accessGuard;

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLIENTE', 'GERENTE')")
    public ResponseEntity<UsuarioResponse> obtenerUsuarioPorId(@PathVariable Long id) {
        accessGuard.requireUserAccess(id);
        return usuarioService.obtenerUsuarioPorId(id)
                .map(UsuarioResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLIENTE', 'GERENTE')")
    public ResponseEntity<UsuarioResponse> actualizarUsuario(@PathVariable Long id, @RequestBody Usuario usuario) {
        accessGuard.requireUserAccess(id);
        Usuario usuarioActualizado = usuarioService.actualizarUsuario(id, usuario);
        return ResponseEntity.ok(UsuarioResponse.from(usuarioActualizado));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('GERENTE')")
    public ResponseEntity<Void> eliminarUsuario(@PathVariable Long id) {
        usuarioService.eliminarUsuario(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize("hasRole('GERENTE')")
    public ResponseEntity<List<UsuarioResponse>> listarUsuarios() {
        List<Usuario> usuarios = usuarioService.listarUsuarios();
        return ResponseEntity.ok(usuarios.stream().map(UsuarioResponse::from).toList());
    }

    /** Solo GERENTE puede asignar roles (por ejemplo, dar de alta a un EMPLEADO). */
    @PutMapping("/{id}/rol")
    @PreAuthorize("hasRole('GERENTE')")
    public ResponseEntity<UsuarioResponse> cambiarRol(@PathVariable Long id, @RequestBody CambioRolRequest request) {
        Usuario actualizado = usuarioService.cambiarRol(id, request.rol());
        return ResponseEntity.ok(UsuarioResponse.from(actualizado));
    }
}

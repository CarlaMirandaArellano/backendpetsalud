package com.petsalud.backendpetsalud.controller;

import com.petsalud.backendpetsalud.dto.AuthResponse;
import com.petsalud.backendpetsalud.dto.LoginRequest;
import com.petsalud.backendpetsalud.dto.UsuarioRegistroRequest;
import com.petsalud.backendpetsalud.dto.UsuarioResponse;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.security.JwtUtil;
import com.petsalud.backendpetsalud.security.UsuarioDetails;
import com.petsalud.backendpetsalud.service.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints públicos de autenticación:
 *  POST /api/auth/registro -> crea una cuenta CLIENTE con la contraseña cifrada con BCrypt.
 *  POST /api/auth/login    -> valida correo y contraseña y devuelve un JWT.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UsuarioService usuarioService;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil,
                          UsuarioService usuarioService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.usuarioService = usuarioService;
    }

    @PostMapping("/registro")
    public ResponseEntity<UsuarioResponse> registrar(@RequestBody UsuarioRegistroRequest request) {
        Usuario usuario = Usuario.builder()
                .email(request.email())
                .password(request.password())
                .nombre(request.nombre())
                .apellido(request.apellido())
                .telefono(request.telefono())
                .direccion(request.direccion())
                .build();
        Usuario nuevo = usuarioService.registrarUsuario(usuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioResponse.from(nuevo));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        if (request == null || request.email() == null || request.password() == null) {
            throw new BadCredentialsException("Credenciales incompletas");
        }
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password()));
            UsuarioDetails usuario = (UsuarioDetails) authentication.getPrincipal();
            String token = jwtUtil.generarToken(usuario);
            log.info("Inicio de sesión exitoso: {}", usuario.getUsername());
            return ResponseEntity.ok(new AuthResponse(
                    token, "Bearer", jwtUtil.getExpirationMs(), usuario.getUsername(), usuario.getRol()));
        } catch (AuthenticationException e) {
            log.warn("Inicio de sesión fallido para {}", request.email());
            throw new BadCredentialsException("Correo o contraseña incorrectos");
        }
    }
}

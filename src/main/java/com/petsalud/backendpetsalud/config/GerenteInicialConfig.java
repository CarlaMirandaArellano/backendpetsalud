package com.petsalud.backendpetsalud.config;

import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

/**
 * Crea la primera cuenta GERENTE al iniciar, solo si se definieron las variables de entorno
 * GERENTE_EMAIL y GERENTE_PASSWORD y la cuenta aún no existe. Así ninguna credencial queda en el código.
 * Desde esa cuenta se asignan los roles EMPLEADO y GERENTE con PUT /api/usuarios/{id}/rol.
 */
@Configuration
public class GerenteInicialConfig {

    private static final Logger log = LoggerFactory.getLogger(GerenteInicialConfig.class);

    @Bean
    CommandLineRunner crearGerenteInicial(UsuarioRepository usuarioRepository,
                                          PasswordEncoder passwordEncoder,
                                          @Value("${app.gerente.email:}") String email,
                                          @Value("${app.gerente.password:}") String password) {
        return args -> {
            if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
                return;
            }
            String correo = email.trim().toLowerCase();
            if (usuarioRepository.findByEmail(correo).isPresent()) {
                return;
            }
            usuarioRepository.save(Usuario.builder()
                    .email(correo)
                    .password(passwordEncoder.encode(password))
                    .nombre("Gerente")
                    .rol(Rol.GERENTE)
                    .build());
            log.info("Cuenta GERENTE inicial creada: {}", correo);
        };
    }
}

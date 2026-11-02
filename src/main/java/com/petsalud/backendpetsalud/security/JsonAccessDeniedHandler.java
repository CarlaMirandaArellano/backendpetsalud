package com.petsalud.backendpetsalud.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** Responde 403 en JSON cuando el usuario está autenticado pero su rol no tiene permiso. */
@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(JsonAccessDeniedHandler.class);

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        String usuario = request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "anónimo";
        log.warn("Acceso denegado: usuario={} {} {}", usuario, request.getMethod(), request.getRequestURI());
        SecurityErrorWriter.write(response, HttpStatus.FORBIDDEN,
                "No tiene permisos para acceder a este recurso.", request.getRequestURI());
    }
}

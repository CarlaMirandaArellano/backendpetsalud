package com.petsalud.backendpetsalud.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro que se ejecuta una vez por petición, antes de UsernamePasswordAuthenticationFilter.
 * Lee el encabezado "Authorization: Bearer <token>", valida el JWT y, si es correcto,
 * deja la identidad del usuario en el SecurityContext.
 *
 * El filtro solo identifica: si no hay token o es inválido, la petición continúa sin identidad
 * y son las reglas de SecurityConfig / @PreAuthorize las que responden 401 o 403.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER = "Bearer ";

    private final JwtUtil jwtUtil;
    private final UsuarioDetailsService usuarioDetailsService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UsuarioDetailsService usuarioDetailsService) {
        this.jwtUtil = jwtUtil;
        this.usuarioDetailsService = usuarioDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER.length()).trim();
        try {
            String username = jwtUtil.extraerUsername(token);
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Se consultan los datos actuales del usuario: si cambió su rol o fue eliminado,
                // el cambio aplica de inmediato aunque el token siga vigente.
                UserDetails userDetails = usuarioDetailsService.loadUserByUsername(username);
                if (jwtUtil.esTokenValido(token, userDetails) && userDetails.isEnabled()
                        && userDetails.isAccountNonLocked()) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException e) {
            // Token alterado, expirado o de un usuario inexistente: se registra sin exponer el token.
            log.warn("Token JWT rechazado para {} {}: {}", request.getMethod(), request.getRequestURI(),
                    e.getClass().getSimpleName());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}

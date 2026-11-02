package com.petsalud.backendpetsalud.security;

import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTest {

    private static final String SECRET = Base64.getEncoder()
            .encodeToString("clave-de-prueba-de-32-bytes-min!".getBytes());
    private static final String OTRO_SECRET = Base64.getEncoder()
            .encodeToString("otra-clave-distinta-de-32-bytes!".getBytes());

    private final JwtUtil jwtUtil = new JwtUtil(SECRET, 60_000, "petsalud-backend");

    private UsuarioDetails usuario(String email, Rol rol) {
        return new UsuarioDetails(Usuario.builder().email(email).password("hash").rol(rol).build());
    }

    @Test
    void generaTokenConClaimsEsperadasYSinPassword() {
        String token = jwtUtil.generarToken(usuario("ana@petsalud.cl", Rol.CLIENTE));

        Claims claims = jwtUtil.validarYObtenerClaims(token);
        assertEquals("ana@petsalud.cl", claims.getSubject());
        assertEquals("CLIENTE", claims.get(JwtUtil.CLAIM_ROL));
        assertEquals("petsalud-backend", claims.getIssuer());
        assertNotNull(claims.getExpiration());
        assertNotNull(claims.getId());
        assertFalse(claims.containsKey("password"));
    }

    @Test
    void tokenEsValidoSoloParaSuUsuario() {
        String token = jwtUtil.generarToken(usuario("ana@petsalud.cl", Rol.CLIENTE));

        assertTrue(jwtUtil.esTokenValido(token, usuario("ana@petsalud.cl", Rol.CLIENTE)));
        assertFalse(jwtUtil.esTokenValido(token, usuario("otro@petsalud.cl", Rol.CLIENTE)));
    }

    @Test
    void rechazaTokenAlterado() {
        String token = jwtUtil.generarToken(usuario("ana@petsalud.cl", Rol.CLIENTE));
        String[] partes = token.split("\\.");
        // Se reemplaza el payload por uno que dice ser GERENTE, manteniendo la firma original
        String payloadFalso = Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"sub\":\"ana@petsalud.cl\",\"rol\":\"GERENTE\",\"iss\":\"petsalud-backend\"}".getBytes());
        String tokenAlterado = partes[0] + "." + payloadFalso + "." + partes[2];

        assertThrows(JwtException.class, () -> jwtUtil.validarYObtenerClaims(tokenAlterado));
    }

    @Test
    void rechazaTokenFirmadoConOtraClave() {
        JwtUtil otroEmisor = new JwtUtil(OTRO_SECRET, 60_000, "petsalud-backend");
        String token = otroEmisor.generarToken(usuario("ana@petsalud.cl", Rol.GERENTE));

        assertThrows(JwtException.class, () -> jwtUtil.validarYObtenerClaims(token));
    }

    @Test
    void rechazaTokenExpirado() {
        JwtUtil expiraAlInstante = new JwtUtil(SECRET, -1_000, "petsalud-backend");
        String token = expiraAlInstante.generarToken(usuario("ana@petsalud.cl", Rol.CLIENTE));

        assertThrows(JwtException.class, () -> jwtUtil.validarYObtenerClaims(token));
    }

    @Test
    void rechazaTokenSinFirma() {
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"none\"}".getBytes());
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"sub\":\"ana@petsalud.cl\",\"iss\":\"petsalud-backend\"}".getBytes());

        assertThrows(JwtException.class, () -> jwtUtil.validarYObtenerClaims(header + "." + payload + "."));
    }

    @Test
    void exigeClaveSecretaDeAlMenos256Bits() {
        String claveCorta = Base64.getEncoder().encodeToString("corta".getBytes());

        assertThrows(WeakKeyException.class, () -> new JwtUtil(claveCorta, 60_000, "petsalud-backend"));
        assertThrows(IllegalStateException.class, () -> new JwtUtil("", 60_000, "petsalud-backend"));
    }
}

package com.petsalud.backendpetsalud.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

/**
 * Genera y valida los JWT de la aplicación.
 *
 * - Firma HS256 con una clave secreta de al menos 256 bits (32 bytes) en Base64,
 *   leída desde la variable de entorno JWT_SECRET (nunca se guarda en el repositorio).
 * - Claims: sub (correo), rol, iss (emisor), iat (emisión), exp (expiración) y jti (identificador).
 * - El payload no lleva contraseñas ni datos personales: solo se codifica, no se cifra.
 */
@Component
public class JwtUtil {

    public static final String CLAIM_ROL = "rol";

    private final SecretKey key;
    private final long expirationMs;
    private final String issuer;

    public JwtUtil(
            @Value("${jwt.secret:}") String secretBase64,
            @Value("${jwt.expiration-ms:3600000}") long expirationMs,
            @Value("${jwt.issuer:petsalud-backend}") String issuer
    ) {
        if (!StringUtils.hasText(secretBase64)) {
            throw new IllegalStateException(
                    "Falta la variable de entorno JWT_SECRET (clave en Base64 de al menos 32 bytes).");
        }
        // hmacShaKeyFor rechaza claves de menos de 256 bits (WeakKeyException)
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretBase64));
        this.expirationMs = expirationMs;
        this.issuer = issuer;
    }

    /** Crea un token firmado para el usuario autenticado. */
    public String generarToken(UserDetails userDetails) {
        Date ahora = new Date();
        String rol = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.replaceFirst("^ROLE_", ""))
                .findFirst()
                .orElse("");

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userDetails.getUsername())
                .issuer(issuer)
                .claim(CLAIM_ROL, rol)
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + expirationMs))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Verifica la firma, el emisor y la expiración, y devuelve las claims.
     * Lanza JwtException si el token fue alterado, expiró, no tiene firma o es de otro emisor.
     */
    public Claims validarYObtenerClaims(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(key)          // solo acepta tokens firmados con nuestra clave (rechaza alg "none")
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** Correo (subject) contenido en un token válido. */
    public String extraerUsername(String token) {
        return validarYObtenerClaims(token).getSubject();
    }

    /** El token es válido si la firma y la expiración son correctas y pertenece al usuario indicado. */
    public boolean esTokenValido(String token, UserDetails userDetails) {
        try {
            return userDetails.getUsername().equals(extraerUsername(token));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}

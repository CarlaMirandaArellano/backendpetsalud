package com.petsalud.backendpetsalud.dto;

import com.petsalud.backendpetsalud.entity.Rol;

/** Respuesta del login: el token y los datos mínimos para que el cliente lo use. */
public record AuthResponse(String token, String tipo, long expiraEnMs, String email, Rol rol) {
}

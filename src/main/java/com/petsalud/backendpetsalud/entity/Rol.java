package com.petsalud.backendpetsalud.entity;

/**
 * Roles del sistema.
 * CLIENTE: dueños de mascotas, solo acceden a sus propios recursos.
 * EMPLEADO: personal de la clínica (veterinarios y atención), acceso clínico y de ventas.
 * GERENTE: administración de la clínica, acceso completo.
 */
public enum Rol {
    CLIENTE,
    EMPLEADO,
    GERENTE;

    /** Nombre de la autoridad que usa Spring Security (prefijo ROLE_ requerido por hasRole). */
    public String authority() {
        return "ROLE_" + name();
    }
}

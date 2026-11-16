package com.petsalud.backendpetsalud.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Proveedor que abastece a la clínica.
 * Un proveedor puede abastecer muchos productos; la relación se guarda en Producto.proveedor.
 */
@Entity
@Table(name = "proveedores")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false, unique = true)
    private String rut;

    @Column(nullable = false)
    private String email;

    private String telefono;
}

package com.petsalud.backendpetsalud.controller;

import com.petsalud.backendpetsalud.entity.Producto;
import com.petsalud.backendpetsalud.entity.Proveedor;
import com.petsalud.backendpetsalud.service.ProveedorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Gestión de proveedores: solo el GERENTE modifica; EMPLEADO puede consultar. */
@RestController
@RequestMapping("/api/proveedores")
public class ProveedorController {

    @Autowired
    private ProveedorService proveedorService;

    @PostMapping
    @PreAuthorize("hasRole('GERENTE')")
    public ResponseEntity<Proveedor> registrarProveedor(@RequestBody Proveedor proveedor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(proveedorService.registrarProveedor(proveedor));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('EMPLEADO', 'GERENTE')")
    public ResponseEntity<List<Proveedor>> listarProveedores() {
        return ResponseEntity.ok(proveedorService.listarProveedores());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'GERENTE')")
    public ResponseEntity<Proveedor> obtenerProveedor(@PathVariable Long id) {
        return proveedorService.obtenerProveedorPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/productos/{productoId}")
    @PreAuthorize("hasRole('GERENTE')")
    public ResponseEntity<Producto> asignarProducto(@PathVariable Long id, @PathVariable Long productoId) {
        return ResponseEntity.ok(proveedorService.asignarProducto(id, productoId));
    }

    @GetMapping("/{id}/productos")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'GERENTE')")
    public ResponseEntity<List<Producto>> listarProductos(@PathVariable Long id) {
        return ResponseEntity.ok(proveedorService.listarProductosDeProveedor(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('GERENTE')")
    public ResponseEntity<Void> eliminarProveedor(@PathVariable Long id) {
        proveedorService.eliminarProveedor(id);
        return ResponseEntity.noContent().build();
    }
}

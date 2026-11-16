package com.petsalud.backendpetsalud.service;

import com.petsalud.backendpetsalud.entity.Producto;
import com.petsalud.backendpetsalud.entity.Proveedor;

import java.util.List;
import java.util.Optional;

public interface ProveedorService {
    Proveedor registrarProveedor(Proveedor proveedor);
    Optional<Proveedor> obtenerProveedorPorId(Long id);
    List<Proveedor> listarProveedores();
    Producto asignarProducto(Long proveedorId, Long productoId);
    List<Producto> listarProductosDeProveedor(Long proveedorId);
    void eliminarProveedor(Long id);
}

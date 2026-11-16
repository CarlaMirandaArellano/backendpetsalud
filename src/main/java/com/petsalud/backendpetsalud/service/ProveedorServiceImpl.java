package com.petsalud.backendpetsalud.service;

import com.petsalud.backendpetsalud.entity.Producto;
import com.petsalud.backendpetsalud.entity.Proveedor;
import com.petsalud.backendpetsalud.repository.ProductoRepository;
import com.petsalud.backendpetsalud.repository.ProveedorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ProveedorServiceImpl implements ProveedorService {

    @Autowired
    private ProveedorRepository proveedorRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Override
    public Proveedor registrarProveedor(Proveedor proveedor) {
        if (proveedor == null || estaVacio(proveedor.getNombre())) {
            throw new IllegalArgumentException("El nombre del proveedor es obligatorio");
        }
        if (estaVacio(proveedor.getRut())) {
            throw new IllegalArgumentException("El RUT del proveedor es obligatorio");
        }
        if (estaVacio(proveedor.getEmail()) || !proveedor.getEmail().contains("@")) {
            throw new IllegalArgumentException("Debe indicar un correo válido para el proveedor");
        }
        proveedor.setRut(proveedor.getRut().trim().toUpperCase());
        if (proveedorRepository.existsByRut(proveedor.getRut())) {
            throw new IllegalArgumentException("Ya existe un proveedor con ese RUT");
        }
        return proveedorRepository.save(proveedor);
    }

    @Override
    public Optional<Proveedor> obtenerProveedorPorId(Long id) {
        return proveedorRepository.findById(id);
    }

    @Override
    public List<Proveedor> listarProveedores() {
        return proveedorRepository.findAll();
    }

    @Override
    @Transactional
    public Producto asignarProducto(Long proveedorId, Long productoId) {
        Proveedor proveedor = buscarProveedor(proveedorId);
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
        producto.setProveedor(proveedor);
        return productoRepository.save(producto);
    }

    @Override
    public List<Producto> listarProductosDeProveedor(Long proveedorId) {
        buscarProveedor(proveedorId);
        return productoRepository.findByProveedorId(proveedorId);
    }

    @Override
    public void eliminarProveedor(Long id) {
        buscarProveedor(id);
        // No se elimina un proveedor que todavía abastece productos, para no dejarlos huérfanos
        if (productoRepository.existsByProveedorId(id)) {
            throw new IllegalArgumentException("No se puede eliminar un proveedor con productos asociados");
        }
        proveedorRepository.deleteById(id);
    }

    private Proveedor buscarProveedor(Long id) {
        return proveedorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Proveedor no encontrado"));
    }

    private boolean estaVacio(String valor) {
        return valor == null || valor.isBlank();
    }
}

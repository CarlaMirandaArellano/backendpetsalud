package com.petsalud.backendpetsalud.repository;

import com.petsalud.backendpetsalud.entity.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {
    boolean existsByRut(String rut);
}

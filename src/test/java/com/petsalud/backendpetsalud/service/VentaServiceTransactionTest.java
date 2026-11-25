package com.petsalud.backendpetsalud.service;

import com.petsalud.backendpetsalud.entity.Producto;
import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.repository.ProductoRepository;
import com.petsalud.backendpetsalud.repository.UsuarioRepository;
import com.petsalud.backendpetsalud.repository.VentaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(VentaServiceImpl.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:venta_transaction_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("VentaServiceImpl - transacciones")
class VentaServiceTransactionTest {

    @Autowired
    private VentaServiceImpl ventaService;

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("Revierte los descuentos de stock si falla la operación después de actualizar productos")
    void revierteStockCuandoFallaLaVenta() {
        // Arrange
        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nombre("Ana")
                .apellido("Rojas")
                .email("ana@petsalud.cl")
                .direccion("Calle 123")
                .password("hash")
                .rol(Rol.CLIENTE)
                .build());
        Producto productoConPrecio = productoRepository.save(Producto.builder()
                .nombre("Alimento")
                .precio(new BigDecimal("1200.00"))
                .stock(3)
                .build());
        Producto productoSinPrecio = productoRepository.save(Producto.builder()
                .nombre("Juguete")
                .precio(null)
                .stock(2)
                .build());

        // Act
        assertThrows(NullPointerException.class,
                () -> ventaService.realizarVenta(
                        usuario.getId(),
                        List.of(productoConPrecio.getId(), productoSinPrecio.getId())));

        // Assert
        assertEquals(3, productoRepository.findById(productoConPrecio.getId()).orElseThrow().getStock());
        assertEquals(2, productoRepository.findById(productoSinPrecio.getId()).orElseThrow().getStock());
        assertEquals(0, ventaRepository.count());
    }
}

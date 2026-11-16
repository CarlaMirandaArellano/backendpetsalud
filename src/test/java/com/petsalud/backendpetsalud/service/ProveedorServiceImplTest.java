package com.petsalud.backendpetsalud.service;

import com.petsalud.backendpetsalud.entity.Producto;
import com.petsalud.backendpetsalud.entity.Proveedor;
import com.petsalud.backendpetsalud.repository.ProductoRepository;
import com.petsalud.backendpetsalud.repository.ProveedorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de ProveedorServiceImpl.
 * Los repositorios se simulan con Mockito, por lo que no se necesita base de datos.
 */
@ExtendWith(MockitoExtension.class)
class ProveedorServiceImplTest {

    @Mock
    private ProveedorRepository proveedorRepository;

    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private ProveedorServiceImpl proveedorService;

    // ---------- Relación Producto -> Proveedor ----------

    @Test
    @DisplayName("Asigna un producto a su proveedor (relación ManyToOne Producto-Proveedor)")
    void asignaProductoAProveedor() {
        // Arrange
        Proveedor proveedor = proveedor(1L, "Distribuidora Vet", "76.123.456-7");
        Producto producto = producto(10L, "Alimento premium");
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor));
        when(productoRepository.findById(10L)).thenReturn(Optional.of(producto));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Producto resultado = proveedorService.asignarProducto(1L, 10L);

        // Assert
        assertSame(proveedor, resultado.getProveedor());
        verify(productoRepository).save(producto);
    }

    @Test
    @DisplayName("Lista los productos que pertenecen a un proveedor")
    void listaProductosDeUnProveedor() {
        // Arrange
        Proveedor proveedor = proveedor(1L, "Distribuidora Vet", "76.123.456-7");
        Producto alimento = producto(10L, "Alimento premium");
        Producto arena = producto(11L, "Arena sanitaria");
        alimento.setProveedor(proveedor);
        arena.setProveedor(proveedor);
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor));
        when(productoRepository.findByProveedorId(1L)).thenReturn(List.of(alimento, arena));

        // Act
        List<Producto> productos = proveedorService.listarProductosDeProveedor(1L);

        // Assert
        assertEquals(2, productos.size());
        productos.forEach(p -> assertSame(proveedor, p.getProveedor()));
    }

    @Test
    @DisplayName("No asigna el producto si el proveedor no existe")
    void noAsignaSiProveedorNoExiste() {
        // Arrange
        when(proveedorRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> proveedorService.asignarProducto(99L, 10L));
        assertEquals("Proveedor no encontrado", ex.getMessage());
        verifyNoInteractions(productoRepository);
    }

    @Test
    @DisplayName("No asigna si el producto no existe")
    void noAsignaSiProductoNoExiste() {
        // Arrange
        when(proveedorRepository.findById(1L))
                .thenReturn(Optional.of(proveedor(1L, "Distribuidora Vet", "76.123.456-7")));
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> proveedorService.asignarProducto(1L, 99L));
        verify(productoRepository, never()).save(any());
    }

    // ---------- Registro y datos obligatorios ----------

    @Test
    @DisplayName("Registra un proveedor válido normalizando el RUT")
    void registraProveedorValido() {
        // Arrange
        Proveedor nuevo = Proveedor.builder()
                .nombre("Distribuidora Vet").rut(" 76.123.456-k ").email("ventas@distvet.cl").build();
        when(proveedorRepository.existsByRut("76.123.456-K")).thenReturn(false);
        when(proveedorRepository.save(any(Proveedor.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        Proveedor guardado = proveedorService.registrarProveedor(nuevo);

        // Assert
        assertEquals("76.123.456-K", guardado.getRut());
        verify(proveedorRepository).save(nuevo);
    }

    @Test
    @DisplayName("Rechaza un proveedor con RUT duplicado")
    void rechazaRutDuplicado() {
        // Arrange
        Proveedor nuevo = Proveedor.builder()
                .nombre("Otra").rut("76.123.456-7").email("otra@prov.cl").build();
        when(proveedorRepository.existsByRut("76.123.456-7")).thenReturn(true);

        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> proveedorService.registrarProveedor(nuevo));
        verify(proveedorRepository, never()).save(any());
    }

    @Test
    void rechazaProveedorSinNombre() {
        assertProveedorRechazado(p -> p.setNombre(" "), "El nombre del proveedor es obligatorio");
    }

    @Test
    void rechazaProveedorSinRut() {
        assertProveedorRechazado(p -> p.setRut(null), "El RUT del proveedor es obligatorio");
    }

    @Test
    void rechazaProveedorConCorreoInvalido() {
        assertProveedorRechazado(p -> p.setEmail("sin-arroba"), "Debe indicar un correo válido para el proveedor");
    }

    // ---------- Consultas y eliminación ----------

    @Test
    void obtieneYListaProveedores() {
        // Arrange
        Proveedor proveedor = proveedor(1L, "Distribuidora Vet", "76.123.456-7");
        when(proveedorRepository.findById(1L)).thenReturn(Optional.of(proveedor));
        when(proveedorRepository.findAll()).thenReturn(List.of(proveedor));

        // Act + Assert
        assertSame(proveedor, proveedorService.obtenerProveedorPorId(1L).orElseThrow());
        assertEquals(List.of(proveedor), proveedorService.listarProveedores());
    }

    @Test
    @DisplayName("Elimina un proveedor sin productos asociados")
    void eliminaProveedorSinProductos() {
        // Arrange
        when(proveedorRepository.findById(1L))
                .thenReturn(Optional.of(proveedor(1L, "Distribuidora Vet", "76.123.456-7")));
        when(productoRepository.existsByProveedorId(1L)).thenReturn(false);

        // Act
        proveedorService.eliminarProveedor(1L);

        // Assert
        verify(proveedorRepository).deleteById(1L);
    }

    @Test
    @DisplayName("No elimina un proveedor que todavía tiene productos")
    void noEliminaProveedorConProductos() {
        // Arrange
        when(proveedorRepository.findById(1L))
                .thenReturn(Optional.of(proveedor(1L, "Distribuidora Vet", "76.123.456-7")));
        when(productoRepository.existsByProveedorId(1L)).thenReturn(true);

        // Act + Assert
        assertThrows(IllegalArgumentException.class, () -> proveedorService.eliminarProveedor(1L));
        verify(proveedorRepository, never()).deleteById(any());
    }

    // ---------- Utilidades ----------

    private void assertProveedorRechazado(Consumer<Proveedor> quitarDato, String mensaje) {
        Proveedor proveedor = Proveedor.builder()
                .nombre("Distribuidora Vet").rut("76.123.456-7").email("ventas@distvet.cl").build();
        quitarDato.accept(proveedor);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> proveedorService.registrarProveedor(proveedor));
        assertEquals(mensaje, ex.getMessage());
        verifyNoInteractions(proveedorRepository);
    }

    private Proveedor proveedor(Long id, String nombre, String rut) {
        return Proveedor.builder().id(id).nombre(nombre).rut(rut).email("contacto@proveedor.cl").build();
    }

    private Producto producto(Long id, String nombre) {
        return Producto.builder().id(id).nombre(nombre).precio(new BigDecimal("1000")).stock(5).build();
    }
}

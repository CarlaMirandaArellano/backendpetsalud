package com.petsalud.backendpetsalud.service;

import com.petsalud.backendpetsalud.entity.Producto;
import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.entity.Venta;
import com.petsalud.backendpetsalud.repository.ProductoRepository;
import com.petsalud.backendpetsalud.repository.UsuarioRepository;
import com.petsalud.backendpetsalud.repository.VentaRepository;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VentaServiceImplTest {

    @Mock
    private VentaRepository ventaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private VentaServiceImpl ventaService;

    @Test
    void registraVentaCalculaTotalDescuentaStockYRelacionaUsuarioYProductos() {
        // Arrange
        Usuario usuario = usuarioCompleto();
        Producto producto1 = producto(10L, "Alimento", "1200.50", 3);
        Producto producto2 = producto(11L, "Juguete", "799.50", 2);
        List<Producto> productos = List.of(producto1, producto2);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(productoRepository.findAllById(List.of(10L, 11L))).thenReturn(productos);
        when(ventaRepository.save(any(Venta.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Venta venta = ventaService.realizarVenta(1L, List.of(10L, 11L));

        // Assert
        assertEquals(new BigDecimal("2000.00"), venta.getTotal());
        assertSame(usuario, venta.getUsuario());
        assertSame(productos, venta.getProductos());
        assertNotNull(venta.getFecha());
        assertEquals(2, producto1.getStock());
        assertEquals(1, producto2.getStock());
        verify(productoRepository).save(producto1);
        verify(productoRepository).save(producto2);
        verify(ventaRepository).save(venta);
    }

    @Test
    void noRegistraVentaNiDescuentaStockSiUnProductoEstaAgotado() {
        // Arrange
        Usuario usuario = usuarioCompleto();
        Producto disponible = producto(10L, "Alimento", "1200.00", 2);
        Producto agotado = producto(11L, "Juguete", "500.00", 0);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(productoRepository.findAllById(List.of(10L, 11L)))
                .thenReturn(List.of(disponible, agotado));

        // Act
        // Assert: assertThrows ejecuta la venta y comprueba que el stock invalido la rechaza.
        assertThrows(IllegalArgumentException.class,
                () -> ventaService.realizarVenta(1L, List.of(10L, 11L)));

        // Assert
        assertEquals(2, disponible.getStock());
        verify(productoRepository, never()).save(any(Producto.class));
        verify(ventaRepository, never()).save(any(Venta.class));
    }

    @Test
    void noRegistraVentaSiElUsuarioNoExiste() {
        // Arrange
        when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());

        // Act
        // Assert: el servicio debe rechazar la venta si el usuario no existe.
        assertThrows(IllegalArgumentException.class,
                () -> ventaService.realizarVenta(1L, List.of(10L)));

        // Assert
        verifyNoInteractions(productoRepository, ventaRepository);
    }

    @Test
    void noRegistraVentaSiElUsuarioNoTieneNombre() {
        // Arrange: el helper elimina el nombre del usuario.
        // Act
        // Assert
        assertVentaRechazadaConUsuarioIncompleto(usuario -> usuario.setNombre(null));
    }

    @Test
    void noRegistraVentaSiElUsuarioNoTieneApellido() {
        // Arrange: el helper elimina el apellido del usuario.
        // Act
        // Assert
        assertVentaRechazadaConUsuarioIncompleto(usuario -> usuario.setApellido(null));
    }

    @Test
    void noRegistraVentaSiElUsuarioNoTieneEmail() {
        // Arrange: el helper elimina el email del usuario.
        // Act
        // Assert
        assertVentaRechazadaConUsuarioIncompleto(usuario -> usuario.setEmail(null));
    }

    @Test
    void noRegistraVentaSiElUsuarioNoTieneDireccion() {
        // Arrange: el helper elimina la direccion del usuario.
        // Act
        // Assert
        assertVentaRechazadaConUsuarioIncompleto(usuario -> usuario.setDireccion(null));
    }

    @Test
    void noRegistraVentaSiFaltaUnProducto() {
        // Arrange
        Usuario usuario = usuarioCompleto();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(productoRepository.findAllById(List.of(10L, 11L)))
                .thenReturn(List.of(producto(10L, "Alimento", "1200.00", 2)));

        // Act
        // Assert: la falta de un producto debe impedir la venta.
        assertThrows(IllegalArgumentException.class,
                () -> ventaService.realizarVenta(1L, List.of(10L, 11L)));

        // Assert
        verify(productoRepository, never()).save(any(Producto.class));
        verify(ventaRepository, never()).save(any(Venta.class));
    }

    @Test
    void obtenerVentaPorIdConsultaRepositorio() {
        // Arrange
        Venta venta = Venta.builder().id(20L).build();
        when(ventaRepository.findById(20L)).thenReturn(Optional.of(venta));

        // Act
        Optional<Venta> resultado = ventaService.obtenerVentaPorId(20L);

        // Assert
        assertSame(venta, resultado.orElseThrow());
    }

    @Test
    void listarVentasDeUsuarioConsultaRepositorio() {
        // Arrange
        List<Venta> ventas = List.of(Venta.builder().id(20L).build());
        when(ventaRepository.findByUsuarioId(1L)).thenReturn(ventas);

        // Act
        List<Venta> resultado = ventaService.listarVentasPorUsuario(1L);

        // Assert
        assertSame(ventas, resultado);
    }

    @Test
    void listarTodasLasVentasConsultaRepositorio() {
        // Arrange
        List<Venta> ventas = List.of(Venta.builder().id(20L).build());
        when(ventaRepository.findAll()).thenReturn(ventas);

        // Act
        List<Venta> resultado = ventaService.listarTodasLasVentas();

        // Assert
        assertSame(ventas, resultado);
    }

    private Usuario usuarioCompleto() {
        return Usuario.builder()
                .id(1L)
                .nombre("Ana")
                .apellido("Pérez")
                .email("ana@petsalud.cl")
                .direccion("Calle 123")
                .password("hash")
                .rol(Rol.CLIENTE)
                .build();
    }

    private void assertVentaRechazadaConUsuarioIncompleto(Consumer<Usuario> quitarDato) {
        // Arrange
        Usuario usuario = usuarioCompleto();
        quitarDato.accept(usuario);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        // Act
        // Assert: la validacion debe impedir que se consulte o guarde producto/venta.
        assertThrows(IllegalArgumentException.class,
                () -> ventaService.realizarVenta(1L, List.of(10L)));

        // Assert
        verifyNoInteractions(productoRepository, ventaRepository);
    }

    private Producto producto(Long id, String nombre, String precio, Integer stock) {
        return Producto.builder()
                .id(id)
                .nombre(nombre)
                .precio(new BigDecimal(precio))
                .stock(stock)
                .build();
    }
}

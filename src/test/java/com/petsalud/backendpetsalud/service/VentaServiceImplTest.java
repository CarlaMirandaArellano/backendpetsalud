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
        Usuario usuario = usuarioCompleto();
        Producto producto1 = producto(10L, "Alimento", "1200.50", 3);
        Producto producto2 = producto(11L, "Juguete", "799.50", 2);
        List<Producto> productos = List.of(producto1, producto2);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(productoRepository.findAllById(List.of(10L, 11L))).thenReturn(productos);
        when(ventaRepository.save(any(Venta.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Venta venta = ventaService.realizarVenta(1L, List.of(10L, 11L));

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
        Usuario usuario = usuarioCompleto();
        Producto disponible = producto(10L, "Alimento", "1200.00", 2);
        Producto agotado = producto(11L, "Juguete", "500.00", 0);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(productoRepository.findAllById(List.of(10L, 11L)))
                .thenReturn(List.of(disponible, agotado));

        assertThrows(IllegalArgumentException.class,
                () -> ventaService.realizarVenta(1L, List.of(10L, 11L)));

        assertEquals(2, disponible.getStock());
        verify(productoRepository, never()).save(any(Producto.class));
        verify(ventaRepository, never()).save(any(Venta.class));
    }

    @Test
    void noRegistraVentaSiElUsuarioNoExiste() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> ventaService.realizarVenta(1L, List.of(10L)));

        verifyNoInteractions(productoRepository, ventaRepository);
    }

    @Test
    void noRegistraVentaSiElUsuarioNoTieneNombre() {
        assertVentaRechazadaConUsuarioIncompleto(usuario -> usuario.setNombre(null));
    }

    @Test
    void noRegistraVentaSiElUsuarioNoTieneApellido() {
        assertVentaRechazadaConUsuarioIncompleto(usuario -> usuario.setApellido(null));
    }

    @Test
    void noRegistraVentaSiElUsuarioNoTieneEmail() {
        assertVentaRechazadaConUsuarioIncompleto(usuario -> usuario.setEmail(null));
    }

    @Test
    void noRegistraVentaSiElUsuarioNoTieneDireccion() {
        assertVentaRechazadaConUsuarioIncompleto(usuario -> usuario.setDireccion(null));
    }

    @Test
    void noRegistraVentaSiFaltaUnProducto() {
        Usuario usuario = usuarioCompleto();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(productoRepository.findAllById(List.of(10L, 11L)))
                .thenReturn(List.of(producto(10L, "Alimento", "1200.00", 2)));

        assertThrows(IllegalArgumentException.class,
                () -> ventaService.realizarVenta(1L, List.of(10L, 11L)));

        verify(productoRepository, never()).save(any(Producto.class));
        verify(ventaRepository, never()).save(any(Venta.class));
    }

    @Test
    void obtenerVentaPorIdConsultaRepositorio() {
        Venta venta = Venta.builder().id(20L).build();
        when(ventaRepository.findById(20L)).thenReturn(Optional.of(venta));

        assertSame(venta, ventaService.obtenerVentaPorId(20L).orElseThrow());
    }

    @Test
    void listarVentasDeUsuarioConsultaRepositorio() {
        List<Venta> ventas = List.of(Venta.builder().id(20L).build());
        when(ventaRepository.findByUsuarioId(1L)).thenReturn(ventas);

        assertSame(ventas, ventaService.listarVentasPorUsuario(1L));
    }

    @Test
    void listarTodasLasVentasConsultaRepositorio() {
        List<Venta> ventas = List.of(Venta.builder().id(20L).build());
        when(ventaRepository.findAll()).thenReturn(ventas);

        assertSame(ventas, ventaService.listarTodasLasVentas());
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
        Usuario usuario = usuarioCompleto();
        quitarDato.accept(usuario);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        assertThrows(IllegalArgumentException.class,
                () -> ventaService.realizarVenta(1L, List.of(10L)));

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

package com.petsalud.backendpetsalud.security;

import com.petsalud.backendpetsalud.controller.ProductoController;
import com.petsalud.backendpetsalud.controller.ExpedienteMedicoController;
import com.petsalud.backendpetsalud.controller.ProveedorController;
import com.petsalud.backendpetsalud.controller.VentaController;
import com.petsalud.backendpetsalud.entity.ExpedienteMedico;
import com.petsalud.backendpetsalud.entity.Producto;
import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.entity.Venta;
import com.petsalud.backendpetsalud.service.ProductoService;
import com.petsalud.backendpetsalud.service.ExpedienteMedicoService;
import com.petsalud.backendpetsalud.service.ProveedorService;
import com.petsalud.backendpetsalud.service.VentaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de acceso de punta a punta sobre la cadena de seguridad:
 * token JWT -> filtro -> reglas de SecurityConfig y @PreAuthorize -> 200 / 401 / 403.
 */
@WebMvcTest({ProductoController.class, VentaController.class, ExpedienteMedicoController.class,
        ProveedorController.class})
@Import({SecurityConfig.class, JwtUtil.class, JwtAuthenticationFilter.class,
        JsonAuthenticationEntryPoint.class, JsonAccessDeniedHandler.class})
@TestPropertySource(properties = {
        "jwt.secret=Y2xhdmUtZGUtcHJ1ZWJhLWRlLTMyLWJ5dGVzLW1pbiE=",
        "jwt.expiration-ms=60000"
})
class AutorizacionPorRolTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockBean
    private UsuarioDetailsService usuarioDetailsService;

    @MockBean
    private ProductoService productoService;

    @MockBean
    private VentaService ventaService;

    @MockBean
    private ExpedienteMedicoService expedienteMedicoService;

    @MockBean
    private ProveedorService proveedorService;

    @MockBean
    private ResourceAccessGuard accessGuard;

    private static final String PRODUCTO_JSON =
            "{\"nombre\":\"Alimento\",\"precio\":1000,\"stock\":5,\"categoria\":\"Alimentos\"}";
    private static final String PROVEEDOR_JSON =
            "{\"nombre\":\"Proveedor Central\",\"rut\":\"12.345.678-9\",\"email\":\"contacto@proveedor.cl\"}";

    @BeforeEach
    void setUp() {
        registrar("cliente@petsalud.cl", Rol.CLIENTE);
        registrar("empleado@petsalud.cl", Rol.EMPLEADO);
        registrar("gerente@petsalud.cl", Rol.GERENTE);
        when(productoService.listarProductos()).thenReturn(List.of());
        when(productoService.listarProductosConStockBajo(any())).thenReturn(List.of());
        when(productoService.agregarProducto(any())).thenReturn(Producto.builder().id(1L).nombre("Alimento").build());
        when(ventaService.realizarVenta(7L, List.of(1L)))
                .thenReturn(Venta.builder().id(1L).build());
    }

    private void registrar(String email, Rol rol) {
        when(usuarioDetailsService.loadUserByUsername(email)).thenReturn(
                new UsuarioDetails(Usuario.builder().email(email).password("hash").rol(rol).build()));
    }

    private String bearer(String email) {
        return "Bearer " + jwtUtil.generarToken(usuarioDetailsService.loadUserByUsername(email));
    }

    @Test
    void sinTokenResponde401() throws Exception {
        // Arrange
        var solicitud = get("/api/productos");

        // Act
        var respuesta = mockMvc.perform(solicitud);

        // Assert
        respuesta.andExpect(status().isUnauthorized());
    }

    @Test
    void tokenInvalidoResponde401() throws Exception {
        // Arrange
        var solicitud = get("/api/productos").header("Authorization", "Bearer token.invalido.123");

        // Act
        var respuesta = mockMvc.perform(solicitud);

        // Assert
        respuesta.andExpect(status().isUnauthorized());
    }

    @Test
    void clientePuedeConsultarCatalogo() throws Exception {
        // Arrange
        var solicitud = get("/api/productos")
                .header("Authorization", bearer("cliente@petsalud.cl"));

        // Act
        var respuesta = mockMvc.perform(solicitud);

        // Assert
        respuesta.andExpect(status().isOk());
    }

    @Test
    void clienteNoPuedeCrearProductos() throws Exception {
        // Arrange
        var solicitud = post("/api/productos")
                        .header("Authorization", bearer("cliente@petsalud.cl"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUCTO_JSON);

        // Act
        var respuesta = mockMvc.perform(solicitud);

        // Assert
        respuesta.andExpect(status().isForbidden());
    }

    @Test
    void empleadoNoPuedeCrearProductos() throws Exception {
        // Arrange
        var solicitud = post("/api/productos")
                        .header("Authorization", bearer("empleado@petsalud.cl"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUCTO_JSON);

        // Act
        var respuesta = mockMvc.perform(solicitud);

        // Assert
        respuesta.andExpect(status().isForbidden());
    }

    @Test
    void gerentePuedeCrearProductos() throws Exception {
        // Arrange
        var solicitud = post("/api/productos")
                        .header("Authorization", bearer("gerente@petsalud.cl"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUCTO_JSON);

        // Act
        var respuesta = mockMvc.perform(solicitud);

        // Assert
        respuesta.andExpect(status().isOk());
    }

    @Test
    void stockBajoSoloParaEmpleadoYGerente() throws Exception {
        // Arrange
        var solicitudCliente = get("/api/productos/stock-bajo").param("cantidad", "5")
                .header("Authorization", bearer("cliente@petsalud.cl"));
        var solicitudEmpleado = get("/api/productos/stock-bajo").param("cantidad", "5")
                .header("Authorization", bearer("empleado@petsalud.cl"));

        // Act
        var respuestaCliente = mockMvc.perform(solicitudCliente);
        var respuestaEmpleado = mockMvc.perform(solicitudEmpleado);

        // Assert
        respuestaCliente.andExpect(status().isForbidden());
        respuestaEmpleado.andExpect(status().isOk());
    }

    @Test
    void gerentePuedeGenerarVentas() throws Exception {
        // Arrange
        var solicitud = post("/api/ventas").param("usuarioId", "7")
                        .header("Authorization", bearer("gerente@petsalud.cl"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1]");

        // Act
        var respuesta = mockMvc.perform(solicitud);

        // Assert
        respuesta.andExpect(status().isOk());
        verify(ventaService).realizarVenta(7L, List.of(1L));
    }

    @Test
    void empleadoPuedeGenerarVentas() throws Exception {
        // Arrange
        var solicitud = post("/api/ventas").param("usuarioId", "7")
                        .header("Authorization", bearer("empleado@petsalud.cl"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1]");

        // Act
        var respuesta = mockMvc.perform(solicitud);

        // Assert
        respuesta.andExpect(status().isOk());
        verify(ventaService).realizarVenta(7L, List.of(1L));
    }

    @Test
    void clienteNoPuedeGenerarVentas() throws Exception {
        // Arrange
        var solicitud = post("/api/ventas").param("usuarioId", "7")
                        .header("Authorization", bearer("cliente@petsalud.cl"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1]");

        // Act
        var respuesta = mockMvc.perform(solicitud);

        // Assert
        respuesta.andExpect(status().isForbidden());
    }

    @Test
    void empleadoPuedeConsultarExpedienteMedico() throws Exception {
        // Arrange
        when(expedienteMedicoService.obtenerRegistroPorId(1L))
                .thenReturn(Optional.of(ExpedienteMedico.builder().id(1L).build()));
        var solicitud = get("/api/expedientes/1")
                .header("Authorization", bearer("empleado@petsalud.cl"));

        // Act
        var respuesta = mockMvc.perform(solicitud);

        // Assert
        respuesta.andExpect(status().isOk());
        verify(expedienteMedicoService).obtenerRegistroPorId(1L);
    }

    @Test
    void clienteNoPuedeConsultarExpedienteMedico() throws Exception {
        // Arrange
        var solicitud = get("/api/expedientes/1")
                .header("Authorization", bearer("cliente@petsalud.cl"));

        // Act
        var respuesta = mockMvc.perform(solicitud);

        // Assert
        respuesta.andExpect(status().isForbidden());
        verifyNoInteractions(expedienteMedicoService);
    }

    @Test
    void clienteNoPuedeCrearProveedores() throws Exception {
        // Arrange
        var solicitud = post("/api/proveedores")
                .header("Authorization", bearer("cliente@petsalud.cl"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(PROVEEDOR_JSON);

        // Act
        var respuesta = mockMvc.perform(solicitud);

        // Assert
        respuesta.andExpect(status().isForbidden());
        verifyNoInteractions(proveedorService);
    }
}

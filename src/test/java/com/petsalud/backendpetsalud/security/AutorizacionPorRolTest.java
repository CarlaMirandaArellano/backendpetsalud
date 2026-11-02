package com.petsalud.backendpetsalud.security;

import com.petsalud.backendpetsalud.controller.ProductoController;
import com.petsalud.backendpetsalud.entity.Producto;
import com.petsalud.backendpetsalud.entity.Rol;
import com.petsalud.backendpetsalud.entity.Usuario;
import com.petsalud.backendpetsalud.service.ProductoService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de acceso de punta a punta sobre la cadena de seguridad:
 * token JWT -> filtro -> reglas de SecurityConfig y @PreAuthorize -> 200 / 401 / 403.
 */
@WebMvcTest(ProductoController.class)
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

    private static final String PRODUCTO_JSON =
            "{\"nombre\":\"Alimento\",\"precio\":1000,\"stock\":5,\"categoria\":\"Alimentos\"}";

    @BeforeEach
    void setUp() {
        registrar("cliente@petsalud.cl", Rol.CLIENTE);
        registrar("empleado@petsalud.cl", Rol.EMPLEADO);
        registrar("gerente@petsalud.cl", Rol.GERENTE);
        when(productoService.listarProductos()).thenReturn(List.of());
        when(productoService.listarProductosConStockBajo(any())).thenReturn(List.of());
        when(productoService.agregarProducto(any())).thenReturn(Producto.builder().id(1L).nombre("Alimento").build());
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
        mockMvc.perform(get("/api/productos")).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenInvalidoResponde401() throws Exception {
        mockMvc.perform(get("/api/productos").header("Authorization", "Bearer token.invalido.123"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void clientePuedeConsultarCatalogo() throws Exception {
        mockMvc.perform(get("/api/productos").header("Authorization", bearer("cliente@petsalud.cl")))
                .andExpect(status().isOk());
    }

    @Test
    void clienteNoPuedeCrearProductos() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .header("Authorization", bearer("cliente@petsalud.cl"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUCTO_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void empleadoNoPuedeCrearProductos() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .header("Authorization", bearer("empleado@petsalud.cl"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUCTO_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void gerentePuedeCrearProductos() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .header("Authorization", bearer("gerente@petsalud.cl"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PRODUCTO_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void stockBajoSoloParaEmpleadoYGerente() throws Exception {
        mockMvc.perform(get("/api/productos/stock-bajo").param("cantidad", "5")
                        .header("Authorization", bearer("cliente@petsalud.cl")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/productos/stock-bajo").param("cantidad", "5")
                        .header("Authorization", bearer("empleado@petsalud.cl")))
                .andExpect(status().isOk());
    }
}

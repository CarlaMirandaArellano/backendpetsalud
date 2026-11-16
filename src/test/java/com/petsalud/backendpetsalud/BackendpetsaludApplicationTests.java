package com.petsalud.backendpetsalud;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** Prueba de arranque completo: requiere MySQL en ejecución y la variable JWT_SECRET (ver README). */
@Disabled("Requiere MySQL en ejecución y la variable JWT_SECRET; ver README")
@SpringBootTest
class BackendpetsaludApplicationTests {

	@Test
	void contextLoads() {
		// Arrange: @SpringBootTest configura el contexto de la aplicacion.
		// Act: Spring intenta iniciar el contexto antes de ejecutar este metodo.
		// Assert: la prueba pasa si la inicializacion no lanza una excepcion.
	}

}

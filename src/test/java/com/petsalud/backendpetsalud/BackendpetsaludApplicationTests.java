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
	}

}

package com.petsalud.backendpetsalud;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** Prueba de arranque completo con una base H2 aislada para tests. */
@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:backendpetsalud_context_test;DB_CLOSE_DELAY=-1",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
		"jwt.secret=Y2xhdmUtZGUtcHJ1ZWJhLWRlLTMyLWJ5dGVzLW1pbiE="
})
class BackendpetsaludApplicationTests {

	@Test
	void contextLoads() {
		// Arrange: @SpringBootTest configura el contexto de la aplicacion.
		// Act: Spring intenta iniciar el contexto antes de ejecutar este metodo.
		// Assert: la prueba pasa si la inicializacion no lanza una excepcion.
	}

}

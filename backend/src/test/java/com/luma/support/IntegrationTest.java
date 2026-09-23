package com.luma.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.mysql.MySQLContainer;

/**
 * Base de las pruebas que necesitan la aplicacion completa y MySQL real.
 *
 * <p>El contenedor es UNO por JVM, compartido por todas las clases que heredan
 * de aqui. Levantar un MySQL por clase multiplicaria por diez el tiempo de la
 * suite sin ganar aislamiento real: Flyway reconstruye el esquema una vez y las
 * pruebas se encargan de crear sus propios datos.
 *
 * <p>Se arranca a mano en un bloque estatico en lugar de con {@code @Container}
 * para no depender del modulo de JUnit Jupiter de Testcontainers. Ryuk, su
 * contenedor centinela, lo apaga cuando termina la JVM.
 *
 * <p>Como todas las subclases comparten la misma configuracion, Spring reutiliza
 * el mismo contexto entre ellas y solo lo levanta una vez.
 *
 * <p><b>Requiere Docker.</b> Si Maven corre dentro de un contenedor, ese
 * contenedor necesita el socket de Docker montado y
 * {@code TESTCONTAINERS_HOST_OVERRIDE}. Ver {@code scripts/verify.ps1}.
 */
@SpringBootTest
public abstract class IntegrationTest {

    /** El tipo va sin parametrizar: en Testcontainers 2.x dejo de ser generico. */
    protected static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4");

    static {
        // Los parametros se fijan por separado y no encadenados: con el tipo sin
        // parametrizar, encadenar devolveria el tipo base y no compilaria.
        MYSQL.withUrlParam("allowPublicKeyRetrieval", "true");
        MYSQL.withUrlParam("useSSL", "false");
        MYSQL.withUrlParam("serverTimezone", "UTC");
        MYSQL.start();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }
}

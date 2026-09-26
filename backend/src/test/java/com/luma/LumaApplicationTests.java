package com.luma;

import static org.assertj.core.api.Assertions.assertThat;

import com.luma.support.IntegrationTest;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * La prueba que levanta la aplicacion completa contra un MySQL real.
 *
 * <p>Es la red de seguridad que las pruebas de dominio no pueden dar. Esas son
 * puras y rapidas, pero no tocan Spring ni la base: no detectan un bean mal
 * cableado, una columna que no coincide con su entidad, ni una migracion que no
 * aplica. Esos son justamente los fallos que en este proyecto llegaron hasta
 * Swagger: los dos {@code CorsConfigurationSource} de la Fase 1, el Flyway que
 * no arrancaba, y los tipos de columna de la Fase 4b.
 *
 * <p>Se usa MySQL real y no una base en memoria a proposito. El SQL de
 * produccion corre sobre MySQL; probarlo contra H2 verificaria un motor que
 * nadie va a usar.
 *
 * <p>El contenedor y la configuracion viven en {@link IntegrationTest}.
 */
@DisplayName("La aplicacion completa")
class LumaApplicationTests extends IntegrationTest {

    @Autowired
    ApplicationContext context;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    @DisplayName("arranca y cablea todos sus beans")
    void elContextoCarga() {
        // Que este test llegue a ejecutarse ya significa que el contexto cargo.
        // La asercion existe para que el fallo diga algo si algun dia cambia la
        // forma de arrancar.
        assertThat(context).isNotNull();
        assertThat(context.getBeanDefinitionCount()).isPositive();
    }

    @Test
    @DisplayName("aplica todas las migraciones de Flyway")
    void flywayMigraDesdeCero() {
        List<String> versiones = jdbc.queryForList(
                """
                SELECT version FROM flyway_schema_history
                WHERE success = 1 AND version IS NOT NULL
                ORDER BY installed_rank
                """,
                String.class);

        assertThat(versiones).containsExactly("1", "2", "3", "4");
    }

    @Test
    @DisplayName("pasa la validacion de esquema de Hibernate")
    void elEsquemaCoincideConLasEntidades() {
        // Con ddl-auto: validate, una entidad que no cuadre con su tabla impide
        // que el contexto arranque. Si llegamos aqui, las entidades cuadran.
        // Esta asercion lo deja explicito en el reporte de pruebas.
        Integer tablas = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE()",
                Integer.class);

        assertThat(tablas).isGreaterThanOrEqualTo(10);
    }

    @Test
    @DisplayName("siembra el catalogo de categorias del sistema")
    void elCatalogoDeCategoriasQuedaSembrado() {
        Integer categorias = jdbc.queryForObject(
                "SELECT COUNT(*) FROM expense_categories WHERE is_system = TRUE AND user_id IS NULL",
                Integer.class);

        assertThat(categorias).isEqualTo(17);
    }

    @Test
    @DisplayName("no deja ninguna tabla sin llave primaria")
    void todasLasTablasTienenLlavePrimaria() {
        // Una tabla sin llave primaria rompe la replicacion y hace imposible
        // actualizar una fila con seguridad. Es barato verificarlo una vez.
        List<String> sinLlave = jdbc.queryForList(
                """
                SELECT t.table_name
                FROM information_schema.tables t
                LEFT JOIN information_schema.table_constraints c
                       ON c.table_schema = t.table_schema
                      AND c.table_name = t.table_name
                      AND c.constraint_type = 'PRIMARY KEY'
                WHERE t.table_schema = DATABASE()
                  AND t.table_type = 'BASE TABLE'
                  AND c.constraint_name IS NULL
                """,
                String.class);

        assertThat(sinLlave).isEmpty();
    }
}

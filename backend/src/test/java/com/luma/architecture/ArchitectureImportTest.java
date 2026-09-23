package com.luma.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verifica que ArchUnit puede leer el bytecode del proyecto.
 *
 * <p>Parece una perogrullada y no lo es. Durante toda la Fase 1 a la 5, ArchUnit
 * corrio contra cero clases: su version traia un ASM que no entiende el formato
 * de Java 25 y fallaba al importar las 114, una por una, escribiendo un WARN que
 * nadie leia. Como cada regla lleva {@code allowEmptyShould(true)} —necesario al
 * principio, cuando habia paquetes vacios—, las ocho pasaban sin evaluar nada.
 *
 * <p>Ocho pruebas verdes que no verificaban una sola linea. Esta clase existe
 * para que eso no pueda repetirse en silencio: si el importador se rompe otra
 * vez, o si alguien sube la version del JDK antes que la de ArchUnit, falla aqui
 * con un mensaje claro en vez de degradarse a nada.
 */
@DisplayName("El importador de ArchUnit")
class ArchitectureImportTest {

    /**
     * Piso deliberadamente holgado. No interesa el numero exacto —cambia con
     * cada clase nueva— sino distinguir "leyo el proyecto" de "leyo cero".
     */
    private static final int MINIMO_DE_CLASES = 80;

    @Test
    @DisplayName("lee el bytecode del proyecto y no se queda en cero")
    void leeElBytecodeDelProyecto() {
        JavaClasses clases = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("com.luma");

        assertThat(clases.size())
                .as("ArchUnit importo %d clases. Si es cero o casi, sus reglas "
                        + "estan pasando vacias: revisa que la version de ArchUnit "
                        + "soporte el bytecode del JDK con el que compilas.",
                        clases.size())
                .isGreaterThan(MINIMO_DE_CLASES);
    }
}

package com.luma.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.GeneralCodingRules;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

/**
 * Las reglas de arquitectura se erosionan solas si nadie las verifica.
 * Estos tests las hacen cumplir en cada build.
 *
 * <p>Todas llevan {@code allowEmptyShould(true)} porque en las primeras fases hay
 * paquetes que todavia no existen: una regla sin clases que evaluar no debe
 * romper el build.
 */
@AnalyzeClasses(packages = "com.luma", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    /**
     * Riesgo R2 del analisis: el dinero nunca se representa con punto flotante.
     * Un solo {@code double} en una entidad basta para producir cifras que no
     * cuadran, y ese error es casi imposible de encontrar despues.
     */
    @ArchTest
    static final ArchRule elDineroNuncaUsaPuntoFlotante = noFields()
            .that()
            .areDeclaredInClassesThat()
            .resideInAPackage("com.luma..")
            .should()
            .haveRawType(double.class)
            .orShould()
            .haveRawType(float.class)
            .orShould()
            .haveRawType(Double.class)
            .orShould()
            .haveRawType(Float.class)
            .because("los importes se representan con BigDecimal; ver common/model/Money")
            .allowEmptyShould(true);

    /**
     * El dominio no conoce el framework web ni la serializacion: nada de Spring,
     * de servlets ni de Jackson.
     *
     * <p>Las anotaciones de JPA si se permiten. Separar la entidad de persistencia
     * del modelo de dominio duplicaria cada clase y su mapeo sin beneficio real a
     * esta escala. La pieza que de verdad debe ser pura es el motor presupuestal,
     * y esa recibe un snapshot inmutable, no entidades.
     */
    @ArchTest
    static final ArchRule elDominioNoDependeDelFramework = noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "org.springframework..",
                    "jakarta.servlet..",
                    "com.fasterxml.jackson..",
                    "tools.jackson..")
            .because("el dominio no conoce infraestructura")
            .allowEmptyShould(true);

    /** La dependencia entre capas va en un solo sentido: api -> application -> domain. */
    @ArchTest
    static final ArchRule laInfraestructuraNoDependeDeLaApi = noClasses()
            .that()
            .resideInAPackage("..infrastructure..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..api..")
            .allowEmptyShould(true);

    /** El dominio tampoco mira hacia afuera. */
    @ArchTest
    static final ArchRule elDominioNoDependeDeLaApiNiDeInfraestructura = noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..api..", "..infrastructure..")
            .allowEmptyShould(true);

    /** Los controladores viven en paquetes {@code api}, para que la capa sea evidente. */
    @ArchTest
    static final ArchRule losControladoresVivenEnApi = classes()
            .that()
            .areAnnotatedWith(RestController.class)
            .should()
            .resideInAPackage("..api..")
            .allowEmptyShould(true);

    /**
     * Inyeccion por constructor, no por campo: hace explicitas las dependencias y
     * permite construir la clase en un test sin levantar Spring.
     */
    @ArchTest
    static final ArchRule sinInyeccionPorCampo = noFields()
            .should()
            .beAnnotatedWith(Autowired.class)
            .because("se usa inyeccion por constructor")
            .allowEmptyShould(true);

    /** Nada de imprimir a consola: todo pasa por el logger, que lleva el traceId. */
    @ArchTest
    static final ArchRule sinSystemOut =
            GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS.allowEmptyShould(true);

    /** Los campos publicos mutables rompen el encapsulamiento. */
    @ArchTest
    static final ArchRule sinCamposPublicosMutables = fields()
            .that()
            .areDeclaredInClassesThat()
            .resideInAPackage("com.luma..")
            .and()
            .arePublic()
            .and()
            .areNotStatic()
            .should()
            .beFinal()
            .allowEmptyShould(true);
}

package de.householdmanager.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

/**
 * Die Architekturregeln des Backends als ausführbare Tests.
 *
 * <p>Quelle der Wahrheit ist {@code AGENTS.md §Backend} (und die Skills
 * {@code backend-ports-adapters} sowie {@code code-review-gate/references/pruefgrundlage.md}).
 * Diese Klasse ist die <em>ausführbare</em> Fassung — wenn eine Regel hier
 * geändert wird, muss die zugehörige Prosa-Regel mitgezogen werden (der
 * Architektur-Reviewer prüft das).
 *
 * <p>Ändert sich eine Regel, ändert sich diese Datei mit. Deshalb sind die
 * {@code because}-Texte Teil der Regeln.
 *
 * <p>Die Regeln gelten für den **Produktionscode** ({@code DoNotIncludeTests}):
 * Testklassen im Fachkern (z. B. {@code WasteCalendarStaleTest} im Paket
 * {@code domain.model}) benutzen AssertJ und JUnit — das ist kein Verstoß gegen
 * die Schichtregel, sondern normale Testinfrastruktur.
 */
@AnalyzeClasses(packages = "de.householdmanager", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureRulesTest {

    @ArchTest
    static final ArchRule fachkern_kennt_nur_java_und_lombok =
            noClasses()
                    .that()
                    .resideInAPackage("..domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideOutsideOfPackages("java..", "javax..", "lombok..", "..domain..")
                    .because("der Fachkern kennt keine Frameworks (AGENTS.md §Backend)");

    @ArchTest
    static final ArchRule fachkern_kennt_die_anwendung_nicht =
            noClasses()
                    .that()
                    .resideInAPackage("..domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..application..")
                    .because("die Abhängigkeitsrichtung zeigt nach innen: domain kennt niemanden");

    @ArchTest
    static final ArchRule anwendung_kennt_keine_adapter_und_keine_technik =
            noClasses()
                    .that()
                    .resideInAPackage("..application..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "..adapters..",
                            "jakarta.persistence..",
                            "jakarta.validation..",
                            "org.springframework.web..",
                            "org.springframework.data..",
                            "com.fasterxml..",
                            "tools.jackson..")
                    .because("die Anwendung kennt nur domain und ihre eigenen Ports");

    @ArchTest
    static final ArchRule transaktionen_liegen_an_der_use_case =
            classes()
                    .that()
                    .areAnnotatedWith(Transactional.class)
                    .should()
                    .resideInAPackage("..application.usecase..")
                    .because("Transaktionen gehören an die Use-Case-Grenze, nicht in den Adapter")
                    // Ohne Produktionscode (z. B. im Architektur-Test selbst)
                    // hat die Regel nichts zu prüfen — das ist kein Verstoß.
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule persistenz_entities_liegen_im_persistence_adapter =
            classes()
                    .that()
                    .areAnnotatedWith(Entity.class)
                    .should()
                    .resideInAPackage("..adapters.outbound.persistence..")
                    .because("JPA-Entities sind Technik des ausgehenden Adapters");

    @ArchTest
    static final ArchRule controller_liegen_im_rest_adapter =
            classes()
                    .that()
                    .areAnnotatedWith(RestController.class)
                    .should()
                    .resideInAPackage("..adapters.inbound.rest..")
                    .because("HTTP ist ein eingehender Adapter");

    @ArchTest
    static final ArchRule dtos_sind_records =
            classes()
                    .that()
                    .resideInAPackage("..adapters.inbound.rest.dto..")
                    .should()
                    .beRecords()
                    .because("DTOs sind Records in adapters/inbound/rest/dto/");

    @ArchTest
    static final ArchRule ports_sind_interfaces =
            classes()
                    .that()
                    .resideInAPackage("..application.port..")
                    .and()
                    .areTopLevelClasses()
                    .should()
                    .beInterfaces()
                    .because("ein Port ist ein Interface, das der Kern definiert und der Adapter erfüllt");

    @ArchTest
    static final ArchRule keine_feldinjektion =
            noFields()
                    .should()
                    .beAnnotatedWith(Autowired.class)
                    .because("Verdrahtung läuft über Konstruktor-Injektion und den Composition Root");
}

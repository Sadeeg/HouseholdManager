package de.householdmanager.adapters.inbound.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Struktur-Guard für die DTO-Regel (Issue #5, Skill
 * {@code backend-ports-adapters}, Regel 10): Request- und Response-DTOs sind
 * eigenständige {@code record}s in {@code adapters/inbound/rest/dto/} — kein
 * DTO als verschachteltes Record im Controller, kein DTO im Controller-Ordner
 * ohne {@code dto/}.
 *
 * <p>Die Typen werden bewusst <em>nicht</em> importiert, sondern aus dem
 * Classpath gelesen: so fällt ein zurückverschobenes Record als Testfehler
 * auf und nicht als blinder Compile-Fehler. Belegt: AC1 (das DTO liegt als
 * eigenständiges Record im DTO-Ordner) und AC3 (der Controller enthält keine
 * DTO-Definition mehr).
 */
class DtoStructureTest {

    private static final String REST_PACKAGE = "de.householdmanager.adapters.inbound.rest";
    private static final String DTO_PACKAGE = "de.householdmanager.adapters.inbound.rest.dto";

    @Test
    void noControllerDeclaresANestedDtoRecord() {
        for (Class<?> controller : productionClassesIn(REST_PACKAGE)) {
            assertThat(controller.getDeclaredClasses())
                    .as("%s darf kein verschachteltes Record enthalten — DTOs gehören in %s",
                            controller.getName(), DTO_PACKAGE)
                    .noneMatch(Class::isRecord);
            assertThat(controller)
                    .as("%s darf selbst kein Record sein — DTOs gehören in %s", controller.getName(), DTO_PACKAGE)
                    .isNotRecord();
        }
    }

    @Test
    void movedDtosAreTopLevelRecordsInTheDtoPackage() {
        for (String simpleName : List.of("UserRefResponse", "CurrentUserResponse")) {
            Class<?> dto = productionClassNamed(simpleName, DTO_PACKAGE);

            assertThat(dto).as("%s muss im DTO-Ordner liegen", simpleName).isNotNull();
            assertThat(dto.isRecord()).as("%s muss ein Record sein", simpleName).isTrue();
            assertThat(dto.getEnclosingClass())
                    .as("%s muss eigenständig sein — nicht verschachtelt", simpleName)
                    .isNull();
            assertThat(dto.getPackageName()).as("%s muss im DTO-Ordner liegen", simpleName).isEqualTo(DTO_PACKAGE);
        }
    }

    @Test
    void controllersReturnTheRecordsFromTheDtoPackage() throws Exception {
        Class<?> searchResponse = elementTypeOf(UserSearchController.class.getMethod("search", String.class));
        Class<?> currentUserResponse = AuthController.class.getMethod("currentUser").getReturnType();

        assertThat(searchResponse.getName()).isEqualTo(DTO_PACKAGE + ".UserRefResponse");
        assertThat(currentUserResponse.getName()).isEqualTo(DTO_PACKAGE + ".CurrentUserResponse");
        assertThat(searchResponse.getEnclosingClass()).isNull();
        assertThat(currentUserResponse.getEnclosingClass()).isNull();
    }

    /** Die Produktionsklassen eines Pakets — ohne Testklassen, ohne Unterpakete. */
    private static List<Class<?>> productionClassesIn(String packageName) {
        String productionClasses = codeSourceOf(AuthController.class);
        List<Class<?>> types = new ArrayList<>();

        Enumeration<URL> resources = resourcesOf(packageName);
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            if (!"file".equals(resource.getProtocol())) {
                continue; // Klassen aus einem Jar: der Ordner ist nicht auflistbar
            }
            try (Stream<Path> files = Files.list(Path.of(resource.toURI()))) {
                for (Path file : files.filter(candidate -> candidate.getFileName().toString().endsWith(".class")).toList()) {
                    String simpleName = file.getFileName().toString().replace(".class", "");
                    Class<?> type = load(packageName + "." + simpleName);
                    if (productionClasses.equals(codeSourceOf(type))) {
                        types.add(type);
                    }
                }
            } catch (IOException | URISyntaxException exception) {
                throw new AssertionError("Paket " + packageName + " nicht lesbar", exception);
            }
        }

        assertThat(types).as("Produktionsklassen im Paket %s gefunden", packageName).isNotEmpty();
        return types;
    }

    /** Die Klasse mit diesem einfachen Namen im Paket — ohne Import, damit der Test nicht tautolog ist. */
    private static Class<?> productionClassNamed(String simpleName, String packageName) {
        return productionClassesIn(packageName).stream()
                .filter(type -> type.getSimpleName().equals(simpleName))
                .findFirst()
                .orElse(null);
    }

    private static Enumeration<URL> resourcesOf(String packageName) {
        try {
            return Thread.currentThread().getContextClassLoader()
                    .getResources(packageName.replace('.', '/'));
        } catch (IOException exception) {
            throw new AssertionError("Paket " + packageName + " nicht auffindbar", exception);
        }
    }

    private static String codeSourceOf(Class<?> type) {
        return type.getProtectionDomain().getCodeSource() == null
                ? ""
                : type.getProtectionDomain().getCodeSource().getLocation().toString();
    }

    private static Class<?> elementTypeOf(Method method) {
        Type returnType = method.getGenericReturnType();
        if (returnType instanceof ParameterizedType parameterized) {
            Type argument = parameterized.getActualTypeArguments()[0];
            if (argument instanceof Class<?> elementType) {
                return elementType;
            }
        }
        throw new AssertionError("Unerwarteter Rückgabetyp: " + returnType);
    }

    private static Class<?> load(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException exception) {
            throw new AssertionError("Klasse nicht ladbar: " + name, exception);
        }
    }
}

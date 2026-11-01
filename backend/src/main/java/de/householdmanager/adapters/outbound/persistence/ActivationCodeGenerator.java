package de.householdmanager.adapters.outbound.persistence;

import java.security.SecureRandom;
import java.util.function.Predicate;
import org.springframework.stereotype.Component;

/**
 * Erzeugt die Freischalt-Codes der Haushalte.
 *
 * <p>Der Code besteht aus Ziffern und ist nicht vorhersagbar — deshalb kommt
 * die Zufallsquelle aus {@link SecureRandom} und nicht aus {@code Random}.
 * Sechs Stellen sind kurz genug zum Abtippen im Chat.
 *
 * <p>Der Code ist über alle Haushalte eindeutig. Deshalb fragt
 * {@link #nextUnused(Predicate)} nach, ob der gezogene Code schon vergeben
 * ist, und zieht bei einer Kollision einen neuen — ein zufälliger Treffer
 * darf kein Fehler des Servers sein.
 */
@Component
public class ActivationCodeGenerator {

    /** Länge des Codes: sechs Ziffern — genug für einen Haushalt, kurz genug zum Abtippen. */
    private static final int CODE_LENGTH = 6;

    /** Die Zufallsquelle des Codes — kryptografisch, nicht vorhersagbar. */
    private final SecureRandom random = new SecureRandom();

    /**
     * Ein Code, den noch kein Haushalt trägt.
     *
     * @param isTaken prüft, ob ein Code bereits vergeben ist.
     * @return der freie Code.
     * @throws IllegalStateException, wenn auch nach mehreren Versuchen kein
     *     freier Code gefunden wurde — das ist ein Extremfall, kein Normalfall.
     */
    public String nextUnused(Predicate<String> isTaken) {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String code = next();
            if (!isTaken.test(code)) {
                return code;
            }
        }
        throw new IllegalStateException(
                "Es wurde kein freier Freischalt-Code gefunden — bitte erneut versuchen.");
    }

    /** Ein Code aus Ziffern — nicht vorhersagbar. */
    public String next() {
        StringBuilder digits = new StringBuilder();
        for (int position = 0; position < CODE_LENGTH; position++) {
            digits.append(random.nextInt(10));
        }
        return digits.toString();
    }

    /**
     * Wie oft ein belegter Code neu gezogen wird, bevor die Erzeugung
     * aufgegeben wird. Bei sechs Ziffern ist eine Kollision sehr selten — die
     * Grenze schützt nur vor dem Extremfall.
     */
    private static final int MAX_ATTEMPTS = 10;
}

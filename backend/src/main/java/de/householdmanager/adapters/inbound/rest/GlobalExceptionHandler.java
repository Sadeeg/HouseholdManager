package de.householdmanager.adapters.inbound.rest;

import de.householdmanager.domain.model.HouseholdNotFoundException;
import de.householdmanager.domain.model.InvalidHouseholdNameException;
import de.householdmanager.domain.model.InvalidTaskTitleException;
import de.householdmanager.domain.model.InvalidWasteCalendarUrlException;
import de.householdmanager.domain.model.MembershipRequiredException;
import de.householdmanager.domain.model.NotHouseholdAdminException;
import de.householdmanager.domain.model.TaskNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Fehlerübersetzung des REST-Adapters: Fehler aus der Anwendung werden hier
 * ins HTTP-Format übersetzt.
 *
 * <p>Die Zuordnung folgt dem API-Vertrag aus Issue #1: Fachfehler der
 * Eingabe → 400, fehlende Mitgliedschaft oder Leitung → 403, nicht
 * gefunden → 404. {@link IllegalArgumentException} ist seit Issue #1 ein
 * gültiger Eingabefehler der Anwendung (z. B. unbekannte Person,
 * Entfernung der Haushaltsleitung) und wird ebenfalls 400.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidHouseholdNameException.class)
    public ProblemDetail handleInvalidName(InvalidHouseholdNameException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Ungültiger Haushaltsname");
        return problem;
    }

    @ExceptionHandler(InvalidTaskTitleException.class)
    public ProblemDetail handleInvalidTaskTitle(InvalidTaskTitleException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Ungültiger Aufgabentitel");
        return problem;
    }

    @ExceptionHandler(InvalidWasteCalendarUrlException.class)
    public ProblemDetail handleInvalidWasteCalendarUrl(InvalidWasteCalendarUrlException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Ungültige ICS-Adresse");
        return problem;
    }

    @ExceptionHandler(MembershipRequiredException.class)
    public ProblemDetail handleMembershipRequired(MembershipRequiredException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN, exception.getMessage());
        problem.setTitle("Kein Zugang");
        return problem;
    }

    @ExceptionHandler(NotHouseholdAdminException.class)
    public ProblemDetail handleNotHouseholdAdmin(NotHouseholdAdminException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN, exception.getMessage());
        problem.setTitle("Nicht die Haushaltsleitung");
        return problem;
    }

    @ExceptionHandler({TaskNotFoundException.class, HouseholdNotFoundException.class})
    public ProblemDetail handleNotFound(RuntimeException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND, exception.getMessage());
        problem.setTitle("Nicht gefunden");
        return problem;
    }

    /**
     * Eingabefehler der Anwendung (unbekannte Person, unzulässige Aktion).
     * Programmiertechnische {@code IllegalStateException} bleiben 500.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                exception.getMessage() == null ? "Ungültige Anfrage" : exception.getMessage());
        problem.setTitle("Ungültige Anfrage");
        return problem;
    }

    @ExceptionHandler(AuthController.NotAuthenticatedException.class)
    public ProblemDetail handleNotAuthenticated(AuthController.NotAuthenticatedException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED, "Nicht angemeldet.");
        problem.setTitle("Nicht angemeldet");
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        String detail = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("Ungültige Anfrage");
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Ungültige Anfrage");
        return problem;
    }
}

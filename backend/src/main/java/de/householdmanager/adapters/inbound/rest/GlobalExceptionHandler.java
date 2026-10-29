package de.householdmanager.adapters.inbound.rest;

import de.householdmanager.domain.model.InvalidHouseholdNameException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Fehlerübersetzung des REST-Adapters: Fehler aus der Anwendung werden hier
 * ins HTTP-Format übersetzt.
 *
 * <p>Nur **Fachfehler** werden zu 400. Andere
 * {@link IllegalArgumentException}s sind Programmierfehler und bleiben 500 —
 * sie dürfen nicht als „Benutzerfehler" getarnt werden.
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

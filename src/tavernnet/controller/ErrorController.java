package tavernnet.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;

import tavernnet.exception.CharacterNotFoundException;
import tavernnet.exception.PostNotFoundException;

import java.util.Map;
import java.util.List;

@RestControllerAdvice
public class ErrorController {

    // ==== ERRORES DE VALIDACION ==============================================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ErrorResponse handleValidationErrors(
        MethodArgumentNotValidException ex,
        HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        problem.setTitle("Validation failed");
        problem.setDetail("One or more fields are invalid");
        problem.setType(
            MvcUriComponentsBuilder
                .fromController(ErrorController.class)
                .pathSegment("error", "validation-failed")
                .build()
                .toUri()
        );

        // Lista de campos con errores
        List<Map<String, String>> errors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(err -> Map.of(
                "field", err.getField(),
                "error", err.getDefaultMessage()
            ))
            .toList();

        problem.setProperty("invalidFields", errors);
        problem.setProperty("path", request.getRequestURI());

        return ErrorResponse.builder(ex, problem).build();
    }

    // ==== ERRORES DE DESERIALIZADO ===========================================

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ErrorResponse handleJsonParseError(HttpMessageNotReadableException ex, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        problem.setTitle("Malformed JSON request");
        problem.setDetail("The request body could not be parsed or contains invalid data");
        problem.setType(
            MvcUriComponentsBuilder
                .fromController(ErrorController.class)
                .pathSegment("error", "json-parse-error")
                .build()
                .toUri()
        );

        problem.setProperty("path", request.getRequestURI());
        // TODO: personalizar mejor este mensaje de error
        problem.setProperty("cause", ex.getMostSpecificCause().getMessage());

        return ErrorResponse.builder(ex, problem).build();
    }

    // ==== ERRORES CUSTOM =====================================================

    // TODO: hacer mas generico, no una excepcion por post/usuario/comentario/mensaje...
    @ExceptionHandler(PostNotFoundException.class)
    public ErrorResponse handlePostNotFound(PostNotFoundException e) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Post " + e.getId() + " not found");
        problem.setDetail("Post with id '" + e.getId() + "' cannot be found");
        problem.setType(
            MvcUriComponentsBuilder
                .fromController(ErrorController.class)
                .pathSegment("error", "post-not-found")
                .build()
                .toUri()
        );
        return ErrorResponse.builder(e, problem).build();
    }

    @ExceptionHandler(CharacterNotFoundException.class)
    public ErrorResponse handleCharacterNotFound(CharacterNotFoundException e) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("Character " + e.getId() + " not found");
        problem.setDetail("Character with id '" + e.getId() + "' cannot be found");
        problem.setType(
            MvcUriComponentsBuilder
                .fromController(ErrorController.class)
                .pathSegment("error", "character-not-found")
                .build()
                .toUri()
        );
        return ErrorResponse.builder(e, problem).build();
    }
}

package tavernnet.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tavernnet.exception.ResourceNotFoundException;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ErrorController {
    private static final Logger log = LoggerFactory.getLogger(ErrorController.class);

    private URI getType(String name) {
        return MvcUriComponentsBuilder
            .fromController(ErrorController.class)
            .pathSegment("error", name)
            .build()
            .toUri();
    }

    // Cuerpo no legible o tipo de contenido incorrecto
    @ExceptionHandler({
        HttpMessageNotReadableException.class,   // Error al leer el cuerpo
        HttpMediaTypeNotSupportedException.class // Content-Type no soportado
    })
    public ErrorResponse handleInvalidBody(Exception ex, HttpServletRequest request) {
        log.warn("Invalid request body at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        var problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Invalid or unsupported request body");
        problem.setDetail("Request body must be a valid JSON and match the expected format");
        problem.setType(getType("invalid-body"));
        problem.setProperty("path", request.getRequestURI());
        return ErrorResponse.builder(ex, problem).build();
    }

    // Errores de validación
    @ExceptionHandler({
        MethodArgumentNotValidException.class,   // @Valid en @RequestBody o DTOs
        ConstraintViolationException.class,      // @Validated en @RequestParam, @PathVariable o metodo
        BindException.class,                     // Errores de binding en formularios o query params
        HandlerMethodValidationException.class   // Validación a nivel de metodo
    })
    public ErrorResponse handleValidation(Exception ex, HttpServletRequest request) {
        log.warn("Validation error at {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        problem.setTitle("Validation failed");
        problem.setDetail("One or more fields are invalid");
        problem.setType(getType("validation-failed"));
        problem.setProperty("path", request.getRequestURI());

        // Lista de campos con errores
        if (ex instanceof MethodArgumentNotValidException manv) {
            var errors = manv.getBindingResult().getFieldErrors().stream()
                .map(err -> Map.of(
                    "field", err.getField(),
                    "error", err.getDefaultMessage()
                ))
                .toList();
            problem.setProperty("invalidFields", errors);
        }
        else if (ex instanceof ConstraintViolationException cve) {
            // Se lanza cuando se violan constraints en parámetros validados con @Validated
            var errors = cve.getConstraintViolations().stream()
                .map(v -> Map.of(
                    "field", v.getPropertyPath().toString(),
                    "error", v.getMessage()
                ))
                .toList();
            problem.setProperty("invalidFields", errors);
        }
        else if (ex instanceof BindException be) {
            // Se lanza cuando falla el binding de parámetros de formulario, query o path a un objeto
            var errors = be.getFieldErrors().stream()
                .map(err -> Map.of(
                    "field", err.getField(),
                    "error", err.getDefaultMessage()
                ))
                .toList();
            problem.setProperty("invalidFields", errors);
        }
        else if (ex instanceof HandlerMethodValidationException hmve) {
            // Se lanza cuando una validación falla en el nivel de metodo
            var errors = hmve.getAllErrors().stream()
                .map(err -> {
                    // Los codes pueden contener nombres como "NotBlank.userInputPost.content"
                    String fieldCode = (err.getCodes().length > 0)
                        ? err.getCodes()[0]
                        : "unknown";
                    // Extrae solo el último segmento después del último '.'
                    String field = fieldCode.contains(".")
                        ? fieldCode.substring(fieldCode.lastIndexOf('.') + 1)
                        : fieldCode;
                    return Map.of("field", field, "error", err.getDefaultMessage());
                })
                .toList();

            problem.setProperty("invalidFields", errors);
        }

        return ErrorResponse.builder(ex, problem).build();
    }

    // Errores de URL, parametros y querys que faltan
    @ExceptionHandler({
        MissingServletRequestParameterException.class,
        MissingPathVariableException.class,
        MethodArgumentTypeMismatchException.class,
        NoResourceFoundException.class
    })
    public ErrorResponse handleInvalidPathOrParams(Exception ex, HttpServletRequest request) {
        log.warn("Invalid path or parameters at {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        var problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Invalid request URL or parameters");
        problem.setDetail("The requested URL or parameters are invalid or incomplete");
        problem.setType(getType("invalid-url-or-params"));
        problem.setProperty("path", request.getRequestURI());

        Map<String, Object> info = new LinkedHashMap<>();
        if (ex instanceof MissingServletRequestParameterException missingParam) {
            info.put("missingParameter", missingParam.getParameterName());
            info.put("expectedType", missingParam.getParameterType());
            problem.setDetail("Missing required query parameter: " + missingParam.getParameterName());
        }
        else if (ex instanceof MissingPathVariableException missingPath) {
            info.put("missingPathVariable", missingPath.getVariableName());
            info.put("expectedType", missingPath.getParameter().getParameterType().getSimpleName());
            problem.setDetail("Missing required path variable: " + missingPath.getVariableName());
        }
        else if (ex instanceof MethodArgumentTypeMismatchException typeMismatch) {
            info.put("parameter", typeMismatch.getName());
            info.put("expectedType", typeMismatch.getRequiredType() != null
                ? typeMismatch.getRequiredType().getSimpleName()
                : "unknown");
            info.put("providedValue", typeMismatch.getValue());
            problem.setDetail(String.format(
                "Parameter '%s' must be of type %s, but value '%s' was provided",
                typeMismatch.getName(),
                info.get("expectedType"),
                typeMismatch.getValue()
            ));
        }
        else if (ex instanceof NoResourceFoundException noRes) {
            info.put("resourcePath", noRes.getResourcePath());
            problem.setDetail("The requested resource does not exist: " + noRes.getResourcePath());
        }

        if (!info.isEmpty()) {
            problem.setProperty("context", info);
        }

        return ErrorResponse.builder(ex, problem).build();
    }

        // Errores de URL, parametros y querys que faltan
        @ExceptionHandler({
            HttpRequestMethodNotSupportedException.class,
        })
        public ErrorResponse handleInvalidPathOrParams(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
            log.warn("Invalid path or parameters at {}: {}", request.getRequestURI(), ex.getMessage(), ex);
            var problem = ProblemDetail.forStatus(HttpStatus.METHOD_NOT_ALLOWED);
            problem.setTitle("Invalid method");
            problem.setDetail("The requested endpoint does not support the %s method".formatted(ex.getMethod()));
            problem.setType(getType("method-not-allowed"));
            problem.setProperty("path", request.getRequestURI());
            return ErrorResponse.builder(ex, problem).build();
        }

    // ==== ERRORES CUSTOM =====================================================

    // Recursos no encontrados
    @ExceptionHandler(ResourceNotFoundException.class)
    public ErrorResponse handlePostNotFound(ResourceNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setTitle("%s was not found".formatted(ex.getType()));
        problem.setDetail(ex.getMessage());
        problem.setType(getType("not-found"));
        return ErrorResponse.builder(ex, problem).build();
    }
}

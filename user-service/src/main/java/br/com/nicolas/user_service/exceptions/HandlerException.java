package br.com.nicolas.user_service.exceptions;



import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestControllerAdvice
public class HandlerException {
  @ExceptionHandler(EmailAlreadyExistsException.class)
  public ResponseEntity<ErroResponse> emailAlreadyException(EmailAlreadyExistsException ex) {

    ErroResponse error = new ErroResponse(
      HttpStatus.BAD_REQUEST.value(),
      ex.getMessage(),
      System.currentTimeMillis()

    );
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(EmailInvalidException.class)
  public ResponseEntity<ErroResponse> emailInvalidException(EmailInvalidException ex) {
    ErroResponse error = new ErroResponse(
      HttpStatus.BAD_REQUEST.value(),
      ex.getMessage(),
      System.currentTimeMillis()
    );
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
    List<String> errors = ex.getBindingResult().getFieldErrors()
      .stream()
      .map(FieldError::getDefaultMessage)
      .toList();

    Map<String, Object> body = new HashMap<>();
    body.put("message", "Erro de validação");
    body.put("errors", errors);
    body.put("statusCode", HttpStatus.BAD_REQUEST.value());

    return ResponseEntity.badRequest().body(body);
  }


}

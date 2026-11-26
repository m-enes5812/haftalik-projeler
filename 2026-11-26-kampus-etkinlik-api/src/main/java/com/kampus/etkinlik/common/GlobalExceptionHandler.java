package com.kampus.etkinlik.common;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Tüm hataları RFC 9457 (application/problem+json) biçiminde döndürür. */
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ApiException.class)
  ProblemDetail handleApi(ApiException ex) {
    return ProblemDetail.forStatusAndDetail(ex.status(), ex.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
    ProblemDetail pd =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "İstek doğrulanamadı");
    Map<String, String> errors = new LinkedHashMap<>();
    ex.getBindingResult()
        .getFieldErrors()
        .forEach(e -> errors.putIfAbsent(e.getField(), e.getDefaultMessage()));
    pd.setProperty("errors", errors);
    return pd;
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ProblemDetail handleIntegrity(DataIntegrityViolationException ex) {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.CONFLICT, "Kayıt mevcut verilerle çakışıyor");
  }
}

package com.loanengine.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> invalid(MethodArgumentNotValidException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(f -> fields.put(f.getField(), f.getDefaultMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "Some fields are not valid", "fields", fields));
    }
}

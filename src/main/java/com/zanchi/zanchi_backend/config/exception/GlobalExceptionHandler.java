package com.zanchi.zanchi_backend.config.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(ApiException.class)
    @ResponseStatus
    public Map<String, Object> handle(ApiException ex, HttpServletRequest req) {
        return problem(ex.getStatus(), ex.getMessage(), req.getRequestURI());
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(NOT_FOUND)
    public Map<String, Object> handleNotFound(NoSuchElementException ex, HttpServletRequest req) {
        return problem(NOT_FOUND, ex.getMessage(), req.getRequestURI());
    }

    private Map<String, Object> problem(HttpStatus st, String msg, String uri) {
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("type", "about:blank");
        b.put("title", st.getReasonPhrase());
        b.put("status", st.value());
        b.put("detail", msg);
        b.put("instance", uri);
        return b;
    }
}
package com.kafka.library_events_producer.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.stream.Collectors;
@Slf4j
@ControllerAdvice
public class LibraryEventsControllerAdvice {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleException(MethodArgumentNotValidException ex){
        String collect = ex.getBindingResult().getFieldErrors().stream().map(fieldError ->
                        fieldError.getField() + " - " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(","));
        log.info("error message: {}",collect);
        return new ResponseEntity<>(collect, HttpStatus.BAD_REQUEST);
    }
}

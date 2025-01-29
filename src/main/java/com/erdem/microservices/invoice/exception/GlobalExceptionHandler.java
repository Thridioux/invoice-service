package com.erdem.microservices.invoice.exception;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestController
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception e) {
        return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
    }
    
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleFileSizeLimitExceed(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(400).body(Map.of("error", "File size exceeds the allowed limit"));
    }


}

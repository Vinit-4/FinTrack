package com.fintrack.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/** The single JSON shape every error in this API uses. */
@Data
@Builder
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;

    /** Only filled for validation failures: field name -> problem. */
    private Map<String, String> fieldErrors;
}

package com.fintrack.dto;

import com.fintrack.entity.Category;
import com.fintrack.entity.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * What we send back to the client. We never return the Transaction entity itself,
 * because it holds a reference to the User (and therefore the password hash).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    private Long id;
    private TransactionType type;
    private BigDecimal amount;
    private Category category;
    private String description;
    private LocalDate transactionDate;
    private LocalDateTime createdAt;
}

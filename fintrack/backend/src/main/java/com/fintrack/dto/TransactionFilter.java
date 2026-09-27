package com.fintrack.dto;

import com.fintrack.entity.Category;
import com.fintrack.entity.TransactionType;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Query parameters of GET /api/transactions.
 * Every field is optional; null means "do not filter on this".
 */
@Data
public class TransactionFilter {

    private TransactionType type;

    private Category category;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    /** 1-12. Only used together with year. */
    private Integer month;

    private Integer year;

    /** "asc" or "desc" (default) on transactionDate. */
    private String sort = "desc";
}

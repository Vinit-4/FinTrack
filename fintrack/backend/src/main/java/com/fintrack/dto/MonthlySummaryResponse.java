package com.fintrack.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** Result of GET /api/analytics/monthly-summary. */
@Data
@Builder
public class MonthlySummaryResponse {
    private int year;
    private int month;
    private String monthLabel;          // e.g. "March 2026"
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal balance;
    private long transactionCount;
    private List<CategoryTotalResponse> categoryTotals;
}

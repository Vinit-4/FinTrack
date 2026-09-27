package com.fintrack.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

/** One point on the "income vs expense" bar chart and the spending trend line. */
@Data
@AllArgsConstructor
public class MonthlyPointResponse {
    private int year;
    private int month;
    private String label;   // e.g. "Mar 2026"
    private BigDecimal income;
    private BigDecimal expense;
    private BigDecimal balance;
}

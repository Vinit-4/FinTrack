package com.fintrack.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class DashboardResponse {
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal balance;
    private BigDecimal currentMonthIncome;
    private BigDecimal currentMonthExpense;
    private BigDecimal currentMonthBalance;
    private List<TransactionResponse> recentTransactions;
}

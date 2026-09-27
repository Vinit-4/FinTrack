package com.fintrack.service;

import com.fintrack.dto.DashboardResponse;
import com.fintrack.entity.TransactionType;
import com.fintrack.repository.TransactionRepository;
import com.fintrack.util.TransactionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Builds the numbers shown on the dashboard cards.
 * The sums are computed by PostgreSQL (SUM ... GROUP BY), not in Java and not in React:
 * the database is far faster at it and the frontend stays a thin display layer.
 */
@Service
public class DashboardService {

    private final TransactionRepository transactionRepository;

    public DashboardService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse getSummary(Long userId) {
        YearMonth currentMonth = YearMonth.now();
        LocalDate monthStart = currentMonth.atDay(1);
        LocalDate monthEnd = currentMonth.atEndOfMonth();

        BigDecimal totalIncome = transactionRepository.sumByType(userId, TransactionType.INCOME);
        BigDecimal totalExpense = transactionRepository.sumByType(userId, TransactionType.EXPENSE);

        BigDecimal monthIncome = transactionRepository
                .sumByTypeAndDateRange(userId, TransactionType.INCOME, monthStart, monthEnd);
        BigDecimal monthExpense = transactionRepository
                .sumByTypeAndDateRange(userId, TransactionType.EXPENSE, monthStart, monthEnd);

        return DashboardResponse.builder()
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .balance(totalIncome.subtract(totalExpense))
                .currentMonthIncome(monthIncome)
                .currentMonthExpense(monthExpense)
                .currentMonthBalance(monthIncome.subtract(monthExpense))
                .recentTransactions(recentTransactions(userId))
                .build();
    }

    private List<com.fintrack.dto.TransactionResponse> recentTransactions(Long userId) {
        return transactionRepository.findTop5ByUserIdOrderByTransactionDateDescIdDesc(userId)
                .stream()
                .map(TransactionMapper::toResponse)
                .toList();
    }
}

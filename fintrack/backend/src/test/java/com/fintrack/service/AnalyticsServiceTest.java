package com.fintrack.service;

import com.fintrack.dto.MonthlyPointResponse;
import com.fintrack.dto.MonthlySummaryResponse;
import com.fintrack.entity.Category;
import com.fintrack.entity.Transaction;
import com.fintrack.entity.TransactionType;
import com.fintrack.exception.BadRequestException;
import com.fintrack.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    private Transaction expense(LocalDate date, String amount) {
        return Transaction.builder()
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal(amount))
                .category(Category.FOOD)
                .transactionDate(date)
                .build();
    }

    private Transaction income(LocalDate date, String amount) {
        return Transaction.builder()
                .type(TransactionType.INCOME)
                .amount(new BigDecimal(amount))
                .category(Category.SALARY)
                .transactionDate(date)
                .build();
    }

    @Test
    void monthlySummaryCalculatesBalanceAndCount() {
        YearMonth month = YearMonth.of(2026, 3);

        when(transactionRepository.sumByTypeAndDateRange(
                eq(USER_ID), eq(TransactionType.INCOME), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BigDecimal("50000.00"));
        when(transactionRepository.sumByTypeAndDateRange(
                eq(USER_ID), eq(TransactionType.EXPENSE), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BigDecimal("18500.00"));
        when(transactionRepository.countByUserIdAndTransactionDateBetween(
                eq(USER_ID), any(LocalDate.class), any(LocalDate.class))).thenReturn(12L);
        when(transactionRepository.sumGroupedByCategory(
                eq(USER_ID), eq(TransactionType.EXPENSE), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(new Object[]{Category.FOOD, new BigDecimal("4500.00")}));

        MonthlySummaryResponse summary =
                analyticsService.getMonthlySummary(USER_ID, month.getYear(), month.getMonthValue());

        assertThat(summary.getBalance()).isEqualByComparingTo("31500.00");
        assertThat(summary.getTransactionCount()).isEqualTo(12L);
        assertThat(summary.getMonthLabel()).isEqualTo("March 2026");
        assertThat(summary.getCategoryTotals()).hasSize(1);
        assertThat(summary.getCategoryTotals().get(0).getTotal()).isEqualByComparingTo("4500.00");
    }

    @Test
    void monthlyTrendGroupsTransactionsIntoOnePointPerMonth() {
        LocalDate thisMonth = YearMonth.now().atDay(5);
        LocalDate lastMonth = YearMonth.now().minusMonths(1).atDay(5);

        when(transactionRepository.findByUserIdAndTransactionDateBetween(
                anyLong(), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(
                        income(lastMonth, "40000.00"),
                        expense(lastMonth, "15000.00"),
                        expense(thisMonth, "2500.00")));

        List<MonthlyPointResponse> points = analyticsService.getMonthlyTrend(USER_ID, 2);

        assertThat(points).hasSize(2);
        assertThat(points.get(0).getIncome()).isEqualByComparingTo("40000.00");
        assertThat(points.get(0).getExpense()).isEqualByComparingTo("15000.00");
        assertThat(points.get(0).getBalance()).isEqualByComparingTo("25000.00");
        assertThat(points.get(1).getExpense()).isEqualByComparingTo("2500.00");
        assertThat(points.get(1).getIncome()).isEqualByComparingTo("0");
    }

    @Test
    void monthlyTrendRejectsAnUnreasonableRange() {
        assertThatThrownBy(() -> analyticsService.getMonthlyTrend(USER_ID, 0))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void monthlySummaryRejectsInvalidMonth() {
        assertThatThrownBy(() -> analyticsService.getMonthlySummary(USER_ID, 2026, 13))
                .isInstanceOf(BadRequestException.class);
    }
}

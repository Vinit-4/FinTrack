package com.fintrack.service;

import com.fintrack.dto.CategoryTotalResponse;
import com.fintrack.dto.MonthlyPointResponse;
import com.fintrack.dto.MonthlySummaryResponse;
import com.fintrack.entity.Category;
import com.fintrack.entity.Transaction;
import com.fintrack.entity.TransactionType;
import com.fintrack.exception.BadRequestException;
import com.fintrack.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Powers the three charts on the analytics page.
 *
 * Category totals are aggregated by the database. The month-by-month series are
 * aggregated in Java after one range query, which keeps the JPQL portable and
 * is perfectly fast for a personal-finance amount of data.
 */
@Service
public class AnalyticsService {

    private static final DateTimeFormatter SHORT_MONTH =
            DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter LONG_MONTH =
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

    private final TransactionRepository transactionRepository;

    public AnalyticsService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /** Category-wise expense breakdown for one month -> pie chart. */
    @Transactional(readOnly = true)
    public List<CategoryTotalResponse> getCategoryBreakdown(Long userId, Integer year, Integer month) {
        YearMonth target = resolveMonth(year, month);
        return categoryTotals(userId, target);
    }

    /** Everything about one month, including the category split. */
    @Transactional(readOnly = true)
    public MonthlySummaryResponse getMonthlySummary(Long userId, Integer year, Integer month) {
        YearMonth target = resolveMonth(year, month);
        LocalDate start = target.atDay(1);
        LocalDate end = target.atEndOfMonth();

        BigDecimal income = transactionRepository
                .sumByTypeAndDateRange(userId, TransactionType.INCOME, start, end);
        BigDecimal expense = transactionRepository
                .sumByTypeAndDateRange(userId, TransactionType.EXPENSE, start, end);

        return MonthlySummaryResponse.builder()
                .year(target.getYear())
                .month(target.getMonthValue())
                .monthLabel(target.atDay(1).format(LONG_MONTH))
                .totalIncome(income)
                .totalExpense(expense)
                .balance(income.subtract(expense))
                .transactionCount(transactionRepository
                        .countByUserIdAndTransactionDateBetween(userId, start, end))
                .categoryTotals(categoryTotals(userId, target))
                .build();
    }

    /**
     * Income vs expense for the last N months (default 6), oldest first.
     * The same series drives the bar chart and the spending trend line.
     */
    @Transactional(readOnly = true)
    public List<MonthlyPointResponse> getMonthlyTrend(Long userId, int months) {
        if (months < 1 || months > 36) {
            throw new BadRequestException("months must be between 1 and 36");
        }

        YearMonth lastMonth = YearMonth.now();
        YearMonth firstMonth = lastMonth.minusMonths(months - 1L);

        List<Transaction> transactions = transactionRepository.findByUserIdAndTransactionDateBetween(
                userId, firstMonth.atDay(1), lastMonth.atEndOfMonth());

        List<MonthlyPointResponse> points = new ArrayList<>();

        for (int i = 0; i < months; i++) {
            YearMonth current = firstMonth.plusMonths(i);

            BigDecimal income = BigDecimal.ZERO;
            BigDecimal expense = BigDecimal.ZERO;

            for (Transaction transaction : transactions) {
                if (YearMonth.from(transaction.getTransactionDate()).equals(current)) {
                    if (transaction.getType() == TransactionType.INCOME) {
                        income = income.add(transaction.getAmount());
                    } else {
                        expense = expense.add(transaction.getAmount());
                    }
                }
            }

            points.add(new MonthlyPointResponse(
                    current.getYear(),
                    current.getMonthValue(),
                    current.atDay(1).format(SHORT_MONTH),
                    income,
                    expense,
                    income.subtract(expense)));
        }

        return points;
    }

    private List<CategoryTotalResponse> categoryTotals(Long userId, YearMonth target) {
        List<Object[]> rows = transactionRepository.sumGroupedByCategory(
                userId, TransactionType.EXPENSE, target.atDay(1), target.atEndOfMonth());

        List<CategoryTotalResponse> totals = new ArrayList<>();
        for (Object[] row : rows) {
            totals.add(new CategoryTotalResponse((Category) row[0], (BigDecimal) row[1]));
        }
        return totals;
    }

    /** Missing year/month simply means "the current month". */
    private YearMonth resolveMonth(Integer year, Integer month) {
        if (year == null && month == null) {
            return YearMonth.now();
        }
        int resolvedYear = year != null ? year : YearMonth.now().getYear();
        int resolvedMonth = month != null ? month : YearMonth.now().getMonthValue();

        if (resolvedMonth < 1 || resolvedMonth > 12) {
            throw new BadRequestException("month must be between 1 and 12");
        }
        if (resolvedYear < 1970 || resolvedYear > 2999) {
            throw new BadRequestException("year is out of range");
        }
        return YearMonth.of(resolvedYear, resolvedMonth);
    }
}

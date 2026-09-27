package com.fintrack.util;

import com.fintrack.dto.TransactionFilter;
import com.fintrack.entity.Transaction;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds the WHERE clause of GET /api/transactions at runtime.
 *
 * Every filter is optional, so instead of writing one query per combination
 * we add a condition only when the user actually sent that parameter.
 * The user id condition is always added first, which is what makes it
 * impossible to read someone else's data through this endpoint.
 */
public final class TransactionSpecifications {

    private TransactionSpecifications() {
    }

    public static Specification<Transaction> forUserWithFilters(Long userId, TransactionFilter filter) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(builder.equal(root.get("user").get("id"), userId));

            if (filter.getType() != null) {
                predicates.add(builder.equal(root.get("type"), filter.getType()));
            }
            if (filter.getCategory() != null) {
                predicates.add(builder.equal(root.get("category"), filter.getCategory()));
            }

            LocalDate start = filter.getStartDate();
            LocalDate end = filter.getEndDate();

            // month/year is a shortcut that overrides an explicit date range
            if (filter.getYear() != null) {
                if (filter.getMonth() != null) {
                    YearMonth yearMonth = YearMonth.of(filter.getYear(), filter.getMonth());
                    start = yearMonth.atDay(1);
                    end = yearMonth.atEndOfMonth();
                } else {
                    start = LocalDate.of(filter.getYear(), 1, 1);
                    end = LocalDate.of(filter.getYear(), 12, 31);
                }
            }

            if (start != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("transactionDate"), start));
            }
            if (end != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("transactionDate"), end));
            }

            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}

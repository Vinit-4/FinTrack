package com.fintrack.repository;

import com.fintrack.entity.Transaction;
import com.fintrack.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * JpaSpecificationExecutor gives us dynamic filtering
 * (see TransactionSpecifications) without writing SQL by hand.
 */
public interface TransactionRepository
        extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    /**
     * The security rule of the whole app: a transaction is only found
     * when it belongs to the user who is asking for it.
     */
    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

    List<Transaction> findTop5ByUserIdOrderByTransactionDateDescIdDesc(Long userId);

    List<Transaction> findByUserIdAndTransactionDateBetween(Long userId, LocalDate start, LocalDate end);

    long countByUserIdAndTransactionDateBetween(Long userId, LocalDate start, LocalDate end);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.user.id = :userId AND t.type = :type")
    BigDecimal sumByType(@Param("userId") Long userId,
                         @Param("type") TransactionType type);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.user.id = :userId AND t.type = :type " +
           "AND t.transactionDate BETWEEN :start AND :end")
    BigDecimal sumByTypeAndDateRange(@Param("userId") Long userId,
                                     @Param("type") TransactionType type,
                                     @Param("start") LocalDate start,
                                     @Param("end") LocalDate end);

    /** Returns rows of [Category, BigDecimal total], biggest spender first. */
    @Query("SELECT t.category, COALESCE(SUM(t.amount), 0) FROM Transaction t " +
           "WHERE t.user.id = :userId AND t.type = :type " +
           "AND t.transactionDate BETWEEN :start AND :end " +
           "GROUP BY t.category ORDER BY SUM(t.amount) DESC")
    List<Object[]> sumGroupedByCategory(@Param("userId") Long userId,
                                        @Param("type") TransactionType type,
                                        @Param("start") LocalDate start,
                                        @Param("end") LocalDate end);
}

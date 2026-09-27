package com.fintrack.service;

import com.fintrack.dto.TransactionFilter;
import com.fintrack.dto.TransactionRequest;
import com.fintrack.dto.TransactionResponse;
import com.fintrack.entity.Category;
import com.fintrack.entity.Transaction;
import com.fintrack.entity.TransactionType;
import com.fintrack.entity.User;
import com.fintrack.exception.ResourceNotFoundException;
import com.fintrack.repository.TransactionRepository;
import com.fintrack.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TransactionService transactionService;

    private User owner() {
        User user = new User();
        user.setId(OWNER_ID);
        user.setName("Asha");
        user.setEmail("asha@example.com");
        return user;
    }

    private Transaction sampleTransaction() {
        return Transaction.builder()
                .id(10L)
                .user(owner())
                .type(TransactionType.EXPENSE)
                .amount(new BigDecimal("450.00"))
                .category(Category.FOOD)
                .description("Groceries")
                .transactionDate(LocalDate.of(2026, 3, 12))
                .build();
    }

    private TransactionRequest sampleRequest() {
        TransactionRequest request = new TransactionRequest();
        request.setType(TransactionType.EXPENSE);
        request.setAmount(new BigDecimal("450.00"));
        request.setCategory(Category.FOOD);
        request.setDescription("Groceries");
        request.setTransactionDate(LocalDate.of(2026, 3, 12));
        return request;
    }

    @Test
    void createSavesTransactionForTheLoggedInUser() {
        when(userRepository.findById(OWNER_ID)).thenReturn(Optional.of(owner()));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(sampleTransaction());

        TransactionResponse response = transactionService.create(OWNER_ID, sampleRequest());

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getAmount()).isEqualByComparingTo("450.00");
        assertThat(response.getCategory()).isEqualTo(Category.FOOD);
    }

    @Test
    void searchReturnsTransactionsSortedByDate() {
        when(transactionRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(sampleTransaction()));

        List<TransactionResponse> results = transactionService.search(OWNER_ID, new TransactionFilter());

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getDescription()).isEqualTo("Groceries");
    }

    @Test
    void getByIdReturnsTheOwnersTransaction() {
        when(transactionRepository.findByIdAndUserId(10L, OWNER_ID))
                .thenReturn(Optional.of(sampleTransaction()));

        assertThat(transactionService.getById(OWNER_ID, 10L).getId()).isEqualTo(10L);
    }

    @Test
    void anotherUserCannotReadSomeoneElsesTransaction() {
        when(transactionRepository.findByIdAndUserId(10L, OTHER_USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.getById(OTHER_USER_ID, 10L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Transaction not found");
    }

    @Test
    void anotherUserCannotDeleteSomeoneElsesTransaction() {
        when(transactionRepository.findByIdAndUserId(10L, OTHER_USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> transactionService.delete(OTHER_USER_ID, 10L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(transactionRepository, never()).delete(any(Transaction.class));
    }
}

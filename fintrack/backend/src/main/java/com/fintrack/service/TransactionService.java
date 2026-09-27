package com.fintrack.service;

import com.fintrack.dto.TransactionFilter;
import com.fintrack.dto.TransactionRequest;
import com.fintrack.dto.TransactionResponse;
import com.fintrack.entity.Transaction;
import com.fintrack.entity.User;
import com.fintrack.exception.ResourceNotFoundException;
import com.fintrack.repository.TransactionRepository;
import com.fintrack.repository.UserRepository;
import com.fintrack.util.TransactionMapper;
import com.fintrack.util.TransactionSpecifications;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * All transaction business logic lives here, not in the controller.
 *
 * Every method takes the caller's userId and uses it in the query itself,
 * so ownership is enforced by the database lookup rather than by an "if" that
 * someone could forget to write.
 */
@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public TransactionService(TransactionRepository transactionRepository, UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public TransactionResponse create(Long userId, TransactionRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Transaction transaction = Transaction.builder()
                .user(user)
                .type(request.getType())
                .amount(request.getAmount())
                .category(request.getCategory())
                .description(request.getDescription())
                .transactionDate(request.getTransactionDate())
                .build();

        return TransactionMapper.toResponse(transactionRepository.save(transaction));
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> search(Long userId, TransactionFilter filter) {
        Sort.Direction direction = "asc".equalsIgnoreCase(filter.getSort())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Sort sort = Sort.by(direction, "transactionDate").and(Sort.by(direction, "id"));

        return transactionRepository
                .findAll(TransactionSpecifications.forUserWithFilters(userId, filter), sort)
                .stream()
                .map(TransactionMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TransactionResponse getById(Long userId, Long transactionId) {
        return TransactionMapper.toResponse(findOwnedTransaction(userId, transactionId));
    }

    @Transactional
    public TransactionResponse update(Long userId, Long transactionId, TransactionRequest request) {
        Transaction transaction = findOwnedTransaction(userId, transactionId);

        transaction.setType(request.getType());
        transaction.setAmount(request.getAmount());
        transaction.setCategory(request.getCategory());
        transaction.setDescription(request.getDescription());
        transaction.setTransactionDate(request.getTransactionDate());

        return TransactionMapper.toResponse(transactionRepository.save(transaction));
    }

    @Transactional
    public void delete(Long userId, Long transactionId) {
        transactionRepository.delete(findOwnedTransaction(userId, transactionId));
    }

    /**
     * Returns the transaction only if it belongs to this user.
     * Another user's id gives exactly the same 404 as a non-existent id,
     * so the API never reveals that someone else's transaction exists.
     */
    private Transaction findOwnedTransaction(Long userId, Long transactionId) {
        return transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
    }
}

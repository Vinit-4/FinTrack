package com.fintrack.controller;

import com.fintrack.dto.TransactionFilter;
import com.fintrack.dto.TransactionRequest;
import com.fintrack.dto.TransactionResponse;
import com.fintrack.security.UserPrincipal;
import com.fintrack.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * Controllers only do three things: read the request, call a service, return a status.
 * The logged-in user is injected by Spring Security via @AuthenticationPrincipal;
 * the client never sends a userId, so it cannot pretend to be someone else.
 */
@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Transactions", description = "Create, read, update and delete your own transactions")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    @Operation(summary = "List transactions, optionally filtered by type, category, date range or month/year")
    public ResponseEntity<List<TransactionResponse>> list(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @ModelAttribute TransactionFilter filter) {
        return ResponseEntity.ok(transactionService.search(currentUser.getId(), filter));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one transaction by id")
    public ResponseEntity<TransactionResponse> getById(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long id) {
        return ResponseEntity.ok(transactionService.getById(currentUser.getId(), id));
    }

    @PostMapping
    @Operation(summary = "Record an income or an expense")
    public ResponseEntity<TransactionResponse> create(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody TransactionRequest request) {
        TransactionResponse created = transactionService.create(currentUser.getId(), request);
        return ResponseEntity.created(URI.create("/api/transactions/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace an existing transaction")
    public ResponseEntity<TransactionResponse> update(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long id,
            @Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.ok(transactionService.update(currentUser.getId(), id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a transaction")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long id) {
        transactionService.delete(currentUser.getId(), id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}

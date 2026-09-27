package com.fintrack.controller;

import com.fintrack.dto.CategoryTotalResponse;
import com.fintrack.dto.MonthlyPointResponse;
import com.fintrack.dto.MonthlySummaryResponse;
import com.fintrack.security.UserPrincipal;
import com.fintrack.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@Tag(name = "Analytics", description = "Aggregated figures that drive the charts")
@SecurityRequirement(name = "bearerAuth")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/categories")
    @Operation(summary = "Expense total per category for a month (defaults to the current month)")
    public ResponseEntity<List<CategoryTotalResponse>> categories(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        return ResponseEntity.ok(analyticsService.getCategoryBreakdown(currentUser.getId(), year, month));
    }

    @GetMapping("/monthly-summary")
    @Operation(summary = "Income, expense, balance, transaction count and category split for one month")
    public ResponseEntity<MonthlySummaryResponse> monthlySummary(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        return ResponseEntity.ok(analyticsService.getMonthlySummary(currentUser.getId(), year, month));
    }

    @GetMapping("/monthly")
    @Operation(summary = "Income vs expense for the last N months, oldest first")
    public ResponseEntity<List<MonthlyPointResponse>> monthly(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(defaultValue = "6") int months) {
        return ResponseEntity.ok(analyticsService.getMonthlyTrend(currentUser.getId(), months));
    }
}

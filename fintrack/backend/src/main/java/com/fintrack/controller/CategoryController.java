package com.fintrack.controller;

import com.fintrack.entity.Category;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/**
 * Lets the frontend build its dropdown from the backend's list,
 * so adding a category is a one-line change in the Category enum.
 */
@RestController
@RequestMapping("/api/categories")
@Tag(name = "Categories", description = "The categories a transaction can use")
@SecurityRequirement(name = "bearerAuth")
public class CategoryController {

    @GetMapping
    @Operation(summary = "List all supported categories")
    public ResponseEntity<List<String>> categories() {
        return ResponseEntity.ok(Arrays.stream(Category.values()).map(Enum::name).toList());
    }
}

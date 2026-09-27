package com.fintrack.dto;

import com.fintrack.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

/** One slice of the category pie chart. */
@Data
@AllArgsConstructor
public class CategoryTotalResponse {
    private Category category;
    private BigDecimal total;
}

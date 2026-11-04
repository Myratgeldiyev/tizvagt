package com.tizvagt.api.catalog;

import java.math.BigDecimal;

public record ProductFilter(String search, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice) {
}

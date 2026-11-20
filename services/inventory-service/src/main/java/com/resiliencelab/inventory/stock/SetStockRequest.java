package com.resiliencelab.inventory.stock;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SetStockRequest(@NotNull @Min(0) Integer availableQuantity) {
}

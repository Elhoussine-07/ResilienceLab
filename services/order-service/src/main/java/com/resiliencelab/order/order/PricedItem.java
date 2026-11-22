package com.resiliencelab.order.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/** An order line with the unit price taken from the catalog at order time. */
public record PricedItem(UUID productId, int quantity, BigDecimal unitPrice) {

    public BigDecimal lineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public static BigDecimal totalOf(List<PricedItem> items) {
        return items.stream()
                .map(PricedItem::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
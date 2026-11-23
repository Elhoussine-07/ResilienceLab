package com.resiliencelab.inventory.stock;

/** Result of a reservation: {@code created} is false when an identical request was replayed. */
public record ReservationOutcome(ReservationResponse reservation, boolean created) {
}

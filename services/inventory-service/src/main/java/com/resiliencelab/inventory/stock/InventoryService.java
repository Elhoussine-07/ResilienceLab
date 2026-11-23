package com.resiliencelab.inventory.stock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class InventoryService {

    private final StockRepository stockRepository;
    private final ReservationRepository reservationRepository;

    public InventoryService(StockRepository stockRepository, ReservationRepository reservationRepository) {
        this.stockRepository = stockRepository;
        this.reservationRepository = reservationRepository;
    }

    @Transactional
    public StockResponse setStock(UUID productId, int availableQuantity) {
        stockRepository.upsertAvailableQuantity(productId, availableQuantity);
        return getStock(productId);
    }

    @Transactional(readOnly = true)
    public StockResponse getStock(UUID productId) {
        return stockRepository.findById(productId)
                .map(StockResponse::from)
                .orElseThrow(() -> new StockItemNotFoundException(productId));
    }

    /**
     * Reserves stock for an order line. Idempotent on (orderId, productId):
     * the reservation row is inserted first, so concurrent duplicates collapse
     * into one. If the stock turns out to be insufficient, the exception rolls
     * the whole transaction back, including the inserted reservation.
     */
    @Transactional
    public ReservationOutcome reserve(ReserveStockRequest request) {
        UUID orderId = request.orderId();
        UUID productId = request.productId();
        int quantity = request.quantity();

        int inserted = reservationRepository.insertIfAbsent(UUID.randomUUID(), orderId, productId, quantity);
        if (inserted == 0) {
            return replay(orderId, productId, quantity);
        }

        int updated = stockRepository.reserve(productId, quantity);
        if (updated == 0) {
            if (!stockRepository.existsById(productId)) {
                throw new StockItemNotFoundException(productId);
            }
            throw new InsufficientStockException(productId, quantity);
        }

        Reservation reservation = reservationRepository.findByOrderIdAndProductId(orderId, productId)
                .orElseThrow(() -> new IllegalStateException("Reservation not found right after insert"));
        return new ReservationOutcome(ReservationResponse.from(reservation), true);
    }

    /**
     * Releases every reservation of an order (saga compensation). Idempotent:
     * only the caller that flips a reservation to RELEASED gives the stock back.
     */
    @Transactional
    public List<ReservationResponse> release(UUID orderId) {
        for (Reservation reservation : reservationRepository.findByOrderId(orderId)) {
            int flipped = reservationRepository.markReleased(reservation.getId());
            if (flipped == 1) {
                stockRepository.restock(reservation.getProductId(), reservation.getQuantity());
            }
        }
        return listReservations(orderId);
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> listReservations(UUID orderId) {
        return reservationRepository.findByOrderId(orderId).stream()
                .map(ReservationResponse::from)
                .toList();
    }

    private ReservationOutcome replay(UUID orderId, UUID productId, int quantity) {
        Reservation existing = reservationRepository.findByOrderIdAndProductId(orderId, productId)
                .orElseThrow(() -> new IllegalStateException("Reservation vanished after conflict"));
        if (existing.getQuantity() != quantity) {
            throw new ReservationConflictException(
                    "Order " + orderId + " already reserved a different quantity of product " + productId);
        }
        if (existing.getStatus() == ReservationStatus.RELEASED) {
            throw new ReservationConflictException(
                    "The reservation of order " + orderId + " for product " + productId + " was already released");
        }
        return new ReservationOutcome(ReservationResponse.from(existing), false);
    }
}

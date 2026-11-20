package com.resiliencelab.inventory.stock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface StockRepository extends JpaRepository<StockItem, UUID> {

    /** Creates the stock line or sets its available quantity, in one atomic statement. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO stock_items (product_id, available_quantity, reserved_quantity, updated_at)
            VALUES (:productId, :quantity, 0, now())
            ON CONFLICT (product_id)
            DO UPDATE SET available_quantity = EXCLUDED.available_quantity, updated_at = now()
            """, nativeQuery = true)
    int upsertAvailableQuantity(@Param("productId") UUID productId, @Param("quantity") int quantity);

    /**
     * Moves {@code quantity} from available to reserved, only if enough is available.
     * Returns 1 on success, 0 if the product is unknown or the stock is insufficient.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            UPDATE stock_items
               SET available_quantity = available_quantity - :quantity,
                   reserved_quantity  = reserved_quantity + :quantity,
                   updated_at         = now()
             WHERE product_id = :productId
               AND available_quantity >= :quantity
            """, nativeQuery = true)
    int reserve(@Param("productId") UUID productId, @Param("quantity") int quantity);

    /** Gives a reserved quantity back to the available stock. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            UPDATE stock_items
               SET available_quantity = available_quantity + :quantity,
                   reserved_quantity  = reserved_quantity - :quantity,
                   updated_at         = now()
             WHERE product_id = :productId
            """, nativeQuery = true)
    int restock(@Param("productId") UUID productId, @Param("quantity") int quantity);
}

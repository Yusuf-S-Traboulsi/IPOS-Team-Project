package com.pharmacy.iposca.api;

import com.pharmacy.iposca.model.Product;

/**
 * IPOS-CA Inventory API Interface
 *
 * This interface provides read-only access to the pharmacy inventory system
 * for external subsystems (e.g., supplier ordering systems, branch inventory checks,
 * reporting tools).
 *
 * <p><strong>Security Note:</strong> This API is read-only. External systems cannot
 * modify inventory data, prices, or stock levels through this interface.</p>
 *
 * <p><strong>Usage Example:</strong></p>
 * <pre>
 * {@code
 * IInventoryAPI api = new CAController();
 *
 * // Search for products
 * Product[] results = api.searchStock("Paracetamol");
 *
 * // Check stock level
 * int stock = api.getStockLevel(1);
 *
 * // Get retail price
 * float price = api.getRetailPrice(1);
 * }
 * </pre>
 *
 * @version 1.0
 * @since 2026
 */
public interface IInventoryAPI {

    /**
     * Search for products by name or ID.
     *
     * <p>Supports partial matching for product names. For example, searching
     * "Paracet" will match "Paracetamol". Numeric searches match product IDs.</p>
     *
     * @param criteria Search string (product name or ID). Case-insensitive.
     *                 If null or empty, returns all products.
     * @return Array of matching Product objects. Returns empty array if no matches.
     *         Never returns null.
     *
     * @since 1.0
     */
    Product[] searchStock(String criteria);

    /**
     * Get the current stock level for a specific product.
     *
     * <p>Stock levels are updated in real-time as sales are processed.</p>
     *
     * @param itemID The product ID (must be positive integer)
     * @return Stock level in units/packs. Returns -1 if product not found.
     *
     * @since 1.0
     */
    int getStockLevel(int itemID);

    /**
     * Get the retail price for a specific product.
     *
     * <p>Price includes VAT at the current rate (default 20%).</p>
     *
     * @param itemID The product ID (must be positive integer)
     * @return Retail price in GBP (£). Returns -1.0f if product not found.
     *
     * @since 1.0
     */
    float getRetailPrice(int itemID);
}
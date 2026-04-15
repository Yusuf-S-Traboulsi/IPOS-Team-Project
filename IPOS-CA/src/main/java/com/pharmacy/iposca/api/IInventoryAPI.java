package com.pharmacy.iposca.api;

import com.pharmacy.iposca.model.Product;

public interface IInventoryAPI {

    Product[] searchStock(String criteria);

    int getStockLevel(int itemID);

    float getRetailPrice(int itemID);
}
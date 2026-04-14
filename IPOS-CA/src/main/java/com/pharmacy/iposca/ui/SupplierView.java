package com.pharmacy.iposca.ui;

import javafx.scene.layout.VBox;

/**
 * This UI class handles the Supplier module.
 * Wrapper for SupplierCatalogueView
 */
public class SupplierView extends VBox {

    public SupplierView() {
        // Just load the catalogue view which has all the functionality
        SupplierCatalogueView catalogueView = new SupplierCatalogueView();
        getChildren().add(catalogueView);
    }

}
package com.pharmacy.iposca.ui;

import javafx.scene.layout.VBox;

public class SupplierView extends VBox {

    public SupplierView() {
        SupplierCatalogueView catalogueView = new SupplierCatalogueView();
        getChildren().add(catalogueView);
    }
}
package com.itc.catalogueservice.exception.catalogue;

public class NoTopSellingProductsException extends CatalogueException {

    public NoTopSellingProductsException() {
        super("No top selling products found");
    }
}
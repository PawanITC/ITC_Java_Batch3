package com.itc.catalogueservice.exception.catalogue;


public class NoProductsException extends CatalogueException {

    public NoProductsException() {
        super("No products available");
    }

}
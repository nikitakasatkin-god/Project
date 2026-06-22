package org.example.model;

public enum ProductType {
    BRANDED("Брендовая продукция"),
    NON_BRANDED("Небрендовая продукция");

    private final String displayName;

    ProductType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
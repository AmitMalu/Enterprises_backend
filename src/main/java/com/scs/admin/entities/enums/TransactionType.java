package com.scs.admin.entities.enums;

public enum TransactionType {
    CREDIT, DEBIT;

    public static TransactionType fromString(String type) {
        for (TransactionType t : TransactionType.values()) {
            if (t.name().equalsIgnoreCase(type)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Invalid transaction type: " + type);
    }
}

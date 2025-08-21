package com.im.admin.entities.enums;

public enum WalletType {
    PREPAID, POSTPAID, GULLAK, BOTH;

    public static WalletType fromString(String type) {
        for(WalletType wt : WalletType.values()) {
            if (wt.name().equalsIgnoreCase(type)) {
                return wt;
            }

        }
        throw new IllegalArgumentException("Invalid wallet type: " + type);
    }
}


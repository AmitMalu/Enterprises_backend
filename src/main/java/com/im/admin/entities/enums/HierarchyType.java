package com.im.admin.entities.enums;

public enum HierarchyType {
    ENTERPRISE("ET"),
    WHITELABEL("WL"),
    SUPER_DISTRIBUTOR("SD"),
    PARTNER("PT"),
    DISTRIBUTOR("DT"),
    RETAILER("RT");

    private final String code;

    HierarchyType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static HierarchyType fromId(String parentId) {
        for (HierarchyType type : HierarchyType.values()) {
            if (parentId.contains("-" + type.getCode() + "-")) {
                return type;
            }
        }
        return null; // If no match found
    }
}


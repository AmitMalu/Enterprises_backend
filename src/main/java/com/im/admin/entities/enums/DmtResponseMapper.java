package com.im.admin.entities.enums;

import java.util.Set;

public enum DmtResponseMapper {
    SUCCESS(Set.of(1)),
    PENDING(Set.of(2, 3, 6,4)),
    FAILED(Set.of(0, 5, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25));

    private final Set<Integer> codes;

    DmtResponseMapper(Set<Integer> codes) {
        this.codes = codes;
    }

//    public static String map(int code) {
//        for (DmtResponseMapper type : values()) {
//            if (type.codes.contains(code)) {
//                return type.name();
//            }
//        }
//        return "PENDING"; // default
//    }


    public static String map(int code, String message) {

        //  Special handling for response_code = 1
        if (code == 1) {
            if (message != null &&
                    message.toLowerCase().contains("successful")) {
                return "SUCCESS";
            } else {
                return "FAILED"; // refunded / invalid / partial etc
            }
        }

        for (DmtResponseMapper type : values()) {
            if (type.codes.contains(code)) {
                return type.name();
            }
        }

        return "PENDING";
    }
}

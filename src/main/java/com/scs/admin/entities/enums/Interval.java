package com.scs.admin.entities.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum Interval {
    DY, MO, QT, HY, YR, OT;

    @JsonCreator
    public static Interval fromValue(String value) {
        switch (value.toUpperCase()) {
            case "DA":
            case "DY": return DY;
            case "MO": return MO;
            case "QT": return QT;
            case "HY": return HY;
            case "YR": return YR;
            case "OT": return OT;
            default:
                throw new IllegalArgumentException("Invalid Interval: " + value);
        }
    }
}
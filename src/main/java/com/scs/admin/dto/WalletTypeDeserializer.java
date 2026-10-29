package com.scs.admin.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.scs.admin.entities.enums.WalletType;

import java.io.IOException;

public class WalletTypeDeserializer extends JsonDeserializer<WalletType> {
    @Override
    public WalletType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText();
        if (value == null || value.trim().isEmpty()) {
            return null; // empty string becomes null
        }
        try {
            return WalletType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null; // invalid value becomes null
        }
    }
}

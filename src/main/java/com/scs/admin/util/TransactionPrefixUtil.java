package com.scs.admin.util;

import java.util.Set;

public class TransactionPrefixUtil {

    private static final Set<String> TRANSACTION_PREFIXES = Set.of("TXN", "FCR", "FDR", "FTR");
    private static final Set<String> WALLET_PREFIXES = Set.of("CHG", "COM", "TDS", "GST");

    public static boolean isTransaction(String txnId) {
        return txnId != null && txnId.length() >= 3 &&
                TRANSACTION_PREFIXES.contains(txnId.substring(0, 3).toUpperCase());
    }

    public static boolean isWallet(String txnId) {
        return txnId != null && txnId.length() >= 3 &&
                WALLET_PREFIXES.contains(txnId.substring(0, 3).toUpperCase());
    }
}


package com.scs.admin.commons;

/**
 * @author SHUBHAM KHOPADE
 */
public class VendorContext {

    // Track whether service layer executed
    private static final ThreadLocal<Boolean> serviceCalled = new ThreadLocal<>();

    // Store vendorId separately
    private static final ThreadLocal<String> vendorHolder = new ThreadLocal<>();

    public static void markServiceCalled() {
        serviceCalled.set(true);
    }

    public static boolean isServiceCalled() {
        return Boolean.TRUE.equals(serviceCalled.get());
    }

    public static void clear() {
        serviceCalled.remove();
    }

    public static void setVendorId(String vendorId) {
        vendorHolder.set(vendorId);
    }

    public static String getVendorId() {
        return vendorHolder.get();
    }

}

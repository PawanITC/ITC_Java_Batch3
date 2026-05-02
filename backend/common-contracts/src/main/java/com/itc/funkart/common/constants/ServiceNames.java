package com.itc.funkart.common.constants;

/**
 * <h2>Service Registry Constants</h2>
 * <p>
 * This class maintains the official identifiers for all microservices within the
 * Funkart ecosystem. By using these constants, we ensure consistency across the
 * API Gateway, Service Discovery, and Kafka consumers.
 * </p>
 *
 * <p>
 * <b>JVM Note:</b> These strings are stored in the <i>String Constant Pool</i>
 * within the Method Area (Metaspace), allowing all services to reference the same
 * memory address for these identifiers.
 * </p>
 */
public final class ServiceNames {

    /** Identifier for the User and Authentication service. */
    public static final String USER_SERVICE = "user-service";

    /** Identifier for the Catalog and Inventory service. */
    public static final String PRODUCT_SERVICE = "product-service";

    /** Identifier for the Checkout and Order management service. */
    public static final String ORDER_SERVICE = "order-service";

    /** Identifier for the Payment processing service. */
    public static final String PAYMENT_SERVICE = "payment-service";

    /**
     * Private constructor to prevent instantiation of this utility class.
     * @throws UnsupportedOperationException if an attempt is made to create an instance.
     */
    private ServiceNames() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
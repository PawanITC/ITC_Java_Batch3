package com.itc.funkart.gateway.config;

import com.itc.funkart.common.constants.ServiceNames;
import org.springframework.stereotype.Component;

/**
 * <h2>Service Registry Abstraction</h2>
 * <p>
 * Defines a contract for resolving downstream microservice base URLs.
 * This avoids hardcoded string keys scattered across the codebase and
 * provides a single source of truth for service discovery within the gateway.
 * </p>
 */
public interface ServiceRegistry {

    /**
     * @return Base URL of the User Service
     */
    String userService();

    /**
     * @return Base URL of the Payment Service
     */
    String paymentService();

    /**
     * @return Base URL of the Order Service
     */
    String orderService();

    /**
     * @return Base URL of the Order Service
     */
    String productService();

    /**
     * <h2>Default AppConfig-backed implementation</h2>
     * <p>
     * Resolves service URLs from {@link AppConfig#services()} map.
     * This implementation is the Spring-managed production instance.
     * </p>
     */
    @Component
    class AppServiceRegistry implements ServiceRegistry {
        private final AppConfig config;

        public AppServiceRegistry(AppConfig config) {
            this.config = config;
        }

        // Using the common constant instead of a hardcoded string
        @Override
        public String userService() {
            return config.getServiceUrl(ServiceNames.USER_SERVICE);
        }

        @Override
        public String paymentService() {
            return config.getServiceUrl(ServiceNames.PAYMENT_SERVICE);
        }

        @Override
        public String productService() {
            return config.getServiceUrl(ServiceNames.PRODUCT_SERVICE);
        }

        @Override
        public String orderService() {
            return config.getServiceUrl(ServiceNames.ORDER_SERVICE);
        }
    }
}
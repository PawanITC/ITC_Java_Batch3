package com.itc.funkart.assignments.sunil;

public class LiskovSubstitutionPrinciple {

    // Abstraction for delivery options
    interface DeliveryOption {
        void deliver(String product);
    }

    // Home delivery implementation
   static class HomeDelivery implements DeliveryOption {
        @Override
        public void deliver(String product) {
            System.out.println("Delivering " + product + " to customer's home...");
        }
    }

    // Store pickup implementation
    static class StorePickup implements DeliveryOption {
        @Override
        public void deliver(String product) {
            System.out.println("Product " + product + " is ready for pickup at the store...");
        }
    }

    // Locker delivery implementation (new option)
   static class LockerDelivery implements DeliveryOption {
        @Override
        public void deliver(String product) {
            System.out.println("Product " + product + " delivered to Amazon Locker...");
        }
    }

    // Context class using DeliveryOption
    static class Order {
        private String product;
        private DeliveryOption deliveryOption;

        public Order(String product, DeliveryOption deliveryOption) {
            this.product = product;
            this.deliveryOption = deliveryOption;
        }

        public void processOrder() {
            System.out.println("Processing order for: " + product);
            deliveryOption.deliver(product);
        }
    }

    // Demo
    public class LSPDemo {
        public static void main(String[] args) {
            Order homeOrder = new Order("Laptop", new HomeDelivery());
            homeOrder.processOrder();

            Order pickupOrder = new Order("Book", new StorePickup());
            pickupOrder.processOrder();

            Order lockerOrder = new Order("Headphones", new LockerDelivery());
            lockerOrder.processOrder();
        }
    }
}

package com.itc.funkart.assignments.sunil;

public class DependencyInversionPrinciple {

    // Abstraction for payment processors
    interface PaymentProcessor {
        void pay(double amount);
    }

    // Concrete implementations
   static class CreditCardPayment implements PaymentProcessor {
        @Override
        public void pay(double amount) {
            System.out.println("Paid " + amount + " via Credit Card");
        }
    }

   static  class UpiPayment implements PaymentProcessor {
        @Override
        public void pay(double amount) {
            System.out.println("Paid " + amount + " via UPI");
        }
    }

   static class NetBankingPayment implements PaymentProcessor {
        @Override
        public void pay(double amount) {
            System.out.println("Paid " + amount + " via Net Banking");
        }
    }

    // High-level module depends on abstraction
    static class OrderService {
        private PaymentProcessor paymentProcessor;

        // Inject dependency via constructor
        public OrderService(PaymentProcessor paymentProcessor) {
            this.paymentProcessor = paymentProcessor;
        }

        public void placeOrder(String product, double amount) {
            System.out.println("Placing order for: " + product);
            paymentProcessor.pay(amount);
        }
    }

    // Demo
    public class DIPDemo {
        public static void main(String[] args) {
            OrderService order1 = new OrderService(new CreditCardPayment());
            order1.placeOrder("Laptop", 1200.00);

            OrderService order2 = new OrderService(new UpiPayment());
            order2.placeOrder("Smartphone", 800.00);

            OrderService order3 = new OrderService(new NetBankingPayment());
            order3.placeOrder("Headphones", 150.00);
        }
    }
}

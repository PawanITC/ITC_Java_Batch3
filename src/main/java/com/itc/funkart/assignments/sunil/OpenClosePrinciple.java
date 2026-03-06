package com.itc.funkart.assignments.sunil;

public class OpenClosePrinciple {

    // Abstraction for discount strategies
    interface DiscountStrategy {
        double apply(double amount);
    }

    // Concrete implementations
    static class PrimeMemberDiscount implements DiscountStrategy {
        @Override
        public double apply(double amount) {
            return amount * 0.85; // 15% off
        }
    }

   static class FestivalDiscount implements DiscountStrategy {
        @Override
        public double apply(double amount) {
            return amount * 0.90; // 10% off
        }
    }

    // Another example: No discount
   static class NoDiscount implements DiscountStrategy {
        @Override
        public double apply(double amount) {
            return amount; // no discount applied
        }
    }

    // Context class that uses the strategy
    static class CheckoutService {
        private DiscountStrategy discountStrategy;

        // Inject discount strategy (Dependency Inversion)
        public CheckoutService(DiscountStrategy discountStrategy) {
            this.discountStrategy = discountStrategy;
        }

        public double checkout(double amount) {
            return discountStrategy.apply(amount);
        }
    }

    // Demo
    public class OCPDemo {
        public static void main(String[] args) {
            double amount = 1000;

            CheckoutService primeCheckout = new CheckoutService(new PrimeMemberDiscount());
            System.out.println("Prime Member Final Amount: " + primeCheckout.checkout(amount));

            CheckoutService festivalCheckout = new CheckoutService(new FestivalDiscount());
            System.out.println("Festival Discount Final Amount: " + festivalCheckout.checkout(amount));

            CheckoutService noDiscountCheckout = new CheckoutService(new NoDiscount());
            System.out.println("No Discount Final Amount: " + noDiscountCheckout.checkout(amount));
        }
    }
}

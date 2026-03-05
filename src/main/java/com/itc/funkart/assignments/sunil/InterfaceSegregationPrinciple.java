package com.itc.funkart.assignments.sunil;

public class InterfaceSegregationPrinciple {

    // Separate interfaces for different customer actions
    interface Searchable {
        void search(String query);
    }

    interface Purchasable {
        void purchase(Product product);
    }

    interface Reviewable {
        void review(Product product, String review);
    }

    // Entity class
   static class Product {
        private String name;
        private double price;

        public Product(String name, double price) {
            this.name = name;
            this.price = price;
        }

        public String getName() { return name; }
        public double getPrice() { return price; }
    }

    // Customer can search, purchase, and review
   static class Customer implements Searchable, Purchasable, Reviewable {
        @Override
        public void search(String query) {
            System.out.println("Customer searching for: " + query);
        }

        @Override
        public void purchase(Product product) {
            System.out.println("Customer purchasing: " + product.getName());
        }

        @Override
        public void review(Product product, String review) {
            System.out.println("Customer reviewing " + product.getName() + ": " + review);
        }
    }

    // Guest can only search
   static class Guest implements Searchable {
        @Override
        public void search(String query) {
            System.out.println("Guest searching for: " + query);
        }
    }

    // Demo
    public class ISPDemo {
        public static void main(String[] args) {
            Product laptop = new Product("Laptop", 1200);

            Customer customer = new Customer();
            customer.search("Smartphone");
            customer.purchase(laptop);
            customer.review(laptop, "Great performance and battery life!");

            Guest guest = new Guest();
            guest.search("Books");
        }
    }
}

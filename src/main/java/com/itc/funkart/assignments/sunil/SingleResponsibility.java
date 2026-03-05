package com.itc.funkart.assignments.sunil;

public class SingleResponsibility {

    // Entity class: represents product data only
    class Product {
        private String name;
        private double price;

        public Product(String name, double price) {
            this.name = name;
            this.price = price;
        }

        public String getName() { return name; }
        public double getPrice() { return price; }
    }

    // Handles persistence (saving product data)
    class ProductRepository {
        void save(Product product) {
            System.out.println("Saving product '" + product.getName() + "' to database...");
            // actual DB logic here
        }
    }

    // Handles notifications (sending emails)
    class NotificationService {
        void sendEmail(String message) {
            System.out.println("Sending email: " + message);
            // actual email logic here
        }
    }

    // Demo class to show usage

        public static void main(String[] args) {
            Product product = new Product("Laptop", 1200.00);

            ProductRepository repo = new ProductRepository();
            repo.save(product);

            NotificationService notify = new NotificationService();
            notify.sendEmail("Order placed for " + product.getName());
        }
}
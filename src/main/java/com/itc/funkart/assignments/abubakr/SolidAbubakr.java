package com.itc.funkart.assignments.abubakr;

// 1️ SRP - Shape only represents a shape
interface Shape {
    double calculateArea();
}


// 2️ LSP - Subtypes can replace Shape
class Circle implements Shape {
    private double radius;

    public Circle(double radius) {
        this.radius = radius;
    }

    public double calculateArea() {
        return Math.PI * radius * radius;
    }
}


class Rectangle implements Shape {
    private double width;
    private double height;

    public Rectangle(double width, double height) {
        this.width = width;
        this.height = height;
    }

    public double calculateArea() {
        return width * height;
    }
}


// 3️ OCP + DIP
class AreaCalculator {

    // Depends on abstraction (Shape) → DIP
    public double calculateTotalArea(Shape[] shapes) {

        double total = 0;

        for (Shape shape : shapes) {
            // Open for extension → OCP
            total += shape.calculateArea();
        }

        return total;
    }
}

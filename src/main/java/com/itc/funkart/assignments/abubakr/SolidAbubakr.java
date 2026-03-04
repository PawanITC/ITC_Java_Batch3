package com.itc.funkart.assignments.abubakr;

// 1️ SRP - Shape only represents a shape,  no unneccasry implementations
interface Shape {
    double calculateArea();
}

interface Corners{//isp, interface segragtion, not all shapes have corners, so will not combine shape/corners methods into single interface
    int numberOfCorners();
}


// 2️ LSP - Subtypes can replace Shape without causing logic errors
class Circle implements Shape {
    private double radius;

    public Circle(double radius) {
        this.radius = radius;
    }

    public double calculateArea() {
        return Math.PI * radius * radius;
    }
}


class Rectangle implements Shape, Corners {
    private double width;
    private double height;

    public Rectangle(double width, double height) {
        this.width = width;
        this.height = height;
    }

    public double calculateArea() {
        return width * height;
    }

    public int numberOfCorners(){
        return 4;
    }
}


// 3️ OCP + DIP
class AreaCalculator {

    // Depends on abstraction (Shape) → DIP
    public double calculateTotalArea(Shape[] shapes) {

        double total = 0;

        for (Shape shape : shapes) {
            // Open for extension → OCP, now if we have new shape we can define the calculateArea, rather than modifying a single calculateArea method
            total += shape.calculateArea();
        }

        return total;
    }
}

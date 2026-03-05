package com.itc.funkart.assignments.abbas.solidprinciples;

public class Main {

    public static void main(String[] args) {

        Animal cat = new Cat();
        Animal dog = new Dog();

        AnimalPrinter printer = new AnimalPrinter();

        printer.printAnimalNoise(cat);
        printer.printAnimalNoise(dog);

        Zoo zoo = new Zoo(cat);
        zoo.makeAnimalSound();
    }

}

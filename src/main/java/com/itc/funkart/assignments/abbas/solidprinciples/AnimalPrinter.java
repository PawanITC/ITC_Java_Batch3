package com.itc.funkart.assignments.abbas.solidprinciples;

public class AnimalPrinter {

    // SRP: this class ONLY prints animal sounds
    public void printAnimalNoise(Animal animal){
        System.out.println(animal.makeNoise());
    }

}

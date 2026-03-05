package com.itc.funkart.assignments.abbas.solidprinciples;

public class Zoo {

    private final Animal animal;

    // Dependency Injection
    public Zoo(Animal animal){
        this.animal = animal;
    }

    public void makeAnimalSound(){
        System.out.println(animal.makeNoise());
    }
}

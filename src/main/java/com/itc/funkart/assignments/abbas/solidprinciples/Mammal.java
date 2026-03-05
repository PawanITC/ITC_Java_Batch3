package com.itc.funkart.assignments.abbas.solidprinciples;

public abstract class Mammal implements Animal {

    @Override
    public String makeNoise(){
        return "Generic Mammal Sound";
    }

    @Override
    public void move(int steps){
        System.out.println("A mammal moves in leaps and bounds!");
    }
}

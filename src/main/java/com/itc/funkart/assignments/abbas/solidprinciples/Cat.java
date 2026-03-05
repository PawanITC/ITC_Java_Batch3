package com.itc.funkart.assignments.abbas.solidprinciples;

public class Cat extends Mammal {

    @Override
    public String makeNoise() {
        return "Miaow!";
    }

    @Override
    public void move(int steps) {
        for(int i=1;i<=steps;i++){
            System.out.println("Cat sprints quietly!");
        }
    }
}

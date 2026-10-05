package com.cinema.ai;

public class EmailObserver implements Observer{
    @Override
    public void update(String status) {
        System.out.println("Email update: " + status);
    }
}

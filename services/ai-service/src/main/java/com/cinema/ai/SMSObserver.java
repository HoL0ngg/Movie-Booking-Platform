package com.cinema.ai;

public class SMSObserver implements Observer{
    @Override
    public void update(Subject s) {
        s.subcribe();
        System.out.println("SMS update: " + status);
    }
}

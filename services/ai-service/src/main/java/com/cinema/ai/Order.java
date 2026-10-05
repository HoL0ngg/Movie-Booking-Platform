package com.cinema.ai;

import java.util.List;

public class Order implements Subject{
    private List<Observer> list;

    @Override
    public void subcribe(Observer observer) {
        this.list.add(observer);
    }

    @Override
    public void unsubcribe(Observer o) {
        this.list.remove((o);
    }

    @Override
    public void notifyObserver() {

    }
}

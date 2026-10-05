package com.cinema.ai;

public interface Subject {
    void subcribe(Observer o);
    void unsubcribe(Observer o);
    void notifyObserver();
}

// URL: https://leetcode.com/problems/traffic-light-controlled-intersection/description/?envType=problem-list-v2&envId=concurrency
package com.vinay.concurrency;

public class TrafficLight {

    private boolean aGreen = true;

    public TrafficLight() {}

    public void carArrived(
            int carId,           // ID of the car
            int roadId,          // ID of the road the car travels on. Can be 1 (road A) or 2 (road B)
            int direction,       // Direction of the car
            Runnable turnGreen,  // Use turnGreen.run() to turn light to green on current road
            Runnable crossCar    // Use crossCar.run() to make car cross the intersection
    ) {
        boolean dirIsA = roadId == 1 ? true : false;
        synchronized (this) {
            if (dirIsA ^ aGreen) {
                turnGreen.run();
                aGreen = !aGreen;
            }
            crossCar.run();
        }
    }
}

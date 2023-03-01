package org.example;

import java.lang.management.ManagementFactory;


public class Main {


    public static void main(String[] args) throws InterruptedException {

        IBMMQSource source = new IBMMQSource();
        source.start();

        System.out.println("ProcessID: "+ ManagementFactory.getRuntimeMXBean().getName());

        Thread.sleep(1000000000);
    }


}
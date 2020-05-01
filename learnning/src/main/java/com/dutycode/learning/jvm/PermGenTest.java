package com.dutycode.learning.jvm;

import java.util.concurrent.TimeUnit;

/**
 * @author zhangzhonghua
 * @version 0.0.1
 * @date 2020-04-29
 */
public class PermGenTest {

    private final static int ONE_MB = 1024*1024;

    /**
     * JVM参数： -xmx20m -xms20m -xmn10m -XX:MaxPermSize=30m -XX:PermSize=30m -XX:PretenureSizeThreshold=100
     * @param args
     */
    public static void main(String[] args) {


        //连续申请3M内存，
        for (int i =0 ; i < 100; i ++){
            allocateMemory(3);
            try {
                TimeUnit.MILLISECONDS.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


    private static void allocateMemory(int size){

        for (int i =0 ; i < size ; i++){
            byte[] bytes = new byte[ONE_MB];
        }

    }

}
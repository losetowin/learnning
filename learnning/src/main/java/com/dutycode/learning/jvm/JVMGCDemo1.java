package com.dutycode.learning.jvm;

import java.util.concurrent.TimeUnit;

/**
 * 测试指定GC参数对堆大小的影响
 * @author zhangzhonghua
 * @version 0.0.1
 * @date 2020-04-13
 */
public class JVMGCDemo1 {

    private static final int ONE_MB = 1024*1024;
    private static final int FIVE_MB = 5 * 1024*1024;



    public static void main(String[] args) {
        new Thread(new Runnable(){

            public void run() {
                while (true){
                    try {

                        TimeUnit.SECONDS.sleep(5);

                        System.out.println("通知系统GC。。。");
                        System.gc();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }


            }
        }).start();


        for (int i = 0; i < 10000; i++){
            //申请20M Young空间
            allocateYoungSpace(20);
            //申请20M 老年代空间
            allocateOldSpace(20);

            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }




    }




    /**
     * 申请Young空间占用
     * @param mSize 单位MB
     */
    private static void allocateYoungSpace(int mSize){

        System.out.println("Allcate YoungSpace , size = " + mSize + "MB");

        for (int i = 0; i < mSize; i++){

            byte[] memBytes = new byte[ONE_MB];

        }

        System.out.println("Allcate YongSpace done");

    }

    /**
     * 申请Old空间占用。 需要配合XX:PretenureSizeThreshold参数，表示超过设置大小之后，对象直接放到老年代。
     * 本程序，我们设定参数的值为3M，即对象超过3M就会进入到老年代
     * @param mSize
     */
    private static void allocateOldSpace(int mSize){
        System.out.println("Allcate OldSpace , size = " + mSize + "MB");

        for (int i = 0; i < mSize/5 ; i++){
            byte[] memBytes = new byte[FIVE_MB];
        }

        if (mSize % 5 != 0 ){
            byte[] additionMem = new byte[mSize % 5 * ONE_MB];
        }

        System.out.println("Allcate OldSpace done");


    }

}




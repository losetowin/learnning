package com.dutycode.learning.dm;

import sun.misc.Unsafe;
import sun.nio.ch.DirectBuffer;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author zhangzhonghua
 * @version 0.0.1
 * @date 2020-04-21
 */
public class DirectMemoryDemo2 {


    private static final int ONE_MB = 1024 * 1024 ;
    public static void main(String[] args) {


        new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        //分配300M空间
//                        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(150 * ONE_MB);
//                        System.out.println("使用ByteBuffer申请直接内存完成，大小150M");

                        printDirectMemory();
                        //使用UnSafe申请内存
                        try {
                            Field f = Unsafe.class.getDeclaredField("theUnsafe");
                            f.setAccessible(true);
                            Unsafe unsafe = (Unsafe)f.get(null);
                            long address = unsafe.allocateMemory(150 * ONE_MB);
                            unsafe.setMemory(address, 150 * ONE_MB, (byte)0);
                            System.out.println("使用Unsafe申请直接内存，大小150M");

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
        ).start();




        try {
            TimeUnit.MINUTES.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }



    private static void printDirectMemory(){
        Class c = null;
        try {
            c = Class.forName("java.nio.Bits");
            Field maxMemory = c.getDeclaredField("maxMemory");
            maxMemory.setAccessible(true);
            Field reservedMemory = c.getDeclaredField("reservedMemory");
            reservedMemory.setAccessible(true);
            synchronized (c) {
                Long maxMemoryValue = (Long) maxMemory.get(null);
                AtomicLong reservedMemoryValue = (AtomicLong)reservedMemory.get(null);
                System.out.println("maxMemoryValue = " + maxMemoryValue + ", reservedMemoryValue=" + reservedMemoryValue.intValue());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}

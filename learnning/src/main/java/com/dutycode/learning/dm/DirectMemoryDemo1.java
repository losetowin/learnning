package com.dutycode.learning.dm;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author zhangzhonghua
 * @version 0.0.1
 * @date 2020-04-21
 */
public class DirectMemoryDemo1 {


    private static final int ONE_MB = 1024 * 1024;

    public static void main(String[] args) {


        int totalSize = 0;
        while(true){

            //使用UnSafe申请内存
            try {
                int size = 1000 * ONE_MB;
                Field f = Unsafe.class.getDeclaredField("theUnsafe");
                f.setAccessible(true);
                Unsafe unsafe = (Unsafe) f.get(null);
                long address = unsafe.allocateMemory(size);
//                unsafe.setMemory(address, size, (byte) 0);
                totalSize += size/ONE_MB;
                System.out.println("address=" + address + ",size =" + totalSize + "MB");
//                printDirectMemory();

                TimeUnit.MILLISECONDS.sleep(2000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }



    }


    private static void printDirectMemory() {
        Class c = null;
        try {
            c = Class.forName("java.nio.Bits");
            Field maxMemory = c.getDeclaredField("maxMemory");
            maxMemory.setAccessible(true);
            Field reservedMemory = c.getDeclaredField("reservedMemory");
            reservedMemory.setAccessible(true);
            synchronized (c) {
                Long maxMemoryValue = (Long) maxMemory.get(null);
                AtomicLong reservedMemoryValue = (AtomicLong) reservedMemory.get(null);
                System.out.println("maxMemoryValue = " + maxMemoryValue + ", reservedMemoryValue=" + reservedMemoryValue.intValue());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}

package com.dutycode.learning.jvm;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 测试指定GC参数对堆大小的影响
 * @author zhangzhonghua
 * @version 0.0.1
 * @date 2020-04-13
 */
public class JVMGCDemo2 {

    private static final int ONE_MB = 1024*1024;

    static  volatile List  list = new ArrayList();
    public static void main(String[] args) {

        //指定要生产的对象大小为512m
        int count = 256;

        //新建一条线程，负责生产对象
        new Thread(() -> {
            try {
                for (int i = 1; i <= 3; i++) {
                    System.out.println(String.format("第%s次生产%s大小的对象", i, count));
                    addObject(list, count);
                    //休眠40秒
                    TimeUnit.SECONDS.sleep(15);
                }
                //最后list清空，释放内存。
                list.clear();

                System.out.println("调用system.gc触发FullGc");
//                //等待2秒后，调用System.gc触发一次FullGC。触发收缩
                System.gc();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();


        //新建一条线程，负责清理list，回收jvm内存
        new Thread(() -> {
            for (;;) {
                if (list.size() >= count) {
                    System.out.println("清理list.... ");
                    list.clear();
                    //打印堆内存信息
                    printJvmMemoryInfo();
                }
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        //阻止程序退出
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


    public static void addObject(List list,int count){
        for (int i = 0; i < count; i++) {
            OOMobject ooMobject = new OOMobject(1);
            //向list添加一个1m的对象
            list.add(ooMobject);
            try {
                //休眠100毫秒
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

public static class OOMobject{
    //生成1m的对象
    private byte[] bytes=null;


    public OOMobject(){
        bytes = new byte[ONE_MB];
    }

    public OOMobject(int size){
        bytes = new byte[size*ONE_MB];
    }

}

    public static void printJvmMemoryInfo() {
        // 虚拟机级内存情况查询
        Runtime rt = Runtime.getRuntime();
        long vmTotal = rt.totalMemory() / ONE_MB;
        long vmFree = rt.freeMemory() / ONE_MB;
        long vmMax = rt.maxMemory() / ONE_MB;
        long vmUse = vmTotal - vmFree;
        System.out.println("");
        System.out.println("JVM内存已用的空间为：" + vmUse + " MB");
        System.out.println("JVM内存的空闲空间为：" + vmFree + " MB");
        System.out.println("JVM总内存空间为：" + vmTotal + " MB");
        System.out.println("JVM总内存最大堆空间为：" + vmMax + " MB");
        System.out.println("");
    }
}




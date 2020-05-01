package com.dutycode.learning.dm;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @author zhangzhonghua
 * @version 0.0.1
 * @date 2020-04-21
 */
public class DirectMemoryDemo {


    private static final int ONE_B = 1;
    private static final int ONE_MB = 1000*1000 ;


    private static final String DEFAULT_PATH = "/opt/tmp/dm/";

    public static void main(String[] args) {


        int initTestTimes = 100000;

//        for (int i = 1; i < 10; i++) {
//
//            int testTimes = i * initTestTimes;
//
//            CostVo vo = testCost(testTimes);
//            System.out.println(testTimes + "," + vo.toString());
//
//            CostVo writeCost = testWriteCost(testTimes);
//            System.out.println(testTimes + " " + writeCost.toString());
//
//            CostVo writeIOCost = testWriteCostWithIO(new File("/opt/wf/tmp/test.mobi"));
//            System.out.println(i + " " + writeIOCost.toString());
//
//        }


        try {
            TimeUnit.SECONDS.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //测试多线程下IO操作性能
        for (int i = 0 ; i < 50; i++){
            new Thread(new Runnable(){

                @Override
                public void run() {
                    CostVo writeIOCost = testWriteCostWithIO(new File("/opt/wf/tmp/test.mobi"));
                    System.out.println( writeIOCost.toString());
                }
            }).start();
        }


        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


    /**
     * 测试申请性能
     * @param testTimes
     * @return
     */
    private static CostVo testCost(int testTimes) {
        //测试申请性能
        //堆内存申请
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < testTimes; i++) {

            ByteBuffer.allocate(ONE_B);

        }
        long heapCost = System.currentTimeMillis() - startTime;

        //直接内存申请
        startTime = System.currentTimeMillis();
        for (int i = 0; i < testTimes; i++) {

            ByteBuffer.allocateDirect(ONE_B);

        }

        long directCost = System.currentTimeMillis() - startTime;

        CostVo vo = new CostVo(heapCost, directCost);
        return vo;
    }

    /**
     * 测试读写性能,无IO
     *
     * @param testTimes
     * @return
     */
    private static CostVo testWriteCost(int testTimes) {
        //申请堆内存空间
        ByteBuffer heapBuffer = ByteBuffer.allocate(ONE_B * 4 * testTimes);

        //堆内存写
        long heapCost = getReadWriteCost(heapBuffer, testTimes);

        //申请直接内存空间
        ByteBuffer directBuffer = ByteBuffer.allocateDirect(ONE_B * 4 * testTimes);

        //直接内存写
        long directCost = getReadWriteCost(directBuffer, testTimes);

        CostVo vo = new CostVo(heapCost, directCost);
        return vo;
    }

    /**
     * 测试读写性能,有IO
     *
     * @return
     */
    private static CostVo testWriteCostWithIO(File f ) {
        //申请堆内存空间
        ByteBuffer heapBuffer = ByteBuffer.allocate(ONE_MB);

        //堆内存读写
        long heapCost = getReadWriteCostWithIO(f, heapBuffer);

        //申请直接内存空间
        ByteBuffer directBuffer = ByteBuffer.allocateDirect(ONE_MB);

        //直接内存读写
        long directCost = getReadWriteCostWithIO(f, directBuffer);

        CostVo vo = new CostVo(heapCost, directCost);
        return vo;
    }


    private static long getReadWriteCost(ByteBuffer byteBuffer, int testTimes) {
        long startTime = System.currentTimeMillis();
        //写
        for (int i = 0; i < testTimes; i++) {
            byteBuffer.putChar('a');
        }
        //将缓冲区翻转
        byteBuffer.flip();

        //读
        for (int i = 0; i < testTimes; i++) {
            byteBuffer.getChar(i);
        }

        long cost = System.currentTimeMillis() - startTime;

        return cost;

    }

    private static long getReadWriteCostWithIO(File f, ByteBuffer byteBuffer) {

        File fout = new File(DEFAULT_PATH  + new Random().nextInt()  +"out_.txt");
        FileInputStream fis = null;
        FileOutputStream fos = null;

        long startTime = System.currentTimeMillis();
        try {

            //读文件
            fis = new FileInputStream(f);
            FileChannel finChanel = fis.getChannel();
            fos = new FileOutputStream(fout);
            FileChannel channel = fos.getChannel();

            int length = -1;
            while((length=finChanel.read(byteBuffer)) != -1 ){
                finChanel.read(byteBuffer);
                byteBuffer.flip();

                //写文件
                channel.write(byteBuffer);
                byteBuffer.clear();

            }

            finChanel.close();
            channel.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fis != null ){
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fos != null){
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


        long cost = System.currentTimeMillis() - startTime;

        return cost;

    }

}

class CostVo {
    private long heapCost;
    private long directCost;

    public CostVo(long heapCost, long directCost) {
        this.heapCost = heapCost;
        this.directCost = directCost;
    }

    public long getHeapCost() {
        return heapCost;
    }

    public void setHeapCost(long heapCost) {
        this.heapCost = heapCost;
    }

    public long getDirectCost() {
        return directCost;
    }

    public void setDirectCost(long directCost) {
        this.directCost = directCost;
    }

    @Override
    public String toString() {
        return heapCost + " " + directCost;
    }
}

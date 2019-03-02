package com.example.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class NIOClient2 {
    private static Charset charset = Charset.forName("UTF-8");
    private static CharsetDecoder charsetDecoder = charset.newDecoder();

    public static void main(String[] args) {
        NIOClient2 client2 = new NIOClient2();
        client2.test();
    }

    public void test() {
        ExecutorService pool = new ThreadPoolExecutor(3, 5, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(20));
        for (int i = 0; i < 1000; i++) {
            pool.execute(new TestThread(i));
        }
    }

    private class TestThread implements Runnable {
        private Integer c;

        public TestThread(Integer c) {
            this.c = c;
        }

        @Override
        public void run() {
            try (SocketChannel sc = SocketChannel.open()) {
                boolean connected = sc.connect(new InetSocketAddress("localhost", 12345));
                long id = Thread.currentThread().getId();
                System.out.println(id + " " + c + " client connected " + connected);
                byte[] bytes = (id + " " + c + " write").getBytes();
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                int count = sc.write(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

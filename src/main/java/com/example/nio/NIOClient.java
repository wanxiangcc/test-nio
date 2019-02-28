package com.example.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NIOClient {
    private static int POOL_SIZE = 10;
    private static InetSocketAddress ADDRESS = new InetSocketAddress("localhost", 12345);
    private static CharsetEncoder ENCODER = Charset.forName("UTF-8").newEncoder();

    private static class DownloadFile implements Runnable {
        private int index;
        public DownloadFile(int index) {
            this.index = index;
        }
        @Override
        public void run() {
            long start = System.currentTimeMillis();
            try {
                SocketChannel client = SocketChannel.open();
                client.configureBlocking(false);
                Selector selector = Selector.open();
                client.register(selector, SelectionKey.OP_CONNECT);
                client.connect(ADDRESS);
                ByteBuffer buffer = ByteBuffer.allocate(8 * 1024);
                int total = 0;
                while (true) {
                    boolean isExit = false;
                    selector.select();
                    Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                    while (it.hasNext()) {
                        SelectionKey selectionKey = (SelectionKey) it.next();
                        it.remove();
                        if (selectionKey.isConnectable()) {
                            SocketChannel channel = (SocketChannel) selectionKey.channel();
                            if (channel.isConnectionPending()) {
                                channel.finishConnect();
                            }
                            channel.write(ENCODER.encode(CharBuffer.wrap("Hello From " + index)));
                            channel.register(selector, SelectionKey.OP_READ);
                        } else if (selectionKey.isReadable()) {
                            SocketChannel channel = (SocketChannel) selectionKey.channel();
                            int count = channel.read(buffer);
                            if (count > 0) {
                                total += count;
                                buffer.clear();
                            } else {
                                channel.close();
                                isExit = true;
                                break;
                            }
                        }
                    }
                    if (isExit) {
                        break;
                    }
                }
                double last = (System.currentTimeMillis() - start) * 1.0 / 1000;
                System.out.println("Thread " + index + " downloaded " + total + " bytes in " + last + "seconds.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        ExecutorService pool = Executors.newFixedThreadPool(POOL_SIZE);
        for (int i = 0; i < POOL_SIZE; i++) {
            pool.execute(new DownloadFile(i));
        }
        pool.shutdown();
    }
}
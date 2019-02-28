package com.example.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NIOServer2 {
    private final static int port = 9000;

    public static void main(String[] args) {
        NIOServer2 server2 = new NIOServer2();
        try {
            server2.listen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void listen() throws IOException {
        Selector selector = Selector.open();
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(port));
        // 设置为非阻塞模式
        serverSocketChannel.configureBlocking(false);
        // 注册accept的selector
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        // 连接计数
        int connectionCount = 0;
        // 处理请求的线程数
        int threads = 3;
        ExecutorService pools = Executors.newFixedThreadPool(threads);
        while (true) {
            int readyCount = selector.select();
            if (readyCount == 0) {
                continue;
            }
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey selectionKey = iterator.next();
                if (selectionKey.isAcceptable()) {
                    // 有通道是accept就绪
                    ServerSocketChannel channel = (ServerSocketChannel) selectionKey.channel();
                    // 接受连接
                    SocketChannel socketChannel = channel.accept();
                    socketChannel.configureBlocking(false);
                    // 注册read channel
                    socketChannel.register(selector, SelectionKey.OP_READ, ++connectionCount);
                } else if (selectionKey.isConnectable()) {
                    // connected
                } else if (selectionKey.isReadable()) {
                    // read 此时由线程池去处理业务
                    pools.execute(new SocketProcess(selectionKey));
                    // 防止线程处理不及时，重复选择
                    selectionKey.cancel();
                } else if (selectionKey.isWritable()) {

                }
                iterator.remove();
            }
        }
    }

    private class SocketProcess implements Runnable {
        private SelectionKey selectionKey;

        public SocketProcess(SelectionKey selectionKey) {
            this.selectionKey = selectionKey;
        }

        @Override
        public void run() {

        }
    }
}

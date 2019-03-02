package com.example.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NIOServer2 {
    private final static int port = 12345;
    private static Charset charset = Charset.forName("UTF-8");
    private static CharsetDecoder charsetDecoder = charset.newDecoder();

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
            try {
                System.out.println("channel attach " + selectionKey.attachment() + " channel data " + readChannelData());
                // 处理完之后关闭，也可往channel中写入数据
                selectionKey.channel().close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public String readChannelData() throws IOException {
            SocketChannel sc = (SocketChannel) selectionKey.channel();
            int byteSize = 1024;
            // allocateDirect 直接分配内存
            ByteBuffer buffer = ByteBuffer.allocateDirect(byteSize);
            // 由于不知道传入的数据有多大，声明一个更大的buffer
            ByteBuffer bigBuffer = null;
            // 循环读取byteSize的次数
            int count = 0;
            while (sc.read(buffer) != -1) {
                count++;
                // 先处理之前的数据
                ByteBuffer temp = ByteBuffer.allocateDirect((count + 1) * byteSize);
                if (bigBuffer != null) {
                    // 把bigBuffer转为读模式
                    bigBuffer.flip();
                    temp.put(bigBuffer);
                }
                bigBuffer = temp;

                // 将本次读取的数据放入bigBuffer
                buffer.flip();
                bigBuffer.put(buffer);

                // 为下一次循环，清空buffer的数据
                buffer.clear();
            }
            if (bigBuffer != null) {
                bigBuffer.flip();
                return charsetDecoder.decode(bigBuffer).toString();
            }
            return null;
        }
    }
}

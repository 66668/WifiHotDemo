package com.sjy.wificlient;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * 监听线程
 */
public class ListenerThread extends Thread {
    private static final String TAG = "SJY";

    private ServerSocket serverSocket = null;
    private Handler handler;
    private int     port;
    private Socket socket;

    public ListenerThread(int port, Handler handler) {
        setName("ListenerThread");
        this.port = port;
        this.handler = handler;
        try {
            serverSocket = new ServerSocket(port);//监听本机的12345端口
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void run() {
        while (true) {
            try {
                //阻塞，等待设备连接
                if (serverSocket != null)
                    socket = serverSocket.accept();
                Message message = Message.obtain();
                message.what = MainActivity.DEVICE_CONNECTING;
                handler.sendMessage(message);
            } catch (IOException e) {
                Log.i(TAG, "error:" + e.getMessage());
                e.printStackTrace();
                interrupt();
            }
        }
    }

    public Socket getSocket() {
        return socket;
    }

    public void close() {
        try {
            if (serverSocket != null)
                serverSocket.close();
            if (socket != null)
                socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

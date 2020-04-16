package com.sjy.wifihot.wifimanager;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.sjy.wifihot.MainActivity;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

//监听线程
public class ListenerThread extends Thread {
    private Handler mHandler;
    private ServerSocket serverSocket = null;
    private Socket socket;

    public ListenerThread(int port, Handler mHandler) {
        this.mHandler = mHandler;
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        super.run();
        while (true) {
            try {
                //阻塞，等待设备连接
                if (serverSocket != null)
                    socket = serverSocket.accept();
                Message message = Message.obtain();
                message.what = MainActivity.DEVICE_CONNECTING;
                mHandler.sendMessage(message);
            } catch (IOException e) {
                Log.i("ListennerThread", "error:" + e.getMessage());
                e.printStackTrace();
            }
        }

    }
}

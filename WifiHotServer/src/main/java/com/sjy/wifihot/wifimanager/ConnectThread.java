package com.sjy.wifihot.wifimanager;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.sjy.wifihot.MainActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

//监听线程
public class ConnectThread extends Thread {
    private Socket socket;
    private Handler nHandler;
    private InputStream inputStream;
    private OutputStream outputStream;
    Context context;

    public ConnectThread(Context context, Socket socket, Handler handler) {
        setName("ConnectThread");
        this.socket = socket;
        this.nHandler = handler;
        this.context = context;
    }

    @Override
    public void run() {
        super.run();
        if (socket == null) {
            return;
        }
        nHandler.sendEmptyMessage(MainActivity.DEVICE_CONNECTED);
        try {
            //获取数据流
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();

            byte[] buffer = new byte[1024];
            int bytes;
            while (true) {
                //读取数据
                bytes = inputStream.read(buffer);
                if (bytes > 0) {
                    final byte[] data = new byte[bytes];
                    System.arraycopy(buffer, 0, data, 0, bytes);

                    Message message = Message.obtain();
                    message.what = MainActivity.GET_CLIENT_MSG;
                    Bundle bundle = new Bundle();
                    bundle.putString("MSG", new String(data));//数据
                    message.setData(bundle);
                    nHandler.sendMessage(message);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                outputStream.close();
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }


    }
}

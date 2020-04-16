package com.sjy.wificlient;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.sjy.wificlient.base.PermissionsActivity;

import java.io.IOException;
import java.net.Socket;

/**
 * c端 连接热点
 * 简单测试
 * 不查找当前连接状态
 */

public class MainActivity extends PermissionsActivity {
    private static final String TAG = "SJY";

    public static final int DEVICE_CONNECTING = 1;//有设备正在连接热点
    public static final int DEVICE_CONNECTED = 2;//有设备连上热点
    public static final int SEND_MSG_SUCCSEE = 3;//发送消息成功
    public static final int SEND_MSG_ERROR = 4;//发送消息失败
    public static final int GET_SERVER_MSG = 6;//获Server取新消息

    private Button btn_start;
    private Button btn_send;
    private TextView tv_content;

    //============================wifi配置 变量============================
    private WifiManager mWifiManager;
    private String WIFIHOT_PS = "666777888";
    private String WIFIHOT_NAME = "DashYagi";//随便起名了
    private String wifiIP = "192.168.43.1";
    private int wifiPort = 1503;
    StringBuilder builder;
    ListenerThread listenerThread;
    ConnectThread connectThread;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DEVICE_CONNECTING:
//                    connectThread = new ConnectThread(listenerThread.getSocket(), mHandler);
//                    connectThread.start();
                    break;
                case DEVICE_CONNECTED:
                    Log.d(TAG, "设备连接成功:\n");
                    builder.append("设备连接成功:\n");
                    tv_content.setText(builder.toString());
                    break;
                case SEND_MSG_SUCCSEE:
                    Log.d(TAG, "发送消息成功:" + msg.getData().getString("MSG"));
                    builder.append("发送消息成功:" + msg.getData().getString("MSG"));
                    tv_content.setText(builder.toString());
                    break;
                case SEND_MSG_ERROR:
                    Log.d(TAG, "发送消息失败:" + msg.getData().getString("MSG"));
                    builder.append("发送消息失败:" + msg.getData().getString("MSG"));
                    tv_content.setText(builder.toString());
                    break;
                case GET_SERVER_MSG:
                    builder.append("收到消息:" + msg.getData().getString("MSG"));
                    tv_content.setText(builder.toString());
                    break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn_start = findViewById(R.id.btn_start);
        btn_send = findViewById(R.id.btn_send);
        tv_content = findViewById(R.id.tv_content);
        builder = new StringBuilder();
        builder.append("c端准备连接热点... \n");

        mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        initWifi();
        initListener();
        createConnectThread();

    }

    @Override
    public void onPermissionsOkay() {

    }

    private void initWifi() {
        //检查Wifi状态
        if (!mWifiManager.isWifiEnabled())
            mWifiManager.setWifiEnabled(true);
        String name = mWifiManager.getConnectionInfo().getSSID();
        String ip = CWifiUtils.getIp(mWifiManager);
        String aRoute = CWifiUtils.getWifiRouteIPAddress(this);//小心内存泄漏
        builder.append("已连接到" + name + "\n");
        builder.append("ip=" + ip + "\n");
        builder.append("路由=" + aRoute + "\n");
        tv_content.setText(builder.toString());
    }

    private void initListener() {
        btn_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initWifi();
            }
        });
        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectThread.sendData("我是客户端发送的信息");
            }
        });

    }

    //开启连接socket和监听线程
    private void createConnectThread() {
        //监听
        listenerThread = new ListenerThread(wifiPort, mHandler);
        listenerThread.start();

        //连接
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Socket socket = new Socket(CWifiUtils.getWifiRouteIPAddress(MainActivity.this), wifiPort);
                    connectThread = new ConnectThread(socket, mHandler);
                    connectThread.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

}

package com.sjy.wifihot;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.sjy.wifihot.base.PermissionsActivity;
import com.sjy.wifihot.wifimanager.ConnectThread;
import com.sjy.wifihot.wifimanager.ListenerThread;
import com.sjy.wifihot.wifimanager.WifiUtils;

import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.Map;

/**
 * wifi热点 路由端/服务端
 * <p>
 * <p>
 * 优化点：可以使用IntentService实现热点的打开关闭，比单纯Thread处理效果好
 */
public class MainActivity extends PermissionsActivity {

    private static final String TAG = "SJY";
    public static final int DEVICE_CONNECTING = 1;//有设备正在连接热点
    public static final int DEVICE_CONNECTED = 2;//有设备连上热点
    public static final int SEND_MSG_SUCCSEE = 3;//发送消息成功
    public static final int SEND_MSG_ERROR = 4;//发送消息失败
    public static final int GET_CLIENT_MSG = 6;//获取client新消息

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
    private final Handler mHandler = new Handler();
    private String mWifiIPAddress;//获取匹配值
    private ConnectThread connectThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn_start = findViewById(R.id.btn_start);
        btn_send = findViewById(R.id.btn_send);
        tv_content = findViewById(R.id.tv_content);
        builder = new StringBuilder();
        builder.append("等待热点开始... \n");
        mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        btn_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (openWifiHot()) {
                    builder.append("热点已打开，... \n");
                    tv_content.setText(builder.toString());
                    //开启监听线程，监听client端的成功回调
                    new ListenerThread(wifiPort, mHandler).start();
                    //server端发送socket连接,并开启通讯线程
                    createServerSocket();
                } else {
                    builder.append("热点打开失败，... \n");
                    tv_content.setText(builder.toString());
                }
            }
        });
        findViewById(R.id.btn_end).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (WifiUtils.closeWifiHot(mWifiManager, MainActivity.this)) {
                    builder.append("热点已关闭 \n");
                    tv_content.setText(builder.toString());
                }
            }
        });

    }

    @Override
    protected void onPermissionsOkay() {
    }


    //打开wifi
    private boolean openWifiHot() {
        if (WifiUtils.openWifiHot(mWifiManager, this, WIFIHOT_NAME, WIFIHOT_PS)) {
            Log.i(TAG, "打开热点成功。");
            return true;
        } else {
            Log.e(TAG, "打开热点失败。");
            WifiUtils.closeWifiHot(mWifiManager, this);
            return false;
        }
    }

    //开启socket通讯
    private void createServerSocket() {
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    //匹配热点
                    List<Map<String, String>> ips = WifiUtils.getIPv4Address();
                    for (Map<String, String> ip : ips) {
                        if (ip.get("name").startsWith("wlan")) {
                            mWifiIPAddress = ip.get("ipv4");
                        }
                    }
                    if (TextUtils.isEmpty(mWifiIPAddress)) {
                        mHandler.postDelayed(this, 2000);
                        return;
                    }
                    Log.d(TAG, "热点Ip：" + mWifiIPAddress);
                    //本地路由开启通信
                    Socket socket = new Socket(wifiIP, wifiPort);//192.168.43.1:1503的socket
                    connectThread = new ConnectThread(MainActivity.this, socket, mHandler);
                    connectThread.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();

    }
}

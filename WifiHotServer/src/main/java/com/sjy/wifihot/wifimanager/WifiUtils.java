package com.sjy.wifihot.wifimanager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.dx.stock.ProxyBuilder;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WifiUtils {
    private static final String TAG = "SJY";

    public static boolean openWifiHot(WifiManager mWifimanager, Context context, String name, String ps) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {//8.0+适配
            return turnOnWifiByNewApi(mWifimanager, context, name, ps);
        } else {//老版本适配
            return turnOnWifiByOldApi(mWifimanager, name, ps);
        }
    }

    /**
     * 9.0+开启热点
     * 开启热点的方式是通过ConnectivityManager的startTethering方法来开启的
     *
     * @param mWifimanager
     * @param context
     * @param name
     * @param ps
     * @return
     */
    private static boolean turnOnWifiByNewApi(WifiManager mWifimanager, Context context, String name, String ps) {
        ConnectivityManager mConnectivityManager = context.getSystemService(ConnectivityManager.class);
        if (isTetherActive(mConnectivityManager)) {
            Log.d(TAG, "Tether already active, returning");
            return false;
        }

        //dexmaker实现预加载（AOP）
        File outputDir = context.getCodeCacheDir();
        Object proxy;
        try {
            proxy = ProxyBuilder.forClass(OnStartTetheringCallbackClass())
                    .dexCache(outputDir).handler(new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            switch (method.getName()) {
                                case "onTetheringStarted":
                                    Log.d(TAG, "onTetheringStarted");
//                                    MyOnStartTetheringCallback.onTetheringStarted();
                                    break;
                                case "onTetheringFailed":
                                    Log.d(TAG, "onTetheringFailed");
//                                    MyOnStartTetheringCallback.onTetheringFailed();
                                    break;
                                default:
                                    ProxyBuilder.callSuper(proxy, method, args);
                            }
                            return null;
                        }

                    }).build();

        } catch (Exception e) {
            Log.e(TAG, "dexmaker实现预加载Error:" + e.toString());
            return false;
        }

        try {
            Method startTethering = mConnectivityManager.getClass().getDeclaredMethod("startTethering", int.class, boolean.class, OnStartTetheringCallbackClass(), Handler.class);
            if (startTethering == null) {
                Log.e(TAG, "startTetheringMethod is null");
            } else {
                startTethering.invoke(mConnectivityManager, ConnectivityManager.TYPE_MOBILE, false, proxy, null);
                Log.d(TAG, "startTethering invoked");
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error in enableTethering:" + e.toString());
            return false;
        }

    }

    /**
     * 8.0+
     *
     * @return true if a tethered device is found, false if not found
     */
    public static boolean isTetherActive(ConnectivityManager mConnectivityManager) {
        try {
            Method method = mConnectivityManager.getClass().getDeclaredMethod("getTetheredIfaces");
            if (method == null) {
                Log.e(TAG, "getTetheredIfaces is null");
            } else {
                String res[] = (String[]) method.invoke(mConnectivityManager, null);
                Log.d(TAG, "getTetheredIfaces invoked");
                Log.d(TAG, Arrays.toString(res));
                if (res.length > 0) {
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in getTetheredIfaces");
            e.printStackTrace();
        }
        return false;
    }

    private static Class OnStartTetheringCallbackClass() {
        try {
            return Class.forName("android.net.ConnectivityManager$OnStartTetheringCallback");
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "OnStartTetheringCallbackClass error: " + e.toString());
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 老版本wifi设置
     *
     * @param mWifimanager
     * @param name
     * @param ps
     * @return
     */
    private static boolean turnOnWifiByOldApi(WifiManager mWifimanager, String name, String ps) {
        if (mWifimanager.isWifiEnabled()) {
            mWifimanager.setWifiEnabled(false);
        }
        try {
            //
            WifiConfiguration config = new WifiConfiguration();
            config.SSID = name;
            config.preSharedKey = ps;
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);//开放系统认证
            config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            //
            Method setWifiApEnabled = mWifimanager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, Boolean.TYPE);

            boolean enable = (Boolean) setWifiApEnabled.invoke(mWifimanager, config, true);
            if (enable) {
                Log.d(TAG, "热点已开启 name:" + name + " password:666777888");
                Log.i(TAG, "打开WIFI");
                mWifimanager.setWifiEnabled(true);
                Log.i(TAG, "扫描WIFI热点");
                mWifimanager.startScan();
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


    //关闭热点
    public static boolean closeWifiHot(WifiManager mWifimanager, Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {//8.0+适配
            return turnOffWifiByNewApi(context);
        } else {//老版本适配
            return turnOffWifiByOldApi(mWifimanager);
        }
    }


    private static boolean turnOffWifiByNewApi(Context context) {
        ConnectivityManager mConnectivityManager = context.getSystemService(ConnectivityManager.class);
        try {
            Method method = mConnectivityManager.getClass().getDeclaredMethod("stopTethering", int.class);
            if (method == null) {
                Log.e(TAG, "stopTetheringMethod is null");
            } else {
                method.invoke(mConnectivityManager, ConnectivityManager.TYPE_MOBILE);
                Log.d(TAG, "stopTethering invoked");
            }
        } catch (Exception e) {
            Log.e(TAG, "stopTethering error: " + e.toString());
            e.printStackTrace();
        }
        return false;
    }


    private static boolean turnOffWifiByOldApi(WifiManager mWifimanager) {
        if (mWifimanager == null) {
            return false;
        }

        if (mWifimanager.isWifiEnabled()) {
            mWifimanager.setWifiEnabled(false);
        }

        try {
            Method wifiApConfiguration = mWifimanager.getClass().getMethod("getWifiApConfiguration");
            wifiApConfiguration.setAccessible(true);
            WifiConfiguration config = (WifiConfiguration) wifiApConfiguration.invoke(mWifimanager);
            Method setWifiApEnabled = mWifimanager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            setWifiApEnabled.invoke(mWifimanager, config, false);
            return true;
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
        return false;
    }


    public static List<Map<String, String>> getIPv4Address() {
        List<Map<String, String>> addresses = new ArrayList<Map<String, String>>();
        try {
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements()) {
                NetworkInterface nif = en.nextElement();
                Enumeration<InetAddress> inet = nif.getInetAddresses();
                while (inet.hasMoreElements()) {
                    InetAddress ip = inet.nextElement();
                    if (!ip.isLoopbackAddress() && ip instanceof Inet4Address) {
                        Map<String, String> addr = new HashMap<String, String>();
                        addr.put("name", nif.getName());
                        addr.put("ipv4", ip.getHostAddress());
                        addresses.add(addr);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "获取IP地址失败"+e.toString());
        }
        return addresses;
    }

    public String getWifiApIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                if (intf.getName().contains("wlan")) {
                    for (Enumeration<InetAddress> enumIpAddr = intf
                            .getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                        InetAddress inetAddress = enumIpAddr.nextElement();
                        if (!inetAddress.isLoopbackAddress()
                                && (inetAddress.getAddress().length == 4)) {
                            Log.d(TAG, inetAddress.getHostAddress());
                            return inetAddress.getHostAddress();
                        }
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e("Main", ex.toString());
        }
        return null;
    }

    /**
     * 适配9。0+ 获取SSID
     *
     * @param activity
     * @return
     */
    public String getWIFISSID(Context activity) {
        String ssid = "unknown id";

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.O || Build.VERSION.SDK_INT == Build.VERSION_CODES.P) {

            WifiManager mWifiManager = (WifiManager) activity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

            assert mWifiManager != null;
            WifiInfo info = mWifiManager.getConnectionInfo();

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                return info.getSSID();
            } else {
                return info.getSSID().replace("\"", "");
            }
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O_MR1) {

            ConnectivityManager connManager = (ConnectivityManager) activity.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            assert connManager != null;
            NetworkInfo networkInfo = connManager.getActiveNetworkInfo();
            if (networkInfo.isConnected()) {
                if (networkInfo.getExtraInfo() != null) {
                    return networkInfo.getExtraInfo().replace("\"", "");
                }
            }
        }
        return ssid;
    }


    /**
     * 打开移动网络
     *
     * @param context
     * @param enabled 是否打开
     */
    public static void setMobileDataState(Context context, boolean enabled) {
        TelephonyManager telephonyService = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        try {
            Method setDataEnabled = telephonyService.getClass().getDeclaredMethod("setDataEnabled", boolean.class);
            if (null != setDataEnabled) {
                setDataEnabled.invoke(telephonyService, enabled);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

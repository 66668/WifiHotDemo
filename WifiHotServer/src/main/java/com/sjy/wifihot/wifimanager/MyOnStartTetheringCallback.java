package com.sjy.wifihot.wifimanager;

/**
 * 8.0+ 自定义 监听回调
 * startTethering 是隐藏的方法，并且第三个参数OnStartTetheringCallback是ConnectivityManager内部抽象类，也是隐藏的
 * 自定义一个回调，监听热点打开情况
 */
public abstract class MyOnStartTetheringCallback {
    /**
     * Called when tethering has been successfully started.
     */
    public abstract void onTetheringStarted();

    /**
     * Called when starting tethering failed.
     */
    public abstract void onTetheringFailed();

}
package com.wtt.service;

import android.accessibilityservice.AccessibilityService;
import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.wtt.R;

import java.util.List;

/**
 * Created by Administrator on 2017/2/6.
 */

public class AuxiliaryService extends AccessibilityService {

    SharedPreferences sharedPreferences;
    private static boolean open = false;
    private static boolean my = false;

    @Override
    protected void onServiceConnected() {
      /*  AccessibilityServiceInfo info = getServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_SPOKEN;
        info.notificationTimeout = 100;
        setServiceInfo(info);
//        我们在代码中注册多个应用的包名，从而可以监听多个应用
        info.packageNames = new String[]{"xxx.xxx.xxx", "yyy.yyy.yyy","...."};
//        我们在onAccessibilityEvent事件监听的方法中做包名的过滤(这种方式最常用)
//        String pkgName = event.getPackageName().toString();
//        if("xxx.xxx.xxx".equals(pkgName)){
//        }else if("yyy.yyy.yyy".equals(pkgName)){
//        }else if("....".equals(pkgName)){
//        }
        setServiceInfo(info);*/
        super.onServiceConnected();
        sharedPreferences = getSharedPreferences("MM_MONEY" , Context.MODE_PRIVATE);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int eventType = event.getEventType();
        switch (eventType){
            //第一步：监听通知栏消息
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                //.......
                List<CharSequence> texts = event.getText();
                for (CharSequence text : texts){
                    String content = text.toString();
                    Log.e("AuxiliaryService", "text:"+content);
                    if(content.contains(getString(R.string.mm_money))){
                        open = true;
                        //模拟打开通知栏消息
                        if (event.getParcelableData() != null&&event.getParcelableData() instanceof Notification) {
                            Notification notification = (Notification) event.getParcelableData();
                            PendingIntent pendingIntent = notification.contentIntent;
                            try {
                                pendingIntent.send();
                            } catch (PendingIntent.CanceledException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                break;
            //第二步：监听是否进入微信红包消息界面
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                //......
                String className = event.getClassName().toString();
                Log.e("AuxiliaryService", "className:"+className);
                if(className.equals(getResources().getString(R.string.mm_launcherUI))){
                    Log.e("AuxiliaryService", "开始抢红包");
                    //开始抢红包
                    getPacket();
                }else if(className.equals(getResources().getString(R.string.mm_moneyReceiveUI))){
                    Log.e("AuxiliaryService", "开始打开红包");
                    //开始打开红包
                    openPacket();
                } else if (className.equals("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI")) {
//                    close();
                    open = false;
                    my = false;
                } else if(className.equals("com.tencent.mm.plugin.wallet.pay.ui.WalletPayUI")) {
                    open = false;
                    my = true;
                }
                break;
            default:
                break;
        }
    }

    /**
     * 关闭红包详情界面,实现自动返回聊天窗口
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void close() {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            //为了演示,直接查看了关闭按钮的id
            List<AccessibilityNodeInfo> infos = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/gw");

            for (AccessibilityNodeInfo item : infos) {
                item.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
                break;
            }
        }
    }

    private void saveOpenCount(){
        int count = sharedPreferences.getInt("count", 0);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("count", count+1);
        editor.commit();
    }

    /**
     * 打开红包
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void openPacket() {
        open = false;
        my = false;
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {

            List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByViewId("com.tencent.mm:id/bi3");
            if(list!=null&&list.size()>0) {
                for (AccessibilityNodeInfo n : list) {
                    n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    saveOpenCount();
                    break;
                }
            }else{
                Log.e("AuxiliaryService", "openPacket：手慢了没抢到");
            }

        }else{
            Log.e("AuxiliaryService", "openPacket:红包没打开");
        }

    }

    /**
     * 模拟点击,打开抢红包界面
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void getPacket() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if(open) {

            List<AccessibilityNodeInfo> list = rootNode.findAccessibilityNodeInfosByText(getResources().getString(R.string.mm_getMoney));
            if(list!=null&&list.size()>0) {
                AccessibilityNodeInfo parent = list.get(list.size()-1).getParent();
                while (parent != null) {
                    if (parent.isClickable()) {
                        parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        break;
                    }
                    parent = parent.getParent();
                }
                open = false;
            }
        }else{
            if(my) {
                List<AccessibilityNodeInfo> list = rootNode.findAccessibilityNodeInfosByText(getString(R.string.mm_MoneyLook));
                if (list != null && list.size() > 0) {
                    AccessibilityNodeInfo parent = list.get(list.size() - 1).getParent();
                    while (parent != null) {
                        if (parent.isClickable()) {
                            parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            break;
                        }
                        parent = parent.getParent();
                    }
                    my = false;
                }
            }else {
                Log.e("AuxiliaryService", "非通知栏进入不抢红包");
            }
        }


      /*  AccessibilityNodeInfo node = recycle(rootNode);

        node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        AccessibilityNodeInfo parent = node.getParent();
        while (parent != null) {
            if (parent.isClickable()) {
                parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                break;
            }
            parent = parent.getParent();
        }*/

    }

    /**
     * 递归查找当前聊天窗口中的红包信息
     *
     * 聊天窗口中的红包都存在"领取红包"一词,因此可根据该词查找红包
     *
     * @param node
     */
    public AccessibilityNodeInfo recycle(AccessibilityNodeInfo node) {
        if (node.getChildCount() == 0) {
            if (node.getText() != null) {
                if (node.getText().toString().equals("领取红包")) {
                    return node;
                }
            }
        } else {
            for (int i = 0; i < node.getChildCount(); i++) {
                if (node.getChild(i) != null) {
                    recycle(node.getChild(i));
                }
            }
        }
        return node;
    }

    @Override
    public void onInterrupt() {

    }

}

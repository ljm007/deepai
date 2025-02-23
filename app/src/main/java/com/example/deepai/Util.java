package com.example.deepai;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class Util {
    private static final String TAG = "AppListUtil";

    /**
     * 获取已安装应用列表（过滤系统应用）
     */
    public static List<AppInfo> getInstalledApps(Context context) {
        List<AppInfo> appList = new ArrayList<>();
        PackageManager pm = context.getPackageManager();

        // 获取安装包列表
        List<PackageInfo> packages = pm.getInstalledPackages(PackageManager.GET_ACTIVITIES);

        // 遍历所有应用
        for (PackageInfo packageInfo : packages) {
            // 过滤系统应用（根据 flag 判断）
            if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) == 0) {
                AppInfo appInfo = new AppInfo();
                appInfo.packageName = packageInfo.packageName;
                appInfo.appName = packageInfo.applicationInfo.loadLabel(pm).toString();
                appInfo.versionName = packageInfo.versionName;
                appInfo.versionCode = packageInfo.versionCode;
                appInfo.installTime = new Date(packageInfo.firstInstallTime);
                appInfo.updateTime = new Date(packageInfo.lastUpdateTime);
                // 获取启动 Activity 信息
                appInfo.launchActivity = getLaunchActivity(pm, packageInfo);
                Log.d(TAG, "getInstalledApps: " + appInfo.appName + " pkg:" + packageInfo.packageName);
                appList.add(appInfo);
            }
        }

        // 按安装时间排序
        Collections.sort(appList, (o1, o2) -> o2.installTime.compareTo(o1.installTime));

        return appList;
    }

    /**
     * 获取应用的启动 Activity
     */
    private static String getLaunchActivity(PackageManager pm, PackageInfo packageInfo) {
        Intent launchIntent = pm.getLaunchIntentForPackage(packageInfo.packageName);
        if (launchIntent != null && launchIntent.getComponent() != null) {
            return launchIntent.getComponent().getClassName();
        }
        return null;
    }

    // 核心转换方法
    public static String bitmapToBase64(Bitmap bitmap) {
        if (bitmap == null) return "";
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.WEBP, 60, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        String ret = Base64.encodeToString(byteArray, Base64.DEFAULT);
        bitmap.recycle();
        return ret;
    }

    public static void updateImage() {

    }

    public static String parseUIHierarchy(AccessibilityNodeInfo root) {
        StringBuilder sb = new StringBuilder();
        sb.append("<hierarchy>\n");
        if (root != null) {
            buildUIHierarchy(root, sb, 0);
            root.recycle(); // 回收根节点
        }
        sb.append("</hierarchy>");
        return sb.toString();
    }

    private static void buildUIHierarchy(AccessibilityNodeInfo node, StringBuilder sb, int level) {
        if (node == null) return;

        // 生成缩进
        String indent = new String(new char[level * 2]).replace('\0', ' ');

        // 节点属性
        sb.append(indent)
                .append("<node")
                .append(" text:\"").append(node.getText() != null ? node.getText().toString() : "").append("\"")
                .append(" resource-id:\"").append(node.getViewIdResourceName() != null ? node.getViewIdResourceName() : "").append("\"")
                .append(" bounds:\"").append(getBoundsString(node)).append("\"")
                .append(" clickable:\"").append(node.isClickable()).append("\"")
                .append(" long-clickable:\"").append(node.isLongClickable()).append("\"")
                .append(" selected:\"").append(node.isSelected()).append("\"");
        if (node.getContentDescription() != null) {
            sb.append(" content_desc:\"").append(node.getContentDescription()).append("\"");
        }
        sb.append(">\n");

        // 递归处理子节点
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null && child.isVisibleToUser()) {
                buildUIHierarchy(child, sb, level + 1);
                child.recycle(); // 及时回收子节点
            }
        }
        sb.append(indent).append("</node>\n");
    }

    private static String getBoundsString(AccessibilityNodeInfo node) {
        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        return String.format("[%d,%d][%d,%d]",
                bounds.left, bounds.top, bounds.right, bounds.bottom);
    }


    /**
     * 应用信息模型类
     */
    public static class AppInfo {
        public String packageName;
        public String appName;
        public String versionName;
        public int versionCode;
        public Date installTime;
        public Date updateTime;
        public String launchActivity;

        @Override
        public String toString() {
            return appName + ":" + packageName + ";";
        }
    }

}

package com.example.deepai;

import static android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK;
import static android.view.accessibility.AccessibilityNodeInfo.ACTION_FOCUS;
import static android.view.accessibility.AccessibilityNodeInfo.ACTION_LONG_CLICK;
import static android.view.accessibility.AccessibilityNodeInfo.ACTION_SET_TEXT;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import com.example.deepai.api.ModelClient;
import com.example.deepai.api.OssClient;
import com.example.deepai.bean.Action;
import com.example.deepai.bean.ActionParameters;
import com.example.deepai.bean.ImageAnalysisResult;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Locale;

public class AutoService extends AccessibilityService {


    private final String TAG = "AutoService";
    private static final int ONGOING_NOTIFICATION_ID = 1111;
    private static AutoService mInstance;

    private final Gson mGson;
    private final Handler mHandler;
    private int width;
    private int height;
    private int dpi;
    private ImageReader mImageReader;
    private VirtualDisplay mVirtualDisplay;

    private int mTaskStatus;//0：开始；1：结束
    private boolean isForeground;
    private Notification mNotification;
    private MediaProjection mMediaProjection;
    private ModelClient.CompletionCallback mCompletionCallback;

    public AutoService() {
        mInstance = this;
        mGson = new Gson();
        mHandler = new Handler();
        mTaskStatus = 0;
    }

    public static AutoService getInstance() {
        return mInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate() called");
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String CHANNEL_ID = "my_channel_01";
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "服务", NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("AI手机助手");
        manager.createNotificationChannel(channel);
        mNotification = new Notification.Builder(this, CHANNEL_ID).setSmallIcon(R.drawable.icon).setContentIntent(pendingIntent).setContentTitle("AI手机助手").setContentText("主人，我来了").setTicker("主人，我来了").build();

        DisplayManager displayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        Display display = displayManager.getDisplay(Display.DEFAULT_DISPLAY);
        Point size = new Point();
        display.getRealSize(size);
        width = size.x;
        height = size.y;
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        dpi = metrics.densityDpi;
        OssClient.initOSS();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
        Log.d(TAG, "onInterrupt() called");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy() called");
        stopForeground(true);
    }

    public void setMediaProjection(MediaProjection mMediaProjection) {
        this.mMediaProjection = mMediaProjection;
        getScreenBitmap();
    }


    public boolean isForeground() {
        return isForeground;
    }

    public void startForeground() {
        isForeground = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(ONGOING_NOTIFICATION_ID, mNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else {
            startForeground(ONGOING_NOTIFICATION_ID, mNotification);
        }
    }

    public void stopForeground() {
        isForeground = false;
        stopForeground(true);
    }

    public void registerAIlistener(ModelClient.CompletionCallback callback) {
        mCompletionCallback = callback;
    }

    public void sendToLLM(String userMessage) {
        Log.d("ljm", "sendToLLM() called with: userMessage = [" + userMessage + "]");
        boolean isCleanContext = false;
        if (mTaskStatus == 1) {
            mTaskStatus = 0;
            isCleanContext = true;
        }
        ModelClient.sendToLLM(userMessage, new ModelClient.CompletionCallback() {
            @Override
            public void onSuccess(String actionJson) {
                Log.d("ljm", "sendToLLM onSuccess() called with: actionJson = [" + actionJson + "]");
                if (mCompletionCallback != null) {
                    mCompletionCallback.onSuccess(actionJson);
                }
                if (!TextUtils.isEmpty(actionJson)) {
                    try {
                        Action action = mGson.fromJson(actionJson, Action.class);
                        handleAction(action);
                    } catch (Exception e) {
                        Log.e(TAG, "onSuccess: 解析json错误；", e);
                    }
                }
            }

            @Override
            public void onFailure(ModelClient.APIException exception) {
                if (mCompletionCallback != null) {
                    mCompletionCallback.onFailure(exception);
                }
            }
        }, isCleanContext);
    }

    @SuppressLint("WrongConstant")
    public Bitmap getScreenBitmap() {
        Log.d(TAG, "getScreenBitmap() called");
        // 创建ImageReader
        if (mImageReader == null) {
            mImageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
        }
        Surface surface = mImageReader.getSurface();
        if (mMediaProjection != null && mVirtualDisplay == null) {
            // 创建VirtualDisplay
            mVirtualDisplay = mMediaProjection.createVirtualDisplay("ScreenCapture", width, height, dpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, surface, null, null);
        }
        // 读取Image
        Image image = mImageReader.acquireLatestImage();
        if (image != null) {
            Image.Plane[] planes = image.getPlanes();
            ByteBuffer buffer = planes[0].getBuffer();
            int pixelStride = planes[0].getPixelStride();
            int rowStride = planes[0].getRowStride();
            int rowPadding = rowStride - pixelStride * width;

            // 创建Bitmap
            Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(buffer);

            // 裁剪Bitmap到正确的尺寸
            Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height);
            image.close();
            return croppedBitmap;
        }
        return null;
    }


    // 保存到应用私有目录（不需要权限）
    private File saveToPrivateStorage(Bitmap bitmap) throws IOException {
        Log.d(TAG, "saveToPrivateStorage() called with: bitmap = [" + bitmap + "]");
        File directory = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "screenshots");
        if (!directory.exists() && !directory.mkdirs()) {
            Log.d(TAG, "saveToPrivateStorage: 无法创建目录");
            return null;
        }
        File file = new File(directory, "temp.webp");
        if (file.exists()) {
            file.delete();
        }
        try (FileOutputStream fos = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            bitmap.recycle();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }

    private GestureDescription createClickGesture(List<List<Integer>> point, long duration) {
        Path path = new Path();
        int x, y;
        if (point.size() >= 2) {
            x = (point.get(0).get(0) + point.get(1).get(0)) / 2;
            y = (point.get(0).get(1) + point.get(1).get(1)) / 2;
        } else {
            x = point.get(0).get(0);
            y = point.get(0).get(1);
        }
        path.moveTo(x, y);
        return new GestureDescription.Builder().addStroke(new GestureDescription.StrokeDescription(path, 0, duration)).build();
    }

    private AccessibilityNodeInfo findNodeByResId(String resId) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root != null) {
            List<AccessibilityNodeInfo> nodeInfos = root.findAccessibilityNodeInfosByViewId(resId);
            if (nodeInfos.isEmpty()) {
                return null;
            } else {
                return nodeInfos.get(0);
            }
        }
        return null;
    }

    private void handleAction(Action action) {
        Log.d(TAG, "handleAction() called with: action = [" + action + "]");
        new Handler(Looper.getMainLooper()).post(() -> {
            String result = "已处理";
            try {
                switch (action.getAction()) {
                    case Action.CLICK:
                        result = handleClick(action.getParameters());
                        break;
                    case Action.DOUBLE_CLICK:
                        result = handleDoubleClick(action.getParameters());
                        break;
                    case Action.LONG_CLICK:
                        result = handleLongClick(action.getParameters());
                        break;
                    case Action.SWIPE:
                        result = handleSwipe(action.getParameters());
                        break;
                    case Action.INPUT:
                        result = handleInput(action.getParameters());
                        break;
                    case Action.LAUNCH_APP:
                        result = handleLaunchApp(action.getParameters());
                        break;
                    case Action.BACK:
                        performGlobalAction(GLOBAL_ACTION_BACK);
                        result = "已执行返回操作";
                        break;
                    case Action.HOME:
                        performGlobalAction(GLOBAL_ACTION_HOME);
                        result = "已返回桌面";
                        break;
                    case Action.RECENT_APPS:
                        performGlobalAction(GLOBAL_ACTION_RECENTS);
                        result = "已显示最近任务";
                        break;
                    case Action.SCROLL:
                        result = handleScroll(action.getParameters());
                        break;
                    case Action.SCREENSHOT_REQUEST:
                        handleScreenshotRequest(action.getParameters());
                        result = "";
                        break;
                    case Action.END:
                        result = "";
                        mTaskStatus = 1;
                        break;
                    case Action.ERROR:
                        showError(action.getParameters().message);
                        result = "错误：" + action.getParameters().message;
                        break;
                    default:
                        result = "未知操作类型: " + action.getAction();
                        Log.w(TAG, result);
                }
            } catch (Exception e) {
                String error = "执行失败: " + e.getMessage();
                result = error;
                Log.e(TAG, error, e);
                showError(error);
            }
            if (!TextUtils.isEmpty(result)) {
                if (result.contains("已启动")) {
                    String finalResult = result;
                    mHandler.postDelayed(() -> sendToLLM(finalResult), 2000);
                } else {
                    sendToLLM(result);
                }
            }
        });
    }

    private void handleScreenshotRequest(ActionParameters parameters) {
        Bitmap bitmap = getScreenBitmap();
        File file = null;
        try {
            file = saveToPrivateStorage(bitmap);
            OssClient.updateFile(file.getAbsolutePath(),
                    () -> sendImageModel(parameters, OssClient.getDownloadUrl()),
                    () -> sendToLLM("处理失败"));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void sendImageModel(ActionParameters parameters, String url) {
        Log.d("ljm", "sendImageModel() called with: parameters = [" + parameters + "], url = [" + url + "]");
        ModelClient.ImageAnalysisRequest request = new ModelClient.ImageAnalysisRequest(parameters.prompt, url);
        ModelClient.sendToImageAnalysis(request, new ModelClient.CompletionCallback() {
            @Override
            public void onSuccess(String json) {
                Log.d("ljm", "handleScreenshotRequest onSuccess : json = [" + json + "]");
                String result = "已处理";
                try {
                    if (mCompletionCallback != null) {
                        mCompletionCallback.onSuccess(json);
                    }

                    ImageAnalysisResult imageAnalysisResult = mGson.fromJson(json, ImageAnalysisResult.class);
                    if (TextUtils.equals(imageAnalysisResult.analysis_type, "negative")) {
                        result = "没有定位到:" + imageAnalysisResult.page_type;
                    } else {
                        ActionParameters actionParameters = new ActionParameters();
                        List<ImageAnalysisResult.Elements> elementsList = imageAnalysisResult.elements;
                        if (!TextUtils.equals(parameters.action_after_loc, Action.PARSE)) {
                            if (TextUtils.equals(imageAnalysisResult.analysis_type, "element")
                                    && elementsList != null
                                    && !elementsList.isEmpty()) {
                                actionParameters.coordinates = elementsList.get(0).coordinates;
                                actionParameters.direction = elementsList.get(0).gesture_relation;
                                switch (parameters.action_after_loc) {
                                    case Action.CLICK:
                                        result = handleClick(actionParameters);
                                        break;
                                    case Action.DOUBLE_CLICK:
                                        result = handleDoubleClick(actionParameters);
                                        break;
                                    case Action.LONG_CLICK:
                                        result = handleLongClick(actionParameters);
                                        break;
                                    case Action.SWIPE:
                                        result = handleSwipe(actionParameters);
                                        break;
                                    case Action.SCROLL:
                                        result = handleScroll(actionParameters);
                                        break;
                                    default:
                                        result = json;
                                        break;
                                }
                            } else {
                                result = "没有定位到";
                            }
                        } else {
                            result = json;
                        }

                    }
                } catch (Exception e) {
                    result = "处理失败:" + e.getMessage();
                }
                sendToLLM(result);
            }

            @Override
            public void onFailure(ModelClient.APIException exception) {
                Log.d(TAG, "handleScreenshotRequest onFailure() called with: exception = [" + exception + "]");
                if (mCompletionCallback != null) {
                    mCompletionCallback.onFailure(exception);
                }
                sendToLLM("处理失败:" + exception.getMessage());
            }
        });
    }


    private String handleClick(ActionParameters params) {
        String ret = "点击失败：";
        if (params.getResource_id() != null) {
            AccessibilityNodeInfo node = findNodeByResId(params.getResource_id());
            if (node != null) {
                node.performAction(ACTION_CLICK);
                node.recycle();
                return "已点击控件：" + params.getResource_id();
            }
            ret = ret + "未找到ID为 " + params.getResource_id() + " 的控件";
        }
        if (params.getTarget_text() != null) {
            AccessibilityNodeInfo node = findNodeByText(params.getTarget_text());
            if (node != null) {
                node.performAction(ACTION_CLICK);
                node.recycle();
                return "已点击文本：" + params.getTarget_text();
            }
            ret = ret + "未找到文本为 " + params.getTarget_text() + " 的控件";
        }
        if (params.getCoordinates() != null && !params.getCoordinates().isEmpty()) {
            dispatchGesture(createClickGesture(params.getCoordinates(), 10), null, null);
            return "已点击坐标：" + params.getCoordinates().toString();
        }
        return ret;
    }

    private String handleDoubleClick(ActionParameters params) {
        String firstClick = handleClick(params);
        if (!firstClick.contains("失败")) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                handleClick(params);
            }, 300);
            return "已双击：" + firstClick.split("：")[1];
        }
        return "双击失败：" + firstClick.split("：")[1];
    }

    private String handleLongClick(ActionParameters params) {
        String ret = "长按失败：";
        if (params.getResource_id() != null) {
            AccessibilityNodeInfo node = findNodeByResId(params.getResource_id());
            if (node != null) {
                node.performAction(ACTION_LONG_CLICK);
                node.recycle();
                return "已长按控件：" + params.getResource_id();
            }
            ret = ret + "未找到ID为 " + params.getResource_id() + " 的控件";
        }
        if (params.getTarget_text() != null) {
            AccessibilityNodeInfo node = findNodeByText(params.getTarget_text());
            if (node != null) {
                node.performAction(ACTION_CLICK);
                node.recycle();
                return "已长按文本：" + params.getTarget_text();
            }
            ret = ret + "未找到文本为 " + params.getTarget_text() + " 的控件";
        }
        if (params.getCoordinates() != null && !params.getCoordinates().isEmpty()) {
            dispatchGesture(createClickGesture(params.getCoordinates(), 500), null, null);
            return "已长按坐标：" + params.getCoordinates().toString();
        }
        return ret;
    }

    private String handleSwipe(ActionParameters params) {
        if (params.coordinates != null && params.getCoordinates().size() >= 2) {
            Path path = new Path();
            path.moveTo(params.getCoordinates().get(0).get(0), params.getCoordinates().get(0).get(1));
            path.lineTo(params.getCoordinates().get(1).get(0), params.getCoordinates().get(1).get(1));

            GestureDescription gesture = new GestureDescription.Builder().addStroke(new GestureDescription.StrokeDescription(path, 0, 300)).build();
            dispatchGesture(gesture, null, null);
            return "已滑动";
        }
        return "滑动失败：需要至少两个坐标点";
    }

    private String handleInput(ActionParameters params) {
        if (TextUtils.isEmpty(params.input_text)) {
            return "输入失败：内容为空";
        }

        AccessibilityNodeInfo target = null;
        if (params.getResource_id() != null) {
            target = findNodeByResId(params.getResource_id());
        } else if (params.getTarget_text() != null) {
            target = findNodeByText(params.getTarget_text());
        }

        if (target != null) {
            target.performAction(ACTION_FOCUS);
            Bundle arguments = new Bundle();
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, params.input_text);
            target.performAction(ACTION_SET_TEXT, arguments);
            target.recycle();
            return "已输入文本";
        }
        return "输入失败：未找到目标输入框";
    }

    private String handleLaunchApp(ActionParameters params) {
        if (TextUtils.isEmpty(params.package_name)) {
            return "启动失败：包名为空";
        }

        try {
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(params.package_name);
            if (launchIntent != null) {
                startActivity(launchIntent);
                return "已启动应用：" + params.package_name;
            }
            return "启动失败：应用未安装 - " + params.package_name;
        } catch (Exception e) {
            return "启动异常：" + e.getMessage();
        }
    }

    private String handleScroll(ActionParameters params) {
        if (TextUtils.isEmpty(params.direction)) {
            return "滚动失败：缺少方向参数";
        }
        String direction = params.direction.toLowerCase(Locale.US);
        // 根据方向匹配标准动作
        int[] start = new int[2];
        int[] end = new int[2];
        switch (direction) {
            case "up":
                start[0] = width / 2;
                start[1] = height * 2 / 3;
                end[0] = width / 2;
                end[1] = height / 3;
                break;
            case "down":
                start[0] = width / 2;
                start[1] = height / 3;
                end[0] = width / 2;
                end[1] = height * 2 / 3;
                break;
            case "left":
                start[0] = width / 2;
                start[1] = height / 2;
                end[0] = 30;
                end[1] = height / 2;
                break;
            case "right":
                start[0] = width / 2;
                start[1] = height / 2;
                end[0] = width - 30;
                end[1] = height / 2;
                break;
        }
        Path path = new Path();
        path.moveTo(start[0], start[1]);
        path.lineTo(end[0], end[1]);
        GestureDescription gesture = new GestureDescription.Builder().addStroke(new GestureDescription.StrokeDescription(path, 0, 300)).build();
        boolean ret = dispatchGesture(gesture, null, null);
        return ret ? "已滑" : "滑动失败";
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    // 辅助方法
    private AccessibilityNodeInfo findNodeByText(String text) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root != null) {
            List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText(text);
            if (!nodes.isEmpty()) {
                return nodes.get(0);
            }
            root.recycle();
        }
        return null;
    }
}

package com.example.deepai;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.deepai.api.ModelClient;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final Gson gson = new Gson();
    private EditText mEditText;
    private ListView mChatListView;
    private Button mBtnSetting;
    private Button mBtnSend;
    private Switch mSwitchService;

    private ArrayAdapter<String> adapter;
    private List<String> chatMessages;

    private ServiceConnection mServiceConnection;
    private static final int REQUEST_CODE = 100;
    private MediaProjectionManager mMediaProjectionManager;
    private MediaProjection mMediaProjection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initAction();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isAccessibilitySettingsOn(getPackageName())) {
            Toast.makeText(this, R.string.toast_please_setting, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            if (!AutoService.getInstance().isForeground()) {
                AutoService.getInstance().startForeground();
            }
            Log.d(TAG, "onActivityResult 截屏权限请求成功");
            mMediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data);
            AutoService.getInstance().setMediaProjection(mMediaProjection);
            mMediaProjection.registerCallback(new MediaProjection.Callback() {
                @Override
                public void onStop() {
                    Log.d(TAG, "mMediaProjection stop 截屏权限失效");
                    AutoService.getInstance().setMediaProjection(null);
                }
            }, new Handler(Looper.getMainLooper()));
        }
    }

    private void initView() {
        mSwitchService = findViewById(R.id.switch_service);
        mBtnSend = findViewById(R.id.btn_send);
        mEditText = findViewById(R.id.edit_msg);
        mChatListView = findViewById(R.id.chatListView);
        mBtnSetting = findViewById(R.id.btn_setting_accessibility);

        chatMessages = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, chatMessages);
        mChatListView.setAdapter(adapter);
    }

    private void initAction() {
        mMediaProjectionManager = (MediaProjectionManager) getApplicationContext()
                .getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        mBtnSetting.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));

        mSwitchService.setEnabled(false);
        mSwitchService.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                mSwitchService.setText(R.string.tv_close_service);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Log.d(TAG, "initAction: 打开前台服务并请求截屏权限");
                    startActivityForResult(mMediaProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
                } else {
                    Log.d(TAG, "initAction: 打开前台服务");
                    AutoService.getInstance().startForeground();
                }
            } else {
                mSwitchService.setText(R.string.tv_open_service);
                AutoService.getInstance().stopForeground();
            }
        });

        mBtnSend.setOnClickListener(v -> {
            String userMessage = mEditText.getText().toString().trim();
            if (!userMessage.isEmpty()) {
                chatMessages.add(userMessage);
                adapter.notifyDataSetChanged();
                mEditText.setText("");
                // 发送消息给大模型
                AutoService.getInstance().sendToLLM(userMessage);
            }
        });

        //添加应用安装列表
        List<Util.AppInfo> appInfos = Util.getInstalledApps(this);
        StringBuilder stringBuilder = new StringBuilder();
        for (Util.AppInfo appInfo : appInfos) {
            stringBuilder.append(appInfo);
        }
        Prompt.LLM = Prompt.LLM.replace("pkg占位符", stringBuilder);

        mServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                mSwitchService.setEnabled(true);
                mSwitchService.setChecked(AutoService.getInstance().isForeground());
                registerAIListener();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };
        Intent intent = new Intent(this, AutoService.class);
        bindService(intent, mServiceConnection, Service.BIND_AUTO_CREATE);
;
    }

    private boolean isAccessibilitySettingsOn(String service) {
        TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');
        String settingValue = Settings.Secure.getString(
                getApplicationContext().getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (settingValue != null) {
            mStringColonSplitter.setString(settingValue);
            while (mStringColonSplitter.hasNext()) {
                String accessibilityService = mStringColonSplitter.next();
                if (accessibilityService.contains(service)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void registerAIListener(){
        AutoService.getInstance().registerAIlistener(new ModelClient.CompletionCallback() {
            @Override
            public void onSuccess(String json) {
                if (!TextUtils.isEmpty(json)) {
                    chatMessages.add("模型: " + json);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(ModelClient.APIException exception) {
                Toast.makeText(MainActivity.this, "发送失败: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
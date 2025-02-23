package com.example.deepai.api;

import android.text.TextUtils;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import com.example.deepai.AutoService;
import com.example.deepai.Prompt;
import com.example.deepai.Util;
import com.example.deepai.bean.InputBean;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.POST;

public class ModelClient {

    private static final String TAG = "ApiClient";
    private static final Gson GSON = new Gson();
    private static final List<Message> LLM_MESSAGES = new ArrayList<>();
    // 添加日志拦截器
    private static final HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor()
            .setLevel(HttpLoggingInterceptor.Level.BODY);

    private static Retrofit RETROFIT;
    private static Message CURRENT_MESSAGE;
    private static InputBean CURRENT_INPUT;


    // OpenAI风格请求体
    public static class LLMRequest {
        // 修改: model 字段以匹配阿里云大模型的模型名称
        private final String model = Config.API.LLM_MODEL;
        private final List<Message> messages = LLM_MESSAGES;
        private final float temperature = 0.6f;
    }

    // OpenAI风格请求体
    public static class ImageAnalysisRequest {
        // 修改: model 字段以匹配阿里云大模型的模型名称
        private final String model = "qwen-vl-max";
        private final List<Message> messages;
        private final boolean vl_high_resolution_images = true;

        public ImageAnalysisRequest(String msg, String url) {
            messages = new ArrayList<>();
            messages.add(new Message("system", "[{\"type\":\"text\",\"text\": \"" + Prompt.IMAGE +
                    "\"}]"));
            String content = "[{\"type\":\"image_url\",\"image_url\":{\"url\":\"" + url +
                    "\"}},{\"type\":\"text\",\"text\":\"" + msg + ";手机分辨率:1080*2376" +
                    "\"}]";
            messages.add(new Message("user", content));
        }
    }

    // 修改: Message 类以适应阿里云大模型的请求格式
    public static class Message {
        public final String role;
        public String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    // 修改: OpenAI风格响应体以匹配阿里云大模型的响应格式
    public static class CompletionsResponse {
        public List<Choice> choices;

        public static class Choice {
            public Message message;
            public String finish_reason;
        }
    }

    // 自定义异常类
    public static class APIException extends Exception {
        public final int statusCode;
        public final String errorBody;

        public APIException(int statusCode, String errorBody) {
            super("API Error: " + statusCode);
            this.statusCode = statusCode;
            this.errorBody = errorBody;
        }
    }

    // 回调接口
    public interface CompletionCallback {
        void onSuccess(String json);

        void onFailure(APIException exception);
    }


    // 初始化Retrofit
    private static synchronized Retrofit getClient() {
        if (RETROFIT == null) {
            OkHttpClient client = new OkHttpClient.Builder().addInterceptor(chain -> {
                        Request original = chain.request();
                        Request request = original.newBuilder()
                                .header("Authorization", "Bearer " + Config.API.API_KEY)
                                .header("Content-Type", "application/json")
                                .method(original.method(), original.body())
                                .build();
                        return chain.proceed(request);
                    }).connectTimeout(300, TimeUnit.SECONDS)
                    .readTimeout(300, TimeUnit.SECONDS)
                    .addInterceptor(loggingInterceptor)
                    .build();

            RETROFIT = new Retrofit.Builder()
                    .baseUrl(Config.API.BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return RETROFIT;
    }

    public static void sendToLLM(String content, final CompletionCallback callback) {
        sendToLLM(content, callback, false);
    }

    public static void sendToLLM(String content, final CompletionCallback callback, boolean isClear) {
        if (isClear) {
            LLM_MESSAGES.clear();
        }
        if (LLM_MESSAGES.isEmpty()) {
            LLM_MESSAGES.add(new Message("system", Prompt.LLM));
        }
        AccessibilityNodeInfo nodeInfo = AutoService.getInstance().getRootInActiveWindow();
        CURRENT_INPUT = new InputBean();
        CURRENT_INPUT.user_request = content;
        if (nodeInfo != null) {
            CURRENT_INPUT.current_app = nodeInfo.getPackageName().toString();
            CURRENT_INPUT.current_activity = nodeInfo.getClassName().toString();
            CURRENT_INPUT.ui_hierarchy = Util.parseUIHierarchy(nodeInfo);
        }
        CURRENT_MESSAGE = new Message("user", GSON.toJson(CURRENT_INPUT));
        LLM_MESSAGES.add(CURRENT_MESSAGE);

        getClient().create(AutoMLApi.class).createCompletion(new LLMRequest()).enqueue(new Callback<CompletionsResponse>() {
            @Override
            public void onResponse(Call<CompletionsResponse> call, Response<CompletionsResponse> response) {
                CURRENT_INPUT.ui_hierarchy = "";
                CURRENT_MESSAGE.content = GSON.toJson(CURRENT_INPUT);
                if (response.isSuccessful()) {
                    if (response.body() == null) {
                        try {
                            callback.onFailure(new APIException(response.code(),
                                    response.errorBody() != null ?
                                            response.errorBody().string() : null));
                        } catch (IOException e) {
                            callback.onFailure(new APIException(0, e.getMessage()));
                        }
                    } else {
                        CompletionsResponse.Choice choice = response.body().choices.get(0);
                        if (TextUtils.equals("stop", choice.finish_reason)) {
                            String actionJson = response.body().choices.get(0).message.content;
                            LLM_MESSAGES.add(new Message("assistant", actionJson));
                            callback.onSuccess(apiDataFormat(response.body()));
                        } else {
                            callback.onFailure(new APIException(-1, "输出停止，请重试"));
                        }
                    }
                } else {
                    try {
                        callback.onFailure(new APIException(response.code(),
                                response.errorBody() != null ?
                                        response.errorBody().string() : null));
                    } catch (IOException e) {
                        callback.onFailure(new APIException(0, e.getMessage()));
                    }
                }
            }

            @Override
            public void onFailure(Call<CompletionsResponse> call, Throwable t) {
                CURRENT_INPUT.ui_hierarchy = "";
                CURRENT_MESSAGE.content = GSON.toJson(CURRENT_INPUT);
                callback.onFailure(new APIException(0, t.getMessage()));
            }
        });
    }


    public static void sendToImageAnalysis(ImageAnalysisRequest request, CompletionCallback callback) {
        getClient().create(AutoMLApi.class).sendToImageMode(request).enqueue(new Callback<CompletionsResponse>() {
            @Override
            public void onResponse(Call<CompletionsResponse> call, Response<CompletionsResponse> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "sendToImageAnalysis onResponse: " + response);
                    callback.onSuccess(apiDataFormat(response.body()));
                } else {
                    try {
                        callback.onFailure(new APIException(response.code(),
                                response.errorBody() != null ? response.errorBody().string() : null));
                    } catch (IOException e) {
                        callback.onFailure(new APIException(0, e.getMessage()));
                    }
                }
            }

            @Override
            public void onFailure(Call<CompletionsResponse> call, Throwable t) {
                callback.onFailure(new APIException(0, t.getMessage()));
            }
        });
    }

    private static String apiDataFormat(CompletionsResponse response) {
        if (response == null) return "";
        String data = response.choices.get(0).message.content;
        data = data.replaceAll("```json", "");
        data = data.replaceAll("```", "");
        return data;
    }

    // API接口定义
    private interface AutoMLApi {
        // 修改: @POST 注解以匹配阿里云大模型的API路径
        @POST("chat/completions")
        Call<CompletionsResponse> createCompletion(@Body LLMRequest request);

        @POST("chat/completions")
        Call<CompletionsResponse> sendToImageMode(@Body ImageAnalysisRequest request);
    }

}
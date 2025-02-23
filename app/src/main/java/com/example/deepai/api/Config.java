package com.example.deepai.api;

public class Config {
    public static class API {
        //阿里百练大模型平台key
        public static final String API_KEY = "sk-xxxx";
        public static final String BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/";
        //llm模型名：r1:deepseek-r1
        public static final String LLM_MODEL = "deepseek-v3";
    }

    public static class OSS {
        public static String REGION = "cn-shanghai";
        public static String FIILE_KEY = "temp.png";
        public static String BUCKET_NAME = "xxx";
        public static String END_POINT = "https://oss-cn-shanghai.aliyuncs.com";
        //阿里云OSS平台key
        public static String ACCESS_KEY_ID = "xxx";
        public static String ACCESS_KEY_SECRET = "xxx";

    }

    public static class User {
        public static final String PROFILE = "xxxx";
    }
}

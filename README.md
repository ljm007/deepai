# 🤖 DeepAI Mobile Assistant

一个基于大语言模型与 Android 无障碍服务的智能手机助手，实现自然语言交互与自动化任务执行。

## 📌 项目概述

本系统通过多模态交互实现智能化的手机自动化操作：

1. **自然语言理解**：解析用户指令生成操作序列
2. **视觉感知**：屏幕内容分析与上下文理解
3. **精准执行**：通过 Android 无障碍服务实现可靠操作
4. **动态决策**：基于执行结果的智能流程控制

## ✨ 核心特性

- 🧠 **大语言模型驱动**

  - deepseek v3 大语言模型支持(大家可以替换成其他大模型)
  - 多轮对话上下文保持
  - 复杂指令任务分解

- 📱 **无障碍服务集成**

  - 全界面元素操作支持
  - 精准点击/滑动/输入
  - 实时屏幕内容捕获

- 🔍 **视觉理解模块**

  - 屏幕 OCR 文字识别
  - 界面元素语义分析
  - 操作结果验证反馈

## 🛠️ 快速开始

### 环境要求

- Android 8.0+ (API 26+)
- [你的大模型服务] API Key

### 权限配置

- 启用开发者选项
- 开启无障碍服务权限
- 允许屏幕内容捕获权限

### 安装步骤

```bash
# 克隆仓库
git clone https://github.com/ljm007/deepai.git

```

## ⚙️ 配置指南

在 `app/src/main/java/com/example/deepai/api/Config.java` 中配置以下关键参数：

### 🤖 大模型配置 (API 类)

```java
public static class API {
    // 大模型 API 密钥（从阿里百练平台获取）
    public static final String API_KEY = "sk-xxxx";

    // API 服务地址（默认阿里云地址）
    public static final String BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/";

    // 使用的大模型名称（当前支持 deepseek-v3）
    public static final String LLM_MODEL = "deepseek-v3";
}
```

### ☁️ 云存储配置 (OSS 类)

```java
public static class OSS {
    // OSS 存储区域（根据实际地域修改）
    public static String REGION = "cn-shanghai";

    // 临时文件存储键名
    public static String FIILE_KEY = "temp.png";

    // OSS 存储桶配置（需替换为实际值）
    public static String BUCKET_NAME = "xxx";
    public static String END_POINT = "https://oss-cn-shanghai.aliyuncs.com";

    // OSS 访问凭证（从阿里云控制台获取）
    public static String ACCESS_KEY_ID = "xxx";
    public static String ACCESS_KEY_SECRET = "xxx";
}
```

### 👤 用户配置 (User 类)

```java
public static class User {
    // 用户个性化配置（根据需求扩展）
    public static final String PROFILE = "xxxx";
}
```

#### 配置注意事项：

- 替换所有 xxx 为实际服务凭证
- OSS 配置需与云服务商设置保持一致
- 模型服务可替换为其他兼容的 LLM 服务

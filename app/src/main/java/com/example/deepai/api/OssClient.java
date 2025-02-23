package com.example.deepai.api;

import android.util.Log;

import com.alibaba.sdk.android.oss.ClientConfiguration;
import com.alibaba.sdk.android.oss.ClientException;
import com.alibaba.sdk.android.oss.OSSClient;
import com.alibaba.sdk.android.oss.ServiceException;
import com.alibaba.sdk.android.oss.callback.OSSCompletedCallback;
import com.alibaba.sdk.android.oss.common.HttpMethod;
import com.alibaba.sdk.android.oss.common.auth.OSSCredentialProvider;
import com.alibaba.sdk.android.oss.common.auth.OSSPlainTextAKSKCredentialProvider;
import com.alibaba.sdk.android.oss.internal.OSSAsyncTask;
import com.alibaba.sdk.android.oss.model.CannedAccessControlList;
import com.alibaba.sdk.android.oss.model.CreateBucketRequest;
import com.alibaba.sdk.android.oss.model.CreateBucketResult;
import com.alibaba.sdk.android.oss.model.GeneratePresignedUrlRequest;
import com.alibaba.sdk.android.oss.model.PutObjectRequest;
import com.alibaba.sdk.android.oss.model.PutObjectResult;
import com.alibaba.sdk.android.oss.signer.SignVersion;
import com.example.deepai.AutoService;

public class OssClient {

    private static OSSClient ossClient;
    private static final String TAG = "OSS";


    public static void initOSS() {
        if (ossClient == null) {
            OSSCredentialProvider credentialProvider =
                    new OSSPlainTextAKSKCredentialProvider(Config.OSS.ACCESS_KEY_ID, Config.OSS.ACCESS_KEY_SECRET);
            ClientConfiguration config = new ClientConfiguration();
            config.setSignVersion(SignVersion.V4);
            ossClient = new OSSClient(AutoService.getInstance(), Config.OSS.END_POINT, credentialProvider);
            ossClient.setRegion(Config.OSS.REGION);
            // 填写存储空间名称。
            CreateBucketRequest createBucketRequest = new CreateBucketRequest(Config.OSS.BUCKET_NAME);
            // 设置存储空间的访问权限为公共读，默认为私有读写。
            createBucketRequest.setBucketACL(CannedAccessControlList.Private);
            // 指定存储空间所在的地域。
            createBucketRequest.setLocationConstraint("oss-cn-shanghai");
            OSSAsyncTask createTask = ossClient.asyncCreateBucket(createBucketRequest, new OSSCompletedCallback<CreateBucketRequest, CreateBucketResult>() {
                @Override
                public void onSuccess(CreateBucketRequest request, CreateBucketResult result) {
                    Log.d(TAG, "onSuccess() called with: request = [" + request + "], result = [" + result + "]");
                }

                @Override
                public void onFailure(CreateBucketRequest request, ClientException clientException, ServiceException serviceException) {
                    // 请求异常。
                    if (clientException != null) {
                        // 本地异常，如网络异常等。
                        clientException.printStackTrace();
                    }
                    if (serviceException != null) {
                        // 服务异常。
                        Log.e("ErrorCode", serviceException.getErrorCode());
                        Log.e("RequestId", serviceException.getRequestId());
                        Log.e("HostId", serviceException.getHostId());
                        Log.e("RawMessage", serviceException.getRawMessage());
                    }
                }
            });
        }
    }


    public static void updateFile(String path, Runnable okRun, Runnable failRun) {
        Log.d(TAG, "updateFile() called with: path = [" + path + "]");
        PutObjectRequest put = new PutObjectRequest(Config.OSS.BUCKET_NAME, Config.OSS.FIILE_KEY, path);
        OSSAsyncTask task = ossClient.asyncPutObject(put, new OSSCompletedCallback<PutObjectRequest, PutObjectResult>() {
            @Override
            public void onSuccess(PutObjectRequest request, PutObjectResult result) {
                Log.d(TAG, "onSuccess() called with: request = [" + request + "], result = [" + result + "]");
                okRun.run();
            }

            @Override
            public void onFailure(PutObjectRequest request, ClientException clientExcepion, ServiceException serviceException) {
                Log.d(TAG, "onFailure() called with: request = [" + request + "], clientExcepion = [" + clientExcepion + "], serviceException = [" + serviceException + "]");
                failRun.run();
                // 请求异常。
                if (clientExcepion != null) {
                    // 本地异常，如网络异常等。
                    clientExcepion.printStackTrace();
                }
                if (serviceException != null) {
                    // 服务异常。
                    Log.e("ErrorCode", serviceException.getErrorCode());
                    Log.e("RequestId", serviceException.getRequestId());
                    Log.e("HostId", serviceException.getHostId());
                    Log.e("RawMessage", serviceException.getRawMessage());
                }
            }
        });


    }

    public static String getDownloadUrl() {
        String url = "";
        try {
            // 生成用于下载文件的签名URL。
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(Config.OSS.BUCKET_NAME, Config.OSS.FIILE_KEY);
            // 设置签名URL的过期时间为1分钟。
            request.setExpiration(60);
            request.setMethod(HttpMethod.GET);
            url = ossClient.presignConstrainedObjectURL(request);
            Log.d("url----------------------:", url);
        } catch (ClientException e) {
            e.printStackTrace();
        }
        return url;
    }
}
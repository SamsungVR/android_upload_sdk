package com.samsung.msca.samsungvr.ui;

import android.util.Log;

import com.samsung.msca.samsungvr.sdk.HttpPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class VRLibHttpPlugin implements HttpPlugin.RequestFactory {

    static final String TAG = "VRLibHttpPlugin";

    private static final int READ_TIMEOUT = 15000;
    private static final int CONNECT_TIMEOUT = 15000;
    private static final int WRITE_TIMEOUT = 15000;
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String HEADER_CONTENT_LENGTH = "Content-Length";

    private static final boolean DEBUG = BuildConfig.DEBUG;

    private final OkHttpClient mHttpClient;

    VRLibHttpPlugin() {
        mHttpClient = new OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT, TimeUnit.MILLISECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS)
                .writeTimeout(WRITE_TIMEOUT, TimeUnit.MILLISECONDS)
                .build();
    }

    protected static class Request implements HttpPlugin.ReadableWritableRequest {

        private final String mMethod;


        private final okhttp3.MediaType mMediaType;
        private final long mContentLength;
        private final okhttp3.Request.Builder mRequestBuilder =
                new okhttp3.Request.Builder();
        private final OkHttpClient mHttpClient;

        protected Request(OkHttpClient httpClient, String urlStr, String method, String headers[][]) throws Exception {

            mHttpClient = httpClient;

            mRequestBuilder.url(urlStr);
            mMethod = method;

            if (DEBUG) {
                Log.d(TAG, "OkHttp connection method: " + mMethod + " url: " + urlStr + " this: " + this);
            }

            okhttp3.MediaType mediaType = null;
            long contentLength = -1;

            if (null != headers) {
                for (int i = 0; i < headers.length; i += 1) {
                    String attr = headers[i][0];
                    String value = headers[i][1];

                    if (HEADER_CONTENT_TYPE.equalsIgnoreCase(attr)) {
                        mediaType = okhttp3.MediaType.parse(value);
                    }
                    if (HEADER_CONTENT_LENGTH.equalsIgnoreCase(attr)) {
                        contentLength = Long.parseLong(value);
                    }
                    if (DEBUG) {
                        Log.d(TAG, "OkHttp add header " + attr + ": " + value + " this: " + this);
                    }
                    mRequestBuilder.addHeader(attr, value);
                }
            }
            mContentLength = contentLength;
            mMediaType = mediaType;
        }

        @Override
        public InputStream input() throws Exception {
            makeRequest();
            return mResponse.body().byteStream();
        }

        public void destroy() {
            if (null != mResponse) {
                if (DEBUG) {
                    Log.d(TAG, "Closing response body this: " + this);
                }
                mResponse.body().close();
            }
        }

        private okhttp3.RequestBody mRequestBody;

        @Override
        public void output(final InputStream input, final byte[] buf) throws Exception {

            mRequestBody = new okhttp3.RequestBody() {

                @Override
                public okhttp3.MediaType contentType() {
                    return mMediaType;
                }

                @Override
                public long contentLength() throws IOException {
                    return mContentLength;
                }

                @Override
                public void writeTo(okio.BufferedSink sink) throws IOException {
                    int len;

                    while (-1 != (len = input.read(buf))) {
                        if (DEBUG) {
                            Log.d(TAG, "Writing " + len + " bytes to sink");
                        }
                        sink.write(buf, 0, len);
                    }
                    sink.close();
                }
            };

            makeRequest();
        }

        private okhttp3.Response mResponse;

        @Override
        public int responseCode() throws Exception {
            makeRequest();
            return mResponse.code();
        }

        private void makeRequest() throws Exception {
            if (null != mResponse) {
                return;
            }
            mRequestBuilder.method(mMethod, mRequestBody);
            okhttp3.Request request = mRequestBuilder.build();
            okhttp3.Call call = mHttpClient.newCall(request);
            mResponse = call.execute();
        }

    }

    private static class GetRequest extends Request implements HttpPlugin.GetRequest {
        private GetRequest(okhttp3.OkHttpClient httpClient, String urlStr,
                           String[][] headers) throws Exception {
            super(httpClient, urlStr, "GET", headers);
        }
    }

    private static class PostRequest extends Request implements HttpPlugin.PostRequest {
        private PostRequest(okhttp3.OkHttpClient httpClient, String urlStr,
                            String[][] headers) throws Exception {
            super(httpClient, urlStr, "POST", headers);
        }
    }

    private static class PutRequest extends Request implements HttpPlugin.PutRequest {
        private PutRequest(okhttp3.OkHttpClient httpClient, String urlStr,
                           String[][] headers) throws Exception {
            super(httpClient, urlStr, "PUT", headers);
        }
    }

    private static class DeleteRequest extends Request implements HttpPlugin.DeleteRequest {
        private DeleteRequest(okhttp3.OkHttpClient httpClient, String urlStr,
                              String[][] headers) throws Exception {
            super(httpClient, urlStr, "DELETE", headers);
        }
    }

    @Override
    public HttpPlugin.GetRequest newGetRequest(String urlStr, String[][] headers) throws Exception {
        return new GetRequest(mHttpClient, urlStr, headers);
    }

    @Override
    public HttpPlugin.PostRequest newPostRequest(String urlStr, String[][] headers) throws Exception {
        return new PostRequest(mHttpClient, urlStr, headers);
    }

    @Override
    public HttpPlugin.DeleteRequest newDeleteRequest(String urlStr, String[][] headers) throws Exception {
        return new DeleteRequest(mHttpClient, urlStr, headers);
    }

    @Override
    public HttpPlugin.PutRequest newPutRequest(String urlStr, String[][] headers) throws Exception {
        return new PutRequest(mHttpClient, urlStr, headers);
    }


}


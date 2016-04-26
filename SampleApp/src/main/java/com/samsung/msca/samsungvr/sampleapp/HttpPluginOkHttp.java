/*
 * Copyright (c) 2016 Samsung Electronics America
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.samsung.msca.samsungvr.sampleapp;

import android.util.Log;

import com.samsung.msca.samsungvr.sdk.HttpPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.internal.Platform;
import okio.BufferedSink;

class HttpPluginOkHttp implements HttpPlugin.RequestFactory {

    static final String TAG = Util.getLogTag(HttpPluginOkHttp.class);
    static final boolean DEBUG_SOCKETS = false;

    private static final int READ_TIMEOUT = 15000;
    private static final int CONNECT_TIMEOUT = 15000;
    private static final int WRITE_TIMEOUT = 15000;


    private final OkHttpClient mHttpClient;

    HttpPluginOkHttp() {
        okhttp3.OkHttpClient.Builder clientBuilder = new okhttp3.OkHttpClient.Builder();

        clientBuilder.readTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS);
        clientBuilder.writeTimeout(WRITE_TIMEOUT, TimeUnit.MILLISECONDS);
        clientBuilder.connectTimeout(CONNECT_TIMEOUT, TimeUnit.MILLISECONDS);

        if (DEBUG_SOCKETS) {
            clientBuilder.addInterceptor(new LoggingInterceptor());
            //clientBuilder.socketFactory(mSSLSocketFactory);
            clientBuilder.sslSocketFactory(new MyDebugSSLSocketFactory());
            //clientBuilder.certificatePinner(mSSLSocketFactory.getCertificatePinner());
        }

        mHttpClient = clientBuilder.build();
    }

    protected static class Request implements HttpPlugin.ReadableWritableRequest {


        private final String mMethod;

        private static final String HEADER_CONTENT_TYPE = "content-type";
        private static final String HEADER_CONTENT_LENGTH = "content-length";

        private final okhttp3.MediaType mMediaType;
        private final long mContentLength;
        private final okhttp3.Request.Builder mRequestBuilder = new okhttp3.Request.Builder();
        private final okhttp3.OkHttpClient mHttpClient;

        protected Request(OkHttpClient httpClient, String urlStr, String method, String headers[][]) throws Exception {

            mHttpClient = httpClient;

            mRequestBuilder.url(urlStr);
            mMethod = method;

            Log.d(TAG, "OkHttp connection " + this + " to " + urlStr + " method " + mMethod);

            MediaType mediaType = null;
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
                    Log.d(TAG, "OkHttp add header " + this + " " + attr + ": " + value);
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
                mResponse.body().close();
            }
        }

        private RequestBody mRequestBody;

        @Override
        public void output(final InputStream input, final byte[] buf) throws Exception {
            mRequestBody = new RequestBody() {

                @Override
                public MediaType contentType() {
                    return mMediaType;
                }

                @Override
                public long contentLength() throws IOException {
                    return mContentLength;
                }

                @Override
                public void writeTo(BufferedSink sink) throws IOException {
                    int len;

                    while (-1 != (len = input.read(buf))) {
                        Log.d(TAG, "Writing " + len + " bytes to sink");
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
        private GetRequest(okhttp3.OkHttpClient httpClient, String urlStr, String[][] headers) throws Exception {
            super(httpClient, urlStr, "GET", headers);
        }
    }

    private static class PostRequest extends Request implements HttpPlugin.PostRequest {
        private PostRequest(okhttp3.OkHttpClient httpClient, String urlStr, String[][] headers) throws Exception {
            super(httpClient, urlStr, "POST", headers);
        }
    }

    private static class PutRequest extends Request implements HttpPlugin.PutRequest {
        private PutRequest(okhttp3.OkHttpClient httpClient, String urlStr, String[][] headers) throws Exception {
            super(httpClient, urlStr, "PUT", headers);
        }
    }

    private static class DeleteRequest extends Request implements HttpPlugin.DeleteRequest {
        private DeleteRequest(okhttp3.OkHttpClient httpClient, String urlStr, String[][] headers) throws Exception {
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

    static class MyDebugSSLSocketFactory extends Util.DebugSSLSocketFactory {

        private SSLSocketFactory delegate;
        private volatile TrustManager trustManager;

        @Override
        protected SSLSocketFactory getBase() {
            if (null == delegate) {
                try {
                    SSLContext sslContext = SSLContext.getInstance("TLS");
                    sslContext.init(null, null, null);
                    delegate = sslContext.getSocketFactory();
                    trustManager = Platform.get().trustManager(delegate);
                } catch (Exception ex) {
                }
            }
            return delegate;
        }

    }


    static class LoggingInterceptor implements okhttp3.Interceptor {

        @Override
        public okhttp3.Response intercept(Chain chain) throws IOException {
            okhttp3.Request request = chain.request();

            long t1 = System.nanoTime();
            Log.d(TAG, String.format("Sending request %s %s on %s%n%s",
                    request.method(), request.url(), chain.connection(), request.headers()));

            okhttp3.Response response = chain.proceed(request);

            long t2 = System.nanoTime();
            Log.d(TAG, String.format("Received response for %s in %.1fms%n%s",
                    response.request().url(), (t2 - t1) / 1e6d, response.headers()));

            return response;
        }
    }

}

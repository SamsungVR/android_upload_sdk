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

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

class HttpPluginHttpUrlConnection implements HttpPlugin.RequestFactory {

    static final String TAG = Util.getLogTag(HttpPluginHttpUrlConnection.class);

    private static class ReadableRequest implements HttpPlugin.ReadableRequest {

        protected final HttpURLConnection mConnection;

        static final boolean DEBUG_SOCKETS = false;

        private static final int READ_TIMEOUT = 5000;
        private static final int CONNECT_TIMEOUT = 5000;

        private static final String HEADER_CONTENT_LENGTH = "content-length";

        private ReadableRequest(String urlStr, String method, String headers[][]) throws Exception {
            URL url;

            try {
                url = new URL(urlStr);
            } catch (MalformedURLException ex) {
                Log.d(TAG, "Bad url: " + urlStr, ex);
                throw ex;
            }
            try {
                mConnection = (HttpURLConnection) url.openConnection();
            } catch (Exception ex) {
                Log.d(TAG, "Failed to open url: " + urlStr, ex);
                throw ex;
            }
            mConnection.setReadTimeout(READ_TIMEOUT);
            mConnection.setConnectTimeout(CONNECT_TIMEOUT);
            mConnection.setDoInput(true);

            enableDebug(mConnection);

            try {
                mConnection.setRequestMethod(method);
            } catch (ProtocolException ex) {
                Log.d(TAG, "Failed to set request method to " + method + " for url: " + urlStr, ex);
                throw ex;
            }
            if (null!= headers) {
                for (int i = 0; i < headers.length; i += 1) {
                    String attr = headers[i][0];
                    String value = headers[i][1];

                    if (HEADER_CONTENT_LENGTH.equalsIgnoreCase(attr)) {
                        mConnection.setFixedLengthStreamingMode(Long.parseLong(value));
                    }
                    mConnection.setRequestProperty(attr, value);
                }
            }
        }

        protected void enableDebug(HttpURLConnection connection) {
            if (DEBUG_SOCKETS) {
                if (connection instanceof HttpsURLConnection) {
                    ((HttpsURLConnection)connection).setSSLSocketFactory(new MyDebugSSLSocketFactory());
                }
            }
        }

        @Override
        public InputStream input() throws Exception {
            if (null == mConnection) {
                throw new IllegalStateException("Connection never initialized");
            }
            int responseCode = responseCode();
            InputStream stream;
            if (responseCode >= HttpURLConnection.HTTP_OK && responseCode < HttpURLConnection.HTTP_BAD_REQUEST) {
                stream = mConnection.getInputStream();
            } else {
                stream = mConnection.getErrorStream();
            }
            return stream;
        }

        public void destroy() {
            if (null == mConnection) {
                throw new IllegalStateException("Connection never initialized");
            }
            mConnection.disconnect();;
        }

        @Override
        public int responseCode() throws Exception {
            if (null == mConnection) {
                throw new IllegalStateException("Connection never initialized");
            }
            return mConnection.getResponseCode();
        }

    }

    private static class ReadableWritableRequest extends ReadableRequest implements HttpPlugin.WritableRequest {

        private ReadableWritableRequest(String urlStr, String method, String headers[][]) throws Exception {
            super(urlStr, method, headers);
            mConnection.setDoOutput(true);
        }

        @Override
        public void output(InputStream input, byte[] buf) throws Exception {
            if (null == mConnection) {
                throw new IllegalStateException("Connection never initialized");
            }
            OutputStream output = mConnection.getOutputStream();
            Util.copy(output, input, buf);
            output.close();
        }

    }

    private static class GetRequest extends ReadableRequest implements HttpPlugin.GetRequest {
        private GetRequest(String urlStr, String[][] headers) throws Exception {
            super(urlStr, "GET", headers);
        }
    }

    private static class PostRequest extends ReadableWritableRequest implements HttpPlugin.PostRequest {
        private PostRequest(String urlStr, String[][] headers) throws Exception {
            super(urlStr, "POST", headers);
        }
    }

    private static class PutRequest extends ReadableWritableRequest implements HttpPlugin.PutRequest {
        private PutRequest(String urlStr, String[][] headers) throws Exception {
            super(urlStr, "PUT", headers);
        }
    }

    private static class DeleteRequest extends ReadableRequest implements HttpPlugin.DeleteRequest {
        private DeleteRequest(String urlStr, String[][] headers) throws Exception {
            super(urlStr, "DELETE", headers);
        }
    }

    /*
     * Below methods MUST be thread safe
     */

    @Override
    public HttpPlugin.GetRequest newGetRequest(String urlStr, String[][] headers) throws Exception {
        return new GetRequest(urlStr, headers);
    }

    @Override
    public HttpPlugin.PostRequest newPostRequest(String urlStr, String[][] headers) throws Exception {
        return new PostRequest(urlStr, headers);
    }

    @Override
    public HttpPlugin.DeleteRequest newDeleteRequest(String urlStr, String[][] headers) throws Exception {
        return new DeleteRequest(urlStr, headers);
    }

    @Override
    public HttpPlugin.PutRequest newPutRequest(String urlStr, String[][] headers) throws Exception {
        return new PutRequest(urlStr, headers);
    }

    static class MyDebugSSLSocketFactory extends Util.DebugSSLSocketFactory {

        private SSLSocketFactory delegate;

        @Override
        protected SSLSocketFactory getBase() {
            if (null == delegate) {
                delegate = HttpsURLConnection.getDefaultSSLSocketFactory();
            }
            return delegate;
        }

    }

}

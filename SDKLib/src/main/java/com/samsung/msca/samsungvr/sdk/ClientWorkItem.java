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

package com.samsung.msca.samsungvr.sdk;

import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

abstract class ClientWorkItem<T extends VR.Result.BaseCallback> extends AsyncWorkItem<ClientWorkItemType> {

    protected final APIClientImpl mAPIClient;

    protected ClientWorkItem(APIClientImpl apiClient, ClientWorkItemType type) {
        super(type);
        mAPIClient = apiClient;
    }

    protected String toRESTUrl(String suffix) {
        return String.format(Locale.US, "%s/%s", mAPIClient.getEndPoint(), suffix);
    }

    protected enum HttpMethod {
        GET,
        POST,
        DELETE,
        PUT
    }

    protected <X extends HttpPlugin.BaseRequest> X newRequest(String url, HttpMethod method,
                    String[][] headers) throws Exception {
        HttpPlugin.RequestFactory reqFactory = mAPIClient.getRequestFactory();
        if (null == reqFactory) {
            return null;
        }
        X result = null;

        switch (method) {
            case GET:
                result = (X)reqFactory.newGetRequest(url, headers);
                break;
            case POST:
                result = (X)reqFactory.newPostRequest(url, headers);
                break;
            case DELETE:
                result = (X)reqFactory.newDeleteRequest(url, headers);
                break;
            case PUT:
                result = (X)reqFactory.newPutRequest(url, headers);
                break;
        }

        return result;
    }

    static final String HEADER_CONTENT_TYPE = "Content-Type";
    static final String HEADER_CONTENT_LENGTH = "Content-Length";
    static final String HEADER_CONTENT_DISPOSITION = "Content-Disposition";
    static final String HEADER_CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding";
    static final String HEADER_COOKIE = "Cookie";
    static final String CONTENT_TYPE_CHARSET_SUFFIX_UTF8 = "; charset=utf-8";

    private <T extends HttpPlugin.BaseRequest> T newEndPointRequest(String urlSuffix,
                    HttpMethod method, String[][] headers) throws Exception {

        String restUrl = toRESTUrl(urlSuffix);
        if (null == restUrl) {
            return null;
        }
        return newRequest(restUrl, method, headers);
    }

    protected HttpPlugin.GetRequest newGetRequest(String suffix, String[][] headers) throws Exception  {
        return newEndPointRequest(suffix, HttpMethod.GET, headers);
    }

    protected HttpPlugin.PostRequest newPostRequest(String suffix, String[][] headers) throws Exception {
        return newEndPointRequest(suffix, HttpMethod.POST, headers);
    }

    protected HttpPlugin.DeleteRequest newDeleteRequest(String suffix, String[][] headers) throws Exception {
        return newEndPointRequest(suffix, HttpMethod.DELETE, headers);
    }

    protected HttpPlugin.PutRequest newPutRequest(String suffix, String[][] headers) throws Exception {
        return newEndPointRequest(suffix, HttpMethod.PUT, headers);
    }

    int getResponseCode(HttpPlugin.ReadableRequest request) throws Exception {
        int responseCode = request.responseCode();
        if (DEBUG) {
            Log.d(TAG, "Returning response code " + responseCode + " from request " + Util.getHashCode(request));
        }
        return responseCode;
    }

    protected String toCookieString(String[][] cookies) {
        String cookieStr = "";
        if (null != cookies) {
            for (int i = 0; i < cookies.length; i += 1) {
                if (i > 0) {
                    cookieStr += "; ";
                }
                cookieStr += cookies[i][0] + "=" + cookies[i][1];
            }
        }
        return cookieStr;
    }

    protected void destroy(HttpPlugin.BaseRequest request) {
        if (DEBUG) {
            Log.d(TAG, "Disconnecting " + Util.getHashCode(request));
        }
        if (null != request) {
            try {
                request.destroy();
            } catch (Exception ex) {
                if (DEBUG) {
                    Log.d(TAG, "Failed to destroy request " + Util.getHashCode(request), ex);
                }
            }
        }
    }

    protected void writeBytes(HttpPlugin.WritableRequest request, final byte[] data, String debugMsg)
        throws Exception {
        int len = data.length;
        if (DEBUG && null != debugMsg) {
            Log.d(TAG, "Writing len: " + len + " msg: " + debugMsg);
        }
        ByteArrayInputStream bis = new ByteArrayInputStream(data);
        request.output(bis, mIOBuf);
        bis.close();
    }

    private String readHttpStream(InputStream in, String debugMsg) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = mIOBuf;

        BufferedInputStream bis;
        try {
            bis = new BufferedInputStream(in);
            while (!isCancelled()) {
                int len = bis.read(buf, 0, buf.length);
                if (len < 1) {
                    break;
                }
                bos.write(buf, 0, len);
            }
        } catch (IOException ex) {
            if (DEBUG) {
                Log.d(TAG, "Error reading input stream", ex);
            }
            return null;
        } finally {
            closeOutputStream(bos);
        }
        if (isCancelled()) {
            return null;
        }
        String result = bos.toString();
        if (DEBUG) {
            Log.d(TAG, "readHttpStream debugMsg: " + debugMsg + " result: " + result);
        }
        return result;
    }

    private boolean closeOutputStream(OutputStream stream) {
        if (null == stream) {
            return false;
        }
        try {
            stream.close();
            return true;
        } catch (IOException ex) {
            if (DEBUG) {
                Log.d(TAG, "Error closing output stream", ex);
            }
        }
        return false;
    }


    private boolean closeInputStream(InputStream stream) {
        if (null == stream) {
            return false;
        }
        try {
            stream.close();
            return true;
        } catch (IOException ex) {
            if (DEBUG) {
                Log.d(TAG, "Error closing input stream", ex);
            }
        }
        return false;
    }

    protected String readHttpStream(HttpPlugin.ReadableRequest request, final String debugMsg) {
        InputStream input = null;
        try {
            input = request.input();
            return readHttpStream(input, debugMsg);
        } catch (Exception ex) {
            if (DEBUG) {
                Log.d(TAG, "Error reading input stream, debugMsg: " + debugMsg, ex);
            }
            return null;
        } finally {
            closeInputStream(input);
        }

    }

    protected static class SplitStream extends InputStream {

        private static class LengthHolder {

            private long mTotal, mAvailable;

            public void setTotal(long total) {
                mTotal = total;
            }

            public long getAvailable() {
                return mAvailable;
            }

            public void onRead(int len) {
                if (-1 == len ) {
                    mAvailable = 0;
                } else {
                    mAvailable = Math.max(0, mAvailable - len);
                }
            }

            public void renew() {
                mAvailable = mTotal;
            }

        }

        private final LengthHolder mChunkInfo = new LengthHolder(), mBaseInfo = new LengthHolder();
        private final InputStream mBase;

        SplitStream(InputStream base, long totalLen, long chunkLen) {
            mBase = base;

            mChunkInfo.setTotal(chunkLen);

            mBaseInfo.setTotal(totalLen);
            mBaseInfo.renew();
        }

        @Override
        public int available() throws IOException {
            return (int)availableAsLong();
        }

        public long availableAsLong() {
            long a = mChunkInfo.getAvailable();
            long b = mBaseInfo.getAvailable();
            return Math.min(a, b);
        }

        @Override
        public long skip(long byteCount) throws IOException {
            throw new IOException();
        }

        @Override
        public synchronized void reset() throws IOException {
            throw new IOException();
        }

        public void renew() {
            mChunkInfo.renew();
        }

        @Override
        public int read() throws IOException {
            long available = availableAsLong();
            if (!canContinue()) {
                if (DEBUG) {
                    Log.d(TAG, "Split cannot continue, available: " + available);
                }
                return -1;
            }
            int canRead = (int)Math.min(1, available);
            if (canRead < 1) {
                if (DEBUG) {
                    Log.d(TAG, "Split no data available, remaining: " + available);
                }
                return -1;
            }
            int result = mBase.read();
            int len = (-1 != result) ? 1 : -1;
            onRead(len);
            if (DEBUG) {
                Log.d(TAG, "Split read byte remaining: " + availableAsLong() + " char: " + result);
            }
            return result;
        }

        private void onRead(int len) {
            mChunkInfo.onRead(len);
            mBaseInfo.onRead(len);
        }

        @Override
        public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
            long available = availableAsLong();
            if (!canContinue()) {
                if (DEBUG) {
                    Log.d(TAG, "Cannot continue, available: " + available);
                }
                return -1;
            }
            byteOffset = Math.max(byteOffset, 0);
            int bufAvailable = Math.min(buffer.length - byteOffset, byteCount);
            int canRead = (int)Math.min(bufAvailable, available);
            if (DEBUG) {
                Log.d(TAG, "Split pre read buf remaining: " + available + " canRead: " +
                        canRead + " byteCount: " + byteCount + " byteOffset: " + byteOffset);
            }
            if (canRead < 1) {
                return -1;
            }
            int wasRead = mBase.read(buffer, byteOffset, canRead);
            onRead(wasRead);
            if (DEBUG) {
                Log.d(TAG, "Split post read buf remaining: " + availableAsLong() + " canRead: " +
                        canRead + " wasRead: " + wasRead + " byteCount: " + byteCount +
                        " byteOffset: " + byteOffset);
            }
            return wasRead;
        }

        @Override
        public int read(byte[] buffer) throws IOException {
            int byteCount = buffer.length;
            return read(buffer, 0, byteCount);
        }

        protected boolean canContinue() {
            return true;
        }
    }

    protected void writeHttpStream(final HttpPlugin.WritableRequest request, InputStream input)
        throws Exception {

        if (DEBUG) {
            Log.d(TAG, "Writing input stream to output stream " + Util.getHashCode(request) + " len: " + input.available());
        }
        request.output(input, mIOBuf);
        if (DEBUG) {
            Log.d(TAG, "Done writing to stream " + Util.getHashCode(request));
        }
    }

    @Override
    protected void recycle() {
        super.recycle();
        mCallbackHolder.clearNoLock();
    }

    abstract void onRun() throws Exception;

    protected final ResultCallbackHolder mCallbackHolder = new ResultCallbackHolder();

    protected void set(T callback, Handler handler, Object closure) {
        mCallbackHolder.setNoLock(callback, handler, closure);
    }

    Object getClosure() {
        return mCallbackHolder.getClosureNoLock();
    }

    private Handler getHandler() {
        return mCallbackHolder.getHandlerNoLock();
    }

    protected static final boolean DEBUG = Util.DEBUG;
    private static final String TAG = Util.getLogTag(ClientWorkItem.class);

    private int mDispatchedCount;

    @Override
    public void run() {
        mDispatchedCount = 0;

        if (DEBUG) {
            Log.d(TAG, "Running work item: " + Util.getHashCode(this) + " type: " + getType());
        }
        if (isCancelled()) {
            dispatchCancelled();
        } else {
            try {
                onRun();
                if ((mDispatchedCount < 1) && isCancelled()) {
                    dispatchCancelled();
                }
            } catch (Exception ex) {
                if (DEBUG) {
                    Log.d(TAG, "Exception occured on work item: " + Util.getHashCode(this)
                            + " type: " + getType(), ex);
                }
                dispatchException(ex);
            }
        }
        if (1 != mDispatchedCount) {
            throw new RuntimeException("Invalid number of dispatches made, count: " + mDispatchedCount);
        }
    }

    protected void dispatchCounted(Util.CallbackNotifier notifier) {
        dispatchUncounted(notifier);
        mDispatchedCount += 1;
        onDispatchCounted(mDispatchedCount);
    }

    protected void onDispatchCounted(int count) {
    }

    protected void dispatchUncounted(Util.CallbackNotifier notifier) {
        notifier.post();
    }

    protected void dispatchCancelled() {
        dispatchCounted(new Util.CancelledCallbackNotifier().setNoLock(mCallbackHolder));
    }

    protected void dispatchFailure(int status) {
        dispatchCounted(new Util.FailureCallbackNotifier(status).setNoLock(mCallbackHolder));
    }

    protected void dispatchSuccess() {
        dispatchCounted(new Util.SuccessCallbackNotifier().setNoLock(mCallbackHolder));
    }

    protected <M> void dispatchSuccessWithResult(M ref) {
        dispatchCounted(new Util.SuccessWithResultCallbackNotifier<M>(ref).setNoLock(mCallbackHolder));
    }

    protected void dispatchException(Exception ex) {
        dispatchCounted(new Util.ExceptionCallbackNotifier(ex).setNoLock(mCallbackHolder));
    }

    protected static class ProgressCallbackNotifier extends Util.CallbackNotifier {

        private final float mProgress;

        public ProgressCallbackNotifier(float progress) {
            mProgress = progress;
        }

        @Override
        void notify(Object callback, Object closure) {
            ((VR.Result.ProgressCallback)callback).onProgress(closure, mProgress);
        }
    }

    protected static String headersToString(String[][] headers) {
        if (null == headers) {
            return null;
        }
        int len = headers.length;
        if (len < 1) {
            return null;
        }
        String result = new String();
        for (int i = 0; i < headers.length; i += 1) {
            String attr = headers[i][0];
            String value = headers[i][1];
            result += attr + ": " + value + ENDL;
        }
        return result;
    }


    protected boolean isHTTPSuccess(int responseCode) {
        return responseCode >= HttpURLConnection.HTTP_OK && responseCode < HttpURLConnection.HTTP_BAD_REQUEST;
    }

    private static final String HYPHENS = "--";
    private static final String QUOTE = "\"";
    private static final String ENDL = "\r\n";

    private static class JoinedInputStreams extends InputStream {

        private final InputStream[] mStreams;
        private int mCurrentIndex;

        public JoinedInputStreams(InputStream[] streams) {
            mStreams = streams;
            mCurrentIndex = 0;
        }

        @Override
        public synchronized void reset() throws IOException {
            throw new IOException();
        }

        @Override
        public long skip(long byteCount) throws IOException {
            throw new IOException();
        }

        private InputStream getCurrentStream() {
            if (mCurrentIndex < 0 || mCurrentIndex >= mStreams.length) {
                return null;
            }
            return mStreams[mCurrentIndex];
        }

        private InputStream nextStream() {
            if (mCurrentIndex >= 0 && mCurrentIndex < mStreams.length) {
                mCurrentIndex += 1;
            }
            return getCurrentStream();
        }


        @Override
        public int read() throws IOException {
            int result = -1;

            InputStream current = getCurrentStream();
            while (null != current) {
                result = current.read();
                if (-1 != result) {
                    onRead(1);
                    break;
                }
                current = nextStream();
            }
            return result;
        }

        @Override
        public int read(byte[] buffer) throws IOException {
            return read(buffer, 0, buffer.length);
        }

        @Override
        public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
            int read = 0;
            final int maxRead = Math.min(byteCount, buffer.length - byteOffset);
            InputStream current = getCurrentStream();
            while (null != current && read < maxRead) {
                int offset = byteOffset + read;
                int thisRead = maxRead - read;
                int actuallyRead = current.read(buffer, offset, thisRead);
                if (-1 == actuallyRead) {
                    current = nextStream();
                    continue;
                }
                read += actuallyRead;
            }
            if (read > 0) {
                onRead(read);
                return read;
            }
            return -1;
        }


        protected void onRead(int len) {
        }
    }

    private static String makeBoundary() {
        //return UUID.randomUUID().toString();
        return Long.toHexString((long)(Math.random() * Long.MAX_VALUE))
                + Long.toHexString(SystemClock.uptimeMillis());
    }

    protected <R> void writeFileAsMultipartFormData(
        String[][] headers, int indexContentType, int indexContentLength,
        final HttpPlugin.WritableRequest request, final ParcelFileDescriptor source)
        throws Exception {

        String boundary = makeBoundary();
        String name = "name_" + boundary;

        String[][] formDataHeaders = {
                {HEADER_CONTENT_DISPOSITION, "form-data; name=" + QUOTE + name + QUOTE},
                {HEADER_CONTENT_TYPE, "application/octet-stream"},
                {HEADER_CONTENT_TRANSFER_ENCODING, "binary"}
        };
        String formDataBeginStr = HYPHENS + boundary + ENDL + headersToString(formDataHeaders) + ENDL;
        String formDataEndStr = ENDL + HYPHENS + boundary + HYPHENS + ENDL;

        byte[] temp;
        long total = source.getStatSize();

        temp = formDataBeginStr.getBytes(StandardCharsets.US_ASCII);
        total += temp.length;
        InputStream beginStream = new ByteArrayInputStream(temp);

        temp = formDataEndStr.getBytes(StandardCharsets.US_ASCII);
        total += temp.length;
        InputStream endStream = new ByteArrayInputStream(temp);
        InputStream fileStream = new FileInputStream(source.getFileDescriptor());

        /* List<InputStream> streams = new ArrayList<>();
        streams.add(beginStream);
        streams.add(fileStream);
        streams.add(endStream);

        SequenceInputStream sis = new SequenceInputStream(Collections.enumeration(streams));
        */

        final long total2 = total;
        JoinedInputStreams streams = new JoinedInputStreams(new InputStream[] {beginStream, fileStream, endStream}) {
            @Override
            protected void onRead(int len) {
                super.onRead(len);

                float progress = (float) len / (float) total2;
                dispatchUncounted(new ProgressCallbackNotifier(progress).setNoLock(mCallbackHolder));
            }
        };
        headers[indexContentType][1] = "multipart/form-data; boundary=" + boundary;
        headers[indexContentLength][1] = String.valueOf(total);
        request.output(streams, mIOBuf);
        streams.close();
    }
}

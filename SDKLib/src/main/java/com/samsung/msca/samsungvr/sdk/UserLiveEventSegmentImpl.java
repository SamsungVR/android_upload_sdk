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
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.security.DigestException;
import java.security.MessageDigest;
import java.util.concurrent.atomic.AtomicBoolean;

class UserLiveEventSegmentImpl implements UserLiveEventSegment {

    private static final String TAG = Util.getLogTag(UserLiveEventSegmentImpl.class);
    private static final boolean DEBUG = Util.DEBUG;

    private final UserLiveEventImpl mUserLiveEvent;
    private final String mSegmentId;

    UserLiveEventSegmentImpl(UserLiveEventImpl userLiveEvent, String segmentId) {
        mUserLiveEvent = userLiveEvent;
        mSegmentId = segmentId;
    }

    private String mInitialSignedUrl, mUploadUrl;
    private boolean mUploading = false;
    private MessageDigest mMD5Digest;

    void setIsUploading(boolean isUploading) {
        synchronized (this) {
            mUploading = isUploading;
        }
    }

    boolean uploadContent(AtomicBoolean cancelHolder, String uploadUrl, MessageDigest md5Digest,
      ParcelFileDescriptor source, String initialSignedUrl, ResultCallbackHolder callbackHolder) {
        synchronized (this) {
            if (mUploading) {
                return false;
            }
            mInitialSignedUrl = initialSignedUrl;
            mUploadUrl = uploadUrl;
            mMD5Digest = md5Digest;
            return retryUploadNoLock(cancelHolder, source,
                    (UserLiveEvent.Result.UploadSegment)callbackHolder.getCallbackNoLock(),
                    callbackHolder.getHandlerNoLock(), callbackHolder.getClosureNoLock());
        }
    }

    void onUploadComplete() {
        synchronized (this) {
            mInitialSignedUrl = null;
            mUploadUrl = null;
            mMD5Digest = null;
            mUploading = false;
        }
    }

    @Override
    public boolean cancelUpload(Object closure) {
        if (DEBUG) {
            Log.d(TAG, "Cancelled segment upload requested this: " + this);
        }
        synchronized (this) {
            if (!mUploading) {
                return false;
            }
            return mUserLiveEvent.cancelUploadSegment(closure);
        }
    }

    private boolean retryUploadNoLock(AtomicBoolean cancelHolder, ParcelFileDescriptor source,
        UserLiveEvent.Result.UploadSegment callback, Handler handler, Object closure) {
        if (null == mSegmentId || null == mInitialSignedUrl) {
            return false;
        }
        AsyncWorkQueue<ClientWorkItemType, ClientWorkItem<?>> workQueue =
                mUserLiveEvent.getContainer().getContainer().getAsyncUploadQueue();

        WorkItemSegmentContentUpload workItem = workQueue.obtainWorkItem(WorkItemSegmentContentUpload.TYPE);
        workItem.set(cancelHolder, mUploadUrl, mMD5Digest, this, mUserLiveEvent, source,
                mInitialSignedUrl, mSegmentId, callback, handler, closure);
        mUploading = workQueue.enqueue(workItem);
        return mUploading;
    }


    static class WorkItemSegmentContentUpload extends UserLiveEventImpl.WorkItemSegmentUploadBase {


        @Override
        protected void dispatchCounted(Util.CallbackNotifier notifier) {
            super.dispatchCounted(notifier);
            mSegment.setIsUploading(false);
        }

        @Override
        protected void dispatchCancelled() {
            super.dispatchCancelled();
            mSegment.onUploadComplete();
        }

        @Override
        protected void dispatchSuccess() {
            super.dispatchSuccess();
            mSegment.onUploadComplete();
        }

        static final ClientWorkItemType TYPE = new ClientWorkItemType() {
            @Override
            public WorkItemSegmentContentUpload newInstance(APIClientImpl apiClient) {
                return new WorkItemSegmentContentUpload(apiClient);
            }
        };

        WorkItemSegmentContentUpload(APIClientImpl apiClient) {
            super(apiClient, TYPE);
        }

        private UserLiveEventSegmentImpl mSegment;
        private ParcelFileDescriptor mSource;
        private String mSegmentId, mInitialSignedUrl;
        private UserLiveEventImpl mUserLiveEvent;
        private MessageDigest mMD5Digest;
        private String mUploadUrl;

        synchronized WorkItemSegmentContentUpload set(AtomicBoolean cancelHolder,
            String uploadUrl, MessageDigest md5Digest,
            UserLiveEventSegmentImpl segment, UserLiveEventImpl userLiveEvent,
            ParcelFileDescriptor source, String initialSignedUrl, String segmentId,
            UserLiveEvent.Result.UploadSegment callback, Handler handler, Object closure) {

            super.set(cancelHolder, callback, handler, closure);
            mSegment = segment;
            mUploadUrl = uploadUrl;
            mMD5Digest = md5Digest;
            mSegmentId = segmentId;
            mUserLiveEvent = userLiveEvent;
            mSource = source;
            mInitialSignedUrl = initialSignedUrl;
            return this;
        }

        @Override
        protected synchronized void recycle() {
            super.recycle();
            mSegment = null;
            mSource = null;
            mUploadUrl = null;
            mInitialSignedUrl = null;
            mMD5Digest = null;
        }

        private static final String TAG = Util.getLogTag(WorkItemSegmentContentUpload.class);

        private class DigestStream extends InputStream {

            private final InputStream mInner;
            private final MessageDigest mDigest;
            private final long mTotalBytes;
            private long mReadBytes;


            private DigestStream(InputStream inner, MessageDigest digest, long total) {
                mDigest = digest;
                mInner = inner;
                mTotalBytes = total;
                mReadBytes = 0;
                mDigest.reset();
            }

            @Override
            public int available() throws IOException {
                if (isCancelled()) {
                    throw new IOException("Cancelled");
                }
                return mInner.available();
            }

            @Override
            public void close() throws IOException {
                mInner.close();
            }

            @Override
            public void mark(int readlimit) {
                mInner.mark(readlimit);
            }

            @Override
            public boolean markSupported() {
                return mInner.markSupported();
            }

            @Override
            public int read() throws IOException {
                if (isCancelled()) {
                    throw new IOException("Cancelled");
                }
                int read = mInner.read();
                if (-1 != read) {
                    byte temp[] = {(byte)read};
                    mDigest.update(temp);
                    updateProgress(1);
                }
                return read;
            }

            private void updateProgress(int readBytes) {
                mReadBytes += readBytes;
                double progress = 100.0 * ((double) mReadBytes / (double) mTotalBytes);
                dispatchUncounted(new ProgressCallbackNotifier((float)Math.min(100.0, progress)).setNoLock(mCallbackHolder));
            }

            @Override
            public int read(byte[] buffer) throws IOException {
                if (isCancelled()) {
                    throw new IOException("Cancelled");
                }
                int read = mInner.read(buffer);
                if (read > 0) {
                    mDigest.update(buffer, 0, read);
                    updateProgress(read);
                }
                return read;
            }

            @Override
            public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
                if (isCancelled()) {
                    throw new IOException("Cancelled");
                }
                int read = mInner.read(buffer, byteOffset, byteCount);
                if (read > 0) {
                    mDigest.update(buffer, byteOffset, read);
                    updateProgress(read);
                }
                return read;
            }

            @Override
            public synchronized void reset() throws IOException {
                mInner.reset();
            }

            @Override
            public long skip(long byteCount) throws IOException {
                return mInner.skip(byteCount);
            }

            byte[] digest() {
                return mDigest.digest();
            }
        }

        @Override
        public void onRun() throws Exception {

            User user = mUserLiveEvent.getUser();
            ParcelFileDescriptor source = mSource;
            UserLiveEventSegmentImpl segment = mSegment;


            long length = source.getStatSize();

            FileInputStream buf = null;
            HttpPlugin.PutRequest uploadRequest = null;
            HttpPlugin.PutRequest finishRequest = null;
            try {

                buf = new FileInputStream(source.getFileDescriptor());
                FileChannel channel = buf.getChannel();
                channel.position(0);

                String headers0[][] = {
                    {HEADER_CONTENT_LENGTH, String.valueOf(length)},
                    {HEADER_CONTENT_TYPE, "video/MP2T"},
                    {HEADER_CONTENT_TRANSFER_ENCODING, "binary"},
                };

                uploadRequest = newRequest(mInitialSignedUrl, HttpMethod.PUT, headers0);
                if (null == uploadRequest) {
                    dispatchFailure(VR.Result.STATUS_HTTP_PLUGIN_NULL_CONNECTION);
                    return;
                }

                DigestStream digestStream = new DigestStream(buf, mMD5Digest, length);

                try {
                    writeHttpStream(uploadRequest, digestStream);
                } catch (Exception ex) {
                    if (isCancelled()) {
                        dispatchCancelled();
                        return;
                    }
                    throw ex;
                }

                int rsp2 = getResponseCode(uploadRequest);

                if (!isHTTPSuccess(rsp2)) {
                    dispatchFailure(UserLiveEvent.Result.UploadSegment.STATUS_SEGMENT_UPLOAD_FAILED);
                    return;
                }
                dispatchUncounted(new ProgressCallbackNotifier(100f).setNoLock(mCallbackHolder));

                destroy(uploadRequest);
                uploadRequest = null;

                byte[] digest = digestStream.digest();
                String hexDigest = Util.bytesToHex(digest, false);

                JSONObject jsonParam = new JSONObject();

                jsonParam.put("status", "uploaded");
                jsonParam.put("md5", hexDigest);

                String jsonStr = jsonParam.toString();
                byte[] data = jsonStr.getBytes(StandardCharsets.UTF_8);

                String headers1[][] = {
                    {HEADER_CONTENT_LENGTH,String.valueOf(data.length)},
                    {HEADER_CONTENT_TYPE, "application/json" + ClientWorkItem.CONTENT_TYPE_CHARSET_SUFFIX_UTF8},
                    {UserImpl.HEADER_SESSION_TOKEN, user.getSessionToken()},
                    {APIClientImpl.HEADER_API_KEY, mAPIClient.getApiKey()},
                };

                finishRequest = newPutRequest(mUploadUrl, headers1);
                if (null == finishRequest) {
                    dispatchFailure(VR.Result.STATUS_HTTP_PLUGIN_NULL_CONNECTION);
                    return;
                }

                writeBytes(finishRequest, data, jsonStr);

                if (isCancelled()) {
                    dispatchCancelled();
                    return;
                }

                int rsp = getResponseCode(finishRequest);

                if (!isHTTPSuccess(rsp)) {
                    dispatchFailure(UserLiveEvent.Result.UploadSegment.STATUS_SEGMENT_END_NOTIFY_FAILED);
                    return;
                }

                dispatchSuccess();

            } finally {
                destroy(uploadRequest);
                destroy(finishRequest);
                if (null != buf) {
                    buf.close();
                }
                mMD5Digest.reset();
            }

        }

    }
}

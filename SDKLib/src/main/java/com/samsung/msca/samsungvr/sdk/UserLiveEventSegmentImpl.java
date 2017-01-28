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
import android.util.Log;

import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
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

        private static class ByteStream extends InputStream {

            private static class ByteArrayHolder {


                private byte[] mArray;
                int mOffset, mLen, mMark;

                int set(byte[] array, int offset, int len) {
                    mOffset = offset;
                    mLen = len;
                    mArray = array;
                    mMark = 0;
                    return mLen;
                }

                int read() {
                    if (null != mArray && mMark < mLen) {
                        int value = mArray[mOffset + mMark];
                        mMark += 1;
                        return value;
                    }
                    return -1;
                }


                int read(byte[] dst, int dstOffset, int dstCount) {
                    if (null != mArray) {
                        int remain = mLen - mMark;
                        if (remain > 0) {
                            int toCopy = Math.min(remain, dstCount);
                            System.arraycopy(mArray, mMark, dst, dstOffset, toCopy);
                            mMark += toCopy;
                            return toCopy;
                        }
                    }
                    return 0;
                }

                void clear() {
                    mOffset = 0;
                    mLen = 0;
                    mMark = 0;
                    mArray = null;
                }
            }


            @Override
            public void close() throws IOException {
                super.close();
            }

            @Override
            public void mark(int readlimit) {
            }

            @Override
            public boolean markSupported() {
                return false;
            }

            @Override
            public synchronized void reset() throws IOException {
            }

            private void resetInternal() {
                for (int i = 0; i < mBufs.length; i += 1) {
                    mBufs[i].clear();
                }
                mTotalAvailable = 0;
                mBufIndex = 0;
                mTotalRead = 0;

            }

            @Override
            public long skip(long byteCount) throws IOException {
                return 0;
            }


            private int mBufIndex, mTotalRead, mTotalAvailable;
            private final ByteArrayHolder[] mBufs = new ByteArrayHolder[] {
                new ByteArrayHolder(), new ByteArrayHolder(), new ByteArrayHolder()
            };

            private void set(int index, byte[] data) {
                mTotalAvailable  += mBufs[index].set(data, 0, data.length);
            }

            private void set(int index, byte[] data, int len) {
                mTotalAvailable  += mBufs[index].set(data, 0, len);
            }

            @Override
            public int available() throws IOException {
                return mTotalAvailable - mTotalRead;
            }

            @Override
            public int read() throws IOException {
                if (available() < 0) {
                    return -1;
                }
                while (mBufIndex < mBufs.length) {
                    ByteArrayHolder holder = mBufs[mBufIndex];
                    int value = holder.read();
                    if (-1 != value) {
                        mTotalRead += 1;
                        return value;
                    }
                    mBufIndex += 1;
                }
                return -1;
            }

            @Override
            public int read(byte[] buffer) throws IOException {
                return read(buffer, 0, buffer.length);
            }


            @Override
            public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
                if (available() < 0) {
                    return -1;
                }
                if (buffer.length < (byteOffset + byteCount)) {
                    throw new IOException();
                }
                int read = 0;
                while (byteCount > 0 && mBufIndex < mBufs.length) {
                    ByteArrayHolder holder = mBufs[mBufIndex];
                    int thisRead = holder.read(buffer, byteOffset, byteCount);
                    byteCount -= thisRead;
                    read += thisRead;
                    byteOffset += thisRead;
                    if (thisRead < byteCount) {
                        mBufIndex += 1;
                    }
                }
                return read;
            }
        }

        @Override
        public void onRun() throws Exception {

            User user = mUserLiveEvent.getUser();
            ParcelFileDescriptor source = mSource;

            long length = source.getStatSize();

            FileInputStream buf = null;
            HttpPlugin.PutRequest uploadRequest = null;
            HttpPlugin.PutRequest finishRequest = null;
            try {
                boolean isChunked = length <= 0;

                buf = new FileInputStream(source.getFileDescriptor());
                FileChannel channel = buf.getChannel();
                try {
                    channel.position(0);
                } catch (IOException ex) {
                }
                String content_type = "video/MP2T";
                if (this.mUserLiveEvent.getSource() == UserLiveEvent.Source.SEGMENTED_MP4) {
                    content_type = "video/mp4";
                }
                String headers0[][] = {
                    null,
                    {HEADER_CONTENT_TYPE, content_type},
                    {HEADER_CONTENT_TRANSFER_ENCODING, "binary"},
                };

                if (isChunked) {
                    headers0[0] = new String[] {HEADER_TRANSFER_ENCODING, TRANSFER_ENCODING_CHUNKED};
                } else {
                    headers0[0] = new String[] {HEADER_CONTENT_LENGTH, String.valueOf(length)};
                }

                uploadRequest = newRequest(mInitialSignedUrl, HttpMethod.PUT, headers0);
                if (null == uploadRequest) {
                    dispatchFailure(VR.Result.STATUS_HTTP_PLUGIN_NULL_CONNECTION);
                    return;
                }

                mMD5Digest.reset();
                ByteStream bis = new ByteStream();
                long totalRead = 0;

                while (!isCancelled()) {
                    int bytesRead = buf.read(mIOBuf);
                    if (bytesRead <= 0) {
                        break;
                    }
                    totalRead += bytesRead;
                    mMD5Digest.update(mIOBuf, 0, bytesRead);
                    bis.resetInternal();
                    if (isChunked) {
                        byte[] header = (String.valueOf(bytesRead) + ENDL).getBytes(StandardCharsets.UTF_8);
                        bis.set(0, header);
                        bis.set(2, ENDL.getBytes(StandardCharsets.UTF_8));
                    }
                    bis.set(1, mIOBuf, bytesRead);
                    writeHttpStream(uploadRequest, bis);
                    if (!isChunked && length > 0) {
                        dispatchUncounted(new ProgressCallbackNotifier(totalRead, length).setNoLock(mCallbackHolder));
                    }
                }

                if (isCancelled()) {
                    dispatchCancelled();
                    return;
                }

                int rsp2 = getResponseCode(uploadRequest);

                if (!isHTTPSuccess(rsp2)) {
                    dispatchFailure(UserLiveEvent.Result.UploadSegment.STATUS_SEGMENT_UPLOAD_FAILED);
                    return;
                }
                dispatchUncounted(new ProgressCallbackNotifier().setNoLock(mCallbackHolder));

                destroy(uploadRequest);
                uploadRequest = null;

                byte[] digest = mMD5Digest.digest();
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

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
import java.nio.channels.FileChannel;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

class UserVideoImpl implements UserVideo {

    private static final String TAG = Util.getLogTag(UserVideo.class);
    private static final boolean DEBUG = Util.DEBUG;

    private final UserImpl mUser;

    private String mTitle, mDesc;
    private final String mVideoId;
    private Permission mPermission;

    UserVideoImpl(UserImpl user, String videoId, String title, String desc, Permission permission) {
        mUser = user;
        mTitle = title;
        mDesc = desc;
        mVideoId = videoId;
        mPermission = permission;
    }

    private int mNumChunks;
    private long mChunkSize;
    private String mUploadId, mInitialSignedUrl;

    private int mLastSuccessfulChunk = -1;
    private boolean mUploading = false;

    void setIsUploading(boolean isUploading) {
        synchronized (this) {
            mUploading = isUploading;
        }
    }

    void setLastSuccessfulChunk(int chunk) {
        synchronized (this) {
            if (chunk >= 0 && chunk < mNumChunks) {
                mLastSuccessfulChunk = chunk;
            }
        }
    }

    boolean uploadContent(AtomicBoolean cancelHolder, ParcelFileDescriptor source, String initialSignedUrl,
                          String uploadId, long chunkSize, int numChunks,
                          ResultCallbackHolder callbackHolder) {
        synchronized (this) {
            if (mUploading) {
                return false;
            }
            mInitialSignedUrl = initialSignedUrl;
            mUploadId = uploadId;
            mChunkSize = chunkSize;
            mNumChunks = numChunks;
            return retryUploadNoLock(cancelHolder, source, (User.Result.UploadVideo)callbackHolder.getCallbackNoLock(),
                    callbackHolder.getHandlerNoLock(), callbackHolder.getClosureNoLock());
        }
    }

    void onUploadComplete() {
        synchronized (this) {
            mInitialSignedUrl = null;
            mUploadId = null;
            mUploading = false;
        }
    }

    @Override
    public boolean cancelUpload(Object closure) {
        if (DEBUG) {
            Log.d(TAG, "Cancelled video upload requested this: " + this);
        }
        synchronized (this) {
            if (!mUploading) {
                return false;
            }
            return mUser.cancelUploadVideo(closure);
        }
    }

    @Override
    public boolean retryUpload(ParcelFileDescriptor source, User.Result.UploadVideo callback,
                               Handler handler, Object closure) {
        if (DEBUG) {
            Log.d(TAG, "Retry video upload requested this: " + this);
        }
        synchronized (this) {
            if (mUploading) {
                return false;
            }
            return retryUploadNoLock(null, source, callback, handler, closure);
        }
    }

    @Override
    public String getVideoId() {
        return mVideoId;
    }

    private boolean retryUploadNoLock(AtomicBoolean cancelHolder, ParcelFileDescriptor source,
        User.Result.UploadVideo callback, Handler handler, Object closure) {
        if (null == mVideoId || null == mUploadId || null == mInitialSignedUrl) {
            return false;
        }
        AsyncWorkQueue<ClientWorkItemType, ClientWorkItem<?>> workQueue =
                mUser.getContainer().getAsyncUploadQueue();

        WorkItemVideoContentUpload workItem = workQueue.obtainWorkItem(WorkItemVideoContentUpload.TYPE);
        workItem.set(cancelHolder, this, mUser, source, mInitialSignedUrl, mVideoId, mUploadId,
                mChunkSize, mNumChunks, mLastSuccessfulChunk,
                callback, handler, closure);
        mUploading = workQueue.enqueue(workItem);
        return mUploading;
    }


    static class WorkItemVideoContentUpload extends UserImpl.WorkItemVideoUploadBase {


        @Override
        protected void dispatchCounted(Util.CallbackNotifier notifier) {
            super.dispatchCounted(notifier);
            mVideo.setIsUploading(false);
        }

        @Override
        protected void dispatchCancelled() {
            super.dispatchCancelled();
            mVideo.onUploadComplete();
        }

        @Override
        protected void dispatchSuccess() {
            super.dispatchSuccess();
            mVideo.onUploadComplete();
        }

        static final ClientWorkItemType TYPE = new ClientWorkItemType() {
            @Override
            public WorkItemVideoContentUpload newInstance(APIClientImpl apiClient) {
                return new WorkItemVideoContentUpload(apiClient);
            }
        };

        WorkItemVideoContentUpload(APIClientImpl apiClient) {
            super(apiClient, TYPE);
        }

        private UserVideoImpl mVideo;
        private ParcelFileDescriptor mSource;
        private int mNumChunks, mLastSuccessfulChunk;
        private long mChunkSize;
        private String mVideoId, mUploadId, mInitialSignedUrl;
        private UserImpl mUser;

        synchronized WorkItemVideoContentUpload set(AtomicBoolean cancelHolder,
            UserVideoImpl video, UserImpl user, ParcelFileDescriptor source,
            String initialSignedUrl, String videoId, String uploadId, long chunkSize, int numChunks,
            int lastSuccessfulChunk, User.Result.UploadVideo callback, Handler handler, Object closure) {

            super.set(cancelHolder, callback, handler, closure);
            mVideo = video;
            mUser = user;
            mLastSuccessfulChunk = lastSuccessfulChunk;
            mSource = source;
            mInitialSignedUrl = initialSignedUrl;
            mVideoId = videoId;
            mUploadId = uploadId;
            mChunkSize = chunkSize;
            mNumChunks = numChunks;
            return this;
        }

        @Override
        protected synchronized void recycle() {
            super.recycle();
            mUser = null;
            mSource = null;
            mInitialSignedUrl = null;
            mVideoId = null;
            mUploadId = null;
            mVideo = null;
        }

        private static final String TAG = Util.getLogTag(WorkItemVideoContentUpload.class);

        private String nextChunkUploadUrl(String userId, String videoId, String uploadId,
                                        String headers[][], int chunkId, boolean readData)
                    throws Exception {

            HttpPlugin.GetRequest nextRequest = null;

            try {
                String url = String.format(Locale.US, "user/%s/video/%s/upload/%s/%d/next",
                        userId, videoId, uploadId, chunkId);
                if (DEBUG) {
                    Log.d(TAG, "Requesting next chunk endpoint from: " + url);
                }
                nextRequest = newGetRequest(url, headers);
                if (null == nextRequest) {
                    dispatchFailure(VR.Result.STATUS_HTTP_PLUGIN_NULL_CONNECTION);
                    return null;
                }
                int rsp3 = getResponseCode(nextRequest);

                if (!isHTTPSuccess(rsp3)) {
                    dispatchFailure(User.Result.UploadVideo.STATUS_SIGNED_URL_QUERY_FAILED);
                    return null;
                }
                if (!readData) {
                    return null;
                }

                String data3 = readHttpStream(nextRequest, "code: " + rsp3);
                if (null == data3) {
                    dispatchFailure(VR.Result.STATUS_HTTP_PLUGIN_STREAM_READ_FAILURE);
                    return null;
                }
                JSONObject jsonObject2 = new JSONObject(data3);
                String signedUrl = jsonObject2.optString("signed_url", null);
                if (null == signedUrl) {
                    dispatchFailure(User.Result.UploadVideo.STATUS_SIGNED_URL_QUERY_FAILED);
                    return null;
                }
                return signedUrl;
            } finally {
                destroy(nextRequest);
            }

        }

        @Override
        public void onRun() throws Exception {

            UserImpl user = mUser;
            String videoId = mVideoId;
            String uploadId = mUploadId;
            long chunkSize = mChunkSize;
            int numChunks = mNumChunks, lastSuccessfulChunk = mLastSuccessfulChunk;
            ParcelFileDescriptor source = mSource;
            UserVideoImpl video = mVideo;

            int currentChunk = lastSuccessfulChunk + 1;
            long filePos = ((long)currentChunk) * chunkSize;

            String headers0[][] = {
                {UserImpl.HEADER_SESSION_TOKEN, user.getSessionToken()},
                {APIClientImpl.HEADER_API_KEY, mAPIClient.getApiKey()},
            };

            long length = source.getStatSize();
            long myLength = ((long)numChunks) * chunkSize;

            if (myLength < length) {
                dispatchFailure(User.Result.UploadVideo.STATUS_FILE_MODIFIED_AFTER_UPLOAD_REQUEST);
                return;
            }

            long remaining = length - filePos;
            if (remaining < 1) {
                dispatchFailure(User.Result.UploadVideo.STATUS_FILE_MODIFIED_AFTER_UPLOAD_REQUEST);
                return;
            }

            if (DEBUG) {
                Log.d(TAG, "Uploading content for videoId: " + videoId + " uploadId: " + uploadId +
                        " chunkSize: " + chunkSize + " numChunks: " + numChunks +
                        " lastSuccessfulChunk: " + lastSuccessfulChunk + " length: " + length +
                        " currentPos: " + filePos + " remaining: " + remaining);
            }

            FileInputStream buf = null;

            try {

                buf = new FileInputStream(source.getFileDescriptor());
                FileChannel channel = buf.getChannel();
                channel.position(filePos);

                SplitStream split = new SplitStream(buf, remaining, chunkSize) {
                    @Override
                    protected boolean canContinue() {
                        return !isCancelled();
                    }
                };

                String headers2[][] = {
                        /*
                         * Content length must be the first header. Index 0 is used to set the
                         * real length later
                         */
                        {HEADER_CONTENT_LENGTH, "0"},
                        {HEADER_CONTENT_TYPE, "application/octet-stream"},
                        {HEADER_CONTENT_TRANSFER_ENCODING, "binary"},

                };

                for (int i = currentChunk; i < numChunks; i++) {

                    if (isCancelled()) {
                        dispatchCancelled();
                        return;
                    }

                    float progress = 100f * ((float)(i) / (float)numChunks);

                    dispatchUncounted(new ProgressCallbackNotifier(progress).setNoLock(mCallbackHolder));

                    String signedUrl;

                    if (i == 0) {

                        signedUrl = mInitialSignedUrl;

                    } else {
                        signedUrl = nextChunkUploadUrl(user.getUserId(), videoId, uploadId,
                                headers0, i - 1, true);
                        if (null == signedUrl) {
                            return;
                        }
                    }

                    if (DEBUG) {
                        Log.d(TAG, "Uploading chunk: " + i + " url: " + signedUrl);
                    }
                    if (isCancelled()) {
                        dispatchCancelled();
                        return;
                    }

                    HttpPlugin.PutRequest uploadRequest = null;

                    try {
                        split.renew();
                        headers2[0][1] = String.valueOf(split.availableAsLong());

                        uploadRequest = newRequest(signedUrl, HttpMethod.PUT, headers2);
                        if (null == uploadRequest) {
                            dispatchFailure(VR.Result.STATUS_HTTP_PLUGIN_NULL_CONNECTION);
                            return;
                        }
                        try {
                            writeHttpStream(uploadRequest, split);
                        } catch (Exception ex) {
                            if (isCancelled()) {
                                dispatchCancelled();
                                return;
                            }
                            throw ex;
                        }

                        int rsp2 = getResponseCode(uploadRequest);

                        if (!isHTTPSuccess(rsp2)) {
                            dispatchFailure(User.Result.UploadVideo.STATUS_CHUNK_UPLOAD_FAILED);
                            return;
                        }

                        video.setLastSuccessfulChunk(i);

                    } finally {
                        destroy(uploadRequest);
                    }
                }
                dispatchUncounted(new ProgressCallbackNotifier(100f).setNoLock(mCallbackHolder));
                if (DEBUG) {
                    Log.d(TAG, "After successful upload, bytes remaining: " + split.availableAsLong());
                }
                /*
                 * next of the last chunk is what triggers the server to declare
                 * that file upload is complete
                 */
                nextChunkUploadUrl(user.getUserId(), videoId, uploadId, headers0, numChunks - 1, false);
                dispatchSuccess();

            } finally {
                if (null != buf) {
                    buf.close();
                }
            }
        }

    }
}

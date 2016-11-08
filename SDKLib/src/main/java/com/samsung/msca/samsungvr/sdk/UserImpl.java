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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

class UserImpl extends ContainedContainer.BaseImpl<APIClientImpl, User.Observer> implements User {

    private static final String TAG = Util.getLogTag(UserImpl.class);
    private static final boolean DEBUG = Util.DEBUG;

    static final String HEADER_SESSION_TOKEN = "X-SESSION-TOKEN";

    private enum Properties {
        USER_ID,
        NAME,
        EMAIL,
        SESSION_TOKEN,
        PROFILE_PIC
    };

    static final Contained.Type sType = new Contained.Type<APIClientImpl, UserImpl>(Properties.class) {

        @Override
        public UserImpl newInstance(APIClientImpl apiClient, JSONObject jsonObject) {
            return new UserImpl(apiClient, jsonObject);
        }

        @Override
        public Object getContainedId(JSONObject jsonObject) {
            return jsonObject.optString("user_id");
        }

        @Override
        public void notifyCreate(Object callback, APIClientImpl apiClient, UserImpl user) {

        }

        @Override
        public void notifyUpdate(Object callback, APIClientImpl apiClient, UserImpl user) {

        }

        @Override
        public void notifyDelete(Object callback, APIClientImpl apiClient, UserImpl user) {

        }

        @Override
        public void notifyQueried(Object callback, APIClientImpl apiClient, UserImpl user) {

        }

        @Override
        public void notifyListQueried(Object callback, APIClientImpl apiClient, List<UserImpl> contained) {

        }

        @Override
        String getEnumName(String key) {
            return key.toUpperCase(Locale.US);
        }

        @Override
        public Object validateValue(Enum<?> key, Object newValue) {
            if (null == newValue) {
                return null;
            }
            switch ((Properties)key) {
                case USER_ID:
                case NAME:
                case EMAIL:
                case SESSION_TOKEN:
                case PROFILE_PIC:
                    return newValue.toString();
                default:
                    break;
            }
            return null;
        }
    };


    private UserImpl(APIClientImpl apiClient, JSONObject jsonObject) {
        super(sType, apiClient, jsonObject);
    }

    @Override
    public <CONTAINED extends Contained.Spec> List<CONTAINED> containerOnQueryListOfContainedFromServiceLocked(Contained.Type type, JSONObject jsonObject) {
        if (type == UserLiveEventImpl.sType) {
            JSONArray jsonItems;
            try {
                Log.d(TAG, "So we are here " );
                jsonItems = jsonObject.getJSONArray("videos");
            }  catch (JSONException ex1) {
                return null;
            }
            return mContainerImpl.processQueryListOfContainedFromServiceLocked(type, jsonItems, null);
        }
        return null;
    }

    @Override
    public <CONTAINED extends Contained.Spec> boolean containerOnQueryOfContainedFromServiceLocked(Contained.Type type, CONTAINED contained, JSONObject jsonObject) {
        if (UserLiveEventImpl.sType == type) {
            return mContainerImpl.processQueryOfContainedFromServiceLocked(type, contained, jsonObject, false);
        }
        return false;
    }

    @Override
    public <CONTAINED extends Contained.Spec> CONTAINED containerOnDeleteOfContainedFromServiceLocked(Contained.Type type, CONTAINED contained) {
        if (UserLiveEventImpl.sType == type) {
            return mContainerImpl.processDeleteOfContainedFromServiceLocked(type, contained);
        }
        return null;
    }

    @Override
    public <CONTAINED extends Contained.Spec> CONTAINED containerOnCreateOfContainedInServiceLocked(Contained.Type type, JSONObject jsonObject) {
        if (UserLiveEventImpl.sType == type) {
            return mContainerImpl.processCreateOfContainedInServiceLocked(type, jsonObject, false);
        }
        return null;
    }

    @Override
    public <CONTAINED extends Contained.Spec> CONTAINED containerOnUpdateOfContainedToServiceLocked(Contained.Type type, CONTAINED contained) {
        if (UserLiveEventImpl.sType == type) {
            return mContainerImpl.processUpdateOfContainedToServiceLocked(type, contained);
        }
        return null;
    }

    @Override
    public Object containedGetIdLocked() {
        return mContainedImpl.getLocked(Properties.USER_ID);
    }

    @Override
    public void containedOnCreateInServiceLocked() {
    }

    @Override
    public void containedOnDeleteFromServiceLocked() {
    }

    @Override
    public void containedOnUpdateToServiceLocked() {
    }

    @Override
    public boolean containedOnQueryFromServiceLocked(JSONObject jsonObject) {
        return mContainedImpl.processQueryFromServiceLocked(jsonObject);
    }

    @Override
    public String getProfilePicUrl() {
        return (String)mContainedImpl.getLocked(Properties.PROFILE_PIC);
    }

    @Override
    public String getName() {
        return (String)mContainedImpl.getLocked(Properties.NAME);
    }

    @Override
    public String getEmail() {
        return (String)mContainedImpl.getLocked(Properties.EMAIL);
    }

    @Override
    public String getUserId() {
        return (String)mContainedImpl.getLocked(Properties.USER_ID);
    }

    @Override
    public String getSessionToken() {
        return (String)mContainedImpl.getLocked(Properties.SESSION_TOKEN);
    }

    @Override
    public boolean createLiveEvent(String title, String description,
                                   UserLiveEvent.Protocol protocol,
                                   UserLiveEvent.VideoStereoscopyType videoStereoscopyType,
                                   UserImpl.Result.CreateLiveEvent callback,
                                   Handler handler, Object closure) {
        AsyncWorkQueue<ClientWorkItemType, ClientWorkItem<?>> workQueue = getContainer().getAsyncWorkQueue();

        WorkItemCreateLiveEvent workItem = workQueue.obtainWorkItem(WorkItemCreateLiveEvent.TYPE);
        workItem.set(this, title, description, protocol,
                videoStereoscopyType, callback, handler, closure);
        return workQueue.enqueue(workItem);
    }

    @Override
    public boolean queryLiveEvents(Result.QueryLiveEvents callback, Handler handler, Object closure) {
        AsyncWorkQueue<ClientWorkItemType, ClientWorkItem<?>> workQueue = getContainer().getAsyncWorkQueue();

        WorkItemQueryLiveEvents workItem = workQueue.obtainWorkItem(WorkItemQueryLiveEvents.TYPE);
        workItem.set(this, callback, handler, closure);
        return workQueue.enqueue(workItem);
    }

    @Override
    public boolean uploadVideo(ParcelFileDescriptor source, String title, String description,
        UserVideo.Permission permission, Result.UploadVideo callback, Handler handler, Object closure) {
        if (DEBUG) {
            Log.d(TAG, "Video upload requested with closure: " + closure + " title: " + title
                    + " description: " + description + " permission: " + permission);
        }
        AsyncWorkQueue<ClientWorkItemType, ClientWorkItem<?>> workQueue = getContainer().getAsyncUploadQueue();

        WorkItemNewVideoUpload workItem = workQueue.obtainWorkItem(WorkItemNewVideoUpload.TYPE);
        workItem.set(this, source, title, description, permission, callback, handler, closure);
        return workQueue.enqueue(workItem);
    }

    @Override
    public boolean cancelUploadVideo(Object closure) {
        if (DEBUG) {
            Log.d(TAG, "Cancelled video upload requested with closure: " + closure);
        }
        AsyncWorkQueue<ClientWorkItemType, ClientWorkItem<?>> workQueue = getContainer().getAsyncUploadQueue();
        BooleanHolder found = new BooleanHolder();
        workQueue.iterateWorkItems(new AsyncWorkQueue.IterationObserver<ClientWorkItemType, ClientWorkItem<?>>() {
            @Override
            public boolean onIterate(ClientWorkItem workItem, Object... args) {
                Object argClosure = args[0];
                BooleanHolder myFound = (BooleanHolder) args[1];
                AsyncWorkItemType type = workItem.getType();
                if (WorkItemNewVideoUpload.TYPE == type ||
                        UserVideoImpl.WorkItemVideoContentUpload.TYPE == type) {
                    Object uploadClosure = workItem.getClosure();
                    if (DEBUG) {
                        Log.d(TAG, "Found video upload related work item " + workItem +
                                " closure: " + uploadClosure);
                    }
                    if (Util.checkEquals(argClosure, uploadClosure)) {
                        workItem.cancel();
                        myFound.setToTrue();
                        if (DEBUG) {
                            Log.d(TAG, "Cancelled video upload related work item " + workItem);
                        }
                    }
                }
                return true;
            }
        }, closure, found);
        boolean ret = found.getValue();
        if (DEBUG) {
            Log.d(TAG, "Cancelled video upload result: " + ret + " closure: " + closure);
        }
        return found.getValue();
    }

    /*
     * Create
     */

    static class WorkItemCreateLiveEvent extends ClientWorkItem<Result.CreateLiveEvent> {

        static final ClientWorkItemType TYPE = new ClientWorkItemType() {
            @Override
            public WorkItemCreateLiveEvent newInstance(APIClientImpl apiClient) {
                return new WorkItemCreateLiveEvent(apiClient);
            }
        };

        WorkItemCreateLiveEvent(APIClientImpl apiClient) {
            super(apiClient, TYPE);
        }

        private String mTitle, mDescription;
        UserLiveEvent.Protocol mProtocol;
        UserLiveEvent.VideoStereoscopyType mVideoStereoscopyType;
        private UserImpl mUser;

        synchronized WorkItemCreateLiveEvent set(UserImpl user, String title, String description,
                                        UserLiveEvent.Protocol protocol,
                                        UserLiveEvent.VideoStereoscopyType videoStereoscopyType,
                                        Result.CreateLiveEvent callback, Handler handler, Object closure) {
            super.set(callback, handler, closure);
            mUser = user;
            mTitle = title;
            mDescription = description;
            mProtocol = protocol;
            mVideoStereoscopyType = videoStereoscopyType;
            return this;
        }

        @Override
        protected synchronized void recycle() {
            super.recycle();
            mDescription = null;
            mProtocol = null;
            mTitle = null;
            mUser = null;
            mVideoStereoscopyType = null;
        }

        private static final String TAG = Util.getLogTag(WorkItemCreateLiveEvent.class);

        @Override
        public void onRun() throws Exception {
            HttpPlugin.PostRequest request = null;

            try {

                JSONObject jsonParam = new JSONObject();
                String userId = mUser.getUserId();

                jsonParam.put("source", "rtmp");
                jsonParam.put("title", mTitle);
                jsonParam.put("description", mDescription);
                switch (mVideoStereoscopyType) {
                    case TOP_BOTTOM_STEREOSCOPIC:
                        jsonParam.put("stereoscopic_type", "top-bottom");
                        break;
                    case LEFT_RIGHT_STEREOSCOPIC:
                        jsonParam.put("stereoscopic_type", "left-right");
                        break;
                    case DUAL_FISHEYE:
                        jsonParam.put("stereoscopic_type", "dual-fisheye");
                        break;

                }
                jsonParam.put("protocol", mProtocol.name().toLowerCase(Locale.US));

                String jsonStr = jsonParam.toString();
                byte[] data = jsonStr.getBytes(StandardCharsets.UTF_8);
                String headers[][] = {
                        {HEADER_CONTENT_LENGTH, String.valueOf(data.length)},
                        {UserImpl.HEADER_SESSION_TOKEN, mUser.getSessionToken()},
                        {APIClientImpl.HEADER_API_KEY, mAPIClient.getApiKey()},
                        {HEADER_CONTENT_TYPE, "application/json" + ClientWorkItem.CONTENT_TYPE_CHARSET_SUFFIX_UTF8}
                };

                request = newPostRequest(String.format(Locale.US, "user/%s/video", userId), headers);
                if (null == request) {
                    dispatchFailure(VR.Result.STATUS_HTTP_PLUGIN_NULL_CONNECTION);
                    return;
                }
                writeBytes(request, data, jsonStr);
                if (isCancelled()) {
                    dispatchCancelled();
                    return;
                }

                int rsp = getResponseCode(request);
                Log.d(TAG, "Rsp code: " +  rsp);
                String data2 = readHttpStream(request, "code: " + rsp);
                if (null == data2) {
                    dispatchFailure(VR.Result.STATUS_HTTP_PLUGIN_STREAM_READ_FAILURE);
                    return;
                }
                JSONObject jsonObject = new JSONObject(data2);
                if (HttpURLConnection.HTTP_OK == rsp) {
                    Log.d(TAG, "date= " +  data);
                    JSONObject liveEvent = jsonObject;
                    UserLiveEvent event = mUser.containerOnCreateOfContainedInServiceLocked(UserLiveEventImpl.sType, liveEvent);
                    if (null != event) {
                        dispatchSuccessWithResult(event);
                    } else {
                        dispatchFailure(VR.Result.STATUS_SERVER_RESPONSE_INVALID);
                    }
                    return;
                }
                int status = jsonObject.optInt("status", VR.Result.STATUS_SERVER_RESPONSE_NO_STATUS_CODE);
                dispatchFailure(status);

            } finally {
                destroy(request);
            }

        }
    }


    static class WorkItemQueryLiveEvents extends ClientWorkItem<Result.QueryLiveEvents> {

        static final ClientWorkItemType TYPE = new ClientWorkItemType() {
            @Override
            public WorkItemQueryLiveEvents newInstance(APIClientImpl apiClient) {
                return new WorkItemQueryLiveEvents(apiClient);
            }
        };

        WorkItemQueryLiveEvents(APIClientImpl apiClient) {
            super(apiClient, TYPE);
        }

        private UserImpl mUser;

        synchronized WorkItemQueryLiveEvents set(UserImpl user, Result.QueryLiveEvents callback,
                                                      Handler handler, Object closure) {
            super.set(callback, handler, closure);
            mUser = user;
            return this;
        }

        @Override
        protected synchronized void recycle() {
            super.recycle();
            mUser = null;
        }

        private static final String TAG = Util.getLogTag(WorkItemQueryLiveEvents.class);

        @Override
        public void onRun() throws Exception {
            HttpPlugin.GetRequest request = null;

            String headers[][] = {
                    {UserImpl.HEADER_SESSION_TOKEN, mUser.getSessionToken()},
                    {APIClientImpl.HEADER_API_KEY, mAPIClient.getApiKey()}
            };

            try {

                String userId = mUser.getUserId();

                request = newGetRequest(String.format(Locale.US, "user/%s/video?source=rtmp", userId), headers);

                if (null == request) {
                    dispatchFailure(VR.Result.STATUS_HTTP_PLUGIN_NULL_CONNECTION);
                    return;

                }

                if (isCancelled()) {
                    return;
                }

                int rsp = getResponseCode(request);
                String data = readHttpStream(request, "code: " + rsp);
                if (null == data) {
                    dispatchFailure(VR.Result.STATUS_HTTP_PLUGIN_STREAM_READ_FAILURE);
                    return;
                }
                JSONObject jsonObject = new JSONObject(data);
                if (isHTTPSuccess(rsp)) {
                    List<UserLiveEventImpl> result = mUser.containerOnQueryListOfContainedFromServiceLocked(UserLiveEventImpl.sType, jsonObject);
                    if (null != result) {
                        dispatchSuccessWithResult(result);
                    } else {
                        dispatchFailure(VR.Result.STATUS_SERVER_RESPONSE_INVALID);
                    }
                    return;
                }

                int status = jsonObject.optInt("status", VR.Result.STATUS_SERVER_RESPONSE_NO_STATUS_CODE);
                dispatchFailure(status);
            } finally {
                destroy(request);
            }

        }
    }

    abstract static class WorkItemVideoUploadBase extends ClientWorkItem<Result.UploadVideo> {

        private AtomicBoolean mCancelHolder;

        WorkItemVideoUploadBase(APIClientImpl apiClient, ClientWorkItemType type) {
            super(apiClient, type);
        }


        protected void set(AtomicBoolean cancelHolder, Result.UploadVideo callback, Handler handler,
                 Object closure) {
            super.set(callback, handler, closure);
            mCancelHolder = cancelHolder;
        }

        @Override
        protected void recycle() {
            super.recycle();
            mCancelHolder = null;
        }

        AtomicBoolean getCancelHolder() {
            return mCancelHolder;
        }

        @Override
        void cancel() {
            synchronized (this) {
                super.cancel();
                if (null != mCancelHolder) {
                    mCancelHolder.set(true);
                }
            }
        }

        @Override
        boolean isCancelled() {
            synchronized (this) {
                boolean cancelA = (null != mCancelHolder && mCancelHolder.get());
                boolean cancelB = super.isCancelled();
                if (DEBUG) {
                    Log.d(TAG, "Check for isCancelled, this: " + this + " a: " + cancelA + " b: " + cancelB);
                }
                return  cancelA || cancelB;
            }
        }
    }

    static class WorkItemNewVideoUpload extends WorkItemVideoUploadBase {

        private static class VideoIdAvailableCallbackNotifier extends Util.CallbackNotifier {

            private final UserVideo mUserVideo;

            public VideoIdAvailableCallbackNotifier(UserVideo userVideo) {
                mUserVideo = userVideo;
            }

            @Override
            void notify(Object callback, Object closure) {
                ((User.Result.UploadVideo)callback).onVideoIdAvailable(closure, mUserVideo);
            }
        }

        static final ClientWorkItemType TYPE = new ClientWorkItemType() {
            @Override
            public WorkItemNewVideoUpload newInstance(APIClientImpl apiClient) {
                return new WorkItemNewVideoUpload(apiClient);
            }
        };

        WorkItemNewVideoUpload(APIClientImpl apiClient) {
            super(apiClient, TYPE);
        }

        private ParcelFileDescriptor mSource;
        private String mTitle, mDescription;
        private UserImpl mUser;
        private UserVideo.Permission mPermission;

        WorkItemNewVideoUpload set(UserImpl user,
                ParcelFileDescriptor source, String title, String description,
                UserVideo.Permission permission, Result.UploadVideo callback, Handler handler,
                Object closure) {

            set(new AtomicBoolean(), callback, handler, closure);
            mUser = user;
            mTitle = title;
            mDescription = description;
            mSource = source;
            mPermission = permission;
            return this;
        }

        @Override
        protected synchronized void recycle() {
            super.recycle();
            mSource = null;
            mTitle = null;
            mDescription = null;
            mPermission = null;
        }

        private static final String TAG = Util.getLogTag(WorkItemNewVideoUpload.class);

        @Override
        public void onRun() throws Exception {

            long length = mSource.getStatSize();

            String headers0[][] = {
                    {UserImpl.HEADER_SESSION_TOKEN, mUser.getSessionToken()},
                    {APIClientImpl.HEADER_API_KEY, mAPIClient.getApiKey()},
            };

            String headers1[][] = {
                    {HEADER_CONTENT_LENGTH, "0"},
                    {HEADER_CONTENT_TYPE, "application/json" + ClientWorkItem.CONTENT_TYPE_CHARSET_SUFFIX_UTF8},
                    headers0[0],
                    headers0[1],
            };

            JSONObject jsonParam = new JSONObject();

            jsonParam.put("title", mTitle);
            jsonParam.put("description", mDescription);
            jsonParam.put("length", length);
            jsonParam.put("permission", mPermission.getStringValue());

            String jsonStr = jsonParam.toString();

            HttpPlugin.PostRequest request = null;
            String videoId = null;
            String uploadId = null;
            String signedUrl = null;
            int chunkSize = 0;
            int numChunks = 0;

            try {

                byte[] data = jsonStr.getBytes(StandardCharsets.UTF_8);
                headers1[0][1] = String.valueOf(data.length);
                request = newPostRequest(String.format(Locale.US, "user/%s/video", mUser.getUserId()), headers1);
                if (null == request) {
                    dispatchFailure(VR.Result.STATUS_HTTP_PLUGIN_NULL_CONNECTION);
                    return;
                }

                writeBytes(request, data, jsonStr);

                if (isCancelled()) {
                    dispatchCancelled();
                    return;
                }

                int rsp = getResponseCode(request);
                String data4 = readHttpStream(request, "code: " + rsp);
                if (null == data4) {
                    dispatchFailure(VR.Result.STATUS_HTTP_PLUGIN_STREAM_READ_FAILURE);
                    return;
                }
                JSONObject jsonObject = new JSONObject(data4);

                if (!isHTTPSuccess(rsp)) {
                    int status = jsonObject.optInt("status",
                            VR.Result.STATUS_SERVER_RESPONSE_NO_STATUS_CODE);
                    dispatchFailure(status);
                    return;
                }

                if (isCancelled()) {
                    dispatchCancelled();
                    return;
                }

                videoId = jsonObject.getString("video_id");
                uploadId = jsonObject.getString("upload_id");
                signedUrl = jsonObject.getString("signed_url");
                chunkSize = jsonObject.getInt("chunk_size");
                numChunks = jsonObject.getInt("chunks");

                UserVideoImpl userVideo = new UserVideoImpl(mUser, videoId, mTitle, mDescription,
                        mPermission);
                Util.CallbackNotifier notifier = new VideoIdAvailableCallbackNotifier(userVideo).setNoLock(mCallbackHolder);

                if (!userVideo.uploadContent(getCancelHolder(), mSource, signedUrl, uploadId,
                        chunkSize, numChunks, mCallbackHolder)) {
                    dispatchUncounted(notifier);
                    dispatchFailure(Result.UploadVideo.STATUS_CONTENT_UPLOAD_SCHEDULING_FAILED);
                } else {
                    dispatchCounted(notifier);
                }

            } finally {
                destroy(request);
            }

        }
    }

}
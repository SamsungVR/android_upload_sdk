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

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

class UserLiveEventImpl extends Contained.BaseImpl<UserImpl> implements UserLiveEvent {

    private static final String TAG = Util.getLogTag(UserLiveEventImpl.class);
    private static final boolean DEBUG = Util.DEBUG;

    private enum Properties {
        ID,
        TITLE,
        PERMISSION,
        PROTOCOL,
        STEREOSCOPIC_TYPE,
        DESCRIPTION,
        INGEST_URL,
        STATE,
        THUMBNAIL_URL,
        VIEWER_COUNT,
        LIVE_STARTED,
        LIVE_STOPPED,
        METADATA
    }

    private int mSegmentId = -1;

    static final Contained.Type sType = new Contained.Type<UserImpl, UserLiveEventImpl>(Properties.class) {

        @Override
        public void notifyCreate(Object callback, UserImpl user, UserLiveEventImpl userLiveEvent) {
            ((User.Observer)callback).onUserLiveEventCreated(userLiveEvent);
        }

        @Override
        public void notifyUpdate(Object callback, UserImpl user, UserLiveEventImpl userLiveEvent) {
            ((User.Observer)callback).onUserLiveEventUpdated(userLiveEvent);
        }

        @Override
        public void notifyDelete(Object callback, UserImpl user, UserLiveEventImpl userLiveEvent) {
            ((User.Observer)callback).onUserLiveEventDeleted(userLiveEvent);
        }

        @Override
        public void notifyQueried(Object callback, UserImpl user, UserLiveEventImpl userLiveEvent) {
            ((User.Observer)callback).onUserLiveEventQueried(userLiveEvent);
        }

        @Override
        public void notifyListQueried(Object callback, UserImpl user, List<UserLiveEventImpl> userLiveEvents) {
            ((User.Observer)callback).onUserLiveEventsQueried(user, (List<UserLiveEvent>) (List<?>) userLiveEvents);
        }

        @Override
        String getEnumName(String key) {
            return key.toUpperCase(Locale.US);
        }

        @Override
        public UserLiveEventImpl newInstance(UserImpl container, JSONObject jsonObject) {
            return new UserLiveEventImpl(container, jsonObject);
        }

        @Override
        public Object getContainedId(JSONObject jsonObject) {
            return jsonObject.optString("id");
//            return jsonObject.optString("video_id");

        }

        @Override
        Object validateValue(Enum<?> key, Object newValue) {
            Log.d("VRSDK", "key: " + key + " newValue:" + newValue);
            if (null == newValue) {
                return null;
            }

            switch ((Properties)key) {
                case TITLE:
                case ID:
                case DESCRIPTION:
                case INGEST_URL:
                case THUMBNAIL_URL:
                    return newValue.toString();
                case VIEWER_COUNT:
                case LIVE_STARTED:
                case LIVE_STOPPED:
                    return Long.parseLong(newValue.toString());
                case STATE:
                    return Util.enumFromString(State.class, newValue.toString());
                case PROTOCOL:
                    return Util.enumFromString(Protocol.class, newValue.toString());
                case PERMISSION:
                    return Util.enumFromString(UserVideo.Permission.class, newValue.toString());
                case METADATA:
                    Log.d("VRSDK", " case METADATA" );
                    String st_type = ((JSONObject)newValue).optString("stereoscopic_type");
                    if (st_type == null) {
                        Log.d("VRSDK", "NULL, returning MONOSCOPIC");
                        return VideoStereoscopyType.MONOSCOPIC;
                    }
                    Log.d("VRSDK", "other " + st_type);
                    if ("top-bottom".equals(st_type)) {
                        Log.d("VRSDK", "returning TOP_BOTTOM_STEREOSCOPIC");
                        return VideoStereoscopyType.TOP_BOTTOM_STEREOSCOPIC;
                    }

                    if ("left-right".equals(st_type)) {
                        Log.d("VRSDK", "returning LEFT_RIGHT_STEREOSCOPIC");
                        return VideoStereoscopyType.LEFT_RIGHT_STEREOSCOPIC;
                    }
                    if ("dual-fisheye".equals(st_type)) {
                        Log.d("VRSDK", "returning DUAL_FISHEYE");

                        return VideoStereoscopyType.DUAL_FISHEYE;
                    }
                    Log.d("VRSDK", "default returning LEFT_RIGHT_STEREOSCOPIC");
                    return VideoStereoscopyType.MONOSCOPIC;

                case STEREOSCOPIC_TYPE:
                   Log.d("VRSDK", "newValue: " + newValue);
                    if ("top-bottom".equals(newValue.toString()))
                        return VideoStereoscopyType.TOP_BOTTOM_STEREOSCOPIC;
                    if ("left-right".equals(newValue.toString()))
                        return VideoStereoscopyType.LEFT_RIGHT_STEREOSCOPIC;
                    if ("dual-fisheye".equals(newValue.toString()))
                        return VideoStereoscopyType.DUAL_FISHEYE;
                    return VideoStereoscopyType.MONOSCOPIC;
                default:
                    Log.d("VRSDK", "unknown tag: " + key);
                    break;
            }
            return null;
        }
    };

    UserLiveEventImpl(UserImpl user, JSONObject jsonObject) throws IllegalArgumentException {
        super(sType, user, jsonObject);
    }

    UserLiveEventImpl(UserImpl container, String id, String title,
                      UserVideo.Permission permission, Protocol protocol,
                      String description, String ingestUrl,
                      VideoStereoscopyType videoStereoscopyType, State state,
                      long viewerCount, long startedTime, long finishedTime) {

        this(container, null);

        setNoLock(Properties.ID, id);
        setNoLock(Properties.TITLE, title);
        setNoLock(Properties.PROTOCOL, protocol);
        setNoLock(Properties.DESCRIPTION, description);
        setNoLock(Properties.INGEST_URL, ingestUrl);
        setNoLock(Properties.PERMISSION, permission);
        setNoLock(Properties.STATE, state);
        setNoLock(Properties.VIEWER_COUNT, viewerCount);
        setNoLock(Properties.LIVE_STARTED, startedTime);
        setNoLock(Properties.LIVE_STOPPED, finishedTime);
        setNoLock(Properties.STEREOSCOPIC_TYPE, videoStereoscopyType);
    }

    UserLiveEventImpl(UserImpl container,
                      String id, String title,
                      UserVideo.Permission permission,
                      Protocol protocol,
                      String description,
                      VideoStereoscopyType videoStereoscopyType,
                      String ingestUrl) {
        this(container, id, title, permission, protocol,
                description, ingestUrl,
                videoStereoscopyType, State.UNKNOWN, 0L, 0L, 0L);
    }

    @Override
    public boolean containedOnQueryFromServiceLocked(JSONObject jsonObject) {
        return processQueryFromServiceLocked(jsonObject);
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
    public Object containedGetIdLocked() {
        return getLocked(Properties.ID);
    }

    @Override
    public boolean query(Result.QueryLiveEvent callback, Handler handler, Object closure) {
        APIClientImpl apiClient = getContainer().getContainer();
        AsyncWorkQueue<ClientWorkItemType, ClientWorkItem<?>> workQueue = apiClient.getAsyncWorkQueue();

        WorkItemQuery workItem = workQueue.obtainWorkItem(WorkItemQuery.TYPE);
        workItem.set(this, callback, handler, closure);
        return workQueue.enqueue(workItem);
    }

    @Override
    public boolean delete(Result.DeleteLiveEvent callback, Handler handler, Object closure) {
        APIClientImpl apiClient = getContainer().getContainer();

        AsyncWorkQueue<ClientWorkItemType, ClientWorkItem<?>> workQueue = apiClient.getAsyncWorkQueue();
        WorkItemDelete workItem = workQueue.obtainWorkItem(WorkItemDelete.TYPE);
        workItem.set(this, callback, handler, closure);
        return workQueue.enqueue(workItem);
    }


    //@Override
    public boolean finish(FinishAction action, Result.Finish callback, Handler handler, Object closure) {
        APIClientImpl apiClient = getContainer().getContainer();

        AsyncWorkQueue<ClientWorkItemType, ClientWorkItem<?>> workQueue = apiClient.getAsyncWorkQueue();
        WorkItemFinish workItem = workQueue.obtainWorkItem(WorkItemFinish.TYPE);
        workItem.set(this, action, callback, handler, closure);
        return workQueue.enqueue(workItem);
    }

    @Override
    public String getId() {
        return (String)getLocked(Properties.ID);
    }

    @Override
    public String getTitle() {
        return (String)getLocked(Properties.TITLE);
    }

    @Override
    public String getDescription() {
        return (String)getLocked(Properties.DESCRIPTION);
    }


    @Override
    public String getProducerUrl() {
        return (String)getLocked(Properties.INGEST_URL);
    }


    @Override
    public VideoStereoscopyType getVideoStereoscopyType() {
        VideoStereoscopyType val = (VideoStereoscopyType)getLocked(Properties.METADATA);
        if (val == null) {
            val = VideoStereoscopyType.MONOSCOPIC;
        }
        return val;
    }

    @Override
    public State getState() {
        return (State)getLocked(Properties.STATE);
    }

    @Override
    public Long getViewerCount() {return (Long)getLocked(Properties.VIEWER_COUNT);}

    @Override
    public Protocol getProtocol() {
        return (Protocol)getLocked(Properties.PROTOCOL);
    }

    @Override
    public UserVideo.Permission getPermission() {
        return (UserVideo.Permission)getLocked(Properties.PERMISSION);
    }

    @Override
    public Long getStartedTime() {
        return (Long)getLocked(Properties.LIVE_STARTED);
    }

    @Override
    public Long getFinishedTime() {
        return (Long)getLocked(Properties.LIVE_STOPPED);
    }

    @Override
    public User getUser() {
        return getContainer();
    }

    @Override
    public String getThumbnailUrl() {
        return (String)getLocked(Properties.THUMBNAIL_URL);
    }

    //@Override
    public boolean uploadThumbnail(ParcelFileDescriptor source, Result.UploadThumbnail callback,
                                   Handler handler, Object closure) {
        if (null == getThumbnailUrl()) {
            return false;
        }
        APIClientImpl apiClient = getContainer().getContainer();

        AsyncWorkQueue<ClientWorkItemType, ClientWorkItem<?>> workQueue = apiClient.getAsyncWorkQueue();
        WorkItemUploadThumbnail workItem = workQueue.obtainWorkItem(WorkItemUploadThumbnail.TYPE);
        workItem.set(this, source, callback, handler, closure);
        return workQueue.enqueue(workItem);
    }

    @Override
    public boolean uploadSegmentFromFD(ParcelFileDescriptor source,
        UserLiveEvent.Result.UploadSegment callback, Handler handler, Object closure) {
        AsyncWorkQueue<ClientWorkItemType, ClientWorkItem<?>> workQueue = getContainer().getContainer().getAsyncUploadQueue();

        UserLiveEventImpl.WorkItemNewSegmentUpload workItem = workQueue.obtainWorkItem(UserImpl.WorkItemNewVideoUpload.TYPE);
        workItem.set(this, Integer.toString(++mSegmentId), source, callback, handler, closure);
        return workQueue.enqueue(workItem);
    }

    @Override
    public boolean cancelUploadSegment(Object closure) {
        if (DEBUG) {
            Log.d(TAG, "Cancelled video upload requested with closure: " + closure);
        }
        AsyncWorkQueue<ClientWorkItemType, ClientWorkItem<?>> workQueue = getContainer().getContainer().getAsyncUploadQueue();
        BooleanHolder found = new BooleanHolder();
        workQueue.iterateWorkItems(new AsyncWorkQueue.IterationObserver<ClientWorkItemType, ClientWorkItem<?>>() {
            @Override
            public boolean onIterate(ClientWorkItem workItem, Object... args) {
                Object argClosure = args[0];
                BooleanHolder myFound = (BooleanHolder) args[1];
                AsyncWorkItemType type = workItem.getType();
                if (UserLiveEventImpl.WorkItemNewSegmentUpload.TYPE == type) {
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
     * Upload thumbnail
     */

    static class WorkItemUploadThumbnail extends ClientWorkItem<Result.UploadThumbnail> {


        static final ClientWorkItemType TYPE = new ClientWorkItemType() {
            @Override
            public WorkItemUploadThumbnail newInstance(APIClientImpl apiClient) {
                return new WorkItemUploadThumbnail(apiClient);
            }
        };

        WorkItemUploadThumbnail(APIClientImpl apiClient) {
            super(apiClient, TYPE);
        }

        private ParcelFileDescriptor mSource;
        private UserLiveEventImpl mLiveEvent;

        synchronized WorkItemUploadThumbnail set(UserLiveEventImpl liveEvent,
                         ParcelFileDescriptor source, Result.UploadThumbnail callback,
                         Handler handler, Object closure) {

            super.set(callback, handler, closure);
            mLiveEvent = liveEvent;
            mSource = source;

            return this;
        }

        @Override
        protected synchronized void recycle() {
            super.recycle();
            mSource = null;
            mLiveEvent = null;
        }

        private static final String TAG = Util.getLogTag(WorkItemUploadThumbnail.class);

        @Override
        public void onRun() throws Exception {

            HttpPlugin.PostRequest request = null;

            try {
                UserImpl user = mLiveEvent.getContainer();

                String headers[][] = {
                        {HEADER_CONTENT_TYPE, ""},
                        {HEADER_CONTENT_LENGTH, "0"},
                        {UserImpl.HEADER_SESSION_TOKEN, user.getSessionToken()},
                        {APIClientImpl.HEADER_API_KEY, mAPIClient.getApiKey()},
                };
                request = newRequest(mLiveEvent.getThumbnailUrl(), HttpMethod.POST, headers);
                if (null == request) {
                    dispatchFailure(VR.Result.STATUS_HTTP_PLUGIN_NULL_CONNECTION);
                    return;
                }
                if (isCancelled()) {
                    dispatchCancelled();
                    return;
                }

                writeFileAsMultipartFormData(headers, 0, 1, request, mSource);
                if (isCancelled()) {
                    dispatchCancelled();
                    return;
                }

                int rsp = getResponseCode(request);
                if (isHTTPSuccess(rsp)) {
                    dispatchSuccess();
                    return;
                }
                dispatchFailure(VR.Result.STATUS_SERVER_RESPONSE_NO_STATUS_CODE);
            } finally {
                destroy(request);
            }
        }
    }

    /*
     * Delete
     */

    private static class WorkItemDelete extends ClientWorkItem<Result.DeleteLiveEvent> {

        static final ClientWorkItemType TYPE = new ClientWorkItemType() {
            @Override
            public WorkItemDelete newInstance(APIClientImpl apiClient) {
                return new WorkItemDelete(apiClient);
            }
        };

        WorkItemDelete(APIClientImpl apiClient) {
            super(apiClient, TYPE);
        }

        private UserLiveEventImpl mUserLiveEvent;

        synchronized WorkItemDelete set(UserLiveEventImpl userLiveEvent, Result.DeleteLiveEvent callback,
                                        Handler handler, Object closure) {
            super.set(callback, handler, closure);
            mUserLiveEvent = userLiveEvent;
            return this;
        }

        @Override
        protected synchronized void recycle() {
            super.recycle();
            mUserLiveEvent = null;
        }

        private static final String TAG = Util.getLogTag(WorkItemDelete.class);

        @Override
        public void onRun() throws Exception {
            User user = mUserLiveEvent.getUser();
            HttpPlugin.DeleteRequest request = null;
            String headers[][] = {
                    {UserImpl.HEADER_SESSION_TOKEN, user.getSessionToken()},
                    {APIClientImpl.HEADER_API_KEY, mAPIClient.getApiKey()}
            };
            try {
                String liveEventId = mUserLiveEvent.getId();

                String userId = user.getUserId();

                request = newDeleteRequest(
                        String.format(Locale.US, "user/%s/video/%s", userId, liveEventId), headers);
                if (null == request) {
                    dispatchFailure(VR.Result.STATUS_HTTP_PLUGIN_NULL_CONNECTION);
                    return;
                }

                if (isCancelled()) {
                    dispatchCancelled();
                    return;
                }

                int rsp = getResponseCode(request);

                if (isHTTPSuccess(rsp)) {
                    if (null != mUserLiveEvent.getContainer().containerOnDeleteOfContainedFromServiceLocked(
                            UserLiveEventImpl.sType, mUserLiveEvent)) {
                        dispatchSuccess();
                    } else {
                        dispatchFailure(VR.Result.STATUS_SERVER_RESPONSE_INVALID);
                    }
                    return;
                }
                String data = readHttpStream(request, "code: " + rsp);
                if (null == data) {
                    dispatchFailure(VR.Result.STATUS_HTTP_PLUGIN_STREAM_READ_FAILURE);
                    return;
                }
                JSONObject jsonObject = new JSONObject(data);
                int status = jsonObject.optInt("status", VR.Result.STATUS_SERVER_RESPONSE_NO_STATUS_CODE);
                dispatchFailure(status);

            } finally {
                destroy(request);
            }

        }
    }


    private static class WorkItemQuery extends ClientWorkItem<Result.QueryLiveEvent> {

        static final ClientWorkItemType TYPE = new ClientWorkItemType() {
            @Override
            public WorkItemQuery newInstance(APIClientImpl apiClient) {
                return new WorkItemQuery(apiClient);
            }
        };

        WorkItemQuery(APIClientImpl apiClient) {
            super(apiClient, TYPE);
        }

        private UserLiveEventImpl mUserLiveEvent;

        synchronized WorkItemQuery set(UserLiveEventImpl userLiveEvent, Result.QueryLiveEvent callback,
                                       Handler handler, Object closure) {
            super.set(callback, handler, closure);
            mUserLiveEvent = userLiveEvent;
            return this;
        }

        @Override
        protected synchronized void recycle() {
            super.recycle();
            mUserLiveEvent = null;
        }

        private static final String TAG = Util.getLogTag(WorkItemQuery.class);

        @Override
        public void onRun() throws Exception {
            HttpPlugin.GetRequest request = null;
            User user = mUserLiveEvent.getContainer();
            String headers[][] = {
                    {UserImpl.HEADER_SESSION_TOKEN, user.getSessionToken()},
                    {APIClientImpl.HEADER_API_KEY, mAPIClient.getApiKey()}
            };
            try {
                String liveEventId = mUserLiveEvent.getId();
                if (liveEventId == null) {
                    Log.d(TAG, "onRun : " + " liveEventId is null! this wont work!");
                    return;

                }
                String userId = user.getUserId();
                request = newGetRequest(String.format(Locale.US, "user/%s/video/%s", userId, liveEventId),
                        headers);
                if (null == request) {
                    dispatchFailure(VR.Result.STATUS_HTTP_PLUGIN_NULL_CONNECTION);
                    return;
                }

                if (isCancelled()) {
                    dispatchCancelled();
                    return;
                }

                int rsp = getResponseCode(request);
                String data = readHttpStream(request, "code: " + rsp);

                if (null == data) {
                    dispatchFailure(VR.Result.STATUS_HTTP_PLUGIN_STREAM_READ_FAILURE);
                    return;
                }

                Log.d(TAG, "onSuccess : " + data);
                JSONObject jsonObject = new JSONObject(data);

                if (isHTTPSuccess(rsp)) {
                    JSONObject liveEvent = jsonObject.getJSONObject("video");
                    mUserLiveEvent.getContainer().containerOnQueryOfContainedFromServiceLocked(
                            UserLiveEventImpl.sType, mUserLiveEvent, liveEvent);
                    dispatchSuccess();
                    return;
                }
                int status = jsonObject.optInt("status", VR.Result.STATUS_SERVER_RESPONSE_NO_STATUS_CODE);
                dispatchFailure(status);

            } finally {
                destroy(request);
            }

        }
    }


    /*
     * Update
     */

    static class WorkItemFinish extends ClientWorkItem<Result.Finish> {

        static final ClientWorkItemType TYPE = new ClientWorkItemType() {
            @Override
            public WorkItemFinish newInstance(APIClientImpl apiClient) {
                return new WorkItemFinish(apiClient);
            }
        };

        WorkItemFinish(APIClientImpl apiClient) {
            super(apiClient, TYPE);
        }

        private UserLiveEventImpl mUserLiveEvent;

        synchronized WorkItemFinish set(UserLiveEventImpl userLiveEvent,
                                             FinishAction action,
                                             Result.Finish callback,
                                             Handler handler, Object closure) {
            super.set(callback, handler, closure);
            mUserLiveEvent = userLiveEvent;
            return this;
        }

        @Override
        protected synchronized void recycle() {
            super.recycle();
            mUserLiveEvent = null;
        }

        private static final String TAG = Util.getLogTag(WorkItemFinish.class);


        @Override
        public void onRun() throws Exception {
            HttpPlugin.PutRequest request = null;
            User user = mUserLiveEvent.getUser();

            JSONObject jsonParam = new JSONObject();
            jsonParam.put("state", State.LIVE_FINISHED_ARCHIVED);
            String jsonStr = jsonParam.toString();
            byte[] bdata = jsonStr.getBytes(StandardCharsets.UTF_8);

            String headers[][] = {
                    {UserImpl.HEADER_SESSION_TOKEN, user.getSessionToken()},
                    {APIClientImpl.HEADER_API_KEY, mAPIClient.getApiKey()},
                    {HEADER_CONTENT_TYPE, "application/json"},
                    {HEADER_CONTENT_LENGTH, String.valueOf(bdata.length)},
            };
            try {
                String liveEventId = mUserLiveEvent.getId();
                String userId = user.getUserId();
                request = newPutRequest(String.format(Locale.US, "user/%s/video/%s", userId, liveEventId),
                        headers);

                if (null == request) {
                    dispatchFailure(VR.Result.STATUS_HTTP_PLUGIN_NULL_CONNECTION);
                    return;
                }

                writeBytes(request, bdata, jsonStr);
                if (isCancelled()) {
                    dispatchCancelled();
                    return;
                }

                int rsp = getResponseCode(request);

                if (isHTTPSuccess(rsp)) {
                    if (null != mUserLiveEvent.getContainer().containerOnUpdateOfContainedToServiceLocked(
                            UserLiveEventImpl.sType, mUserLiveEvent)) {
                        dispatchSuccess();
                    } else {
                        dispatchFailure(VR.Result.STATUS_SERVER_RESPONSE_INVALID);
                    }
                    return;
                }

                String data = readHttpStream(request, "failure");
                if (null == data) {
                    dispatchFailure(VR.Result.STATUS_HTTP_PLUGIN_STREAM_READ_FAILURE);
                    return;
                }

                Log.d(TAG, "onSuccess : " + data);

                JSONObject jsonObject = new JSONObject(data);
                int status = jsonObject.optInt("status", VR.Result.STATUS_SERVER_RESPONSE_NO_STATUS_CODE);;

                dispatchFailure(status);

            } finally {
                destroy(request);
            }
        }
    }

    abstract static class WorkItemSegmentUploadBase extends ClientWorkItem<UserLiveEvent.Result.UploadSegment> {

        private AtomicBoolean mCancelHolder;

        WorkItemSegmentUploadBase(APIClientImpl apiClient, ClientWorkItemType type) {
            super(apiClient, type);
        }


        protected void set(AtomicBoolean cancelHolder, UserLiveEvent.Result.UploadSegment callback, Handler handler,
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

    static class WorkItemNewSegmentUpload extends UserLiveEventImpl.WorkItemSegmentUploadBase {

        private static class SegmentIdAvailableCallbackNotifier extends Util.CallbackNotifier {

            private final UserLiveEventSegment mSegment;

            public SegmentIdAvailableCallbackNotifier(UserLiveEventSegment segment) {
                mSegment = segment;
            }

            @Override
            void notify(Object callback, Object closure) {
                ((UserLiveEvent.Result.UploadSegment)callback).onSegmentIdAvailable(closure, mSegment);
            }
        }

        static final ClientWorkItemType TYPE = new ClientWorkItemType() {
            @Override
            public UserLiveEventImpl.WorkItemNewSegmentUpload newInstance(APIClientImpl apiClient) {
                return new UserLiveEventImpl.WorkItemNewSegmentUpload(apiClient);
            }
        };

        WorkItemNewSegmentUpload(APIClientImpl apiClient) {
            super(apiClient, TYPE);
        }

        private ParcelFileDescriptor mSource;
        private UserLiveEventImpl mUserLiveEvent;
        private String mSegmentId;

        UserLiveEventImpl.WorkItemNewSegmentUpload set(UserLiveEventImpl userLiveEvent, String segmentId,
            ParcelFileDescriptor source, UserLiveEvent.Result.UploadSegment callback, Handler handler,
            Object closure) {

            set(new AtomicBoolean(), callback, handler, closure);
            mSegmentId = segmentId;
            mUserLiveEvent = userLiveEvent;
            mSource = source;
            return this;
        }

        @Override
        protected synchronized void recycle() {
            super.recycle();
            mSource = null;
        }

        private static final String TAG = Util.getLogTag(UserImpl.WorkItemNewVideoUpload.class);

        @Override
        public void onRun() throws Exception {

            UserImpl user = mUserLiveEvent.getContainer();

            String headers0[][] = {
                    {UserImpl.HEADER_SESSION_TOKEN, user.getSessionToken()},
                    {APIClientImpl.HEADER_API_KEY, mAPIClient.getApiKey()},
            };

            String headers1[][] = {
                    {HEADER_CONTENT_LENGTH, "0"},
                    {HEADER_CONTENT_TYPE, "application/json" + ClientWorkItem.CONTENT_TYPE_CHARSET_SUFFIX_UTF8},
                    headers0[0],
                    headers0[1],
            };

            JSONObject jsonParam = new JSONObject();

            jsonParam.put("status", "init");

            String jsonStr = jsonParam.toString();

            HttpPlugin.PostRequest request = null;
            String signedUrl = null;

            try {

                MessageDigest digest = user.getMD5Digest();
                if (null == digest) {
                    dispatchFailure(Result.UploadSegment.STATUS_SEGMENT_NO_MD5_IMPL);
                }

                byte[] data = jsonStr.getBytes(StandardCharsets.UTF_8);
                String uploadUrl = String.format(Locale.US, "user/%s/video/%s/live_segment/%s",
                        user.getUserId(), mUserLiveEvent.getId(), mSegmentId);
                headers1[0][1] = String.valueOf(data.length);
                HttpPlugin.PutRequest setupRequest  = newPutRequest(uploadUrl, headers1);
                if (null == setupRequest) {
                    dispatchFailure(VR.Result.STATUS_HTTP_PLUGIN_NULL_CONNECTION);
                    return;
                }

                writeBytes(setupRequest, data, jsonStr);

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

                signedUrl = jsonObject.getString("signed_url");

                UserLiveEventSegmentImpl segment = new UserLiveEventSegmentImpl(mUserLiveEvent, mSegmentId);
                Util.CallbackNotifier notifier = new UserLiveEventImpl.WorkItemNewSegmentUpload.SegmentIdAvailableCallbackNotifier(segment).setNoLock(mCallbackHolder);

                if (!segment.uploadContent(getCancelHolder(), uploadUrl, digest, mSource, signedUrl, mCallbackHolder)) {
                    dispatchUncounted(notifier);
                    dispatchFailure(User.Result.UploadVideo.STATUS_CONTENT_UPLOAD_SCHEDULING_FAILED);
                } else {
                    dispatchCounted(notifier);
                }

            } finally {
                destroy(request);
            }

        }
    }



    @Override
    public String toString() {

        StringBuffer str = new StringBuffer ();

        str.append(" user=");
        str.append(getUser().getUserId());

        str.append(" id=");
        str.append(getId());

        str.append(" title=");
        str.append(getTitle());

        str.append(" description=");
        str.append(getDescription());

        str.append(" producerURL=");
        str.append(getProducerUrl());

        str.append(" state=");
        str.append(getState().toString());

        str.append(" viewerCount=");
        str.append(getViewerCount());

        str.append(" getProtocol=");
        str.append(getProtocol());

        str.append(" videoStereoscopyType=");
        str.append(getVideoStereoscopyType());

        str.append(" thumbnailUrl=");
        str.append(getThumbnailUrl());

        str.append(" permission=");
        str.append(getPermission());

        str.append(" finishedTime=");
        str.append(getFinishedTime());

        str.append(" startedTime=");
        str.append(getStartedTime());

        return str.toString();
    }
}

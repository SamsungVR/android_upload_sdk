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
        SOURCE,
        STEREOSCOPIC_TYPE,
        DESCRIPTION,
        INGEST_URL,
        VIEW_URL,
        STATE,
        TAKEDOWN,
        THUMBNAIL_URL,
        VIEWER_COUNT,
        LIVE_STARTED,
        LIVE_STOPPED,
        METADATA,
        REACTIONS
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
                case VIEW_URL:
                case THUMBNAIL_URL:
                    return newValue.toString();
                case VIEWER_COUNT:
                case LIVE_STARTED:
                case LIVE_STOPPED:
                    return Long.parseLong(newValue.toString());
                case TAKEDOWN:
                    return Boolean.parseBoolean(newValue.toString());
                case STATE:
                    return Util.enumFromString(State.class, newValue.toString());
                case SOURCE:
                    return Util.enumFromString(Source.class, newValue.toString());
                case PERMISSION:
                    return Util.enumFromString(UserVideo.Permission.class, newValue.toString());
                case REACTIONS:
                    UserVideoImpl.ReactionsImpl reactions = new UserVideoImpl.ReactionsImpl();
                    Log.d("VRSDK", " case REACTIONS" );
                    reactions.setScared(((JSONObject)newValue).optLong("scared",0L));
                    reactions.setAngry(((JSONObject)newValue).optLong("angry",0L));
                    reactions.setHappy(((JSONObject)newValue).optLong("happy",0L));
                    reactions.setSad(((JSONObject)newValue).optLong("sad",0L));
                    reactions.setSick(((JSONObject)newValue).optLong("sick",0L));
                    reactions.setWow(((JSONObject)newValue).optLong("wow",0L));
                    return reactions;
                case METADATA:
                    String st_type = ((JSONObject)newValue).optString("stereoscopic_type");
                    if (st_type == null) {
                        return UserVideo.VideoStereoscopyType.MONOSCOPIC;
                    }
                    if ("top-bottom".equals(st_type)) {
                        return UserVideo.VideoStereoscopyType.TOP_BOTTOM_STEREOSCOPIC;
                    }
                    if ("left-right".equals(st_type)) {
                        return UserVideo.VideoStereoscopyType.LEFT_RIGHT_STEREOSCOPIC;
                    }
                    if ("dual-fisheye".equals(st_type)) {
                        return UserVideo.VideoStereoscopyType.DUAL_FISHEYE;
                    }
                    if ("experimental".equals(st_type)) {
                        return UserVideo.VideoStereoscopyType.EXPERIMENTAL;
                    }
                    return UserVideo.VideoStereoscopyType.MONOSCOPIC;

                case STEREOSCOPIC_TYPE:
                   Log.d("VRSDK", "newValue: " + newValue);
                    if ("top-bottom".equals(newValue.toString()))
                        return UserVideo.VideoStereoscopyType.TOP_BOTTOM_STEREOSCOPIC;
                    if ("left-right".equals(newValue.toString()))
                        return UserVideo.VideoStereoscopyType.LEFT_RIGHT_STEREOSCOPIC;
                    if ("dual-fisheye".equals(newValue.toString()))
                        return UserVideo.VideoStereoscopyType.DUAL_FISHEYE;
                    if ("experimental".equals(newValue.toString()))
                        return UserVideo.VideoStereoscopyType.EXPERIMENTAL;
                    return UserVideo.VideoStereoscopyType.MONOSCOPIC;
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
                      UserVideo.Permission permission, Source source,
                      String description, String ingestUrl, String viewUrl,
                      UserVideo.VideoStereoscopyType videoStereoscopyType, State state, Boolean takedown,
                      long viewerCount, long startedTime, long finishedTime) {

        this(container, null);

        setNoLock(Properties.ID, id);
        setNoLock(Properties.TITLE, title);
        setNoLock(Properties.SOURCE, source);
        setNoLock(Properties.DESCRIPTION, description);
        setNoLock(Properties.INGEST_URL, ingestUrl);
        setNoLock(Properties.VIEW_URL,  viewUrl);
        setNoLock(Properties.PERMISSION, permission);
        setNoLock(Properties.STATE, state);
        setNoLock(Properties.TAKEDOWN, takedown);
        setNoLock(Properties.VIEWER_COUNT, viewerCount);
        setNoLock(Properties.LIVE_STARTED, startedTime);
        setNoLock(Properties.LIVE_STOPPED, finishedTime);
        setNoLock(Properties.STEREOSCOPIC_TYPE, videoStereoscopyType);
    }

    UserLiveEventImpl(UserImpl container,
                      String id, String title,
                      UserVideo.Permission permission,
                      Source source,
                      String description,
                      UserVideo.VideoStereoscopyType videoStereoscopyType,
                      String ingestUrl,
                      String viewUrl,
                      Boolean takedown) {
        this(container, id, title, permission, source,
                description, ingestUrl, viewUrl,
                videoStereoscopyType, State.UNKNOWN, takedown, 0L, 0L, 0L);
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
        workItem.set(this.getContainer(), getId(), this, callback, handler, closure);
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


    @Override
    public boolean finish(FinishAction action, Result.Finish callback, Handler handler, Object closure) {
        APIClientImpl apiClient = getContainer().getContainer();

        AsyncWorkQueue<ClientWorkItemType, ClientWorkItem<?>> workQueue = apiClient.getAsyncWorkQueue();
        WorkItemFinish workItem = workQueue.obtainWorkItem(WorkItemFinish.TYPE);
        workItem.set(this, action, callback, handler, closure);
        return workQueue.enqueue(workItem);
    }

    @Override
    public boolean setPermission(UserVideo.Permission permission, Result.SetPermission callback, Handler handler, Object closure) {
        APIClientImpl apiClient = getContainer().getContainer();

        AsyncWorkQueue<ClientWorkItemType, ClientWorkItem<?>> workQueue = apiClient.getAsyncWorkQueue();
        WorkItemSetPermission workItem = workQueue.obtainWorkItem(WorkItemSetPermission.TYPE);
        workItem.set(this, permission, callback, handler, closure);
        return workQueue.enqueue(workItem);
    }

    @Override
    public boolean setTitle(String title, VR.Result.SimpleCallback callback,
                            Handler handler, Object closure) {
        APIClientImpl apiClient = getContainer().getContainer();
        AsyncWorkQueue<ClientWorkItemType, ClientWorkItem<?>> workQueue = apiClient.getAsyncWorkQueue();
        WorkItemSetTitle workItem = workQueue.obtainWorkItem(WorkItemSetTitle.TYPE);
        workItem.set(this, title, callback, handler, closure);
        return workQueue.enqueue(workItem);
    }

    @Override
    public boolean setDescription(String description, VR.Result.SimpleCallback callback,
                                  Handler handler, Object closure) {
        APIClientImpl apiClient = getContainer().getContainer();
        AsyncWorkQueue<ClientWorkItemType, ClientWorkItem<?>> workQueue = apiClient.getAsyncWorkQueue();
        WorkItemSetDescription workItem = workQueue.obtainWorkItem(WorkItemSetDescription.TYPE);
        workItem.set(this, description, callback, handler, closure);
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
    public UserVideo.Reactions getReactions() {
        return (UserVideo.Reactions)getLocked(Properties.REACTIONS);
    }

    @Override
    public String getProducerUrl() {
        return (String)getLocked(Properties.INGEST_URL);
    }

    @Override
    public String getViewUrl() {
        return (String)getLocked(Properties.VIEW_URL);
    }


    @Override
    public UserVideo.VideoStereoscopyType getVideoStereoscopyType() {
        UserVideo.VideoStereoscopyType val = (UserVideo.VideoStereoscopyType)getLocked(Properties.METADATA);
        if (val == null) {
            val = UserVideo.VideoStereoscopyType.MONOSCOPIC;
        }
        return val;
    }

    @Override
    public State getState() {
        return (State)getLocked(Properties.STATE);
    }



    @Override
    public boolean hasTakenDown() {
        return (Boolean)getLocked(Properties.TAKEDOWN);
    }


    @Override
    public Long getViewerCount() {return (Long)getLocked(Properties.VIEWER_COUNT);}

    @Override
    public Source getSource() {
        return (Source)getLocked(Properties.SOURCE);
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

        UserLiveEventImpl.WorkItemNewSegmentUpload workItem = workQueue.obtainWorkItem(UserLiveEventImpl.WorkItemNewSegmentUpload.TYPE);
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
                if (UserLiveEventImpl.WorkItemNewSegmentUpload.TYPE == type ||
                        UserLiveEventSegmentImpl.WorkItemSegmentContentUpload.TYPE == type) {
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


    static class WorkItemQuery extends ClientWorkItem<Result.QueryLiveEvent> {

        static final ClientWorkItemType TYPE = new ClientWorkItemType() {
            @Override
            public WorkItemQuery newInstance(APIClientImpl apiClient) {
                return new WorkItemQuery(apiClient);
            }
        };

        WorkItemQuery(APIClientImpl apiClient) {
            super(apiClient, TYPE);
        }

        private UserImpl mUser;
        private String mUserLiveEventId;
        private UserLiveEventImpl mUserLiveEventImpl;


        synchronized WorkItemQuery set(UserImpl user, String userLiveEventId,
               UserLiveEventImpl userLiveEventImpl,
               Result.QueryLiveEvent callback,
               Handler handler, Object closure) {
            super.set(callback, handler, closure);
            mUserLiveEventImpl = userLiveEventImpl;
            mUserLiveEventId = userLiveEventId;
            mUser = user;
            return this;
        }

        @Override
        protected synchronized void recycle() {
            super.recycle();
            mUser = null;
            mUserLiveEventId = null;
            mUserLiveEventImpl = null;
        }

        private static final String TAG = Util.getLogTag(WorkItemQuery.class);

        @Override
        public void onRun() throws Exception {
            HttpPlugin.GetRequest request = null;
            User user = mUser;
            String headers[][] = {
                    {UserImpl.HEADER_SESSION_TOKEN, user.getSessionToken()},
                    {APIClientImpl.HEADER_API_KEY, mAPIClient.getApiKey()}
            };
            try {
                String liveEventId = mUserLiveEventId;
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
                    UserLiveEventImpl result = mUser.containerOnQueryOfContainedFromServiceLocked(
                            UserLiveEventImpl.sType, mUserLiveEventImpl, liveEvent);
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
                int status = VR.Result.STATUS_SERVER_RESPONSE_NO_STATUS_CODE;
                try {
                    JSONObject jsonObject = new JSONObject(data);
                    status = jsonObject.getInt("status");
                }
                finally {
                }
                dispatchFailure(status);
            } finally {
                destroy(request);
            }
        }
    }


     /*
     * Update
     */

    static class WorkItemSetPermission extends ClientWorkItem<Result.SetPermission> {

        static final ClientWorkItemType TYPE = new ClientWorkItemType() {
            @Override
            public WorkItemSetPermission newInstance(APIClientImpl apiClient) {
                return new WorkItemSetPermission(apiClient);
            }
        };

        WorkItemSetPermission(APIClientImpl apiClient) {
            super(apiClient, TYPE);
        }

        private UserLiveEventImpl mUserLiveEvent;
        private UserVideo.Permission mPermission;

        synchronized WorkItemSetPermission set(UserLiveEventImpl userLiveEvent,
                                        UserVideo.Permission permission,
                                        Result.SetPermission callback,
                                        Handler handler, Object closure) {
            super.set(callback, handler, closure);
            mUserLiveEvent = userLiveEvent;
            mPermission = permission;
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
            //todo check server
            jsonParam.put("permission", mPermission);
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


    static class WorkItemSetTitle extends ClientWorkItem<VR.Result.SimpleCallback> {

        static final ClientWorkItemType TYPE = new ClientWorkItemType() {
            @Override
            public WorkItemSetTitle newInstance(APIClientImpl apiClient) {
                return new WorkItemSetTitle(apiClient);
            }
        };

        WorkItemSetTitle(APIClientImpl apiClient) {
            super(apiClient, TYPE);
        }

        private UserLiveEventImpl mUserLiveEvent;
        private String mTitle;

        synchronized WorkItemSetTitle set(UserLiveEventImpl userLiveEvent,
                                               String title,
                                               VR.Result.SimpleCallback callback,
                                               Handler handler, Object closure) {
            super.set(callback, handler, closure);
            mUserLiveEvent = userLiveEvent;
            mTitle = title;
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
            //todo check server
            jsonParam.put("title", mTitle);
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



    static class WorkItemSetDescription extends ClientWorkItem<VR.Result.SimpleCallback> {

        static final ClientWorkItemType TYPE = new ClientWorkItemType() {
            @Override
            public WorkItemSetDescription newInstance(APIClientImpl apiClient) {
                return new WorkItemSetDescription(apiClient);
            }
        };

        WorkItemSetDescription(APIClientImpl apiClient) {
            super(apiClient, TYPE);
        }

        private UserLiveEventImpl mUserLiveEvent;
        private String mDescription;

        synchronized WorkItemSetDescription set(UserLiveEventImpl userLiveEvent,
                                          String description,
                                          VR.Result.SimpleCallback callback,
                                          Handler handler, Object closure) {
            super.set(callback, handler, closure);
            mUserLiveEvent = userLiveEvent;
            mDescription = description;
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
            //todo check server
            jsonParam.put("description", mDescription);
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

            HttpPlugin.PutRequest setupRequest = null;
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
                setupRequest  = newPutRequest(uploadUrl, headers1);
                if (null == setupRequest) {
                    dispatchFailure(VR.Result.STATUS_HTTP_PLUGIN_NULL_CONNECTION);
                    return;
                }

                writeBytes(setupRequest, data, jsonStr);

                if (isCancelled()) {
                    dispatchCancelled();
                    return;
                }

                int rsp = getResponseCode(setupRequest);
                String data4 = readHttpStream(setupRequest, "code: " + rsp);
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
                destroy(setupRequest);
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

        str.append(" viewURL=");
        str.append(getViewUrl());

        str.append(" state=");
        str.append(getState().toString());

        str.append(" viewerCount=");
        str.append(getViewerCount());

        str.append(" getSource=");
        str.append(getSource());

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

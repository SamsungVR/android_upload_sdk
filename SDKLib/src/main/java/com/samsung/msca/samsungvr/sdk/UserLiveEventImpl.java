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
import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
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
            return jsonObject.optString("id", null);
        }

        @Override
        Object validateValue(Enum<?> key, Object newValue) {
            if (DEBUG) {
                Log.d(TAG, "key: " + key + " newValue:" + newValue);
            }
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
                    Boolean retVal = Boolean.parseBoolean(newValue.toString());
                    return retVal;
                case STATE:
                    return Util.enumFromString(State.class, newValue.toString());
                case SOURCE:
                    return Util.enumFromString(Source.class, newValue.toString());
                case PERMISSION:
                    return Util.enumFromString(UserVideo.Permission.class, newValue.toString());
                case REACTIONS:
                    UserVideoImpl.ReactionsImpl reactions = new UserVideoImpl.ReactionsImpl();
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
                    if (DEBUG) {
                        Log.d("VRSDK", "newValue: " + newValue);
                    }
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
                    if (DEBUG) {
                        Log.d("VRSDK", "unknown tag: " + key);
                    }
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
                      UserVideo.VideoStereoscopyType videoStereoscopyType, State state,
                      Boolean takedown,
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
                      String viewUrl) {
        this(container, id, title, permission, source,
                description, ingestUrl, viewUrl,
                videoStereoscopyType, State.UNKNOWN,
                false, 0L, 0L, 0L);
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
    public boolean updateLiveEvent(String title, String description,
                                   UserVideo.Permission permission,
                                   VR.Result.SimpleCallback callback,
                                   Handler handler, Object closure) {
        APIClientImpl apiClient = getContainer().getContainer();
        AsyncWorkQueue<ClientWorkItemType, ClientWorkItem<?>> workQueue = apiClient.getAsyncWorkQueue();
        WorkItemUpdateLiveEvent workItem = workQueue.obtainWorkItem(WorkItemUpdateLiveEvent.TYPE);
        workItem.set(this, title, description, permission, callback, handler, closure);
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
    public Boolean hasTakenDown() {
        Boolean retVal = (Boolean)getLocked(Properties.TAKEDOWN);
        if (retVal == null) {
            retVal = false;
        }
        return retVal;
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
    public boolean uploadSegmentAsBytes(byte[] source,
            UserLiveEvent.Result.UploadSegmentAsBytes callback,
            Handler handler, Object closure) {

        AsyncWorkQueue<ClientWorkItemType, ClientWorkItem<?>> workQueue =
                getContainer().getContainer().getAsyncUploadQueue();

        UserLiveEventImpl.WorkItemNewSegmentUploadAsBytes workItem =
                workQueue.obtainWorkItem(UserLiveEventImpl.WorkItemNewSegmentUploadAsBytes.TYPE);
        workItem.set(this, Integer.toString(++mSegmentId), source, callback, handler, closure);
        return workQueue.enqueue(workItem);
    }

    @Override
    public boolean cancelUploadSegment(Object closure) {
        //if (DEBUG) {
            Log.d(TAG, "Cancelled video upload requested with closure: " + closure);
        //}
        AsyncWorkQueue<ClientWorkItemType, ClientWorkItem<?>> workQueue =
                getContainer().getContainer().getAsyncUploadQueue();
        BooleanHolder found = new BooleanHolder();
        workQueue.iterateWorkItems(new AsyncWorkQueue.IterationObserver<ClientWorkItemType, ClientWorkItem<?>>() {
            @Override
            public boolean onIterate(ClientWorkItem workItem, Object... args) {
                Object argClosure = args[0];
                BooleanHolder myFound = (BooleanHolder) args[1];
                AsyncWorkItemType type = workItem.getType();
                if (UserLiveEventImpl.WorkItemNewSegmentUploadAsBytes.TYPE == type) {
                    Object uploadClosure = workItem.getClosure();
          //          if (DEBUG) {
                        Log.d(TAG, "Found video upload related work item " + workItem +
                                " closure: " + uploadClosure);
          //          }
                    if (Util.checkEquals(argClosure, uploadClosure)) {
                        workItem.cancel();
                        myFound.setToTrue();
           //             if (DEBUG) {
                            Log.d(TAG, "Cancelled video upload related work item " + workItem);
            //            }
                    }
                }
                return true;
            }
        }, closure, found);
        boolean ret = found.getValue();
        //if (DEBUG) {
            Log.d(TAG, "Cancelled video upload result: " + ret + " closure: " + closure);
        //}
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
                    Log.e(TAG, "onRun : " + " liveEventId is null! this wont work!");
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
                if (DEBUG) {
                    Log.d(TAG, "onSuccess : " + data);
                }
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
                if (DEBUG) {
                    Log.d(TAG, "onSuccess : " + data);
                }
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
                if (DEBUG) {
                    Log.d(TAG, "onSuccess : " + data);
                }

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
                if (DEBUG) {
                    Log.d(TAG, "onSuccess : " + data);
                }

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
                if (DEBUG) {
                    Log.d(TAG, "onSuccess : " + data);
                }

                JSONObject jsonObject = new JSONObject(data);
                int status = jsonObject.optInt("status", VR.Result.STATUS_SERVER_RESPONSE_NO_STATUS_CODE);;

                dispatchFailure(status);

            } finally {
                destroy(request);
            }
        }
    }


    static class WorkItemUpdateLiveEvent extends ClientWorkItem<VR.Result.SimpleCallback> {

        static final ClientWorkItemType TYPE = new ClientWorkItemType() {
            @Override
            public WorkItemUpdateLiveEvent newInstance(APIClientImpl apiClient) {
                return new WorkItemUpdateLiveEvent(apiClient);
            }
        };

        WorkItemUpdateLiveEvent(APIClientImpl apiClient) {
            super(apiClient, TYPE);
        }

        private UserLiveEventImpl mUserLiveEvent;
        private String mTitle;
        private String mDescription;
        private UserVideo.Permission mPermission;

        synchronized WorkItemUpdateLiveEvent set(UserLiveEventImpl userLiveEvent,
                                                 String title,
                                                 String description,
                                                 UserVideo.Permission permission,
                                                 VR.Result.SimpleCallback callback,
                                                 Handler handler, Object closure) {
            super.set(callback, handler, closure);
            mUserLiveEvent = userLiveEvent;
            mTitle = title;
            mDescription = description;
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
            if (mTitle != null ) {
                jsonParam.put("title", mTitle);
            }
            if (mDescription != null) {
                jsonParam.put("description", mDescription);
            }
            if (mPermission != null) {
                jsonParam.put("permission", mPermission);
            }
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
                if (DEBUG) {
                    Log.d(TAG, "onSuccess : " + data);
                }

                JSONObject jsonObject = new JSONObject(data);
                int status = jsonObject.optInt("status", VR.Result.STATUS_SERVER_RESPONSE_NO_STATUS_CODE);;

                dispatchFailure(status);

            } finally {
                destroy(request);
            }
        }
    }


    static class DigestStream extends ClientWorkItem.HttpUploadStream {

        protected final long mTotalBytes;

        DigestStream(InputStream inner, long total, byte[] ioBuf) {
            super(inner, ioBuf, total <= 0);
            mTotalBytes = total;
        }

        @Override
        protected void onBytesProvided(byte[] data, int offset, int len) {
        }
    }

    static class WorkItemNewSegmentUploadAsBytes extends ClientWorkItem<UserLiveEvent.Result.UploadSegmentAsBytes> {

        private static class SegmentUploadCompleteCallbackNotifier extends Util.CallbackNotifier {

            private final long mDurationInMilliseconds;

            public SegmentUploadCompleteCallbackNotifier(long durationInMilliseconds) {
                mDurationInMilliseconds = durationInMilliseconds;
            }

            @Override
            void notify(Object callback, Object closure) {
                ((UserLiveEvent.Result.UploadSegmentAsBytes)callback).onSegmentUploadComplete(closure,
                        mDurationInMilliseconds);
            }
        }

        static final ClientWorkItemType TYPE = new ClientWorkItemType() {
            @Override
            public UserLiveEventImpl.WorkItemNewSegmentUploadAsBytes newInstance(APIClientImpl apiClient) {
                return new UserLiveEventImpl.WorkItemNewSegmentUploadAsBytes(apiClient);
            }
        };

        WorkItemNewSegmentUploadAsBytes(APIClientImpl apiClient) {
            super(apiClient, TYPE);
        }

        private byte[] mSource;
        private UserLiveEventImpl mUserLiveEvent;
        private String mSegmentId;
        private String mUploadUrl;


        UserLiveEventImpl.WorkItemNewSegmentUploadAsBytes set(UserLiveEventImpl userLiveEvent, String segmentId,
            byte[] source, UserLiveEvent.Result.UploadSegmentAsBytes callback, Handler handler,
            Object closure) {

            super.set(callback, handler, closure);
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


        private class MyDigestStream extends UserLiveEventImpl.DigestStream {

            private MyDigestStream(InputStream inner, long total) {
                super(inner, total, mIOBuf);
            }

            @Override
            protected boolean canContinue() {
                return !isCancelled();
            }

            @Override
            protected void onProgress(long providedSoFar, boolean isEOF) {
                dispatchUncounted(new ProgressCallbackNotifier(providedSoFar, mTotalBytes).setNoLock(mCallbackHolder));
            }
        }

        @Override
        public void onRun() throws Exception {

            HttpPlugin.PutRequest setupRequest = null;

            try {

                long now = SystemClock.elapsedRealtime();
                {
                    User user = mUserLiveEvent.getUser();
                    byte[] source = mSource;
                    long length = source.length;

                    ByteArrayInputStream buf = new ByteArrayInputStream(source);
                    HttpPlugin.PutRequest uploadRequest = null;
                    try {
                        String content_type = "video/MP2T";
                        String postfix = ".ts";
                        if (this.mUserLiveEvent.getSource() == UserLiveEvent.Source.SEGMENTED_MP4) {
                            postfix = ".mp4";
                            content_type = "video/mp4";
                        }
                        String rawAuthString = mUserLiveEvent.getId() + ':' + "password";
                        String auth_code = Base64.encodeToString(rawAuthString.getBytes(),
                                Base64.NO_WRAP);
                        Log.d(TAG, auth_code);

                        String headers0[][] = {
                                null,
                                {HEADER_AUTHORIZATION, "Basic " + auth_code},
                                {HEADER_CONTENT_TYPE, content_type},
                                {HEADER_CONTENT_TRANSFER_ENCODING, "binary"},
                        };

                        headers0[0] = new String[] {HEADER_CONTENT_LENGTH, String.valueOf(length)};

                        String upload_url = this.mUserLiveEvent.getProducerUrl() +
                                "/" +
                                mUserLiveEvent.getId() +
                                "_" +
                                System.currentTimeMillis() / 1000 +
                                "_" +
                                mUserLiveEvent.mSegmentId +
                                postfix;
                        uploadRequest = newRequest(upload_url, HttpMethod.PUT, headers0);
                        if (null == uploadRequest) {
                            dispatchFailure(VR.Result.STATUS_HTTP_PLUGIN_NULL_CONNECTION);
                            return;
                        }

                        MyDigestStream digestStream = new MyDigestStream(buf, length);
                        try {
                            writeHttpStream(uploadRequest, digestStream);
                        } catch (Exception ex) {
                            Log.e(TAG,"",ex);
                            if (isCancelled()) {
                                dispatchCancelled();
                                return;
                            }
                            throw ex;
                        }

                        int rsp2 = getResponseCode(uploadRequest);

                        if (!isHTTPSuccess(rsp2)) {
                            dispatchFailure(UserLiveEvent.Result.UploadSegmentAsBytes.STATUS_SEGMENT_UPLOAD_FAILED);
                            return;
                        }

                        destroy(uploadRequest);
                        uploadRequest = null;


                        Util.CallbackNotifier notifier = new UserLiveEventImpl.WorkItemNewSegmentUploadAsBytes.
                                SegmentUploadCompleteCallbackNotifier(SystemClock.elapsedRealtime() - now).setNoLock(mCallbackHolder);
                        dispatchCounted(notifier);

                    } finally {
                        destroy(uploadRequest);
                        if (null != buf) {
                            buf.close();
                        }
                    }

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

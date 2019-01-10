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
import android.util.Log;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

class APIClientImpl extends Container.BaseImpl implements APIClient {

    private final String mEndPoint, mApiKey;
    private final HttpPlugin.RequestFactory mHttpRequestFactory;

    private final AsyncWorkQueue.AsyncWorkItemFactory mWorkItemFactory =
            new AsyncWorkQueue.AsyncWorkItemFactory<ClientWorkItemType, ClientWorkItem<?>>() {
        @Override
        public ClientWorkItem newWorkItem(ClientWorkItemType type) {
            return type.newInstance(APIClientImpl.this);
        }
    };

    private static final int MUL = 1024;

    private final AsyncWorkQueue.Observer mQueueObserver = new AsyncWorkQueue.Observer() {

        @Override
        public void onQuit(AsyncWorkQueue<?, ?> queue) {
        }
    };

    private final AsyncWorkQueue<ClientWorkItemType, ClientWorkItem<?>> mAsyncWorkQueue = new AsyncWorkQueue(mWorkItemFactory, 8 * MUL, mQueueObserver, 0);
    private final AsyncWorkQueue<ClientWorkItemType, ClientWorkItem<?>> mAsyncUploadQueue = new AsyncWorkQueue(mWorkItemFactory, 1024 * MUL, mQueueObserver, 0);

    static final String HEADER_API_KEY = "X-API-KEY";

    static boolean newInstance(String endPoint, String apiKey, HttpPlugin.RequestFactory httpRequestFactory,
                               APIClient.Result.Init callback, Handler handler, Object closure) {
        APIClientImpl result = new APIClientImpl(endPoint, apiKey, httpRequestFactory);
        if (null != callback) {
            new Util.SuccessWithResultCallbackNotifier<APIClient>(result).setNoLock(callback, handler, closure).post();
        }
        return true;
    }

    private enum State {
        INITIALIZED,
        DESTROYING,
        DESTROYED
    }

    private final StateManager<APIClient> mStateManager;

    private APIClientImpl(String endPoint, String apiKey, HttpPlugin.RequestFactory httpRequestFactory) {
        registerType(UserImpl.sType, true);
        registerType(UnverifiedUserImpl.sType, false);

        mEndPoint = endPoint;
        mApiKey = apiKey;
        mHttpRequestFactory = httpRequestFactory;
        mStateManager = new StateManager<>((APIClient)this, State.INITIALIZED);
    }

    HttpPlugin.RequestFactory getRequestFactory() {
        return mHttpRequestFactory;
    }


    @Override
    synchronized public boolean destroy() {
        if (!mStateManager.isInState(State.INITIALIZED)) {
            return false;
        }
        mStateManager.setState(State.DESTROYING);
        mAsyncWorkQueue.quit();
        mAsyncUploadQueue.quit();
        mStateManager.setState(State.DESTROYED);
        if (null != mDestroyCallback) {
            new Util.SuccessCallbackNotifier().setNoLock(mDestroyCallback).post();
        }
        //if (DEBUG) {
//            Log.d(TAG, "Destroyed api client endpoint: " + mEndPoint + " apiKey: " + mApiKey + " obj: " + Util.getHashCode(this));
        //}
        return true;
    }

    private ResultCallbackHolder mDestroyCallback;

    @Override
    synchronized public boolean destroyAsync(APIClient.Result.Destroy callback, Handler handler, Object closure) {
        if (!mStateManager.isInState(State.INITIALIZED)) {
            return false;
        }
        mStateManager.setState(State.DESTROYING);
        mDestroyCallback = new ResultCallbackHolder().setNoLock(callback, handler, closure);
        mAsyncWorkQueue.quitAsync();
        mAsyncUploadQueue.quitAsync();
        return true;
    }

    @Override
    public boolean getRegionInfo(VR.Result.GetRegionInfo callback, Handler handler, Object closure) {

        WorkItemGetRegionInfo workItem = mAsyncWorkQueue.obtainWorkItem(WorkItemGetRegionInfo.TYPE);
        workItem.set(callback, handler, closure);
        return mAsyncWorkQueue.enqueue(workItem);
    }


    @Override
    public boolean getRegionInfoEx(String sessionToken, String regionCode,
                                   VR.Result.GetRegionInfo callback,
                                   Handler handler, Object closure) {

        WorkItemGetRegionInfoEx workItem = mAsyncWorkQueue.obtainWorkItem(WorkItemGetRegionInfoEx.TYPE);
        workItem.set(sessionToken, regionCode, callback, handler, closure);
        return mAsyncWorkQueue.enqueue(workItem);
    }

    @Override
    public String getEndPoint() {
        return mEndPoint;
    }

    @Override
    public String getApiKey() {
        return mApiKey;
    }

    @Override
    public AsyncWorkQueue<ClientWorkItemType, ClientWorkItem<?>> getAsyncWorkQueue() {
        return mAsyncWorkQueue;
    }

    @Override
    public AsyncWorkQueue<ClientWorkItemType, ClientWorkItem<?>> getAsyncUploadQueue() {
        return mAsyncUploadQueue;
    }

    @Override
    public boolean login(String email, String password, VR.Result.Login callback, Handler handler, Object closure) {
        WorkItemPerformLogin workItem = mAsyncWorkQueue.obtainWorkItem(WorkItemPerformLogin.TYPE);
        workItem.set(email, password, callback, handler, closure);
        return mAsyncWorkQueue.enqueue(workItem);
    }


    @Override
    public boolean loginSamsungAccount(String samsung_sso_token, String api_server, String  auth_server,
                                        VR.Result.Login callback, Handler handler, Object closure) {
        WorkItemPerformLoginSamsungAccount workItem = mAsyncWorkQueue.obtainWorkItem(WorkItemPerformLoginSamsungAccount.TYPE);
        workItem.set(samsung_sso_token, api_server, auth_server, callback, handler, closure);
        return mAsyncWorkQueue.enqueue(workItem);
    }

    private static final boolean DEBUG = Util.DEBUG;
    private static final String TAG = Util.getLogTag(APIClientImpl.class);

    @Override
    public User getUserById(String userId) {
        return getContainedByIdLocked(UserImpl.sType, userId);
    }

    @Override
    public boolean getUserBySessionToken(String sessionToken, VR.Result.GetUserBySessionToken callback,
                               Handler handler, Object closure) {
        WorkItemGetUserBySessionToken workItem = mAsyncWorkQueue.obtainWorkItem(WorkItemGetUserBySessionToken.TYPE);
        workItem.set(sessionToken, callback, handler, closure);
        return mAsyncWorkQueue.enqueue(workItem);
    }


    @Override
    public <CONTAINED extends Contained.Spec> List<CONTAINED> containerOnQueryListOfContainedFromServiceLocked(Contained.Type type, JSONObject jsonObject) {
        return null;
    }

    @Override
    public <CONTAINED extends Contained.Spec> CONTAINED containerOnQueryOfContainedFromServiceLocked(Contained.Type type, CONTAINED contained, JSONObject jsonObject) {
        return null;
    }

    @Override
    public <CONTAINED extends Contained.Spec> CONTAINED containerOnCreateOfContainedInServiceLocked(Contained.Type type, JSONObject jsonObject) {
        CONTAINED result = processCreateOfContainedInServiceLocked(type, jsonObject, true);
        if (DEBUG) {
            Log.d(TAG, "Add contained: " + result);
        }
        return result;
    }

    @Override
    public <CONTAINED extends Contained.Spec> CONTAINED containerOnUpdateOfContainedToServiceLocked(Contained.Type type, CONTAINED contained) {
        return null;
    }

    @Override
    public <CONTAINED extends Contained.Spec> CONTAINED containerOnDeleteOfContainedFromServiceLocked(Contained.Type type, CONTAINED contained) {
        return null;
    }

    static class WorkItemGetUserBySessionToken extends ClientWorkItem<VR.Result.GetUserBySessionToken> {

        static final ClientWorkItemType TYPE = new ClientWorkItemType() {
            @Override
            public WorkItemGetUserBySessionToken newInstance(APIClientImpl apiClient) {
                return new WorkItemGetUserBySessionToken(apiClient);
            }
        };

        WorkItemGetUserBySessionToken(APIClientImpl apiClient) {
            super(apiClient, TYPE);
        }

        private String mSessionToken;

        synchronized WorkItemGetUserBySessionToken set(String sessionToken,
            VR.Result.GetUserBySessionToken callback, Handler handler, Object closure) {

            super.set(callback, handler, closure);
            mSessionToken = sessionToken;
            return this;
        }

        @Override
        protected synchronized void recycle() {
            super.recycle();
            mSessionToken = null;
        }

        private static final String TAG = Util.getLogTag(WorkItemGetUserBySessionToken.class);

        @Override
        public void onRun() throws Exception {
            HttpPlugin.GetRequest request = null;
            String cookies[][] = {
                {"session_id", mSessionToken}
            };

            String headers[][] = {
                    {HEADER_COOKIE, toCookieString(cookies)},
                    {HEADER_API_KEY, mAPIClient.getApiKey()}
            };
            try {

                request = newGetRequest("user/authenticate", headers);

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

                JSONObject jsonObject = new JSONObject(data);

                if (isHTTPSuccess(rsp)) {
                    if (!jsonObject.optBoolean("authenticated", false)) {
                        dispatchFailure(VR.Result.GetUserBySessionToken.STATUS_TOKEN_INVALID_OR_EXPIRED);
                    } else {
                        User user = mAPIClient.containerOnCreateOfContainedInServiceLocked(UserImpl.sType, jsonObject);
                        if (null != user) {
                            dispatchSuccessWithResult(user);
                        } else {
                            dispatchFailure(VR.Result.STATUS_SERVER_RESPONSE_INVALID);
                        }
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

    static class WorkItemPerformLogin extends ClientWorkItem<VR.Result.Login> {

        static final ClientWorkItemType TYPE = new ClientWorkItemType() {
            @Override
            public WorkItemPerformLogin newInstance(APIClientImpl apiClient) {
                return new WorkItemPerformLogin(apiClient);
            }
        };

        WorkItemPerformLogin(APIClientImpl apiClient) {
            super(apiClient, TYPE);
        }

        private String mEmail, mPassword;

        synchronized WorkItemPerformLogin set(String email, String password,
            VR.Result.Login callback, Handler handler, Object closure) {

            super.set(callback, handler, closure);
            mEmail = email;
            mPassword = password;

            return this;
        }

        @Override
        protected synchronized void recycle() {
            super.recycle();
            mEmail = null;
            mPassword = null;
        }

        private static final String TAG = Util.getLogTag(WorkItemPerformLogin.class);

        @Override
        public void onRun() throws Exception {
            HttpPlugin.PostRequest request = null;
            try {

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("email", mEmail);
                jsonParam.put("password", mPassword);

                String jsonStr = jsonParam.toString();
                byte[] data = jsonStr.getBytes(StandardCharsets.UTF_8);

                String headers[][] = {
                        {HEADER_CONTENT_LENGTH, String.valueOf(data.length)},
                        {HEADER_CONTENT_TYPE, "application/json" + ClientWorkItem.CONTENT_TYPE_CHARSET_SUFFIX_UTF8},
                        {HEADER_API_KEY, mAPIClient.getApiKey()}
                };

                request = newPostRequest("user/authenticate", headers);
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
                String data2 = readHttpStream(request, "code: " + rsp);
                if (null == data2) {
                    dispatchFailure(VR.Result.STATUS_HTTP_PLUGIN_STREAM_READ_FAILURE);
                    return;
                }
                JSONObject jsonObject = new JSONObject(data2);

                if (isHTTPSuccess(rsp)) {
                    User user = mAPIClient.containerOnCreateOfContainedInServiceLocked(UserImpl.sType, jsonObject);

                    if (null != user) {
                        dispatchSuccessWithResult(user);
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


    static class WorkItemPerformLoginSamsungAccount extends ClientWorkItem<VR.Result.Login> {

        /*
            authentication = {
                0: "Success",
                1: "Invalid username or password",
                2: "Account is locked due to excessive failed login attempts",
                3: "Invalid username or password, account lockout imminent",  # deprecated
                4: "Account not yet activated",
                5: "Unknown user", # deprecated
                6: "Invalid username or password",  # duplicate msg of 1
                7: "Invalid or expired SSO token",
                9: "Unable to verify Samsung SSO account",
                10: "Unable to retrieve Samsung SSO account profile",
                11: "Samsung Account not registered with Samsung VR, please register first", #deprecated
                12: "An account is already registered with an email address matching your Samsung Account profile", #deprecated
                13: "Invalid regional server",
                14: "Unable to register account, server error",
                15: "Invalid authentication type",
            }
        */

        static final ClientWorkItemType TYPE = new ClientWorkItemType() {
            @Override
            public WorkItemPerformLoginSamsungAccount newInstance(APIClientImpl apiClient) {
                return new WorkItemPerformLoginSamsungAccount(apiClient);
            }
        };

        WorkItemPerformLoginSamsungAccount(APIClientImpl apiClient) {
            super(apiClient, TYPE);
        }

        private String mSamsungSSOToken, mApiServer, mAuthServer;

        synchronized WorkItemPerformLoginSamsungAccount set(String samsung_sso_token,
            String api_server, String auth_server,
            VR.Result.Login callback, Handler handler, Object closure) {

            super.set(callback, handler, closure);
            mSamsungSSOToken = samsung_sso_token;
            mApiServer = api_server;
            mAuthServer = auth_server;

            return this;
        }

        @Override
        protected synchronized void recycle() {
            super.recycle();
            mSamsungSSOToken = null;
            mApiServer = null;
            mAuthServer = null;
        }

        private static final String TAG = Util.getLogTag(WorkItemPerformLoginSamsungAccount.class);

        @Override
        public void onRun() throws Exception {
            HttpPlugin.PostRequest request = null;
            try {

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("auth_type", "SamsungSSO");
                jsonParam.put("samsung_sso_token", mSamsungSSOToken);
                if (mApiServer != null) {
                    jsonParam.put("api_hostname", mApiServer);
                }
                if (mAuthServer != null) {
                    jsonParam.put("auth_hostname", mAuthServer);
                }

                String jsonStr = jsonParam.toString();
                byte[] data = jsonStr.getBytes(StandardCharsets.UTF_8);

                String headers[][] = {
                        {HEADER_CONTENT_LENGTH, String.valueOf(data.length)},
                        {HEADER_CONTENT_TYPE, "application/json" + ClientWorkItem.CONTENT_TYPE_CHARSET_SUFFIX_UTF8},
                        {HEADER_API_KEY, mAPIClient.getApiKey()}
                };

                request = newPostRequest("user/authenticate", headers);
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
                String data2 = readHttpStream(request, "code: " + rsp);
                if (null == data2) {
                    dispatchFailure(VR.Result.STATUS_HTTP_PLUGIN_STREAM_READ_FAILURE);
                    return;
                }
                JSONObject jsonObject = new JSONObject(data2);

                if (isHTTPSuccess(rsp)) {
                    User user = mAPIClient.containerOnCreateOfContainedInServiceLocked(UserImpl.sType, jsonObject);

                    if (null != user) {
                        dispatchSuccessWithResult(user);
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


    protected static class RegionInfoImpl implements VR.RegionInfo {


        protected String mClientRegion;
        protected boolean mUGCCountry;

        public String getClientRegion() {
            return mClientRegion;
        }

        public boolean isUGCCountry() {
            return mUGCCountry;
        }
    }


    static class WorkItemGetRegionInfo extends ClientWorkItem<VR.Result.GetRegionInfo> {


        static final ClientWorkItemType TYPE = new ClientWorkItemType() {
            @Override
            public WorkItemGetRegionInfo newInstance(APIClientImpl apiClient) {
                return new WorkItemGetRegionInfo(apiClient);
            }
        };

        WorkItemGetRegionInfo(APIClientImpl apiClient) {
            super(apiClient, TYPE);
        }


        @Override
        protected synchronized void recycle() {
            super.recycle();
        }

        private static final String TAG = Util.getLogTag(WorkItemGetRegionInfo.class);

        @Override
        public void onRun() throws Exception {
            HttpPlugin.GetRequest request = null;

            String headers[][] = {
                    {APIClientImpl.HEADER_API_KEY, mAPIClient.getApiKey()},
            };


            try {
                request = newGetRequest(String.format(Locale.US, "info"), headers);
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

                if (isHTTPSuccess(rsp)) {
                    JSONObject jsonObject = new JSONObject(data);
                    RegionInfoImpl regionInfo = new RegionInfoImpl();
                    regionInfo.mUGCCountry = jsonObject.getBoolean("isUGCCountry");
                    regionInfo.mClientRegion = jsonObject.getString("region");
                    dispatchSuccessWithResult(regionInfo);
                    return;
                }

                dispatchFailure(VR.Result.STATUS_SERVER_RESPONSE_NO_STATUS_CODE);

            } finally {
                destroy(request);
            }
        }
    }


    static class WorkItemGetRegionInfoEx extends ClientWorkItem<VR.Result.GetRegionInfo> {


        static final ClientWorkItemType TYPE = new ClientWorkItemType() {
            @Override
            public WorkItemGetRegionInfoEx newInstance(APIClientImpl apiClient) {
                return new WorkItemGetRegionInfoEx(apiClient);
            }
        };


        private String mSessionToken, mRegionCode;

        synchronized WorkItemGetRegionInfoEx set(String sessionToken, String regionCode,
                                               VR.Result.GetRegionInfo callback, Handler handler, Object closure) {

            super.set(callback, handler, closure);
            mSessionToken = sessionToken;
            mRegionCode = regionCode;
            return this;
        }

        WorkItemGetRegionInfoEx(APIClientImpl apiClient) {
            super(apiClient, TYPE);
        }


        @Override
        protected synchronized void recycle() {
            super.recycle();
        }

        private static final String TAG = Util.getLogTag(WorkItemGetRegionInfoEx.class);

        @Override
        public void onRun() throws Exception {
            HttpPlugin.GetRequest request = null;

            String headers[][] = {
                    {APIClientImpl.HEADER_API_KEY, mAPIClient.getApiKey()},
            };

            if (mSessionToken != null) {
                //add session token header
            }

            try {
                String urlChunk = "info";
                if (mRegionCode != null) {
                    urlChunk += "?region=";
                    urlChunk += mRegionCode;
                }
                request = newGetRequest(String.format(Locale.US, urlChunk), headers);
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

                if (isHTTPSuccess(rsp)) {
                    JSONObject jsonObject = new JSONObject(data);
                    RegionInfoImpl regionInfo = new RegionInfoImpl();
                    regionInfo.mUGCCountry = jsonObject.getBoolean("isUGCCountry");
                    regionInfo.mClientRegion = jsonObject.getString("region");
                    dispatchSuccessWithResult(regionInfo);
                    return;
                }

                dispatchFailure(VR.Result.STATUS_SERVER_RESPONSE_NO_STATUS_CODE);

            } finally {
                destroy(request);
            }
        }
    }
}

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

//        if (DEBUG) {
//            Log.d(TAG, "Created api client endpoint: " + mEndPoint + " apiKey: " + mApiKey + " obj: " + Util.getHashCode(this));
//        }
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
        if (DEBUG) {
//            Log.d(TAG, "Destroyed api client endpoint: " + mEndPoint + " apiKey: " + mApiKey + " obj: " + Util.getHashCode(this));
        }
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
        if (DEBUG) {
//            Log.d(TAG, "Destroyed api client endpoint: " + mEndPoint + " apiKey: " + mApiKey + " obj: " + Util.getHashCode(this));
        }
        return true;
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
    public boolean loginSamsungAccount(String samsung_sso_token, String  auth_server, VR.Result.LoginSSO callback, Handler handler, Object closure) {
        WorkItemPerformLoginSamsungAccount workItem = mAsyncWorkQueue.obtainWorkItem(WorkItemPerformLoginSamsungAccount.TYPE);
        workItem.set(samsung_sso_token, auth_server, callback, handler, closure);
        return mAsyncWorkQueue.enqueue(workItem);
    }



    @Override
    public boolean newUser(String name, String email, String password, VR.Result.NewUser callback,
                           Handler handler, Object closure) {
        WorkItemCreateNewUser workItem = mAsyncWorkQueue.obtainWorkItem(WorkItemCreateNewUser.TYPE);
        workItem.set(name, email, password, callback, handler, closure);
        return mAsyncWorkQueue.enqueue(workItem);
    }

    private static final boolean DEBUG = Util.DEBUG;
    private static final String TAG = Util.getLogTag(APIClientImpl.class);

    @Override
    public User getUserById(String userId) {
        return getContainedByIdLocked(UserImpl.sType, userId);
    }

    @Override
    public boolean getUserBySessionId(String sessionId, VR.Result.GetUserBySessionId callback,
                               Handler handler, Object closure) {
        WorkItemGetUserBySessionId workItem = mAsyncWorkQueue.obtainWorkItem(WorkItemGetUserBySessionId.TYPE);
        workItem.set(sessionId, callback, handler, closure);
        return mAsyncWorkQueue.enqueue(workItem);
    }


    @Override
    public boolean getUserBySessionToken( String userId, String sessionToken, VR.Result.GetUserBySessionToken callback,
                               Handler handler, Object closure) {
        WorkItemGetUserBySessionToken workItem = mAsyncWorkQueue.obtainWorkItem(WorkItemGetUserBySessionToken.TYPE);
        workItem.set(userId, sessionToken, callback, handler, closure);
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


    static class WorkItemGetUserBySessionId extends ClientWorkItem<VR.Result.GetUserBySessionId> {

        static final ClientWorkItemType TYPE = new ClientWorkItemType() {
            @Override
            public WorkItemGetUserBySessionId newInstance(APIClientImpl apiClient) {
                return new WorkItemGetUserBySessionId(apiClient);
            }
        };

        WorkItemGetUserBySessionId(APIClientImpl apiClient) {
            super(apiClient, TYPE);
        }

        private String mSessionId;

        synchronized WorkItemGetUserBySessionId set(String sessionId,
            VR.Result.GetUserBySessionId callback, Handler handler, Object closure) {

            super.set(callback, handler, closure);
            mSessionId = sessionId;
            return this;
        }

        @Override
        protected synchronized void recycle() {
            super.recycle();
            mSessionId = null;
        }

        private static final String TAG = Util.getLogTag(WorkItemGetUserBySessionId.class);

        @Override
        public void onRun() throws Exception {
            HttpPlugin.GetRequest request = null;
            String cookies[][] = {
                {"session_id", mSessionId}
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
        private String mUserId;


        synchronized WorkItemGetUserBySessionToken set(String userId, String sessionToken,
                VR.Result.GetUserBySessionToken callback, Handler handler, Object closure) {
            super.set(callback, handler, closure);
            mSessionToken = sessionToken;
            mUserId = userId;
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


            String headers[][] = {
                    {UserImpl.HEADER_SESSION_TOKEN, mSessionToken},
                    {APIClientImpl.HEADER_API_KEY, mAPIClient.getApiKey()},
            };


            try {

                request = newGetRequest(String.format(Locale.US, "user/%s", mUserId), headers);

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


    static class WorkItemPerformLoginSamsungAccount extends ClientWorkItem<VR.Result.LoginSSO> {

        /*
            >>authentication = {
                  0: "Success",
                  1: "Invalid username or password",
                  2: "Account is locked due to excessive failed login attempts",
                  3: "Invalid username or password, account lockout imminent",  # deprecated
                  4: "Account not yet activated",
                  5: "Unknown user",
                  6: "Invalid username or password",  # duplicate msg of 1
                  7: "Invalid or expired SSO token",
                  9: "Unable to verify Samsung SSO account",
                  10: "Unable to retrieve Samsung SSO account profile",
                  11: "Samsung Account not registered with Samsung VR, please register first",
                  12: "Invalid authentication type",
              }

            >>registration = {
                  0: "Success",
                  1: "Missing fields(s) - user_name, user_id or password not specified",
                  2: "Name too short - user name must be at least 3 chars",
                  3: "Password is too weak",
                  4: "email bad form",
                  5: "Password cannot contain email address",
                  6: "Password cannot contain user name",
                  7: "A user is already registered with this email address",
                  8: "Unable to create account; account already created",
                  9: "Unable to verify Samsung SSO account",
                  10: "Unable to retrieve Samsung SSO account profile",
                  11: "Invalid or expired SSO token",
                  12: "An account is already registered with an email address matching your Samsung Account profile",
                  13: "Invalid regional server",
                  14: "Unable to register account, server error",
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

        private String mSamsungSSOToken, mAuthServer;

        synchronized WorkItemPerformLoginSamsungAccount set(String samsung_sso_token, String auth_server,
            VR.Result.LoginSSO callback, Handler handler, Object closure) {

            super.set(callback, handler, closure);
            mSamsungSSOToken = samsung_sso_token;
            mAuthServer = auth_server;

            return this;
        }

        @Override
        protected synchronized void recycle() {
            super.recycle();
            mSamsungSSOToken = null;
            mAuthServer = null;
        }

        private static final String TAG = Util.getLogTag(WorkItemPerformLoginSamsungAccount.class);

        private boolean tryLogin(ObjectHolder<Integer> statusOut) throws Exception {

            HttpPlugin.PostRequest request = null;
            try {

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("auth_type", "SamsungSSO");
                jsonParam.put("samsung_sso_token", mSamsungSSOToken);
                if (mAuthServer != null) {
                    jsonParam.put("auth_server", mAuthServer);
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
                    return true;
                }

                writeBytes(request, data, jsonStr);

                if (isCancelled()) {
                    dispatchCancelled();
                    return true;
                }

                int rsp = getResponseCode(request);
                String data2 = readHttpStream(request, "code: " + rsp);
                if (null == data2) {
                    dispatchFailure(VR.Result.STATUS_HTTP_PLUGIN_STREAM_READ_FAILURE);
                    return true;
                }
                JSONObject jsonObject = new JSONObject(data2);

                if (isHTTPSuccess(rsp)) {
                    User user = mAPIClient.containerOnCreateOfContainedInServiceLocked(UserImpl.sType, jsonObject);

                    if (null != user) {
                        dispatchSuccessWithResult(user);
                    } else {
                        dispatchFailure(VR.Result.STATUS_SERVER_RESPONSE_INVALID);
                    }
                    return true;
                }
                int status = jsonObject.optInt("status", VR.Result.STATUS_SERVER_RESPONSE_NO_STATUS_CODE);
                if (status == VR.Result.STATUS_SERVER_RESPONSE_NO_STATUS_CODE) {
                    dispatchFailure(status);
                    return true;
                }
                statusOut.set(Integer.valueOf(status));

                return false;

            } finally {
                destroy(request);
            }
        }

        private boolean tryRegister() throws Exception {

            HttpPlugin.PostRequest request = null;
            try {
                JSONObject jsonParam = new JSONObject();
                jsonParam.put("auth_type", "SamsungSSO");
                jsonParam.put("samsung_sso_token", mSamsungSSOToken);
                if (mAuthServer != null) {
                    jsonParam.put("auth_server", mAuthServer);
                }
                String jsonStr = jsonParam.toString();
                byte[] data = jsonStr.getBytes(StandardCharsets.UTF_8);

                String headers[][] = {
                        {HEADER_CONTENT_LENGTH, String.valueOf(data.length)},
                        {HEADER_CONTENT_TYPE, "application/json" + ClientWorkItem.CONTENT_TYPE_CHARSET_SUFFIX_UTF8},
                        {HEADER_API_KEY, mAPIClient.getApiKey()}
                };

                request = newPostRequest("user", headers);
                if (null == request) {
                    dispatchFailure(VR.Result.STATUS_HTTP_PLUGIN_NULL_CONNECTION);
                    return true;
                }

                writeBytes(request, data, jsonStr);

                if (isCancelled()) {
                    dispatchCancelled();
                    return true;
                }

                int rsp = getResponseCode(request);
                String data2 = readHttpStream(request, "code: " + rsp);
                if (null == data2) {
                    dispatchFailure(VR.Result.STATUS_HTTP_PLUGIN_STREAM_READ_FAILURE);
                    return true;
                }
                if (isHTTPSuccess(rsp)) {
                    return false;
                }
                JSONObject jsonObject = new JSONObject(data2);
                int status = jsonObject.optInt("status", VR.Result.STATUS_SERVER_RESPONSE_NO_STATUS_CODE);
                dispatchFailure(status);

                return true;
            } finally {
                destroy(request);
            }
        }

        @Override
        public void onRun() throws Exception {
            ObjectHolder<Integer> status = new ObjectHolder<>();

            if (tryLogin(status)) {
                return;
            }
            int statusInt = status.get();
            Log.d(TAG, "Login failed with status " + statusInt);
            if (11 == statusInt) {
                if (tryRegister()) {
                    return;
                }
                if (tryLogin(status)) {
                    return;
                }
                statusInt = status.get();
            }
            dispatchFailure(statusInt);
        }
    }



    static class WorkItemCreateNewUser extends ClientWorkItem<VR.Result.NewUser> {

        static final ClientWorkItemType TYPE = new ClientWorkItemType() {
            @Override
            public WorkItemCreateNewUser newInstance(APIClientImpl apiClient) {
                return new WorkItemCreateNewUser(apiClient);
            }
        };

        WorkItemCreateNewUser(APIClientImpl apiClient) {
            super(apiClient, TYPE);
        }

        private String mEmail, mPassword, mName;

        synchronized WorkItemCreateNewUser set(String name, String email, String password,
            VR.Result.NewUser callback, Handler handler, Object closure) {

            super.set(callback, handler, closure);
            mEmail = email;
            mPassword = password;
            mName = name;

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
                jsonParam.put("user_name", mName);
                jsonParam.put("email", mEmail);
                jsonParam.put("password", mPassword);

                String jsonStr = jsonParam.toString();
                byte[] data = jsonStr.getBytes(StandardCharsets.UTF_8);

                String headers[][] = {
                        {HEADER_CONTENT_LENGTH, String.valueOf(data.length)},
                        {HEADER_CONTENT_TYPE, "application/json" + ClientWorkItem.CONTENT_TYPE_CHARSET_SUFFIX_UTF8},
                        {HEADER_API_KEY, mAPIClient.getApiKey()}
                };

                request = newPostRequest("user", headers);
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
                    UnverifiedUser user = mAPIClient.containerOnCreateOfContainedInServiceLocked(UnverifiedUserImpl.sType, jsonObject);

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

}

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


import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.util.Log;


public class VR {

    private static final String TAG = Util.getLogTag(VR.class);

    private static APIClient sAPIClient;
    private static Result.Init sCallback;
    private static Handler sCallbackHandler;


    /**
     * Initialize the SDK.
     *
     * @param context Android context, not null
     * @param endPoint The Server endpoint to communicate with, not null
     * @param apiKey The API key provided for your application by Samsung, not null
     * @param factory A HTTP transport plugin that handles all HTTP communication.  The SDK
     *                has not been buit with any HTTP transport library. Not null.
     * @param callback Provides async notification whether the init succeeded. Can be null.
     * @return true if init was never performed and is in progress, false otherwise.
     */

    public static synchronized boolean init(Context context, String endPoint, String apiKey,
                Result.Init callback, HttpPlugin.RequestFactory factory) {
        if (null != sAPIClient) {
            return false;
        }
        sCallback = callback;
        sAPIClient = new APIClientImpl(context.getApplicationContext(), endPoint, apiKey, factory);
        if (null != sCallback) {
            sCallbackHandler = new Handler(Looper.getMainLooper());
            sCallbackHandler.post(new Runnable() {
                @Override
                public void run() {
                    sCallback.onSuccess();
                }
            });

        }
        return true;
    }

    /**
     * Destroy the SDK. Any calls made to SDK or its objects will fail after this.  Call this
     * to cleanup SDK resources when SDK services are no longer needed.
     *
     * @return true if destroy was not called after the last init and destroy is in progress,
     *         false otherwise.
     */

    public static synchronized boolean destroy() {
        if (null != sAPIClient) {
            sAPIClient.destroy();
            sAPIClient = null;
            sCallback = null;
            sCallbackHandler = null;
            return true;
        }
        return false;
    }

    private static final boolean DEBUG = Util.DEBUG;


    /**
     * Login as user
     *
     * @param email The email address associated with the user's account, not null
     * @param password The password of the user, not null
     * @param callback A callback to receive results async. May be null.
     * @param handler A handler on which callback should be called. If null, main handler is used.
     * @param closure An object that the application can use to uniquely identify this request.
     *                See callback documentation.
     * @return true if the SDK was initialized and the request could be scheduled, false otherwise.
     */

    public static synchronized boolean login(String email, String password,
            Result.Login callback, Handler handler, Object closure) {
        if (null == sAPIClient) {
            return false;
        }
        if (DEBUG) {
            Log.d(TAG, String.format("authenticate called. email=%s password=%s ", email, password));
        }
        return sAPIClient.login(email, password, callback, handler, closure);
    }

    /**
     * Given a session id, retrieve the corresponding user.  This is useful in situations where
     * there user is logged in on a browser and the User needs to be determined using the
     * Session Id available to the browser.
     *
     * @param sessionId Session id available to the browser
     * @param callback A callback to receive results async. May be null.
     * @param handler A handler on which callback should be called. If null, main handler is used.
     * @param closure An object that the application can use to uniquely identify this request.
     *                See callback documentation.
     * @return true if the SDK was initialized and the request could be scheduled, false otherwise.
     */

    public static synchronized boolean getUserBySessionId(String sessionId,
            Result.GetUserBySessionId callback, Handler handler, Object closure) {
        if (null == sAPIClient) {
            return false;
        }
        if (DEBUG) {
            Log.d(TAG, String.format("getUserBySessionId called. id=%s", sessionId));
        }
        return sAPIClient.getUserBySessionId(sessionId, callback, handler, closure);
    }

    @Deprecated
    public static synchronized boolean getUserBySessionToken(String sessionToken,
        Result.GetUserBySessionToken callback, Handler handler, Object closure) {
        if (null == sAPIClient) {
            return false;
        }
        if (DEBUG) {
            Log.d(TAG, String.format("getUserBySessionToken called. id=%s", sessionToken));
        }
        return sAPIClient.getUserBySessionToken(sessionToken, callback, handler, closure);
    }

    /**
     * Given a user id, retrieve the corresponding user from the local cache.  This does not make
     * a request to the server.
     *
     * @param userId Not null.
     * @return A non null user object if found, null otherwise
     */

    public static synchronized User getUserById(String userId) {
        if (null == sAPIClient) {
            return null;
        }
        return sAPIClient.getUserById(userId);
    }

    /**
     * Returns the end point that this SDK is currently working with - provided by application
     * on the init call.
     *
     * @return A non null string if init was performed with a valid end point, null otherwise
     */

    public static synchronized String getEndPoint() {
        if (null == sAPIClient) {
            return null;
        }
        return sAPIClient.getEndPoint();
    }

    /**
     * Returns the api key that this SDK is currently using - provided by application
     * on the init call.
     *
     * @return A non null string if init was performed with a valid end point, null otherwise
     */

    public static synchronized String getApiKey() {
        if (null == sAPIClient) {
            return null;
        }
        return sAPIClient.getApiKey();

    }

    /**
     * The result class is used as a grouping (akin to namespace) for callbacks that provide
     * results for requests asynchronously. Results are dispatched to Handlers provided by the
     * application and callbacks are called on the Threads backing the Handlers.
     */

    public static final class Result {

        private Result() {
        }


        /**
         * These status codes are common across all requests.  Some of them, for example
         * STATUS_MISSING_API_KEY, STATUS_INVALID_API_KEY are not meant for the end user.
         */

        /**
         * User id in HTTP header is invalid
         */

        public static final int STATUS_INVALID_USER_ID = 1002;

        /**
         * Session token in HTTP header is missing or invalid
         */

        public static final int STATUS_MISSING_X_SESSION_TOKEN_HEADER = 1003;
        public static final int STATUS_INVALID_SESSION = 1004;

        /**
         * API key in HTTP header is missing or invalid
         */

        public static final int STATUS_MISSING_API_KEY = 1005;
        public static final int STATUS_INVALID_API_KEY = 1006;

        private static final int STATUS_HTTP_BASE = 1 << 16;

        /**
         * Http Plugin returned a NULL connection object
         */

        public static final int STATUS_HTTP_PLUGIN_NULL_CONNECTION = STATUS_HTTP_BASE | 1;

        /**
         * Http Plugin provided input stream for reading server responses could not be read
         */

        public static final int STATUS_HTTP_PLUGIN_STREAM_READ_FAILURE = STATUS_HTTP_BASE | 2;


        private static final int STATUS_SERVER_RESPONSE_BASE = 2 << 16;

        /**
         * We were unable to process the server response json. Possible causes are response
         * did not contain JSON, or JSON is missing important data
         */
        public static final int STATUS_SERVER_RESPONSE_INVALID = STATUS_SERVER_RESPONSE_BASE | 1;

        /**
         * The server returned a HTTP Error code, but no JSON based status was found.
         */

        public static final int STATUS_SERVER_RESPONSE_NO_STATUS_CODE = STATUS_SERVER_RESPONSE_BASE | 2;

        private static final int STATUS_SDK_BASE = 3 << 16;

        /**
         * This feature is not supported
         */

        public static final int STATUS_FEATURE_NOT_SUPPORTED = STATUS_SDK_BASE | 1;

        /**
         * The initializer group of callbacks. Used by VR.init()
         */

        public interface Init {

            /**
             * The SDK library was initialized successfully
             */
            void onSuccess();

            /**
             * Failure status codes
             */

            int STATUS_INVALID_ARGS = 1;

            /**
             * The SDK library initialized failed.
             *
             * @param status Failure status code
             */

            void onFailure(int status);
        }


        /**
         * Base interface for other result callback groups.
         */

        public interface BaseCallback {

            /**
             * This request was cancelled.  The request is identified by the closure param.
             *
             * @param closure Application provided object used to identify this request.
             */

            void onCancelled(Object closure);

            /**
             * This request resulted in an exception.  Some exceptions can be analyzed to take
             * corrective actions.  For example:  if HTTP Socket Write timeout is a user configurable
             * param, the application should examine the exception to see if it is a SocketWriteTimeout
             * exception.  If so, the user could be prompted to increase the timeout value.
             *
             * In other cases, the application should log these exceptions for review by
             * engineering.
             *
             * @param closure Application provided object used to identify this request.
             */

            void onException(Object closure, Exception ex);

            /**
             * The request failed
             *
             *
             * @param closure Application provided object used to identify this request.
             * @param status The reason for failure. These are specific to the request. The callback
             *               interface for the request will have these defined.  Also, status'es
             *               common for any request, from VR.Result could also be provided here.
             */

            void onFailure(Object closure, int status);

        }

        /**
         * A callback used to notify progress of a long running request
         */

        public interface ProgressCallback {

            /**
             * The latest progress update.
             *
             * @param closure Application provided object used to identify this request.
             * @param progressPercent Progress percentage between 0.0 to 100.0
             */

            void onProgress(Object closure, float progressPercent);
        }


        /**
         * Callbacks used to notify success of a request. Two types are defined here, one with
         * a result object, and the other with only the closure
         */
        public interface SuccessCallback<T> {
            /**
             * The request was successful
             *
             * @param closure Application provided object used to identify this request.
             */

            void onSuccess(Object closure);

        }

        public interface SuccessWithResultCallback<T> {

            /**
             * The request was successful and a result object was located or created
             *
             * @param result The object created or located for the request made
             * @param closure Application provided object used to identify this request.
             */

            void onSuccess(Object closure, T result);
        }

        /**
         * Callback for the getUserBySessionId request. The success callback has result
         * of type User
         */

        public interface GetUserBySessionId extends BaseCallback, SuccessWithResultCallback<User> {
        }


        /**
         * Intentionally undocumented
         */

        @Deprecated
        public interface GetUserBySessionToken extends BaseCallback, SuccessWithResultCallback<User> {
        }

        /**
         * Callback for the login request. Success callback has a result of type User.
         * Status codes are not documented and are self explanatory.
         */

        public interface Login extends BaseCallback, SuccessWithResultCallback<User> {

            int STATUS_MISSING_EMAIL_OR_PASSWORD = 1;
            int STATUS_ACCOUNT_LOCKED_EXCESSIVE_FAILED_ATTEMPTS = 2;
            int STATUS_ACCOUNT_WILL_BE_LOCKED_OUT = 3;
            int STATUS_ACCOUNT_NOT_YET_ACTIVATED = 4;
            int STATUS_UNKNOWN_USER = 5;
            int STATUS_LOGIN_FAILED = 6;

        }

    }

}

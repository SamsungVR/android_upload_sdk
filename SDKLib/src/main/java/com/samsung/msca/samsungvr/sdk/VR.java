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


public class VR {

    public static boolean newAPIClient(String endPoint, String apiKey, HttpPlugin.RequestFactory httpRequestFactory,
                                      APIClient.Result.Init callback, Handler handler, Object closure) {
        return APIClientImpl.newInstance(endPoint, apiKey, httpRequestFactory, callback, handler, closure);
    }

    private static final String TAG = Util.getLogTag(VR.class);

    private static APIClient sAPIClient;
    private static final Object sLock = new Object();

    /**
     * Initialize the SDK.
     */

    private static Result.Init sInitCallbackApp;
    private static APIClient.Result.Init sInitCallbackApi;

    public interface RegionInfo {
        public String getClientRegion();
        public boolean isUGCCountry();
    }

    /**
     * Initialize the SDK.
     *
     * @param endPoint The Server endpoint to communicate with, not null
     * @param apiKey The API key provided for your application by Samsung, not null
     * @param factory A HTTP transport plugin that handles all HTTP communication.  The SDK
     *                has not been buit with any HTTP transport library. Not null.
     * @param callback Provides async notification whether the init succeeded. Can be null.
     * @return true if init was never performed and is in progress, false otherwise.
     */

    public static boolean init(String endPoint, String apiKey,
        HttpPlugin.RequestFactory factory, Result.Init callback, Handler handler, Object closure) {

        synchronized (sLock) {
            if (null != sAPIClient || null != sInitCallbackApi) {
                return false;
            }
            sInitCallbackApp = callback;
            sInitCallbackApi = new APIClient.Result.Init() {

                @Override
                public void onSuccess(Object closure, APIClient result) {
                    synchronized (sLock) {
                        sAPIClient = result;
                        if (null != sInitCallbackApp) {
                            sInitCallbackApp.onSuccess(closure);
                        }
                        cleanupNoLock();
                    }
                }

                @Override
                public void onFailure(Object closure, int status) {
                    synchronized (sLock) {
                        if (null != sInitCallbackApp) {
                            sInitCallbackApp.onFailure(closure, status);
                        }
                        cleanupNoLock();
                    }

                }

                private void cleanupNoLock() {
                    sInitCallbackApp = null;
                    sInitCallbackApi = null;
                }
            };

            return APIClientImpl.newInstance(endPoint, apiKey, factory, sInitCallbackApi, handler, closure);
        }
    }

    /**
     * Destroy the SDK. Any calls made to SDK or its objects will fail after this.  Call this
     * to cleanup SDK resources when SDK services are no longer needed.
     *
     * @return true if destroy was not called after the last init and destroy is in progress,
     *         false otherwise.
     */

    private static Result.Destroy sDestroyCallbackApp;
    private static APIClient.Result.Destroy sDestroyCallbackApi;

    public static boolean destroyAsync(Result.Destroy callback, Handler handler, Object closure) {
        synchronized (sLock) {
            if (null == sAPIClient || null != sDestroyCallbackApi) {
                return false;
            }
            sDestroyCallbackApp = callback;
            sDestroyCallbackApi = new APIClient.Result.Destroy() {

                @Override
                public void onFailure(Object closure, int status) {
                    synchronized (sLock) {
                        if (null != sDestroyCallbackApp) {
                            sDestroyCallbackApp.onFailure(closure, status);
                        }
                        cleanupNoLock();
                    }
                }

                @Override
                public void onSuccess(Object closure) {
                    synchronized (sLock) {
                        sAPIClient = null;
                        if (null != sDestroyCallbackApp) {
                            sDestroyCallbackApp.onSuccess(closure);
                        }
                        cleanupNoLock();
                    }
                }

                private void cleanupNoLock() {
                    sDestroyCallbackApp = null;
                    sDestroyCallbackApi = null;
                }
            };

            return sAPIClient.destroyAsync(sDestroyCallbackApi, handler, closure);
        }
    }

    public static synchronized boolean destroy() {
        if (null != sAPIClient && null == sDestroyCallbackApi && sAPIClient.destroy()) {
            sAPIClient = null;
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

    public static boolean login(String email, String password,
            Result.Login callback, Handler handler, Object closure) {
        synchronized (sLock) {
            if (null == sAPIClient) {
                return false;
            }
            if (DEBUG) {
                Log.d(TAG, String.format("authenticate called. email=%s password=%s ", email, password));
            }
            return sAPIClient.login(email, password, callback, handler, closure);
        }
    }



    /**
     * Login as Samsung Account user
     *
     * @param samsung_sso_token The Samsung Account SSO token
     * @param auth_server Regional Samsung Account SSO auth server address,
     *                    optional, send null if no address
     * @param callback A callback to receive results async. May be null.
     * @param handler A handler on which callback should be called. If null, main handler is used.
     * @param closure An object that the application can use to uniquely identify this request.
     *                See callback documentation.
     * @return true if the SDK was initialized and the request could be scheduled, false otherwise.
     */

    public static boolean loginSamsungAccount(String samsung_sso_token, String api_server,
                                String auth_server, Result.Login callback, Handler handler, Object closure) {
        synchronized (sLock) {
            if (null == sAPIClient) {
                return false;
            }
            return sAPIClient.loginSamsungAccount(samsung_sso_token, api_server, auth_server, callback, handler, closure);
        }
    }



    /**
     * Note: This method is for SamsungVR internal use. DO NOT use this method.
     *
     * Given a session id, retrieve the corresponding user.  This is useful in situations where
     * there user is logged in on a browser and the User needs to be determined using the
     * Session Id available to the browser.
     *
     * @param sessionToken Session token saved from the user object (User.getSessionToken())
     * @param callback A callback to receive results async. May be null.
     * @param handler A handler on which callback should be called. If null, main handler is used.
     * @param closure An object that the application can use to uniquely identify this request.
     *                See callback documentation.
     * @return true if the SDK was initialized and the request could be scheduled, false otherwise.
     */

    public static boolean getUserBySessionToken(String sessionToken,
            Result.GetUserBySessionToken callback, Handler handler, Object closure) {
        synchronized (sLock) {
            if (null == sAPIClient) {
                return false;
            }
            if (DEBUG) {
                Log.d(TAG, String.format("getUserBySessionToken called. id=%s", sessionToken));
            }
            return sAPIClient.getUserBySessionToken(sessionToken, callback, handler, closure);
        }
    }

    public static boolean getRegionInfo(Result.GetRegionInfo callback, Handler handler, Object closure) {
        synchronized (sLock) {
            if (null == sAPIClient) {
                return false;
            }
            return sAPIClient.getRegionInfo(callback, handler, closure);
        }
    }


    public static boolean getRegionInfoEx(String sessionToken, String regionCode,
                                          Result.GetRegionInfo callback, Handler handler, Object closure) {
        synchronized (sLock) {
            if (null == sAPIClient) {
                return false;
            }
            return sAPIClient.getRegionInfoEx(sessionToken, regionCode, callback, handler, closure);
        }
    }


    /**
     * Given a user id, retrieve the corresponding user from the local cache.  This does not make
     * a request to the server. The user object is not persisted, therefore this call can only be
     * used while the application is still alive.
     *
     * @param userId Not null.
     * @return A non null user object if found, null otherwise
     */

    public static User getUserById(String userId) {
        synchronized (sLock) {
            if (null == sAPIClient) {
                return null;
            }
            return sAPIClient.getUserById(userId);
        }
    }



    /**
     * Returns the end point that this SDK is currently working with - provided by application
     * on the init call.
     *
     * @return A non null string if init was performed with a valid end point, null otherwise
     */

    public static String getEndPoint() {
        synchronized (sLock) {
            if (null == sAPIClient) {
                return null;
            }
            return sAPIClient.getEndPoint();
        }
    }

    /**
     * Returns the api key that this SDK is currently using - provided by application
     * on the init call.
     *
     * @return A non null string if init was performed with a valid end point, null otherwise
     */

    public static String getApiKey() {
        synchronized (sLock) {
            if (null == sAPIClient) {
                return null;
            }
            return sAPIClient.getApiKey();
        }
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

        public interface Init extends SuccessCallback, FailureCallback {
        }

        public interface Destroy extends SuccessCallback, FailureCallback {
        }

        /**
         * Callbacks used to notify failure of a request.
         */

        public interface FailureCallback {
            /**
             * The request failed
             *
             * @param closure Application provided object used to identify this request.
             * @param status The reason for failure. These are specific to the request. The callback
             *               interface for the request will have these defined.  Also, status'es
             *               common for any request, from VR.Result could also be provided here.
             */

            void onFailure(Object closure, int status);
        }

        /**
         * Base interface for other result callback groups.
         */

        public interface BaseCallback extends FailureCallback {

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

        }

        /**
         * A callback used to notify progress of a long running request
         */

        public interface ProgressCallback {


            /**
             * The latest progress update.  There are two callback methods. One in which
             * the progress percent could be determined. The other in which the progress
             * percentage could not be determined and the processed value is provided as is.
             *
             * @param closure Application provided object used to identify this request.
             * @param progressPercent Progress percentage between 0.0 to 100.0
             */

            void onProgress(Object closure, float progressPercent, long complete, long max);
            void onProgress(Object closure, long complete);

        }


        /**
         * Callbacks used to notify success of a request. Two types are defined here, one with
         * a result object, and the other with only the closure
         */
        public interface SuccessCallback {
            /**
             * The request was successful
             *
             * @param closure Application provided object used to identify this request.
             */

            void onSuccess(Object closure);

        }


        /**
         * This callback is used to provide status update for simple API calls.
         */

        public interface SimpleCallback extends VR.Result.BaseCallback, VR.Result.SuccessCallback {

            int INVALID_LIVE_EVENT_ID = 1;

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

        public interface GetUserBySessionToken extends BaseCallback, SuccessWithResultCallback<User> {
            int STATUS_TOKEN_INVALID_OR_EXPIRED = 1;
        }

        /**
         * Base callback for all login requests. Success callback has a result of type User.
         * Status codes are not documented and are self explanatory.
         */

        public interface Login extends BaseCallback, SuccessWithResultCallback<User> {
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

            int STATUS_ACCOUNT_LOCKED_EXCESSIVE_FAILED_ATTEMPTS = 2;
            int STATUS_ACCOUNT_NOT_YET_ACTIVATED = 4;

            int STATUS_MISSING_EMAIL_OR_PASSWORD = 1;
            int STATUS_LOGIN_FAILED = 6;

            int STATUS_LOGIN_TOKEN_EXPIRED = 7;
            int STATUS_LOGIN_VERIFY_FAILED = 9;
            int STATUS_LOGIN_PROFILE_FAILURE = 10;
            int STATUS_LOGIN_REGIONAL_SERVER_INVALID = 13;
            int STATUS_LOGIN_REGISTER_FAILURE = 14;
        }

        /**
         * This callback is used to provide status update for GetRegionInfo() .
         */

        public interface GetRegionInfo extends BaseCallback, SuccessWithResultCallback<RegionInfo> {

        }

    }

}

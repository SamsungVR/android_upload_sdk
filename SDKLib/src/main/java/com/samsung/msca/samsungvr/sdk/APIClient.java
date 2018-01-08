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

import org.json.JSONObject;

interface APIClient {

    final class Result {

        private Result() {
        }

        public interface Init extends VR.Result.SuccessWithResultCallback<APIClient>,
                VR.Result.FailureCallback {
        }

        public interface Destroy extends VR.Result.SuccessCallback, VR.Result.FailureCallback {
        }

    }

    boolean destroy();
    boolean destroyAsync(Result.Destroy callback, Handler handler, Object closure);
    String getEndPoint();
    String getApiKey();
    AsyncWorkQueue<ClientWorkItemType, ClientWorkItem<?>> getAsyncWorkQueue();
    AsyncWorkQueue<ClientWorkItemType, ClientWorkItem<?>> getAsyncUploadQueue();
    boolean login(String email, String password, VR.Result.Login callback, Handler handler,
                  Object closure);
    boolean loginSamsungAccount(String samsung_sso_token, String auth_server,
        VR.Result.Login callback, Handler handler, Object closure);

    User getUserById(String userId);
    boolean getUserBySessionId(String sessionId, VR.Result.GetUserBySessionId callback,
                               Handler handler, Object closure);

    boolean getUserBySessionToken(String userId, String sessionToken,
                                  VR.Result.GetUserBySessionToken callback,
                                  Handler handler, Object closure);
    boolean getRegionInfo(VR.Result.GetRegionInfo callback, Handler handler, Object closure);
    boolean getRegionInfoEx(String sessionToken, String regionCode,
                            VR.Result.GetRegionInfo callback, Handler handler, Object closure);



}

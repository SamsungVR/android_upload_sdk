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

import org.json.JSONObject;

import java.util.List;
import java.util.Locale;

class UnverifiedUserImpl extends Contained.BaseImpl<APIClientImpl> implements UnverifiedUser {

    private static final String TAG = Util.getLogTag(UnverifiedUserImpl.class);
    private static final boolean DEBUG = Util.DEBUG;

    static final String HEADER_SESSION_TOKEN = "X-SESSION-TOKEN";


    private enum Properties {
        USER_ID,
    }

    static final Contained.Type sType = new Contained.Type<APIClientImpl, UnverifiedUserImpl>(Properties.class) {

        @Override
        public void notifyCreate(Object callback, APIClientImpl apiClient, UnverifiedUserImpl user) {
        }

        @Override
        public void notifyUpdate(Object callback, APIClientImpl apiClient, UnverifiedUserImpl user) {
        }

        @Override
        public void notifyDelete(Object callback, APIClientImpl apiClient, UnverifiedUserImpl user) {
        }

        @Override
        public void notifyQueried(Object callback, APIClientImpl apiClient, UnverifiedUserImpl user) {
        }

        @Override
        public void notifyListQueried(Object callback, APIClientImpl apiClient, List<UnverifiedUserImpl> users) {
        }

        @Override
        Class<Enum> getPropertiesClass() {
            return super.getPropertiesClass();
        }

        @Override
        String getEnumName(String key) {
            return key.toUpperCase(Locale.US);
        }

        @Override
        public UnverifiedUserImpl newInstance(APIClientImpl container, JSONObject jsonObject) {
            return new UnverifiedUserImpl(container, jsonObject);
        }

        @Override
        public Object getContainedId(JSONObject jsonObject) {
            return jsonObject.optString("user_id", null);
        }

    };


    private UnverifiedUserImpl(APIClientImpl apiClient, JSONObject jsonObject) {
        super(sType, apiClient, jsonObject);
    }

    @Override
    public Object containedGetIdLocked() {
        return getLocked(Properties.USER_ID);
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
        return processQueryFromServiceLocked(jsonObject);
    }

    @Override
    public String getUserId() {
        return (String)getLocked(Properties.USER_ID);
    }


}
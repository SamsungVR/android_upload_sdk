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
import android.os.Looper;
import android.os.ParcelFileDescriptor;

class Util {

    static final boolean DEBUG = BuildConfig.DEBUG;

    static String getLogTag(Object obj) {
        String result = "VRSDK.";
        if (obj instanceof Class<?>) {
            Class<?> cls = (Class<?>)obj;
            result += cls.getSimpleName();
        } else {
            if (null == obj) {
                result += "NULL";
            } else {
                Class<?> cls = obj.getClass();
                result += cls.getSimpleName() + " [" + getHashCode(obj) + "]";
            }
        }
        return result;
    }

    static String getHashCode(Object obj) {
        return "0x" + Integer.toHexString(System.identityHashCode(obj));
    }

    static Enum enumFromString(Class enumClass, String str) {
        if (null == str) {
            return null;
        }
        Enum[] items = ((Class<Enum>)enumClass).getEnumConstants();
        if (null == items) {
            return null;
        }
        for (int i = items.length - 1; i >= 0; i -= 1) {
            Enum item = items[i];
            if (str.equalsIgnoreCase(item.name())) {
                return item;
            }
        }
        return null;
    }

    static boolean checkEquals(Object a, Object b) {
        return (a == b) ||
                ((null != a) && a.equals(b)) ||
                ((null != b) && b.equals(a));
    }

    static abstract class CallbackNotifier extends ResultCallbackHolder implements Runnable {

        @Override
        public void run() {
            Object callback = getCallbackNoLock();
            if (null == callback) {
                return;
            }
            notify(callback, getClosureNoLock());
        }

        boolean post() {
            Handler handler = getHandlerNoLock();
            if (null != handler) {
                return handler.post(this);
            }
            return true;
        }

        @Override
        public CallbackNotifier setNoLock(Object callback, Handler handler, Object closure) {
            super.setNoLock(callback, handler, closure);
            return this;
        }

        @Override
        public CallbackNotifier setNoLock(ResultCallbackHolder other) {
            super.setNoLock(other);
            return this;
        }

        @Override
        public CallbackNotifier clearNoLock() {
            super.clearNoLock();
            return this;
        }

        abstract void notify(Object callback, Object closure);

    }

    static class SuccessCallbackNotifier extends CallbackNotifier {

        @Override
        void notify(Object callback, Object closure) {
            ((VR.Result.SuccessCallback)callback).onSuccess(closure);
        }
    }

    static class SuccessWithResultCallbackNotifier<Y> extends CallbackNotifier {

        private final Y mRef;

        SuccessWithResultCallbackNotifier(Y ref) {
            mRef = ref;
        }

        @Override
        void notify(Object callback, Object closure) {
            ((VR.Result.SuccessWithResultCallback<Y>)callback).onSuccess(closure, mRef);
        }
    }

    static class FailureCallbackNotifier extends CallbackNotifier {

        private final int mStatus;

        public FailureCallbackNotifier(int status) {
            mStatus = status;
        }

        @Override
        void notify(Object callback, Object closure) {
            ((VR.Result.BaseCallback)callback).onFailure(closure, mStatus);
        }
    }

    static class CancelledCallbackNotifier extends CallbackNotifier {

        @Override
        void notify(Object callback, Object closure) {
            ((VR.Result.BaseCallback)callback).onCancelled(closure);
        }

    }

    static class ExceptionCallbackNotifier extends CallbackNotifier {

        private final Exception mException;

        public ExceptionCallbackNotifier(Exception exception) {
            mException = exception;
        }

        @Override
        void notify(Object callback, Object closure) {
            ((VR.Result.BaseCallback)callback).onException(closure, mException);
        }
    }

}

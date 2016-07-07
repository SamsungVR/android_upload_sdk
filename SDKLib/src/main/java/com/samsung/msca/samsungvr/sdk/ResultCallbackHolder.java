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
import java.lang.ref.WeakReference;

class ResultCallbackHolder {

    private WeakReference<Object> mCallbackWeakRef;
    private WeakReference<Handler> mHandlerWeakRef;
    private Object mClosure;

    private static final String TAG = Util.getLogTag(ResultCallbackHolder.class);
    private static final Handler sMainHandler = new Handler(Looper.getMainLooper());

    public ResultCallbackHolder setNoLock(Object callback, Handler handler, Object closure) {
        if (null != callback) {
            mCallbackWeakRef = new WeakReference<>(callback);
        } else {
            mCallbackWeakRef = null;
        }
        if (null == handler) {
            handler = sMainHandler;
        }
        mHandlerWeakRef = new WeakReference<>(handler);
        mClosure = closure;
        return this;
    }

    public ResultCallbackHolder setNoLock(ResultCallbackHolder other) {
        return setNoLock(other.getCallbackNoLock(), other.getHandlerNoLock(), other.getClosureNoLock());
    }

    public ResultCallbackHolder clearNoLock() {
        setNoLock(null, null, null);
        return this;
    }

    public Object getClosureNoLock() {
        return mClosure;
    }

    public Handler getHandlerNoLock() {
        if (null != mHandlerWeakRef) {
            return mHandlerWeakRef.get();
        }
        return null;
    }

    public Object getCallbackNoLock() {
        if (null != mCallbackWeakRef) {
            return mCallbackWeakRef.get();
        }
        return null;
    }
}

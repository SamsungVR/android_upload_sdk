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

import java.lang.ref.WeakReference;

class ResultCallbackHolder<T> {

    private WeakReference<T> mCallbackWeakRef;
    private WeakReference<Handler> mHandlerWeakRef;
    private Object mClosure;

    private static final String TAG = Util.getLogTag(ResultCallbackHolder.class);

    ResultCallbackHolder(T callback, Handler handler, Object closure) {
        setNoLock(callback, handler, closure);
    }

    ResultCallbackHolder() {
        this(null, null, null);
    }

    public void setNoLock(T callback, Handler handler, Object closure) {
        if (null != callback) {
            mCallbackWeakRef = new WeakReference<>(callback);
        } else {
            mCallbackWeakRef = null;
        }
        if (null != handler) {
            mHandlerWeakRef = new WeakReference<>(handler);
        } else {
            mHandlerWeakRef = null;
        }
        mClosure = closure;
    }

    public void clearNoLock() {
        setNoLock(null, null, null);
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

    public T getCallbackNoLock() {
        if (null != mCallbackWeakRef) {
            return mCallbackWeakRef.get();
        }
        return null;
    }

    public void copyFromNoLock(ResultCallbackHolder<T> other) {
        setNoLock(other.getCallbackNoLock(), other.getHandlerNoLock(), other.getClosureNoLock());
    }

}

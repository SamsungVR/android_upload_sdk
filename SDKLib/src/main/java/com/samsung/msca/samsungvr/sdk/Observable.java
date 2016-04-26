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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

abstract class Observable {

    interface Spec<T> {

        /**
         * Add an observer that will be notified of events happening on this Observable.  The observer
         * will be notified on the main thread.
         *
         * @param observer The observer that will be notified of events. Non null.
         */

        void addObserver(T observer);

        /**
         * Add an observer that will be notified of events happening on this Observable.  The observer
         * will be notified on the main thread.
         *
         * @param observer The observer that will be notified of events. Non null.
         * @param handler The handler to which the event should be dispatched. If NULL, a handler
         *                of the Main thread is used.
         */

        void addObserver(T observer, Handler handler);

        /**
         * Remove an observer
         *
         * @param observer The observer that was added earlier
         */

        void removeObserver(T observer);
    }

    interface IterationObserver<X> {
        boolean onIterate(Block<X> block, Object ... closure);
    }

    static class Block<X> {
        final X mCallback;
        Handler mHandler;

        private Block(X callback) {
            mCallback = callback;
        }
    }

    static class BaseImpl<T> implements Spec<T> {

        private final Map<T, Block<T>> mObservers = new WeakHashMap<>();
        private final Handler mMainHandler;

        BaseImpl() {
            mMainHandler = new Handler(Looper.getMainLooper());
        }

        public void addObserver(T observer) {
            addObserver(observer, null);
        }

        public void addObserver(T observer, Handler handler) {
            handler = (null == handler) ? mMainHandler : handler;

            synchronized (mObservers) {
                Block found = mObservers.get(observer);
                if (null == found) {
                    found = newBlock(observer);
                    if (null == found) {
                        return;
                    }
                    mObservers.put(observer, found);
                }
                found.mHandler = handler;
            }
        }

        public void removeObserver(T observer) {
            synchronized (mObservers) {
                mObservers.remove(observer);
            }
        }

        protected Block<T> newBlock(T callback) {
            return new Block<T>(callback);
        }

        void clear() {
            synchronized (mObservers) {
                mObservers.clear();
            }
        }

        boolean hasObserver(T observer) {
            boolean result;
            synchronized (mObservers) {
                result = mObservers.containsKey(observer);
            }
            return result;
        }

        void iterate(IterationObserver<T> iterationObserver, Object ... closure) {
            synchronized (mObservers) {
                List<T> observers = new ArrayList<>(mObservers.keySet());
                for (T observer : observers) {
                    Block<T> block = mObservers.get(observer);
                    if (null == block) {
                        mObservers.remove(observer);
                        continue;
                    }
                    if (!iterationObserver.onIterate(block, closure)) {
                        break;
                    }
                }
            }
        }

    }

}

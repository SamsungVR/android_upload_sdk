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

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class AsyncWorkQueue<T extends AsyncWorkItemType, W extends AsyncWorkItem<T>> {

    private final boolean mShouldRecycleWorkItems;
    private final List<W> mWorkItems = new ArrayList<>();
    private final byte[] mIOBuf;

    private W mActiveWorkItem;

    private final String TAG = Util.getLogTag(this);
    private static final boolean DEBUG = Util.DEBUG;

    interface AsyncWorkItemFactory<T extends AsyncWorkItemType, W extends AsyncWorkItem<T>> {
        W newWorkItem(T type);
    }

    interface Observer {
        void onQuit(AsyncWorkQueue<?, ?> queue);
    }

    private final AsyncWorkItemFactory<T, W> mFactory;
    private final Map<T, List<W>> mRecycledWorkItems = new HashMap<>();

    private final Thread mThread = new Thread() {
        @Override
        public void run() {
            if (DEBUG) {
                Log.d(TAG, "Running worker thread");
            }
            while (!isInterrupted()) {
                synchronized (mWorkItems) {
                    if (mWorkItems.isEmpty()) {
                        try {
                            mWorkItems.wait();
                        } catch (InterruptedException ex) {
                            break;
                        }
                    }
                    if (mWorkItems.isEmpty()) {
                        continue;
                    }
                    mActiveWorkItem = mWorkItems.remove(0);
                }
                mActiveWorkItem.run();
                if (mShouldRecycleWorkItems) {
                    mActiveWorkItem.recycle();
                    synchronized (mRecycledWorkItems) {
                        T type = mActiveWorkItem.getType();
                        List<W> recycledList = mRecycledWorkItems.get(type);
                        if (null != recycledList) {
                            if (DEBUG) {
                                Log.d(TAG, "Recycled work item: " + Util.getHashCode(mActiveWorkItem) + " type: " + type);
                            }
                            recycledList.add(mActiveWorkItem);
                        }
                    }
                }
                synchronized (mWorkItems) {
                    mActiveWorkItem = null;
                }
            }
            if (DEBUG) {
                Log.d(TAG, "Quitting worker thread");
            }
            if (mShouldRecycleWorkItems) {
                synchronized (mRecycledWorkItems) {
                    mRecycledWorkItems.clear();
                }
            }
            if (null != mObserver) {
                mObserver.onQuit(AsyncWorkQueue.this);
            }
        }
    };

    private static final boolean RECYCLE_WORK_ITEMS = true;
    private static final int IO_BUF_SIZE = 4096;

    AsyncWorkQueue(AsyncWorkItemFactory<T, W> factory, int ioBufSize, Observer observer) {
        this(factory, RECYCLE_WORK_ITEMS, ioBufSize, observer);
    }

    private final Observer mObserver;

    AsyncWorkQueue(AsyncWorkItemFactory<T, W> factory, boolean shouldRecycle, int ioBufSize,
                   Observer observer) {
        mFactory = factory;
        mObserver = observer;
        mShouldRecycleWorkItems = shouldRecycle;
        mIOBuf = new byte[ioBufSize];
        mThread.start();
    }

    boolean enqueue(W workItem) {
        if (!mThread.isAlive() || mThread.isInterrupted()) {
            return false;
        }
        synchronized (mWorkItems) {
            mWorkItems.add(workItem);
            mWorkItems.notify();
        }
        return true;
    }

    void clear() {
        synchronized (mWorkItems) {
            clearNoLock();
        }
    }

    void clearNoLock() {
        mWorkItems.clear();
        if (null != mActiveWorkItem) {
            mActiveWorkItem.cancel();
        }
    }

    private static final long JOIN_TIMEOUT = 0;

    void quitAsync() {
        clear();
        mThread.interrupt();
    }

    void quit() {
        clear();
        mThread.interrupt();
        try {
            mThread.join(JOIN_TIMEOUT);
        } catch (InterruptedException ex) {
        }
    }

    public <X extends W> X obtainWorkItem(T type) {
        W result = null;
        if (mShouldRecycleWorkItems) {
            synchronized (mRecycledWorkItems) {
                List<W> recycledList = mRecycledWorkItems.get(type);
                if (null == recycledList) {
                    recycledList = new ArrayList<>();
                    mRecycledWorkItems.put(type, recycledList);
                }
                if (recycledList.size() > 0) {
                    result = recycledList.remove(0);
                    if (DEBUG) {
                        Log.d(TAG, "Got work item from recycle bin: " + Util.getHashCode(result) + " type: " + type);
                    }
                }
            }
        }
        if (null == result) {
            result = mFactory.newWorkItem(type);
            if (DEBUG) {
                Log.d(TAG, "Got work item from factory: " + Util.getHashCode(result) + " type: " + type);
            }
        }
        if (null != result) {
            result.renew(mIOBuf);
        }
        return (X)result;
    }

    public interface IterationObserver<T extends AsyncWorkItemType, W extends AsyncWorkItem<T>> {
        boolean onIterate(W workItem, Object ... args);
    }

    public void iterateWorkItems(IterationObserver observer, Object ... args) {
        synchronized (mWorkItems) {
            if (null != mActiveWorkItem) {
                if (!observer.onIterate(mActiveWorkItem, args)) {
                    return;
                }
            }
            for (AsyncWorkItem workItem : mWorkItems) {
                if (!observer.onIterate(workItem, args)) {
                    break;
                }
            }
        }
    }

}

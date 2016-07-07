package com.samsung.msca.samsungvr.sdk;

import android.util.Log;

class StateManager<R> extends Observable.BaseImpl<StateManager.Observer<R>> {

    private Enum<?> mCurrentState;
    private final R mObj;

    interface Observer<R> {
        void onStateChanged(R obj, Enum<?> oldState, Enum<?> newState);
    }

    StateManager(R obj, Enum<?> initialState) {
        mCurrentState = initialState;
        mObj = obj;
    }

    private final Object mLock = new Object();

    public Enum<?> getState() {
        synchronized (mLock) {
            return mCurrentState;
        }
    }

    public boolean isInState(Enum<?> state) {
        synchronized (mLock) {
            return (state == mCurrentState);
        }
    }

    private static final String TAG = Util.getLogTag(UserImpl.class);
    private static final boolean DEBUG = Util.DEBUG;


    public boolean setState(Enum<?> newState) {
        Enum<?> oldState;

        synchronized (mLock) {
            if (newState == mCurrentState) {
                return false;
            }
            if (DEBUG) {
                Log.d(TAG, "Setting state to " + newState + " from " + mCurrentState + " on " + mObj);
            }
            oldState = mCurrentState;
            mCurrentState = newState;
        }
        super.iterate(new Observable.IterationObserver<StateManager.Observer<R>>() {
            @Override
            public boolean onIterate(Observable.Block<StateManager.Observer<R>> block, Object... closure) {
                R obj = (R)closure[0];
                Enum<?> os = (Enum<?>)closure[1];
                Enum<?> ns = (Enum<?>)closure[2];
                block.mCallback.onStateChanged(obj, os, ns);
                return true;
            }
        }, mObj, oldState, newState);
        return true;
    }

}

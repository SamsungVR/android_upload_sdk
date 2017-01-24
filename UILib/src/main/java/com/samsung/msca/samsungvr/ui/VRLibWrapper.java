package com.samsung.msca.samsungvr.ui;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.samsung.msca.samsungvr.sdk.HttpPlugin;
import com.samsung.msca.samsungvr.sdk.VR;

/**
 * A wrapper around the VRLib. Initializes the VRLib and maintains its state
 */

class VRLibWrapper {

    static VRLibWrapper sInstance;

    public interface Callback {
        void onVRLibReady();
    }

    static VRLibWrapper getInstance(Context context, String serverEndpoint, String serverAppKey,
        Handler handler, Callback callback) {
        if (null == sInstance) {
            sInstance = new VRLibWrapper(context.getApplicationContext(),
                serverEndpoint, serverAppKey, VRLibHttpPlugin.getInstance(), handler, callback);
        }
        return sInstance;
    }

    // VRState. A null value indicates initialization has not been started
    enum VRState {
        INITIALIZING,
        INITIALIZED
    }

    private final Context mAppContext;
    private VRState mVRState;
    private final String mServerEndpoint, mServerAppKey;
    private final HttpPlugin.RequestFactory mHttpPluginReqFactory;
    private final Callback mCallback;
    private final Handler mHandler;

    private VRLibWrapper(Context appContext, String serverEndpoint, String serverAppKey,
        HttpPlugin.RequestFactory httpPluginReqFactory, Handler handler, Callback callback) {
        mAppContext = appContext;
        mServerEndpoint = serverEndpoint;
        mServerAppKey = serverAppKey;
        mHttpPluginReqFactory = httpPluginReqFactory;
        mHandler = (null == handler) ? new Handler(Looper.getMainLooper()) : handler;
        mCallback = callback;
    }

    private void notifySDKLibReady() {
        if (null != mCallback && null != mHandler) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mCallback.onVRLibReady();
                }
            });

        }
    }
    /**
     * Initializes the VRLib if necessary.
     * @return true if the library is ready to use; false if not. If this method returns false,
     *         a VRLibReady event will be fired when the VRLib becomes ready.
     */
    public boolean initializeVRLib() {
        boolean rc = mVRState == VRState.INITIALIZED;
        if (!rc && (mVRState != VRState.INITIALIZING)) {
            if (VR.init(mServerEndpoint, mServerAppKey,
                    mHttpPluginReqFactory, mVRInitListener, null, null)) {
                mVRState = VRState.INITIALIZING;
            } else {
                // Somehow already initialized?
                mVRState = VRState.INITIALIZED;
                notifySDKLibReady();
                rc = true;
            }
        }
        return rc;
    }

    private final VR.Result.Init mVRInitListener = new VR.Result.Init() {

        @Override
        public void onSuccess(Object closure) {
            mVRState = VRState.INITIALIZED;
            notifySDKLibReady();
        }

        @Override
        public void onFailure(Object closure, int status) {

        }
    };
}

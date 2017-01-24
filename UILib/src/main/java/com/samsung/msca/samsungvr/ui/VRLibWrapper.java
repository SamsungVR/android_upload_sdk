package com.samsung.msca.samsungvr.ui;

import android.content.Context;

import com.samsung.msca.samsungvr.sdk.HttpPlugin;
import com.samsung.msca.samsungvr.sdk.VR;

/**
 * A wrapper around the VRLib. Initializes the VRLib and maintains its state
 */
class VRLibWrapper {

    public final Bus.VRLibReadyEvent mVRLibReadyEvent = new Bus.VRLibReadyEvent();

    // VRState. A null value indicates initialization has not been started
    enum VRState {
        INITIALIZING,
        INITIALIZED
    }

    private final Context mAppContext;
    private final Bus     mBus;
    private final String  mServerEndPoint, mServerAppKey;
    private final HttpPlugin.RequestFactory mHttpPlugin;

    private VRState mVRState;

    VRLibWrapper(Context context, String serverEndPoint, String serverAppKey,
                 HttpPlugin.RequestFactory httpPlugin) {
        mAppContext = context.getApplicationContext();
        mServerAppKey = serverAppKey;
        mServerEndPoint = serverEndPoint;
        mHttpPlugin = httpPlugin;
        mBus = Bus.getEventBus();
    }

    /**
     * Initializes the VRLib if necessary.
     * @return true if the library is ready to use; false if not. If this method returns false,
     *         a VRLibReady event will be fired when the VRLib becomes ready.
     */
    public boolean initializeVRLib() {
        boolean rc = mVRState == VRState.INITIALIZED;
        if (!rc && (mVRState != VRState.INITIALIZING)) {
            if (VR.init(mServerEndPoint, mServerAppKey, mHttpPlugin,
                    mVRInitListener, null, null)) {
                mVRState = VRState.INITIALIZING;
            } else {
                // Somehow already initialized?
                mVRState = VRState.INITIALIZED;
                mBus.post(mVRLibReadyEvent);
                rc = true;
            }
        }
        return rc;
    }

    private final VR.Result.Init mVRInitListener = new VR.Result.Init() {

        @Override
        public void onSuccess(Object closure) {
            mVRState = VRState.INITIALIZED;
            mBus.post(mVRLibReadyEvent);
        }

        @Override
        public void onFailure(Object closure, int status) {

        }
    };
}

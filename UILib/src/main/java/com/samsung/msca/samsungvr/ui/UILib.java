package com.samsung.msca.samsungvr.ui;

import android.content.Context;

public class UILib {

    private static UILib sUILib;

    public static UILib initInstance(Context context,
        String serverEndPoint, String serverApiKey, String ssoAppId, String ssoAppSecret) {
        if (null == sUILib) {
            sUILib = new UILib(context, serverEndPoint, serverApiKey, ssoAppId, ssoAppSecret);
        }
        return sUILib;
    }

    public static UILib getInstance() {
        if (null == sUILib) {
            throw new RuntimeException();
        }
        return sUILib;
    }


    private final Context mContext;
    private final SyncSignInState mSyncSignInState;
    private final VRLibHttpPlugin mHttpPlugin;
    private final VRLibWrapper mVRLibWrapper;
    private final SALibWrapper mSALibWrapper;

    private UILib(Context context, String serverEndPoint, String serverApiKey, String ssoAppId,
        String ssoAppSecret) {

        mContext = context;
        mSyncSignInState = new SyncSignInState(mContext);
        mHttpPlugin = new VRLibHttpPlugin();
        mVRLibWrapper = new VRLibWrapper(mContext, serverEndPoint, serverApiKey, mHttpPlugin);
        mVRLibWrapper.initializeVRLib();
        mSALibWrapper = new SALibWrapper(mContext, ssoAppId, ssoAppSecret);
    }

    SALibWrapper getSALibWrapper() {
        return mSALibWrapper;
    }

    SyncSignInState getSyncSignInState() {
        return mSyncSignInState;
    }

    String getExternalServerBaseURL() {
        return null;
    }

    public VRLibHttpPlugin getHttpPlugin() {
        return mHttpPlugin;
    }
}

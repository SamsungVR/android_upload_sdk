package com.samsung.msca.samsungvr.ui;

import android.content.Context;
import android.content.Intent;

import com.samsung.msca.samsungvr.sdk.VR;

public class UILib {

    private static UILib sUILib;

    public static UILib initInstance(Context context,
        String serverEndPoint, String serverApiKey, String ssoAppId, String ssoAppSecret) {
        if (null != sUILib) {
            sUILib.destroy();
        }
        sUILib = new UILib(context, serverEndPoint, serverApiKey, ssoAppId, ssoAppSecret);
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
        mHttpPlugin = new VRLibHttpPlugin();

        VR.init(serverEndPoint, serverApiKey, mHttpPlugin, null, null, null);
        mSyncSignInState = new SyncSignInState(mContext);

        mVRLibWrapper = new VRLibWrapper(mContext, serverEndPoint, serverApiKey, mHttpPlugin);
        mVRLibWrapper.initializeVRLib();
        mSALibWrapper = new SALibWrapper(mContext, ssoAppId, ssoAppSecret);
    }

    public void destroy() {
        mSALibWrapper.close();
        if (sUILib == this) {
            VR.destroy();
            sUILib = null;
        }
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

    VRLibWrapper getVRLibWrapper() {
        return mVRLibWrapper;
    }

    public VRLibHttpPlugin getHttpPlugin() {
        return mHttpPlugin;
    }

    public void login() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClass(mContext, SignInActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY);
        mContext.startActivity(intent);
    }
}

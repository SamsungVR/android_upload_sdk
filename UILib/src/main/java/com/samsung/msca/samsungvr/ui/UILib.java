package com.samsung.msca.samsungvr.ui;

import android.content.Context;
import android.content.Intent;

import com.samsung.dallas.salib.SamsungSSO;
import com.samsung.msca.samsungvr.sdk.HttpPlugin;
import com.samsung.msca.samsungvr.sdk.VR;
import com.samsung.msca.samsungvr.sdk.APIClient;

public class UILib {

    private static UILib sUILib;

    public static boolean initInstance(Context context,
        String serverEndPoint, String serverApiKey, String ssoAppId, String ssoAppSecret) throws RuntimeException {
        if (null != sUILib) {
            sUILib.destroyInternal();
        }
        sUILib = new UILib(context, serverEndPoint, serverApiKey, ssoAppId, ssoAppSecret);
        return true;
    }

    public static boolean login() {
        if (null == sUILib) {
            return false;
        }
        return sUILib.loginInternal();
    }

    public static void destroy() {
        if (null != sUILib) {
            sUILib.destroyInternal();
        }
    }

    public static HttpPlugin.RequestFactory getHttpPluginRequestFactory() {
        if (null == sUILib) {
            return null;
        }
        return sUILib.getHttpPluginInternal();
    }

    static UILib getInstance() {
        return sUILib;
    }

    private final Context mContext;
    private final SyncSignInState mSyncSignInState;
    private final VRLibHttpPlugin mHttpPlugin;
    private final Bus mBus;
    private final SALibWrapper mSALibWrapper;

    private APIClient mAPIClient;

    private UILib(Context context, String serverEndPoint, String serverApiKey, String ssoAppId,
        String ssoAppSecret) throws RuntimeException {

        mContext = context;
        mBus = Bus.getEventBus();
        mHttpPlugin = new VRLibHttpPlugin();
        VR.newAPIClient(serverEndPoint, serverApiKey, mHttpPlugin, new APIClient.Result.Init() {
            @Override
            public void onSuccess(Object o, APIClient apiClient) {
                mAPIClient = apiClient;
                mBus.post(new Bus.VRLibReadyEvent(UILib.this));
            }

            @Override
            public void onFailure(Object o, int i) {
                throw new RuntimeException("Failed to create client");
            }
        }, null, null);

        mSyncSignInState = new SyncSignInState(mContext, this);
        mSALibWrapper = new SALibWrapper(mContext, ssoAppId, ssoAppSecret, this);
    }

    Bus getEventBus() {
        return mBus;
    }

    void destroyInternal() {
        mSyncSignInState.destroy();
        mSALibWrapper.close();
        if (null != mAPIClient) {
            mAPIClient.destroyAsync(new APIClient.Result.Destroy() {
                @Override
                public void onSuccess(Object o) {
                }

                @Override
                public void onFailure(Object o, int i) {
                }
            }, null, null);
            mAPIClient = null;
        }
        if (sUILib == this) {
            sUILib = null;
        }
    }

    APIClient getAPIClientInternal() {
        return mAPIClient;
    }

    SALibWrapper getSALibWrapperInternal() {
        return mSALibWrapper;
    }

    SyncSignInState getSyncSignInStateInternal() {
        return mSyncSignInState;
    }

    String getExternalServerBaseURLInternal() {
        return null;
    }

    VRLibHttpPlugin getHttpPluginInternal() {
        return mHttpPlugin;
    }

    boolean loginInternal() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClass(mContext, SignInActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY);
        try {
            mContext.startActivity(intent);
        } catch (Exception ex) {
            return false;
        }
        return true;
    }
}

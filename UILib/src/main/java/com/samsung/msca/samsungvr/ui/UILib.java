package com.samsung.msca.samsungvr.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.samsung.msca.samsungvr.sdk.HttpPlugin;
import com.samsung.msca.samsungvr.sdk.User;
import com.samsung.msca.samsungvr.sdk.VR;

public class UILib {

    public interface Callback {
        void onLoggedIn(User user, Object closure);
    }

    private static UILib sUILib;

    public static boolean initInstance(Context context,
           String serverEndPoint, String serverApiKey, String ssoAppId, String ssoAppSecret,
           Callback callback, Object closure) throws RuntimeException {
        if (DEBUG) {
            Log.d(TAG, "initInstance " + serverEndPoint + " " + serverApiKey + " " + callback);
        }
        if (null == sUILib || !sUILib.matches(serverEndPoint, serverApiKey, ssoAppId, ssoAppSecret)) {
            if (null != sUILib) {
                sUILib.destroyInternal();
            }
            sUILib = new UILib(context, serverEndPoint, serverApiKey, ssoAppId, ssoAppSecret,
                    callback, closure);
        }
        return true;
    }


    public static boolean login() {
        if (null == sUILib) {
            return false;
        }
        return sUILib.loginInternal();
    }

    public static void destroy() {
        if (DEBUG) {
            Log.d(TAG, "destroy " + sUILib);
        }
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

    static boolean setCallback(UILib.Callback callback) {
        if (null == sUILib) {
            return false;
        }
        return sUILib.setCallbackInternal(callback);
    }

    static String getHashCode(Object obj) {
        return "0x" + Integer.toHexString(System.identityHashCode(obj));
    }

    static boolean DEBUG = true;

    static String getLogTag(Object obj) {
        String result = "UILib.";
        if (obj instanceof Class<?>) {
            Class<?> cls = (Class<?>)obj;
            result += cls.getSimpleName();
        } else {
            if (null == obj) {
                result += "NULL";
            } else {
                Class<?> cls = obj.getClass();
                result += cls.getSimpleName() + " [" + getHashCode(obj) + "]";
            }
        }
        return result;
    }

    private final Context mContext;
    private final SyncSignInState mSyncSignInState;
    private final VRLibHttpPlugin mHttpPlugin;
    private final Bus mBus;
    private final SALibWrapper mSALibWrapper;
    private final Object mClosure;
    private final Handler mHandler;
    private UILib.Callback mCallback;

    private Bus.Callback mBusCallback = new Bus.Callback() {
        @Override
        public void onLoggedInEvent(final Bus.LoggedInEvent event) {
            if (null != mCallback && sUILib == UILib.this) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (sUILib == UILib.this && null != mCallback) {
                            mCallback.onLoggedIn(event.mVrLibUser, mClosure);
                        }
                    }
                });
            }
        }
    };

    private final String mServerApiKey, mServerEndPoint, mSSOoAppId, mSSOAppSecret;


    boolean matches(String serverEndPoint, String serverApiKey, String ssoAppId,
                    String ssoAppSecret) {
        return mSSOAppSecret.equals(ssoAppSecret) && mSSOoAppId.equals(ssoAppId) &&
                mServerEndPoint.equals(serverEndPoint) && mServerApiKey.equals(serverApiKey);
    }

    private UILib(Context context, String serverEndPoint, String serverApiKey, String ssoAppId,
                  String ssoAppSecret, UILib.Callback callback, Object closure) throws RuntimeException {
        if (DEBUG) {
            Log.d(TAG, "constructor this: " + this);
        }
        mServerApiKey = serverApiKey;
        mServerEndPoint = serverEndPoint;
        mSSOoAppId = ssoAppId;
        mSSOAppSecret = ssoAppSecret;

        mContext = context;
        mBus = Bus.getEventBus();
        mCallback = callback;
        mHandler = new Handler(Looper.getMainLooper());
        mClosure = closure;
        mHttpPlugin = new VRLibHttpPlugin();
        mBus.addObserver(mBusCallback);

        VR.init(serverEndPoint, serverApiKey, mHttpPlugin, new VR.Result.Init() {

            @Override
            public void onFailure(Object o, int i) {
            }

            @Override
            public void onSuccess(Object o) {
                mBus.post(new Bus.VRLibReadyEvent(UILib.this));
            }

        }, null, null);

        mSyncSignInState = new SyncSignInState(mContext, this);
        mSALibWrapper = new SALibWrapper(mContext, ssoAppId, ssoAppSecret, this);


    }

    boolean setCallbackInternal(UILib.Callback callback) {
        mCallback = callback;
        return true;
    }


    Bus getEventBus() {
        return mBus;
    }

    private static final String TAG = UILib.getLogTag(UILib.class);

    void destroyInternal() {
        if (DEBUG) {
            Log.d(TAG, "destroyInternal this: " + this);
        }
        mBus.removeObserver(mBusCallback);
        mSyncSignInState.destroy();
        mSALibWrapper.close();
        VR.destroy();
        if (sUILib == this) {
            sUILib = null;
        }
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

package com.samsung.msca.samsungvr.ui;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.samsung.msca.samsungvr.sdk.HttpPlugin;
import com.samsung.msca.samsungvr.sdk.User;
import com.samsung.msca.samsungvr.sdk.VR;

public class UILib {

    public interface Callback {
        void onLibInitSuccess(Object closure);
        void onLibInitFailed(Object closure);
        void onLoggedIn(User user, Object closure);
        void onFailure(Object closure);
    }

    private static UILib sUILib;

    static final String getPrefsName(Context context) {
        return "ui_prefs";
    };

    public static boolean init(Context context,
           String serverEndPoint, String serverApiKey, String ssoAppId, String ssoAppSecret,
           Callback callback, Object closure) throws RuntimeException {
        if (null != sUILib) {
            if (sUILib.matches(serverEndPoint, serverApiKey, ssoAppId, ssoAppSecret)) {
                sUILib.setCallbackInternal(callback, true);
                return true;
            }
            if (!sUILib.destroyInternal()) {
                return false;
            }
            sUILib = null;
        }
        sUILib = new UILib(context, serverEndPoint, serverApiKey, ssoAppId, ssoAppSecret, callback, closure);
        if (DEBUG) {
            Log.d(TAG, "initInstance " + serverEndPoint + " " + serverApiKey + " " + callback + " uilib " + sUILib);
        }
        return true;
    }

    public static boolean login() {
        if (null == sUILib) {
            return false;
        }

        return sUILib.loginInternal();
    }

    public static boolean logout() {
        if (null == sUILib) {
            return false;
        }
        return sUILib.logoutInternal();
    }

    public static boolean destroy() {
        if (DEBUG) {
            Log.d(TAG, "destroy " + sUILib);
        }
        if (null == sUILib) {
            return false;
        }
        if (!sUILib.destroyInternal()) {
            return false;
        }
        sUILib = null;
        return true;
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
        return sUILib.setCallbackInternal(callback, false);
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
    private User mUser;

    private Bus.Callback mBusCallback = new Bus.Callback() {
        @Override
        public void onLoggedInEvent(final Bus.LoggedInEvent event) {
            if (!isActive()) {
                return;
            }
            mUser = event.mVrLibUser;
            saveSessionCreds(mUser);
            if (null != mCallback) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isActive() && null != mCallback) {
                            mCallback.onLoggedIn(mUser, mClosure);
                        }
                    }
                });
            }
        }

        @Override
        public void onSignInActivityDestroyed(Bus.SignInActivityDestroyed event) {
            if (!isActive()) {
                return;
            }
            if (DEBUG) {
                Log.d(TAG, "onSignInActivityDestroyed user: " + mUser + " cb: " + mCallback);
            }
            if (null != mCallback && null == mUser) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isActive() && null != mCallback && null == mUser) {
                            mCallback.onFailure(mClosure);
                        }
                    }
                });
            }
        }
    };

    private final String mServerApiKey, mServerEndPoint, mSSOoAppId, mSSOAppSecret;
    private final SharedPreferences mSharedPrefs;
    private boolean mVRLibInitialzed = false;

    private void notifyLibInitInternal() {
        if (DEBUG) {
            Log.d(TAG, "notifyLibInternal preCalling: " + mCallback + " active: "
                    + isActive() + " closure: " + mClosure);
        }
        if (isActive() && null != mCallback) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (DEBUG) {
                        Log.d(TAG, "notifyLibInternal calling: " + mCallback + " active: "
                                + isActive() + " closure: " + mClosure);
                    }
                    if (isActive() && null != mCallback) {
                        mCallback.onLibInitSuccess(mClosure);
                    }
                }
            });
        }
    }

    boolean matches(String serverEndPoint, String serverApiKey, String ssoAppId,
                    String ssoAppSecret) {
        return mSSOAppSecret.equals(ssoAppSecret) && mSSOoAppId.equals(ssoAppId) &&
                mServerEndPoint.equals(serverEndPoint) && mServerApiKey.equals(serverApiKey);
    }

    boolean isActive() {
        return sUILib == this;
    }

    private UILib(Context context, String serverEndPoint, String serverApiKey, String ssoAppId,
                  String ssoAppSecret, UILib.Callback callback, Object closure) throws RuntimeException {
        if (DEBUG) {
            Log.d(TAG, "constructor this: " + this);
        }
        mSharedPrefs = context.getSharedPreferences(getPrefsName(context), Context.MODE_PRIVATE);
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
                if (isActive() && null != mCallback) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (isActive() && null != mCallback) {
                                mCallback.onLibInitFailed(mClosure);
                            }
                        }
                    });
                }
            }

            @Override
            public void onSuccess(Object o) {
                mVRLibInitialzed = true;
                mBus.post(new Bus.VRLibReadyEvent(UILib.this));
                notifyLibInitInternal();
            }

        }, null, null);

        mSyncSignInState = new SyncSignInState(mContext, this);
        mSALibWrapper = new SALibWrapper(mContext, ssoAppId, ssoAppSecret, this);

    }

    boolean setCallbackInternal(UILib.Callback callback, boolean notify) {
        if (DEBUG) {
            Log.d(TAG, "setCallbackInternal old: " + mCallback + " new: " + callback + " notify: " + notify);
        }
        mCallback = callback;
        if (notify) {
            notifyLibInitInternal();
        }
        return true;
    }


    Bus getEventBus() {
        return mBus;
    }

    private static final String TAG = UILib.getLogTag(UILib.class);

    boolean destroyInternal() {
        if (DEBUG) {
            Log.d(TAG, "destroyInternal this: " + this);
        }
        if (!isActive() || !mVRLibInitialzed) {
            return false;
        }
        if (!VR.destroy()) {
            return false;
        }
        mVRLibInitialzed = false;
        mUser = null;
        mBus.removeObserver(mBusCallback);
        mBus.post(new Bus.KillActivitiesEvent());
        mSyncSignInState.destroy();
        mSALibWrapper.close();
        return true;
    }

    SALibWrapper getSALibWrapperInternal() {
        return mSALibWrapper;
    }

    SyncSignInState getSyncSignInStateInternal() {
        return mSyncSignInState;
    }

    String getExternalServerBaseURLInternal() {
        Resources res = mContext.getResources();
        Uri uri = new Uri.Builder()
                .scheme(res.getString(R.string.scheme_https))
                .authority(res.getString(R.string.host_public))
                .build();
        return uri.toString();
    }

    VRLibHttpPlugin getHttpPluginInternal() {
        return mHttpPlugin;
    }

    // Callback used when signing in with a session token
    private VR.Result.GetUserBySessionToken mTokenSignInCallback = new VR.Result.GetUserBySessionToken() {

        @Override
        public void onException(Object o, Exception e) {
            if (DEBUG) {
                Log.e(TAG, "GetUserBySessionToken.onException", e);
            }
            onFailure(o, -1);
        }

        @Override
        public void onCancelled(Object o) {
        }

        @Override
        public void onSuccess(Object o, User user) {
            mBus.post(new Bus.LoggedInEvent(user));
        }

        @Override
        public void onFailure(Object o, int i) {
            if (isActive()) {
                loginViaActivity();
            }
        }
    };

    static final String PREFS_USER_ID = "userId";
    static final String PREFS_SESSION_TOKEN = "sessionToken";

    boolean loginInternal() {
        if (DEBUG) {
            Log.d(TAG, "loginInternal this: " + this);
        }
        mUser = null;
        if (!mVRLibInitialzed) {
            return false;
        }

        String userId = mSharedPrefs.getString(PREFS_USER_ID, null);
        String sessionToken = mSharedPrefs.getString(PREFS_SESSION_TOKEN, null);
        if (DEBUG) {
            Log.d(TAG, "found persisted  userId=" + userId + " sessionToken=" + sessionToken);
        }
        if (null != userId && null != sessionToken &&
                VR.getUserBySessionToken(userId, sessionToken, mTokenSignInCallback, null, null)) {
            return true;
        }
        return loginViaActivity();
    }

    boolean logoutInternal() {
        if (DEBUG) {
            Log.d(TAG, "logoutInternal this: " + this);
        }
        if (saveSessionCreds(null, null)) {
            mUser = null;
            mBus.post(new Bus.LoggedOutEvent());
            return true;
        }
        return false;
    }

    private static void saveStrToPrefsInternal(SharedPreferences.Editor editor, String key, String value) {
        if (DEBUG) {
            Log.d(TAG, "saveStrToPrefs key: " + key + " value: " + value);
        }
        if (null == value) {
            editor.remove(key);
        } else {
            editor.putString(key, value);
        }
    }

    boolean saveSessionCreds(String userId, String sessionToken) {
        SharedPreferences.Editor editor = mSharedPrefs.edit();
        saveStrToPrefsInternal(editor, PREFS_USER_ID, userId);
        saveStrToPrefsInternal(editor, PREFS_SESSION_TOKEN, sessionToken);
        return editor.commit();
    }

    boolean saveSessionCreds(User user) {
        if (null != user) {
            return saveSessionCreds(user.getUserId(), user.getSessionToken());
        }
        return false;
    }

    private boolean loginViaActivity() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClass(mContext, SignInActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            mContext.startActivity(intent);
        } catch (Exception ex) {
            if (DEBUG) {
                Log.d(TAG, "loginInternal start activity exception: " + this, ex);
            }
            return false;
        }
        return true;
    }
}

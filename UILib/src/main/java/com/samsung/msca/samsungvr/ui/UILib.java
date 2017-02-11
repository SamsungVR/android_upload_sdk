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

    static final String getPrefsName(Context context) {
        return "ui_prefs";
    };

    private static UILib sUILib;
    private static final Handler sMainHandler = new Handler(Looper.getMainLooper());
    private static final Object sLock = new Object();

    public static void init(final Context context,
            final String serverEndPoint, final String serverApiKey, final String ssoAppId,
            final String ssoAppSecret, final Callback callback, final Handler handler,
            final Object closure) {
        sMainHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (sLock) {
                    if (null == sUILib) {
                        sUILib = new UILib(context);
                    }
                }
                sUILib.initInternal(serverEndPoint, serverApiKey, ssoAppId, ssoAppSecret, callback,
                        handler, closure);
            }
        });
    }

    public static void init(final Context context,
                            final String serverEndPoint, final String serverApiKey, final String ssoAppId,
                            final String ssoAppSecret, final Callback callback, final Object closure) {
        init(context, serverEndPoint, serverApiKey, ssoAppId, ssoAppSecret, callback, null, closure);
    }


    public static boolean login() {
        synchronized (sLock) {
            if (null == sUILib) {
                return false;
            }
        }
        return sMainHandler.post(new Runnable() {
            @Override
            public void run() {
                sUILib.loginInternal();
            }
        });
    }

    public static boolean logout() {
        synchronized (sLock) {
            if (null == sUILib) {
                return false;
            }
        }
        return sMainHandler.post(new Runnable() {
            @Override
            public void run() {
                sUILib.logoutInternal();
            }
        });
    }

    public static boolean destroy() {
        synchronized (sLock) {
            if (null == sUILib) {
                return false;
            }
        }
        return sMainHandler.post(new Runnable() {
            @Override
            public void run() {
                sUILib.destroyInternal();
            }
        });
    }

    public static HttpPlugin.RequestFactory getHttpPluginRequestFactory() {
        if (null == sUILib) {
            return null;
        }
        return sUILib.getHttpPluginInternal();
    }

    static UILib getInstance() {
        synchronized (sLock) {
            return sUILib;
        }
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

    private abstract class CallbackNotifier implements Runnable {

        protected final int mMyId;

        protected CallbackNotifier(int id) {
            mMyId = id;
        }

        @Override
        public void run() {

            int activeId;
            Object closure;
            Callback callback;

            synchronized (mLock) {
                activeId = mId;
                closure = mClosure;
                callback = mCallback;
            }
            if (mMyId != activeId || null == callback) {
                return;
            }
            onRun(callback, closure);
        }

        protected abstract void onRun(Callback callback, Object closure);
    }

    private class LoginSuccessNotifier extends CallbackNotifier {

        private final User mMyUser;

        private LoginSuccessNotifier(int id, User user) {
            super(id);
            mMyUser = user;
        }

        @Override
        protected void onRun(Callback callback, Object closure) {
            if (null != mMyUser) {
                mCallback.onLoggedIn(mMyUser, closure);
            }

        }
    }

    private class LoginFailureNotifier extends CallbackNotifier {

        private LoginFailureNotifier(int id) {
            super(id);
        }

        @Override
        protected void onRun(Callback callback, Object closure) {
            User user = mSyncSignInState.getUser();

            if (null == user) {
                mCallback.onFailure(closure);
            }
        }
    }

    private Bus.Callback mBusCallback = new Bus.Callback() {
        @Override
        public void onLoggedInEvent(final Bus.LoggedInEvent event) {
            User user = event.mVrLibUser;
            if (null != user) {
                saveSessionCreds(user);
                mHandler.post(new LoginSuccessNotifier(mId, user));
            }
        }

        @Override
        public void onSignInActivityDestroyed(Bus.SignInActivityDestroyed event) {
            User user = mSyncSignInState.getUser();

            if (DEBUG) {
                Log.d(TAG, "onSignInActivityDestroyed user: " + user + " cb: " + mCallback);
            }
            if (null == user) {
                mHandler.post(new LoginFailureNotifier(mId));
            }
        }
    };

    private final SharedPreferences mSharedPrefs;
    private boolean mVRLibInitialzed = false;

<<<<<<< HEAD
    private final Context mContext;
    private final VRLibHttpPlugin mHttpPlugin;
    private final Bus mBus;
    private final SyncSignInState mSyncSignInState;
    private User mUser;

    private UILib(Context context) throws RuntimeException {
        if (DEBUG) {
            Log.d(TAG, "constructor this: " + this);
        }
        mSharedPrefs = context.getSharedPreferences(getPrefsName(context), Context.MODE_PRIVATE);
        mContext = context;
        mBus = Bus.getEventBus();
        mHttpPlugin = new VRLibHttpPlugin();
        mSyncSignInState = new SyncSignInState(mContext, this);
    }

    private int mId = -1;

    private class InitStatusNotifier extends CallbackNotifier {

        private final boolean mMySuccess;

        private InitStatusNotifier(int id, boolean success) {
            super(id);
            mMySuccess = success;
        }

        @Override
        protected void onRun(Callback callback, Object closure) {
            if (mMySuccess) {
                mCallback.onLibInitSuccess(closure);
            } else {
                mCallback.onLibInitFailed(closure);
            }
        }
    }

    private final Object mLock = new Object();

    private SALibWrapper mSALibWrapper;
    private String mSSOAppSecret, mSSOoAppId, mServerApiKey, mServerEndPoint;
    private Callback mCallback;
    private Handler mHandler;
    private Object mClosure;


    private void initInternal(String serverEndPoint, String serverApiKey, String ssoAppId,
                             String ssoAppSecret, UILib.Callback callback, Handler handler, Object closure) {
        synchronized (sLock) {
            ++mId;
            mCallback = callback;
            mHandler = null == handler ? sMainHandler : handler;
            mClosure = closure;
        }

        mBus.addObserver(mBusCallback);

        boolean matches = (mSSOAppSecret == ssoAppSecret || null != mSSOAppSecret && mSSOAppSecret.equals(ssoAppSecret)) &&
                        ((mSSOoAppId == ssoAppId) || null != mSSOoAppId && mSSOoAppId.equals(ssoAppId)) &&
                        ((mServerEndPoint == serverEndPoint) || null != mServerEndPoint && mServerEndPoint.equals(serverEndPoint)) &&
                ((mServerApiKey == serverApiKey) || null != mServerApiKey && mServerApiKey.equals(serverApiKey));

        if (matches) {
            mHandler.post(new InitStatusNotifier(mId, true));
            return;
        }

        if (!destroyInternal()) {
            mHandler.post(new InitStatusNotifier(mId, false));
            return;
        }

        mSSOAppSecret = ssoAppSecret;
        mSSOoAppId = ssoAppId;
        mServerApiKey = serverApiKey;
        mServerEndPoint = serverEndPoint;
        mSyncSignInState.init();
        mSALibWrapper = new SALibWrapper(mContext, mSSOoAppId, mSSOAppSecret, this);


        VR.init(serverEndPoint, serverApiKey, mHttpPlugin, new VR.Result.Init() {

            @Override
            public void onFailure(Object o, int i) {
                mHandler.post(new InitStatusNotifier(mId, false));
            }

            @Override
            public void onSuccess(Object o) {
                mVRLibInitialzed = true;
                mBus.post(new Bus.VRLibReadyEvent(UILib.this));
                mHandler.post(new InitStatusNotifier(mId, true));
            }

        }, sMainHandler, null);

    }

    Bus getEventBus() {
        return mBus;
    }

    private static final String TAG = UILib.getLogTag(UILib.class);

    boolean destroyInternal() {
        if (DEBUG) {
            Log.d(TAG, "destroyInternal this: " + this);
        }
        if (!mVRLibInitialzed) {
            return true;
        }
        if (!VR.destroy()) {
            return false;
        }
        mVRLibInitialzed = false;
        mBus.removeObserver(mBusCallback);
        mBus.post(new Bus.KillActivitiesEvent());
        mSyncSignInState.destroy();
        mSALibWrapper.close();
        mSALibWrapper = null;
        return true;
    }

    SALibWrapper getSALibWrapperInternal() {
        if (DEBUG) {
            Log.d(TAG, "return SALibWrapper: " + mSALibWrapper);
        }
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
            loginViaActivity();
        }
    };

    static final String PREFS_USER_ID = "userId";
    static final String PREFS_SESSION_TOKEN = "sessionToken";

    boolean loginInternal() {
        if (DEBUG) {
            Log.d(TAG, "loginInternal this: " + this);
        }
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

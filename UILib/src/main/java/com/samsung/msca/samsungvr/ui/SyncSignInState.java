package com.samsung.msca.samsungvr.ui;

import android.content.Context;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.samsung.dallas.salib.SamsungSSO;
import com.samsung.msca.samsungvr.sdk.User;
import com.samsung.msca.samsungvr.sdk.VR;

import org.json.JSONObject;

import java.net.HttpCookie;
import java.util.List;

/**
 * A singleton used to sign-in to the VR Server and synchronize the sign-in credentials between
 * the 2D and 3D applications.
 */
class SyncSignInState {

    private static final String TAG = UILib.getLogTag(SyncSignInState.class);
    private static final boolean DEBUG = UILib.DEBUG;

    enum SignInState {
        WAITING_VRLIB,          // Waiting for VRLib to be ready before starting the sign-in sequence
        WAITING_SSO_TOKEN,      // Waiting for an SSO token before starting the sign-in sequence
        LOGIN_VIA_CREDS,        // Login via credentials request is outstanding
        REGISTER,               // Register request is outstanding
        LOGIN_VIA_TOKEN         // Logging in via Token
    }

    // This (and corresponding code) should go away once we convert to using the VRLIB login.
    private static final String AUTH_TYPE_SAMSUNG = "SamsungSSO";

    private final Context   mAppContext;
    private final Bus       mBus;
    private SignInCreds     mCredentials;
    private String          mSignInToken;
    private User            mUser;
    private SignInState     mSignInState;     // Current sign-in state


    com.samsung.msca.samsungvr.ui.Bus.Callback mBusCallback = new com.samsung.msca.samsungvr.ui.Bus.Callback() {

        @Override
        public void onSamsungSsoStatusEvent(Bus.SamsungSsoStatusEvent event) {
            if (mSignInState == SignInState.WAITING_SSO_TOKEN)  {
                SamsungSSO.Status status = event.mStatus;
                SamsungSSO.UserInfo info = mUILib.getSALibWrapperInternal().getUserInfo();
                if ((status == SamsungSSO.Status.USER_INFO_UPDATED)
                        && (info != null)
                        && info.mUserId.equals(mCredentials.mSamsungSsoInfo.mUserId)) {
                    mCredentials = new SignInCreds(info);
                    signInViaCredentials();
                } else {
                    mSignInState = null;
                    mCredentials = null;
                    mBus.post(new Bus.LoginErrorEvent(mAppContext.getString(R.string.signin_failure_generic)));
                }
            }
        }

        @Override
        public void onVRLibReadyEvent(Bus.VRLibReadyEvent event) {
            if (mSignInState == SignInState.WAITING_VRLIB) {
                if (mCredentials != null) {
                    signInViaCredentials();
                }
            }
        }

    };

    private final UILib mUILib;

    SyncSignInState(Context context, UILib uiLib) {
        mAppContext = context.getApplicationContext();
        mUILib = uiLib;
        mBus = mUILib.getEventBus();
        mBus.addObserver(mBusCallback);
    }

    void destroy() {
        mBus.removeObserver(mBusCallback);
    }

    /**
     * Get the currently signed-in user
     * @return the currently signed-inn user; null if no user is currently signed in
     */
    public User getUser() {
        return mUser;
    }

    /**
     * Get the Id of the currently signed-in user
     * @return the user's Id; null if no user is currently signed in
     */
    public String getUserId() {
        return mUser != null ? mUser.getUserId() : null;
    }

    /**
     * Get the URL for this user's image
     * @return the url to this user's image; null if no user is currently signed in or if the user
     *         has no image.
     */
    public String getUserImageUrl() {
        return mUser != null ? mUser.getProfilePicUrl() : null;
    }

    /**
     * Get the name of the currently signed-in user
     * @return the user's name; null if no user is currently signed in
     */
    public String getUserName() {
        return mUser != null ? mUser.getName() : null;
    }

    /**
     * Get the email address of the currently signed-in user
     * @return the user's email; null if no user is currently signed in
     */
    public String getUserEmail() {
        return mUser != null ? mUser.getEmail() : null;
    }

    /**
     * Determine if a user is currently signed in
     * @return true if a user is currently signed in; false if not
     */
    public boolean isSignedIn() {
        return mUser != null;
    }

    /**
     * SignIn with an email/password and propagate that change to the VR application.
     *
     * @param email the email address used to login
     * @param password the password used to login
     * @return true if an attempt is made to sign in; false if not (already signed in or invalid
     *         email/password).
     */
    public boolean signIn(String email, String password) {
        boolean rc = !(TextUtils.isEmpty(email) || TextUtils.isEmpty(password));
        if (rc) {
            signOut();
            mCredentials = new SignInCreds(email, password);
            mSignInState = SignInState.WAITING_VRLIB;
            signInViaCredentials();
        }
        return rc;
    }

    /**
     * SignIn with an email/password and propagate that change to the VR application.
     *
     * @param info the SamsungSSO.UserInfo to use for signing-in
     *
     * @return true if an attempt is made to sign in; false if not (info is null)
     */
    public boolean signIn(SamsungSSO.UserInfo info) {
        boolean rc = info != null;
        if (rc) {
            signOut();
            mCredentials = new SignInCreds(info);
            mSignInState = SignInState.WAITING_VRLIB;
            signInViaCredentials();
        }
        return rc;
    }

    /**
     * Sign out the current User and propagate that change to the VR application
     */
    public void signOut() {
        signOutInternal();
    }

    private void signInViaCredentials() {
        if (mCredentials != null) {
            mSignInState = SignInState.LOGIN_VIA_CREDS;
            if (DEBUG) {
                Log.i(TAG, "VRLib signin using credentials");
            }
            VR.login(mCredentials.mEmail, mCredentials.mPassword,
                    mCredentialsSignInCallback, null, mCredentials);
        }
    }

    private boolean signOutInternal() {
        boolean wasSignedIn = isSignedIn();
        mUser = null;
        mCredentials = null;
        mSignInToken = null;
        mSignInState = null;
        mBus.post(new Bus.LoggedOutEvent());
        return wasSignedIn;
    }

    // Callback used when signing in to VRLib with email/password
    private final VR.Result.Login mCredentialsSignInCallback = new VR.Result.Login() {

        @Override
        public void onException(Object o, Exception e) {
            if (DEBUG) {
                Log.e(TAG, "Login.onException", e);
            }
            onFailure(o, -1);
        }

        @Override
        public void onCancelled(Object o) {
            if (o == mCredentials) {
                mCredentials = null;
                mSignInState = null;
                if (DEBUG) {
                    Log.d(TAG, "Login.onCancelled");
                }
            }
        }

        @Override
        public void onSuccess(Object o, User user) {
            if (o == mCredentials) {

                mCredentials = null;
                mSignInState = null;
                mUser = user;
                if (DEBUG) {
                    Log.i(TAG, "Login.onSuccess USER: " + mUser);
                }
                mBus.post(new Bus.LoggedInEvent(user));
            }
        }

        @Override
        public void onFailure(Object o, int i) {
            if (o == mCredentials) {
                mCredentials = null;
                mSignInState = null;
                String reason = mAppContext.getResources().getString(R.string.signin_failure_code, i);
                if (DEBUG) {
                    Log.i(TAG, "Login.onError: " + reason);
                }
                mBus.post(new Bus.LoginErrorEvent(reason));
            }
        }
    };

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
            if ((o == mSignInToken) && (mSignInState == SignInState.LOGIN_VIA_TOKEN)) {
                mSignInState = null;
                if (DEBUG) {
                    Log.d(TAG, "GetUserBySessionToken.onCancelled");
                }
            }
        }

        @Override
        public void onSuccess(Object o, User user) {
            if ((o == mSignInToken) && (mSignInState == SignInState.LOGIN_VIA_TOKEN)) {
                mSignInState = null;
                mUser = user;
                if (DEBUG) {
                    Log.i(TAG, "GetUserBySessionToken.onSuccess");
                }
                mBus.post(new Bus.LoggedInEvent(user));
            }
        }

        @Override
        public void onFailure(Object o, int i) {
            if ((o == mSignInToken) && (mSignInState == SignInState.LOGIN_VIA_TOKEN)) {
                mSignInState = null;
                String reason = mAppContext.getResources().getString(R.string.signin_failure_code, i);
                if (DEBUG) {
                    Log.i(TAG, "GetUserBySessionToken.onError: " + reason);
                }
                //Toast360.makeText(mAppContext, reason, Toast.LENGTH_LONG).show();
                mBus.post(new Bus.LoginErrorEvent(reason));
            }
        }
    };

    /**
     * Simple class used to hold the last set of credentials sent to the server. We only remember
     * the last set of credentials sent and clear them as soon as a corresponding result is received.
     * This allows us to have multiple sign-ins in flight but to only act on the last set sent.
     */
    private static class SignInCreds {
        final String mEmail;
        final String mPassword;
        final SamsungSSO.UserInfo mSamsungSsoInfo;

        SignInCreds(String email, String password) {
            mEmail = email;
            mPassword = password;
            mSamsungSsoInfo = null;
        }

        SignInCreds(SamsungSSO.UserInfo info) {
            mEmail = null;
            mPassword = null;
            mSamsungSsoInfo = info;
        }
    }
}

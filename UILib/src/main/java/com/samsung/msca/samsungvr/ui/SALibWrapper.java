package com.samsung.msca.samsungvr.ui;

import android.content.Context;
import android.content.Intent;

import com.samsung.dallas.salib.SamsungSSO;

import java.io.Closeable;

/**
 * Singleton that interfaces to the Samsung Account Service.
 * <br>
 * This class is not thread-safe; all public methods are expected to be called from the main
 * thread and all callbacks also occur on the main thread.
 * <br>
 * This class creates and holds several resources. When the owner is done using the class, it
 * should always call the <code>close</code> method to release those resources. Once <code>close</code>
 * is called, <code>init</code> must be called in order to use the singleton again.
 *
 */
class SALibWrapper implements Closeable {

    private SamsungSSO mSaLib;
    private Bus mBus;

    SALibWrapper(Context context, String appId, String appSecret) {
        mBus = Bus.getEventBus();
        mSaLib = new SamsungSSO(context, appId, appSecret, mCallback, BuildConfig.DEBUG);
        mSaLib.init();
    }

    /**
     * Close the SamsungAccount module. It is important that this method be called once the
     * application is done using it since several system resources must be freed. After being
     * closed, no public APIs should be called (they will typically fail if they are called).
     */
    @Override
    public void close() {
        mSaLib.close();
    }

    /**
     * Get the current Samsung Account status. Status can also be presented to a single listener
     * using the <code>setListener</code> API.
     *
     * @return the current Samsung Account status
     */
    public SamsungSSO.Status getStatus() {
        return mSaLib.getStatus();
    }

    /**
     * Get the user information for the current SSO user (if any). If an SSO user is defined but
     * has not been authenticated, then all fields except mLoginId are null.
     *
     * @return the UserInfo corresponding to the current user; may be null if there
     *         is no SSO user defined and authenticated.
     */
    public SamsungSSO.UserInfo getUserInfo() {
        return mSaLib.getUserInfo();
    }

    /**
     * Obtain user info from the Samsung Account Manager. This invalidates any current user
     * information defined.
     */
    public void loadUserInfo(String expiredToken) {
        mSaLib.requestUserInfo(expiredToken);
    }

    /**
     * Builds an intent that can be used to launch the app that will create a Samsung SSO
     * account.
     *
     * @return Intent that can be used to launch the create account dialog; can be null if
     *         the SSO service is not available or has not been initialized.
     */
    public Intent buildAddAccountIntent() {
        return mSaLib.buildAddAccountIntent();
    }

    /**
     * Builds an intent that can be used to launch the app that will create a Samsung SSO
     * account via a popup window.
     *
     * @param theme the theme to use for the popup window; valid values are null (use default),
     *              "dark", "light" and "invisible"
     * @return Intent that can be used to launch the Samsung SSO login application; can be null
     *         if the SSO service is not available or has not been initialized.
     */
    public Intent buildAddAccountPopupIntent(String theme) {
        return mSaLib.buildAddAccountPopupIntent(theme);
    }

    /**
     * Builds an intent that can be used to launch the app that will prompt for the Samsung SSO
     * user password, etc. This does not create a new account, it updates an existing account and/or
     * reconfirms a password.
     *
     * @param theme the theme to use for the popup window; valid values are null (use default),
     *              "dark", "light" and "invisible"
     * @return Intent that can be used to launch the Samsung SSO login application; can be null
     *         if the SSO service is not available or has not been initialized.
     */
    public Intent buildRequestTokenIntent(String theme) {
        return mSaLib.buildRequestTokenIntent(null, theme, null);
    }

    private final SamsungSSO.Callback mCallback = new SamsungSSO.Callback() {
        @Override
        public void onSsoStatus(SamsungSSO.Status status) {
            mBus.post(new Bus.SamsungSsoStatusEvent(status));
        }
    };
}

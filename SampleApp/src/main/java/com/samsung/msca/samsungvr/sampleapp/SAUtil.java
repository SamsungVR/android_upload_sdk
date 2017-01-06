package com.samsung.msca.samsungvr.sampleapp;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by tim on 1/4/17.
 */

public class SAUtil {

    // Samsung SSO extra data that can be requested along with the AccessToken
    public static final String  EXTRA_API_SERVER_URL    = "api_server_url";
    public static final String  EXTRA_AUTH_SERVER_URL   = "auth_server_url";
    public static final String  EXTRA_BIRTHDAY          = "birthday";
    public static final String  EXTRA_LOGIN_ID          = "login_id";
    public static final String  EXTRA_USER_ID           = "user_id";
    public static final String  EXTRA_ACCESS_TOKEN      = "access_token";

    // Result codes received via the resultCode in onActivityResult
    public static final int RESULT_OK       = -1;
    public static final int RESULT_CANCELED = 0;
    public static final int RESULT_FAILED   = 1;

    // Fields in an error response
    public static final String  RESPONSE_ERROR_CODE     = "error_code";
    public static final String  RESPONSE_ERROR_MSG      = "error_message";

    // Possible error codes (in the event of a failure)
    public static final String ERROR_NO_SSO_ACCT    = "SAC_0102";   // No SSO account defined
    public static final String ERROR_UPDATED_RQD    = "SAC_0203";   // SSO service update required
    public static final String ERROR_VERIFY         = "SAC_0204";   // Additional terms/verifications required
    public static final String ERROR_SIGNATURE      = "SAC_0205";   // Invalid application signature
    public static final String ERROR_NETWORK        = "SAC_0301";   // Network in unavailable
    public static final String ERROR_SSL            = "SAC_0302";   // SSL connection error
    public static final String ERROR_INTERNAL       = "SAC_0401";   // Some type of internal error
    public static final String ERROR_TOKEN_EXPIRED  = "SAC_0402";   // Token is expired; password must be re-entered

    private static final int SSO_ERROR_VERIFY    = 204; // Additional terms/verifications required
    private static final int SSO_ERROR_SIGNATURE = 205; // Invalid application signature
    private static final int SSO_ERROR_NETWORK   = 301; // Network in unavailable
    private static final int SSO_ERROR_SSL       = 302; // SSL connection error
    private static final int SSO_ERROR_INTERNAL  = 401; // Some type of internal error
    private static final int SSO_ERROR_EXPIRED   = 402; // Expired token


    private static final String INTENT_SSO_ADD_ACCOUNT  = "com.osp.app.signin.action.ADD_SAMSUNG_ACCOUNT";
    private static final String INTENT_SSO_RQ_TOKEN     = "com.msc.action.samsungaccount.REQUEST_ACCESSTOKEN";

    // Samsung SSO request/response keys
    private static final String SSO_KEY_CLIENT_ID       = "client_id";
    private static final String SSO_KEY_CLIENT_SECRET   = "client_secret";
    private static final String SSO_KEY_ADDITIONAL_DATA = "additional";
    private static final String SSO_KEY_EXPIRED_TOKEN   = "expired_access_token";
    private static final String SSO_KEY_OSP_VERSION     = "OSP_VER";
    private static final String SSO_KEY_MODE            = "MODE";
    private static final String SSO_KEY_MY_PACKAGE      = "mypackage";
    private static final String SSO_KEY_PROG_THEME      = "progress_theme";


    // Sasmung SSO constant field values
    private static final String SSO_OSP_VERSION         = "OSP_02";
    private static final String SSO_OSP_ADD_ACCOUNT     = "ADD_ACCOUNT";

    // Do not allow instantiation
    private SAUtil() {};


    /**
     * Builds an intent that can be used to launch the app that will create a Samsung SSO
     * account.
     *
     * @param context a context; cannot be null
     * @param ssoAppId the SSO application ID; cannot be null
     * @param ssoAppSecret the SSO application secret; cannot be null
     * @return Intent that can be used to launch the create account dialog
     */
    public static Intent buildAddAccountIntent(Context context, String ssoAppId, String ssoAppSecret) {
        String pkgName = context.getPackageName();

        Intent intent = new Intent(INTENT_SSO_ADD_ACCOUNT);
        intent.putExtra(SSO_KEY_CLIENT_ID, ssoAppId);
        intent.putExtra(SSO_KEY_CLIENT_SECRET, ssoAppSecret);
        intent.putExtra(SSO_KEY_MY_PACKAGE, pkgName);
        intent.putExtra(SSO_KEY_OSP_VERSION, SSO_OSP_VERSION);
        intent.putExtra(SSO_KEY_MODE, SSO_OSP_ADD_ACCOUNT);

        Log.d("SAUtil", "Add account pkg: '" + pkgName + "' appId: '" + ssoAppId + "' appSecret: '" + ssoAppSecret + "'");
        return intent;
    }

    /**
     * Builds an intent that can be used to launch the app that will prompt for the Samsung SSO
     * token. It also prompts for a password if the user session has expored. This does not
     * create a new account, it updates an existing account and/or reconfirms a password.
     *
     * @param ssoAppId the SSO application ID; cannot be null
     * @param ssoAppSecret the SSO application secret; cannot be null
     * @param extraFields the extra fields you want returned in addition to the token. Can be null.
     *                    Refer to EXTRA_ fields defined in this module
     * @param theme the theme to use for the popup window; valid values are null (use default),
     *              "dark", "light" and "invisible"
     * @return Intent that can be used to launch the Samsung SSO login application.
     */
    public static Intent buildRequestTokenIntent(String ssoAppId, String ssoAppSecret,
                                          String[] extraFields, String theme, String expiredToken) {

        Intent intent = new Intent(INTENT_SSO_RQ_TOKEN);
        intent.putExtra(SSO_KEY_CLIENT_ID, ssoAppId);
        intent.putExtra(SSO_KEY_CLIENT_SECRET, ssoAppSecret);
        if ((extraFields != null) && (extraFields.length > 0)) {
            intent.putExtra(SSO_KEY_ADDITIONAL_DATA, extraFields);
        }
        if (theme != null) {
            intent.putExtra(SSO_KEY_PROG_THEME, theme);
        }
        if (expiredToken != null) {
            intent.putExtra(SSO_KEY_EXPIRED_TOKEN, expiredToken);
        }
        return intent;
    }


}

/*
 * Copyright (c) 2016 Samsung Electronics America
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.samsung.msca.samsungvr.sampleapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import android.content.res.Resources;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.samsung.msca.samsungvr.sdk.User;
import com.samsung.msca.samsungvr.sdk.VR;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class LoginFragment extends BaseFragment {

    static final String TAG = Util.getLogTag(LoginFragment.class);
    private static final boolean DEBUG = Util.DEBUG;

    private View mLoginGroup, mGrpSSOLogin;
    private TextView mEmail, mEndPoint, mSSOLoginId, mSSOAuthURL, mSSOAuthToken;
    private EditText mPassword;
    private TextView mStatus = null;

    private final List<View> mViewStack = new ArrayList<>();

    private final CheckBox.OnCheckedChangeListener mSetShowPassword = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                mPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            } else {
                mPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View result = inflater.inflate(R.layout.fragment_login, null, false);

        mEmail = (TextView)result.findViewById(R.id.email);
        mEndPoint = (TextView)result.findViewById(R.id.end_point);
        mPassword = (EditText)result.findViewById(R.id.password);
        mLoginGroup = result.findViewById(R.id.grp_login);
        mGrpSSOLogin = result.findViewById(R.id.grp_sso_login);
        mSSOLoginId = (TextView)result.findViewById(R.id.sso_login_id);
        mSSOAuthToken = (TextView)result.findViewById(R.id.sso_auth_token);
        mSSOAuthURL = (TextView)result.findViewById(R.id.sso_auth_url);

        result.findViewById(R.id.sso_refresh).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ssoRefresh();
            }
        });

        CheckBox setShowPassword = ((CheckBox)result.findViewById(R.id.showPassword));
        setShowPassword.setOnCheckedChangeListener(mSetShowPassword);

        mStatus = (TextView)result.findViewById(R.id.status);
        result.findViewById(R.id.vr_login).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mStatus.setText(R.string.in_progress);
                VR.login(mEmail.getText().toString(), mPassword.getText().toString(),
                        mVRCallback, null, null);
            }
        });


        result.findViewById(R.id.sso_login).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mStatus.setText(R.string.in_progress);
                VR.loginSamsungAccount(mSSOAuthToken.getText().toString(), mSSOAuthURL.getText().toString(),
                        mSSOCallback, null, null);
            }
        });

        result.findViewById(R.id.create_account).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Util.showCreateAccountPage(mLocalBroadcastManager);
            }
        });

        mEmail.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        mEndPoint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = getActivity();
                Intent intent = new Intent(context, EndPointConfigActivity.class);
                context.startActivity(intent);
            }
        });
        mSetShowPassword.onCheckedChanged(setShowPassword, setShowPassword.isChecked());

        String endPoint = VR.getEndPoint();
        if (null != endPoint) {
            mEndPoint.setText(endPoint);
        }
        setLoginEnable(false);
        ssoRefresh();

        return result;
    }

    private void setLoginEnable(boolean enable) {
        Util.setEnabled(mViewStack, mLoginGroup, enable);
    }


    private final VR.Result.GetUserBySessionToken mCallbackForToken = new VR.Result.GetUserBySessionToken() {

        @Override
        public void onSuccess(Object closure, User user) {
            if (DEBUG) {
                Log.d(TAG, "onSuccess user: " + user);
            }

            if (hasValidViews()) {
                Resources res = getResources();
                String text = String.format(res.getString(R.string.auth_success), user.getUserId());
                mStatus.setText(text);
                Bundle args = new Bundle();
                args.putString(LoggedInFragment.PARAM_USER, user.getUserId());
                Util.showLoggedInPage(mLocalBroadcastManager, args);
            }
        }

        @Override
        public void onFailure(Object closure, int status) {
            if (DEBUG) {
                Log.d(TAG, "onError status: " + status);
            }
            if (hasValidViews()) {
                Resources res = getResources();
                String text = String.format(res.getString(R.string.failure_with_status), status);
                mStatus.setText(text);
            }
        }

        @Override
        public void onCancelled(Object closure) {
            if (DEBUG) {
                Log.d(TAG, "onCancelled");
            }
        }

        @Override
        public void onException(Object o, Exception ex) {
            if (hasValidViews()) {
                Resources res = getResources();
                String text = String.format(res.getString(R.string.failure_with_exception), ex.getMessage());
                mStatus.setText(text);
            }
        }
    };


    private final VR.Result.Destroy mDestroyCallback = new VR.Result.Destroy() {

        @Override
        public void onFailure(Object closure, int status) {

        }

        @Override
        public void onSuccess(Object closure) {
            initVR();
        }

    };

    private final VR.Result.Init mInitCallback = new VR.Result.Init() {

        @Override
        public void onFailure(Object closure, int status) {
            setLoginEnable(false);
        }

        @Override
        public void onSuccess(Object closure) {

            Context ctx = getActivity().getApplicationContext();
            SharedPreferences sharedPref = ctx.getSharedPreferences("Sample2016", Context.MODE_PRIVATE);
            String userId = sharedPref.getString("UserID", null);
            String sessionToken = sharedPref.getString("SessionToken", null);
            Log.d(TAG, "found persisted  userId=" + userId + " sessionToken=" + sessionToken);

            if ((userId != null)  && (sessionToken !=null)) {
                VR.getUserBySessionToken(userId, sessionToken, mCallbackForToken, null, null);
            }
            else {
                setLoginEnable(true);
            }
        }
    };

    private void initVR() {
        Context context = getActivity();

        JSONObject configItem = EndPointConfigFragment.getSelectedEndPointConfig(context);
        updateEndPointOnUI(configItem);
        if (null != configItem) {
            String apiKey = configItem.optString(EndPointConfigFragment.CFG_API_KEY, null);
            String endPoint = configItem.optString(EndPointConfigFragment.CFG_ENDPOINT, null);
            if (null != apiKey && null != endPoint) {
                VR.init(endPoint, apiKey, new HttpPluginOkHttp(), mInitCallback, null, null);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        Context context = getActivity();
        JSONObject configItem = EndPointConfigFragment.getSelectedEndPointConfig(context);
        updateEndPointOnUI(configItem);

        String vrEP = VR.getEndPoint();
        String vrAK = VR.getApiKey();

        if (null == configItem ||
            !configItem.optString(EndPointConfigFragment.CFG_API_KEY, "").equals(vrAK) ||
                !configItem.optString(EndPointConfigFragment.CFG_ENDPOINT, "").equals(vrEP)) {
            setLoginEnable(false);
            if (!VR.destroyAsync(mDestroyCallback, null, null)) {
                initVR();
            }
        } else {
            setLoginEnable(true);
        }
    }

    private void updateEndPointOnUI(JSONObject item) {
        if (null == mEndPoint) {
            return;
        }
        String ep = null;
        if (null != item) {
            ep = item.optString(EndPointConfigFragment.CFG_ENDPOINT, null);
        }
        if (null == ep || ep.isEmpty()) {
            mEndPoint.setText(R.string.select_config);
        } else {
            mEndPoint.setText(ep);
        }
    }

    private static final int REQUEST_ID_ACCESS_TOKEN   = 0x1001;
    private static final String[] SSO_ADDITIONAL_USER_DATA = new String[] {
            SAUtil.EXTRA_AUTH_SERVER_URL,
            SAUtil.EXTRA_BIRTHDAY,
            SAUtil.EXTRA_LOGIN_ID,
            SAUtil.EXTRA_USER_ID };

    private void ssoRefresh() {
        mSSOData = null;
        updateSSOUI();

        Context context = getActivity();
        JSONObject selectedConfig = EndPointConfigFragment.getSelectedEndPointConfig(context);
        if (null == selectedConfig) {
            mStatus.setText(R.string.invalid_config);
            return;
        }
        String ssoAppId = selectedConfig.optString(EndPointConfigFragment.CFG_SSO_APP_ID);
        if (null == ssoAppId) {
            mStatus.setText(R.string.invalid_sso_app_id);
            return;
        }
        String ssoAppSecret = selectedConfig.optString(EndPointConfigFragment.CFG_SSO_APP_SECRET);
        if (null == ssoAppSecret) {
            mStatus.setText(R.string.invalid_sso_app_secret);
            return;
        }
        startActivityForResult(SAUtil.buildRequestTokenIntent(ssoAppId, ssoAppSecret,
                SSO_ADDITIONAL_USER_DATA, null, null), REQUEST_ID_ACCESS_TOKEN);
    }

    private Bundle mSSOData = null;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ID_ACCESS_TOKEN:
                mSSOData = null;
                if (data != null) {
                    StringBuilder bldr = new StringBuilder();
                    mSSOData = data.getExtras();
                    bldr.append("RequestToken").append(" * ").append(resultCode);
                    if (null != mSSOData) {
                        for (String key : mSSOData.keySet()) {
                            bldr.append('\n').append(key).append(": ").append(mSSOData.get(key));
                        }
                    }
                    Log.e(TAG, bldr.toString());
                }
                updateSSOUI();
                return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void updateSSOUI() {
        if (!hasValidViews()) {
            return;
        }
        mSSOAuthToken.setText("");
        mSSOAuthURL.setText("");
        mSSOLoginId.setText("");
        if (null == mSSOData) {
            return;
        }
        mSSOAuthToken.setText(mSSOData.getString(SAUtil.EXTRA_ACCESS_TOKEN));
        mSSOAuthURL.setText(mSSOData.getString(SAUtil.EXTRA_AUTH_SERVER_URL));
        mSSOLoginId.setText(mSSOData.getString(SAUtil.EXTRA_LOGIN_ID));
    }

    @Override
    public void onDestroyView() {
        mEndPoint.setOnClickListener(null);

        mEmail = null;
        mEndPoint = null;
        mPassword = null;
        mGrpSSOLogin = null;
        mStatus = null;
        mLoginGroup = null;
        mSSOAuthToken = null;
        mSSOLoginId = null;
        mSSOAuthURL = null;
        super.onDestroyView();
    }

    private final VR.Result.Login mVRCallback = new VR.Result.Login() {

        @Override
        public void onSuccess(Object closure, User user) {
            Resources res = getResources();
            String text = String.format(res.getString(R.string.auth_success), user.getUserId());

            Context ctx = getActivity().getApplicationContext();
            SharedPreferences sharedPref = ctx.getSharedPreferences("Sample2016", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("UserID", user.getUserId());
            editor.putString("SessionToken", user.getSessionToken());
            editor.commit();

            if (DEBUG) {
                Log.d(TAG, "onSuccess text: " + text);
            }
            if (hasValidViews()) {
                mStatus.setText(text);

                Bundle args = new Bundle();
                args.putString(LoggedInFragment.PARAM_USER, user.getUserId());
                Util.showLoggedInPage(mLocalBroadcastManager, args);
            }
        }

        @Override
        public void onFailure(Object closure, int status) {
            Resources res = getResources();
            String text = String.format(res.getString(R.string.auth_failure), status);
            if (DEBUG) {
                Log.d(TAG, "onError text: " + text);
            }
            if (hasValidViews()) {
                mStatus.setText(text);
            }
        }

        @Override
        public void onCancelled(Object closure) {
            if (DEBUG) {
                Log.d(TAG, "onCancelled");
            }
            if (hasValidViews()) {
                mStatus.setText(R.string.auth_cancelled);
            }

        }

        @Override
        public void onException(Object closure, Exception ex) {
            if (hasValidViews()) {
                Resources res = getResources();
                String text = String.format(res.getString(R.string.failure_with_exception), ex.getMessage());
                mStatus.setText(text);
            }

        }
    };

    private final VR.Result.LoginSSO mSSOCallback = new VR.Result.LoginSSO() {

        @Override
        public void onSuccess(Object closure, User user) {
            Resources res = getResources();
            String text = String.format(res.getString(R.string.auth_success), user.getUserId());

            Context ctx = getActivity().getApplicationContext();
            SharedPreferences sharedPref = ctx.getSharedPreferences("Sample2016", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("UserID", user.getUserId());
            editor.putString("SessionToken", user.getSessionToken());
            editor.commit();

            if (DEBUG) {
                Log.d(TAG, "onSuccess text: " + text);
            }
            if (hasValidViews()) {
                mStatus.setText(text);

                Bundle args = new Bundle();
                args.putString(LoggedInFragment.PARAM_USER, user.getUserId());
                Util.showLoggedInPage(mLocalBroadcastManager, args);
            }
        }

        @Override
        public void onFailure(Object closure, int status) {
            Resources res = getResources();
            String text = String.format(res.getString(R.string.auth_failure), status);
            if (DEBUG) {
                Log.d(TAG, "onError text: " + text);
            }
            if (hasValidViews()) {
                mStatus.setText(text);
            }
        }

        @Override
        public void onCancelled(Object closure) {
            if (DEBUG) {
                Log.d(TAG, "onCancelled");
            }
            if (hasValidViews()) {
                mStatus.setText(R.string.auth_cancelled);
            }

        }

        @Override
        public void onException(Object closure, Exception ex) {
            if (hasValidViews()) {
                Resources res = getResources();
                String text = String.format(res.getString(R.string.failure_with_exception), ex.getMessage());
                mStatus.setText(text);
            }

        }
    };

    static LoginFragment newFragment() {
        return new LoginFragment();
    }

}

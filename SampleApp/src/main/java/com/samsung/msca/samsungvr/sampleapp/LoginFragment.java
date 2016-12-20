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

public class LoginFragment extends BaseFragment {

    static final String TAG = Util.getLogTag(LoginFragment.class);
    private static final boolean DEBUG = Util.DEBUG;

    private TextView mEmail, mEndPoint;
    private EditText mPassword;
    private CheckBox mShowPassword;
    private TextView mStatus = null;
    private Button mLogin = null, mCreateAccount = null;

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

    private final View.OnClickListener mConfigureEndPoints = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Context context = getActivity();
            Intent intent = new Intent(context, EndPointConfigActivity.class);
            context.startActivity(intent);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View result = inflater.inflate(R.layout.fragment_login, null, false);

        mEmail = (TextView)result.findViewById(R.id.email);
        mEndPoint = (TextView)result.findViewById(R.id.end_point);
        mPassword = (EditText)result.findViewById(R.id.password);
        mShowPassword = (CheckBox)result.findViewById(R.id.showPassword);
        mStatus = (TextView)result.findViewById(R.id.status);
        mLogin = (Button)result.findViewById(R.id.login);
        mCreateAccount = (Button)result.findViewById(R.id.create_account);

        mShowPassword.setOnCheckedChangeListener(mSetShowPassword);
        mLogin.setOnClickListener(onLogin);
        mEmail.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        mCreateAccount.setOnClickListener(onCreateAccount);
        mEndPoint.setOnClickListener(mConfigureEndPoints);
        mSetShowPassword.onCheckedChanged(mShowPassword, mShowPassword.isChecked());

        String endPoint = VR.getEndPoint();
        if (null != endPoint) {
            mEndPoint.setText(endPoint);
        }
        setLoginEnable(false);
        return result;
    }

    private void setLoginEnable(boolean enable) {
        mLogin.setEnabled(enable);
        mEmail.setEnabled(enable);
        mPassword.setEnabled(enable);
        mShowPassword.setEnabled(enable);
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

        EndPointConfigFragment.ConfigItem configItem = EndPointConfigFragment.getSelectedEndPointConfig(context);
        updateEndPointOnUI(configItem);
        if (null != configItem) {
            String endPoint = configItem.getEndPoint();
            VR.init(endPoint, configItem.getApiKey(), new HttpPluginOkHttp(), mInitCallback, null, null);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        Context context = getActivity();
        EndPointConfigFragment.ConfigItem configItem = EndPointConfigFragment.getSelectedEndPointConfig(context);
        updateEndPointOnUI(configItem);

        if (null == configItem || !configItem.matches(VR.getEndPoint(), VR.getApiKey())) {
            setLoginEnable(false);
            if (!VR.destroyAsync(mDestroyCallback, null, null)) {
                initVR();
            }
        } else {
            setLoginEnable(true);
        }
    }

    private void updateEndPointOnUI(EndPointConfigFragment.ConfigItem item) {
        if (null == mEndPoint) {
            return;
        }
        if (null == item) {
            mEndPoint.setText(R.string.select_end_point);
        } else {
            mEndPoint.setText(item.getEndPoint());
        }
    }

    @Override
    public void onDestroyView() {
        mLogin.setOnClickListener(null);
        mEndPoint.setOnClickListener(null);
        mShowPassword.setOnCheckedChangeListener(null);
        mCreateAccount.setOnClickListener(null);

        mEmail = null;
        mEndPoint = null;
        mPassword = null;
        mLogin = null;
        mStatus = null;
        mShowPassword = null;
        mCreateAccount = null;
        super.onDestroyView();
    }

    private final VR.Result.Login mCallback = new VR.Result.Login() {

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

    View.OnClickListener onLogin = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mStatus.setText(R.string.in_progress);
            VR.login(
                    mEmail.getText().toString(),
                    mPassword.getText().toString(),
                    mCallback, null, null);
        }
    };

    View.OnClickListener onCreateAccount = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Util.showCreateAccountPage(mLocalBroadcastManager);
        }
    };

    static LoginFragment newFragment() {
        return new LoginFragment();
    }

}

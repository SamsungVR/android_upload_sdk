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
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.pm.ActivityInfoCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.samsung.msca.samsungvr.sdk.UnverifiedUser;
import com.samsung.msca.samsungvr.sdk.User;
import com.samsung.msca.samsungvr.sdk.UserLiveEvent;
import com.samsung.msca.samsungvr.sdk.VR;

import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class NewUserFragment extends BaseFragment {


    static final String TAG = Util.getLogTag(NewUserFragment.class);
    private static final boolean DEBUG = Util.DEBUG;

    private TextView mStatus, mEmail, mPassword, mUsername;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private static final int REQUEST_ID_CREATE_ACCOUNT = 0x1000;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View result = inflater.inflate(R.layout.fragment_new_user, null, false);

        result.findViewById(R.id.create_vr_account).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mStatus.setText(R.string.in_progress);
                VR.newUser(mUsername.getText().toString(),
                        mEmail.getText().toString(), mPassword.getText().toString(),
                        mCallback, null, null);
            }
        });

        result.findViewById(R.id.create_sso_account).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mStatus.setText("");

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
                startActivityForResult(SAUtil.buildAddAccountIntent(getActivity(), ssoAppId, ssoAppSecret),
                        REQUEST_ID_CREATE_ACCOUNT);
            }
        });

        mEmail = (TextView)result.findViewById(R.id.email);
        mPassword = (EditText)result.findViewById(R.id.password);
        mUsername = (EditText)result.findViewById(R.id.user_name);
        mStatus = (TextView)result.findViewById(R.id.status);

        return result;
    }

    @Override
    public void onDestroyView() {
        mStatus = null;
        mEmail = null;
        mPassword = null;
        mUsername = null;
        super.onDestroyView();
    }

    private final VR.Result.NewUser mCallback = new VR.Result.NewUser() {

        @Override
        public void onSuccess(Object closure, UnverifiedUser user) {
            Resources res = getResources();
            String text = String.format(res.getString(R.string.create_user_success), user.getUserId());

            if (DEBUG) {
                Log.d(TAG, "onSuccess text: " + text);
            }
            if (hasValidViews()) {
                mStatus.setText(text);
            }
        }

        @Override
        public void onFailure(Object closure, int status) {
            Resources res = getResources();
            String text = String.format(res.getString(R.string.create_user_failure), status);
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
                mStatus.setText(R.string.create_user_cancelled);
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ID_CREATE_ACCOUNT:
                mStatus.setText(Activity.RESULT_OK == resultCode ? R.string.success : R.string.failure);
                String result = null;
                if (data != null) {
                    StringBuilder bldr = new StringBuilder();
                    Bundle bundle = data.getExtras();
                    bldr.append("RequestToken").append(" * ").append(resultCode);
                    if (bundle != null) {
                        for (String key : bundle.keySet()) {
                            bldr.append('\n').append(key).append(": ").append(bundle.get(key));
                        }
                    }
                    result = bldr.toString();
                    Log.e(TAG, result);
                }
                return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    static NewUserFragment newFragment() {
        return new NewUserFragment();
    }


}

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
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.samsung.msca.samsungvr.sdk.User;
import com.samsung.msca.samsungvr.sdk.VR;
import com.samsung.msca.samsungvr.ui.UILib;

import org.json.JSONObject;

public class LoginUILibFragment extends BaseFragment {

    static final String TAG = Util.getLogTag(LoginUILibFragment.class);
    private static final boolean DEBUG = Util.DEBUG;

    private TextView mEndPoint;
    private TextView mStatus = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View result = inflater.inflate(R.layout.fragment_login_uilib, null, false);

        mEndPoint = (TextView)result.findViewById(R.id.end_point);
        mStatus = (TextView)result.findViewById(R.id.status);
        result.findViewById(R.id.init).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "init button click");
                Context context = getActivity();

                JSONObject configItem = EndPointConfigFragment.getSelectedEndPointConfig(context);
                updateEndPointOnUI(configItem);
                if (null != configItem) {
                    String apiKey = configItem.optString(EndPointConfigFragment.CFG_API_KEY, null);
                    String endPoint = configItem.optString(EndPointConfigFragment.CFG_ENDPOINT, null);
                    String ssoAppId = configItem.optString(EndPointConfigFragment.CFG_SSO_APP_ID, null);
                    String ssoAppSecret = configItem.optString(EndPointConfigFragment.CFG_SSO_APP_SECRET, null);

                    if (null != apiKey && null != endPoint) {
                        UILib.init(getActivity(), endPoint, apiKey, ssoAppId, ssoAppSecret, mUILibCallback, null);
                    }
                }
            }
        });
        result.findViewById(R.id.login).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mStatus.setText(R.string.empty);
                boolean status = UILib.login();
                Log.d(TAG, "login button click status: " + status);

            }
        });

        result.findViewById(R.id.logout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mStatus.setText(R.string.empty);
                boolean status = UILib.logout();
                Log.d(TAG, "logout button click " + status);
            }
        });

        result.findViewById(R.id.clear_status).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "clear status button click");
                mStatus.setText(R.string.empty);
                mStatus.invalidate();
            }
        });

        mEndPoint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = getActivity();
                Intent intent = new Intent(context, EndPointConfigActivity.class);
                context.startActivity(intent);
            }
        });

        String endPoint = VR.getEndPoint();
        if (null != endPoint) {
            mEndPoint.setText(endPoint);
        }

        return result;
    }

    private UILib.Callback mUILibCallback = new UILib.Callback() {
        @Override
        public void onLoggedIn(User user, Object o) {
            Log.d(TAG, "onLoggedIn: " + user + " " + o);
            Bundle args = new Bundle();
            args.putString(LoggedInFragment.PARAM_USER, user.getUserId());
            Util.showLoggedInPage(mLocalBroadcastManager, args);
        }

        @Override
        public void onLibInitSuccess(Object o) {
            Log.d(TAG, "onLibInitSuccess: " + o);
            if (hasValidViews()) {
                mStatus.setText(R.string.lib_init_success);
            }

        }

        @Override
        public void onLibInitFailed(Object o) {
            Log.d(TAG, "onLibInitFailed: " + o);
            if (hasValidViews()) {
                mStatus.setText(R.string.lib_init_fail);
            }
        }

        @Override
        public void onFailure(Object o) {
            Log.d(TAG, "onFailure: " + o);
            if (hasValidViews()) {
                mStatus.setText(R.string.failure);
            }
        }

    };


    @Override
    public void onResume() {
        super.onResume();

        Context context = getActivity();
        JSONObject configItem = EndPointConfigFragment.getSelectedEndPointConfig(context);
        updateEndPointOnUI(configItem);
        mStatus.setText(R.string.empty);
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

    @Override
    public void onDestroyView() {
        mEndPoint.setOnClickListener(null);
        mStatus = null;
        super.onDestroyView();
    }

}

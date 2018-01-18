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

import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.samsung.msca.samsungvr.sdk.User;
import com.samsung.msca.samsungvr.sdk.VR;

import org.json.JSONException;
import org.json.JSONObject;

public class GetUserBySessionInfoFragment extends BaseFragment {

    static final String TAG = Util.getLogTag(GetUserBySessionInfoFragment.class);
    private static final boolean DEBUG = Util.DEBUG;

    private TextView mStatus, mSessionInfo, mUserId, mUserName, mUserEmail;
    private Button mGetUser;
    private SimpleNetworkImageView mProfilePicDark, mProfilePicLight;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();
        if (null != bundle) {
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View result = inflater.inflate(R.layout.fragment_get_user_by_session_info, null, false);

        mGetUser = (Button)result.findViewById(R.id.getUser);
        mStatus = (TextView)result.findViewById(R.id.status);
        mSessionInfo = (TextView)result.findViewById(R.id.session_info);
        mUserName = (TextView)result.findViewById(R.id.user_name);
        mUserId = (TextView)result.findViewById(R.id.user_id);
        mProfilePicDark = (SimpleNetworkImageView)result.findViewById(R.id.user_profile_pic_dark);
        mProfilePicLight = (SimpleNetworkImageView)result.findViewById(R.id.user_profile_pic_light);
        mUserEmail = (TextView)result.findViewById(R.id.user_email);
        mGetUser.setOnClickListener(mOnClickListener);

        return result;
    }

    @Override
    public void onDestroyView() {
        mGetUser.setOnClickListener(null);
        mGetUser = null;
        mStatus = null;
        mSessionInfo = null;
        mUserName = null;
        mUserId = null;
        mUserEmail = null;
        mProfilePicDark = null;
        mProfilePicLight = null;

        super.onDestroyView();
    }

    private final VR.Result.GetUserBySessionToken mCallbackForToken = new VR.Result.GetUserBySessionToken() {

        @Override
        public void onSuccess(Object closure, User user) {
            if (DEBUG) {
                Log.d(TAG, "onSuccess user: " + user);
            }
            if (hasValidViews()) {
                mStatus.setText(R.string.success);
                mUserId.setText(user.getUserId());
                mUserName.setText(user.getName());
                mUserEmail.setText(user.getEmail());
                mProfilePicDark.setImageUrl(user.getProfilePicDarkUrl());
                mProfilePicLight.setImageUrl(user.getProfilePicLightUrl());
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
        public void onException(Object closure, Exception ex) {
            if (hasValidViews()) {
                Resources res = getResources();
                String text = String.format(res.getString(R.string.failure_with_exception), ex.getMessage());
                mStatus.setText(text);
            }

        }
    };

    View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mStatus.setText(R.string.in_progress);
            mUserId.setText("");
            mUserName.setText("");
            mUserEmail.setText("");
            mProfilePicDark.setImageDrawable(null);
            mProfilePicLight.setImageDrawable(null);
            VR.getUserBySessionToken(mSessionInfo.getText().toString(), mCallbackForToken, null, null);
        }
    };

    static GetUserBySessionInfoFragment newFragment() {
        return new GetUserBySessionInfoFragment();
    }

}

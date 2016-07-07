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
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class NewUserFragment extends BaseFragment {


    static final String TAG = Util.getLogTag(NewUserFragment.class);
    private static final boolean DEBUG = Util.DEBUG;

    private Button mCreateUser;
    private TextView mStatus, mEmail, mPassword, mUsername;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View result = inflater.inflate(R.layout.fragment_new_user, null, false);

        mCreateUser = (Button)result.findViewById(R.id.create_user);

        mCreateUser.setOnClickListener(mOnClickListener);
        mEmail = (TextView)result.findViewById(R.id.email);
        mPassword = (EditText)result.findViewById(R.id.password);
        mUsername = (EditText)result.findViewById(R.id.user_name);
        mStatus = (TextView)result.findViewById(R.id.status);

        return result;
    }

    @Override
    public void onDestroyView() {
        mCreateUser.setOnClickListener(null);
        mCreateUser = null;
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


    View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mStatus.setText(R.string.in_progress);
            VR.newUser(mUsername.getText().toString(),
                    mEmail.getText().toString(), mPassword.getText().toString(),
                    mCallback, null, null);
        }
    };

    static NewUserFragment newFragment() {
        return new NewUserFragment();
    }


}

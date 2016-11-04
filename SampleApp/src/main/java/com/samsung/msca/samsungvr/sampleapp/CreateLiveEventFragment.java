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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import com.samsung.msca.samsungvr.sdk.User;
import com.samsung.msca.samsungvr.sdk.UserLiveEvent;
import com.samsung.msca.samsungvr.sdk.VR;

import java.util.Calendar;

public class CreateLiveEventFragment extends BaseFragment {

    static final String TAG = Util.getLogTag(CreateLiveEventFragment.class);
    private static final boolean DEBUG = Util.DEBUG;

    private TextView mTitle, mDescription, mStatus;
    private Spinner mProtocol, mVideoStereoscopicType;
    private Button mCreateLiveEvent;

    private User mUser;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();
        if (null != bundle) {
            String userId = bundle.getString(LoggedInFragment.PARAM_USER);
            if (null != userId) {
                mUser = VR.getUserById(userId);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View result = inflater.inflate(R.layout.fragment_create_live_event, null, false);

        mTitle = (TextView)result.findViewById(R.id.title);
        mDescription = (TextView)result.findViewById(R.id.description);
        mProtocol = (Spinner)result.findViewById(R.id.protocol);
        mVideoStereoscopicType = (Spinner)result.findViewById(R.id.video_stereoscopy_type);
        mCreateLiveEvent = (Button)result.findViewById(R.id.createLiveEvent);
        mStatus = (TextView)result.findViewById(R.id.status);
        mCreateLiveEvent = (Button)result.findViewById(R.id.createLiveEvent);

        ArrayAdapter<UserLiveEvent.Protocol> protocolAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, UserLiveEvent.Protocol.values());
        protocolAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mProtocol.setAdapter(protocolAdapter);


        ArrayAdapter<UserLiveEvent.VideoStereoscopyType> videoStereoscopyTypeAdapter =
                new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, UserLiveEvent.VideoStereoscopyType.values());
        videoStereoscopyTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mVideoStereoscopicType.setAdapter(videoStereoscopyTypeAdapter);

        mCreateLiveEvent.setOnClickListener(mOnClickListener);

        return result;
    }

    @Override
    public void onDestroyView() {
        mCreateLiveEvent.setOnClickListener(null);
        mTitle = null;
        mDescription = null;
        mProtocol = null;
        mCreateLiveEvent = null;
        mStatus = null;
        super.onDestroyView();
    }

    private final User.Result.CreateLiveEvent mCallback = new User.Result.CreateLiveEvent() {

        @Override
        public void onSuccess(Object closure, UserLiveEvent event) {
            if (DEBUG) {
                Log.d(TAG, "onSuccess event: " + event);
            }
            if (hasValidViews()) {
                mStatus.setText(R.string.success);
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
            Resources res = getResources();
            String text = String.format(res.getString(R.string.failure_with_exception), ex.getMessage());
            if (hasValidViews()) {
                mStatus.setText(text);
            }
        }
    };

    View.OnClickListener mOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (null == mUser) {
                return;
            }


            mStatus.setText(R.string.in_progress);
            mUser.createLiveEvent(mTitle.getText().toString(),
                    mDescription.getText().toString(),
                    (UserLiveEvent.Protocol) mProtocol.getSelectedItem(),
                    (UserLiveEvent.VideoStereoscopyType) mVideoStereoscopicType.getSelectedItem(),

                    mCallback, null, null);
        }
    };

    static CreateLiveEventFragment newFragment() {
        return new CreateLiveEventFragment();
    }

}

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
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.samsung.msca.samsungvr.sdk.User;
import com.samsung.msca.samsungvr.sdk.UserVideo;
import com.samsung.msca.samsungvr.sdk.VR;

import java.util.ArrayList;
import java.util.List;

import static com.samsung.msca.samsungvr.sdk.User.Result;

public class UploadVideoFragment extends BaseFragment {

    static final String TAG = Util.getLogTag(UploadVideoFragment.class);

    private Button mRetryButton, mCancelButton, mUploadButton, mAbortButton;
    private ProgressBar mUploadProgress;
    private TextView mStatus, mUploadProgressRaw;
    private User mUser;
    private Spinner mPermission;
    private LocationManager mLocationManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLocationManager = (LocationManager)getActivity().getSystemService(Context.LOCATION_SERVICE);
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
        View result = inflater.inflate(R.layout.fragment_upload_video, null, false);

        mUploadButton = (Button)result.findViewById(R.id.upload);
        mCancelButton = (Button)result.findViewById(R.id.cancel);
        mRetryButton = (Button)result.findViewById(R.id.retry);
        mAbortButton = (Button)result.findViewById(R.id.abort);

        mUploadProgress = (ProgressBar)result.findViewById(R.id.uploadProgress);
        mUploadProgressRaw = (TextView)result.findViewById(R.id.uploadProgressRaw);
        mStatus = (TextView)result.findViewById(R.id.status);
        mStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (hasValidViews()) {
                    mStatus.setText("");
                }
            }
        });
        mVideoUploadClosure = new Object();

        mUploadButton.setOnClickListener(mPerformAction);
        mCancelButton.setOnClickListener(mPerformAction);
        mRetryButton.setOnClickListener(mPerformAction);
        mAbortButton.setOnClickListener(mPerformAction);

        mPermission = (Spinner)result.findViewById(R.id.permission);

        ArrayAdapter<UserVideo.Permission> permissionAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_item, UserVideo.Permission.values());
        permissionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mPermission.setAdapter(permissionAdapter);
        return result;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setMode(Mode.IDLE);
    }

    @Override
    public void onDestroyView() {
        closeSource();

        mRetryButton.setOnClickListener(null);
        mRetryButton = null;
        mCancelButton.setOnClickListener(null);
        mCancelButton = null;
        mUploadButton.setOnClickListener(null);
        mUploadButton = null;
        mAbortButton.setOnClickListener(null);
        mAbortButton = null;

        mStatus.setOnClickListener(null);

        mUploadProgress = null;
        mUploadProgressRaw = null;
        mVideoUploadClosure = null;
        mPermission = null;
        mStatus = null;
        super.onDestroyView();
    }

    private final Result.UploadVideo mCallback = new Result.UploadVideo() {

        @Override
        public void onException(Object closure, Exception ex) {
            if (null != mUserVideo) {
                setMode(Mode.FAILED);
            } else {
                closeSource();
            }

            if (hasValidViews()) {
                Resources res = getResources();
                String text = String.format(res.getString(R.string.failure_with_exception), ex.getMessage());
                mStatus.setText(text);
            }
        }

        @Override
        public void onSuccess(Object closure) {
            closeSource();
            if (hasValidViews()) {
                mStatus.setText(R.string.success);
            }

        }

        @Override
        public void onProgress(Object o, float progress, long l, long l1) {
            if (hasValidViews()) {
                mUploadProgress.setProgress((int)progress);
                mUploadProgressRaw.setText(Float.toString(progress));
            }

        }

        @Override
        public void onProgress(Object o, long completed) {
            mUploadProgressRaw.setText(Long.toString(completed));
        }

        @Override
        public void onFailure(Object closure, int status) {
            setMode(Mode.FAILED);
            if (hasValidViews()) {
                mStatus.setText(R.string.failure);
            }
        }

        @Override
        public void onCancelled(Object closure) {
            if (hasValidViews()) {
                mStatus.setText(R.string.cancelled);
            }
            closeSource();
        }

        @Override
        public void onVideoIdAvailable(Object o, UserVideo userVideo) {
            mUserVideo = userVideo;
            Log.d(TAG, "Video id available: " + mUserVideo.getVideoId());
        }
    };

    private Object mVideoUploadClosure;
    private UserVideo mUserVideo;

    private static final int ACTIVITY_CHOOSE_FILE = 0x1000;

    private ParcelFileDescriptor mSource;

    private boolean uploadVideo(Uri uri) {
        if (null == uri) {
            return false;
        }
        try {
            mSource = getActivity().getContentResolver().openFileDescriptor(uri, "r");
            long now = System.currentTimeMillis();
            UserVideo.Permission permission = (UserVideo.Permission) mPermission.getSelectedItem();
            String txt = "Test_" + now;
            List<String> tags = new ArrayList<>();
            tags.add(String.valueOf(System.currentTimeMillis()));
            tags.add(Build.DEVICE);
            tags.add(Build.BRAND);
            tags.add(Build.MANUFACTURER);
            tags.add(Build.MODEL);
            tags.add(Build.SERIAL);

            Boolean stabilize = false;
            UserVideo.LocationInfo locationInfo = new UserVideo.LocationInfo(0.1, 0.2, 0.3);

            if (mUser.uploadVideo(mSource,
                    txt,
                    "Desc_" + txt,
                    tags,
                    permission,
                    "TestCam",
                    locationInfo,
                    stabilize,
                    mCallback,
                    null,
                    mVideoUploadClosure)) {
                setMode(Mode.UPLOADING);
            }
            return true;
        } catch (Exception ex) {
            Log.d(TAG, "Could not open file: " + uri, ex);
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (Activity.RESULT_OK != resultCode) {
            return;
        }
        switch (requestCode) {
            case ACTIVITY_CHOOSE_FILE:
                uploadVideo(data.getData());
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private boolean chooseUploadVideo() {
        if (null == mUser) {
            return false;
        }

        Intent chooseFile;
        Intent intent;
        chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
        chooseFile.setType("*/*");
        intent = Intent.createChooser(chooseFile, getResources().getString(R.string.choose_file));
        if (null != intent) {
            try {
                startActivityForResult(intent, ACTIVITY_CHOOSE_FILE);
                return true;
            } catch (Exception ex) {
            }
        }
        Toast.makeText(getActivity(), R.string.missing_app_file_chooser, Toast.LENGTH_SHORT).show();
        return false;
    }

    private void cancelUploadVideo() {
        if (null != mUserVideo) {
            mUserVideo.cancelUpload(mVideoUploadClosure);
        } else {
            mUser.cancelUploadVideo(mVideoUploadClosure);
        }
    }

    private void retryUploadVideo() {
        if (null != mUserVideo &&
                mUserVideo.retryUpload(mSource, mCallback, null, mVideoUploadClosure)) {
            setMode(Mode.UPLOADING);
        }
    }

    private enum Mode {
        IDLE,
        UPLOADING,
        FAILED
    }

    private Mode mCurrentMode = Mode.IDLE;

    private void setMode(Mode mode) {
        mCurrentMode = mode;
        if (hasValidViews()) {
            switch (mCurrentMode) {
                case IDLE:
                    mUploadButton.setVisibility(View.VISIBLE);
                    mCancelButton.setVisibility(View.GONE);
                    mRetryButton.setVisibility(View.GONE);
                    mAbortButton.setVisibility(View.GONE);
                    mUploadProgress.setProgress(0);
                    mUploadProgressRaw.setText("");
                    break;

                case FAILED:
                    mUploadButton.setVisibility(View.GONE);
                    mCancelButton.setVisibility(View.GONE);
                    mAbortButton.setVisibility(View.VISIBLE);
                    mRetryButton.setVisibility(View.VISIBLE);
                    break;

                case UPLOADING:
                    mAbortButton.setVisibility(View.GONE);
                    mUploadButton.setVisibility(View.GONE);
                    mCancelButton.setVisibility(View.VISIBLE);
                    mRetryButton.setVisibility(View.GONE);
                    mStatus.setText(R.string.in_progress);
                    break;
            }
        }
    }

    final View.OnClickListener mPerformAction = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (mCurrentMode) {
                case IDLE:
                    if (v == mUploadButton) {
                        chooseUploadVideo();
                    }
                    break;
                case UPLOADING:
                    if (v == mCancelButton) {
                        cancelUploadVideo();
                    }
                    break;
                case FAILED:
                    if (v == mAbortButton) {
                        closeSource();
                    } else if (v == mRetryButton) {
                        retryUploadVideo();
                    }
            }
        }
    };

    private void closeSource() {
        if (null != mSource) {
            try {
                mSource.close();
            } catch (Exception ex) {
            }
            mSource = null;
        }
        mUserVideo = null;
        setMode(Mode.IDLE);
    }

    static UploadVideoFragment newFragment() {
        return new UploadVideoFragment();
    }
}

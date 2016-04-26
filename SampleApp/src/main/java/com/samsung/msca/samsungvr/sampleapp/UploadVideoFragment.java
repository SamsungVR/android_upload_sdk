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
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
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

public class UploadVideoFragment extends BaseFragment {

    static final String TAG = Util.getLogTag(UploadVideoFragment.class);

    private Button mActionButton;
    private ProgressBar mUploadProgress;
    private TextView mStatus, mUploadProgressRaw;
    private User mUser;
    private Spinner mPermission;

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
        View result = inflater.inflate(R.layout.fragment_upload_video, null, false);

        mActionButton = (Button)result.findViewById(R.id.uploadVideo);
        mUploadProgress = (ProgressBar)result.findViewById(R.id.uploadProgress);
        mUploadProgressRaw = (TextView)result.findViewById(R.id.uploadProgressRaw);
        mStatus = (TextView)result.findViewById(R.id.status);
        mVideoUploadClosure = new Object();
        mActionButton.setOnClickListener(mPerformAction);
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
        setMode(Mode.UPLOAD);
    }

    @Override
    public void onDestroyView() {
        closeSource();

        mActionButton.setOnClickListener(null);
        mActionButton = null;
        mUploadProgress = null;
        mUploadProgressRaw = null;
        mVideoUploadClosure = null;
        mPermission = null;
        mStatus = null;
        super.onDestroyView();
    }

    private final User.Result.UploadVideo mCallback = new User.Result.UploadVideo() {

        @Override
        public void onException(Object closure, Exception ex) {
            closeSource();
            setMode(Mode.UPLOAD);

            if (hasValidViews()) {
                Resources res = getResources();
                String text = String.format(res.getString(R.string.failure_with_exception), ex.getMessage());
                mStatus.setText(text);
            }
        }

        @Override
        public void onSuccess(Object closure) {
            closeSource();
            setMode(Mode.UPLOAD);
            if (hasValidViews()) {
                mStatus.setText(R.string.success);
            }

        }

        @Override
        public void onProgress(Object closure, float progress) {
            if (hasValidViews()) {
                mUploadProgress.setProgress((int) progress);
                mUploadProgressRaw.setText(Float.toString(progress));
            }
        }

        @Override
        public void onFailure(Object closure, int status) {
            closeSource();
            setMode(Mode.UPLOAD);

            if (hasValidViews()) {
                mStatus.setText(R.string.failure);
            }


        }

        @Override
        public void onCancelled(Object closure) {
            if (hasValidViews()) {
                mStatus.setText(R.string.cancelled);
            }
            setMode(Mode.UPLOAD);
        }

    };

    private Object mVideoUploadClosure;

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
            if (mUser.uploadVideo(mSource, txt, "Desc_" + txt, permission,
                    mCallback, null, mVideoUploadClosure)) {
                setMode(Mode.CANCEL);
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
        mUser.cancelUploadVideo(mVideoUploadClosure);
    }

    private enum Mode {
        UPLOAD,
        CANCEL
    }

    private Mode mCurrentMode = Mode.UPLOAD;

    private void setMode(Mode mode) {
        mCurrentMode = mode;
        if (hasValidViews()) {
            switch (mCurrentMode) {
                case UPLOAD:
                    mActionButton.setText(R.string.upload_video);
                    mUploadProgress.setProgress(0);
                    mUploadProgressRaw.setText("");
                    break;
                case CANCEL:
                    mActionButton.setText(R.string.cancel_upload_video);
                    mStatus.setText(R.string.in_progress);
                    break;
            }
        }
    }

    final View.OnClickListener mPerformAction = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (mCurrentMode) {
                case UPLOAD:
                    chooseUploadVideo();
                    break;
                case CANCEL:
                    cancelUploadVideo();
                    break;
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
    }

    static UploadVideoFragment newFragment() {
        return new UploadVideoFragment();
    }
}

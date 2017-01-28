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
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.samsung.msca.samsungvr.sdk.User;
import com.samsung.msca.samsungvr.sdk.UserLiveEvent;
import com.samsung.msca.samsungvr.sdk.UserLiveEventSegment;
import com.samsung.msca.samsungvr.sdk.VR;

public class PublishLiveEventFromFileFragment extends BaseFragment {


    static final String TAG = Util.getLogTag(PublishLiveEventFromFileFragment.class);
    private static final boolean DEBUG = Util.DEBUG;

    private ViewGroup mUploadSegmentList;
    private TextView mIdView;
    private LayoutInflater mLayoutInflator;

    private UserLiveEvent mUserLiveEvent = null;

    static final String PARAM_LIVE_EVENT_ID = "paramLiveEventId";

    private final UserLiveEvent.Result.QueryLiveEvent mCallbackFindLiveEvent = new UserLiveEvent.Result.QueryLiveEvent() {

        @Override
        public void onSuccess(Object o, UserLiveEvent userLiveEvent) {
            mUserLiveEvent = userLiveEvent;
            if (hasValidViews()) {
                mIdView.setText(userLiveEvent.getId());
            }
        }

        @Override
        public void onFailure(Object closure, int status) {
            mUserLiveEvent = null;
            if (hasValidViews()) {
                Resources res = getActivity().getResources();
                String text = String.format(res.getString(R.string.failure_with_status), status);
                mIdView.setText(text);
            }
        }

        @Override
        public void onCancelled(Object o) {
            mUserLiveEvent = null;
        }

        @Override
        public void onException(Object o, Exception ex) {
            mUserLiveEvent = null;
            if (hasValidViews()) {
                Resources res = getActivity().getResources();
                String text = String.format(res.getString(R.string.failure_with_exception), ex.getMessage());
                mIdView.setText(text);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mLayoutInflator = LayoutInflater.from(getActivity());
        View result = mLayoutInflator.inflate(R.layout.fragment_publish_live_event_from_file, null, false);

        mUploadSegmentList = (ViewGroup)result.findViewById(R.id.upload_segment_list);
        mIdView = (TextView)result.findViewById(R.id.live_event_id);

        result.findViewById(R.id.pick_segment).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Util.launchDocPicker(PublishLiveEventFromFileFragment.this, PICK_SEGMENT_URI);
            }
        });

        Bundle bundle = getArguments();
        if (null != bundle) {
            String userId = bundle.getString(LoggedInFragment.PARAM_USER);
            if (null != userId) {
                User user = VR.getUserById(userId);
                if (null != user) {
                    String liveEventId = bundle.getString(PARAM_LIVE_EVENT_ID);
                    if (null != liveEventId) {
                        Log.d(TAG, "Querying live event with id: " + liveEventId);
                        user.queryLiveEvent(liveEventId, mCallbackFindLiveEvent, null, null);
                    }
                }
            }
        }

        return result;
    }

    @Override
    public void onDestroyView() {
        removeAllSegmentUploadListViews();

        mIdView = null;
        mUploadSegmentList = null;
        mLayoutInflator = null;
        super.onDestroyView();
    }

    public static final int PICK_SEGMENT_URI = 0x2001;

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (null != data) {
            int flags = data.getFlags();
            Log.d(TAG, "onActivityRest code: " + requestCode + " result: " + resultCode +
                    " data: " + data.toString() + " flags: " + flags);
            if (resultCode == Activity.RESULT_OK) {
                switch (requestCode) {
                    case PICK_SEGMENT_URI:
                        Uri uri = data.getData();
                        mUploadSegmentList.addView(new UploadSegmentViewHolder(getActivity(),
                                mLayoutInflator, uri, mUserLiveEvent).getRootView());
                        return;
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private static class UploadSegmentViewHolder {

        private final View mRootView, mViewCancel;
        private final UserLiveEvent mUserLiveEvent;
        private final Context mContext;
        private final TextView mViewStatus;
        private ProgressBar mUploadProgress;


        public UploadSegmentViewHolder(Context context, LayoutInflater inflater,
                                       Uri uri, UserLiveEvent userLiveEvent) {
            mContext = context;
            mUserLiveEvent = userLiveEvent;

            mRootView = inflater.inflate(R.layout.publish_live_event_from_file_segment_item, null, false);
            mRootView.setTag(this);
            ((TextView)mRootView.findViewById(R.id.content_uri)).setText(uri.toString());
            mViewCancel = mRootView.findViewById(R.id.cancel);
            mViewCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mDestroyed || null == mSegment) {
                        return;
                    }
                    mSegment.cancelUpload(null);

                }
            });

            mRootView.findViewById(R.id.remove).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mDestroyed) {
                        return;
                    }
                    ((ViewGroup)mRootView.getParent()).removeView(mRootView);
                    destroy();
                }
            });

            mViewStatus = (TextView)mRootView.findViewById(R.id.status);
            mUploadProgress = (ProgressBar)mRootView.findViewById(R.id.upload_progress);
            try {
                ParcelFileDescriptor pfd = mContext.getContentResolver().openFileDescriptor(uri, "r");
                mUserLiveEvent.uploadSegmentFromFD(pfd, mUploadCallback, null, null);
            } catch (Exception ex) {
                Resources res = mContext.getResources();
                String text = String.format(res.getString(R.string.failure_with_exception), ex.getMessage());
                mViewStatus.setText(text);

            }

        }

        private UserLiveEventSegment mSegment = null;

        private UserLiveEvent.Result.UploadSegment mUploadCallback = new UserLiveEvent.Result.UploadSegment() {

            @Override
            public void onSuccess(Object closure) {
                mSegment = null;
                if (!mDestroyed) {
                    mViewCancel.setEnabled(false);
                }
            }

            @Override
            public void onProgress(Object o, float progress, long l, long l1) {
                if (mDestroyed) {
                    return;
                }
                mUploadProgress.setProgress((int) progress);
            }

            @Override
            public void onProgress(Object o, long completed) {

            }


            @Override
            public void onFailure(Object closure, int status) {
                mSegment = null;
                if (!mDestroyed) {
                    Resources res = mContext.getResources();
                    String text = String.format(res.getString(R.string.failure_with_status), status);
                    mViewStatus.setText(text);
                    mViewCancel.setEnabled(false);
                }
            }

            @Override
            public void onCancelled(Object closure) {
                mSegment = null;
                mViewCancel.setEnabled(false);
            }

            @Override
            public void onException(Object closure, Exception ex) {
                mSegment = null;
                if (!mDestroyed) {
                    Resources res = mContext.getResources();
                    String text = String.format(res.getString(R.string.failure_with_exception), ex.getMessage());
                    mViewStatus.setText(text);
                    mViewCancel.setEnabled(false);
                }
            }

            @Override
            public void onSegmentIdAvailable(Object closure, UserLiveEventSegment segment) {
                mSegment = segment;
            }
        };

        private boolean mDestroyed;

        public void destroy() {
            mDestroyed = true;
            if (null != mSegment) {
                mSegment.cancelUpload(null);
                mSegment = null;
            }
        }

        public View getRootView() {
            return mRootView;
        }

    }

    private void removeAllSegmentUploadListViews() {
        for (int i = mUploadSegmentList.getChildCount() - 1; i >= 0; i -= 1) {
            View v = mUploadSegmentList.getChildAt(i);
            Object tag = v.getTag();
            if (tag instanceof UploadSegmentViewHolder) {
                UploadSegmentViewHolder viewHolder = (UploadSegmentViewHolder)tag;
                viewHolder.destroy();
            }
        }
        mUploadSegmentList.removeAllViews();
    }

    static PublishLiveEventFromFileFragment newFragment() {
        return new PublishLiveEventFromFileFragment();
    }


}

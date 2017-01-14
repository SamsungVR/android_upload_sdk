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
import android.widget.TextView;

import com.samsung.msca.samsungvr.sdk.User;
import com.samsung.msca.samsungvr.sdk.UserLiveEvent;
import com.samsung.msca.samsungvr.sdk.VR;

public class PublishLiveEventFragment extends BaseFragment {


    static final String TAG = Util.getLogTag(PublishLiveEventFragment.class);
    private static final boolean DEBUG = Util.DEBUG;

    private ViewGroup mSegmentStatus;
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
        View result = mLayoutInflator.inflate(R.layout.fragment_publish_live_event, null, false);

        mSegmentStatus = (ViewGroup)result.findViewById(R.id.segment_status);
        mIdView = (TextView)result.findViewById(R.id.live_event_id);

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
        mIdView = null;
        mSegmentStatus = null;
        mLayoutInflator = null;
        super.onDestroyView();
    }

    /*
    private static class LiveEventViewHolder implements View.OnClickListener {

        private final View mRootView;
        private final TextView mViewId, mViewTitle, mViewDescription, mViewPermission,
                    mViewProducerUrl, mViewStatus, mViewNumViewers, mViewStereoType, mViewState,
                    mViewStarted, mViewFinished, mViewSource;

        private final View mViewRefresh, mViewFinish, mViewEmail, mViewDelete;
        private final UserLiveEvent mLiveEvent;
        private final Context mContext;
        private final DateFormat mDateFormat;

        public LiveEventViewHolder(Context context, LayoutInflater inflater, DateFormat dateFormat, UserLiveEvent liveEvent) {
            mContext = context;
            mDateFormat = dateFormat;

            mLiveEvent = liveEvent;

            mRootView = inflater.inflate(R.layout.live_event_item, null, false);
            mRootView.setTag(this);

            mViewStatus = (TextView)mRootView.findViewById(R.id.status);

            mViewId = (TextView)mRootView.findViewById(R.id.event_id);
            mViewTitle = (TextView)mRootView.findViewById(R.id.title);
            mViewDescription = (TextView)mRootView.findViewById(R.id.description);
            mViewProducerUrl = (TextView)mRootView.findViewById(R.id.producer_url);
            mViewStereoType = (TextView)mRootView.findViewById(R.id.stereo_type);
            mViewSource = (TextView)mRootView.findViewById(R.id.source);
            mViewPermission = (TextView)mRootView.findViewById(R.id.permission);

            mViewState = (TextView)mRootView.findViewById(R.id.state);
            mViewNumViewers = (TextView)mRootView.findViewById(R.id.num_viewers);

            mViewStarted = (TextView)mRootView.findViewById(R.id.started);
            mViewFinished = (TextView)mRootView.findViewById(R.id.finished);

            mViewRefresh = mRootView.findViewById(R.id.refresh);
            mViewFinish = mRootView.findViewById(R.id.finish);
            mViewEmail = mRootView.findViewById(R.id.email);
            mViewDelete = mRootView.findViewById(R.id.delete);

            mViewId.setText(mLiveEvent.getId());
            mViewTitle.setText(mLiveEvent.getTitle());
            mViewDescription.setText(mLiveEvent.getDescription());

            if (mLiveEvent.getVideoStereoscopyType() == null) {
                Log.d(TAG, "mLiveEvent.getVideoStereoscopyType() is null");
            }

            mViewStereoType.setText(mLiveEvent.getVideoStereoscopyType().toString());

            String producerUrlTxt = mLiveEvent.getProducerUrl();
            if (null != producerUrlTxt) {
                mViewProducerUrl.setText(producerUrlTxt);
            } else {
                mViewProducerUrl.setText(R.string.pending_generation);
            }

            mViewState.setText(mLiveEvent.getState().toString());
            mViewPermission.setText((mLiveEvent.getPermission().toString()));
            mViewNumViewers.setText(mLiveEvent.getViewerCount().toString());
            mViewStarted.setText(mLiveEvent.getStartedTime().toString());
            mViewFinished.setText(mLiveEvent.getFinishedTime().toString());
            //mViewProtocol.setText(mLiveEvent.getProtocol().toString());

            mViewRefresh.setEnabled(true);
            mViewRefresh.setOnClickListener(this);

            mViewFinish.setEnabled(true);
            mViewFinish.setOnClickListener(this);

            mViewEmail.setEnabled(true);
            mViewEmail.setOnClickListener(this);

            mViewDelete.setEnabled(true);
            mViewDelete.setOnClickListener(this);
        }

        public void markAsDeleted() {
            mRootView.setBackgroundColor(ContextCompat.getColor(mContext, R.color.deleted_live_event_bg));
        }


        private final UserLiveEvent.Result.Finish mCallbackFinish = new UserLiveEvent.Result.Finish() {

            @Override
            public void onSuccess(Object closure) {
                mViewStatus.setText(R.string.success);
            }

            @Override
            public void onFailure(Object closure, int status) {
                Resources res = mContext.getResources();
                String text = String.format(res.getString(R.string.failure_with_status), status);
                mViewStatus.setText(text);
            }

            @Override
            public void onCancelled(Object o) {
            }

            @Override
            public void onException(Object o, Exception ex) {
                Resources res = mContext.getResources();
                String text = String.format(res.getString(R.string.failure_with_exception), ex.getMessage());
                mViewStatus.setText(text);
            }
        };

        private final UserLiveEvent.Result.DeleteLiveEvent mCallbackDeleteLiveEvent = new UserLiveEvent.Result.DeleteLiveEvent() {

            @Override
            public void onSuccess(Object closure) {
                mViewStatus.setText(R.string.success);
                markAsDeleted();
            }

            @Override
            public void onFailure(Object closure, int status) {
                Resources res = mContext.getResources();
                String text = String.format(res.getString(R.string.failure_with_status), status);
                mViewStatus.setText(text);
            }

            @Override
            public void onCancelled(Object o) {

            }

            @Override
            public void onException(Object o, Exception ex) {
                Resources res = mContext.getResources();
                String text = String.format(res.getString(R.string.failure_with_exception), ex.getMessage());
                mViewStatus.setText(text);
            }
        };




        private final UserLiveEvent.Result.QueryLiveEvent mCallbackRefreshLiveEvent = new UserLiveEvent.Result.QueryLiveEvent() {

            @Override
            public void onSuccess(Object closure) {
                mViewStatus.setText(R.string.success);
                mViewState.setText(mLiveEvent.getState().toString());
                mViewNumViewers.setText(mLiveEvent.getViewerCount().toString());

            }

            @Override
            public void onFailure(Object closure, int status) {
                Resources res = mContext.getResources();
                String text = String.format(res.getString(R.string.failure_with_status), status);
                mViewStatus.setText(text);
            }

            @Override
            public void onCancelled(Object o) {

            }

            @Override
            public void onException(Object o, Exception ex) {
                Resources res = mContext.getResources();
                String text = String.format(res.getString(R.string.failure_with_exception), ex.getMessage());
                mViewStatus.setText(text);
            }
        };


        @Override
        public void onClick(View v) {
            if (v == mViewDelete) {
                mLiveEvent.delete(mCallbackDeleteLiveEvent, null, null);
            } else if (v == mViewRefresh) {
                mLiveEvent.query(mCallbackRefreshLiveEvent, null, null);
            } else if (v == mViewFinish){
                mLiveEvent.finish(UserLiveEvent.FinishAction.ARCHIVE, mCallbackFinish, null, null);
            } else if (v == mViewEmail) {
                StringBuffer message = new StringBuffer();
                message.append("title:");
                message.append(mLiveEvent.getTitle());
                message.append("\n");
                message.append("ingest url: ");
                message.append(mLiveEvent.getProducerUrl());

                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Live event created");
                emailIntent.putExtra(Intent.EXTRA_TEXT, message.toString());
                mContext.startActivity(Intent.createChooser(emailIntent, null));
            }
        }

        public void destroy() {
        }

        public View getRootView() {
            return mRootView;
        }
    }

    */


    static PublishLiveEventFragment newFragment() {
        return new PublishLiveEventFragment();
    }


}

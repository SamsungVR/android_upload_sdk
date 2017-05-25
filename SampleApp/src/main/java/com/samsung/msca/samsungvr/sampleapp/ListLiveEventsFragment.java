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
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.samsung.msca.samsungvr.sdk.User;
import com.samsung.msca.samsungvr.sdk.UserLiveEvent;
import com.samsung.msca.samsungvr.sdk.VR;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

public class ListLiveEventsFragment extends BaseFragment {

    static final String TAG = Util.getLogTag(ListLiveEventsFragment.class);
    private static final boolean DEBUG = Util.DEBUG;

    private ViewGroup mListing;
    private TextView mStatus;
    private LayoutInflater mLayoutInflator;

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
        mLayoutInflator = LayoutInflater.from(getActivity());
        View result = mLayoutInflator.inflate(R.layout.fragment_list_live_events, null, false);

        View listAll = result.findViewById(R.id.list_all);
        listAll.setOnClickListener(mOnClickListener);

        mListing = (ViewGroup)result.findViewById(R.id.items);
        mStatus = (TextView)result.findViewById(R.id.status);

        mOnClickListener.onClick(listAll);
        return result;
    }

    @Override
    public void onDestroyView() {
        removeAllLiveEventViews();

        mStatus = null;
        mListing = null;
        mLayoutInflator = null;
        super.onDestroyView();
    }

    private final DateFormat mDateFormat = new SimpleDateFormat("MMM dd,yyyy  hh:mm a");

    private void removeAllLiveEventViews() {
        for (int i = mListing.getChildCount() - 1; i >= 0; i -= 1) {
            View v = mListing.getChildAt(i);
            Object tag = v.getTag();
            if (tag instanceof LiveEventViewHolder) {
                LiveEventViewHolder viewHolder = (LiveEventViewHolder)tag;
                viewHolder.destroy();
            }
        }
        mListing.removeAllViews();
    }

    private static class LiveEventViewHolder implements View.OnClickListener {

        private final View mRootView;
        private final TextView mViewId, mViewTitle, mViewDescription, mViewPermission,
                    mViewProducerUrl, mViewViewUrl, mViewStatus, mViewNumViewers, mViewStereoType, mViewState,
                    mViewStarted, mViewFinished, mViewSource, mViewTakedown;

        private final View mViewRefresh, mViewFinish, mViewEmail, mViewDelete, mViewStreamMP4TS,
            mViewStreamCAM;
        private final UserLiveEvent mLiveEvent;
        private final Context mContext;
        private final DateFormat mDateFormat;

        private static String asString(Object o) {
            if (null == o) {
                return "NULL";
            }
            return o.toString();
        }

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
            mViewViewUrl = (TextView)mRootView.findViewById(R.id.view_url);
            mViewStereoType = (TextView)mRootView.findViewById(R.id.stereo_type);
            mViewSource = (TextView)mRootView.findViewById(R.id.source);
            mViewPermission = (TextView)mRootView.findViewById(R.id.permission);

            mViewState = (TextView)mRootView.findViewById(R.id.state);
            mViewTakedown = (TextView)mRootView.findViewById(R.id.takedown);

            mViewNumViewers = (TextView)mRootView.findViewById(R.id.num_viewers);
            mViewStarted = (TextView)mRootView.findViewById(R.id.started);
            mViewFinished = (TextView)mRootView.findViewById(R.id.finished);

            mViewRefresh = mRootView.findViewById(R.id.refresh);
            mViewFinish = mRootView.findViewById(R.id.finish);
            mViewEmail = mRootView.findViewById(R.id.email);
            mViewDelete = mRootView.findViewById(R.id.delete);
            mViewStreamMP4TS = mRootView.findViewById(R.id.publish_from_file);
            mViewStreamCAM = mRootView.findViewById(R.id.publish_from_cam);

            mViewId.setText(mLiveEvent.getId());
            mViewTitle.setText(mLiveEvent.getTitle());
            mViewDescription.setText(mLiveEvent.getDescription());


            mViewStereoType.setText(asString(mLiveEvent.getVideoStereoscopyType()));
            mViewProducerUrl.setText(asString(mLiveEvent.getProducerUrl()));
            mViewViewUrl.setText(asString(mLiveEvent.getViewUrl()));

            mViewState.setText(asString(mLiveEvent.getState()));
            mViewTakedown.setText(asString(mLiveEvent.hasTakenDown()));

            mViewPermission.setText(asString(mLiveEvent.getPermission()));
            mViewNumViewers.setText(asString(mLiveEvent.getViewerCount()));
            mViewStarted.setText(asString(mLiveEvent.getStartedTime()));
            mViewFinished.setText(asString(mLiveEvent.getFinishedTime()));
            mViewSource.setText(asString(mLiveEvent.getSource()));

            mViewRefresh.setEnabled(true);
            mViewRefresh.setOnClickListener(this);

            mViewFinish.setEnabled(true);
            mViewFinish.setOnClickListener(this);

            mViewEmail.setEnabled(true);
            mViewEmail.setOnClickListener(this);

            mViewDelete.setEnabled(true);
            mViewDelete.setOnClickListener(this);

            mViewStreamMP4TS.setEnabled(true);
            mViewStreamMP4TS.setOnClickListener(this);

            mViewStreamCAM.setEnabled(true);
            mViewStreamCAM.setOnClickListener(this);
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
            public void onSuccess(Object o, UserLiveEvent userLiveEvent) {
                mViewStatus.setText(R.string.success);
                mViewState.setText(asString(mLiveEvent.getState()));
                mViewTakedown.setText(asString(mLiveEvent.hasTakenDown()));
                mViewNumViewers.setText(asString(mLiveEvent.getViewerCount()));
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
            if (DEBUG) {
                Log.d(TAG, "Received onClick: " + v);
            }
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
                message.append("\n");
                message.append("view url: ");
                message.append(mLiveEvent.getViewUrl());

                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Live event created");
                emailIntent.putExtra(Intent.EXTRA_TEXT, message.toString());
                mContext.startActivity(Intent.createChooser(emailIntent, null));
            } else if (v == mViewStreamMP4TS) {
                Bundle args = new Bundle();
                args.putString(LoggedInFragment.PARAM_USER, mLiveEvent.getUser().getUserId());
                args.putString(PublishLiveEventFromFileFragment.PARAM_LIVE_EVENT_ID, mLiveEvent.getId());
                Util.sendBroadcast(LocalBroadcastManager.getInstance(mContext),
                        LoggedInFragment.ACTION_PUBLISH_LIVE_EVENT_FROM_FILE_PAGE,
                        LoggedInFragment.EXTRA_PUBLISH_LIVE_EVENT_FROM_FILE_ARGS, args);
            } else if (v == mViewStreamCAM) {
                Bundle args = new Bundle();
                args.putString(LoggedInFragment.PARAM_USER, mLiveEvent.getUser().getUserId());
                args.putString(PublishLiveEventFromCamFragment.PARAM_LIVE_EVENT_ID, mLiveEvent.getId());
                Util.sendBroadcast(LocalBroadcastManager.getInstance(mContext),
                        LoggedInFragment.ACTION_PUBLISH_LIVE_EVENT_FROM_CAM_PAGE,
                        LoggedInFragment.EXTRA_PUBLISH_LIVE_EVENT_FROM_CAM_ARGS, args);

            }
        }

        public void destroy() {
        }

        public View getRootView() {
            return mRootView;
        }
    }



    private final User.Result.QueryLiveEvents mCallback = new User.Result.QueryLiveEvents() {

        @Override
        public void onException(Object closure, Exception ex) {
            if (hasValidViews()) {
                Resources res = getResources();
                String text = String.format(res.getString(R.string.failure_with_exception), ex.getMessage());
                mStatus.setText(text);
            }
        }

        @Override
        public void onSuccess(Object closure, List<UserLiveEvent> events) {
            if (DEBUG) {
                Log.d(TAG, "onSuccess events: " + events);
            }
            if (hasValidViews()) {
                Context context = getActivity();
                mStatus.setText(R.string.success);
                removeAllLiveEventViews();
                for (int i = 0; i < events.size(); i += 1) {
                    LiveEventViewHolder viewHolder = new LiveEventViewHolder(context,
                            mLayoutInflator, mDateFormat, events.get(i));
                    mListing.addView(viewHolder.getRootView());
                }
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
    };

    View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (null == mUser) {
                return;
            }
            mStatus.setText(R.string.in_progress);
            mUser.queryLiveEvents(mCallback, null, null);
        }
    };

}

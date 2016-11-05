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
import android.widget.TextView;
import android.widget.Toast;

import com.samsung.msca.samsungvr.sdk.User;
import com.samsung.msca.samsungvr.sdk.UserLiveEvent;
import com.samsung.msca.samsungvr.sdk.VR;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ListLiveEventsFragment extends BaseFragment {


    static final String TAG = Util.getLogTag(ListLiveEventsFragment.class);
    private static final boolean DEBUG = Util.DEBUG;

    private Button mListAll;
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

        mListAll = (Button)result.findViewById(R.id.list_all);
        mListing = (ViewGroup)result.findViewById(R.id.items);
        mStatus = (TextView)result.findViewById(R.id.status);

        mListAll.setOnClickListener(mOnClickListener);

        mOnClickListener.onClick(mListAll);

        return result;
    }

    @Override
    public void onDestroyView() {
        removeAllLiveEventViews();

        mListAll.setOnClickListener(null);
        mListAll = null;
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
        private final TextView mViewId, mViewTitle, mViewDescription,
                    mViewProducerUrl, mViewStatus, mViewStereoType;

        private final View mViewWatch2d, mViewWatch3d, mViewRefresh, mViewDelete;
        private final UserLiveEvent mLiveEvent;
        private final Context mContext;
        private final DateFormat mDateFormat;

        public LiveEventViewHolder(Context context, LayoutInflater inflater, DateFormat dateFormat, UserLiveEvent liveEvent) {
            mContext = context;
            mDateFormat = dateFormat;

            mLiveEvent = liveEvent;

            mRootView = inflater.inflate(R.layout.live_event_item, null, false);
            mRootView.setTag(this);

            mViewId = (TextView)mRootView.findViewById(R.id.event_id);
            mViewTitle = (TextView)mRootView.findViewById(R.id.title);
            mViewDescription = (TextView)mRootView.findViewById(R.id.description);
            mViewProducerUrl = (TextView)mRootView.findViewById(R.id.producer_url);
            mViewStatus = (TextView)mRootView.findViewById(R.id.status);
            mViewStereoType = (TextView)mRootView.findViewById(R.id.stereo_type);

            mViewWatch2d = mRootView.findViewById(R.id.watch2d);
            mViewWatch3d = mRootView.findViewById(R.id.watch3d);
            mViewRefresh = mRootView.findViewById(R.id.refresh);
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

            mViewWatch2d.setOnClickListener(this);
            mViewWatch3d.setOnClickListener(this);

            mViewRefresh.setEnabled(true);
            mViewRefresh.setOnClickListener(this);
            mViewDelete.setEnabled(true);
            mViewDelete.setOnClickListener(this);
        }

        public void markAsDeleted() {
            mRootView.setBackgroundColor(ContextCompat.getColor(mContext, R.color.deleted_live_event_bg));
        }

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
            if (v == mViewWatch2d) {
                watchLiveEventIn2DApp();
            } else if (v == mViewWatch3d) {
                watchLiveEventIn3DApp();
            } else if (v == mViewDelete) {
                mLiveEvent.delete(mCallbackDeleteLiveEvent, null, null);
            } else if (v == mViewRefresh) {
                mLiveEvent.query(mCallbackRefreshLiveEvent, null, null);
            }
        }

        private void watchLiveEventInExternalApp() {
            String consumerUrl = mLiveEvent.getConsumerUrl();
            Uri uri = Uri.parse(consumerUrl);

            /*
             * Example
             * adb shell am start -a android.intent.action.VIEW -t "video/*" -d "http://devimages.apple.com/iphone/samples/bipbop/bipbopall.m3u8"
             * More restrictive (VLC player) adb shell am start -a android.intent.action.VIEW
             *              -t "application/x-mpegURL" -d "http://devimages.apple.com/iphone/samples/bipbop/bipbopall.m3u8"
             */
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "video/*");

            Resources res = mContext.getResources();
            String chooserTitle = res.getString(R.string.watch_using);
            Intent chooser = Intent.createChooser(intent, chooserTitle);
            if (intent.resolveActivity(mContext.getPackageManager()) != null) {
                mContext.startActivity(chooser);
            } else {
                Toast.makeText(mContext, R.string.no_player_for_watch, Toast.LENGTH_SHORT).show();
            }

        }

        private void watchLiveEventIn2DApp() {
            String consumerUrl = mLiveEvent.getConsumerUrl();
            String title = mLiveEvent.getTitle();

            String milkVRUrl = String.format("samsungvr360://sideload/?url=%s&title=%s",
                    consumerUrl, title);
            Uri uriObj = Uri.parse(milkVRUrl);

            Intent intent = new Intent();
            intent.setData(uriObj);
            try {
                mContext.startActivity(intent);
            } catch (Exception ex) {
                Toast.makeText(mContext, R.string.no_player_for_watch, Toast.LENGTH_SHORT).show();
            }
        }

        private void watchLiveEventIn3DApp() {
            String consumerUrl = mLiveEvent.getConsumerUrl();
            String title = mLiveEvent.getTitle();


            UserLiveEvent.VideoStereoscopyType videoStereoscopyType = mLiveEvent.getVideoStereoscopyType();
            String video_type = "";
            switch (videoStereoscopyType) {
                case TOP_BOTTOM_STEREOSCOPIC:
                    video_type = "&video_type=3dv";
                    break;
                case LEFT_RIGHT_STEREOSCOPIC:
                    video_type = "&video_type=3dh";
                    break;
                default:
                    video_type = "";
            }

            String milkVRUrl = String.format("milkvr://sideload/?url=%s&title=%s%s",
                    consumerUrl, title, video_type);
            Uri uriObj = Uri.parse(milkVRUrl);

            Intent intent = new Intent();
            intent.setData(uriObj);
            try {
                mContext.startActivity(intent);
            } catch (Exception ex) {
                Toast.makeText(mContext, R.string.no_player_for_watch, Toast.LENGTH_SHORT).show();
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

    static ListLiveEventsFragment newFragment() {
        return new ListLiveEventsFragment();
    }


}

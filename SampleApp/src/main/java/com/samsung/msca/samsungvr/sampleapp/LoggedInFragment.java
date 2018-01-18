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

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.samsung.msca.samsungvr.sdk.User;
import com.samsung.msca.samsungvr.sdk.VR;

public class LoggedInFragment extends BaseFragment {

    static final String TAG = Util.getLogTag(LoggedInFragment.class);
    private static final boolean DEBUG = Util.DEBUG;

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private User mUser;
    private FragmentManager mFragmentManager;


    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    private final BroadcastReceiver mLocalBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Fragment fragment = null;
            String tag = null;
            if (DEBUG) {
                Log.d(TAG, "Receive intent: " + intent);
            }
            /*
             * If you add something here, *DO NOT FORGET* to add it to the intent filter in onCreate
             */
            if (ACTION_PUBLISH_LIVE_EVENT_FROM_FILE_PAGE.equals(action)) {
                tag = PublishLiveEventFromFileFragment.TAG;
                fragment = mFragmentManager.findFragmentByTag(tag);
                if (null == fragment) {
                    fragment = PublishLiveEventFromFileFragment.newFragment();
                }
                fragment.setArguments(intent.getBundleExtra(EXTRA_PUBLISH_LIVE_EVENT_FROM_FILE_ARGS));
            }

            if (null != fragment & null != tag) {
                FragmentTransaction ft = mFragmentManager.beginTransaction();
                ft.replace(R.id.content_frame, fragment, tag);
                ft.commitAllowingStateLoss();
                mDrawerList.setItemChecked(mCurrentSelection, false);
                mCurrentSelection = -1;
            }
        }
    };

    private int mCurrentSelection = -1;

    private void selectItem(int position) {
        Log.d(TAG, "Handle selection: " + position + " current: " + mCurrentSelection);
        if (null == mUser || position == mCurrentSelection) {
            return;
        }
        mCurrentSelection = position;
        mDrawerList.setItemChecked(position, true);
        mDrawerLayout.closeDrawer(mDrawerList);

        Bundle args = new Bundle();
        Class<?> fragmentClass = null;
        switch (position) {

            case 1: /* Create live event */

                fragmentClass = CreateLiveEventFragment.class;
                break;

            case 2: /* List live events */

                fragmentClass = ListLiveEventsFragment.class;
                break;

            case 3: /* Upload video */

                fragmentClass = UploadVideoFragment.class;
                break;

            case 4: /* Get user by session id */

                fragmentClass = GetUserBySessionInfoFragment.class;
                break;

            case 5: /* Logout */

                Util.sendBroadcast(mLocalBroadcastManager, Util.ACTION_LOGOUT, null, null);
                Util.showLoginPage(mLocalBroadcastManager);
                return;

        }
        Log.d(TAG, "Current logged in fragment class: " + fragmentClass + " user: " + mUser);
        if (null != args) {
            args.putString(PARAM_USER, mUser.getUserId());
        }

        if (null != fragmentClass) {
            String tag = fragmentClass.getName();

            Fragment fragment = mFragmentManager.findFragmentByTag(tag);
            if (null == fragment) {
                try {
                    fragment = (Fragment)fragmentClass.newInstance();
                    if (null != args) {
                        fragment.setArguments(args);
                    }
                } catch (java.lang.InstantiationException ex) {
                    fragment = null;
                } catch (IllegalAccessException ex) {
                }
            } else {
                if (null != args) {
                    fragment.getArguments().putAll(args);
                }
            }
            if (null != fragment) {
                FragmentTransaction ft = mFragmentManager.beginTransaction();
                mActiveFragment = fragment;
                ft.replace(R.id.content_frame, mActiveFragment, tag);
                ft.commitAllowingStateLoss();
            }
            Log.d(TAG, "Current logged in fragment: " + fragment + " tag: " + tag + " user: " + mUser);
        }
    }

    private Fragment mActiveFragment;

    static final String ACTION_PUBLISH_LIVE_EVENT_FROM_FILE_PAGE = BuildConfig.APPLICATION_ID + ".publishLiveEventFromFile";
    static final String EXTRA_PUBLISH_LIVE_EVENT_FROM_FILE_ARGS = BuildConfig.APPLICATION_ID + ".publishLiveEventFromFile.args";

    static final String EXTRA_PUBLISH_LIVE_EVENT_FROM_CAM_ARGS = BuildConfig.APPLICATION_ID + ".publishLiveEventFromCam.args";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActiveFragment = null;
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(getActivity());
        mFragmentManager = getFragmentManager();
        Bundle bundle = getArguments();
        if (null != bundle) {
            String userId = bundle.getString(PARAM_USER);
            if (null != userId) {
                mUser = VR.getUserById(userId);
            }
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_PUBLISH_LIVE_EVENT_FROM_FILE_PAGE);

        mLocalBroadcastManager.registerReceiver(mLocalBroadcastReceiver, filter);

        if (null == mUser) {
            Util.showLoginPage(mLocalBroadcastManager);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View result = inflater.inflate(R.layout.fragment_logged_in, null, false);

        mDrawerLayout = (DrawerLayout)result.findViewById(R.id.drawer_layout);
        mDrawerList = (ListView)result.findViewById(R.id.left_drawer);

        View headerRoot = inflater.inflate(R.layout.logged_in_drawer_list_header, null, false);
        TextView headerName = (TextView)headerRoot.findViewById(R.id.header_name);
        TextView headerEmail = (TextView)headerRoot.findViewById(R.id.header_email);
        TextView headerCredits = (TextView)headerRoot.findViewById(R.id.header_credits);
        SimpleNetworkImageView headerProfilePicLight = (SimpleNetworkImageView)headerRoot.findViewById(R.id.header_profile_pic_light);
        SimpleNetworkImageView headerProfilePicDark = (SimpleNetworkImageView)headerRoot.findViewById(R.id.header_profile_pic_dark);

        headerName.setText(mUser.getName());
        headerEmail.setText(mUser.getEmail());

        if (mUser.getUploadCredits() < 0 ) {
            headerCredits.setText("Unlimited uploads");
        }
        else if (mUser.getUploadCredits() == 0 ) {
            headerCredits.setText("No uploads left");
        }
        else {
            headerCredits.setText(mUser.getUploadCredits() + " uploads left");
        }
        headerProfilePicDark.setImageUrl(mUser.getProfilePicDarkUrl());
        headerProfilePicLight.setImageUrl(mUser.getProfilePicLightUrl());

        mDrawerList.addHeaderView(headerRoot);

        mDrawerList.setAdapter(new ArrayAdapter<>(getActivity(), R.layout.logged_in_drawer_list_item,
                getResources().getStringArray(R.array.user_options)));

        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
        mDrawerLayout.openDrawer(Gravity.LEFT);

        return result;
    }

    @Override
    public void onDestroyView() {

        mDrawerLayout = null;
        mDrawerList = null;
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (null != mActiveFragment && !getActivity().isDestroyed()) {
            FragmentManager manager = getFragmentManager();
            FragmentTransaction transaction = manager.beginTransaction();
            transaction.remove(mActiveFragment);
            transaction.commitAllowingStateLoss();
        }
        mActiveFragment = null;
        mLocalBroadcastManager.unregisterReceiver(mLocalBroadcastReceiver);
    }

    static final String PARAM_USER = "paramUser";

}

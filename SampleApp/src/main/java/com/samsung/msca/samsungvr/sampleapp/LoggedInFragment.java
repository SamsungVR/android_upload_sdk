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
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
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

    private int mCurrentSelection = -1;

    private void selectItem(int position) {
        if (null == mUser || position == mCurrentSelection) {
            return;
        }
        mCurrentSelection = position;
        mDrawerList.setItemChecked(position, true);
        mDrawerLayout.closeDrawer(mDrawerList);

        Bundle args = null;
        FragmentTransaction ft = mFragmentManager.beginTransaction();
        Fragment fragment = null;
        String tag = null;

        switch (position) {
            case 1: /* Create live event */

                tag = CreateLiveEventFragment.TAG;
                fragment = mFragmentManager.findFragmentByTag(tag);
                if (null == fragment) {
                    fragment = CreateLiveEventFragment.newFragment();
                }
                args = new Bundle();
                args.putString(PARAM_USER, mUser.getUserId());
                break;

            case 2: /* List live events */

                tag = ListLiveEventsFragment.TAG;
                fragment = mFragmentManager.findFragmentByTag(tag);
                if (null == fragment) {
                    fragment = ListLiveEventsFragment.newFragment();
                }
                args = new Bundle();
                args.putString(PARAM_USER, mUser.getUserId());
                break;

            case 3: /* Upload video */

                tag = UploadVideoFragment.TAG;
                fragment = mFragmentManager.findFragmentByTag(tag);
                if (null == fragment) {
                    fragment = UploadVideoFragment.newFragment();
                }
                args = new Bundle();
                args.putString(PARAM_USER, mUser.getUserId());
                break;

            case 4: /* Get user by session id */

                tag = GetUserBySessionInfoFragment.TAG;
                fragment = mFragmentManager.findFragmentByTag(tag);
                if (null == fragment) {
                    fragment = GetUserBySessionInfoFragment.newFragment();
                }
                break;

            case 5: /* Logout */

                Context ctx = getActivity().getApplicationContext();
                SharedPreferences sharedPref = ctx.getSharedPreferences("Sample2016", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.clear();
                editor.commit();

                Util.showLoginPage(mLocalBroadcastManager);
                break;


        }
        if (null != fragment && !fragment.isVisible()) {
            if (null != args) {
                if (fragment.getArguments() == null) {
                    fragment.setArguments(args);
                }
                else {
                    fragment.getArguments().putAll(args);
                }
            }
            ft.replace(R.id.content_frame, fragment, tag);
            ft.commitAllowingStateLoss();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFragmentManager = getFragmentManager();
        Bundle bundle = getArguments();
        if (null != bundle) {
            String userId = bundle.getString(PARAM_USER);
            if (null != userId) {
                mUser = VR.getUserById(userId);
            }
        }

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
        SimpleNetworkImageView headerProfilePic = (SimpleNetworkImageView)headerRoot.findViewById(R.id.header_profile_pic);

        headerName.setText(mUser.getName());
        headerEmail.setText(mUser.getEmail());
        headerProfilePic.setImageUrl(mUser.getProfilePicUrl());

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

    static LoggedInFragment newFragment() {
        return new LoggedInFragment();
    }

    static final String PARAM_USER = "paramUser";

}

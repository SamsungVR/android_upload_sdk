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
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import com.samsung.msca.samsungvr.sdk.VR;

public class MainActivity extends Activity {

    private static final String TAG = Util.getLogTag(MainActivity.class);

    private LocalBroadcastManager mLocalBroadcastManager;
    private FragmentManager mFragmentManager;

    private final BroadcastReceiver mLocalBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Util.ACTION_SHOW_LOGIN_PAGE.equals(action)) {
                FragmentTransaction ft = mFragmentManager.beginTransaction();
                Fragment fragment = mFragmentManager.findFragmentByTag(LoginFragment.TAG);
                if (null == fragment) {
                    fragment = LoginFragment.newFragment();
                }
                ft.replace(android.R.id.content, fragment, LoginFragment.TAG);
                ft.commitAllowingStateLoss();
            } else if (Util.ACTION_SHOW_LOGGED_IN_PAGE.equals(action)) {
                FragmentTransaction ft = mFragmentManager.beginTransaction();
                Fragment fragment = mFragmentManager.findFragmentByTag(LoggedInFragment.TAG);
                if (null == fragment) {
                    fragment = LoggedInFragment.newFragment();
                }
                fragment.setArguments(intent.getBundleExtra(Util.EXTRA_SHOW_LOGGED_IN_PAGE_ARGS));
                ft.replace(android.R.id.content, fragment, LoggedInFragment.TAG);
                ft.commitAllowingStateLoss();
            } else if (Util.ACTION_SHOW_CREATE_ACCOUNT_PAGE.equals(action)) {
                FragmentTransaction ft = mFragmentManager.beginTransaction();
                Fragment fragment = mFragmentManager.findFragmentByTag(NewUserFragment.TAG);
                if (null == fragment) {
                    fragment = NewUserFragment.newFragment();
                }
                ft.add(android.R.id.content, fragment, NewUserFragment.TAG);
                ft.addToBackStack(null);
                ft.commitAllowingStateLoss();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        mFragmentManager = getFragmentManager();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Util.ACTION_SHOW_LOGIN_PAGE);
        filter.addAction(Util.ACTION_SHOW_LOGGED_IN_PAGE);
        filter.addAction(Util.ACTION_SHOW_CREATE_ACCOUNT_PAGE);

        mLocalBroadcastManager.registerReceiver(mLocalBroadcastReceiver, filter);

        setContentView(R.layout.activity_main);

        if (null != savedInstanceState) {
            return;
        }

        FragmentTransaction ft = mFragmentManager.beginTransaction();

        ft.add(android.R.id.content, LoginFragment.newFragment(), LoginFragment.TAG);
        ft.commitAllowingStateLoss();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLocalBroadcastManager.unregisterReceiver(mLocalBroadcastReceiver);
    }
}

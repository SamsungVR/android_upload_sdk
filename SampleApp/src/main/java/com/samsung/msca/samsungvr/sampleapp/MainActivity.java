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
import android.util.Log;

import com.samsung.msca.samsungvr.ui.UILib;

import static com.samsung.msca.samsungvr.sampleapp.Util.ACTION_SHOW_LOGIN_PAGE;

public class MainActivity extends Activity {

    private LocalBroadcastManager mLocalBroadcastManager;
    private FragmentManager mFragmentManager;

    private static final Class<?> sLoginFragmentClass = LoginUILibFragment.class;

    private final BroadcastReceiver mLocalBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            handleBroadcastAction(intent.getAction(), intent);
        }
    };

    private static final boolean DEBUG = Util.DEBUG;
    private static final String TAG = Util.getLogTag(MainActivity.class);

    private void handleBroadcastAction(String action, Intent intent) {
        Bundle args = null;
        Class<?> fragmentClass = null;

        if (DEBUG) {
            Log.d(TAG, "Mainactivity received broadcast: " + action + " " + intent);
        }
        if (ACTION_SHOW_LOGIN_PAGE.equals(action)) {
            fragmentClass = sLoginFragmentClass;
        } else if (Util.ACTION_SHOW_LOGGED_IN_PAGE.equals(action) && null != intent) {
            fragmentClass = LoggedInFragment.class;
            args = intent.getBundleExtra(Util.EXTRA_SHOW_LOGGED_IN_PAGE_ARGS);
        } else if (Util.ACTION_LOGOUT.equals(action)) {
            if (sLoginFragmentClass == LoginUILibFragment.class) {
                UILib.logout();
            }
            return;
        }
        if (null != fragmentClass) {
            FragmentTransaction ft = mFragmentManager.beginTransaction();
            Fragment fragment = mFragmentManager.findFragmentByTag(fragmentClass.getName());
            if (null == fragment) {
                try {
                    fragment = (Fragment) fragmentClass.newInstance();
                } catch (InstantiationException ex) {
                    return;
                } catch (IllegalAccessException ex) {
                    return;
                }
            }
            if (null != args) {
                fragment.setArguments(args);
            }
            if (DEBUG) {
                Log.d(TAG, "Replacing content fragment with: " + fragment);
            }
            ft.replace(android.R.id.content, fragment, fragmentClass.getName());
            ft.commitAllowingStateLoss();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEBUG) {
            Log.d(TAG, "onCreate");
        }

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        mFragmentManager = getFragmentManager();

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_SHOW_LOGIN_PAGE);
        filter.addAction(Util.ACTION_SHOW_LOGGED_IN_PAGE);
        filter.addAction(Util.ACTION_LOGOUT);

        mLocalBroadcastManager.registerReceiver(mLocalBroadcastReceiver, filter);

        setContentView(R.layout.activity_main);

        if (null != savedInstanceState) {
            return;
        }
        handleBroadcastAction(ACTION_SHOW_LOGIN_PAGE, null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (DEBUG) {
            Log.d(TAG, "onDestroy");
        }
        mLocalBroadcastManager.unregisterReceiver(mLocalBroadcastReceiver);
    }
}

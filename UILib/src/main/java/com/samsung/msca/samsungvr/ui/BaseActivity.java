package com.samsung.msca.samsungvr.ui;

import android.app.Activity;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.Toast;

public class BaseActivity extends Activity {

    protected final boolean DEBUG = UILib.DEBUG;
    private static final String TAG = UILib.getLogTag(BaseActivity.class);

    public boolean canHandleEvent() {
        return !isDestroyed() && !isFinishing();
    }

    private boolean baseActivityResumed;

    @Override
    protected void onResume() {
        super.onResume();
        baseActivityResumed = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        baseActivityResumed = false;
    }

    protected boolean canHandleForegroundEvent() {
        return canHandleEvent() && baseActivityResumed;
    }

    protected boolean canReachSamsungVRService(boolean showAppUpdateDialog, boolean showUnreachableToast) {
        return true;
    }

    protected ProgressBar progressBar;
}

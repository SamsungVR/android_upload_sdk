package com.samsung.msca.samsungvr.ui;


import android.util.Log;

import com.samsung.dallas.salib.SamsungSSO;
import com.samsung.msca.samsungvr.sdk.Observable;
import com.samsung.msca.samsungvr.sdk.User;

class Bus extends Observable.BaseImpl<Bus.Callback> {

    private static final String TAG = UILib.getLogTag(Bus.class);
    private static final boolean DEBUG = UILib.DEBUG;

    public static class Callback {

        public void onSamsungSsoStatusEvent(SamsungSsoStatusEvent event) {
        }

        public void onLoginErrorEvent(LoginErrorEvent event) {
        }

        public void onLoggedOutEvent(LoggedOutEvent event) {
        }

        public void onLoggedInEvent(LoggedInEvent event) {
        }

        public void onInitEvent(InitEvent event) {
        }

        public void onRequestKillActivities(KillActivitiesEvent event) {
        }

        public void onSignInActivityDestroyed(SignInActivityDestroyed event) {
        }

    }

    abstract static class BusEvent {

        abstract boolean dispatch(Callback callback);
    }

    abstract static class EventWithCutoffTimestamp extends BusEvent {

        final long mCutoffTimestamp;
        final UILib mUILib;

        EventWithCutoffTimestamp(UILib uiLib, long cutoffTimestamp) {
            mCutoffTimestamp = cutoffTimestamp;
            mUILib = uiLib;
        }

        @Override
        boolean dispatch(Callback callback) {
            long currentCutoffTimestamp = mUILib.getCutoffTimestampNoLock();
            if (mCutoffTimestamp < currentCutoffTimestamp) {
                Log.d(TAG, "Not dispatching stale event: " + this +
                        " current: " + currentCutoffTimestamp + " mine: " + mCutoffTimestamp);
                return false;
            }
            onDispatch(callback);
            return true;
        }

        abstract void onDispatch(Callback callback);
    }

    public static class InitEvent extends EventWithCutoffTimestamp {

        InitEvent(UILib uiLib, long cutoffTimestamp) {
            super(uiLib, cutoffTimestamp);
        }

        @Override
        void onDispatch(Callback callback) {
            callback.onInitEvent(this);
        }
    }

    static class SamsungSsoStatusEvent extends EventWithCutoffTimestamp {

        final SamsungSSO.Status mStatus;

        public SamsungSsoStatusEvent(UILib uiLib, long cutoffTimestamp, SamsungSSO.Status status) {
            super(uiLib, cutoffTimestamp);
            mStatus = status;
        }

        @Override
        void onDispatch(Callback callback) {
            callback.onSamsungSsoStatusEvent(this);
        }
    }

    public static class KillActivitiesEvent extends BusEvent {

        @Override
        boolean dispatch(Callback callback) {
            callback.onRequestKillActivities(this);
            return true;
        }
    }

    public static class SignInActivityDestroyed extends EventWithCutoffTimestamp {

        SignInActivityDestroyed(UILib uiLib, long cutoffTimestamp) {
            super(uiLib, cutoffTimestamp);
        }

        @Override
        void onDispatch(Callback callback) {
            callback.onSignInActivityDestroyed(this);
        }
    }


    static class LoginErrorEvent extends EventWithCutoffTimestamp {

        final String mMessage;

        @Override
        void onDispatch(Callback callback) {
            callback.onLoginErrorEvent(this);
        }

        LoginErrorEvent(UILib uiLib, long cutoffTimestamp, String message) {
            super(uiLib, cutoffTimestamp);
            mMessage = message;
        }

    }

    static class LoggedOutEvent extends EventWithCutoffTimestamp {

        LoggedOutEvent(UILib uiLib, long cutoffTimestamp) {
            super(uiLib, cutoffTimestamp);
        }

        @Override
        void onDispatch(Callback callback) {
            callback.onLoggedOutEvent(this);
        }
    }

    static class LoggedInEvent extends EventWithCutoffTimestamp {

        final User mVrLibUser;

        @Override
        void onDispatch(Callback callback) {
            callback.onLoggedInEvent(this);
        }

        LoggedInEvent(UILib uiLib, long cutoffTimestamp, User user) {
            super(uiLib, cutoffTimestamp);
            mVrLibUser = user;
        }
    }

    private static Bus sBus;

    static Bus getEventBus() {
        if (null == sBus) {
            sBus = new Bus();
        }
        return sBus;
    }

    private Bus() {
    }


    public void post(final Callback caller, final BusEvent event) {
        if (DEBUG) {
            Log.d(TAG, "post: " + event + " caller " + caller);
        }
        iterate(new Observable.IterationObserver<Callback>() {
            @Override
            public boolean onIterate(final Observable.Block<Callback> block, Object... closure) {
                block.getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        Callback callback = block.getCallback();
                        if (caller != callback && hasObserver(callback)) {
                            if (event.dispatch(callback)) {
                                if (DEBUG) {
                                    Log.d(TAG, "dispatched " + event + " to " + callback + " from caller " + caller);
                                }
                            }
                        }
                    }
                });
                return true;
            }
        });
    }
}

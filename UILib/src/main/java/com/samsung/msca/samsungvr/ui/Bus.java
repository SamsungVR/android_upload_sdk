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

        public void onVRLibReadyEvent(VRLibReadyEvent event) {
        }

        public void onCreatedVrAcct(CreatedVrAccountEvent event) {
        }

        public void onRequestKillActivities(KillActivitiesEvent event) {
        }
    }

    public abstract static class BusEvent {

        abstract void dispatch(Callback callback);
    }

    public static class VRLibReadyEvent extends BusEvent {

        public final UILib mUILib;

        VRLibReadyEvent(UILib lib) {
            mUILib = lib;
        }

        @Override
        void dispatch(Callback callback) {
            callback.onVRLibReadyEvent(this);
        }
    }

    public static class SamsungSsoStatusEvent extends BusEvent {

        @Override
        void dispatch(Callback callback) {
            callback.onSamsungSsoStatusEvent(this);
        }

        public final SamsungSSO.Status mStatus;

        public SamsungSsoStatusEvent(SamsungSSO.Status status) {
            mStatus = status;
        }
    }

    public static class KillActivitiesEvent extends BusEvent {

        @Override
        void dispatch(Callback callback) {
            callback.onRequestKillActivities(this);
        }
    }


    public static class CreatedVrAccountEvent extends BusEvent {
        public final boolean mSuccess;
        public final CreateVrAcctStatus mStatus;

        public CreatedVrAccountEvent(final boolean success, final CreateVrAcctStatus status) {
            mSuccess = success;
            mStatus = status;
        }

        @Override
        void dispatch(Callback callback) {
            callback.onCreatedVrAcct(this);
        }
    }

    public static class LoginErrorEvent extends BusEvent {

        private final String message;

        @Override
        void dispatch(Callback callback) {
            callback.onLoginErrorEvent(this);
        }

        public LoginErrorEvent(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class LoggedOutEvent extends BusEvent {

        @Override
        void dispatch(Callback callback) {
            callback.onLoggedOutEvent(this);
        }
    }

    public static class LoggedInEvent extends BusEvent {
        public final User mVrLibUser;

        @Override
        void dispatch(Callback callback) {
            callback.onLoggedInEvent(this);
        }

        public LoggedInEvent(User user) {
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


    public void post(final BusEvent event) {
        if (DEBUG) {
            Log.d(TAG, "post: " + event);
        }
        iterate(new Observable.IterationObserver<Callback>() {
            @Override
            public boolean onIterate(final Observable.Block<Callback> block, Object... closure) {
                block.getHandler().post(new Runnable() {
                    @Override
                    public void run() {
                        Callback callback = block.getCallback();
                        if (hasObserver(callback)) {
                            if (DEBUG) {
                                Log.d(TAG, "dispatching " + event + " to " + callback);
                            }
                            event.dispatch(callback);
                        }
                    }
                });
                return true;
            }
        });
    }
}

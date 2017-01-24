package com.samsung.msca.samsungvr.ui;


import com.samsung.dallas.salib.SamsungSSO;
import com.samsung.msca.samsungvr.sdk.User;

class Bus extends Observable.BaseImpl<Bus.Callback> {

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
    }

    public abstract static class BusEvent {

        abstract void dispatch(Callback callback);
    }

    public static class VRLibReadyEvent extends BusEvent {
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
        iterate(new Observable.IterationObserver<Callback>() {
            @Override
            public boolean onIterate(final Observable.Block<Callback> block, Object... closure) {
                block.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (hasObserver(block.mCallback)) {
                            event.dispatch(block.mCallback);
                        }
                    }
                });
                return true;
            }
        });
    }
}

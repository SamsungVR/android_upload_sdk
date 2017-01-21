package com.samsung.msca.samsungvr.sdk;

public class ReactionsImpl implements Reactions{

    long mScared = 0L;
    long mWow = 0L;
    long mSad = 0L;
    long mSick = 0L;
    long mAngry = 0L;
    long mHappy = 0L;

    public void setScared(long num) {
        mScared = num;
    }

    public long getScared() {
        return mScared;
    }

    public void setWow(long num) {
        mWow = num;
    }

    public long getWow() {
        return mWow;
    }

    public void setSad(long num) {
        mSad = num;
    }

    public long getSad() {
        return mSad;
    }

    public void setSick(long num) {
        mSick = num;
    }
    public long getSick() {
        return mSick;
    }

    public void setAngry(long num) {
        mAngry = num;
    }

    public long getAngry() {
        return mAngry;
    }

    public void setHappy(long num) {
        mHappy = num;
    }

    public long getHappy() {
        return mHappy;
    }
}

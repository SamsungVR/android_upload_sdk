package com.samsung.msca.samsungvr.ui;

import android.util.SparseArray;

enum CreateVrAcctStatus {
    UNKNOWN(0, R.string.err_unknown),
    MISSING_NAME_EMAIL_OR_PASSWORD(1, R.string.err_name_email_or_pwd_missing),
    NAME_TOO_SHORT_LESS_THAN_3_CHARS(2, R.string.err_name_too_short),
    PASSWORD_TOO_WEAK(3, R.string.err_weak_password),
    EMAIL_BAD_FORM(4, R.string.err_email_bad_form),
    PASSWORD_CANNOT_CONTAIN_EMAIL(5, R.string.err_password_cannot_contain_email),
    PASSWORD_CANNOT_CONTAIN_USERNAME(6, R.string.err_password_cannot_contain_username),
    USER_WITH_EMAIL_ALREADY_EXISTS(7, R.string.err_email_in_use);

    private int status;
    private int cannedMsgStrId;

    CreateVrAcctStatus(final int status, final int msgResId) {
        this.status = status;
        this.cannedMsgStrId = msgResId;

    }

    private static final SparseArray<CreateVrAcctStatus> statusMap = new SparseArray<CreateVrAcctStatus>();

    static {
        for (CreateVrAcctStatus type : CreateVrAcctStatus.values()) {
            statusMap.put(type.status, type);
        }
    }

    public static CreateVrAcctStatus getStatus(final int i) {
        return statusMap.get(i, CreateVrAcctStatus.UNKNOWN);
    }


    public int getCannedMsgId() {
        return cannedMsgStrId;
    }
}
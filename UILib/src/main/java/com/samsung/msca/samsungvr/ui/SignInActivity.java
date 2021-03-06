package com.samsung.msca.samsungvr.ui;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.samsung.dallas.salib.SamsungSSO;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SignInActivity extends BaseActivity {

    private EditText mEmailForm;
    private EditText mPasswordForm;
    private TextView mSsoBtn;
    private CheckBox mShowPwdCheckbox;
    private ImageView mLoginBtn;
    private Bus mBus;

    private Drawable OK_CHECK_MARK;

    private void inject() {
        progressBar = (ProgressBar)findViewById(R.id.progressbar);
        mEmailForm = (EditText)findViewById(R.id.email_form);
        mPasswordForm = (EditText)findViewById(R.id.password_form);
        mSsoBtn = (TextView)findViewById(R.id.sso_username_or_create_acct);
        mSsoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSsoBtnClicked();
            }
        });
        mShowPwdCheckbox = (CheckBox)findViewById(R.id.show_pwd_checkbox);
        mLoginBtn = (ImageView)findViewById(R.id.login_button);
        mLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onLoginButtonClicked();
            }
        });
    }

    private static final String TAG = UILib.getLogTag(SignInActivity.class);
    private static final boolean DEBUG = UILib.DEBUG;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEBUG) {
            Log.d(TAG, "onCreate");
        }
        mBus = Bus.getEventBus();

        setContentView(R.layout.activity_sign_in);
        inject();

        OK_CHECK_MARK = getDrawable(R.drawable.btn_check_buttonless_on);

        // set button disabled since user has not entered anything yet
        mLoginBtn.setEnabled(false);

        // these will monitor validity of the entries and show/hide ok checkboxes
        // and enable/disable login button
        setEntryTextChangeListener(mEmailForm);
        setEntryTextChangeListener(mPasswordForm);

        // Trigger login when DONE keyboard button is pressed
        mPasswordForm.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    if (DEBUG) {
                        Log.d(TAG, "onEditorAction: IME_ACTION_GO from password form");
                    }
                    doBeginLoginProcess();
                    return true;
                }
                return false;
            }
        });
        mShowPwdCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mPasswordForm.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                } else {
                    mPasswordForm.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                }
                mPasswordForm.setSelection(mPasswordForm.getText().length());
            }
        });
        onIntent(getIntent());
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        onIntent(intent);
    }

    private long mCutoffTimestamp = -1;

    private SALibWrapper getSALibWrapper() {
        UILib uiLib = getUILib();
        if (null == uiLib) {
            return  null;
        }
        return uiLib.getSALibWrapperInternal();
    }

    private SyncSignInState getSyncSignInState() {
        UILib uiLib = getUILib();
        if (null == uiLib) {
            return  null;
        }
        return uiLib.getSyncSignInStateInternal();
    }

    private void onIntent(Intent intent) {
        mCutoffTimestamp = intent.getLongExtra(UILib.INTENT_PARAM_ID, -1);
        if (DEBUG) {
            Log.d(TAG, "onIntent: " + intent + " id: " + mCutoffTimestamp);
        }
        SALibWrapper saLibWrapper = getSALibWrapper();
        if (null != saLibWrapper) {
            processSamsungSsoStatus(saLibWrapper.getStatus());
        }
    }

    private UILib getUILib() {
        return UILib.getInstance();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (DEBUG) {
            Log.d(TAG, "onPause");
        }
        mBus.removeObserver(mBusCallback);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (DEBUG) {
            Log.d(TAG, "onResume");
        }

        mBus.addObserver(mBusCallback);
        SALibWrapper saLibWrapper = getSALibWrapper();
        if (null != saLibWrapper) {
            saLibWrapper.loadUserInfo(null);
        }
    }

    public void onLoginButtonClicked() {
        if (!canHandleForegroundEvent()) {
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "onLoginButtonClicked");
        }
        doBeginLoginProcess();
    }


    private static final int SSO_REQUEST_CODE = 1001;

    public void onSsoBtnClicked() {
        if (DEBUG) {
            Log.d(TAG, "onSsoBtnClicked");
        }
        UILib uiLib = getUILib();
        if (null == uiLib) {
            return;
        }

        SALibWrapper saLibWrapper = getSALibWrapper();
        if (null == saLibWrapper) {
            return;
        }
        SyncSignInState syncSignInState = getSyncSignInState();
        if (null == syncSignInState) {
            return;
        }
        SamsungSSO.UserInfo userInfo = saLibWrapper.getUserInfo();
        if (DEBUG) {
            Log.d(TAG, "onSsoBtnClicked userInfo: " + userInfo);
        }

        // If user info is available, then attempt to use it
        if (userInfo != null) {
            if (!canReachSamsungVRService(true, true)) {
                return;
            }
            syncSignInState.signIn(userInfo);
        } else {
            final SamsungSSO.Status status = saLibWrapper.getStatus();
            if (DEBUG) {
                Log.d(TAG, "SamsungSSO status: " + status);
            }
            Intent intent = null;
            if (status == SamsungSSO.Status.USER_NOT_DEFINED) {
                intent = saLibWrapper.buildAddAccountIntent();
            } else if (status == SamsungSSO.Status.USER_PW_REQUIRED) {
                intent = saLibWrapper.buildRequestTokenIntent(null);
            } else if ((status == SamsungSSO.Status.SSO_NOT_AVAILABLE)
                    || (status == SamsungSSO.Status.SSO_SIGNATURE_ERROR)) {
                Toast.makeText(this, getString(R.string.samsung_sso_unavailable), Toast.LENGTH_SHORT).show();
            } else {
                // In all other cases just show a generic login failure
                Toast.makeText(this, getString(R.string.signin_failure_generic), Toast.LENGTH_SHORT).show();
            }
            if (intent != null) {
                try {
                    if (DEBUG) {
                        Log.d(TAG, "Start sso activity: " + intent);
                    }
                    startActivityForResult(intent, SSO_REQUEST_CODE);
                } catch (ActivityNotFoundException e) {
                    if (DEBUG) {
                        Log.d(TAG, "Start sso activity failed", e);
                    }
                    Toast.makeText(this, getString(R.string.samsung_sso_unavailable), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (DEBUG) {
            Log.d(TAG, "onActivityResult requestCode: " + requestCode + " resultCode: " + resultCode
                + " data: " + data);
        }
        switch (requestCode) {
            case SSO_REQUEST_CODE:
                if (resultCode == SamsungSSO.RESULT_OK) {
                    SALibWrapper saLibWrapper = getSALibWrapper();
                    if (null != saLibWrapper) {
                        saLibWrapper.loadUserInfo(null);
                    }
                } else {
                    Toast.makeText(this, getString(R.string.signin_failure_generic), Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private Bus.Callback mBusCallback = new Bus.Callback() {

        @Override
        public void onLoggedInEvent(Bus.LoggedInEvent event) {

            // Login is complete finish activity
            if (SignInActivity.this.canHandleEvent()) {
                Toast.makeText(SignInActivity.this, getString(R.string.signed_in), Toast.LENGTH_SHORT).show();
                finish();
            }
        }

        @Override
        public void onLoginErrorEvent(Bus.LoginErrorEvent event) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(SignInActivity.this, event.mMessage, Toast.LENGTH_LONG).show();
        }

        @Override
        public void onSamsungSsoStatusEvent(Bus.SamsungSsoStatusEvent event) {
            processSamsungSsoStatus(event.mStatus);
        }

        @Override
        public void onInitEvent(Bus.InitEvent event) {
            finish();
        }

        @Override
        public void onRequestKillActivities(Bus.KillActivitiesEvent event) {
            finish();
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (DEBUG) {
            Log.d(TAG, "onDestroy");
        }
        mBus.post(mBusCallback, new Bus.SignInActivityDestroyed(getUILib(), mCutoffTimestamp));
    }

    private void processSamsungSsoStatus(SamsungSSO.Status status) {
        if (DEBUG) {
            Log.d(TAG, "processSamsungSsoStatus: " + status);
        }

        UILib uiLib = getUILib();
        if (null == uiLib) {
            return;
        }
        // preset to unavailable
        mSsoBtn.setText(getResources().getString(R.string.account_sso_unavailable));
        mSsoBtn.setEnabled(false);

        switch (status) {
            case USER_NOT_DEFINED:
                mSsoBtn.setText(getResources().getString(R.string.create_account_sso));
                mSsoBtn.setEnabled(true);
                break;

            case USER_INFO_UPDATED:
                SALibWrapper saLibWrapper = getSALibWrapper();
                if (null != saLibWrapper) {
                    final SamsungSSO.UserInfo info = saLibWrapper.getUserInfo();
                    if (info != null) {
                        mSsoBtn.setText(info.mLoginId);
                        mSsoBtn.setEnabled(true);
                    }
                }
                break;

            case USER_PW_REQUIRED:
                mSsoBtn.setText(getResources().getString(R.string.account_sso_verify_pw));
                mSsoBtn.setEnabled(true);
                break;
        }
    }

    private void doBeginLoginProcess() {
        String email = mEmailForm.getText().toString().trim();
        String pass = mPasswordForm.getText().toString().trim();

        //close the input keyboard
        final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mPasswordForm.getWindowToken(), 0);

        //ensure that the user has actually typed entry into the text fields
        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, R.string.signin_empty_userid_password, Toast.LENGTH_SHORT).show();
        } else {
            if (!canReachSamsungVRService(true, true)) {
                return;
            }
            progressBar.setVisibility(View.VISIBLE);
            SyncSignInState syncSignInState = getSyncSignInState();
            if (null != syncSignInState) {
                syncSignInState.signIn(email, pass);
            }
        }
    }

    protected void setEntryTextChangeListener(final EditText entry) {
        if (entry == null) {
            return;
        }

        entry.addTextChangedListener(new TextWatcher() {
                                         @Override
                                         public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                                         }

                                         @Override
                                         public void onTextChanged(CharSequence s, int start, int before, int count) {

                                         }

                                         @Override
                                         public void afterTextChanged(Editable s) {
                                             final String emailText = mEmailForm.getText().toString().trim();
                                             final String pwdText = mPasswordForm.getText().toString().trim();
                                             if (isEmailValid(emailText) &&
                                                     isPwdValid(pwdText, emailText)) {
                                                 mLoginBtn.setEnabled(true);

                                                 // set  OK check mark at the end of all entries
                                                 mEmailForm.setCompoundDrawablesWithIntrinsicBounds(null, null, OK_CHECK_MARK, null);
                                                 mPasswordForm.setCompoundDrawablesWithIntrinsicBounds(null, null, OK_CHECK_MARK, null);

                                             } else {
                                                 mLoginBtn.setEnabled(false);

                                                 // set/unset green check mark at the end of an entry if entry is valid
                                                 if (entry.equals(mEmailForm)) {
                                                     mEmailForm.setCompoundDrawablesWithIntrinsicBounds(null,
                                                             null,
                                                             isEmailValid(emailText) ? OK_CHECK_MARK : null,
                                                             null);

                                                 } else if (entry.equals(mPasswordForm)) {
                                                     mPasswordForm.setCompoundDrawablesWithIntrinsicBounds(null,
                                                             null,
                                                             isPwdValid(pwdText, emailText) ? OK_CHECK_MARK : null,
                                                             null);

                                                 }

                                             }
                                         }
                                     }

        );
    }

    private final static int PWD_MIN_LENGTH = 10;
    // check this value
    private final static int PWD_MAX_LENGTH = 32;

    private static boolean isEmailValidInternal(final String email) {
        boolean bEmailValid = false;
        if (!TextUtils.isEmpty(email)) {
            // basic email validation
            Pattern pattern = Pattern.compile(".+@.+\\.[a-z]+");
            Matcher matcher = pattern.matcher(email);
            bEmailValid = matcher.matches();
        }
        return bEmailValid;
    }

    private static boolean isPwdValidInternal(final String pwd) {
        boolean ret = false;
        if (!TextUtils.isEmpty(pwd) && pwd.length() >= PWD_MIN_LENGTH && pwd.length() <= PWD_MAX_LENGTH) {
            ret = true;
        }
        return ret;
    }

    private static boolean isEmailValid(final String email) {
        return (isEmailValidInternal(email) || isSpecialCommand(email));
    }

    private static boolean isSpecialCommand(final String text) {
        return false;
    }


    private static boolean isPwdValid(final String pwd, final String email) {
        boolean ret = false;
        if (isSpecialCommand(email) && !TextUtils.isEmpty(pwd)) {
            // if email contains special command then password doesn't have to be 10 characters minimum
            ret = true;
        } else {
            ret = isPwdValidInternal(pwd);
        }

        return ret;
    }
}




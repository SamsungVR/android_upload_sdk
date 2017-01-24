package com.samsung.msca.samsungvr.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.util.Linkify;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.samsung.msca.samsungvr.sdk.UnverifiedUser;
import com.samsung.msca.samsungvr.sdk.VR;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class CreateVRAccountActivity extends BaseActivity {

    private View mCreateAcctInfoLayout;
    private EditText mEmailForm;
    private EditText mNameForm;
    private EditText mCompanyUrlForm;
    private EditText mPasswordForm;
    private EditText mVerifyPasswordForm;
    private TextView mAgreeToTerms;
    private View mSuccessLayout;
    private CheckBox mShowPwdCheckbox;
    private ImageButton mCreateAccountButton;

    private VrAcctInfo mCreateAcctInfo;
    // same as on the server
    private final static int PWD_MIN_LENGTH = 10;
    // check this value
    private final static int PWD_MAX_LENGTH = 32;
    private final static int DISPLAY_NAME_MIN_LENGTH = 3;
    private final static int DISPLAY_NAME_MAX_LENGTH = 64;
    private Drawable OK_CHECK_MARK;
    private String mSuccessEmailAddr;

    private Bus mBus;

    private static final String TAG = "CreateVRAccountActivity";

    private void inject() {
        progressBar = (ProgressBar)findViewById(R.id.progressbar);
        mCreateAcctInfoLayout = findViewById(R.id.create_acct_info_layout);
        mEmailForm = (EditText)findViewById(R.id.email_form);
        mNameForm = (EditText)findViewById(R.id.name_form);
        mCompanyUrlForm = (EditText)findViewById(R.id.company_url_form);
        mPasswordForm = (EditText)findViewById(R.id.password_form);
        mVerifyPasswordForm = (EditText)findViewById(R.id.verify_password_form);
        mAgreeToTerms = (TextView)findViewById(R.id.agree_to_terms);
        mSuccessLayout = findViewById(R.id.success_layout);
        mShowPwdCheckbox = (CheckBox)findViewById(R.id.show_pwd_checkbox);
        mCreateAccountButton = (ImageButton)findViewById(R.id.create_account_button);
        mCreateAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onCreateVrAccountClicked();
            }
        });
        findViewById(R.id.dismiss_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDismissBtnClicked();
            }
        });

    }

    static void addLink(TextView textView, String patternToMatch,
                        final String link) {
        Linkify.TransformFilter filter = new Linkify.TransformFilter() {
            @Override public String transformUrl(Matcher match, String url) {
                return link;
            }
        };
        Linkify.addLinks(textView, Pattern.compile(patternToMatch), null, null,
                filter);
    }

    private final Bus.Callback mBusCallback = new Bus.Callback() {
        @Override
        public void onCreatedVrAcct(Bus.CreatedVrAccountEvent event) {
            if (CreateVRAccountActivity.this.canHandleEvent()) {
                progressBar.setVisibility(View.GONE);
                if (event.mSuccess) {
                    // Account is created -> prompt user to verify the account
                    //Toast360.makeText(this, R.string.create_account_vr_success, Toast.LENGTH_LONG).show();
                    mCreateAcctInfoLayout.setVisibility(View.GONE);
                    mSuccessLayout.setVisibility(View.VISIBLE);
                    ((TextView) mSuccessLayout.findViewById(R.id.success_email)).setText(mSuccessEmailAddr);
                } else {
                    final String err = getResources().getString(R.string.create_account_vr_failure,
                            getResources().getString(event.mStatus.getCannedMsgId()));
                    Toast.makeText(CreateVRAccountActivity.this, err, Toast.LENGTH_LONG).show();
                    finish();
                }

            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBus = Bus.getEventBus();
        mBus.addObserver(mBusCallback);

        getWindow().getDecorView().setBackgroundColor(ContextCompat.getColor(CreateVRAccountActivity.this, R.color.translucent_black_30_percent));
        setContentView(R.layout.activity_create_vr_account);
        inject();



        // Linkify "Terms of Use"
        Resources resources = getResources();
        String terms_of_use = resources.getString(R.string.eula_header_terms);
        String text = resources.getString(R.string.create_vr_acct_header, terms_of_use);
        mAgreeToTerms.setText(text);
        addLink(mAgreeToTerms, terms_of_use, UILib.getInstance().getExternalServerBaseURL() +
                resources.getString(R.string.link_terms_and_conditions));

        // Set max length for the password fields
        InputFilter[] filters = new InputFilter[1];
        filters[0] = new InputFilter.LengthFilter(PWD_MAX_LENGTH);
        mPasswordForm.setFilters(filters);
        mVerifyPasswordForm.setFilters(filters);
        // test code
//        mCreateAcctInfoLayout.setVisibility(View.GONE);
//        mSuccessLayout.setVisibility(View.VISIBLE);

//        VRLibWrapper.INSTANCE.initializeVRLib();
//        // Trigger login when DONE keyboard button is pressed
//        mPasswordForm.setOnEditorActionListener(new TextView.OnEditorActionListener() {
//            @Override
//            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
//                if (actionId == EditorInfo.IME_ACTION_GO) {
//                    Timber.d("onEditorAction: IME_ACTION_GO from password form");
//                    doBeginLoginProcess();
//                    return true;
//                }
//                return false;
//            }
//        });
        // set button disabled since user has not entered anything yet
        mCreateAccountButton.setEnabled(false);

        OK_CHECK_MARK = getDrawable(R.drawable.btn_check_buttonless_on);
        mShowPwdCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mPasswordForm.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    mVerifyPasswordForm.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                } else {
                    mPasswordForm.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    mVerifyPasswordForm.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                }
                mPasswordForm.setSelection(mPasswordForm.getText().length());
                mVerifyPasswordForm.setSelection(mVerifyPasswordForm.getText().length());
            }
        });

        setEntryTextChangeListener(mNameForm);
        setEntryTextChangeListener(mEmailForm);
        setEntryTextChangeListener(mPasswordForm);
        setEntryTextChangeListener(mVerifyPasswordForm);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBus.removeObserver(mBusCallback);
    }

    public void onCreateVrAccountClicked() {
        if (!canHandleForegroundEvent()) {
            return;
        }
        Log.d(TAG, "onCreateVrAccountClicked");
        createAccount();
    }

    public void onDismissBtnClicked() {
        if (!canHandleForegroundEvent()) {
            return;
        }
        Log.d(TAG, "onDismissBtnClicked");
        finish();
    }

    private void createAccount() {
        Log.d(TAG, "createAccount");
        if (!canReachSamsungVRService(true, true)) {
            return;
        }
        final String email = mEmailForm.getText().toString().trim();
        final String name = mNameForm.getText().toString().trim();
        // TODO company Url is optional. For now we are not passing it to VR lib during
        // because VR lib doesn't support it yet
        final String companyUrl = mCompanyUrlForm.getText().toString().trim();
        final String pass = mPasswordForm.getText().toString().trim();
        final String verifyPass = mVerifyPasswordForm.getText().toString().trim();


        //close the input keyboard
        final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mPasswordForm.getWindowToken(), 0);
        //ensure that the user has actually typed entry into the text fields
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(name) ||
                TextUtils.isEmpty(pass) || TextUtils.isEmpty(verifyPass)) {
            // required input fields must not be empty
            Toast.makeText(this, R.string.err_input_field_empty, Toast.LENGTH_SHORT).show();

        } else if (!isEmailValid(email)) {
            Toast.makeText(this, R.string.err_invalid_email, Toast.LENGTH_SHORT).show();

        } else if (pass.length() < PWD_MIN_LENGTH) {
            // password must be at least 10 characters long
            Toast.makeText(this, this.getString(R.string.err_pwd_min_length, PWD_MIN_LENGTH), Toast.LENGTH_SHORT).show();


        } else if ((pass.length() >= PWD_MIN_LENGTH && verifyPass.length() < PWD_MIN_LENGTH) ||
                !verifyPass.equals(pass)) {

            // verify password field must match password field
            Toast.makeText(this, R.string.err_verify_pwd_mismatch, Toast.LENGTH_SHORT).show();

        } else {
            progressBar.setVisibility(View.VISIBLE);
            mCreateAcctInfo = new VrAcctInfo(email, name, companyUrl, pass);
            // Test code
//            mSuccessEmailAddr = mCreateAcctInfo.mEmail;
//            postEventAsync(new CreatedVrAccountEvent(true, null));
            VR.newUser(name, email, pass, mCreateAcctCallback, null, mCreateAcctInfo);
        }
    }

    private static boolean isNameValid(final String name) {
        boolean ret = false;
        if (!TextUtils.isEmpty(name) && name.length() >= DISPLAY_NAME_MIN_LENGTH && name.length() <= DISPLAY_NAME_MAX_LENGTH) {
            ret = true;
        }
        return ret;
    }

    static boolean isEmailValid(final String email) {
        boolean bEmailValid = false;
        if (!TextUtils.isEmpty(email)) {
            // basic email validation
            Pattern pattern = Pattern.compile(".+@.+\\.[a-z]+");
            Matcher matcher = pattern.matcher(email);
            bEmailValid = matcher.matches();
        }
        return bEmailValid;
    }

    static boolean isPwdValid(final String pwd) {
        boolean ret = false;
        if (!TextUtils.isEmpty(pwd) && pwd.length() >= PWD_MIN_LENGTH && pwd.length() <= PWD_MAX_LENGTH) {
            ret = true;
        }
        return ret;
    }


    private boolean pwdEntriesValid() {
        // this checks to make sure password and verify password entries are valid and matching
        final String pwd = mPasswordForm.getText().toString().trim();
        final String verifyPwd = mVerifyPasswordForm.getText().toString().trim();
        return (isPwdValid(pwd) && isPwdValid(verifyPwd) && pwd.equals(verifyPwd));
    }

    private boolean verifyAllEntries() {
        final String name = mNameForm.getText().toString().trim();
        final String email = mEmailForm.getText().toString().trim();
        final String pass = mPasswordForm.getText().toString().trim();
        final String verifyPass = mVerifyPasswordForm.getText().toString().trim();
        return allEntriesValid(name, email, pass, verifyPass);
    }

    private boolean allEntriesValid(final String name, final String email, final String pwd, final String verifyPwd) {
        return (isNameValid(name) && isEmailValid(email) && isPwdValid(pwd) && isPwdValid(verifyPwd) && pwd.equals(verifyPwd));
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
                                             if (verifyAllEntries()) {
                                                 mCreateAccountButton.setEnabled(true);

                                                 // set  green check mark at the end of all entries
                                                 mNameForm.setCompoundDrawablesWithIntrinsicBounds(null, null, OK_CHECK_MARK, null);
                                                 mEmailForm.setCompoundDrawablesWithIntrinsicBounds(null, null, OK_CHECK_MARK, null);
                                                 mPasswordForm.setCompoundDrawablesWithIntrinsicBounds(null, null, OK_CHECK_MARK, null);
                                                 mVerifyPasswordForm.setCompoundDrawablesWithIntrinsicBounds(null, null, OK_CHECK_MARK, null);

                                             } else {
                                                 mCreateAccountButton.setEnabled(false);

                                                 // set/unset green check mark at the end of an entry if entry is valid
                                                 if (entry.equals(mNameForm)) {
                                                     mNameForm.setCompoundDrawablesWithIntrinsicBounds(null,
                                                             null,
                                                             isNameValid(mNameForm.getText().toString().trim()) ? OK_CHECK_MARK : null,
                                                             null);

                                                 } else if (entry.equals(mEmailForm)) {
                                                     mEmailForm.setCompoundDrawablesWithIntrinsicBounds(null,
                                                             null,
                                                             isEmailValid(mEmailForm.getText().toString().trim()) ? OK_CHECK_MARK : null,
                                                             null);

                                                 } else if (entry.equals(mPasswordForm)) {
                                                     // mPasswordForm is valid if its contents are valid
                                                     mPasswordForm.setCompoundDrawablesWithIntrinsicBounds(null,
                                                             null,
                                                             isPwdValid(mPasswordForm.getText().toString().trim()) ? OK_CHECK_MARK : null,
                                                             null);
                                                     // but mVerifyPasswordForm is only valid if
                                                     // both mVerifyPasswordForm and mPasswordForm contents are valid
                                                     mVerifyPasswordForm.setCompoundDrawablesWithIntrinsicBounds(null,
                                                             null,
                                                             pwdEntriesValid() ? OK_CHECK_MARK : null,
                                                             null);

                                                 } else if (entry.equals(mVerifyPasswordForm)) {
                                                     mVerifyPasswordForm.setCompoundDrawablesWithIntrinsicBounds(null,
                                                             null,
                                                             pwdEntriesValid() ? OK_CHECK_MARK : null,
                                                             null);

                                                 }

                                             }
                                         }
                                     }

        );
    }


    // Callback used when creating new VR user account via VR lib
    private VR.Result.NewUser mCreateAcctCallback = new VR.Result.NewUser() {
        @Override
        public void onCancelled(Object o) {
            if (o == mCreateAcctInfo) {
                Log.e(TAG, "NewUser.onCancelled");
                mCreateAcctInfo = null;
                mBus.post(new Bus.CreatedVrAccountEvent(false, CreateVrAcctStatus.UNKNOWN));
            }
        }

        @Override
        public void onException(Object o, Exception e) {
            Log.e(TAG, "NewUser.onException", e);
            mCreateAcctInfo = null;
            mBus.post(new Bus.CreatedVrAccountEvent(false, CreateVrAcctStatus.UNKNOWN));
        }

        @Override
        public void onFailure(Object o, int i) {
            if (o == mCreateAcctInfo) {
                Log.e(TAG, "NewUser.onFailure" + i);
                mCreateAcctInfo = null;
                mBus.post(new Bus.CreatedVrAccountEvent(false, CreateVrAcctStatus.getStatus(i)));
            }

        }

        @Override
        public void onSuccess(Object o, UnverifiedUser unverifiedUser) {
            if (o == mCreateAcctInfo) {
                mSuccessEmailAddr = mCreateAcctInfo.mEmail;
                mCreateAcctInfo = null;
                Log.d(TAG, "NewUser.onSuccess" + unverifiedUser.getUserId());
                mBus.post(new Bus.CreatedVrAccountEvent(true, null));
            }
        }
    };

    private static class VrAcctInfo {
        final String mEmail;
        final String mName;
        final String mCompanyUrl;
        final String mPwd;

        VrAcctInfo(final String email, final String name, final String companyUrl, final String pwd) {
            mEmail = email;
            mName = name;
            mCompanyUrl = companyUrl;
            mPwd = pwd;
        }

    }

}

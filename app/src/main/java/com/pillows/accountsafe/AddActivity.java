package com.pillows.accountsafe;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;

/**
 * Created by agudz on 19/01/16.
 */
public class AddActivity extends AppCompatActivity {

    private AutoCompleteTextView mNameView;
    private EditText mPasswordView;
    private EditText mRePasswordView;
    private EditText mLoginView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add);
        // Set back button in top bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Button mEmailSignInButton = (Button) findViewById(R.id.submit_add_button);
        mEmailSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptAddSubmit();
            }
        });

        mNameView = (AutoCompleteTextView) findViewById(R.id.account_name);
        mPasswordView = (EditText) findViewById(R.id.account_password);
        mRePasswordView = (EditText) findViewById(R.id.account_repassword);
        mLoginView = (EditText) findViewById(R.id.account_login);
    }

    private void attemptAddSubmit() {

        mNameView.setError(null);
        mPasswordView.setError(null);
        mRePasswordView.setError(null);

        String name = mNameView.getText().toString();
        String password = mPasswordView.getText().toString();
        String repassword = mRePasswordView.getText().toString();
        String login = mLoginView.getText().toString();

        View focusView = null;

        if (TextUtils.isEmpty(name)) {
            mNameView.setError(getString(R.string.error_field_required));
            focusView = mNameView;
        } else
        if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
        } else
        if (TextUtils.isEmpty(repassword)) {
            mRePasswordView.setError(getString(R.string.error_field_required));
            focusView = mRePasswordView;
        } else if (!TextUtils.equals(password, repassword)) {
            mRePasswordView.setError(getString(R.string.error_incorrect_repassword));
            focusView = mRePasswordView;
        }

        if (focusView != null) {
            focusView.requestFocus();
        } else {
            Bundle conData = new Bundle();
            conData.putString("account_name", name);
            conData.putString("account_password", password);
            conData.putString("account_login", login);
            Intent intent = new Intent();
            intent.putExtras(conData);
            setResult(RESULT_OK, intent);
            finish();
        }
    }

}
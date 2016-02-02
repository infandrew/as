package com.pillows.accountsafe;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;

import java.math.BigInteger;
import java.util.Random;

/**
 * Created by agudz on 19/01/16.
 */
public class EditActivity extends AppCompatActivity {

    private AutoCompleteTextView mNameView;
    private EditText mPasswordView;
    private EditText mRePasswordView;
    private EditText mLoginView;
    private int position;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_add);
        // Set back button in top bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Button submitButton = (Button) findViewById(R.id.submit_add_button);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptAddSubmit();
            }
        });
        submitButton.setText(R.string.submit_edit_button_text);

        mNameView = (AutoCompleteTextView) findViewById(R.id.account_name);
        mPasswordView = (EditText) findViewById(R.id.account_password);
        mRePasswordView = (EditText) findViewById(R.id.account_repassword);
        mLoginView = (EditText) findViewById(R.id.account_login);

        Bundle res = getIntent().getExtras();
        if (res != null) {
            position = res.getInt("account_position");
            String name = res.getString("account_name");
            mNameView.setText(name);
        }
    }

    private void attemptAddSubmit() {

        mNameView.setError(null);
        mPasswordView.setError(null);
        mRePasswordView.setError(null);

        String name = mNameView.getText().toString();
        String password = mPasswordView.getText().toString();
        String repassword = mRePasswordView.getText().toString();
        String login = mLoginView.getText().toString();

        if (Settings.DEBUG && TextUtils.isEmpty(login)) {
            Random rand = new Random();
            name = new BigInteger(60, rand).toString(32);
            password = new BigInteger(60, rand).toString(32);
            repassword = password;
            login = new BigInteger(60, rand).toString(32);
        }

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
            conData.putInt("account_position", position);
            conData.putString("account_name", name);
            conData.putString("account_password", password);
            conData.putString("account_login", login);
            Intent intent = new Intent();
            intent.putExtras(conData);
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
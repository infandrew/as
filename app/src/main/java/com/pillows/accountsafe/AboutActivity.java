package com.pillows.accountsafe;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;

import java.math.BigInteger;
import java.util.Random;

/**
 * Created by agudz on 19/01/16.
 */
public class AboutActivity extends AppCompatActivity {

    private AutoCompleteTextView mNameView;
    private EditText mPasswordView;
    private EditText mRePasswordView;
    private EditText mLoginView;
    private int position;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_about);
        // Set back button in top bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
}
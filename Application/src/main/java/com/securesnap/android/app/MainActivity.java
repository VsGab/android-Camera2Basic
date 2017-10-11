package com.securesnap.android.app;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    EditText mPassword;
    TextView mPasswordLabel;
    TextView mMagicValue;
    Button mSet;
    Button mGallery;
    Button mCamera;
    SecureSnapApp mApp;
    ProgressBar mSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mApp = (SecureSnapApp)getApplication();
        mPasswordLabel = (TextView) findViewById(R.id.password_label);
        mPassword = (EditText) findViewById(R.id.password_field);
        mSet = (Button) findViewById(R.id.password_set);
        mGallery = (Button) findViewById(R.id.gallery_button);
        mCamera = (Button) findViewById(R.id.camera_button);
        mMagicValue = (TextView) findViewById(R.id.magic_value);
        mMagicValue.setText(mApp.getMagicString());
        mSpinner = (ProgressBar) findViewById(R.id.key_progress);

        setLabel();
        mSet.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String password = mPassword.getText().toString();
                if (setPassword(password)) {
                    mPassword.setText("");
                }
                else {
                    passwordRulesShow();
                }
            }
        });
        mCamera.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startCamera();
            }
        });
        mGallery.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startGallery();
            }
        });
    }

   private void setLabel() {
        if (mApp.haveKey()) {
            mPasswordLabel.setText(getString(R.string.password_ok));
            mPassword.setVisibility(View.INVISIBLE);
            mSet.setVisibility(View.INVISIBLE);
            mSpinner.setVisibility(View.INVISIBLE);
        }
        else {
            mPasswordLabel.setText(getString(R.string.password_enter));
        }
    }

    private boolean setPassword(String password) {
        if (password.length() < 8)
            return false;

        final Activity self  = this;
        Runnable onKeyAvailable = new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mSpinner.setVisibility(View.INVISIBLE);
                        mSet.setEnabled(true);
                        setLabel();
                    }
                });
            }
        };

        Runnable passwordInvalid = new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(self, R.string.password_incorrect, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        };

        mApp.setPassword(password,onKeyAvailable,passwordInvalid);
        mSpinner.setVisibility(View.VISIBLE);
        mSet.setEnabled(false);

        return true;
    }

    private void passwordRulesShow() {
        Toast.makeText(this, R.string.password_invalid, Toast.LENGTH_LONG).show();
    }

    private void startGallery() {
        if (mApp.haveKey()) {
            Intent intent = new Intent(this, GalleryActivity.class);
            startActivity(intent);
        }
        else {
            Toast.makeText(this, R.string.password_not_set, Toast.LENGTH_SHORT).show();
        }
    }

    private void startCamera() {
        if (mApp.haveKey()) {
            Intent intent = new Intent(this, CameraActivity.class);
            startActivity(intent);
        }
        else {
            Toast.makeText(this, R.string.password_not_set, Toast.LENGTH_SHORT).show();
        }
    }

}

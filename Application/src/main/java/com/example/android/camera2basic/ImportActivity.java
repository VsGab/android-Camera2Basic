package com.example.android.camera2basic;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.InputStream;

import javax.crypto.spec.SecretKeySpec;

public class ImportActivity extends Activity {
    private InputStream mContentStream;
    private EditText mPassword;
    private EditText mMagic;
    private Button mImport;
    private ProgressBar mSpinner;
    private SecureSnapApp mApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import);
        mApp = (SecureSnapApp)getApplication();

        // Get the intent that started this activity
        Intent intent = getIntent();
        Uri uri = intent.getData();
        try {
            mContentStream = getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            finish();
        }

        mMagic = (EditText) findViewById(R.id.peer_magic);
        mPassword = (EditText) findViewById(R.id.peer_password);
        mImport = (Button) findViewById(R.id.import_button);
        mSpinner = (ProgressBar)findViewById(R.id.import_spinner);
        mSpinner.setVisibility(View.INVISIBLE);

        if (!mApp.haveKey())
            Toast.makeText(this, R.string.password_not_set, Toast.LENGTH_SHORT).show();

        final Activity self = this;
        mImport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mApp.haveKey()) {
                    mSpinner.setVisibility(View.VISIBLE);
                    mImport.setEnabled(false);
                    AsyncTask.execute(new Runnable() {
                        @Override
                        public void run() {
                            importImage();
                        }
                    });
                }
                else {
                    Toast.makeText(self, R.string.password_not_set, Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    private void importImage() {
        SecretKeySpec key = EncryptionUtils.buildKey(mPassword.getText().toString(),mMagic.getText().toString());
        byte[] data = EncryptionUtils.decrypt(mContentStream,key);
        mApp.saveEncrypted(data);
        final Activity self = this;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mSpinner.setVisibility(View.INVISIBLE);
                Intent intent = new Intent(self, GalleryActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

}

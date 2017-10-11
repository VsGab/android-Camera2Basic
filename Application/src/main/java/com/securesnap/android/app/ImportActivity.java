package com.securesnap.android.app;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import javax.crypto.spec.SecretKeySpec;

public class ImportActivity extends Activity {
    private InputStream mContentStream;
    private EditText mPassword;
    private EditText mMagic;
    private Button mImport;
    private Button mView;
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
        mView = (Button) findViewById(R.id.view_button);
        mSpinner = (ProgressBar)findViewById(R.id.import_spinner);
        mSpinner.setVisibility(View.INVISIBLE);

        if (!mApp.haveKey()) {
            Toast.makeText(this, R.string.password_not_set, Toast.LENGTH_SHORT).show();
            mImport.setEnabled(false);
        }

        final Activity self = this;
        mImport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mApp.haveKey()) {
                    mSpinner.setVisibility(View.VISIBLE);
                    mImport.setEnabled(false);
                    mView.setEnabled(false);
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

        mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSpinner.setVisibility(View.VISIBLE);
                mImport.setEnabled(false);
                mView.setEnabled(false);
                AsyncTask.execute(new Runnable() {
                        @Override
                        public void run() {
                            viewImage();
                        }
                    });
            }
        });

    }

    private void viewImage() {
        try {
            String fname = UUID.randomUUID().toString() + ".safe";
            final File file = new File(getExternalFilesDir(null), fname);
            FileUtils.copyInputStreamToFile(mContentStream, file);

            byte[] key = EncryptionUtils.buildKeyRaw(mPassword.getText().toString(),mMagic.getText().toString());
            final String b64Key = Base64.encodeToString(key,Base64.DEFAULT);

            final Activity self = this;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mSpinner.setVisibility(View.INVISIBLE);
                    Intent intent = new Intent(self, GalleryActivity.class);
                    intent.putExtra("key", b64Key);
                    intent.putExtra("file", file.getAbsolutePath());
                    startActivity(intent);
                    finish();
                }});

            } catch (IOException e) {
                e.printStackTrace();
            }
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

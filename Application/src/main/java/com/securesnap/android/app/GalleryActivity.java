package com.securesnap.android.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class GalleryActivity extends Activity  {
    private final static String TAG = "SecureGallery";

    private SecureSnapApp mApp;
    private PinchZoomImageView mImageView;
    private ImageButton mPrevImage;
    private ImageButton mNextImage;
    private ImageButton mShareImage;
    private ImageButton mDeleteImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String key = intent.getStringExtra("key");
        String file = intent.getStringExtra("file");

        setContentView(R.layout.activity_gallery);
        mApp = (SecureSnapApp)getApplication();
        mImageView = (PinchZoomImageView)findViewById(R.id.image_box);
        mPrevImage = (ImageButton)findViewById(R.id.prev_image);
        mNextImage = (ImageButton)findViewById(R.id.next_image);
        mShareImage = (ImageButton)findViewById(R.id.image_share);
        mDeleteImage = (ImageButton)findViewById(R.id.image_delete);

        File tmpFile = null;
        boolean useExternalKey = false;

        if (key != null && file != null) {
            // view mode
            tmpFile = new File(file);
            useExternalKey = true;
        }
        else {
            // gallery mode
            tmpFile = mApp.lastImage();

            mNextImage.setVisibility(View.VISIBLE);
            mPrevImage.setVisibility(View.VISIBLE);
            mShareImage.setVisibility(View.VISIBLE);
            mDeleteImage.setVisibility(View.VISIBLE);

            mPrevImage.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    previousImage();
                }
            });
            mNextImage.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    nextImage();
                }
            });
            mShareImage.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    shareImage();
                }
            });
        }


        try {
            FileInputStream fi = new FileInputStream(tmpFile);
            byte[] data = null;
            if (useExternalKey) {
                byte[] keyRaw = Base64.decode(key,Base64.DEFAULT);
                data = EncryptionUtils.decrypt(fi,EncryptionUtils.buildKey(keyRaw));
            }
            else{
                data = mApp.decrypt(fi);
            }
            if (data != null)
                mImageView.setImageData(data);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


    }

    private void nextImage() {
        Log.d(TAG, "left");
    }

    private void previousImage() {
        Log.d(TAG, "right");
    }

    private void shareImage() {
        File outputFile = mApp.lastImage();
        Uri uri = Uri.fromFile(outputFile);

        Intent share = new Intent();
        share.setAction(Intent.ACTION_SEND);
        share.setType("application/octet-stream");
        share.putExtra(Intent.EXTRA_STREAM, uri);
        share.setPackage("com.whatsapp");

        startActivity(share);
    }
}


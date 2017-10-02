package com.example.android.camera2basic;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.Image;
import android.net.Uri;
import android.os.AsyncTask;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import javax.crypto.spec.SecretKeySpec;

/**
 * Created by gabi on 10/1/17.
 */

public class SecureSnapApp extends Application {
    private SecretKeySpec mKey;
    private String mRandSalt;
    private String mKeyHash;
    private SharedPreferences mPrefs;
    private File mSaveDir;

    private final static String RAND_SALT = "rand_salt";
    private final static String KEY_HASH = "key_hash";

    @Override
    public void onCreate() {
        super.onCreate();
        mPrefs = getSharedPreferences(
                getString(R.string.preference_file_key),Context.MODE_PRIVATE);
        initSalt();
        mKeyHash = mPrefs.getString(KEY_HASH,null);
        mSaveDir = getExternalFilesDir(null);
    }

    // Called by the system when the device configuration changes while your component is running.
    // Overriding this method is totally optional!
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    // This is called when the overall system is running low on memory,
    // and would like actively running processes to tighten their belts.
    // Overriding this method is totally optional!
    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    public synchronized void setPassword(final String password, final Runnable onKeyAvailable, final Runnable passwordIncorrect) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                mKey = EncryptionUtils.buildKey(password, mRandSalt);
                if (mKeyHash == null) {
                    String keyHash = EncryptionUtils.hash(mKey.getEncoded());
                    SharedPreferences.Editor editor = mPrefs.edit();
                    editor.putString(KEY_HASH, keyHash);
                    editor.commit();
                    mKeyHash = keyHash;
                }
                else {
                    String enteredPasswordKeyHash = EncryptionUtils.hash(mKey.getEncoded());
                    if (!enteredPasswordKeyHash.equals(mKeyHash)) {
                        passwordIncorrect.run();
                        return;
                    }
                }

                onKeyAvailable.run();
            }
        });
    }

    public byte[] decrypt(InputStream in) {
        byte[] data =  EncryptionUtils.decrypt(in,mKey);
        return data;
    }

    public void saveEncrypted(byte[] data) {
        try {
            FileOutputStream output = new FileOutputStream(new File(mSaveDir, "pic.safe"));
            EncryptionUtils.encrypt(output, data, mKey);
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean haveKey() {
        return mKey != null;
    }

    public String getMagicString() {
        return mRandSalt;
    }

    private void initSalt() {
        mRandSalt = mPrefs.getString(RAND_SALT,null);

        if (mRandSalt == null) {
            String randSalt =  EncryptionUtils.randSalt();
            SharedPreferences.Editor editor = mPrefs.edit();
            editor.putString(RAND_SALT, randSalt);
            editor.commit();
            mRandSalt = randSalt;
        }
    }

    public ImageSaver nextImageSaver(Image image) {
        return new ImageSaver(image, new File(mSaveDir, "pic.safe"));
    }

    public File lastImage() {
        return new File(mSaveDir, "pic.safe");
    }

    /**
     * Saves a JPEG {@link Image} into the specified {@link File}.
     */
    public class ImageSaver implements Runnable {

        /**
         * The JPEG image
         */
        private final Image mImage;
        /**
         * The file we save the image into.
         */
        private final File mFile;

        public ImageSaver(Image image, File file) {
            mImage = image;
            mFile = file;
        }

        @Override
        public void run() {
            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);


            FileOutputStream output = null;
            try {
                output = new FileOutputStream(mFile);
                EncryptionUtils.encrypt(output,bytes,mKey);

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mImage.close();
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }
}
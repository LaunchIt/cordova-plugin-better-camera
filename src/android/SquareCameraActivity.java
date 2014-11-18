package org.apache.cordova.camera;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import android.net.Uri;
import android.view.Display;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.hardware.Camera;
import android.widget.ImageView;

import android.widget.RelativeLayout;

/**
 * Activity displaying the camera and mustache preview.
 *
 * @author Sebastian Kaspari <sebastian@androidzeitgeist.com>
 */
public class SquareCameraActivity extends Activity implements CameraFragmentListener {

    public static final String TAG = "Mustache/SquareCameraActivity";

    private static final int PICTURE_QUALITY = 90;

    private String mDate = null;

    private int mScreenWidth;

    private int mScreenHeight;

    /**
     * On activity getting created.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(getResources().getIdentifier("activity_camera", "layout", getPackageName()));

        Display display = getWindowManager().getDefaultDisplay();
        // Necessary to use deprecated methods for Android 2.x support
        mScreenWidth = display.getWidth();
        mScreenHeight = display.getHeight();

        float density = getResources().getDisplayMetrics().density;
        final ImageView viewfinder = (ImageView) findViewById(getResources().getIdentifier("viewfinder", "id", getPackageName()));
        final RelativeLayout focusButton = (RelativeLayout) findViewById(getResources().getIdentifier("viewfinderArea", "id", getPackageName()));

        CameraFragment fragment = (CameraFragment) getFragmentManager()
                .findFragmentById(getResources().getIdentifier("camera_fragment", "id", getPackageName()));

        Camera camera = fragment.getCamera();
        focusButton.setOnTouchListener(new FocusButton(mScreenWidth, mScreenHeight, density, camera, viewfinder));

    }

    /**
     * On fragment notifying about a non-recoverable problem with the camera.
     */
    @Override
    public void onCameraError() {
        Toast.makeText(this, getString(getResources().getIdentifier("toat_error_camera_preview", "string", getPackageName())), Toast.LENGTH_SHORT)
                .show();
        finish();
    }

    /**
     * The user wants to take a picture.
     */
    public void takePicture(View view) {
        view.setEnabled(false);
        CameraFragment fragment = (CameraFragment) getFragmentManager().findFragmentById(
                getResources().getIdentifier("camera_fragment", "id", getPackageName()));
        fragment.takePicture();
    }

    public void swapCamera(View view) {
        CameraFragment fragment = (CameraFragment) getFragmentManager().findFragmentById(
                getResources().getIdentifier("camera_fragment", "id", getPackageName()));
        fragment.swapCamera();
    }

    public void swapFlash(View view) {
        CameraFragment fragment = (CameraFragment) getFragmentManager().findFragmentById(
                getResources().getIdentifier("camera_fragment", "id", getPackageName()));
        fragment.swapFlash();
    }

    /**
     * A picture has been taken.
     */
    public void onPictureTaken(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] data = stream.toByteArray();

        Uri fileUri = (Uri) getIntent().getExtras().get(MediaStore.EXTRA_OUTPUT);
        File pictureFile = new File(fileUri.getPath());
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            fos.write(data);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
        }
        setResult(RESULT_OK);
        finish();
    }

    private void showSavingPictureErrorToast() {
        Toast.makeText(this, getText(getResources().getIdentifier("toat_error_save_picture", "string", getPackageName())), Toast.LENGTH_SHORT).show();
    }


    private File createCaptureFile() {
        File oldFile = new File(getTempDirectoryPath(getApplicationContext()), "Pic-" + mDate + ".jpg");
        if (oldFile.exists()) {
            oldFile.delete();
        }

        Calendar c = Calendar.getInstance();
        mDate = "" + c.get(Calendar.DAY_OF_MONTH)
                + c.get(Calendar.MONTH)
                + c.get(Calendar.YEAR)
                + c.get(Calendar.HOUR_OF_DAY)
                + c.get(Calendar.MINUTE)
                + c.get(Calendar.SECOND);

        return new File(getTempDirectoryPath(getApplicationContext()), "Pic-" + mDate + ".jpg");
    }

    private String getTempDirectoryPath(Context ctx) {
        File cache = null;

        // SD Card Mounted
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            cache = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/" + ctx.getPackageName() + "/cache/");
        }
        // Use internal storage
        else {
            cache = ctx.getCacheDir();
        }

        // Create the cache directory if it doesn't exist
        if (!cache.exists()) {
            cache.mkdirs();
        }

        return cache.getAbsolutePath();
    }
}

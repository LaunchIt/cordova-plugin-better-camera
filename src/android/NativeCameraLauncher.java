/*
	    Copyright 2014 Giovanni Di Gregorio.

		Licensed under the Apache License, Version 2.0 (the "License");
		you may not use this file except in compliance with the License.
		You may obtain a copy of the License at

		http://www.apache.org/licenses/LICENSE-2.0

		Unless required by applicable law or agreed to in writing, software
		distributed under the License is distributed on an "AS IS" BASIS,
		WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
		See the License for the specific language governing permissions and
   		limitations under the License.   			
 */

package org.apache.cordova.camera;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.camera.ExifHelper;

import org.json.JSONArray;
import org.json.JSONException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;

/**
 * This class launches the camera view, allows the user to take a picture,
 * closes the camera view, and returns the captured image. When the camera view
 * is closed, the screen displayed before the camera view was shown is
 * redisplayed.
 */
public class NativeCameraLauncher extends CordovaPlugin {

    private static final String LOG_TAG = "NativeCameraLauncher";

    private int mQuality;

    private int mTargetWidth;

    private int mTargetHeight;

    private Uri mImageUri;

    private File mPhotoFile;

    private static final String _DATA = "_data";

    private CallbackContext mCallbackContext;

    private String mDate = null;

    private boolean isSquared = false;

    public NativeCameraLauncher() {
    }

    void failPicture(String reason) {
        mCallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, reason));
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        PluginResult.Status status = PluginResult.Status.OK;
        String result = "";
        this.mCallbackContext = callbackContext;
        try {
            if (action.equals("takePicture")) {
                mTargetHeight = 0;
                mTargetWidth = 0;
                mQuality = 80;
                mTargetHeight = args.getInt(4);
                mTargetWidth = args.getInt(3);
                mQuality = args.getInt(0);
                isSquared = args.getBoolean(12);
                takePicture();
                PluginResult r = new PluginResult(PluginResult.Status.NO_RESULT);
                r.setKeepCallback(true);
                callbackContext.sendPluginResult(r);
                return true;
            } else {
                return false;
            }

        } catch (JSONException e) {
            e.printStackTrace();
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.JSON_EXCEPTION));
            return true;
        }
    }

    public void takePicture() {
        // Save the number of images currently on disk for later
        Intent intent = null;
        if (isSquared){
            intent = new Intent(this.cordova.getActivity().getApplicationContext(), SquareCameraActivity.class);
        } else {
            intent = new Intent(this.cordova.getActivity().getApplicationContext(), CameraActivity.class);
        }
        mPhotoFile = createCaptureFile();
        mImageUri = Uri.fromFile(mPhotoFile);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, mImageUri);
        cordova.startActivityForResult((CordovaPlugin) this, intent, 1);
    }

    private File createCaptureFile() {
        File oldFile = new File(getTempDirectoryPath(this.cordova.getActivity().getApplicationContext()), "Pic-" + mDate + ".jpg");
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

        return new File(getTempDirectoryPath(this.cordova.getActivity().getApplicationContext()), "Pic-" + mDate + ".jpg");
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        // If image available
        if (resultCode == Activity.RESULT_OK) {
            int rotate = 0;
            try {
                // Create an ExifHelper to save the exif data that is lost
                // during compression
                ExifHelper exif = new ExifHelper();
                exif.createInFile(getTempDirectoryPath(this.cordova.getActivity().getApplicationContext()) + "/Pic-" + mDate + ".jpg");
                exif.readExifData();
                rotate = exif.getOrientation();

                // Read in bitmap of captured image
                Bitmap bitmap;
                try {
                    bitmap = android.provider.MediaStore.Images.Media.getBitmap(this.cordova.getActivity().getContentResolver(), mImageUri);
                } catch (FileNotFoundException e) {
                    Uri uri = intent.getData();
                    android.content.ContentResolver resolver = this.cordova.getActivity().getContentResolver();
                    bitmap = android.graphics.BitmapFactory.decodeStream(resolver.openInputStream(uri));
                }

                // If bitmap cannot be decoded, this may return null
                if (bitmap == null) {
                    this.failPicture("Error decoding image.");
                    return;
                }

                bitmap = scaleBitmap(bitmap);

                // Add compressed version of captured image to returned media
                // store Uri
                bitmap = getRotatedBitmap(rotate, bitmap, exif);
                Log.i(LOG_TAG, "URI: " + this.mImageUri.toString());
                OutputStream os = this.cordova.getActivity().getContentResolver().openOutputStream(this.mImageUri);
                bitmap.compress(Bitmap.CompressFormat.JPEG, this.mQuality, os);
                os.close();

                // Restore exif data to file
                exif.createOutFile(this.mImageUri.getPath());
                exif.writeExifData();

                // Send Uri back to JavaScript for viewing image
                this.mCallbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, this.mImageUri.toString()));

                bitmap.recycle();
                bitmap = null;
                System.gc();
            } catch (IOException e) {
                e.printStackTrace();
                this.failPicture("Error capturing image.");
            }
        }

        // If cancelled
        else if (resultCode == Activity.RESULT_CANCELED) {
            this.failPicture("Camera cancelled.");
        }

        // If something else
        else {
            this.failPicture("Did not complete!");
        }
    }

    public Bitmap scaleBitmap(Bitmap bitmap) {
        int newWidth = this.mTargetWidth;
        int newHeight = this.mTargetHeight;
        int origWidth = bitmap.getWidth();
        int origHeight = bitmap.getHeight();

        // If no new width or height were specified return the original bitmap
        if (newWidth <= 0 && newHeight <= 0) {
            return bitmap;
        }
        // Only the width was specified
        else if (newWidth > 0 && newHeight <= 0) {
            newHeight = (newWidth * origHeight) / origWidth;
        }
        // only the height was specified
        else if (newWidth <= 0 && newHeight > 0) {
            newWidth = (newHeight * origWidth) / origHeight;
        }
        // If the user specified both a positive width and height
        // (potentially different aspect ratio) then the width or height is
        // scaled so that the image fits while maintaining aspect ratio.
        // Alternatively, the specified width and height could have been
        // kept and Bitmap.SCALE_TO_FIT specified when scaling, but this
        // would result in whitespace in the new image.
        else {
            double newRatio = newWidth / (double) newHeight;
            double origRatio = origWidth / (double) origHeight;

            if (origRatio > newRatio) {
                newHeight = (newWidth * origHeight) / origWidth;
            } else if (origRatio < newRatio) {
                newWidth = (newHeight * origWidth) / origHeight;
            }
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    private Bitmap getRotatedBitmap(int rotate, Bitmap bitmap, ExifHelper exif) {
        Matrix matrix = new Matrix();
        matrix.setRotate(rotate);
        try {
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            exif.resetOrientation();
        } catch (OutOfMemoryError oom) {
            // You can run out of memory if the image is very large:
            // http://simonmacdonald.blogspot.ca/2012/07/change-to-camera-code-in-phonegap-190.html
            // If this happens, simply do not rotate the image and return it unmodified.
            // If you do not catch the OutOfMemoryError, the Android app crashes.
        }
        return bitmap;
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

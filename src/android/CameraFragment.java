package org.apache.cordova.camera;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import java.util.List;

/**
 * Fragment for displaying the mCamera preview.
 *
 * @author Sebastian Kaspari <sebastian@androidzeitgeist.com>
 */
public class CameraFragment extends Fragment implements SurfaceHolder.Callback, Camera.PictureCallback {

    public static final String TAG = "Mustache/CameraFragment";

    private static final int PICTURE_SIZE_MAX_WIDTH = 1280;

    private static final int PREVIEW_SIZE_MAX_WIDTH = 640;

    private int mCameraId = CameraInfo.CAMERA_FACING_BACK;

    private Camera mCamera;

    private SurfaceHolder mSurfaceHolder;

    private CameraFragmentListener mCameraFragmentListener;

    private int mDisplayOrientation;

    private int mLayoutOrientation;

    private CameraOrientationListener mOrientationListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (!(activity instanceof CameraFragmentListener)) {
            throw new IllegalArgumentException("Activity has to implement CameraFragmentListener interface");
        }

        mCameraFragmentListener = (CameraFragmentListener) activity;

        mOrientationListener = new CameraOrientationListener(activity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        CameraPreview previewView = new CameraPreview(getActivity());
        previewView.getHolder().addCallback(this);
        return previewView;
    }

    @Override
    public void onResume() {
        super.onResume();

        mOrientationListener.enable();

        try {
            startCamera();
        } catch (Exception exception) {
            Log.e(TAG, "Can't open camera with id " + mCameraId, exception);
            mCameraFragmentListener.onCameraError();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        mOrientationListener.disable();

        stopCamera();
    }

    /**
     * Start the mCamera preview.
     */
    private synchronized void startCameraPreview() {
        determineDisplayOrientation();
        setupCamera();

        try {
            mCamera.setPreviewDisplay(mSurfaceHolder);
            mCamera.startPreview();
        } catch (Exception exception) {
            Log.e(TAG, "Can't start camera preview due to Exception", exception);
            mCameraFragmentListener.onCameraError();
        }
    }

    private synchronized void stopCameraPreview() {
        try {
            mCamera.stopPreview();
        } catch (Exception exception) {
            Log.i(TAG, "Exception during stopping camera preview");
        }
    }

    /**
     * Determine the current display orientation and rotate the mCamera preview
     * accordingly.
     */
    public void determineDisplayOrientation() {
        CameraInfo cameraInfo = new CameraInfo();
        Camera.getCameraInfo(mCameraId, cameraInfo);

        int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;

        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;

            case Surface.ROTATION_90:
                degrees = 90;
                break;

            case Surface.ROTATION_180:
                degrees = 180;
                break;

            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int displayOrientation;

        if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
            displayOrientation = (cameraInfo.orientation + degrees) % 360;
            displayOrientation = (360 - displayOrientation) % 360;
        } else {
            displayOrientation = (cameraInfo.orientation - degrees + 360) % 360;
        }

        this.mDisplayOrientation = displayOrientation;
        this.mLayoutOrientation = degrees;

        mCamera.setDisplayOrientation(displayOrientation);
    }

    public void setupCamera() {
        Camera.Parameters parameters = mCamera.getParameters();

        Size bestPreviewSize = determineBestPreviewSize(parameters);
        Size bestPictureSize = determineBestPictureSize(parameters);

        parameters.setPreviewSize(bestPreviewSize.width, bestPreviewSize.height);
        parameters.setPictureSize(bestPictureSize.width, bestPictureSize.height);

        mCamera.setParameters(parameters);

        setFlashButtonState();
    }

    private Size determineBestPreviewSize(Camera.Parameters parameters) {
        List<Size> sizes = parameters.getSupportedPreviewSizes();

        return determineBestSize(sizes, PREVIEW_SIZE_MAX_WIDTH);
    }

    private Size determineBestPictureSize(Camera.Parameters parameters) {
        List<Size> sizes = parameters.getSupportedPictureSizes();

        return determineBestSize(sizes, PICTURE_SIZE_MAX_WIDTH);
    }

    protected Size determineBestSize(List<Size> sizes, int widthThreshold) {
        Size bestSize = null;

        for (Size currentSize : sizes) {
            boolean isDesiredRatio = (currentSize.width / 4) == (currentSize.height / 3);
            boolean isBetterSize = (bestSize == null || currentSize.width > bestSize.width);
            boolean isInBounds = currentSize.width <= PICTURE_SIZE_MAX_WIDTH;

            if (isDesiredRatio && isInBounds && isBetterSize) {
                bestSize = currentSize;
            }
        }

        if (bestSize == null) {
            mCameraFragmentListener.onCameraError();

            return sizes.get(0);
        }

        return bestSize;
    }

    /**
     * Take a picture and notify the listener once the picture is taken.
     */
    public void takePicture() {
        mOrientationListener.rememberOrientation();

        mCamera.takePicture(null, null, this);
    }

    public void swapCamera() {
        if (Camera.getNumberOfCameras() > 1 && mCameraId < Camera.getNumberOfCameras() - 1) {
            mCameraId = mCameraId + 1;
        } else {
            mCameraId = CameraInfo.CAMERA_FACING_BACK;
        }
        startCamera();
    }

    public void swapFlash() {
        Camera.Parameters params = mCamera.getParameters();
        List<String> flashModes = params.getSupportedFlashModes();
        if (flashModes == null || flashModes.size() == 0) {
            return;
        }

        String currentFlashMode = params.getFlashMode();
        String newFlashMode = currentFlashMode;
        if (currentFlashMode.equals(Camera.Parameters.FLASH_MODE_OFF) && flashModes.contains(Camera.Parameters.FLASH_MODE_AUTO)) {
            newFlashMode = Camera.Parameters.FLASH_MODE_AUTO;
        } else if (currentFlashMode.equals(Camera.Parameters.FLASH_MODE_OFF) && flashModes.contains(Camera.Parameters.FLASH_MODE_ON)) {
            newFlashMode = Camera.Parameters.FLASH_MODE_ON;
        } else if (currentFlashMode.equals(Camera.Parameters.FLASH_MODE_AUTO) && flashModes.contains(Camera.Parameters.FLASH_MODE_ON)) {
            newFlashMode = Camera.Parameters.FLASH_MODE_ON;
        } else if (currentFlashMode.equals(Camera.Parameters.FLASH_MODE_AUTO) && flashModes.contains(Camera.Parameters.FLASH_MODE_OFF)) {
            newFlashMode = Camera.Parameters.FLASH_MODE_OFF;
        } else if (currentFlashMode.equals(Camera.Parameters.FLASH_MODE_ON) && flashModes.contains(Camera.Parameters.FLASH_MODE_OFF)) {
            newFlashMode = Camera.Parameters.FLASH_MODE_OFF;
        } else if (currentFlashMode.equals(Camera.Parameters.FLASH_MODE_ON) && flashModes.contains(Camera.Parameters.FLASH_MODE_AUTO)) {
            newFlashMode = Camera.Parameters.FLASH_MODE_AUTO;
        }

        params.setFlashMode(newFlashMode);
        mCamera.setParameters(params);
        setFlashButtonState();
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

        int rotation = (mDisplayOrientation + mOrientationListener.getRememberedOrientation() + mLayoutOrientation) % 360;

        if (rotation != 0) {
            Bitmap oldBitmap = bitmap;

            Matrix matrix = new Matrix();
            matrix.postRotate(rotation);

            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
            oldBitmap.recycle();
        }

        bitmap = cropBitmapToSquare(bitmap);

        mCameraFragmentListener.onPictureTaken(bitmap);
    }

    private Bitmap cropBitmapToSquare(Bitmap source) {
        Bitmap cropped;
        int h = source.getHeight();
        int w = source.getWidth();
        if (w >= h) {
            cropped = Bitmap.createBitmap(source, w / 2 - h / 2, 0, h, h);
        } else {
            cropped = Bitmap.createBitmap(source, 0, h / 2 - w / 2, w, w);
        }
        return cropped;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        this.mSurfaceHolder = holder;

        startCameraPreview();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // The interface forces us to have this method but we don't need it
        // up to now.
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // We don't need to handle this case as the fragment takes care of
        // releasing the mCamera when needed.
    }

    private void startCamera() {
        if (mCamera != null) {
            stopCamera();
        }
        mCamera = Camera.open(mCameraId);
        startCameraPreview();
    }

    private void stopCamera() {
        stopCameraPreview();
        mCamera.release();
    }

    private void setFlashButtonState() {
        List<String> flashModes = mCamera.getParameters().getSupportedFlashModes();
        ImageButton flashModeButton = (ImageButton) getActivity()
                .findViewById(getResources().getIdentifier("flash_mode_button", "id", getActivity().getPackageName()));
        if (null == flashModes || flashModes.size() == 0) {
            flashModeButton.setVisibility(View.INVISIBLE);
        } else {
            flashModeButton.setVisibility(View.VISIBLE);
            if (mCamera.getParameters().getFlashMode().equals(Camera.Parameters.FLASH_MODE_OFF)) {
                flashModeButton.setBackgroundResource(getResources().getIdentifier("@drawable/btn_flash_no", null, getActivity().getPackageName()));
            }
            if (mCamera.getParameters().getFlashMode().equals(Camera.Parameters.FLASH_MODE_ON)) {
                flashModeButton.setBackgroundResource(getResources().getIdentifier("@drawable/btn_flash_on", null, getActivity().getPackageName()));
            }
            if (mCamera.getParameters().getFlashMode().equals(Camera.Parameters.FLASH_MODE_AUTO)) {
                flashModeButton.setBackgroundResource(getResources().getIdentifier("@drawable/btn_flash_auto", null, getActivity().getPackageName()));
            }
        }
    }
}

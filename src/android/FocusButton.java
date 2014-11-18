package org.apache.cordova.camera;

import java.util.List;
import java.util.ArrayList;

import android.graphics.Rect;
import android.hardware.Camera.Parameters;
import android.view.View;
import android.view.MotionEvent;
import android.hardware.Camera;
import android.widget.ImageView;
import android.util.Log;

import org.apache.cordova.camera.Utils;

public class FocusButton implements View.OnTouchListener {

    private int mScreenWidth;

    private int mScreenHeight;

    private float mViewFinderHalfPx;

    private float mDensity;

    private Camera mCamera;

    private ImageView mViewFinder;

    public FocusButton(int screenWidth, int screenHeight, float density, Camera camera, ImageView viewfinder) {
        mScreenWidth = screenWidth;
        mScreenHeight = screenHeight;
        mDensity = density;
        mCamera = camera;
        mViewFinder = viewfinder;
        mViewFinderHalfPx = Utils.pxFromDp(mDensity, 72) / 2;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        float x = 0;
        float y = 0;
        Rect focusRect;
        y = (((event.getX() * 2000) / mScreenWidth) - 1000) * -1;
        x = (((event.getY() * 2000) / mScreenHeight) - 1000);

        if ((int) x - 100 > -1000 && (int) x + 100 < 1000 && (int) y - 100 > -1000 && (int) y + 100 < 1000) {
            focusRect = new Rect((int) x - 100, (int) y - 100, (int) x + 100, (int) y + 100);
        } else {
            focusRect = new Rect(-100, -100, 100, 100);
        }

        if (mCamera == null) {
            return true;
        }

        Parameters parameters = mCamera.getParameters();

        if (parameters.getMaxNumFocusAreas() > 0) {

            if (event.getX() - mViewFinderHalfPx < 0) {
                mViewFinder.setX(0);
            } else if (event.getX() + mViewFinderHalfPx > mScreenWidth) {
                mViewFinder.setX(mScreenWidth - mViewFinderHalfPx * 2);
            } else {
                mViewFinder.setX(event.getX() - mViewFinderHalfPx);
            }

            if (event.getY() - mViewFinderHalfPx < 0) {
                mViewFinder.setY(0);
            } else if (event.getY() + mViewFinderHalfPx > mScreenHeight - Utils.pxFromDp(mDensity, 125)) {
                mViewFinder.setY((mScreenHeight - Utils.pxFromDp(mDensity, 125)) - mViewFinderHalfPx * 2);
            } else {
                mViewFinder.setY(event.getY() - mViewFinderHalfPx);
            }

            List<Camera.Area> focusArea = new ArrayList<Camera.Area>();
            focusArea.add(new Camera.Area(focusRect, 750));
            parameters.setFocusAreas(focusArea);
            if (parameters.getMaxNumMeteringAreas() > 0) {
                parameters.setMeteringAreas(focusArea);
            }

            mCamera.setParameters(parameters);
        }
        return true;
    }
}
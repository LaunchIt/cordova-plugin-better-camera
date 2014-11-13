package org.apache.cordova.camera;

import android.graphics.Bitmap;

/**
 * Listener interface that has to be implemented by activities using
 * {@link CameraFragment} instances.
 *
 * @author Sebastian Kaspari <sebastian@androidzeitgeist.com>
 */
public interface CameraFragmentListener {

    public void onCameraError();

    public void onPictureTaken(Bitmap bitmap);
}

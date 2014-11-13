# Launch.it Cordova Camera plugin

This is a fork of the official Apache Cordova Camera plugin with a few tweaks for Android.

The initial Android code is based on [https://github.com/shaithana/cordova-plugin-wezka-nativecamera](https://github.com/shaithana/cordova-plugin-wezka-nativecamera)

### Usage
We keep the original interface, in a couple different flavor

#### Android system camera

```javascript
navigator.camera.getPicture(onSuccess, onFail, {
                quality: 50,
                destinationType: Camera.DestinationType.FILE_URI
            });
```

#### Integrate Camera Activity
```javascript
navigator.camera.getPicture(onSuccess, onFail, {
                quality: 50,
                destinationType: Camera.DestinationType.FILE_URI.
                foreground : true
            });
```

### Example
A working ready-to-go example is [here](https://github.com/LaunchIt/cordova-plugin-better-camera-example)

### Roadmap
* Squared camera preview and squared picture
* Custom camera activity UI

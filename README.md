# Launch.it Cordova Camera plugin

This is a fork of the official Apache Cordova Camera plugin with a few tweaks for Android.

The initial Android code is based on [https://github.com/shaithana/cordova-plugin-wezka-nativecamera](https://github.com/shaithana/cordova-plugin-wezka-nativecamera)

The squared camera code is based on [https://github.com/charlesbedrosian/Instant-Mustache](https://github.com/charlesbedrosian/Instant-Mustache)

## Usage
We kept the original interface as it is, but we add a couple params:

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

#### Integrate Squared Camera Activity
```javascript
navigator.camera.getPicture(onSuccess, onFail, {
                quality: 50,
                destinationType: Camera.DestinationType.FILE_URI.
                squared : true
            });
```

## Example
A ready-to-go example is [here](https://github.com/LaunchIt/cordova-plugin-better-camera-example)

## Roadmap
* Custom camera activity UI

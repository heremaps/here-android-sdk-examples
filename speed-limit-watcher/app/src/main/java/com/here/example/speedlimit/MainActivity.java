package com.here.example.speedlimit;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.support.v4.app.ActivityCompat;
import android.widget.Toast;

import com.here.android.mpa.common.ApplicationContext;
import com.here.android.mpa.common.MapEngine;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.common.PositioningManager;
import com.here.android.mpa.guidance.NavigationManager;


public class MainActivity extends AppCompatActivity {

    private final static int REQUEST_CODE_ASK_PERMISSIONS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // check Android version to request permissions
        if (shouldCheckPermissions()) {
            requestPermissions();
        } else {
            initSDK();
        }

    }

    private boolean shouldCheckPermissions() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    private void requestPermissions() {
        String[] permissions = getRequiredPermissions(this);

        ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_ASK_PERMISSIONS);
    }

    private String[] getRequiredPermissions (Context context) {

        try {
            return context
                    .getPackageManager()
                    .getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS)
                    .requestedPermissions;
        }
        catch (PackageManager.NameNotFoundException e) {
            //handle or raise error
        }
        return new String[0];
    }

    private void initSDK() {
        ApplicationContext appContext = new ApplicationContext(this);

        MapEngine.getInstance().init(appContext, new OnEngineInitListener() {
            @Override
            public void onEngineInitializationCompleted(Error error) {
                if (error == Error.NONE) {

                    startPositioningManager();
                    startNavigationManager();
                    activateSpeedLimitFragment();

                } else {
                    //handle error here
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS: {
                for (int index = 0; index < permissions.length; index++) {
                    if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {

                        /**
                         * If the user turned down the permission request in the past and chose the
                         * Don't ask again option in the permission request system dialog.
                         */
                        if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                                permissions[index])) {
                            Toast.makeText(this,
                                    "Required permission " + permissions[index] + " not granted. "
                                            + "Please go to settings and turn on for sample app",
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this,
                                    "Required permission " + permissions[index] + " not granted",
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                }

                /**
                 * All permission requests are being handled.Create map engine view.Please note
                 * the HERE SDK requires all permissions defined above to operate properly.
                 */
                initSDK();

                break;
            }
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (MapEngine.isInitialized()) {
            MapEngine.getInstance().onResume();
            startPositioningManager();
        }
    }

    @Override
    protected void onPause() {
        if (MapEngine.isInitialized()) {
            stopPositioningManager();
        }
        super.onPause();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void startPositioningManager() {
        boolean positioningManagerStarted = PositioningManager.getInstance().start(PositioningManager.LocationMethod.GPS_NETWORK);

        if (!positioningManagerStarted) {
            //handle error here
        }
    }

    private void stopPositioningManager() {
        PositioningManager.getInstance().stop();
    }

    private void startNavigationManager() {
        NavigationManager.Error navError = NavigationManager.getInstance().startTracking();

        if (navError != NavigationManager.Error.NONE) {
            //handle error navError.toString());
        }

    }

    private void activateSpeedLimitFragment() {
        SpeedLimitFragment speedLimitFragment = (SpeedLimitFragment)
                getSupportFragmentManager().findFragmentById(R.id.speedLimitFragment);

        if (speedLimitFragment != null) {
            speedLimitFragment.startListeners();
        }
    }

}
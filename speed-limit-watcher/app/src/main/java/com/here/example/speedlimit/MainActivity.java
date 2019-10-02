package com.here.example.speedlimit;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.here.android.mpa.common.ApplicationContext;
import com.here.android.mpa.common.MapEngine;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.common.PositioningManager;
import com.here.android.mpa.guidance.NavigationManager;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private final static int REQUEST_CODE_ASK_PERMISSIONS = 1;

    private static final String[] RUNTIME_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // check Android version to request permissions
        if (hasPermissions(this, RUNTIME_PERMISSIONS)) {
            initSDK();
        } else {
            ActivityCompat
                    .requestPermissions(this, RUNTIME_PERMISSIONS, REQUEST_CODE_ASK_PERMISSIONS);
        }
    }

    private void initSDK() {
        // Set path of disk cache
        String diskCacheRoot = this.getFilesDir().getPath()
                + File.separator + ".isolated-here-maps";
        // Retrieve intent name from manifest
        String intentName = "";
        try {
            ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(),
                    PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;
            intentName = bundle.getString("INTENT_NAME");
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("MainActivity",
                    "Failed to find intent name, NameNotFound: " + e.getMessage());
        }
        boolean success = com.here.android.mpa.common.MapSettings.setIsolatedDiskCacheRootPath(
                diskCacheRoot, intentName);
        if (!success) {
            Toast.makeText(this, "Operation 'setIsolatedDiskCacheRootPath' was not successful",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        ApplicationContext appContext = new ApplicationContext(this);
        MapEngine.getInstance().init(appContext, new OnEngineInitListener() {
            @Override
            public void onEngineInitializationCompleted(Error error) {
                if (error == Error.NONE) {

                    startPositioningManager();
                    startNavigationManager();
                    activateSpeedLimitFragment();

                } else {
                    Log.e("MainActivity", " init error: " + error + ", " + error.getDetails(),
                            error.getThrowable());

                    new AlertDialog.Builder(MainActivity.this).setMessage(
                            "Error : " + error.name() + "\n\n" + error.getDetails())
                            .setTitle(R.string.engine_init_error)
                            .setNegativeButton(android.R.string.cancel,
                                               new DialogInterface.OnClickListener() {
                                                   @Override
                                                   public void onClick(
                                                           DialogInterface dialog,
                                                           int which) {
                                                       MainActivity.this.finish();
                                                   }
                                               }).create().show();
                }
            }
        });
    }

    /**
     * Only when the app's target SDK is 23 or higher, it requests each dangerous permissions it
     * needs when the app is running.
     */
    private static boolean hasPermissions(Context context, String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS: {
                for (int index = 0; index < permissions.length; index++) {
                    if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {

                        /*
                         * If the user turned down the permission request in the past and chose the
                         * Don't ask again option in the permission request system dialog.
                         */
                        if (!ActivityCompat
                                .shouldShowRequestPermissionRationale(this, permissions[index])) {
                            Toast.makeText(this, "Required permission " + permissions[index]
                                                   + " not granted. "
                                                   + "Please go to settings and turn on for sample app",
                                           Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "Required permission " + permissions[index]
                                    + " not granted", Toast.LENGTH_LONG).show();
                        }
                    }
                }

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
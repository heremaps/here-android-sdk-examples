/*
 * Copyright (c) 2020 HERE Europe B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.here.android.example.backgroundpositioningexample;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.Nullable;

import com.here.android.mpa.common.ApplicationContext;
import com.here.android.mpa.common.LocationDataSource;
import com.here.android.mpa.common.LocationDataSourceHERE;
import com.here.android.mpa.common.MapEngine;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.common.PositioningManager;

/**
 * Positioning service that is run as foreground service to ensure map engine existence after
 * the activity has been destroyed.
 */
public class PositioningService extends Service {

    private static final String TAG = PositioningService.class.getSimpleName();

    // Notifications definitions.

    /** Android notification ID. */
    /** Action to get controller binder API. */
    private static final String ACTION_GET_API = "getApi";
    /** Extras key for content pending intent value. */
    private static final String KEY_CONTENT_INTENT = "contentIntent";
    /** Flag that states if service has been started and running. */
    private static boolean mRunning;
    /** Service event listener reference. */
    private IPositioningServiceListener mListener;
    /** */
    private NotificationUtils mNotificationUtils;

    /**
     * Abstract base service connection implementation.
     */
    private static abstract class ConnectionBase implements ServiceConnection {

        /** True when service is connected. */
        private volatile boolean mConnected;

        /**
         * Bind to the service.
         * @param context Android context instance.
         * @param intent Binding target intent.
         * @param flags Bind flags.
         * @return True if bind was successfully called.
         */
        boolean bind(Context context, Intent intent, int flags) {
            if (mConnected) {
                return true;
            }
            mConnected = true;
            return context.bindService(intent, this, flags);
        }

        /**
         * Unbind service.
         * @param context Android context instance.
         */
        public void unBind(Context context) {
            if (!mConnected) {
                return;
            }
            mConnected = false;
            context.unbindService(this);
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mConnected = true;
            onBaseConnected(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mConnected = false;
            onDisconnected();
        }

        /**
         * This callback is called when service connection succeeds.
         * @param service Service binder instance.
         */
        protected abstract void onBaseConnected(IBinder service);

        /**
         * This callback is called when service is disconnected.
         */
        protected abstract void onDisconnected();
    }

    /**
     * Abstract service controller implementation.
     */
    public abstract static class ApiConnection extends ConnectionBase {

        @Override
        protected void onBaseConnected(IBinder service) {
            onConnected(IPositioningServiceControl.Stub.asInterface(service));
        }

        /**
         * This callback is called when valid collector service controller AIDL instance when service connection
         * is made.
         * @param controller Service controller AIDL instance.
         */
        public abstract void onConnected(IPositioningServiceControl controller);
    }

    /**
     * Start foreground service.
     * @param context
     */
    public static void start(Context context, PendingIntent contentIntent) {
        if (mRunning) {
            return;
        }
        final Intent intent = new Intent(context, PositioningService.class);
        intent.putExtra(KEY_CONTENT_INTENT, contentIntent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        if (ACTION_GET_API.equals(intent.getAction())) {
            return new IPositioningServiceControl.Stub() {
                @Override
                public void setListener(IPositioningServiceListener listener) throws RemoteException {
                    mListener = listener;
                    if (mListener == null) {
                        throw new IllegalArgumentException("listener cannot be null");
                    }
                    if (MapEngine.isInitialized()) {
                        try {
                            mListener.onEngineIntialized();
                        } catch (RemoteException ex) {
                            Log.e(TAG, ex.getMessage());
                        }
                    }
                }

                @Override
                public void startBackground() throws RemoteException {
                    final PositioningManager posManager = PositioningManager.getInstance();
                    if (posManager != null) {
                        if (posManager.start(PositioningManager.LocationMethod.GPS_NETWORK)) {
                            mNotificationUtils.updateNotification(
                                    R.drawable.ic_playing,
                                    R.string.notificationTitle,
                                    R.string.notificationBgPositioningStarted);
                        }
                    }
                }

                @Override
                public void stopBackground() throws RemoteException {
                    final PositioningManager posManager = PositioningManager.getInstance();
                    if (posManager != null) {
                        posManager.stop();
                        mNotificationUtils.updateNotification(
                                R.drawable.ic_paused,
                                R.string.notificationTitle,
                                R.string.notificationMapEngineReady);
                    }
                    mListener = null;
                }
            };
        }
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mRunning = false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_STICKY;
        }
        initMapEngine();
        mRunning = true;
        final PendingIntent contentIntent = intent.getParcelableExtra(KEY_CONTENT_INTENT);
        mNotificationUtils = new NotificationUtils(getApplicationContext(), contentIntent);
        return startForegroundService();
    }

    /**
     * Start foreground service.
     * @return Start flags, which will be passed to OS.
     */
    private int startForegroundService() {
        final NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            stopSelf();
            return 0;
        }
        mNotificationUtils.setupNotificationChannel();
        startForeground(
                NotificationUtils.getNotificationId(),
                mNotificationUtils.createNotification(
                        R.drawable.ic_stopped,
                        R.string.notificationTitle,
                        R.string.notificationServiceStarted));
        return START_STICKY;
    }

    /**
     * Initialize HERE SDK map engine.
     */
    private void initMapEngine() {
        if (MapEngine.isInitialized()) {
            return;
        }
        final MapEngine engine = MapEngine.getInstance();
        engine.init(new ApplicationContext(getApplicationContext()), new OnEngineInitListener() {
            @Override
            public void onEngineInitializationCompleted(Error error) {
                if (error == OnEngineInitListener.Error.NONE) {
                    mNotificationUtils.updateNotification(
                            R.drawable.ic_paused,
                            R.string.notificationTitle,
                            R.string.notificationMapEngineReady);
                    if (mListener != null) {
                        try {
                            mListener.onEngineIntialized();
                        } catch (RemoteException ex) {
                            // ignored
                        }
                    }
                    final PositioningManager posManager = PositioningManager.getInstance();
                    final LocationDataSource hereLocation = LocationDataSourceHERE.getInstance();
                    if (hereLocation == null) {
                        Log.e(TAG, "initMapEngine: failed to instantiate HERE location");
                        return;
                    }
                    posManager.setDataSource(hereLocation);
                } else {
                    Log.e(TAG, "initMapEngine: MapEngine.init failed: " + error.getDetails());
                }
            }
        });
    }

    /**
     * Bind service controller.
     * @param context Android context instance.
     * @param connection Service controller connection instance. Service connection interface will be passed to this
     * instance once the binding has completed.
     * @return True if bind is called successfully.
     */
    public static boolean bind(Context context, ApiConnection connection) {
        final Intent intent = new Intent(context, PositioningService.class);
        intent.setAction(ACTION_GET_API);
        return connection.bind(context, intent, Context.BIND_NOT_FOREGROUND);
    }
}

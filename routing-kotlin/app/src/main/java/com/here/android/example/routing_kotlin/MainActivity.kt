/*
 * Copyright (c) 2011-2020 HERE Europe B.V.
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

package com.here.android.example.routing_kotlin

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Main activity which launches map view and handles Android run-time requesting permission.
 */

class MainActivity : AppCompatActivity() {
    private val REQUEST_CODE_ASK_PERMISSIONS = 1

    private val RUNTIME_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.ACCESS_NETWORK_STATE
    )

    lateinit var m_mapFragment: MapFragmentView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestPermissions()
    }

    private fun requestPermissions() {
        if (!hasPermissions()) {
            ActivityCompat.requestPermissions(
                this,
                RUNTIME_PERMISSIONS, REQUEST_CODE_ASK_PERMISSIONS
            )
        } else {
            launchMapFragmentView()
        }
    }

    // Checks whether permission represented by this string is granted
    private fun String.permissionGranted(ctx: Context) =
        ContextCompat.checkSelfPermission(ctx, this) == PackageManager.PERMISSION_GRANTED

    private fun hasPermissions(): Boolean {
        /**
         * Only when the app's target SDK is 23 or higher, it requests each dangerous permissions it
         * needs when the app is running.
         */
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true
        }

        return RUNTIME_PERMISSIONS.count { !it.permissionGranted(this) } == 0
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_CODE_ASK_PERMISSIONS -> {
                for (index in RUNTIME_PERMISSIONS.indices) {
                    if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                        var notGrantedMessage =
                            "Required permission ${permissions[index]} not granted."

                        /**
                         * If the user turned down the permission request in the past and chose the
                         * Don't ask again option in the permission request system dialog.
                         */
                        if (!ActivityCompat.shouldShowRequestPermissionRationale(
                                this,
                                permissions[index]
                            )
                        ) {
                            notGrantedMessage += "Please go to settings and turn on for sample app."
                        }

                        Toast.makeText(this, notGrantedMessage, Toast.LENGTH_LONG).show();
                    }
                }

                /**
                 * All permission requests are being handled. Create map fragment view. Please note
                 * the HERE Mobile SDK requires all permissions defined above to operate properly.
                 */
                launchMapFragmentView()
            }
            else -> {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }

    private fun launchMapFragmentView() {
        // All permission requests are being handled. Create map fragment view. Please note
        // the HERE Mobile SDK requires all permissions defined above to operate properly.
        m_mapFragment = MapFragmentView(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return m_mapFragment.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return m_mapFragment.onOptionsItemSelected(item)
    }
}

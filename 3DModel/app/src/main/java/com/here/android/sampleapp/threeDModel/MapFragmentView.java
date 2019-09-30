/*
 * Copyright (c) 2011-2019 HERE Europe B.V.
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

package com.here.android.sampleapp.threeDModel;

import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.Image;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.mapping.LocalMesh;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.SupportMapFragment;
import com.here.android.mpa.mapping.MapLocalModel;
import com.here.android.mpa.mapping.MapModelObject;
import com.owens.oobjloader.builder.Build;
import com.owens.oobjloader.builder.Face;
import com.owens.oobjloader.builder.FaceVertex;
import com.owens.oobjloader.builder.Group;
import com.owens.oobjloader.builder.Material;
import com.owens.oobjloader.parser.Parse;

import android.support.v7.app.AppCompatActivity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

/**
 * This class encapsulates the properties and functionality of the Map view.It uses some classes
 * from a OBJ loader @ https://github.com/seanrowens/oObjLoader to parse the OBJ file. After that,
 * it extracts data from the loader and convert it into what HERE Android SDK APIs accept.
 */
public class MapFragmentView {
    private SupportMapFragment m_mapFragment;
    private AppCompatActivity m_activity;
    private Map m_map;
    private Button m_displayButton;
    private ArrayList<MapLocalModel> m_modelList;

    public MapFragmentView(AppCompatActivity activity) {
        m_activity = activity;
        initMapFragment();
        initDisplayButton();
    }

    private SupportMapFragment getMapFragment() {
        return (SupportMapFragment) m_activity.getSupportFragmentManager().findFragmentById(R.id.mapfragment);
    }

    private void initMapFragment() {
        /* Locate the mapFragment UI element */
        m_mapFragment = getMapFragment();

        // Set path of disk cache
        String diskCacheRoot = m_activity.getFilesDir().getPath()
                + File.separator + ".isolated-here-maps";
        // Retrieve intent name from manifest
        String intentName = "";
        try {
            ApplicationInfo ai =
                    m_activity.getPackageManager().getApplicationInfo(m_activity.getPackageName(),
                                                                      PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;
            intentName = bundle.getString("INTENT_NAME");
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(this.getClass().toString(),
                  "Failed to find intent name, NameNotFound: " + e.getMessage());
        }

        boolean success =
                com.here.android.mpa.common.MapSettings.setIsolatedDiskCacheRootPath(diskCacheRoot,
                                                                                     intentName);
        if (!success) {
            // Setting the isolated disk cache was not successful, please check if the path is valid and
            // ensure that it does not match the default location
            // (getExternalStorageDirectory()/.here-maps).
            // Also, ensure the provided intent name does not match the default intent name.
        } else {
            if (m_mapFragment != null) {
                /* Initialize the SupportMapFragment, results will be given via the called back. */
                m_mapFragment.init(new OnEngineInitListener() {
                    @Override
                    public void onEngineInitializationCompleted(OnEngineInitListener.Error error) {

                        if (error == Error.NONE) {
                            m_map = m_mapFragment.getMap();
                            m_map.setCenter(new GeoCoordinate(49.259149, -123.008555),
                                            Map.Animation.LINEAR);
                            m_map.setZoomLevel(13.2);

                        } else {
                            Toast.makeText(m_activity,
                                           "ERROR: Cannot initialize Map with error " + error,
                                           Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        }
    }

    private void initDisplayButton() {
        m_displayButton = (Button) m_activity.findViewById(R.id.displayButton);
        m_displayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Trigger a 3D model loader task by specifying the name of the OBJ file
                new Handle3DModelTask().execute("lexus_hs.obj");
            }
        });

    }

    private class Handle3DModelTask extends AsyncTask<String, Void, ArrayList<MapLocalModel>> {

        @Override
        protected void onPreExecute() {
            Toast.makeText(m_activity, "Start parsing OBJ file...", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected ArrayList<MapLocalModel> doInBackground(String... fileName) {
            return construct3DModel(fileName);
        }

        @Override
        protected void onPostExecute(ArrayList<MapLocalModel> modelList) {
            // Clear the map if there are any existing models
            if (m_modelList != null) {
                for (MapLocalModel model : m_modelList) {
                    m_map.removeMapObject(model);
                }
            }

            // Display the model on map
            for (MapLocalModel model : modelList) {
                model.setAnchor(m_map.getCenter());
                m_map.addMapObject(model);
                m_map.setZoomLevel(18);
                m_map.setTilt(60f);
            }
            m_modelList = modelList;

            Toast.makeText(m_activity, "3D model displayed successfully!", Toast.LENGTH_SHORT)
                    .show();
        }

        private ArrayList<MapLocalModel> construct3DModel(String[] path) {
            // Parsing OBJ file
            Build builder = new Build();
            try {
                Parse parser = new Parse(builder, m_activity, path[0]);
            } catch (IOException e) {
                Toast.makeText(m_activity, e.getMessage(), Toast.LENGTH_SHORT).show();
            }

            ArrayList<MapLocalModel> modelList = new ArrayList<>();
            // Iterate each group defined in the OBJ file to construct a model
            for (java.util.Map.Entry<String, Group> entry : builder.groups.entrySet()) {
                Group group = entry.getValue();

                // Build the mesh first
                MapLocalModel model = constructMesh(group);

                // Read material data from the file
                Material currentMat = builder.materialLib.get(group.getMaterialName());
                model = configureModelMaterial(model, currentMat);

                modelList.add(model);
            }
            return modelList;
        }

        @NonNull
        private MapLocalModel constructMesh(Group group) {
            // Allocate 3 buffers to store data
            FloatBuffer verticesBuffer = FloatBuffer.allocate(group.getVertIndicesList().size());
            FloatBuffer textCoordBuffer = FloatBuffer
                    .allocate(group.getVertIndicesList().size() / 3 * 2);
            IntBuffer vertIndicesBuffer = IntBuffer.allocate(group.getVertIndicesList().size());

            // The OBJ loader already generated faces with corresponding vertices and texture
            // coordinates which are specified in the OBJ file, so we will need to iterate each
            // face to read these data.
            int offset = 0;
            for (Face face : group.getFaceList()) {
                for (FaceVertex fv : face.vertices) {
                    verticesBuffer.put(fv.v.x);
                    verticesBuffer.put(fv.v.y);
                    verticesBuffer.put(fv.v.z);
                    if (fv.t != null) {
                        textCoordBuffer.put(fv.t.u);
                        // OpenGL considers (0,0) to be bottom left, while OBJ file considers
                        // (0,0) to be top left.
                        textCoordBuffer.put(1 - fv.t.v);
                    }
                }

                // All faces need to be decomposed into triangles.The vertex indices are ordered
                // in clockwise direction.Because the vertices were already put into buffer in
                // the order specified in OBJ file,so the an offset should also be added to the
                // vertex indices.

                // Triangles
                vertIndicesBuffer.put(0 + offset);
                vertIndicesBuffer.put(1 + offset);
                vertIndicesBuffer.put(2 + offset);

                // Quadragons.Decomposed into 2 triangles.
                if (face.vertices.size() >= 4) {
                    vertIndicesBuffer.put(0 + offset);
                    vertIndicesBuffer.put(2 + offset);
                    vertIndicesBuffer.put(3 + offset);
                }

                // Pentagons.Decomposed into 3 triangles.
                if (face.vertices.size() >= 5) {
                    vertIndicesBuffer.put(0 + offset);
                    vertIndicesBuffer.put(3 + offset);
                    vertIndicesBuffer.put(4 + offset);
                }

                offset += face.vertices.size();
            }

            // Create model
            LocalMesh mesh = new LocalMesh();
            mesh.setVertices(verticesBuffer);
            mesh.setVertexIndices(vertIndicesBuffer);
            mesh.setTextureCoordinates(textCoordBuffer);
            MapLocalModel model = new MapLocalModel();
            model.setMesh(mesh);
            return model;
        }

        private MapLocalModel configureModelMaterial(MapLocalModel model, Material currentMat) {
            if (currentMat.mapKdFilename != null) {
                Image img = new Image();
                try {
                    img.setImageAsset(currentMat.mapKdFilename);
                } catch (IOException e) {
                    Toast.makeText(m_activity, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                model.setTexture(img);
            }

            MapModelObject.PhongMaterial mat = new MapModelObject.PhongMaterial();
            int red = ((int) (currentMat.kd.rx * 256)) << 16;
            int green = ((int) (currentMat.kd.gy * 256)) << 8;
            int blue = (int) (currentMat.kd.bz * 256);
            mat.setDiffuseColor(0xFF << 24 | red | green | blue);

            red = ((int) (currentMat.ka.rx * 256)) << 16;
            green = ((int) (currentMat.ka.gy * 256)) << 8;
            blue = (int) (currentMat.ka.bz * 256);
            mat.setAmbientColor(0xFF << 24 | red | green | blue);
            model.setMaterial(mat);
            return model;
        }
    }
}

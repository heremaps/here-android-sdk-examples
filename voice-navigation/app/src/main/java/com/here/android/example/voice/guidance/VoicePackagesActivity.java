/*
 * Copyright (c) 2011-2021 HERE Europe B.V.
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

package com.here.android.example.voice.guidance;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.here.android.mpa.guidance.VoiceCatalog;
import com.here.android.mpa.guidance.VoicePackage;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class VoicePackagesActivity extends AppCompatActivity {
    private ProgressBar m_progressBar;
    private RecyclerView m_packagesView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_packages);
        setTitle(R.string.voice_packages);

        m_progressBar = findViewById(R.id.progressBar);
        m_packagesView = findViewById(R.id.voicePackagesView);
        m_packagesView.setHasFixedSize(true);
        m_packagesView.setLayoutManager(new LinearLayoutManager(this));

        downloadVoiceCatalog();
    }

    private void downloadVoiceCatalog() {
        VoiceCatalog voiceCatalog = VoiceCatalog.getInstance();

        // Download the catalog of voices if we haven't done so already.
        if (voiceCatalog.getCatalogList().isEmpty()) {
            voiceCatalog.downloadCatalog(new VoiceCatalog.OnDownloadDoneListener() {
                @Override public void onDownloadDone(VoiceCatalog.Error error) {
                    m_progressBar.setVisibility(View.GONE);
                    if (error == VoiceCatalog.Error.NONE) {
                        refreshVoicePackageList();
                    } else {
                        Toast.makeText(VoicePackagesActivity.this,
                                       "Download catalog failed: " + error.toString(),
                                       Toast.LENGTH_LONG).show();
                    }
                }
            });

            m_progressBar.setIndeterminate(true);
            m_progressBar.setVisibility(View.VISIBLE);
        }

        refreshVoicePackageList();
    }

    private void refreshVoicePackageList() {
        m_packagesView.setAdapter(new VoicePackagesAdapter(this));
    }

    static class VoicePackagesAdapter
            extends RecyclerView.Adapter<VoicePackagesAdapter.VoiceViewHolder> {
        private Context m_context;
        private LayoutInflater m_inflater;
        private List<VoicePackage> m_packages;

        public VoicePackagesAdapter(Context context) {
            m_context = context;
            m_inflater = LayoutInflater.from(context);
            // get a list of packages available for download
            m_packages = VoiceCatalog.getInstance().getCatalogList();
        }

        @NonNull
        @Override
        public VoiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = m_inflater.inflate(R.layout.voice_package_item, parent, false);
            return new VoiceViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final VoiceViewHolder holder, final int position) {
            final VoicePackage voicePackage = m_packages.get(position);
            holder.m_isDownloaded.setChecked(voicePackage.isLocal());
            holder.m_idView.setText(Long.toString(voicePackage.getId()));
            holder.m_nameView.setText(voicePackage.getName());
            holder.m_marcView.setText(voicePackage.getMarcCode());
            holder.m_languageView.setText(voicePackage.getLocalizedLanguage());
            holder.m_typeView.setText(voicePackage.getLocalizedType());
            holder.m_sizeView.setText(String.format("%.2f Mb", voicePackage.getDownloadSize()));
            holder.m_ttsView.setText("TTS : " + voicePackage.isTts());

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final VoiceCatalog voiceCatalog = VoiceCatalog.getInstance();
                    if (voiceCatalog.isDownloading()) {
                        return;
                    }

                    // Check if the package has been already downloaded
                    if (voicePackage.isLocal()) {
                        new AlertDialog.Builder(m_context)
                                .setMessage(R.string.remove_package).setPositiveButton(
                                android.R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // Uninstall the package
                                        voiceCatalog.deleteVoiceSkin(voicePackage.getId());
                                        m_packages = VoiceCatalog.getInstance().getCatalogList();
                                        notifyItemChanged(position);
                                    }
                                }).setNegativeButton(android.R.string.cancel, null).create().show();
                    } else {
                        new AlertDialog.Builder(m_context)
                                .setMessage(R.string.install_package).setPositiveButton(
                                android.R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        holder.m_progressBar.setVisibility(View.VISIBLE);

                                        // Download and install the package
                                        voiceCatalog.downloadVoice(
                                                voicePackage.getId(),
                                                new VoiceCatalog.OnDownloadDoneListener() {
                                                    @Override
                                                    public void onDownloadDone(
                                                            VoiceCatalog.Error error) {
                                                        holder.m_progressBar
                                                                .setVisibility(View.GONE);
                                                        notifyItemChanged(position);
                                                    }
                                                });
                                        voiceCatalog.setOnProgressEventListener(
                                                new VoiceCatalog.OnProgressListener() {
                                                    @Override public void onProgress(int i) {
                                                        /*
                                                         * The download progress can be retrieved
                                                         * in this callback.
                                                         */
                                                    }
                                                });
                                    }
                                }).setNegativeButton(android.R.string.cancel, null).create().show();
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return m_packages.size();
        }

        private static class VoiceViewHolder extends RecyclerView.ViewHolder {
            CheckBox m_isDownloaded;
            TextView m_idView;
            TextView m_nameView;
            TextView m_marcView;
            TextView m_languageView;
            TextView m_typeView;
            TextView m_sizeView;
            TextView m_ttsView;
            ProgressBar m_progressBar;

            VoiceViewHolder(View itemView) {
                super(itemView);

                m_isDownloaded = itemView.findViewById(R.id.voiceDownloaded);
                m_idView = itemView.findViewById(R.id.voiceId);
                m_nameView = itemView.findViewById(R.id.voiceName);
                m_marcView = itemView.findViewById(R.id.voiceMarc);
                m_languageView = itemView.findViewById(R.id.voiceLanguage);
                m_typeView = itemView.findViewById(R.id.voiceType);
                m_sizeView = itemView.findViewById(R.id.voiceSize);
                m_ttsView = itemView.findViewById(R.id.voiceTts);
                m_progressBar = itemView.findViewById(R.id.progressBar);
            }
        }
    }
}

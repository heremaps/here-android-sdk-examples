/*
 * Copyright (c) 2011-2018 HERE Europe B.V.
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

package com.here.android.example.nlp;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import com.here.android.mpa.nlp.Nlp;
import com.here.android.mpa.nlp.SpeechToTextProvider;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Speech to text. IMPORTANT! Do NOT add state flags like started, etc.
 * SpeechRecognizer handles it better
 *
 */
public class MyASR implements SpeechToTextProvider {

    private Context m_context = null;

    private volatile SpeechRecognizer m_stt = null;

    private Nlp m_nlp = null;

    private boolean m_isListening = false;

    /**
     * Create Speech recognizer
     */
    MyASR(Context context) {
        m_context = context;
        create();
    }

    /**
     * Create the service instance
     */
    synchronized void create() {
        if (m_stt == null) {
            m_stt = SpeechRecognizer.createSpeechRecognizer(m_context);
            m_stt.setRecognitionListener(m_sttListener);
        }
    }

    public void setNlp(Nlp nlp) {
        m_nlp = nlp;
    }

    // SpeechToTextProvider interface abstract APIs implementation

    @Override
    public synchronized void cancel() {
        m_stt.cancel();
    }

    /**
     * Destroy speech service instance
     */
    @Override
    public synchronized void destroy() {
        if (stop()) {
            m_stt.destroy();
            m_stt = null;
            m_isListening = false;
        }
    }

    /**
     * Pause == destroy
     */
    @Override
    public synchronized void pause() {
        destroy();
    }

    /**
     * Resume == recreate if stt is null or context is different
     */
    @Override
    public void resume(Context context) {
        if (m_context != context || m_stt == null) {
            destroy();
            m_context = context;
            create();
        }
    }

    /**
     * Schedule to start listening
     */
    @Override
    public synchronized void start() {

        // Do not start if already listening
        if (!m_isListening) {
            // Make sure m_stt is instantiated
            create();

            final Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString());
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, Locale.US.toString());
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 4);

            try {
                m_stt.startListening(intent);
                m_isListening = true;
            } catch (Exception e) {
                destroy();
            }
        }
    }

    /**
     * Stop listening voice
     */
    @Override
    public synchronized boolean stop() {
        if (m_stt != null) {
            m_stt.stopListening();
            return true;
        }
        return false;
    }

    /**
     * Informs whether Android SpeechRecognizer is currently listening to speech
     */
    @Override
    public synchronized boolean isListening() {
        return m_isListening;
    }

    /**
     * Android SpeechRecognizer listener
     */
    private final RecognitionListener m_sttListener = new RecognitionListener() {
        @Override
        public void onBeginningOfSpeech() {
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
        }

        @Override
        public void onEndOfSpeech() {
        }

        @Override
        public void onError(int error) {
            m_isListening = false;
            synchronized (MyASR.this) {
                switch (error) {
                    case SpeechRecognizer.ERROR_NO_MATCH:
                    case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                    case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                        new ToneGenerator(AudioManager.STREAM_MUSIC, ToneGenerator.MAX_VOLUME)
                                .startTone(ToneGenerator.TONE_PROP_BEEP2);
                        break;
                    default:
                        break;
                }
            }
        }

        @Override
        public void onEvent(int eventType, Bundle params) {
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
        }

        @Override
        public void onReadyForSpeech(Bundle params) {
        }

        @Override
        public void onResults(final Bundle results) {
            synchronized (MyASR.this) {
                final ArrayList<String> data =
                        results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

                m_isListening = false;
                if (data != null &&
                        !data.isEmpty() &&
                        m_nlp != null &&
                        m_nlp.isInitialized()) {
                    m_nlp.understand(data.get(0));
                }
            }
        }

        @Override
        public void onRmsChanged(float rms) {
        }
    };
}

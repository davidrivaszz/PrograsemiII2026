package com.example.proyectof;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    // ── Vistas ──────────────────────────────────────────────────────────────
    private PreviewView previewView;
    private TextView tvTranslation, tvCameraStatus;
    private MaterialButton btnHablar, btnEscuchar, btnModo;

    // ── Cámara ───────────────────────────────────────────────────────────────
    private ProcessCameraProvider cameraProvider;

    // ── TTS ──────────────────────────────────────────────────────────────────
    private TextToSpeech tts;
    private boolean ttsReady = false;

    // ── STT ──────────────────────────────────────────────────────────────────
    private SpeechRecognizer speechRecognizer;

    // ── Simulación de detección ──────────────────────────────────────────────
    private final Handler detectionHandler = new Handler(Looper.getMainLooper());
    private final String[] SIGNS = {
            "Hola", "Buenos días", "Gracias", "Por favor", "Sí", "No",
            "Ayuda", "Agua", "Casa", "Familia", "Amor", "Bien",
            "A", "B", "C", "D", "E", "F", "G", "H", "I"
    };
    private int signIndex = 0;
    private boolean cameraRunning = false;

    // ── Modo conversación ────────────────────────────────────────────────────
    private boolean modoConversacion = false;

    // ── Permisos ─────────────────────────────────────────────────────────────
    private static final int REQUEST_PERMISSIONS = 100;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
    };

    // ────────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindViews();
        initTTS();
        initSpeechRecognizer();
        setupButtons();

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_PERMISSIONS);
        }
    }

    // ── Enlazar vistas ───────────────────────────────────────────────────────
    private void bindViews() {
        previewView   = findViewById(R.id.previewView);
        tvTranslation = findViewById(R.id.tvTranslation);
        tvCameraStatus = findViewById(R.id.tvCameraStatus);
        btnHablar     = findViewById(R.id.btnHablar);
        btnEscuchar   = findViewById(R.id.btnEscuchar);
        btnModo       = findViewById(R.id.btnModo);
    }

    // ── Cámara ───────────────────────────────────────────────────────────────
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(this);

        future.addListener(() -> {
            try {
                cameraProvider = future.get();
                bindPreview(cameraProvider);
                cameraRunning = true;
                startSignDetection();
            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Error al iniciar cámara", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(ProcessCameraProvider provider) {
        provider.unbindAll();
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        provider.bindToLifecycle(this,
                CameraSelector.DEFAULT_FRONT_CAMERA, preview);
    }

    // ── Detección simulada de señas ──────────────────────────────────────────
    private void startSignDetection() {
        detectionHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (cameraRunning) {
                    String seña = SIGNS[signIndex % SIGNS.length];
                    tvTranslation.setText(seña);
                    signIndex++;
                    detectionHandler.postDelayed(this, 2500);
                }
            }
        }, 2500);
    }

    private void stopSignDetection() {
        detectionHandler.removeCallbacksAndMessages(null);
        cameraRunning = false;
    }

    // ── Text-To-Speech ───────────────────────────────────────────────────────
    private void initTTS() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("es", "ES"));
                ttsReady = true;
            }
        });
    }

    private void speak(String text) {
        if (ttsReady && text != null && !text.isEmpty()) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts1");
        }
    }

    // ── Speech-To-Text ───────────────────────────────────────────────────────
    private void initSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle p) {
                tvCameraStatus.setText("🎤 ESCUCHANDO");
                tvCameraStatus.setTextColor(0xFF00C853);
            }
            @Override public void onResults(Bundle results) {
                ArrayList<String> matches =
                        results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    tvTranslation.setText(matches.get(0));
                }
                tvCameraStatus.setText("● EN VIVO");
                tvCameraStatus.setTextColor(0xFFFF4444);
            }
            @Override public void onError(int error) {
                Toast.makeText(MainActivity.this,
                        "No se reconoció voz, intenta de nuevo", Toast.LENGTH_SHORT).show();
                tvCameraStatus.setText("● EN VIVO");
                tvCameraStatus.setTextColor(0xFFFF4444);
            }
            // Métodos requeridos vacíos
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float v) {}
            @Override public void onBufferReceived(byte[] b) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onPartialResults(Bundle b) {}
            @Override public void onEvent(int t, Bundle b) {}
        });
    }

    private void startListening() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Habla ahora...");
        speechRecognizer.startListening(intent);
    }

    // ── Botones ──────────────────────────────────────────────────────────────
    private void setupButtons() {

        // 🔊 Reproducir voz — lee la traducción actual en voz alta
        btnHablar.setOnClickListener(v -> {
            String texto = tvTranslation.getText().toString();
            if (texto.equals("Traducción aparecerá aquí")) {
                Toast.makeText(this, "No hay traducción para reproducir", Toast.LENGTH_SHORT).show();
            } else {
                speak(texto);
            }
        });

        // 🎤 Voz a texto — escucha y muestra en la card de traducción
        btnEscuchar.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED) {
                stopSignDetection();
                startListening();
            } else {
                Toast.makeText(this, "Se necesita permiso de micrófono", Toast.LENGTH_SHORT).show();
            }
        });

        // 🔄 Modo conversación — alterna entre cámara activa y modo voz
        btnModo.setOnClickListener(v -> {
            modoConversacion = !modoConversacion;

            if (modoConversacion) {
                // Activar modo conversación: detener detección, esperar voz
                stopSignDetection();
                btnModo.setText("📷  MODO CÁMARA");
                tvTranslation.setText("Modo conversación activo");
                Toast.makeText(this,
                        "Modo conversación: usa el botón 🎤 para hablar", Toast.LENGTH_LONG).show();
            } else {
                // Volver a modo cámara
                btnModo.setText("🔄  MODO CONVERSACIÓN");
                tvTranslation.setText("Traducción aparecerá aquí");
                cameraRunning = true;
                startSignDetection();
            }
        });
    }

    // ── Permisos ─────────────────────────────────────────────────────────────
    private boolean allPermissionsGranted() {
        for (String perm : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, perm)
                    != PackageManager.PERMISSION_GRANTED) return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == REQUEST_PERMISSIONS && allPermissionsGranted()) {
            startCamera();
        } else {
            Toast.makeText(this,
                    "Se necesitan permisos de cámara y micrófono", Toast.LENGTH_LONG).show();
        }
    }

    // ── Ciclo de vida ────────────────────────────────────────────────────────
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopSignDetection();
        if (tts != null) { tts.stop(); tts.shutdown(); }
        if (speechRecognizer != null) speechRecognizer.destroy();
        if (cameraProvider != null) cameraProvider.unbindAll();
    }
}
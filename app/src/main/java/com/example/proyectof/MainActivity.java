package com.example.proyectof;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker;
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "SeñasApp";

    // ── Vistas ───────────────────────────────────────────────────────────────
    private PreviewView previewView;
    private TextView tvTranslation, tvCameraStatus;
    private MaterialButton btnHablar, btnEscuchar, btnModo;

    // ── Cámara ───────────────────────────────────────────────────────────────
    private ProcessCameraProvider cameraProvider;
    private ExecutorService cameraExecutor;

    // ── MediaPipe HandLandmarker ─────────────────────────────────────────────
    private HandLandmarker handLandmarker;

    // ── TTS ──────────────────────────────────────────────────────────────────
    private TextToSpeech tts;
    private boolean ttsReady = false;

    // ── STT ──────────────────────────────────────────────────────────────────
    private SpeechRecognizer speechRecognizer;

    // ── Estado ───────────────────────────────────────────────────────────────
    private boolean modoConversacion = false;
    // FIX 1: cameraRunning empieza en TRUE para que analyzeFrame no se bloquee
    private boolean cameraRunning = true;

    // FIX 2: STABLE_FRAMES reducido a 2 para detectar cambios más rápido
    private String lastSign    = "";
    private int    stableCount = 0;
    private static final int STABLE_FRAMES = 2;

    // Debounce TTS
    private final Handler ttsDebounce = new Handler(Looper.getMainLooper());
    private static final long TTS_DELAY_MS = 1500;
    private String lastSpoken = "";

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

        cameraExecutor = Executors.newSingleThreadExecutor();

        bindViews();
        initTTS();
        initSpeechRecognizer();
        setupButtons();
        initHandLandmarker();

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_PERMISSIONS);
        }
    }

    // ── Enlazar vistas ───────────────────────────────────────────────────────
    private void bindViews() {
        previewView    = findViewById(R.id.previewView);
        tvTranslation  = findViewById(R.id.tvTranslation);
        tvCameraStatus = findViewById(R.id.tvCameraStatus);
        btnHablar      = findViewById(R.id.btnHablar);
        btnEscuchar    = findViewById(R.id.btnEscuchar);
        btnModo        = findViewById(R.id.btnModo);
    }

    // ── Inicializar MediaPipe HandLandmarker ─────────────────────────────────
    private void initHandLandmarker() {
        try {
            BaseOptions baseOptions = BaseOptions.builder()
                    .setModelAssetPath("hand_landmarker.task")
                    .build();

            HandLandmarker.HandLandmarkerOptions options =
                    HandLandmarker.HandLandmarkerOptions.builder()
                            .setBaseOptions(baseOptions)
                            .setNumHands(1)
                            // FIX 3: umbrales más bajos para detectar en condiciones variadas
                            .setMinHandDetectionConfidence(0.3f)
                            .setMinHandPresenceConfidence(0.3f)
                            .setMinTrackingConfidence(0.3f)
                            .setRunningMode(RunningMode.IMAGE)
                            .build();

            handLandmarker = HandLandmarker.createFromOptions(this, options);
            Log.d(TAG, "HandLandmarker inicializado correctamente");
        } catch (Exception e) {
            Log.e(TAG, "Error al inicializar HandLandmarker: " + e.getMessage());
            Toast.makeText(this,
                    "Error: asegúrate de que hand_landmarker.task está en assets/",
                    Toast.LENGTH_LONG).show();
        }
    }

    // ── Cámara ───────────────────────────────────────────────────────────────
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(this);

        future.addListener(() -> {
            try {
                cameraProvider = future.get();
                bindCameraUseCases(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Error al iniciar cámara", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases(ProcessCameraProvider provider) {
        provider.unbindAll();

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeFrame);

        provider.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_FRONT_CAMERA,
                preview,
                imageAnalysis
        );
    }

    // ── Análisis de frame con MediaPipe ──────────────────────────────────────
    private void analyzeFrame(ImageProxy imageProxy) {
        if (modoConversacion || handLandmarker == null) {
            imageProxy.close();
            return;
        }

        // FIX 4: Se eliminó el espejo horizontal — MediaPipe en modo IMAGE
        // ya maneja la orientación de la cámara frontal correctamente.
        // Espejarlo causaba que los landmarks X quedaran invertidos y
        // el clasificador fallaba al comparar posiciones izquierda/derecha.
        try {
            Bitmap bitmap = imageProxy.toBitmap();
            MPImage mpImage = new BitmapImageBuilder(bitmap).build();
            HandLandmarkerResult result = handLandmarker.detect(mpImage);

            if (result.landmarks() != null && !result.landmarks().isEmpty()) {
                List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark> rawLandmarks
                        = result.landmarks().get(0);

                // Log para diagnóstico — ver en Logcat con filtro "SeñasApp"
                Log.d(TAG, "Mano detectada con " + rawLandmarks.size() + " landmarks");

                List<SignClassifier.Point> points = new ArrayList<>();
                for (var lm : rawLandmarks) {
                    points.add(new SignClassifier.Point(lm.x(), lm.y()));
                }

                String detected = SignClassifier.classify(points);
                Log.d(TAG, "Seña clasificada: " + detected);

                handleDetection(detected);
            } else {
                Log.d(TAG, "Sin mano detectada en este frame");
                handleDetection(null);
            }

            bitmap.recycle();
        } catch (Exception e) {
            Log.e(TAG, "Error en análisis de frame: " + e.getMessage());
        } finally {
            imageProxy.close();
        }
    }

    // ── Manejar resultado de detección ───────────────────────────────────────
    private void handleDetection(String sign) {
        runOnUiThread(() -> {
            if (sign == null) {
                tvCameraStatus.setText("● BUSCANDO MANO");
                tvCameraStatus.setTextColor(0xFFFF9800);
                stableCount = 0;
                lastSign = "";
                return;
            }

            tvCameraStatus.setText("● EN VIVO");
            tvCameraStatus.setTextColor(0xFF44FF44);

            if (sign.equals(lastSign)) {
                stableCount++;
            } else {
                lastSign = sign;
                stableCount = 1;
            }

            if (stableCount >= STABLE_FRAMES) {
                tvTranslation.setText(sign);

                if (!sign.equals(lastSpoken)) {
                    ttsDebounce.removeCallbacksAndMessages(null);
                    ttsDebounce.postDelayed(() -> {
                        speak(sign);
                        lastSpoken = sign;
                    }, TTS_DELAY_MS);
                }
            }
        });
    }

    // ── Text-To-Speech ───────────────────────────────────────────────────────
    private void initTTS() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(new Locale("es", "SV"));
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                        result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(new Locale("es", "ES"));
                }
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
                tvCameraStatus.setTextColor(0xFF44FF44);
                cameraRunning = true;
            }
            @Override public void onError(int error) {
                Toast.makeText(MainActivity.this,
                        "No se reconoció voz, intenta de nuevo", Toast.LENGTH_SHORT).show();
                tvCameraStatus.setText("● EN VIVO");
                tvCameraStatus.setTextColor(0xFF44FF44);
                cameraRunning = true;
            }
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
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-SV");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Habla ahora...");
        speechRecognizer.startListening(intent);
    }

    // ── Botones ──────────────────────────────────────────────────────────────
    private void setupButtons() {

        btnHablar.setOnClickListener(v -> {
            String texto = tvTranslation.getText().toString();
            if (texto.equals("Traducción aparecerá aquí") || texto.isEmpty()) {
                Toast.makeText(this, "No hay traducción para reproducir", Toast.LENGTH_SHORT).show();
            } else {
                speak(texto);
            }
        });

        btnEscuchar.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED) {
                cameraRunning = false;
                startListening();
            } else {
                Toast.makeText(this, "Se necesita permiso de micrófono", Toast.LENGTH_SHORT).show();
            }
        });

        btnModo.setOnClickListener(v -> {
            modoConversacion = !modoConversacion;

            if (modoConversacion) {
                cameraRunning = false;
                btnModo.setText("📷  MODO CÁMARA");
                tvTranslation.setText("Modo conversación activo");
                tvCameraStatus.setText("● MODO VOZ");
                tvCameraStatus.setTextColor(0xFF2196F3);
                Toast.makeText(this,
                        "Modo conversación: usa el botón 🎤 para hablar", Toast.LENGTH_LONG).show();
            } else {
                modoConversacion = false;
                cameraRunning = true;
                btnModo.setText("🔄  MODO CONVERSACIÓN");
                tvTranslation.setText("Apunta la cámara a tu mano");
                lastSign = "";
                lastSpoken = "";
                stableCount = 0;
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
                                           @NonNull String[] permissions,
                                           @NonNull int[] results) {
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
        cameraRunning = false;
        if (tts != null) { tts.stop(); tts.shutdown(); }
        if (speechRecognizer != null) speechRecognizer.destroy();
        if (cameraProvider != null) cameraProvider.unbindAll();
        if (cameraExecutor != null) cameraExecutor.shutdown();
        if (handLandmarker != null) handLandmarker.close();
        ttsDebounce.removeCallbacksAndMessages(null);
    }
}
package com.example.spamdetector;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.util.Log;

import org.json.JSONObject;
import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LocalPredictor {
    public interface PredictionCallback {
        void onPrediction(float score);
    }

    public interface DetailedPredictionCallback {
        void onPrediction(EnsemblePrediction prediction);
    }

    public static class EnsemblePrediction {
        public final float finalScore;
        public final float nbScore;
        public final float lrScore;
        public final boolean ensembleActive;
        public final String modelSummary;

        public EnsemblePrediction(float finalScore, float nbScore, float lrScore, boolean ensembleActive, String modelSummary) {
            this.finalScore = finalScore;
            this.nbScore = nbScore;
            this.lrScore = lrScore;
            this.ensembleActive = ensembleActive;
            this.modelSummary = modelSummary;
        }
    }

    private static final String TAG = "LocalPredictor";
    private static final String LEGACY_MODEL = "spam_model.tflite";
    private static final String[] NB_MODELS = {"spam_nb_model.tflite", "spam_nb.tflite"};
    private static final String[] LR_MODELS = {"spam_lr_model.tflite", "spam_lr.tflite"};

    private final ExecutorService inferenceExecutor = Executors.newSingleThreadExecutor();
    private Interpreter legacyInterpreter;
    private Interpreter naiveBayesInterpreter;
    private Interpreter logisticRegressionInterpreter;
    private Map<String, Integer> vocabulary;
    private int inputSize;

    public LocalPredictor(Context context) {
        try {
            vocabulary = loadVocabulary(context, "vocab.json");
            legacyInterpreter = tryLoadInterpreter(context, LEGACY_MODEL);
            naiveBayesInterpreter = tryLoadInterpreter(context, NB_MODELS);
            logisticRegressionInterpreter = tryLoadInterpreter(context, LR_MODELS);

            if (logisticRegressionInterpreter != null) {
                inputSize = logisticRegressionInterpreter.getInputTensor(0).shape()[1];
            } else if (naiveBayesInterpreter != null) {
                inputSize = naiveBayesInterpreter.getInputTensor(0).shape()[1];
            } else if (legacyInterpreter != null) {
                inputSize = legacyInterpreter.getInputTensor(0).shape()[1];
            }

            Log.i(TAG, "Predictor initialized. " + getLoadedModelSummary());
        } catch (Exception e) {
            Log.e(TAG, "Error initializing LocalPredictor: " + e.getMessage());
        }
    }

    private Interpreter tryLoadInterpreter(Context context, String... modelPaths) {
        for (String modelPath : modelPaths) {
            try {
                if (!assetExists(context, modelPath)) {
                    continue;
                }
                Log.i(TAG, "Loading local model: " + modelPath);
                return new Interpreter(loadModelFile(context, modelPath));
            } catch (Exception e) {
                Log.e(TAG, "Failed to load model " + modelPath + ": " + e.getMessage());
            }
        }
        return null;
    }

    private boolean assetExists(Context context, String fileName) {
        try {
            AssetManager assetManager = context.getAssets();
            String[] files = assetManager.list("");
            if (files == null) {
                return false;
            }
            for (String file : files) {
                if (fileName.equals(file)) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private MappedByteBuffer loadModelFile(Context context, String modelPath) throws Exception {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.getStartOffset(), fileDescriptor.getDeclaredLength());
    }

    private Map<String, Integer> loadVocabulary(Context context, String fileName) throws Exception {
        InputStream is = context.getAssets().open(fileName);
        Scanner s = new Scanner(is).useDelimiter("\\A");
        String jsonString = s.hasNext() ? s.next() : "";

        JSONObject jsonObject = new JSONObject(jsonString);
        Map<String, Integer> map = new HashMap<>();
        jsonObject.keys().forEachRemaining(key -> {
            try {
                map.put(key, jsonObject.getInt(key));
            } catch (Exception e) {
                Log.e(TAG, "Vocabulary parse issue: " + e.getMessage());
            }
        });
        return map;
    }

    private float[] vectorize(String text) {
        float[] inputVector = new float[inputSize];
        List<String> features = TextFeatureExtractor.buildFeatures(text);
        for (String token : features) {
            Integer index = vocabulary.get(token);
            if (index != null && index < inputSize) {
                inputVector[index] += 1.0f;
            }
        }
        normalizeVector(inputVector, features.size());
        return inputVector;
    }

    private void normalizeVector(float[] inputVector, int tokenCount) {
        if (tokenCount <= 0) {
            return;
        }

        // Keeps the feature scale stable across short and long messages while we
        // wait for dedicated exported TF-IDF weights for the NB/LR ensemble.
        float divisor = (float) tokenCount;
        for (int i = 0; i < inputVector.length; i++) {
            inputVector[i] = inputVector[i] / divisor;
        }
    }

    private float runInterpreter(Interpreter interpreter, float[] inputVector) {
        if (interpreter == null) {
            return -1f;
        }
        float[][] output = new float[1][1];
        float[][] batchedInput = new float[1][inputSize];
        System.arraycopy(inputVector, 0, batchedInput[0], 0, inputSize);
        interpreter.run(batchedInput, output);
        return clampScore(output[0][0]);
    }

    private float clampScore(float score) {
        if (Float.isNaN(score) || Float.isInfinite(score)) {
            return -1f;
        }
        return Math.max(0f, Math.min(1f, score));
    }

    public EnsemblePrediction predictDetailed(String text) {
        if (vocabulary == null || inputSize <= 0) {
            return new EnsemblePrediction(-1f, -1f, -1f, false, "No model available");
        }

        float[] inputVector = vectorize(text);

        float nbScore = runInterpreter(naiveBayesInterpreter, inputVector);
        float lrScore = runInterpreter(logisticRegressionInterpreter, inputVector);

        if (nbScore >= 0f && lrScore >= 0f) {
            float finalScore = (0.4f * nbScore) + (0.6f * lrScore);
            return new EnsemblePrediction(finalScore, nbScore, lrScore, true, "Ensemble: Naive Bayes + Logistic Regression");
        }

        float legacyScore = runInterpreter(legacyInterpreter, inputVector);
        if (legacyScore >= 0f) {
            return new EnsemblePrediction(legacyScore, -1f, -1f, false, "Legacy single local model");
        }

        if (lrScore >= 0f) {
            return new EnsemblePrediction(lrScore, -1f, lrScore, false, "Logistic Regression only");
        }
        if (nbScore >= 0f) {
            return new EnsemblePrediction(nbScore, nbScore, -1f, false, "Naive Bayes only");
        }

        return new EnsemblePrediction(-1f, -1f, -1f, false, "No working local model");
    }

    public float predict(String text) {
        return predictDetailed(text).finalScore;
    }

    public void predictAsync(String text, PredictionCallback callback) {
        inferenceExecutor.execute(() -> callback.onPrediction(predict(text)));
    }

    public void predictDetailedAsync(String text, DetailedPredictionCallback callback) {
        inferenceExecutor.execute(() -> callback.onPrediction(predictDetailed(text)));
    }

    public String getLoadedModelSummary() {
        if (naiveBayesInterpreter != null && logisticRegressionInterpreter != null) {
            return "Loaded ensemble models: NB + LR";
        }
        if (legacyInterpreter != null) {
            return "Loaded legacy single model (n-gram + engineered features)";
        }
        if (logisticRegressionInterpreter != null) {
            return "Loaded Logistic Regression model";
        }
        if (naiveBayesInterpreter != null) {
            return "Loaded Naive Bayes model";
        }
        return "No local model loaded";
    }
}

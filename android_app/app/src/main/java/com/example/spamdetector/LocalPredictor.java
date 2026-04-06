package com.example.spamdetector;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;
import org.json.JSONObject;
import org.tensorflow.lite.Interpreter;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class LocalPredictor {
    private static final String TAG = "LocalPredictor";
    private Interpreter tflite;
    private Map<String, Integer> vocabulary;
    private int inputSize;

    public LocalPredictor(Context context) {
        try {
            // 1. Load TFLite Model
            tflite = new Interpreter(loadModelFile(context, "spam_model.tflite"));
            
            // 2. Load Vocabulary JSON
            vocabulary = loadVocabulary(context, "vocab.json");
            
            // Get expected input size from model
            inputSize = tflite.getInputTensor(0).shape()[1];
            
            Log.i(TAG, "TFLite Model and Vocabulary loaded. Input size: " + inputSize);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing LocalPredictor: " + e.getMessage());
        }
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
            } catch (Exception e) { e.printStackTrace(); }
        });
        return map;
    }

    public float predict(String text) {
        if (tflite == null || vocabulary == null) return -1;

        // 1. Simple Vectorization (Bag of Words)
        float[] inputVector = new float[inputSize];
        String[] tokens = text.toLowerCase().replaceAll("[^a-z ]", "").split("\\s+");
        
        for (String token : tokens) {
            if (vocabulary.containsKey(token)) {
                int index = vocabulary.get(token);
                if (index < inputSize) {
                    inputVector[index] += 1.0f; // Simplified term frequency
                }
            }
        }

        // 2. Run Inference
        float[][] output = new float[1][1];
        tflite.run(inputVector, output);
        
        return output[0][0]; // Probability (0.0 to 1.0)
    }
}

package com.example.spamdetector;

import android.util.Log;

import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class ApiHelper {
    private static final String TAG = "SpamGuard_Api";
    private static final String API_URL = "https://sms-spam-detector-production.up.railway.app/predict";
    private static final String API_KEY = "SMS_GUARD_2024_SECURE";

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .build();

    public interface DetectionCallback {
        void onResult(String label, float confidence, String sender, String messageText);
        void onFailure();
    }

    public static void checkSpam(String sender, String messageText, DetectionCallback callback) {
        try {
            JSONObject json = new JSONObject();
            json.put("message", messageText);

            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.get("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(API_URL)
                    .post(body)
                    .addHeader("X-API-KEY", API_KEY)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Network Error: " + e.getMessage());
                    callback.onFailure();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try (ResponseBody responseBody = response.body()) {
                        if (response.isSuccessful() && responseBody != null) {
                            JSONObject result = new JSONObject(responseBody.string());
                            String prediction = result.getString("prediction");
                            float confidence = (float) result.optDouble("confidence", 0.5);
                            callback.onResult(prediction, confidence, sender, messageText);
                        } else {
                            Log.e(TAG, "Server Error: " + response.code());
                            callback.onFailure();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "JSON Error: " + e.getMessage());
                        callback.onFailure();
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Request Error: " + e.getMessage());
            callback.onFailure();
        }
    }
}

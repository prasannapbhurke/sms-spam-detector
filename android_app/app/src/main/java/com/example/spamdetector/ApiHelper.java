package com.example.spamdetector;

import android.content.Context;
import android.util.Log;
import okhttp3.*;
import org.json.JSONObject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ApiHelper {
    private static final String TAG = "SpamGuard_Api";
    
    // Replace with your actual Railway URL
    private static final String API_URL = "https://your-project.up.railway.app/predict";
    
    // MUST match the API_KEY in app.py exactly
    private static final String API_KEY = "your-very-secret-string-here";

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .build();

    public static void checkSpam(Context context, String messageText) {
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
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try (ResponseBody responseBody = response.body()) {
                        if (response.isSuccessful() && responseBody != null) {
                            JSONObject result = new JSONObject(responseBody.string());
                            NotificationHelper.showNotification(context, 
                                result.getString("prediction"), 
                                String.valueOf(result.getDouble("confidence")), 
                                messageText);
                        } else {
                            Log.e(TAG, "Server Error: " + response.code());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "JSON Error: " + e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Request Error: " + e.getMessage());
        }
    }
}

package com.example.spamdetector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

public class SmsReceiver extends BroadcastReceiver {
    private static final String TAG = "SmsReceiver";
    private LocalPredictor localPredictor;

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("android.provider.Telephony.SMS_RECEIVED".equals(intent.getAction())) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Object[] pdus = (Object[]) bundle.get("pdus");
                if (pdus != null) {
                    for (Object pdu : pdus) {
                        SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
                        String sender = smsMessage.getDisplayOriginatingAddress();
                        String messageBody = smsMessage.getMessageBody();

                        Log.d(TAG, "SMS Received: " + messageBody);

                        // 1. Try Local Prediction first (Fast & Offline)
                        if (localPredictor == null) localPredictor = new LocalPredictor(context);
                        float probability = localPredictor.predict(messageBody);

                        if (probability >= 0.5f) {
                            // Show instant local spam notification
                            NotificationHelper.showNotification(context, "Spam", String.valueOf(probability), messageBody);
                        } else {
                            // 2. Fallback to Cloud API for more accuracy if needed
                            ApiHelper.checkSpam(context, messageBody);
                        }
                    }
                }
            }
        }
    }
}

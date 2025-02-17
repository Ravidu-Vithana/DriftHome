package com.ryvk.drifthome;

import android.app.AlertDialog;
import android.content.Context;

public class AlertUtils {
    public static void showAlert(Context context, String title, String message) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
}
package com.ryvk.drifthome;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.firestore.GeoPoint;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Utils {
    public static List<LatLng> decodePolyline(String encoded) {
        List<LatLng> polyline = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dLat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dLat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dLng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dLng;

            polyline.add(new LatLng(lat / 1E5, lng / 1E5));
        }

        return polyline;
    }
    public static GeoPoint getClosestGeoPoint(Context context, Drinker drinker, GeoPoint target, List<GeoPoint> geoPoints) {
        if (geoPoints == null || geoPoints.isEmpty()) {
            return null; // No points to compare
        }

        String apiKey = drinker.getApiKey(context);
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("API Key is missing or invalid!");
        }

        GeoPoint closest = null;
        int minDistance = Integer.MAX_VALUE;

        for (GeoPoint point : geoPoints) {
            int distance = getRoadDistance(apiKey, target, point);
            if (distance != -1 && distance < minDistance) {
                minDistance = distance;
                closest = point;
            }
        }

        return closest;
    }

    public static int getRoadDistance(String apiKey, GeoPoint origin, GeoPoint destination) {
        try {
            String url = "https://maps.googleapis.com/maps/api/distancematrix/json?origins=" +
                    origin.getLatitude() + "," + origin.getLongitude() +
                    "&destinations=" + destination.getLatitude() + "," + destination.getLongitude() +
                    "&key=" + apiKey;

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();

            OkHttpClient httpClient = new OkHttpClient();
            Response response = httpClient.newCall(request).execute();

            if (response.isSuccessful() && response.body() != null) {
                String responseData = response.body().string();
                JSONObject jsonResponse = new JSONObject(responseData);
                JSONArray rows = jsonResponse.getJSONArray("rows");

                if (rows.length() > 0) {
                    JSONObject elements = rows.getJSONObject(0).getJSONArray("elements").getJSONObject(0);
                    if (!elements.getString("status").equals("OK")) {
                        // Distance not available
                        Log.d("Utils", "getRoadDistance: Failure -> status is not OK");
                        return -1;
                    }
                    return elements.getJSONObject("distance").getInt("value"); // Distance in meters
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("Utils", "getRoadDistance: Failure",e );
        }
        return -1;
    }
    public static void hideKeyboard(Activity activity) {
        if (activity != null && activity.getCurrentFocus() != null) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
            }
        }
    }

    public static void hideKeyboard(View view) {
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    public static Bitmap getRoundedImageBitmap(Uri imageUri){
        Bitmap originalBitmap = BitmapFactory.decodeFile(imageUri.getPath());
        int width = originalBitmap.getWidth();
        int height = originalBitmap.getHeight();
        int radius = Math.min(width, height) / 2;

        Bitmap circularBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(circularBitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);
        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawCircle(width / 2, height / 2, radius, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(originalBitmap, 0, 0, paint);

        return circularBitmap;
    }

    public static void loadImageUrlToView(Context context, ImageView imageView, String downloadUrl){
        Glide.with(context)
                .load(downloadUrl)
                .apply(RequestOptions.circleCropTransform())
                .into(imageView);
    }
}

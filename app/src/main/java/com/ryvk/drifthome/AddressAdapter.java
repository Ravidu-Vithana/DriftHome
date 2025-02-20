package com.ryvk.drifthome;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddressAdapter extends RecyclerView.Adapter<AddressAdapter.ViewHolder> {

    private List<AddressCard> addressList;
    private Context context;

    private static final String TAG = "AddressAdapter";

    public AddressAdapter(List<AddressCard> addressList, Context context) {
        this.addressList = addressList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.address_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AddressCard item = addressList.get(position);
        holder.textView.setText(item.getAddressText());
        Drinker loggedDrinker = Drinker.getSPDrinker(context);

        if (position == 0) {
            holder.buttonHome.setVisibility(View.VISIBLE);
        } else {
            holder.buttonHome.setVisibility(View.GONE);
        }

        RecyclerView recyclerView = ((Activity) context).findViewById(R.id.recyclerView);

        GeoPoint geoPoint = item.getGeoPoint();
        double latitude = geoPoint.getLatitude();
        double longitude = geoPoint.getLongitude();

        holder.buttonMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri gmmIntentUri = Uri.parse("geo:" + latitude + "," + longitude + "?q=" + latitude + "," + longitude);
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");

                if (mapIntent.resolveActivity(view.getContext().getPackageManager()) != null) {
                    view.getContext().startActivity(mapIntent);
                } else {
                    Toast.makeText(view.getContext(), "Google Maps is not installed", Toast.LENGTH_SHORT).show();
                }
            }
        });

        holder.buttonDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loggedDrinker.removeAddress(position, context, new Drinker.AddressCallback() {
                    @Override
                    public void onAddressesReady(List<AddressCard> addressCards) {
                        ((Activity) context).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                AddressAdapter adapter = new AddressAdapter(addressCards, context);
                                recyclerView.setAdapter(adapter);
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        Log.i(TAG, "Address removing: failure " + error);
                    }
                });

                // Update the drinker's address list in Firestore
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                Map<String, Object> drinkerAddressData = new HashMap<>();
                drinkerAddressData.put("addresses", loggedDrinker.getAddresses());

                db.collection("drinker")
                        .document(loggedDrinker.getEmail())
                        .update(drinkerAddressData)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {
                                Log.i(TAG, "Address removed successfully");
                                loggedDrinker.updateSPDrinker(context,loggedDrinker);
                                Toast.makeText(context, "Address removed successfully", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e(TAG, "Address removal failed: " + e);
                                Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });
    }

    @Override
    public int getItemCount() {
        return addressList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        Button buttonMap, buttonDelete, buttonHome;

        public ViewHolder(View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.textView43);
            buttonMap = itemView.findViewById(R.id.button23);
            buttonDelete = itemView.findViewById(R.id.button22);
            buttonHome = itemView.findViewById(R.id.button24);
        }
    }
}

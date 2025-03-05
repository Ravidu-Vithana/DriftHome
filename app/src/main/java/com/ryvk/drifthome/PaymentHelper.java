package com.ryvk.drifthome;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import androidx.fragment.app.Fragment;

import lk.payhere.androidsdk.PHConfigs;
import lk.payhere.androidsdk.PHConstants;
import lk.payhere.androidsdk.PHMainActivity;
import lk.payhere.androidsdk.model.InitRequest;
import lk.payhere.androidsdk.model.Item;
public class PaymentHelper {
    public static final int RC_PAYHERE = 110;
    public static void initiatePayment(Fragment fragment, double amount, String orderId, String description, String email, String firstName, String lastName, String mobile) {
        InitRequest req = new InitRequest();
        req.setMerchantId("1221370");
        req.setCurrency("LKR");
        req.setAmount(amount);
        req.setOrderId(orderId);
        req.setItemsDescription(description);
        req.getCustomer().setFirstName(firstName);
        req.getCustomer().setLastName(lastName);
        req.getCustomer().setEmail(email);
        req.getCustomer().setPhone(mobile);
        req.getCustomer().getAddress().setAddress("No.1, Galle Road");
        req.getCustomer().getAddress().setCity("Colombo");
        req.getCustomer().getAddress().setCountry("Sri Lanka");

        Intent intent = new Intent(fragment.getActivity(), PHMainActivity.class);
        intent.putExtra(PHConstants.INTENT_EXTRA_DATA, req);
        PHConfigs.setBaseUrl(PHConfigs.SANDBOX_URL); // Use LIVE_URL for production

        Log.d("PaymentHelper", "Starting payment activity with Order ID: " + orderId);

        fragment.startActivityForResult(intent, RC_PAYHERE);
    }
}

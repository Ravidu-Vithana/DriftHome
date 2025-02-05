package com.ryvk.drifthome;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentContainerView;

public class BookingActivity extends AppCompatActivity {

    float bookRideBtnAnimate;
    float bookRideBtnHeight;
    float bookRideTextY;
    float bookRideDriverCardAnimate;
    float bookRideAddressTextAnimate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_booking);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Thread bookingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                initiateBooking();
            }
        });
        bookingThread.start();

    }

    private void initiateBooking(){
        Button bookBtn = findViewById(R.id.button10);
        Button cancelBtn = findViewById(R.id.button11);
        TextView infoText = findViewById(R.id.textView14);
        ProgressBar progressBar = findViewById(R.id.progressBar4);
        FragmentContainerView driverCardView = findViewById(R.id.fragmentContainerView2);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.GONE);
                infoText.setVisibility(View.VISIBLE);
                cancelBtn.setVisibility(View.GONE);
                bookBtn.setText("");
                bookBtn.setTextSize(60f);
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int x = 3; x >= 0; x--){
                    int finalX = x;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            bookBtn.setText(String.valueOf(finalX));
                        }
                    });
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        infoText.setText(R.string.d_booking_text1_driverOnTheWay);

                        // Change button background color
                        int newColor = ContextCompat.getColor(getApplicationContext(), R.color.d_red1);
                        bookBtn.setBackgroundColor(newColor);
                        bookBtn.setText(R.string.d_booking_btn1_cancel);
                        bookBtn.setTextSize(20f);

                        // Get new size from dimens.xml
                        int newSize = (int) getResources().getDimension(R.dimen.d_circle_btn_small);
                        int currentWidth = bookBtn.getWidth();

                        // Animate width and height
                        ValueAnimator sizeAnimator = ValueAnimator.ofInt(currentWidth, newSize);
                        sizeAnimator.setDuration(300);
                        sizeAnimator.addUpdateListener(animation -> {
                            int animatedValue = (int) animation.getAnimatedValue();
                            ViewGroup.LayoutParams params = bookBtn.getLayoutParams();
                            params.width = animatedValue;
                            params.height = animatedValue;
                            bookBtn.setLayoutParams(params);
                        });
                        sizeAnimator.start();

                        bookRideBtnAnimate = getResources().getDimension(R.dimen.d_bookRideBtn_animate);
                        ObjectAnimator animator = ObjectAnimator.ofFloat(bookBtn, "y", bookRideBtnAnimate);
                        animator.setDuration(500);

                        bookRideBtnHeight = getResources().getDimension(R.dimen.d_circle_btn_small);
                        bookRideTextY = bookRideBtnAnimate + bookRideBtnHeight + 50f;
                        ObjectAnimator animator2 = ObjectAnimator.ofFloat(infoText, "y", bookRideTextY);
                        animator2.setDuration(500);
                        animator2.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                super.onAnimationEnd(animation);
                                driverCardView.setVisibility(View.VISIBLE);
                                bookBtn.setClickable(true);
                            }
                        });

                        bookRideDriverCardAnimate = getResources().getDimension(R.dimen.d_bookRideDriverCard_animate);
                        ObjectAnimator animator3 = ObjectAnimator.ofFloat(driverCardView, "y", bookRideTextY+bookRideDriverCardAnimate);
                        animator3.setDuration(100);

                        AnimatorSet animatorSet = new AnimatorSet();
                        animatorSet.playTogether(animator, animator2, animator3);
                        animatorSet.start();
                    }
                });
            }
        }).start();
    }

    private void startTrip(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent i = new Intent(BookingActivity.this, TripActivity.class);
                startActivity(i);
            }
        });
    }

}
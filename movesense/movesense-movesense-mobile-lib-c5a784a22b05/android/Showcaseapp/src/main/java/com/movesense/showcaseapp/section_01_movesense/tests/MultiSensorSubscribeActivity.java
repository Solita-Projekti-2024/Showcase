package com.movesense.showcaseapp.section_01_movesense.tests;

import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.movesense.mds.Mds;
import com.movesense.mds.MdsException;
import com.movesense.mds.MdsNotificationListener;
import com.movesense.mds.MdsSubscription;
import com.movesense.mds.internal.connectivity.MovesenseConnectedDevices;
import com.movesense.showcaseapp.R;
import com.movesense.showcaseapp.model.LinearAcceleration;
import com.movesense.showcaseapp.utils.FormatHelper;

import java.util.Locale;

public class MultiSensorSubscribeActivity extends AppCompatActivity {

    private static final String TAG = "MultiSensorSubscribe";
    private static final String LINEAR_ACC_PATH = "Meas/Acc/";
    private static final String ECG_PATH = "Meas/ECG/";
    private static final String URI_EVENTLISTENER = "suunto://MDS/EventListener";
    private MdsSubscription linearAccSubscription;
    private MdsSubscription ecgSubscription;

    // Views
    private SwitchCompat switchSubscriptionLinearAcc;
    private SwitchCompat switchSubscriptionECG;
    private TextView xAxisLinearAccTextView;
    private TextView yAxisLinearAccTextView;
    private TextView zAxisLinearAccTextView;
    private TextView ecgTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_sensor_subscribe);

        // Initialize views
        switchSubscriptionLinearAcc = findViewById(R.id.switchSubscriptionLinearAcc);
        switchSubscriptionECG = findViewById(R.id.switchSubscriptionECG);
        xAxisLinearAccTextView = findViewById(R.id.x_axis_linearAcc_textView);
        yAxisLinearAccTextView = findViewById(R.id.y_axis_linearAcc_textView);
        zAxisLinearAccTextView = findViewById(R.id.z_axis_linearAcc_textView);
        ecgTextView = findViewById(R.id.ecg_textView);

        // Handle Linear Acceleration subscription toggle
        switchSubscriptionLinearAcc.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (MovesenseConnectedDevices.getConnectedDevices().size() > 0) {
                    subscribeToLinearAcc();
                } else {
                    Toast.makeText(MultiSensorSubscribeActivity.this, "No connected device found", Toast.LENGTH_SHORT).show();
                    switchSubscriptionLinearAcc.setChecked(false);
                }
            } else {
                unsubscribeLinearAcc();
            }
        });

        // Handle ECG subscription toggle
        switchSubscriptionECG.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (MovesenseConnectedDevices.getConnectedDevices().size() > 0) {
                    subscribeToECG();
                } else {
                    Toast.makeText(MultiSensorSubscribeActivity.this, "No connected device found", Toast.LENGTH_SHORT).show();
                    switchSubscriptionECG.setChecked(false);
                }
            } else {
                unsubscribeECG();
            }
        });
    }

    private void subscribeToLinearAcc() {
        String linearAccUri = FormatHelper.formatContractToJson(
                MovesenseConnectedDevices.getConnectedDevice(0).getSerial(),
                LINEAR_ACC_PATH + "13");

        linearAccSubscription = Mds.builder().build(this).subscribe(URI_EVENTLISTENER, linearAccUri, new MdsNotificationListener() {
            @Override
            public void onNotification(String data) {
                Log.d(TAG, "Linear Acceleration Data: " + data);
                LinearAcceleration linearAccelerationData = new Gson().fromJson(data, LinearAcceleration.class);
                if (linearAccelerationData != null) {
                    LinearAcceleration.Array arrayData = linearAccelerationData.body.array[0];
                    xAxisLinearAccTextView.setText(String.format(Locale.getDefault(), "x: %.6f", arrayData.x));
                    yAxisLinearAccTextView.setText(String.format(Locale.getDefault(), "y: %.6f", arrayData.y));
                    zAxisLinearAccTextView.setText(String.format(Locale.getDefault(), "z: %.6f", arrayData.z));
                }
            }

            @Override
            public void onError(MdsException e) {
                Log.e(TAG, "Linear Acceleration Subscription Error: " + e.getMessage());
                Toast.makeText(MultiSensorSubscribeActivity.this, "Linear Acceleration Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                if (switchSubscriptionLinearAcc.isChecked()) {
                    switchSubscriptionLinearAcc.setChecked(false);
                }
            }
        });
    }

    private void subscribeToECG() {
        String ecgUri = FormatHelper.formatContractToJson(
                MovesenseConnectedDevices.getConnectedDevice(0).getSerial(),
                ECG_PATH + "125");

        ecgSubscription = Mds.builder().build(this).subscribe(URI_EVENTLISTENER, ecgUri, new MdsNotificationListener() {
            @Override
            public void onNotification(String data) {
                Log.d(TAG, "ECG Data: " + data);
                // Process ECG data if necessary, for example:
                ecgTextView.setText(data);  // You can format this as needed
            }

            @Override
            public void onError(MdsException e) {
                Log.e(TAG, "ECG Subscription Error: " + e.getMessage());
                Toast.makeText(MultiSensorSubscribeActivity.this, "ECG Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                if (switchSubscriptionECG.isChecked()) {
                    switchSubscriptionECG.setChecked(false);
                }
            }
        });
    }

    private void unsubscribeLinearAcc() {
        if (linearAccSubscription != null) {
            linearAccSubscription.unsubscribe();
            linearAccSubscription = null;
        }
    }

    private void unsubscribeECG() {
        if (ecgSubscription != null) {
            ecgSubscription.unsubscribe();
            ecgSubscription = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unsubscribeLinearAcc();
        unsubscribeECG();
    }
}

package com.movesense.showcaseapp.section_01_movesense.tests;

import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.movesense.mds.Mds;
import com.movesense.mds.MdsException;
import com.movesense.mds.MdsNotificationListener;
import com.movesense.mds.MdsSubscription;
import com.movesense.mds.internal.connectivity.MovesenseConnectedDevices;
import com.movesense.showcaseapp.R;
import com.movesense.showcaseapp.model.AngularVelocity;
import com.movesense.showcaseapp.model.EcgModel;
import com.movesense.showcaseapp.model.LinearAcceleration;
import com.movesense.showcaseapp.utils.FormatHelper;

import java.util.Locale;

public class MultiSensorSubscribeActivity extends AppCompatActivity {

    private static final String TAG = "MultiSensorSubscribe";
    private static final String LINEAR_ACC_PATH = "Meas/Acc/";
    private static final String ECG_PATH = "Meas/ECG/";
    private static final String GYRO_PATH = "Meas/Gyro/";

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
    private GraphView ecgGraphView;

    // ECG Graph
    private LineGraphSeries<DataPoint> ecgSeries;
    private int ecgDataPoints = 0; // Counter for ECG data points

    //GYRO
    private SwitchCompat switchSubscriptionGyro;
    private TextView xAxisGyroTextView;
    private TextView yAxisGyroTextView;
    private TextView zAxisGyroTextView;
    private MdsSubscription gyroSubscription;


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
        ecgGraphView = findViewById(R.id.ecg_graph_view);

        // Initialize ECG Graph
        ecgSeries = new LineGraphSeries<>();
        setupEcgGraph();

        switchSubscriptionGyro = findViewById(R.id.switchSubscriptionGyro);
        xAxisGyroTextView = findViewById(R.id.x_axis_gyro_textView);
        yAxisGyroTextView = findViewById(R.id.y_axis_gyro_textView);
        zAxisGyroTextView = findViewById(R.id.z_axis_gyro_textView);


        // Handle Linear Acceleration subscription toggle
        switchSubscriptionLinearAcc.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (MovesenseConnectedDevices.getConnectedDevices().size() > 0) {
                    subscribeToLinearAcc();
                } else {
                    Toast.makeText(this, "No connected device found", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(this, "No connected device found", Toast.LENGTH_SHORT).show();
                    switchSubscriptionECG.setChecked(false);
                }
            } else {
                unsubscribeECG();
            }
        });

        // GYRO Subscription
        switchSubscriptionGyro.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (MovesenseConnectedDevices.getConnectedDevices().size() > 0) {
                    subscribeToGyro();
                } else {
                    Toast.makeText(this, "No connected device found", Toast.LENGTH_SHORT).show();
                    switchSubscriptionGyro.setChecked(false);
                }
            } else {
                unsubscribeGyro();
            }
        });

    }

    private void setupEcgGraph() {
        ecgGraphView.addSeries(ecgSeries);
        ecgGraphView.getViewport().setXAxisBoundsManual(true);
        ecgGraphView.getViewport().setMinX(0);
        ecgGraphView.getViewport().setMaxX(500);

        ecgGraphView.getViewport().setYAxisBoundsManual(true);
        ecgGraphView.getViewport().setMinY(-5000);
        ecgGraphView.getViewport().setMaxY(5000);

        ecgGraphView.getViewport().setScrollable(false);
        ecgGraphView.getViewport().setScrollableY(false);

        ecgSeries.setColor(getResources().getColor(R.color.colorGreen));
    }

    private void subscribeToGyro() {
        String gyroUri = FormatHelper.formatContractToJson(
                MovesenseConnectedDevices.getConnectedDevice(0).getSerial(),
                GYRO_PATH + "13" // Change the rate if needed
        );

        gyroSubscription = Mds.builder().build(this).subscribe(URI_EVENTLISTENER, gyroUri, new MdsNotificationListener() {
            @Override
            public void onNotification(String data) {
                Log.d(TAG, "Gyro Data: " + data);
                AngularVelocity gyroData = new Gson().fromJson(data, AngularVelocity.class);

                if (gyroData != null && gyroData.body != null && gyroData.body.array.length > 0) {
                    AngularVelocity.Array arrayData = gyroData.body.array[0];
                    xAxisGyroTextView.setText(String.format(Locale.getDefault(), "x: %.6f", arrayData.x));
                    yAxisGyroTextView.setText(String.format(Locale.getDefault(), "y: %.6f", arrayData.y));
                    zAxisGyroTextView.setText(String.format(Locale.getDefault(), "z: %.6f", arrayData.z));
                }
            }

            @Override
            public void onError(MdsException e) {
                Log.e(TAG, "Gyro Error: " + e.getMessage());
                switchSubscriptionGyro.setChecked(false);
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
                Log.e(TAG, "Linear Acc Error: " + e.getMessage());
                switchSubscriptionLinearAcc.setChecked(false);
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

                EcgModel ecgData = new Gson().fromJson(data, EcgModel.class);
                if (ecgData != null && ecgData.getBody() != null) {
                    int[] ecgSamples = ecgData.getBody().getData();
                    for (int sample : ecgSamples) {
                        ecgSeries.appendData(
                                new DataPoint(ecgDataPoints++, sample),
                                false,
                                500
                        );
                        if (ecgDataPoints >= 500) {
                            ecgDataPoints = 0;
                            ecgSeries.resetData(new DataPoint[0]);
                        }
                    }
                }
            }

            @Override
            public void onError(MdsException e) {
                Log.e(TAG, "ECG Error: " + e.getMessage());
                switchSubscriptionECG.setChecked(false);
            }
        });
    }

    private void unsubscribeGyro() {
        if (gyroSubscription != null) {
            gyroSubscription.unsubscribe();
            gyroSubscription = null;
        }
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
        unsubscribeGyro();

    }
}

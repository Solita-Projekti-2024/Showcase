package com.movesense.showcaseapp.section_01_movesense.tests;

import android.app.UiAutomation;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.ImageView;
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
import com.movesense.showcaseapp.model.HeartRate;

import com.movesense.showcaseapp.model.LinearAcceleration;
import com.movesense.showcaseapp.utils.FormatHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Locale;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AlertDialog;
import android.util.Log;

public class MultiSensorSubscribeActivity extends AppCompatActivity {

    private static final String TAG = "MultiSensorSubscribe";
    private static final String LINEAR_ACC_PATH = "Meas/Acc/";
    private static final String ECG_PATH = "Meas/ECG/";
    private static final String GYRO_PATH = "Meas/Gyro/";
    private final String HEART_RATE_PATH = "Meas/Hr";


    private static final String URI_EVENTLISTENER = "suunto://MDS/EventListener";

    private MdsSubscription linearAccSubscription;
    private MdsSubscription ecgSubscription;

    // Views
    private MdsSubscription heartRateSubscription;

    private TextView xAxisLinearAccTextView;
    private TextView yAxisLinearAccTextView;
    private TextView zAxisLinearAccTextView;
    private TextView ecgTextView;
    private GraphView ecgGraphView;

    // ECG Graph
    private LineGraphSeries<DataPoint> ecgSeries;
    private int ecgDataPoints = 0; // Counter for ECG data points

    //GYRO
    private TextView xAxisGyroTextView;
    private TextView yAxisGyroTextView;
    private TextView zAxisGyroTextView;
    private MdsSubscription gyroSubscription;

    // HR
    private TextView heartRateTextView; // TextView for heart rate

    // Define window size for the median filter
    private static final int WINDOW_SIZE = 7;

    // Buffers to hold the recent values for each axis
    private LinkedList<Float> xBuffer = new LinkedList<>();
    private LinkedList<Float> yBuffer = new LinkedList<>();
    private LinkedList<Float> zBuffer = new LinkedList<>();

    private File csvFile;
    private StringBuilder csvRowBuffer = new StringBuilder();

    private Handler popupHandler;
    private Runnable alertRunnable;
    private boolean tiltExceeded = false; // Flag to check if tilt threshold is exceeded
    private long tiltStartTime = 0;  // To track when the tilt exceeds the threshold
    private AlertDialog alertDialog;
    private double currentHeartRate = 0.0;
    private static final double HEART_RATE_THRESHOLD = 120.0;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.measurements); // Ensure this matches your XML file name

        // Initialize the handler
        popupHandler = new Handler(Looper.getMainLooper());

        // Init CSV
        csvFile = new File(getExternalFilesDir(null), "sensor_measurements.csv");
        if (!csvFile.exists()) {
            try (FileWriter writer = new FileWriter(csvFile, true)) {
                writer.append("Timestamp;LinearAccX;LinearAccY;LinearAccZ;GyroX;GyroY;GyroZ;HeartRate;ECG\n");
            } catch (IOException e) {
                Log.e(TAG, "Error creating CSV", e);
            }
        }

        // Clean old data from CSV
        //cleanOldCsvData();



        // Initialize views based on updated XML layout
        xAxisLinearAccTextView = findViewById(R.id.x_axis_linearAcc_textView);
        yAxisLinearAccTextView = findViewById(R.id.y_axis_linearAcc_textView);
        zAxisLinearAccTextView = findViewById(R.id.z_axis_linearAcc_textView);

        ecgGraphView = findViewById(R.id.ecg_graph_view);

        heartRateTextView = findViewById(R.id.heart_rate_textView);

        xAxisGyroTextView = findViewById(R.id.x_axis_gyro_textView);
        yAxisGyroTextView = findViewById(R.id.y_axis_gyro_textView);
        zAxisGyroTextView = findViewById(R.id.z_axis_gyro_textView);


        // ECG Graph initialization
        ecgSeries = new LineGraphSeries<>();
        setupEcgGraph();



    // Automatically enable subscriptions
        if (MovesenseConnectedDevices.getConnectedDevices().size() > 0) {
            subscribeToLinearAcc();
            subscribeToECG();
            subscribeToGyro();
            subscribeToHeartRate();


        } else {
            Toast.makeText(this, "No connected device found", Toast.LENGTH_SHORT).show();
        }








    }

    private void logDataToCsv(String linearAccData, String gyroData, String heartRateData, String ecgData) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(new Date());

        // Append data to row buffer
        csvRowBuffer.setLength(0); // Clear previous row
        csvRowBuffer
                .append(timestamp).append(";")
                .append(linearAccData).append(";")
                .append(gyroData).append(";")
                .append(heartRateData).append(";")
                .append(ecgData).append("\n");

        // Write the row to the file
        try (FileWriter writer = new FileWriter(csvFile, true)) {
            writer.append(csvRowBuffer.toString());
        } catch (IOException e) {
            Log.e(TAG, "Error writing to CSV file", e);
        }
    }

    private void cleanOldCsvData() {
        File tempFile = new File(getExternalFilesDir(null), "temp_sensor_measurements.csv");
        long currentTime = System.currentTimeMillis();
        long twentyFourHoursInMillis = 24 * 60 * 60 * 1000;

        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile));
             FileWriter writer = new FileWriter(tempFile)) {

            String header = reader.readLine();
            if (header != null) {
                writer.append(header).append("\n");
            }

            String line;
            while ((line = reader.readLine()) != null) {
                String[] columns = line.split(";");
                if (columns.length > 0) {
                    String timeStampStr = columns[0];
                    SimpleDateFormat dateFormat;

                    // Handle different timestamp formats
                    if (timeStampStr.contains("-")) {
                        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
                    } else {
                        dateFormat = new SimpleDateFormat("dd.M.yyyy HH:mm", Locale.getDefault());
                    }

                    try {
                        Date timestamp = dateFormat.parse(timeStampStr);
                        if (timestamp != null && currentTime - timestamp.getTime() <= twentyFourHoursInMillis) {
                            writer.append(line).append("\n");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing timestamp: " + timeStampStr, e);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning old CSV data", e);
        }

        if (tempFile.exists() && csvFile.delete()) {
            tempFile.renameTo(csvFile);
        }
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

    // Method to monitor the tilt
    private void monitorTilt(double tiltValue) {
        // Define your tilt threshold (e.g., 30 degrees)
        double tiltThreshold = 30.0;

        if (Math.abs(tiltValue) > tiltThreshold && currentHeartRate > HEART_RATE_THRESHOLD) {
            if (!tiltExceeded) {
                tiltExceeded = true;
                tiltStartTime = System.currentTimeMillis();  // Record the start time of tilt
            } else {
                // Check if the tilt has been sustained for 10 seconds
                if (System.currentTimeMillis() - tiltStartTime >= 10000) {
                    if (alertDialog == null || !alertDialog.isShowing()) {
                        showPopup("Korkea syke makuuasennossa havaittu!\nTarvitsetko apua?\n\n'Hätätila! SOS' lähettää hälytyksen\n\n'Olen OK' kuittaa väärän hälytyksen");
                    }}
            }
        } else {
            tiltExceeded = false;  // Reset if tilt goes below threshold
        }
    }

    private void sendAlert() {
        // Logic to send the alert (e.g., send a notification, log an event, etc.)
        Log.d("MultiSensorSubscribe", "Alert sent!");
    }

    private void showPopup(String message) {
        if (alertDialog != null && alertDialog.isShowing()) {
            // Dialog is already displayed, do not show another
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Alert")
                .setMessage(message)
                .setPositiveButton("Hätätila! SOS", (dialog, which) -> dialog.dismiss())
                .setNegativeButton("Olen OK", (dialog, which) -> dialog.dismiss());

        alertDialog = builder.create();
        if (!isFinishing()) {
            alertDialog.show();
        }
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

                    String gyroDataStr = String.format(Locale.getDefault(), "%.6f;%.6f;%.6f", arrayData.x, arrayData.y, arrayData.z);

                    // Log data with placeholders for other sensors
                    logDataToCsv("N/A;N/A;N/A", gyroDataStr, "N/A", "N/A");
                }
            }

            @Override
            public void onError(MdsException e) {
                Log.e(TAG, "Gyro Error: " + e.getMessage());
            }
        });
    }


    private float calculateMedian(LinkedList<Float> buffer) {
        LinkedList<Float> sortedBuffer = new LinkedList<>(buffer);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            sortedBuffer.sort(Float::compareTo);
        }
        int middle = sortedBuffer.size() / 2;
        if (sortedBuffer.size() % 2 == 0) {
            return (sortedBuffer.get(middle - 1) + sortedBuffer.get(middle)) / 2.0f;
        } else {
            return sortedBuffer.get(middle);
        }
    }

    // Method to calculate both pitch and roll, and return the greater absolute angle
    private float calculateMaxTilt(float accX, float accY, float accZ) {
        // Calculate pitch (forward/backward tilt)
        float pitch = (float) Math.toDegrees(Math.atan2(accZ, Math.sqrt(accX * accX + accY * accY)));

        // Calculate roll (left/right tilt)
        float roll = (float) Math.toDegrees(Math.atan2(accX, Math.sqrt(accY * accY + accZ * accZ)));

        // Return the greater of the two angles (by absolute value)
        return Math.abs(pitch) > Math.abs(roll) ? pitch : roll;
    }



    private void subscribeToLinearAcc() {
        String linearAccUri = FormatHelper.formatContractToJson(
                MovesenseConnectedDevices.getConnectedDevice(0).getSerial(),
                LINEAR_ACC_PATH + "13");

        linearAccSubscription = Mds.builder().build(this).subscribe(URI_EVENTLISTENER, linearAccUri, new MdsNotificationListener() {
            @Override
            public void onNotification(String data) {
                Log.d(TAG, "Linear Acceleration Data: " + data);
                ImageView stickmanImage = findViewById(R.id.image_stickman);

                LinearAcceleration linearAccelerationData = new Gson().fromJson(data, LinearAcceleration.class);
                if (linearAccelerationData != null) {
                    LinearAcceleration.Array arrayData = linearAccelerationData.body.array[0];

                    // Update buffers
                    if (xBuffer.size() >= WINDOW_SIZE) xBuffer.poll();  // Remove oldest value
                    if (yBuffer.size() >= WINDOW_SIZE) yBuffer.poll();
                    if (zBuffer.size() >= WINDOW_SIZE) zBuffer.poll();
                    xBuffer.add((float) arrayData.x);
                    yBuffer.add((float) arrayData.y);
                    zBuffer.add((float) arrayData.z);

                    // Apply median filter
                    float filteredX = calculateMedian(xBuffer);
                    float filteredY = calculateMedian(yBuffer);
                    float filteredZ = calculateMedian(zBuffer);

                    xAxisLinearAccTextView.setText(String.format(Locale.getDefault(), "x: %.6f", filteredX));
                    yAxisLinearAccTextView.setText(String.format(Locale.getDefault(), "y: %.6f", filteredY));
                    zAxisLinearAccTextView.setText(String.format(Locale.getDefault(), "z: %.6f", filteredZ));

                    float maxTilt = calculateMaxTilt(filteredX, filteredY, filteredZ);

                    String linearAccData = String.format(Locale.getDefault(), "%.6f;%.6f;%.6f", filteredX, filteredY, filteredZ);

                    // Log data with placeholders for other sensors
                    logDataToCsv(linearAccData, "N/A;N/A;N/A", "N/A", "N/A");


                    monitorTilt(maxTilt);
                    runOnUiThread(() -> {
                        stickmanImage.setRotation(maxTilt);
                                });


                    //Check leaning
                    /*if (filteredY < 6.5 && filteredY > 3.2) {
                        Log.d(TAG, "Person is leaning");
                        //TÄÄLLÄ PITÄISI JOTAIN TEHDÄ
                        stickmanImage.setRotation(45);
                    } else if (filteredY <= 3.2) {
                        Log.d(TAG, "Person is leaning SIGNIFICANTLY or HAS FALLEN");
                        //VARMAAN JOTAIN PITÄIS TÄÄLLÄKIN TEHDÄ
                        stickmanImage.setRotation(90);
                    } else {
                        stickmanImage.setRotation(0);
                    }*/



                }
            }

            @Override
            public void onError(MdsException e) {
                Log.e(TAG, "Linear Acc Error: " + e.getMessage());
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
                        String ecgDataStr = String.format(Locale.getDefault(), "%d", sample);

                        // Log data with placeholders for other sensors
                        logDataToCsv("N/A;N/A;N/A", "N/A;N/A;N/A", "N/A", ecgDataStr);
                    }
                }
            }

            @Override
            public void onError(MdsException e) {
                Log.e(TAG, "ECG Error: " + e.getMessage());
            }
        });
    }

    private void unsubscribeGyro() {
        if (gyroSubscription != null) {
            gyroSubscription.unsubscribe();
            gyroSubscription = null;
        }
    }
    // Subscribe to Heart Rate
    private void subscribeToHeartRate() {
        String heartRateUri = FormatHelper.formatContractToJson(
                MovesenseConnectedDevices.getConnectedDevice(0).getSerial(),
                HEART_RATE_PATH
        );

        heartRateSubscription = Mds.builder().build(this).subscribe(URI_EVENTLISTENER, heartRateUri, new MdsNotificationListener() {
            @Override
            public void onNotification(String data) {
                Log.d(TAG, "Heart Rate Data: " + data);
                HeartRate heartRate = new Gson().fromJson(data, HeartRate.class);

                if (heartRate != null) {
                    heartRateTextView.setText(String.format(Locale.getDefault(),
                            "Heart Rate: %.2f bpm", heartRate.body.average));

                    String heartRateData = String.format(Locale.getDefault(), "%.2f", heartRate.body.average);
                    currentHeartRate = heartRate.body.average;

                    // Log data with placeholders for other sensors
                    logDataToCsv("N/A;N/A;N/A", "N/A;N/A;N/A", heartRateData, "N/A");
                }
            }

            @Override
            public void onError(MdsException e) {
                Log.e(TAG, "Heart Rate Error: " + e.getMessage());
            }
        });
    }

    // Unsubscribe from Heart Rate
    private void unsubscribeHeartRate() {
        if (heartRateSubscription != null) {
            heartRateSubscription.unsubscribe();
            heartRateSubscription = null;
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
        unsubscribeHeartRate(); // Unsubscribe heart rate when destroying activity

        if (popupHandler != null){
            popupHandler.removeCallbacksAndMessages(null);
        }


    }
}

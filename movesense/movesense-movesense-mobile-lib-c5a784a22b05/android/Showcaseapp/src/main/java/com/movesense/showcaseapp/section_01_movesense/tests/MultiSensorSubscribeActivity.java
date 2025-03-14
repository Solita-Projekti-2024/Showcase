package com.movesense.showcaseapp.section_01_movesense.tests;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
import java.text.ParseException;
import java.util.LinkedList;
import java.util.Locale;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AlertDialog;

import android.view.View;
import android.widget.Button;
import android.content.Context;




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



    // ECG Graph
    private LineGraphSeries<DataPoint> ecgSeries;
    private int ecgDataPoints = 0; // Counter for ECG data points


    private MdsSubscription gyroSubscription;

    // HR
    private TextView heartRateTextView; // TextView for heart rate

    // Define window size for the median filter
    private static final int WINDOW_SIZE = 7;

    // Buffers to hold the recent values for each axis
    private LinkedList<Float> xBuffer = new LinkedList<>();
    private LinkedList<Float> yBuffer = new LinkedList<>();
    private LinkedList<Float> zBuffer = new LinkedList<>();

    private StringBuilder csvRowBuffer = new StringBuilder();

    private Handler popupHandler;
    private boolean tiltExceeded = false; // Flag to check if tilt threshold is exceeded
    private long tiltStartTime = 0;  // To track when the tilt exceeds the threshold
    private AlertDialog alertDialog;
    private double currentHeartRate = 0.0;
    private static final double HEART_RATE_THRESHOLD = 120.0;
    private boolean gyroThresholdExceeded = false;
    private long gyroThresholdTime = 0;
    private static final long MONITOR_DURATION = 1000;
    private boolean alertAcknowledged = false;
    private File linearAccFile;
    private File gyroFile;
    private File heartRateFile;
    private File ecgFile;


    private String classifyActivity(float accX, float accY, float accZ) {


        float uprightLowerThreshold = 9.2f;
        float uprightUpperThreshold = 10.4f;

        if (accY >= uprightLowerThreshold && accY <= uprightUpperThreshold) {
            return "Upright";
        } else if (accY < 4.0f) {
            return "Lying";
        } else {
            return "Walking";
        }
    }

    private void initializeCsvFile(File file, String header) {
        try {
            if (!file.exists()) {
                FileWriter writer = new FileWriter(file);
                writer.append(header);
                writer.flush();
                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.measurements); // Ensure this matches your XML file name

        // Initialize the handler
        popupHandler = new Handler(Looper.getMainLooper());

        Button btnBack3 = findViewById(R.id.btn_back3);

        btnBack3.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                onDestroy();
                System.exit(0);
            }
        });

        Button fullDataButton = findViewById(R.id.full_data);

        fullDataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), DebugView.class);
                startActivity(intent);
            }
        });

        Button yourButton = findViewById(R.id.activity);
        yourButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Action to perform on button click
                classifyActivitiesFromCsv();
                Toast.makeText(getApplicationContext(), "Button clicked! Analyzing last 24 hours of data.", Toast.LENGTH_SHORT).show();
            }
        });

        // Init CSV
        linearAccFile = new File(getExternalFilesDir(null), "linear_acceleration.csv");
        gyroFile = new File(getExternalFilesDir(null), "gyroscope.csv");
        heartRateFile = new File(getExternalFilesDir(null), "heart_rate.csv");
        ecgFile = new File(getExternalFilesDir(null), "ecg.csv");

        // Initialize CSV files with headers if they don’t exist
        initializeCsvFile(linearAccFile, "Timestamp;LinearAccX;LinearAccY;LinearAccZ\n");
        initializeCsvFile(gyroFile, "Timestamp;GyroX;GyroY;GyroZ\n");
        initializeCsvFile(heartRateFile, "Timestamp;HeartRate\n");
        initializeCsvFile(ecgFile, "Timestamp;ECG\n");

        heartRateTextView = findViewById(R.id.heart_rate_textView);

        // ECG Graph initialization
        ecgSeries = new LineGraphSeries<>();

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

    private void logDataToCsv(File file, String data) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(new Date());

        try (FileWriter writer = new FileWriter(file, true)) {
            writer.append(timestamp).append(";").append(data).append("\n");
        } catch (IOException e) {
            Log.e(TAG, "Error writing to " + file.getName(), e);
        }
    }

    // Method to monitor the tilt
    private void monitorTilt(double tiltValue) {
        double tiltThreshold = 30.0;

        // Always check if the tilt is above the threshold, regardless of heart rate
        if (Math.abs(tiltValue) > tiltThreshold) {
            if (!tiltExceeded) {
                tiltExceeded = true;
                tiltStartTime = System.currentTimeMillis();
            } else if (System.currentTimeMillis() - tiltStartTime >= 10000) {
                if (!alertAcknowledged && (alertDialog == null || !alertDialog.isShowing()) && currentHeartRate > HEART_RATE_THRESHOLD) {
                    String message = "Korkea syke makuuasennossa havaittu!\nTarvitsetko apua?\n\n'Hätätila! SOS' lähettää hälytyksen\n\n'Olen OK' kuittaa väärän hälytyksen";
                    showPopup(message);
                }
            }
        } else {
            if (alertDialog != null && alertDialog.isShowing()) {
                alertDialog.dismiss();
                alertDialog = null;
            }
            alertAcknowledged = false;
            tiltExceeded = false;
        }
    }

    private void showPopup(String message) {
        if (alertDialog != null && alertDialog.isShowing()) {
            // Dialog is already displayed, do not show another
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Alert")
                .setMessage(message)
                .setPositiveButton("Hätätila! SOS", (dialog, which) -> {
                    alertAcknowledged = true;
                    dialog.dismiss();
                    alertDialog = null;
                    tiltExceeded = false; // Reset the tilt flag after the alert is acknowledged
                    sendAlert(this, message);  // Send alert when SOS is pressed
                })
                .setNegativeButton("Olen OK", (dialog, which) -> {
                    alertAcknowledged = true;
                    dialog.dismiss();
                    alertDialog = null;
                    tiltExceeded = false; // Reset the tilt flag after the alert is acknowledged
                });

        alertDialog = builder.create();
        if (!isFinishing()) {
            alertDialog.show();
        }
    }

    private void sendAlert(Context context, String message) {
        // Logic to send the alert (e.g., send a notification, log an event, etc.)
        if(message.contains("Kaatuminen")){
            FhirMessageCreator.createFallObservation(context, "12345");
        } else if (message.contains("Korkea")) {
            FhirMessageCreator.createHighHeartRateObservation(context,"12345", currentHeartRate);
        }
        Log.d("MultiSensorSubscribe", "Alert sent!");
    }

    private void subscribeToGyro() {
        String gyroUri = FormatHelper.formatContractToJson(
                MovesenseConnectedDevices.getConnectedDevice(0).getSerial(),
                GYRO_PATH + "13" // Change the rate if needed
        );

        gyroSubscription = Mds.builder().build(this).subscribe(URI_EVENTLISTENER, gyroUri, new MdsNotificationListener() {
            @Override
            public void onNotification(String data) {
                //Log.d(TAG, "Gyro Data: " + data);
                AngularVelocity gyroData = new Gson().fromJson(data, AngularVelocity.class);

                if (gyroData != null && gyroData.body != null && gyroData.body.array.length > 0) {
                    AngularVelocity.Array arrayData = gyroData.body.array[0];

                    double gyroX = arrayData.x;
                    double gyroZ = arrayData.z;

/*                    xAxisGyroTextView.setText(String.format(Locale.getDefault(), "x: %.6f", arrayData.x));
                    yAxisGyroTextView.setText(String.format(Locale.getDefault(), "y: %.6f", arrayData.y));
                    zAxisGyroTextView.setText(String.format(Locale.getDefault(), "z: %.6f", arrayData.z));*/

                    double gyroThreshold = 200.0;
                    if(Math.abs(gyroX) > gyroThreshold || Math.abs(gyroZ) > gyroThreshold){
                        if(!gyroThresholdExceeded){
                            gyroThresholdExceeded = true;
                            gyroThresholdTime = System.currentTimeMillis();
                            Log.d(TAG, "Gyro Threshold exceeded. Start tilt monitor");
                        }
                    }

                    String gyroDataStr = String.format(Locale.getDefault(), "%.6f;%.6f;%.6f", arrayData.x, arrayData.y, arrayData.z);
                    logDataToCsv(gyroFile, gyroDataStr);
                }
            }

            @Override
            public void onError(MdsException e) {
                Log.e(TAG, "Gyro Error: " + e.getMessage());
            }
        });
    }

    private volatile boolean keepRunning = true; // Add this flag to control the loop

    private Thread processingThread;

    private void classifyActivitiesFromCsv() {
        processingThread = new Thread(() -> {
            long currentTime = System.currentTimeMillis();
            long twentyFourHoursInMillis = 24 * 60 * 60 * 1000;

            float lyingTime = 0, uprightTime = 0, walkingTime = 0;

            // Define multiple date formats
            String[] datePatterns = {
                    "yyyy-MM-dd HH:mm:ss.SSS", // Format 1: 2025-02-08 10:14:57.027
                    "d.M.yyyy  HH:mm:ss"      // Format 2: 8.2.2025 10:15:02
            };

            try (BufferedReader reader = new BufferedReader(new FileReader(linearAccFile))) {
                String line;
                while ((line = reader.readLine()) != null && keepRunning) { // Check flag here
                    String[] columns = line.split(";");
                    if (columns.length >= 4) {
                        String timestampStr = columns[0];
                        Date timestamp = null;

                        // Try parsing with each pattern
                        for (String pattern : datePatterns) {
                            try {
                                SimpleDateFormat dateFormat = new SimpleDateFormat(pattern, Locale.getDefault());
                                timestamp = dateFormat.parse(timestampStr);
                                if (timestamp != null) {
                                    break; // If successful, break out of the loop
                                }
                            } catch (ParseException e) {
                                // Skip to next pattern if parsing fails
                            }
                        }

                        if (timestamp != null && (currentTime - timestamp.getTime()) <= twentyFourHoursInMillis) {
                            // Check and replace commas with periods, also handle "N/A"
                            try {
                                float accX = columns[1].equals("N/A") ? Float.NaN : Float.parseFloat(columns[1].replace(',', '.'));
                                float accY = columns[2].equals("N/A") ? Float.NaN : Float.parseFloat(columns[2].replace(',', '.'));
                                float accZ = columns[3].equals("N/A") ? Float.NaN : Float.parseFloat(columns[3].replace(',', '.'));

                                // Skip rows with invalid accelerometer data
                                if (Float.isNaN(accX) || Float.isNaN(accY) || Float.isNaN(accZ)) {
                                    continue;
                                }

                                String activity = classifyActivity(accX, accY, accZ);
                                if (activity.equals("Lying")) {
                                    lyingTime += 1;
                                } else if (activity.equals("Upright")) {
                                    uprightTime += 1;
                                } else if (activity.equals("Walking")) {
                                    walkingTime += 1;
                                }
                            } catch (NumberFormatException e) {
                                // Log the error and continue with the next line
                                Log.e(TAG, "Error parsing accelerometer values", e);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error reading or parsing CSV", e);
            }

            // Convert time to total seconds (lyingTime, uprightTime, walkingTime are in 1/13ths of a second)
            int totalLyingSeconds = (int) (lyingTime * (1.0f / 13));
            int totalUprightSeconds = (int) (uprightTime * (1.0f / 13));
            int totalWalkingSeconds = (int) (walkingTime * (1.0f / 13));

            // Now, break down the total seconds into hours, minutes, and seconds
            int lyingHours = totalLyingSeconds / 3600;
            int lyingMinutes = (totalLyingSeconds % 3600) / 60;
            int lyingSeconds = totalLyingSeconds % 60;

            int uprightHours = totalUprightSeconds / 3600;
            int uprightMinutes = (totalUprightSeconds % 3600) / 60;
            int uprightSeconds = totalUprightSeconds % 60;

            int walkingHours = totalWalkingSeconds / 3600;
            int walkingMinutes = (totalWalkingSeconds % 3600) / 60;
            int walkingSeconds = totalWalkingSeconds % 60;

            // Log the times in hours, minutes, and seconds
            Log.d(TAG, "Time in Lying: " + lyingHours + " hours " + lyingMinutes + " minutes " + lyingSeconds + " seconds");
            Log.d(TAG, "Time in Upright: " + uprightHours + " hours " + uprightMinutes + " minutes " + uprightSeconds + " seconds");
            Log.d(TAG, "Time in Walking: " + walkingHours + " hours " + walkingMinutes + " minutes " + walkingSeconds + " seconds");

            // Update the UI to display the times
            runOnUiThread(() -> {
                // Update your UI elements here (e.g., TextViews or Toasts)
                Toast.makeText(getApplicationContext(),
                        "Time in Lying: " + lyingHours + " hours " + lyingMinutes + " minutes " + lyingSeconds + " seconds\n" +
                                "Time in Upright: " + uprightHours + " hours " + uprightMinutes + " minutes " + uprightSeconds + " seconds\n" +
                                "Time in Walking: " + walkingHours + " hours " + walkingMinutes + " minutes " + walkingSeconds + " seconds",
                        Toast.LENGTH_LONG).show();
                // Find TextViews for status boxes
                TextView lyingTimeTextView = findViewById(R.id.status_makuulla_time);
                TextView uprightTimeTextView = findViewById(R.id.status_pystyssa_time);
                TextView walkingTimeTextView = findViewById(R.id.status_aktiivinen_time);

                // Format time as HH:mm:ss
                String lyingTimeFormatted = String.format(Locale.getDefault(), "%02d:%02d:%02d", lyingHours, lyingMinutes, lyingSeconds);
                String uprightTimeFormatted = String.format(Locale.getDefault(), "%02d:%02d:%02d", uprightHours, uprightMinutes, uprightSeconds);
                String walkingTimeFormatted = String.format(Locale.getDefault(), "%02d:%02d:%02d", walkingHours, walkingMinutes, walkingSeconds);

                // Update the TextViews
                lyingTimeTextView.setText(lyingTimeFormatted);
                uprightTimeTextView.setText(uprightTimeFormatted);
                walkingTimeTextView.setText(walkingTimeFormatted);
            });
        });

        processingThread.start();
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
                //Log.d(TAG, "Linear Acceleration Data: " + data);
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

                    float maxTilt = calculateMaxTilt(filteredX, filteredY, filteredZ);

                    if (gyroThresholdExceeded){
                        long elapsedTime = System.currentTimeMillis() - gyroThresholdTime;
                        if (elapsedTime <= MONITOR_DURATION){
                            if (Math.abs(maxTilt) > 30.0){
                                Log.d(TAG, "Tilt threshold exceeded durng fall monitoring");
                                if(alertDialog == null || !alertDialog.isShowing()){
                                    showPopup("Kaatuminen havaittu!\nTarvitsetko apua?\n'OLEN OK' kuittaa väärän hälytyksen\n'HÄTÄTILA SOS!' lähettää hälytyksen");
                                    tiltExceeded = false;
                                }
                            }
                        }else{
                            gyroThresholdExceeded = false;
                            Log.d(TAG, "Fall monitoring ended");
                        }
                    }

                    String linearAccData = String.format(Locale.getDefault(), "%.6f;%.6f;%.6f", arrayData.x, arrayData.y, arrayData.z);
                    logDataToCsv(linearAccFile, linearAccData);

                    monitorTilt(maxTilt);
                    runOnUiThread(() -> {
                        stickmanImage.setRotation(maxTilt);
                                });
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
                //Log.d(TAG, "ECG Data: " + data);

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
                        logDataToCsv(ecgFile, ecgDataStr);
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
                //Log.d(TAG, "Heart Rate Data: " + data);
                HeartRate heartRate = new Gson().fromJson(data, HeartRate.class);

                if (heartRate != null) {
                    heartRateTextView.setText(String.format(Locale.getDefault(),
                            "  Syke: %.2f bpm", heartRate.body.average));

                    String heartRateData = String.format(Locale.getDefault(), "%.2f", heartRate.body.average);
                    currentHeartRate = heartRate.body.average;

                    // Log data with placeholders for other sensors
                    logDataToCsv(heartRateFile, heartRateData);
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
        keepRunning = false;

        // Wait for the background thread to finish if it's still running
        if (processingThread != null && processingThread.isAlive()) {
            try {
                processingThread.join(); // This will block until the thread finishes
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

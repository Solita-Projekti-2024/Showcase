package com.movesense.showcaseapp.section_01_movesense.tests;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

public class FhirMessageCreator {

    private static final String FILE_NAME = "fhir_alerts.json"; // File name for saved messages

    public static void createFallObservation(Context context, String patientId) {
        try {
            JSONObject observation = generateFallObservation(patientId);
            saveToFile(context, observation);
        } catch (Exception e) {
            Log.e("FhirMessageCreator", "Error creating fall observation", e);
        }
    }

    public static void createHighHeartRateObservation(Context context, String patientId, double heartRate) {
        try {
            JSONObject observation = generateHighHeartRateObservation(patientId, heartRate);
            saveToFile(context, observation);
        } catch (Exception e) {
            Log.e("FhirMessageCreator", "Error creating heart rate observation", e);
        }
    }

    private static JSONObject generateFallObservation(String patientId) throws JSONException {
        JSONObject observation = new JSONObject();

        observation.put("resourceType", "Observation");

        // Unique identifier
        JSONObject identifier = new JSONObject();
        identifier.put("system", "http://example.com/identifiers");
        identifier.put("value", "fall-" + System.currentTimeMillis());

        // Category
        JSONObject categoryCoding = new JSONObject();
        categoryCoding.put("system", "http://terminology.hl7.org/CodeSystem/observation-category");
        categoryCoding.put("code", "activity");
        categoryCoding.put("display", "Activity");

        JSONObject category = new JSONObject();
        category.put("coding", new JSONArray().put(categoryCoding));

        // Code (Fall event)
        JSONObject codeCoding = new JSONObject();
        codeCoding.put("system", "http://loinc.org");
        codeCoding.put("code", "8867-4");
        codeCoding.put("display", "Fall Detection");

        JSONObject code = new JSONObject();
        code.put("coding", new JSONArray().put(codeCoding));

        // Effective time
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            observation.put("effectiveDateTime", new Date().toInstant().toString());
        }
        observation.put("status", "final");

        // Subject (Patient)
        JSONObject subject = new JSONObject();
        subject.put("reference", "Patient/" + patientId);

        observation.put("identifier", new JSONArray().put(identifier));
        observation.put("category", new JSONArray().put(category));
        observation.put("code", code);
        observation.put("subject", subject);

        return observation;
    }

    private static JSONObject generateHighHeartRateObservation(String patientId, double heartRate) throws JSONException {
        JSONObject observation = new JSONObject();

        observation.put("resourceType", "Observation");

        // Unique identifier
        JSONObject identifier = new JSONObject();
        identifier.put("system", "http://example.com/identifiers");
        identifier.put("value", "hr-" + System.currentTimeMillis());

        // Category
        JSONObject categoryCoding = new JSONObject();
        categoryCoding.put("system", "http://terminology.hl7.org/CodeSystem/observation-category");
        categoryCoding.put("code", "vital-signs");
        categoryCoding.put("display", "Vital Signs");

        JSONObject category = new JSONObject();
        category.put("coding", new JSONArray().put(categoryCoding));

        // Code (Heart Rate)
        JSONObject codeCoding = new JSONObject();
        codeCoding.put("system", "http://loinc.org");
        codeCoding.put("code", "8867-4");
        codeCoding.put("display", "Heart Rate");

        JSONObject code = new JSONObject();
        code.put("coding", new JSONArray().put(codeCoding));

        // Effective time
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            observation.put("effectiveDateTime", new Date().toInstant().toString());
        }
        observation.put("status", "final");

        // Value (Heart Rate)
        JSONObject valueQuantity = new JSONObject();
        valueQuantity.put("value", heartRate);
        valueQuantity.put("unit", "beats/min");
        valueQuantity.put("system", "http://unitsofmeasure.org");
        valueQuantity.put("code", "/min");

        // Subject (Patient)
        JSONObject subject = new JSONObject();
        subject.put("reference", "Patient/" + patientId);

        observation.put("identifier", new JSONArray().put(identifier));
        observation.put("category", new JSONArray().put(category));
        observation.put("code", code);
        observation.put("valueQuantity", valueQuantity);
        observation.put("subject", subject);

        return observation;
    }

    private static void saveToFile(Context context, JSONObject observation) {
        // Get the app's internal directory where the file is stored
        File directory = context.getFilesDir();
        File file = new File(context.getExternalFilesDir(null), FILE_NAME);

        // Log the directory path
        Log.d("FhirMessageCreator", "Saving to directory: " + directory.getAbsolutePath());

        FileWriter writer = null;

        try {
            // If file doesn't exist, create it and write the header
            if (!file.exists()) {
                writer = new FileWriter(file);
                writer.flush();
            } else {
                // Append the observation to the existing file
                writer = new FileWriter(file, true);
            }

            // Write the observation as a JSON string
            writer.append(observation.toString(2)).append("\n\n");
            writer.flush(); // Ensure the data is written immediately
            Log.d("FhirMessageCreator", "FHIR observation saved: " + observation.toString(2));

        } catch (IOException | JSONException e) {
            Log.e("FhirMessageCreator", "Error writing to file", e);
        } finally {
            // Close the writer if it was opened
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    Log.e("FhirMessageCreator", "Error closing file writer", e);
                }
            }
        }
    }

}

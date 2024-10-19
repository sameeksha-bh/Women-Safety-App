package com.example.fin;


import android.Manifest;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_REQUEST_CODE = 101;
    private static final int SMS_REQUEST_CODE = 102;

    private SharedPreferences sharedPreferences;
    private LocationManager locationManager;
    private String userNumber;
    private String guardianNumber;
    private String guardianName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences("SafetyAppPrefs", MODE_PRIVATE);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        Button btnRegisterUserNumber = findViewById(R.id.btnRegisterUserNumber);
        Button btnRegisterGuardianNumber = findViewById(R.id.btnRegisterGuardianNumber);
        Button btnViewGuardians = findViewById(R.id.btnViewGuardians);
        Button btnViewInstructions = findViewById(R.id.btnViewInstructions);
        Button btnSendAlert = findViewById(R.id.btnSendAlert);

        btnRegisterUserNumber.setOnClickListener(view -> registerUserNumber());
        btnRegisterGuardianNumber.setOnClickListener(view -> showGuardianInputDialog());
        btnViewGuardians.setOnClickListener(view -> viewGuardians());
        btnViewInstructions.setOnClickListener(view -> viewInstructions());
        btnSendAlert.setOnClickListener(view -> sendAlert());

        userNumber = sharedPreferences.getString("UserNumber", null);
        guardianNumber = sharedPreferences.getString("GuardianNumber", null);

        requestLocationPermission();
        requestSmsPermission();
    }

    private void registerUserNumber() {
        // Show a dialog to enter the user number
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Register User Number");

        final EditText input = new EditText(this);
        input.setHint("Enter your phone number");
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String number = input.getText().toString().trim();
            if (!number.isEmpty()) {
                sharedPreferences.edit().putString("UserNumber", number).apply();
                Toast.makeText(this, "User number registered", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void showGuardianInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Register Guardian");

        View view = getLayoutInflater().inflate(R.layout.dialog_guardian_input, null);
        builder.setView(view);

        EditText etGuardianName = view.findViewById(R.id.etGuardianName);
        EditText etGuardianNumber = view.findViewById(R.id.etGuardianNumber);

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                guardianName = etGuardianName.getText().toString().trim();
                guardianNumber = etGuardianNumber.getText().toString().trim();

                if (!guardianName.isEmpty() && !guardianNumber.isEmpty()) {
                    sharedPreferences.edit().putString("GuardianName", guardianName).apply();
                    sharedPreferences.edit().putString("GuardianNumber", guardianNumber).apply();
                    Toast.makeText(MainActivity.this, "Guardian registered", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Please enter valid details", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void viewGuardians() {
        // Retrieve and display guardian info
        guardianName = sharedPreferences.getString("GuardianName", "No guardian registered");
        guardianNumber = sharedPreferences.getString("GuardianNumber", "No number registered");

        String info = "Guardian: " + guardianName + "\nNumber: " + guardianNumber;
        Toast.makeText(this, info, Toast.LENGTH_LONG).show();
    }

    private void viewInstructions() {
        // Display instructions to the user
        String instructions = "Instructions:\n1. Register your and guardian's number.\n2. Click 'Send Alert' in an emergency.";
        Toast.makeText(this, instructions, Toast.LENGTH_LONG).show();
        //Toast.makeText(this, instructions, Toast.LENGTH_LONG).show();
    }

    /*private void sendAlert() {
        if (guardianNumber == null) {
            Toast.makeText(this, "No guardian number registered", Toast.LENGTH_SHORT).show();
            return;
        }
        // Check location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        } else {
            // Get location and send SMS
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        }
    }*/

    private void sendAlert() {
        if (guardianNumber == null) {
            Toast.makeText(this, "No guardian number registered", Toast.LENGTH_SHORT).show();
            return;
        }
        // Check location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        } else {
            // Get location and send SMS
            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location != null) {
                sendSmsWithLocation(location);
            } else {
                // Request a single location update to get the latest location
                locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, new LocationListener() {
                    @Override
                    public void onLocationChanged(@NonNull Location location) {
                        sendSmsWithLocation(location);
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {}

                    @Override
                    public void onProviderEnabled(@NonNull String provider) {}

                    @Override
                    public void onProviderDisabled(@NonNull String provider) {}
                }, null);
            }
        }
    }
    private void sendSmsWithLocation(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        String message = "Help! I need assistance. My location: http://maps.google.com/maps?q=" + latitude + "," + longitude;

        SmsManager smsManager = SmsManager.getDefault();
        try {
            smsManager.sendTextMessage(guardianNumber, null, message, null, null);
            Toast.makeText(MainActivity.this, "Alert sent to guardian", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "Failed to send SMS", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            String message = "Help! I need assistance. My location: http://maps.google.com/maps?q=" + latitude + "," + longitude;

            /*SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(guardianNumber, null, message, null, null);
            Toast.makeText(MainActivity.this, "Alert sent to guardian", Toast.LENGTH_SHORT).show();

            // Stop location updates after getting the current location
            locationManager.removeUpdates(locationListener);*/
            SmsManager smsManager = SmsManager.getDefault();
            try {
                smsManager.sendTextMessage(guardianNumber, null, message, null, null);
                Toast.makeText(MainActivity.this, "Alert sent to guardian", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "Failed to send SMS", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }

        }
    };

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }
    }

    private void requestSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, SMS_REQUEST_CODE);
        }
    }

    //@Override
    /*public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST_CODE || requestCode == SMS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }*/

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "SMS permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

}
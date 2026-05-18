package com.example.mapapplication;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private Button btnMap;
    private double latitude;
    private double longitude;
    private double altitude;
    private float accuracy;

    RequestQueue requestQueue;
    String insertUrl = "http://10.0.2.2/map_project/createPosition.php";
    LocationManager locationManager;

    private static final int PERMISSION_REQUEST_CODE = 100;

    // Listener réutilisable pour pouvoir le supprimer proprement
    private final LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            latitude  = location.getLatitude();
            longitude = location.getLongitude();
            altitude  = location.getAltitude();
            accuracy  = location.getAccuracy();

            String msg = String.format(
                    getResources().getString(R.string.new_location),
                    latitude, longitude, altitude, accuracy);

            addPosition(latitude, longitude);
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
        }

        @Override public void onStatusChanged(String p, int s, Bundle e) {}
        @Override public void onProviderEnabled(@NonNull String p) {}
        @Override public void onProviderDisabled(@NonNull String p) {
            // Proposer d'activer la localisation si le fournisseur est coupé
            promptEnableLocation();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestQueue   = Volley.newRequestQueue(getApplicationContext());
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        btnMap = findViewById(R.id.btnMap);
        btnMap.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, GoogleMapActivity.class)));

        // Permissions nécessaires : uniquement la localisation
        String[] perms = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };

        boolean fineGranted   = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarseGranted = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (fineGranted || coarseGranted) {
            startLocationUpdates();
        } else {
            ActivityCompat.requestPermissions(this, perms, PERMISSION_REQUEST_CODE);
        }
    }

    /**
     * Démarre le suivi GPS + réseau et charge la dernière position connue immédiatement.
     */
    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // Vérifier si la localisation est activée dans les paramètres
        boolean gpsEnabled     = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!gpsEnabled && !networkEnabled) {
            promptEnableLocation();
            return;
        }

        // ── 1. Charger immédiatement la dernière position connue ──────────────
        Location lastKnown = null;

        if (gpsEnabled) {
            lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }
        if (lastKnown == null && networkEnabled) {
            lastKnown = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }

        if (lastKnown != null) {
            locationListener.onLocationChanged(lastKnown); // affiche et envoie immédiatement
        }

        // ── 2. S'abonner aux mises à jour (intervalles raisonnables pour tester) ──
        //    minTime = 10 s, minDistance = 10 m  (à ajuster en production)
        if (gpsEnabled) {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 10_000, 10, locationListener);
        }
        if (networkEnabled) {
            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, 10_000, 10, locationListener);
        }
    }

    /**
     * Affiche une boîte de dialogue invitant l'utilisateur à activer la localisation.
     */
    private void promptEnableLocation() {
        new AlertDialog.Builder(this)
                .setTitle("Localisation désactivée")
                .setMessage("Veuillez activer la localisation pour que l'application puisse vous suivre.")
                .setPositiveButton("Paramètres", (d, w) ->
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                .setNegativeButton("Annuler", null)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                Toast.makeText(this,
                        "Permission refusée. L'application ne peut pas fonctionner.",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Envoie latitude + longitude au serveur PHP.
     */
    void addPosition(final double lat, final double lon) {
        StringRequest request = new StringRequest(
                Request.Method.POST, insertUrl,
                response -> { /* succès */ },
                error -> {
                    String msg = "Erreur inconnue";
                    if (error.networkResponse != null) {
                        msg = "HTTP " + error.networkResponse.statusCode;
                    } else if (error.getMessage() != null) {
                        msg = error.getMessage();
                    } else if (error.getCause() != null) {
                        msg = error.getCause().toString();
                    }
                    Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
                }
        ) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                HashMap<String, String> params = new HashMap<>();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                params.put("latitude",  String.valueOf(lat));
                params.put("longitude", String.valueOf(lon));
                params.put("date",      sdf.format(new Date()));
                params.put("imei", Settings.Secure.getString(
                        getContentResolver(), Settings.Secure.ANDROID_ID));
                return params;
            }
        };
        requestQueue.add(request);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Toujours supprimer le listener pour éviter les fuites mémoire
        locationManager.removeUpdates(locationListener);
    }
}
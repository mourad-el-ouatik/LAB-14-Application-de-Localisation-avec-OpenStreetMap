package com.example.mapapplication;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

public class GoogleMapActivity extends AppCompatActivity {

    private MapView map;
    private RequestQueue requestQueue;
    private MyLocationNewOverlay myLocationOverlay;

    private final String showUrl = "http://10.0.2.2/map_project/getPosition.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().load(
                getApplicationContext(),
                getSharedPreferences("prefs", MODE_PRIVATE));

        setContentView(R.layout.activity_google_map);

        // ── Carte ────────────────────────────────────────────────────────────
        map = findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);
        map.getController().setZoom(15.0);

        // ── Overlay position de l'utilisateur (point bleu) ───────────────────
        myLocationOverlay = new MyLocationNewOverlay(
                new GpsMyLocationProvider(this), map);
        myLocationOverlay.enableMyLocation();

        // Recentrer automatiquement sur la position réelle au premier fix
        myLocationOverlay.runOnFirstFix(() -> runOnUiThread(() -> {
            GeoPoint myPos = myLocationOverlay.getMyLocation();
            if (myPos != null) {
                map.getController().animateTo(myPos);
            }
        }));

        map.getOverlays().add(myLocationOverlay);

        // ── Requêtes réseau ──────────────────────────────────────────────────
        requestQueue = Volley.newRequestQueue(getApplicationContext());
        loadPositions();
    }

    /**
     * Charge les positions depuis le serveur et les affiche comme marqueurs.
     */
    private void loadPositions() {
        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.POST, showUrl, null,
                response -> {
                    try {
                        JSONArray positions = response.getJSONArray("positions");
                        for (int i = 0; i < positions.length(); i++) {
                            JSONObject pos = positions.getJSONObject(i);
                            double lat = pos.getDouble("latitude");
                            double lng = pos.getDouble("longitude");
                            addMarker(lat, lng, "Position " + (i + 1));
                        }
                        map.invalidate();
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Erreur JSON : " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    error.printStackTrace();
                    Toast.makeText(this, "Erreur réseau : " + error.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
        );
        requestQueue.add(req);
    }

    /**
     * Crée et ajoute un marqueur à la carte.
     */
    private void addMarker(double lat, double lng, String title) {
        Marker marker = new Marker(map);
        marker.setPosition(new GeoPoint(lat, lng));
        marker.setTitle(title);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.marker);
        if (drawable != null) {
            Bitmap bmp    = drawableToBitmap(drawable);
            Bitmap scaled = Bitmap.createScaledBitmap(bmp, 80, 80, false);
            marker.setIcon(new BitmapDrawable(getResources(), scaled));
        }

        map.getOverlays().add(marker);
    }

    /**
     * Convertit n'importe quel Drawable en Bitmap (compatible VectorDrawable).
     */
    private Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }
        Bitmap bmp = Bitmap.createBitmap(
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bmp;
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
        myLocationOverlay.enableMyLocation();
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
        myLocationOverlay.disableMyLocation();
    }
}
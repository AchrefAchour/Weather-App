package essths.li3.weatherapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.events.MapEventsReceiver;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private EditText editCity;
    private Button button;
    private TextView textView;
    private MapView map;

    private Marker cityMarker;
    private RequestQueue requestQueue;

    private static final String API_KEY = BuildConfig.OPENWEATHER_API_KEY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Configuration.getInstance().setUserAgentValue(getPackageName());
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        editCity = findViewById(R.id.editCity);
        textView = findViewById(R.id.titre);
        button = findViewById(R.id.myButton);
        map = findViewById(R.id.map);

        requestQueue = Volley.newRequestQueue(this);

        map.setMultiTouchControls(true);
        map.getController().setZoom(10);
        map.getController().setCenter(new GeoPoint(36.8065, 10.1815));

        // Vérifier la connectivité réseau
        checkNetworkConnectivity();

        button.setOnClickListener(v -> {
            String city = editCity.getText().toString().trim();
            Log.d("WEATHER_APP", "=== BOUTON CLIQUE ===");
            Log.d("WEATHER_APP", "Ville saisie: '" + city + "'");

            if (!city.isEmpty()) {
                if (isValidCityName(city)) {

                    Toast.makeText(this, "Recherche en cours...", Toast.LENGTH_SHORT).show();
                    searchCity(city);
                } else {
                    Toast.makeText(this,
                            "Nom de ville invalide. Utilisez uniquement des lettres, espaces, traits d'union et apostrophes.",
                            Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Entrez un nom de ville!", Toast.LENGTH_SHORT).show();
            }
        });

        map.getOverlays().add(new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                Log.d("WEATHER_APP", "=== CARTE CLIQUE ===");
                String cityName = getCityFromCoordinates(p);

                if (cityName != null) {
                    editCity.setText(cityName);
                    addMarker(p, cityName);
                    fetchWeatherByCoordinates(p.getLatitude(), p.getLongitude());
                } else {
                    Toast.makeText(MainActivity.this, "Ville non trouvée à cet endroit!", Toast.LENGTH_SHORT).show();
                }

                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) { return false; }
        }));
    }

    private void checkNetworkConnectivity() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        Log.d("WEATHER_APP", "Connectivité réseau: " + isConnected);
        if (!isConnected) {
            Toast.makeText(this, "⚠️ Pas de connexion Internet!", Toast.LENGTH_LONG).show();
        }
    }

    private boolean isValidCityName(String city) {
        // Accepter lettres, espaces, traits d'union, apostrophes et accents
        return city.matches("^[a-zA-ZÀ-ÿ\\s\\-']+$") && city.length() >= 2;
    }

    private String getCityFromCoordinates(GeoPoint p) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addressList = geocoder.getFromLocation(p.getLatitude(), p.getLongitude(), 1);

            if (addressList != null && !addressList.isEmpty()) {
                Address address = addressList.get(0);
                String city = address.getLocality();
                if (city == null) {
                    city = address.getSubAdminArea(); // Essayer une autre source
                }
                if (city == null) {
                    city = address.getAdminArea(); // Dernier recours
                }

                Log.d("WEATHER_APP", "Ville trouvée via coordonnées: " + city);
                return city;
            }
        } catch (Exception e) {
            Log.e("WEATHER_APP", "Erreur Geocoder", e);
        }

        return null;
    }

    private void addMarker(GeoPoint point, String title) {
        if (cityMarker != null) {
            map.getOverlays().remove(cityMarker);
        }

        cityMarker = new Marker(map);
        cityMarker.setPosition(point);
        cityMarker.setTitle(title);
        cityMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        map.getOverlays().add(cityMarker);

        map.getController().animateTo(point);
        Log.d("WEATHER_APP", "Marqueur ajouté: " + title + " à " + point);

        // Forcer le rafraîchissement de la carte
        map.invalidate();
    }

    private String normalizeCityName(String cityName) {
        if (cityName == null || cityName.trim().isEmpty()) {
            return "";
        }

        // Supprimer les espaces multiples et les espaces en début/fin
        String normalized = cityName.trim().replaceAll("\\s+", " ");

        // Mettre la première lettre de chaque mot en majuscule
        String[] words = normalized.split(" ");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            if (!words[i].isEmpty()) {
                if (i > 0) result.append(" ");
                // Conserver la première lettre en majuscule, le reste en minuscule
                if (words[i].length() > 1) {
                    result.append(words[i].substring(0, 1).toUpperCase())
                            .append(words[i].substring(1).toLowerCase());
                } else {
                    result.append(words[i].toUpperCase());
                }
            }
        }

        Log.d("WEATHER_APP", "Normalisé: '" + cityName + "' -> '" + result.toString() + "'");
        return result.toString();
    }

    private void searchCity(String cityName) {
        Log.d("WEATHER_APP", "=== DÉBUT RECHERCHE VILLE ===");

        final String normalizedCity = normalizeCityName(cityName);
        Log.d("WEATHER_APP", "Ville normalisée: " + normalizedCity);

        // Essayer plusieurs variantes de recherche
        List<String> searchVariants = new ArrayList<>();
        searchVariants.add(normalizedCity);

        // Ajouter variante sans accents si nécessaire
        String withoutAccents = removeAccents(normalizedCity);
        if (!withoutAccents.equalsIgnoreCase(normalizedCity)) {
            searchVariants.add(withoutAccents);
        }

        // Ajouter variante tout en majuscules (pour certaines API)
        searchVariants.add(normalizedCity.toUpperCase());

        Log.d("WEATHER_APP", "Variantes à tester: " + searchVariants);

        // Essayer chaque variante
        for (int i = 0; i < searchVariants.size(); i++) {
            final String variant = searchVariants.get(i);
            final boolean isLastAttempt = (i == searchVariants.size() - 1);

            try {
                performCitySearch(variant, isLastAttempt);

                // Petite pause entre les requêtes
                if (!isLastAttempt) {
                    Thread.sleep(200);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                Log.e("WEATHER_APP", "Erreur lors de la recherche", e);
            }
        }
    }

    private String removeAccents(String text) {
        if (text == null) return "";

        return text.toLowerCase()
                .replaceAll("[éèêë]", "e")
                .replaceAll("[àâä]", "a")
                .replaceAll("[ôö]", "o")
                .replaceAll("[ùûü]", "u")
                .replaceAll("[ïî]", "i")
                .replaceAll("ç", "c")
                .replaceAll("ñ", "n");
    }

    private void performCitySearch(String cityName, boolean isLastAttempt) {
        try {
            // Encodage URL correct
            String encodedCity = URLEncoder.encode(cityName, "UTF-8")
                    .replaceAll("\\+", "%20"); // Remplacer + par %20 pour les espaces

            String url = "https://api.openweathermap.org/geo/1.0/direct?q="
                    + encodedCity + "&limit=5&appid=" + API_KEY;

            Log.d("WEATHER_APP", "Tentative avec variante: " + cityName);
            Log.d("WEATHER_APP", "URL: " + url);

            StringRequest request = new StringRequest(Request.Method.GET, url,
                    response -> {
                        Log.d("WEATHER_APP", "Réponse reçue pour '" + cityName + "': " + response);

                        try {
                            JSONArray results = new JSONArray(response);

                            if (results.length() > 0) {
                                // Prendre le résultat le plus pertinent (premier)
                                JSONObject location = results.getJSONObject(0);

                                double lat = location.getDouble("lat");
                                double lon = location.getDouble("lon");
                                String foundName = location.getString("name");
                                String country = location.optString("country", "");
                                String state = location.optString("state", "");

                                Log.d("WEATHER_APP", "✓ Ville trouvée: " + foundName +
                                        ", Pays: " + country + ", État: " + state);

                                runOnUiThread(() -> {
                                    GeoPoint point = new GeoPoint(lat, lon);

                                    String displayName = foundName;
                                    if (!state.isEmpty()) {
                                        displayName += ", " + state;
                                    }
                                    if (!country.isEmpty()) {
                                        displayName += " (" + country + ")";
                                    }

                                    addMarker(point, displayName);
                                    editCity.setText(foundName); // Mettre à jour avec le nom correct
                                    fetchWeatherByCoordinates(lat, lon);

                                    Toast.makeText(MainActivity.this,
                                            "Ville trouvée: " + foundName,
                                            Toast.LENGTH_SHORT).show();
                                });

                                return; // Arrêter après avoir trouvé
                            }

                            // Si aucun résultat et c'est la dernière tentative
                            if (isLastAttempt && results.length() == 0) {
                                runOnUiThread(() -> {
                                    Toast.makeText(MainActivity.this,
                                            "Ville '" + cityName + "' non trouvée. Essayez un nom différent.",
                                            Toast.LENGTH_LONG).show();
                                    textView.setText("Ville non trouvée: " + cityName);
                                });
                            }

                        } catch (Exception e) {
                            Log.e("WEATHER_APP", "Erreur parsing JSON pour '" + cityName + "'", e);

                            if (isLastAttempt) {
                                runOnUiThread(() -> {
                                    Toast.makeText(MainActivity.this,
                                            "Erreur de traitement des données",
                                            Toast.LENGTH_SHORT).show();
                                });
                            }
                        }
                    },
                    error -> {
                        Log.e("WEATHER_APP", "Erreur réseau pour '" + cityName + "': " + error.getMessage());

                        if (isLastAttempt) {
                            runOnUiThread(() -> {
                                String errorMsg = "Erreur de connexion";
                                if (error.networkResponse != null) {
                                    errorMsg += " (HTTP " + error.networkResponse.statusCode + ")";
                                }
                                Toast.makeText(MainActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                                textView.setText("Erreur réseau");
                            });
                        }
                    }
            );

            // Configurer les timeouts et retry policy
            request.setRetryPolicy(new DefaultRetryPolicy(
                    15000, // 15 secondes timeout
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
            ));

            // Ajouter à la file de requêtes
            requestQueue.add(request);

        } catch (UnsupportedEncodingException e) {
            Log.e("WEATHER_APP", "Erreur d'encodage pour '" + cityName + "'", e);

            if (isLastAttempt) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this,
                            "Erreur d'encodage du nom de ville",
                            Toast.LENGTH_SHORT).show();
                });
            }
        }
    }

    private void fetchWeatherByCoordinates(double lat, double lon) {
        Log.d("WEATHER_APP", "=== RÉCUPÉRATION MÉTÉO ===");
        Log.d("WEATHER_APP", "Coordonnées: lat=" + lat + ", lon=" + lon);

        String url = "https://api.openweathermap.org/data/2.5/weather?lat=" +
                lat + "&lon=" + lon + "&appid=" + API_KEY + "&units=metric&lang=fr";

        Log.d("WEATHER_APP", "URL météo: " + url);

        @SuppressLint("SetTextI18n") StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    Log.d("WEATHER_APP", "Réponse météo reçue");
                    try {
                        JSONObject json = new JSONObject(response);

                        final String city = json.getString("name");  // Déclaré final
                        final String temp = String.format("%.1f", json.getJSONObject("main").getDouble("temp"));
                        final String humidity = json.getJSONObject("main").getString("humidity");
                        final String wind = String.format("%.2f", json.getJSONObject("wind").getDouble("speed"));

                        // Récupérer la description météo
                        final String description;  // Déclaré final
                        JSONArray weatherArray = json.getJSONArray("weather");
                        if (weatherArray.length() > 0) {
                            String desc = weatherArray.getJSONObject(0).getString("description");
                            // Capitaliser la première lettre
                            desc = desc.substring(0, 1).toUpperCase() + desc.substring(1);
                            description = desc;  // Assignation
                        } else {
                            description = "";  // Assignation
                        }

                        // Créer le texte final en dehors du lambda
                        final String weatherText = "📍 Ville: " + city +
                                "\n🌡 Température: " + temp + "°C" +
                                "\n💧 Humidité: " + humidity + "%" +
                                "\n💨 Vent: " + wind + " m/s" +
                                (description.isEmpty() ? "" : "\n☁️ " + description);

                        final String toastMessage = "Météo actualisée pour " + city;

                        runOnUiThread(() -> {
                            editCity.setText(city);
                            textView.setText(weatherText);
                            Toast.makeText(MainActivity.this, toastMessage, Toast.LENGTH_SHORT).show();
                        });

                        Log.d("WEATHER_APP", "Météo affichée pour: " + city);

                    } catch (Exception e) {
                        Log.e("WEATHER_APP", "Erreur parsing météo", e);
                        runOnUiThread(() -> {
                            textView.setText("❌ Erreur lecture météo!");
                        });
                    }
                },
                error -> {
                    Log.e("WEATHER_APP", "Erreur requête météo", error);
                    runOnUiThread(() -> {
                        textView.setText("❌ Requête météo échouée!");
                        Toast.makeText(MainActivity.this,
                                "Impossible de récupérer la météo",
                                Toast.LENGTH_SHORT).show();
                    });
                }
        );
        // Configurer les timeouts
        request.setRetryPolicy(new DefaultRetryPolicy(
                10000, // 10 secondes timeout
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        requestQueue.add(request);
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        map.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (requestQueue != null) {
            requestQueue.cancelAll(this);
        }
    }
}
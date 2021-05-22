package com.junctionx.pathfinder;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.junctionx.pathfinder.model.Address;
import com.junctionx.pathfinder.model.Mobilities;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraAnimation;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.InfoWindow;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.Overlay;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.overlay.PathOverlay;
import com.naver.maps.map.util.FusedLocationSource;

import org.json.JSONException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public class GuideActivity extends AppCompatActivity implements OnMapReadyCallback {

    private BackPressedForFinish backPressedForFinish;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private FusedLocationSource locationSource;
    private NaverMap naverMap;

    private FusedLocationProviderClient fusedLocationClient;

    private LatLng currentPosition;

    private FloatingActionButton floatingMyLocation;
    private TextView textViewEndAddress;
    private LinearLayout layoutMobilities;

    private Double endLat;
    private Double endLng;
    private Double startLat;
    private Double startLng;

    String uu;

    InfoWindow infoWindow = new InfoWindow();

    private ArrayList<Integer> turnTypeList = new ArrayList<>();
    private ArrayList<ArrayList<Double>> directionList = new ArrayList<ArrayList<Double>>();

    private JsonPlaceHolderApi jsonPlaceHolderApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide);

        // 데이터 불러오기
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(getResources().getString(R.string.url))
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        jsonPlaceHolderApi = retrofit.create(JsonPlaceHolderApi.class);

        floatingMyLocation = findViewById(R.id.floatingMyLocation);
        //layoutMobilities = findViewById(R.id.layoutMobilities);

        endLat = getIntent().getDoubleExtra("endLat", 0);
        endLng = getIntent().getDoubleExtra("endLng", 0);
        startLat = getIntent().getDoubleExtra("startLat", 0);
        startLng = getIntent().getDoubleExtra("startLng", 0);

        locationSource =
                new FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE);

        FragmentManager fm = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment) fm.findFragmentById(R.id.map);
        if (mapFragment == null) {
            fm.beginTransaction().add(R.id.map, mapFragment).commit();
        }

        mapFragment.getMapAsync(this);

        floatingMyLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CameraUpdate cameraUpdate = CameraUpdate.scrollTo(currentPosition).animate(CameraAnimation.Easing);;
                naverMap.moveCamera(cameraUpdate);
            }
        });

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                currentPosition = new LatLng(location.getLatitude(),location.getLongitude());

                if (location != null) {
                    // Logic to handle location object
                }
            }
        });

        URL url = null;
        HttpURLConnection urlConnection = null;

        try {
            String appKey = "l7xxf3e592c06dc8432aa3f34091d016eaba";
            String sLat = String.valueOf(startLat);
            String sLng = String.valueOf(startLat);
            String eLat = String.valueOf(endLat);
            String eLng = String.valueOf(endLng);
            String reqCoordType = "WGS84GEO";
            String rescoordType = "EPSG3857";
            String startName = URLEncoder.encode("출발지", "UTF-8");
            String endName = URLEncoder.encode("도착지", "UTF-8");

            uu = "https://apis.openapi.sk.com/tmap/routes/pedestrian?version=1&format=json&callback=result&appKey="
                    + appKey + "&startX=" + startLng + "&startY=" + startLat + "&endX=" + endLng + "&endY=" + endLat
                    + "&startName=" + startName + "&endName=" + endName + "&searchOption=" + 30;
            url = new URL(uu);

        } catch (UnsupportedEncodingException | MalformedURLException e) {
            e.printStackTrace();
        }

        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Accept-Charset", "utf-8");
            urlConnection.setRequestProperty("Content-Type", "application/x-form-urlencoded");

            NetworkTask networkTask = new NetworkTask(uu, null);
            networkTask.execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public class NetworkTask extends AsyncTask<Void, Void, String> {
        private String url;
        private ContentValues values;

        public NetworkTask(String url, ContentValues values) {
            this.url = url;
            this.values = values;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Void... params) {
            String result;

            RequestHttpURLConnection requestHttpURLConnection = new RequestHttpURLConnection();
            result = requestHttpURLConnection.request(url, values);

            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            try {
                JSONParser parser = new JSONParser();
                Object obj =  parser.parse(s);
                JSONObject response = (JSONObject) obj;
                JSONArray features = (JSONArray) response.get("features");

                for (int i = 0; i < features.size(); i ++) {
                    JSONObject feature = (JSONObject) features.get(i);
                    JSONObject geometry = (JSONObject) feature.get("geometry");
                    JSONObject properties = (JSONObject) feature.get("properties");

                    String feature_type = (String) feature.get("type");
                    String geometry_type = (String) geometry.get("type");

                    JSONArray coordinates = (JSONArray) geometry.get("coordinates");

                    if (feature_type.equals("Feature")) {
                        if (geometry_type.equals("Point")) {

                            String description = (String) properties.get("description");
                            int turnType = ((Long) properties.get("turnType")).intValue();
                            if (turnType == 211 || turnType == 212
                                    || turnType == 213 || turnType == 214 || turnType == 215 || turnType == 216 || turnType == 217 || turnType == 218) {
                                turnTypeList.add(turnType);
                                Marker marker = new Marker();
                                marker.setPosition(new LatLng((Double) coordinates.get(1), (Double) coordinates.get(0)));
                                marker.setIcon(OverlayImage.fromResource(R.drawable.ic_bodo));
                                marker.setMap(naverMap);

                                marker.setTag(description);
                                marker.setOnClickListener(overlay -> {
                                    infoWindow.open(marker);
                                    return true;
                                });

                                Overlay.OnClickListener listener = overlay -> {
                                    Marker overlayMarker = (Marker) overlay;

                                    if (overlayMarker.getInfoWindow() == null) {
                                        infoWindow.open(overlayMarker);
                                    } else {
                                        infoWindow.close();
                                    }

                                    return true;
                                };

                                marker.setOnClickListener(listener);

                                ArrayList<Double> data = new ArrayList<>();
                                data.add((Double) coordinates.get(1));
                                data.add((Double) coordinates.get(0));
                                directionList.add(data);
                            }


                        } else if (geometry_type.equals("LineString")) {
                            PathOverlay path = new PathOverlay();
                            List<LatLng> ArrayPositionList = new ArrayList<>();
                            for (int k = 0; k < coordinates.size(); k++) {
                                JSONArray coordinate = (JSONArray) coordinates.get(k);
                                ArrayPositionList.add(new LatLng((Double) coordinate.get(1), (Double) coordinate.get(0)));

                                if (k == 0  || k == coordinate.size()) {
                                    ArrayList<Double> data = new ArrayList<>();
                                    data.add((Double) coordinate.get(1));
                                    data.add((Double) coordinate.get(0));
                                    directionList.add(data);
                                }

                            }

                            path.setCoords(ArrayPositionList);
                            path.setColor(Color.rgb(107, 102, 255));
                            path.setMap(naverMap);
                        }
                    }
                }
                getObstacles();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (locationSource.onRequestPermissionsResult(
                requestCode, permissions, grantResults)) {
            return;
        }
        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);
    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.naverMap = naverMap;
        naverMap.setLocationSource(locationSource);
        naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);

        CameraUpdate cameraUpdate = CameraUpdate.zoomTo(17);
        naverMap.moveCamera(cameraUpdate);

        naverMap.setOnMapClickListener((coord, point) -> {
            infoWindow.close();
        });

        infoWindow.setAdapter(new InfoWindow.DefaultTextAdapter(getApplicationContext()) {
            @NonNull
            @Override
            public CharSequence getText(@NonNull InfoWindow infoWindow) {
                // 정보 창이 열린 마커의 tag를 텍스트로 노출하도록 반환
                return (CharSequence)infoWindow.getMarker().getTag();
            }
        });

        naverMap.addOnLocationChangeListener(location ->
                getObstaclesPosition(location.getLatitude(), location.getLongitude()));
                //Toast.makeText(this,location.getLatitude() + ", " + location.getLongitude(), Toast.LENGTH_SHORT).show());

    }

    private void getObstacles() {
        Call<Mobilities> call = jsonPlaceHolderApi.getObstacles(directionList);
        call.enqueue(new Callback<Mobilities>() {
            @Override
            public void onResponse(Call<Mobilities> call, Response<Mobilities> response) {
                if (!response.isSuccessful()) {

                    return;
                }

                String json = new Gson().toJson(response.body());
                try {
                    JSONParser parser = new JSONParser();
                    Object obj =  parser.parse(json);
                    JSONObject res = (JSONObject) obj;
                    JSONObject responseObj = (JSONObject) res.get("response");
                    JSONArray payload = (JSONArray) responseObj.get("payload");

                    for (int i = 0; i < payload.size(); i++) {
                        JSONObject item = (JSONObject) payload.get(i);
                        JSONObject impulse = (JSONObject) item.get("impulse");

                        int id = ((Double) item.get("id")).intValue();
                        int level = ((Double) impulse.get("level")).intValue();
                        String latStr = (String) item.get("lat");
                        String lngStr = (String) item.get("lng");

                        Double lat = Double.parseDouble(latStr);
                        Double lng = Double.parseDouble(lngStr);

                        Marker marker = new Marker();
                        marker.setPosition(new LatLng(lat, lng));

                        if (level == 1) {
                            marker.setIcon(OverlayImage.fromResource(R.drawable.ic_one));
                        } else if (level == 2) {
                            marker.setIcon(OverlayImage.fromResource(R.drawable.ic_two));
                        } else if (level == 3) {
                            marker.setIcon(OverlayImage.fromResource(R.drawable.ic_three));
                        }

                        marker.setWidth(64);
                        marker.setHeight(64);

                        marker.setMap(naverMap);
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<Mobilities> call, Throwable t) {
                Log.d("Json Parse", t.getMessage());
            }
        });
    }

    private void getObstaclesPosition(double lat, double lng) {
        Call<Mobilities> call = jsonPlaceHolderApi.getObstaclesPosition(lat, lng, "kick_board");
        call.enqueue(new Callback<Mobilities>() {
            @Override
            public void onResponse(Call<Mobilities> call, Response<Mobilities> response) {
                if (!response.isSuccessful()) {

                    return;
                }

                String json = new Gson().toJson(response.body());
                try {
                    JSONParser parser = new JSONParser();
                    Object obj =  parser.parse(json);
                    JSONObject res = (JSONObject) obj;
                    JSONObject responseObj = (JSONObject) res.get("response");
                    JSONArray payload = (JSONArray) responseObj.get("payload");

                    if (payload.size() > 0) {
                        Log.d("SEKFJLSEf", "FSEKFJL" + payload);
                        Vibrator vib = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                        vib.vibrate(1000);

                        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                        Ringtone ringtone = RingtoneManager.getRingtone(getApplicationContext(), notification);
                        ringtone.play();

                        showPushToast("주변에 위험요소가 있을수도 있습니다.");
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<Mobilities> call, Throwable t) {
                Log.d("Json Parse", t.getMessage());
                Log.d("SEFKDV", t.getMessage());
            }
        });
    }

    public void showPushToast(String message) {
        TextView push_text;
        LayoutInflater inflater = getLayoutInflater();

        View layout = inflater.inflate(R.layout.toast_layout, (ViewGroup) findViewById(R.id.toast_root));
        push_text = layout.findViewById(R.id.push_text);

        push_text.setText(message);
        //layout.setBackgroundColor(color);

        Toast toast = new Toast(getApplicationContext());
        toast.setGravity(Gravity.TOP, 20, 20);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(layout);

        toast.show();
    }
}
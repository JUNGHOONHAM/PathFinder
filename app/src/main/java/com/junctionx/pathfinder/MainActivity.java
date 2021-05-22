package com.junctionx.pathfinder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.junctionx.pathfinder.adapter.SearchAdapter;
import com.junctionx.pathfinder.model.Address;
import com.junctionx.pathfinder.model.Mobilities;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraAnimation;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.util.FusedLocationSource;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private BackPressedForFinish backPressedForFinish;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    private FusedLocationSource locationSource;
    private NaverMap naverMap;

    private FusedLocationProviderClient fusedLocationClient;

    private LatLng currentPosition;

    private FloatingActionButton floatingMyLocation;
    private EditText editTextAddressSearch;
    private RecyclerView recyclerView;
    private SearchAdapter adapter;

    private ArrayList<Address> addressList = new ArrayList<>();

    private final Geocoder geocoder = new Geocoder(this);

    private ArrayList<Marker> markerList = new ArrayList<>();

    private JsonPlaceHolderApi jsonPlaceHolderApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        floatingMyLocation = findViewById(R.id.floatingMyLocation);
        editTextAddressSearch = findViewById(R.id.editTextAddressSearch);
        recyclerView = findViewById(R.id.recyclerView);

        // 데이터 불러오기
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(getResources().getString(R.string.url))
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        jsonPlaceHolderApi = retrofit.create(JsonPlaceHolderApi.class);

        adapter = new SearchAdapter(getApplicationContext(), addressList, new SearchAdapter.OnClickListener() {
            @Override
            public void onItemClick(String address) {
                List<android.location.Address> addresses = null;

                try {
                    addresses = geocoder.getFromLocationName(address, 1);
                    if (addresses != null && !addresses.equals(" ")) {
                        LatLng latLng = search(addresses);

                        Intent intent = new Intent(getApplicationContext(), DirectionSettingActivity.class);
                        intent.putExtra("endAddress", address);
                        intent.putExtra("startLat", currentPosition.latitude);
                        intent.putExtra("startLng", currentPosition.longitude);
                        intent.putExtra("endLat", latLng.latitude);
                        intent.putExtra("endLng", latLng.longitude);
                        startActivity(intent);
                    }
                } catch(Exception e) {

                }
            }
        });

        // 리스트뷰 참조 및 Adapter 달기
        LinearLayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

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

        final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        editTextAddressSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_ACTION_SEARCH) {
                    // 검색창에서 텍스트를 가져온다
                    String searchText = editTextAddressSearch.getText().toString();
                    // editTextAddressSearch.setText("");
                    editTextAddressSearch.clearFocus();

                    imm.hideSoftInputFromWindow(editTextAddressSearch.getWindowToken(), 0);

                    new Thread(){
                        @Override
                        public void run() {
                            searchPlace(searchText);
                        }
                    }.start();

                }

                return false;
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

        naverMap.addOnCameraIdleListener(() -> {
            getMobilities();
        });
    }

    public void searchPlace(String keyword){
        try {
            keyword = URLEncoder.encode(keyword, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("encoding fail!",e);
        }

        String apiURL = "https://openapi.naver.com/v1/search/local.json?query="+keyword+"&display=20&start=1&sort=random";    // json 결과
        //String apiURL = "https://openapi.naver.com/v1/search/blog.xml?query="+ text; // xml 결과

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("X-Naver-Client-Id", "54IdrDtJuFoAJ6K5j7qx");
        requestHeaders.put("X-Naver-Client-Secret", "F90T72Arbf");
        String responseBody = get(apiURL,requestHeaders);

        System.out.println("네이버에서 받은 결과 = " + responseBody);
        System.out.println("-----------------------------------------");

    }

    private String get(String apiUrl, Map<String, String> requestHeaders){
        HttpURLConnection con = connect(apiUrl);

        try {
            con.setRequestMethod("GET");
            for(Map.Entry<String, String> header :requestHeaders.entrySet()) {
                con.setRequestProperty(header.getKey(), header.getValue());
            }

            int responseCode = con.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) { // 정상 호출
                return readBody(con.getInputStream());
            } else { // 에러 발생
                return readBody(con.getErrorStream());
            }
        } catch (IOException e) {
            throw new RuntimeException("API 요청과 응답 실패", e);
        } finally {
            con.disconnect();
        }
    }

    private HttpURLConnection connect(String apiUrl){
        try {
            URL url = new URL(apiUrl);
            return (HttpURLConnection)url.openConnection();
        } catch (MalformedURLException e) {
            throw new RuntimeException("API URL이 잘못되었습니다. : " + apiUrl, e);
        } catch (IOException e) {
            throw new RuntimeException("연결이 실패했습니다. : " + apiUrl, e);
        }
    }

    private String readBody(InputStream body){
        InputStreamReader streamReader = new InputStreamReader(body, StandardCharsets.UTF_8);
        try {
            JSONParser parser = new JSONParser();
            Object obj =  parser.parse(streamReader);
            JSONObject response = (JSONObject) obj;
            JSONArray items = (JSONArray) response.get("items");
            addressList.clear();
            for (int i = 0; i < items.size(); i ++) {
                JSONObject item = (JSONObject) items.get(i);

                Address data = new Address();
                data.setTitle(removeTag((String) item.get("title")));
                data.setAddress((String) item.get("address"));
                addressList.add(data);
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adapter.notifyDataSetChanged();
                }
            });

        } catch(Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // 구글맵 주소 검색 메서드
    protected LatLng search(List<android.location.Address> addresses) {
        android.location.Address address = addresses.get(0);
        LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());

        return latLng;
    }

    private void getMobilities() {

        for (int i = 0; i < markerList.size(); i++) {
            markerList.get(i).setMap(null);
        }

        LatLng northeast = naverMap.getContentBounds().getNorthEast();
        LatLng southwest = naverMap.getContentBounds().getSouthWest();

        Call<Mobilities> call = jsonPlaceHolderApi.getMobilities(northeast.latitude, northeast.longitude, southwest.latitude, southwest.longitude);
        call.enqueue(new Callback<Mobilities>() {
            @Override
            public void onResponse(Call<Mobilities> call, Response<Mobilities> response) {
                if (!response.isSuccessful()) {

                    return;
                }

                String json = new Gson().toJson(response.body());
                Log.d("SELKFJ", json + "");
                try {
                    JSONParser parser = new JSONParser();
                    Object obj =  parser.parse(json);
                    Log.d("SELKFJ", obj + "");
                    JSONObject res = (JSONObject) obj;
                    Log.d("SELKFJ", res + "");
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
                        markerList.add(marker);
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

    public String removeTag(String html) throws Exception {
        return html.replaceAll("<(/)?([a-zA-Z]*)(\\s[a-zA-Z]*=[^>]*)?(\\s)*(/)?>", "");
    }
}
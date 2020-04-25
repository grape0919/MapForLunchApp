package info.hkdevstudio.gom.map;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.*;
import com.google.android.gms.maps.*;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;
import info.hkdevstudio.gom.R;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import static android.content.Context.LOCATION_SERVICE;

public class GomMapView extends MapView implements OnMapReadyCallback {

    private static final String TAG = "PETFINDER.MapView";
    private static final LatLng SEOUL = new LatLng(37.56, 126.97);
    public GoogleMap mGoogleMap = null;
    private Marker masterMarker = null;
    private Marker petMarker = null;

    private Activity activity = null;

    //onRequestPermissionResult에서 수신된 결과에서 ActivityCompat.requestPermissions
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private static final int UPDATE_INTERVAL_MS = 1000;  // 1초
    private static final int FASTEST_UPDATE_INTERVAL_MS = 500; // 0.5초
    // 앱에 필요한 퍼미션 정의
    String[] REQUIRED_PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

    //Snackbar 사용하기 위한 View
    private View mLayout;

    public FusedLocationProviderClient mFusedLocationClient;
    public LocationRequest locationRequest;
    private Location location;
    Location mCurrentLocation;
    LatLng currentPosition;

    public GomMapView(Context context, Activity activity) {
        super(context);
        this.activity = activity;
        mLayout = findViewById(R.id.layout_main);
        locationRequest = new LocationRequest()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL_MS)
                .setFastestInterval(FASTEST_UPDATE_INTERVAL_MS);

        LocationSettingsRequest.Builder builder =
                new LocationSettingsRequest.Builder();

        builder.addLocationRequest(locationRequest);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
    }

//    public MapView(Activity ac) {
//        super(ac);
//    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        Log.d(TAG, "onMapReady :");

        mGoogleMap = googleMap;

        setDefaultLocation(mGoogleMap);

        //위치 퍼미션 체크
        if(checkPermission()){
            startLocationUpdates();    //퍼미션 가지고 있다면 현재위치 업데이트
        }else{//퍼미션이 없을 경우 퍼미션 요청
            if(ActivityCompat.shouldShowRequestPermissionRationale(activity, REQUIRED_PERMISSIONS[0])){
                //퍼미션 거부한적이 있는 경우
                Snackbar.make(mLayout, "이 앱을 실행하려면 위치 접근 권한이 필요합니다.",
                        Snackbar.LENGTH_INDEFINITE).setAction("확인", new View.OnClickListener(){

                    @Override
                    public void onClick(View v) {

                        ActivityCompat.requestPermissions(activity, REQUIRED_PERMISSIONS,
                                PERMISSION_REQUEST_CODE);
                    }
                }).show();
            }else{
                //퍼미션 거부한적이 없는 경우
                //요청 결과는 onRequestPermissionResult 에서 수신됨
                ActivityCompat.requestPermissions(activity, REQUIRED_PERMISSIONS,
                        PERMISSION_REQUEST_CODE);
            }

        }

        mGoogleMap.getUiSettings().setMyLocationButtonEnabled(true);
        mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(15));
        mGoogleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                Log.d(TAG, "onMapClick :");
            }
        });
    }

    public LocationCallback locationCallback = new LocationCallback(){
        @Override
        public void onLocationResult(LocationResult locationResult){
            super.onLocationResult(locationResult);
            //각 위치를 저정하는 리스트
            List<Location> locationList = locationResult.getLocations();

            if(locationList.size() > 0){
                //가장 최근 Location?
                location = locationList.get(locationList.size() - 1);

                currentPosition = new LatLng(location.getLatitude(), location.getLongitude());

                String markerTitle = getCurrentAddress(currentPosition);
                String markerSnippet = "위도:" + String.valueOf(location.getLatitude())
                        + " 경도:" + String.valueOf(location.getLongitude());

                Log.d(TAG, "onLocationResult : " + markerSnippet);

                setCurrentLocation(location, markerTitle, markerSnippet);

                mCurrentLocation = location;

            }
        }
    };

    public void setDefaultLocation(GoogleMap googleMap){
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(SEOUL, 15));

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(SEOUL);
        markerOptions.title("서울");
        markerOptions.snippet("한국의 수도");
        googleMap.addMarker(markerOptions);
    }

    private void startLocationUpdates(){
        //퍼미션 체크 후
        if(!checkLocationServicesStatus()){
            Log.d(TAG, "startLocationUpdates : call showDialogForLocationServiceSetting");
            showDialogForLocationServiceSetting();
        }else{
            //위치 퍼미션 체크
            if(!checkPermission()){
                Log.d(TAG, "startLocationUpdates() : 퍼미션이 없음");
                return;
            }

            Log.d(TAG, "startLocationUpdates : call mFusedLocationClient.requestLocationUpdates");

            mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());

            if(checkPermission()){
                mGoogleMap.setMyLocationEnabled(true);
            }
        }
    }




    public boolean checkLocationServicesStatus(){
        LocationManager locationManager = (LocationManager) activity.getSystemService(LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }


    public boolean checkPermission(){
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(this.getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION);
        int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(this.getContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION);

        if(hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED){
            Log.d(TAG, "checkPermission() : 퍼미션이 있음");
            return true;
        }

        return false;
    }

    //GPS 설정을 위해 화면 이동
    private void showDialogForLocationServiceSetting(){
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("위치 서비스 비활성화");
        builder.setMessage("앱을 사용하기 위해서는 위치 서비스가 필요합니다.\n 위치 설정을 수정하실래요?");
        builder.setCancelable(true);
        builder.setPositiveButton("설정", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent callGPSSettingIntent = new Intent(Settings.ACTION_LOCALE_SETTINGS);
                activity.startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);
            }
        });

        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.create().show();
    }

    public String getCurrentAddress(LatLng latLng){

        Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());

        List<Address> addresses;

        try{
            addresses = geocoder.getFromLocation(
                    latLng.latitude,
                    latLng.longitude,
                    1);
        }catch(IOException ioException){
            Toast.makeText(getContext(), "자오코더 서비스 사용불가", Toast.LENGTH_LONG).show();
            return "지오코더 서비스 불가";
        }catch(IllegalArgumentException e){
            Toast.makeText(getContext(), "잘못된 GPS 좌표", Toast.LENGTH_LONG).show();
            return "잘못된 GPS 좌표";
        }

        if(addresses == null || addresses.size() == 0) {
            Toast.makeText(getContext(), "주소 없음", Toast.LENGTH_LONG).show();
            return "주소 없음";
        }else {
            Address address = addresses.get(0);
            return address.getAddressLine(0).toString();
        }
    }

    public void setCurrentLocation(Location location, String markerTitle, String markerSnippet){
        if(masterMarker != null ) masterMarker.remove();

        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(currentLatLng);
        markerOptions.title(markerTitle);
        markerOptions.snippet(markerSnippet);
        markerOptions.draggable(true);

        masterMarker = mGoogleMap.addMarker(markerOptions);

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(currentLatLng);
        mGoogleMap.moveCamera(cameraUpdate);


    }


}
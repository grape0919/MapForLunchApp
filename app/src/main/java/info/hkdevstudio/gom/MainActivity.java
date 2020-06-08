package info.hkdevstudio.gom;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.snackbar.Snackbar;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import info.hkdevstudio.gom.conf.Configuration;
import info.hkdevstudio.gom.db.DBManager;
import info.hkdevstudio.gom.db.UserContDB;
import info.hkdevstudio.gom.gps.GpsTracker;
import info.hkdevstudio.gom.handler.RequestParam;
import info.hkdevstudio.gom.handler.RestApiHandler;
import info.hkdevstudio.gom.util.GomStaticUtil;
import info.hkdevstudio.gom.view.dialog.InputDialog;
import info.hkdevstudio.gom.view.map.CustomBalloonAdapter;
import info.hkdevstudio.gom.vo.DocumentVo;
import info.hkdevstudio.gom.vo.MetaVo;
import net.daum.mf.map.api.MapPOIItem;
import net.daum.mf.map.api.MapPoint;
import net.daum.mf.map.api.MapView;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class MainActivity extends Activity implements MapView.POIItemEventListener{
    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    private final long FINISH_INTERVAL_TIME = 2000;
    private long backPressedTime = 0;

    String[] REQUIRED_PERMISSIONS  = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
    GpsTracker gpsTracker;

    private AdView mAdView;
    private View mLayout;

    Configuration conf;
    DBManager db;

    MapView mapView;
    Button randomChoiceButton;
    ImageButton radius;
    ImageButton myLocation;

    ImageButton searchButton;
    EditText searchKeyword;

    WebView webView;
    ProgressBar webViewProgress;
    ImageButton webView_cancel;
    WebSettings mWebSettings;
    List<MapPOIItem> placeList = new ArrayList<>();

    SlidingUpPanelLayout slidingLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //main 화면 시작
        setContentView(R.layout.activity_main);
        //광고 코드
        MobileAds.initialize(this);
        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
        //버튼 및 레이아웃 세팅
        mLayout = findViewById(R.id.layout_main);
        randomChoiceButton = findViewById(R.id.choose_button);
        myLocation = findViewById(R.id.renew_my_location);
        radius = findViewById(R.id.radius);

        //detailTitle = findViewById(R.id.detail_title);
        //
        // detailTitle.setText("TEST");

        slidingLayout = findViewById(R.id.layout_main);
        slidingLayout.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {
                if(slideOffset == 1.0f){
                    slidingLayout.setTouchEnabled(false);
                }else{
                    slidingLayout.setTouchEnabled(true);
                }
            }

            @Override
            public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {

            }
        });
        webViewProgress = findViewById(R.id.web_view_progressBar);
        webView = findViewById(R.id.detail_web_view);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if(webView.getUrl().equals(url)){
                    return true;
                }
                try {
                    if (url.startsWith("tel:")) {
                        Intent tel = new Intent(Intent.ACTION_DIAL, Uri.parse(url));
                        startActivity(tel);
                        return true;
                    } else if(url.startsWith("intent:")){
                        Intent intent = null;

                        intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);

                        Intent existPackage = getPackageManager().getLaunchIntentForPackage(intent.getPackage());
                        if (existPackage != null) {
                            startActivity(intent);
                        }
//                        else {
//                            Intent marketIntent = new Intent(Intent.ACTION_VIEW);
//                            marketIntent.setData(Uri.parse("market://details?id="+intent.getPackage()));
//                            startActivity(marketIntent);
//                        }
                        return true;
                    }else{
                        return false;
//                        return super.shouldOverrideUrlLoading(view, request);
                    }
                    /*else{
                        return super.shouldOverrideUrlLoading(view, request);
                    }*/
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                    return super.shouldOverrideUrlLoading(view, request);
                }
            }

// 페이지 로딩 시작시 호출
            @Override
            public void onPageStarted(WebView view,String url , Bitmap favicon){
                webViewProgress.setVisibility(View.VISIBLE);
            }
//페이지 로딩 종료시 호출
            public void onPageFinished(WebView view,String Url){
                webViewProgress.setVisibility(View.GONE);
            }

        });// 클릭시 새창 안뜨게
        mWebSettings = webView.getSettings(); //세부 세팅 등록
        mWebSettings.setJavaScriptEnabled(true); // 웹페이지 자바스클비트 허용 여부
        mWebSettings.setSupportMultipleWindows(false); // 새창 띄우기 허용 여부
        mWebSettings.setJavaScriptCanOpenWindowsAutomatically(false); // 자바스크립트 새창 띄우기(멀티뷰) 허용 여부
        mWebSettings.setLoadWithOverviewMode(true); // 메타태그 허용 여부
        mWebSettings.setUseWideViewPort(true); // 화면 사이즈 맞추기 허용 여부
        mWebSettings.setSupportZoom(false); // 화면 줌 허용 여부
        mWebSettings.setBuiltInZoomControls(false); // 화면 확대 축소 허용 여부
        mWebSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN); // 컨텐츠 사이즈 맞추기
        mWebSettings.setCacheMode(WebSettings.LOAD_NO_CACHE); // 브라우저 캐시 허용 여부
        mWebSettings.setDomStorageEnabled(true); // 로컬저장소 허용 여부

        webView_cancel = findViewById(R.id.web_view_close);
        webView_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(slidingLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED){
                    slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                }
            }
        });

        searchButton = findViewById(R.id.search_button);
        searchKeyword = findViewById(R.id.search_keyword);
        searchKeyword.setText("맛집");

        gpsTracker = new GpsTracker(this);


        if (!checkLocationServicesStatus()) {
            showDialogForLocationServiceSetting();
        }else {
            checkPermission();
        }

        //설정값 세팅
        db = new DBManager(this);
        db.onCreate(db.getWritableDatabase());

        conf = new Configuration();
        String distance = db.selectUserConf(UserContDB.DISTANCE_KEY);
        conf.setDisatance(Integer.parseInt(distance));

        //지도 생성
        mapView = new MapView(this);
        ViewGroup mapViewContainer = findViewById(R.id.map_view);
        mapViewContainer.addView(mapView);

        //커스텀 말풍선
        mapView.setCalloutBalloonAdapter(new CustomBalloonAdapter(this));
        mapView.setPOIItemEventListener(this);

        myLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                renewLocation();
            }
        });


        radius.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputDialog dialog = new InputDialog(MainActivity.this, db, conf);
                dialog.show();
            }
        });

        searchKeyword.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                if(actionId == EditorInfo.IME_ACTION_SEARCH) { // 뷰의 id를 식별, 키보드의 완료 키 입력 검출
                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    renewLocation();
                    return true;
                }
                return false;
            }
        });

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                renewLocation();
            }
        });

        randomChoiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(placeList!=null && placeList.size() > 0){
                    long seed = System.currentTimeMillis();
                    Random random = new Random(seed);
                    final int index = random.nextInt(placeList.size()-1)+1;
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if(Arrays.asList(mapView.getPOIItems()).equals(placeList)){
                                mapView.selectPOIItem(placeList.get(index), true);
                                onPOIItemSelected(mapView, placeList.get(index));
                            }else{
                                Toast.makeText(MainActivity.this, "아직 맛집을 모두 불러오지 못했어요!", Toast.LENGTH_LONG).show();
                            }
                        }
                    }, 300);
                }
            }
        });

        renewLocation();

    }

    public void selectMarker(String url){
        if(webView != null && url != null && !url.equals("")) {
            url = url.replaceAll("http", "https");
            //System.out.println("!@#!@# selectMarker : " + url);
            webView.clearHistory();
            webView.loadUrl(url);

            if(slidingLayout.getPanelState() != SlidingUpPanelLayout.PanelState.EXPANDED) {
                slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
            }
        }
    }

    public boolean hasPermission(){
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION);



        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED   ) {

            // 2. 이미 퍼미션을 가지고 있다면
            // ( 안드로이드 6.0 이하 버전은 런타임 퍼미션이 필요없기 때문에 이미 허용된 걸로 인식합니다.)

            Log.d("[DEBUG]", "퍼미션 갖고 있음");
            return true;
        }else{
            return false;
        }
    }

    public void checkPermission(){
        //런타임 퍼미션 처리
        // 1. 위치 퍼미션을 가지고 있는지 체크합니다.
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION);

        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED   ) {

            // 2. 이미 퍼미션을 가지고 있다면
            // ( 안드로이드 6.0 이하 버전은 런타임 퍼미션이 필요없기 때문에 이미 허용된 걸로 인식합니다.)

            Log.d("[DEBUG]", "퍼미션 갖고 있음");
        }else {  //2. 퍼미션 요청을 허용한 적이 없다면 퍼미션 요청이 필요합니다. 2가지 경우(3-1, 4-1)가 있습니다.

            // 3-1. 사용자가 퍼미션 거부를 한 적이 있는 경우에는
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])) {
                Log.d("[DEBUG]", "퍼미션 갖고 거부");
                // 3-2. 요청을 진행하기 전에 사용자가에게 퍼미션이 필요한 이유를 설명해줄 필요가 있습니다.
                Snackbar.make(mLayout, "이 앱을 실행하려면 위치 접근 권한이 필요합니다.",
                        Snackbar.LENGTH_INDEFINITE).setAction("확인", new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {

                        // 3-3. 사용자게에 퍼미션 요청을 합니다. 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                        ActivityCompat.requestPermissions( MainActivity.this, REQUIRED_PERMISSIONS,
                                PERMISSIONS_REQUEST_CODE);
                    }
                }).show();


            } else {
                // 4-1. 사용자가 퍼미션 거부를 한 적이 없는 경우에는 퍼미션 요청을 바로 합니다.
                // 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                Log.d("[DEBUG]", "퍼미션 요청");
                ActivityCompat.requestPermissions( this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);
            }
        }
    }

    public void setDefaultLocation() {

        mapView.removeAllPOIItems();

        //디폴트 위치, Seoul
        LatLng DEFAULT_LOCATION = new LatLng(37.56, 126.97);
        String markerTitle = "위치정보 가져올 수 없음";
        String markerSnippet = "위치 퍼미션과 GPS 활성 확인하세요";

        // 현재 위치 마커
        MapPOIItem marker = new MapPOIItem();
        marker.setItemName("위치 정보를 가져올 수 없습니다.");
        marker.setTag(0);

        marker.setMapPoint(MapPoint.mapPointWithGeoCoord(DEFAULT_LOCATION.latitude, DEFAULT_LOCATION.longitude));
        marker.setMarkerType(MapPOIItem.MarkerType.CustomImage);
        Bitmap markerImg = GomStaticUtil.getBitmapFromVectorDrawable(this, R.drawable.marker_red);
        marker.setCustomImageBitmap(markerImg);
        marker.setCustomImageAutoscale(false);
        marker.setCustomImageAnchor(0.5f, 1.0f);

        mapView.addPOIItem(marker);
    }




    /*
     * ActivityCompat.requestPermissions를 사용한 퍼미션 요청의 결과를 리턴받는 메소드입니다.
     */
    @Override
    public void onRequestPermissionsResult(int permsRequestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grandResults) {

        if ( permsRequestCode == PERMISSIONS_REQUEST_CODE && grandResults.length == REQUIRED_PERMISSIONS.length) {

            // 요청 코드가 PERMISSIONS_REQUEST_CODE 이고, 요청한 퍼미션 개수만큼 수신되었다면

            boolean check_result = true;


            // 모든 퍼미션을 허용했는지 체크합니다.

            for (int result : grandResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    check_result = false;
                    break;
                }
            }


            if ( check_result ) {

                // 퍼미션을 허용했다면 위치 업데이트를 시작합니다.
                renewLocation();
            }
            else {
                // 거부한 퍼미션이 있다면 앱을 사용할 수 없는 이유를 설명해주고 앱을 종료합니다.2 가지 경우가 있습니다.

                if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])
                        || ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[1])) {


                    // 사용자가 거부만 선택한 경우에는 앱을 다시 실행하여 허용을 선택하면 앱을 사용할 수 있습니다.
                    Snackbar.make(mLayout, "퍼미션이 거부되었습니다. 앱을 다시 실행하여 퍼미션을 허용해주세요. ",
                            Snackbar.LENGTH_INDEFINITE).setAction("확인", new View.OnClickListener() {

                        @Override
                        public void onClick(View view) {

                            finish();
                        }
                    }).show();

                }else {


                    // "다시 묻지 않음"을 사용자가 체크하고 거부를 선택한 경우에는 설정(앱 정보)에서 퍼미션을 허용해야 앱을 사용할 수 있습니다.
                    Snackbar.make(mLayout, "퍼미션이 거부되었습니다. 설정(앱 정보)에서 퍼미션을 허용해야 합니다. ",
                            Snackbar.LENGTH_INDEFINITE).setAction("확인", new View.OnClickListener() {

                        @Override
                        public void onClick(View view) {

                            finish();
                        }
                    }).show();
                }
            }

        }
    }


    //여기부터는 GPS 활성화를 위한 메소드들
    private void showDialogForLocationServiceSetting() {

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("위치 서비스 비활성화");
        builder.setMessage("앱을 사용하기 위해서는 위치 서비스가 필요합니다.\n"
                + "위치 설정을 수정하실래요?");
        builder.setCancelable(true);
        builder.setPositiveButton("설정", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Intent callGPSSettingIntent
                        = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.create().show();
    }
    public boolean checkLocationServicesStatus() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {

            case GPS_ENABLE_REQUEST_CODE:

                //사용자가 GPS 활성 시켰는지 검사
                if (checkLocationServicesStatus()) {
                    if (checkLocationServicesStatus()) {

                        Log.d("[DEBUG]", "onActivityResult : GPS 활성화 되있음");


                        //needRequest = true;

                        return;
                    }
                }

                break;
        }
    }

    public void renewLocation(){

        if(hasPermission()) {
            gpsTracker.getLocation();
            final double latitude = gpsTracker.getLatitude();
            final double longitude = gpsTracker.getLongitude();

            //검색 REST API
            // 현재 위치 기준 반경 500m 근처 맛집 찾아서 마커로 표시
            AsyncTask.execute(new Runnable() {
                @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                @Override
                public synchronized void run() {
                    boolean is_end = false;
                    int page = 1;
                    placeList.clear();

                    Log.d("DEBUG", "latitude : " + latitude);
                    Log.d("DEBUG", "longitude : " + longitude);
                    // 중심점 변경 + 줌 레벨 변경

                    mapView.setMapCenterPointAndZoomLevel(MapPoint.mapPointWithGeoCoord(latitude, longitude), 1, true);

                    // 이전에 그려진 마커들 삭제
                    mapView.removeAllPOIItems();

                    // 현재 위치 마커
                    MapPOIItem marker = new MapPOIItem();
                    marker.setItemName("현재 위치");
                    marker.setTag(0);

                    marker.setMapPoint(MapPoint.mapPointWithGeoCoord(latitude, longitude));
                    marker.setMarkerType(MapPOIItem.MarkerType.CustomImage);
                    Bitmap markerImg = GomStaticUtil.getBitmapFromVectorDrawable(MainActivity.this, R.drawable.me);
                    marker.setCustomImageBitmap(markerImg);
                    marker.setCustomImageAutoscale(false);
                    marker.setCustomImageAnchor(0.5f, 1.0f);

                    mapView.addPOIItem(marker);
                    placeList.add(marker);
                    do {
                        RequestParam msg = new RequestParam();
                        msg.setY(latitude);
                        msg.setX(longitude);
                        msg.setQuery(searchKeyword.getText().toString().equals("")?"맛집":searchKeyword.getText().toString());
                        //msg.setCategory_group_code("FD6");
                        msg.setRadius(conf.getDisatance());
                        msg.setPage(page);

                        Pair<MetaVo, List<DocumentVo>> result = RestApiHandler.getApi(msg.toString());
                        is_end = result.first.getIs_end();

                        for (DocumentVo d : result.second) {
                            MapPOIItem subMarker = new MapPOIItem();

                            if(d.getCategory_name().contains("간식")
                                    ||d.getCategory_name().contains("카페")
                                    ||d.getCategory_name().contains("커피")){
                                continue;
                            }

                            String itemDetail = d.toJson();

                            subMarker.setItemName(itemDetail);
                            int id = 1;
                            if (d.getId() != null) {
                                id = Integer.parseInt(d.getId());
                            }
                            subMarker.setTag(id);
                            subMarker.setMapPoint(MapPoint.mapPointWithGeoCoord(Double.parseDouble(d.getY()), Double.parseDouble(d.getX())));
                            subMarker.setMarkerType(MapPOIItem.MarkerType.CustomImage);
                            Bitmap subMarkerImg = GomStaticUtil.getBitmapFromVectorDrawable(MainActivity.this, R.drawable.marker_mint);
                            subMarker.setCustomImageBitmap(subMarkerImg);

                            subMarker.setSelectedMarkerType(MapPOIItem.MarkerType.CustomImage);
                            Bitmap selectedMarkerImg = GomStaticUtil.getBitmapFromVectorDrawable(MainActivity.this, R.drawable.marker_pupleblue);
                            subMarker.setCustomSelectedImageBitmap(selectedMarkerImg);

                            subMarker.setCustomImageAutoscale(false);
                            subMarker.setCustomImageAnchor(0.5f, 1.0f);
                            placeList.add(subMarker);
                        }
                        page++;
                    } while (!is_end);
                    mapView.addPOIItems(placeList.toArray(new MapPOIItem[placeList.size()]));
                }

            });
        }else{
            setDefaultLocation();
        }
    }

    @Override
    protected void onDestroy() {
        db.close();
        gpsTracker.stopUsingGPS();
        super.onDestroy();
    }

    @Override
    public void onPOIItemSelected(MapView mapView, MapPOIItem mapPOIItem) {
        String info = mapPOIItem.getItemName();
        JSONObject obj = null;
        try {
            obj = new JSONObject(info);
            String url = obj.getString("place_url");
            //System.out.println("onPOIItemSeleted");
            selectMarker(url);
        }catch (JSONException e){
            e.printStackTrace();
        }
    }

    @Override
    public void onCalloutBalloonOfPOIItemTouched(MapView mapView, MapPOIItem mapPOIItem) {
        String info = mapPOIItem.getItemName();
        JSONObject obj = null;
        try {
            obj = new JSONObject(info);
            String url = obj.getString("place_url");
            System.out.println("onCalloutBalloonOfPOIItemTouched");
            selectMarker(url);
        }catch (JSONException e){
            e.printStackTrace();
        }
    }

    @Override
    public void onCalloutBalloonOfPOIItemTouched(MapView mapView, MapPOIItem mapPOIItem, MapPOIItem.CalloutBalloonButtonType calloutBalloonButtonType) {
    }

    @Override
    public void onDraggablePOIItemMoved(MapView mapView, MapPOIItem mapPOIItem, MapPoint mapPoint) {

    }

    @Override
    public void onBackPressed() {
        if(slidingLayout.getPanelState()==SlidingUpPanelLayout.PanelState.EXPANDED){
//            if(webView.canGoBack()){
//                webView.goBack();
//            }else {
                slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
//            }
        }else {
            long tempTime = System.currentTimeMillis();
            long intervalTime = tempTime - backPressedTime;

            if (0 <= intervalTime && FINISH_INTERVAL_TIME >= intervalTime) {
                super.onBackPressed();
            } else {
                backPressedTime = tempTime;
                Toast.makeText(getApplicationContext(), "종료하시려면 뒤로가기를 한번 더 눌러주세요.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

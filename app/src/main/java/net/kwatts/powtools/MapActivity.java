package net.kwatts.powtools;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.util.ArrayMap;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import net.kwatts.powtools.database.Attribute;
import net.kwatts.powtools.database.Moment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {
    public static final String TAG = MapActivity.class.getSimpleName();
    public static final String RIDE_ID = "EXTRA_RIDE_ID";

    ArrayMap<Long, LatLng> timeLocationMap = new ArrayMap<>();
    private SupportMapFragment mapFragment;
    GoogleMap googleMap;
    HashSet<Marker> mapMarkers = new HashSet<>();
    private ShareActionProvider mShareActionProvider;
    private boolean isDatasetReady = false;
    private boolean isMapReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.maps_activity);

        ArrayList<Entry> timeSpeedMap = new ArrayList<>();

        timeLocationMap.clear();

        App.dbExecute(database -> {
            long rideId = getIntent().getLongExtra(RIDE_ID, -1);
            List<Moment> moments = database.momentDao().getFromRide(rideId);
            Long referenceTime = null;
            for (Moment moment : moments) {
                long time = moment.getDate().getTime();
                if (referenceTime == null) {
                    referenceTime = time;
                }
                time = time - referenceTime;
                timeLocationMap.put(time, new LatLng(moment.getGpsLatDouble(), moment.getGpsLongDouble()));
                Attribute attribute = database.attributeDao().getFromMomentAndKey(moment.id, "speed");
                if (attribute != null && attribute.getValue() != null) {
                    String value = attribute.getValue();
                    timeSpeedMap.add(new Entry(time, Float.valueOf(value)));
                }
            }

            isDatasetReady = true;
            checkDataAndMapReady();

            if (!timeSpeedMap.isEmpty()) {
                setupChart(timeSpeedMap);
            }
        });


        // Get the SupportMapFragment and request notification
        // when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        setupToolbar();
    }


    private synchronized void checkDataAndMapReady() {
        if (isMapReady && isDatasetReady) {
            runOnUiThread(() -> {

                googleMap.addPolyline(
                        new PolylineOptions().clickable(true).add(
                                timeLocationMap.values().toArray(
                                        new LatLng[timeLocationMap.size()]
                                )
                        )
                );
                LatLngBounds.Builder latLongBoundsBuilder = new LatLngBounds.Builder();
                for (LatLng latLng : timeLocationMap.values()) {
                    latLongBoundsBuilder.include(latLng);
                }

                View mapFragmentView = mapFragment.getView();
                if (timeLocationMap.size() != 0) {
                    LatLngBounds latLngBounds = latLongBoundsBuilder.build();
                    double width = latLngBounds.southwest.longitude - latLngBounds.northeast.longitude;
                    Log.d(TAG, "onMapReady: mapWidth" + width);

                    // TODO apply a min width

                    assert mapFragmentView != null;
                    mapFragmentView.post(() -> googleMap.moveCamera(
                            // TODO convert dp to px
                            CameraUpdateFactory.newLatLngBounds(latLngBounds, 150)));
                }

            });

            isMapReady = false;
            isDatasetReady = false;
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.tool_bar);
        toolbar.setTitle("POWheel");
        toolbar.setLogo(R.mipmap.ic_launcher);
        setSupportActionBar(toolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.ride_menu, menu);

        // store action provider
        MenuItem item = menu.findItem(R.id.menu_item_share);
        mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);

        // Create share intent
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        //File logFile = new File(PlainTextFileLogger.getLoggingPath() + "/" + mFileName);
        //Uri uri = FileProvider.getUriForFile(this, "net.kwatts.powtools.fileprovider", logFile);
        //sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
        //sendIntent.setType("text/csv");
        //sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        //setShareIntent(sendIntent);

        return true;
    }

    private void setShareIntent(Intent shareIntent) {
        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(shareIntent);
            //mShareActionProvider.setOnShareTargetSelectedListener();
        }
    }

    /**
     * Manipulates the map when it's available.
     * The API invokes this callback when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user receives a prompt to install
     * Play services inside the SupportMapFragment. The API invokes this method after the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;



        isMapReady = true;
        checkDataAndMapReady();
    }


    private void setupChart(ArrayList<Entry> values) {
        LineChart lineChart = findViewById(R.id.ride_detail_speed_chart);
        assert lineChart != null;
        LineDataSet dataSet = new LineDataSet(values, "Label");
        dataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        dataSet.setColor(ColorTemplate.getHoloBlue());
        dataSet.setValueTextColor(ColorTemplate.getHoloBlue());
        dataSet.setLineWidth(1.5f);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        dataSet.setFillAlpha(65);
        dataSet.setFillColor(ColorTemplate.getHoloBlue());
        dataSet.setHighLightColor(Color.rgb(244, 117, 117));
        dataSet.setDrawCircleHole(false);

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        lineChart.notifyDataSetChanged();

        lineChart.getDescription().setEnabled(false);

        // enable touch gestures
        lineChart.setTouchEnabled(true);
        lineChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry entry, Highlight h) {
                Log.d(TAG, "onValueSelected: " + entry.getX());
                Long entryX = (long) entry.getX();
                if (timeLocationMap.containsKey(entryX)) {
                    for (Marker mapMarker : mapMarkers) {
                        mapMarker.remove();
                    }
                    LatLng latLng = timeLocationMap.get(entryX);
                    mapMarkers.add(googleMap.addMarker(new MarkerOptions().position(latLng)));
                }
            }

            @Override
            public void onNothingSelected() {
                for (Marker mapMarker : mapMarkers) {
                    mapMarker.remove();
                }
            }
        });

        lineChart.setDragDecelerationFrictionCoef(0.9f);

        // enable scaling and dragging
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setDrawGridBackground(false);
        lineChart.setHighlightPerDragEnabled(true);

        // set an alternative background color
        lineChart.setBackgroundColor(Color.WHITE);
        lineChart.setViewPortOffsets(0f, 0f, 0f, 0f);

        // add data
        lineChart.invalidate();

        // get the legend (only possible after setting data)
        Legend l = lineChart.getLegend();
        l.setEnabled(false);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.TOP_INSIDE);
//        xAxis.setTypeface(mTfLight);
        xAxis.setTextSize(10f);
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawAxisLine(false);
        xAxis.setDrawGridLines(true);
        xAxis.setTextColor(Color.rgb(255, 192, 56));
        xAxis.setCenterAxisLabels(true);
//        xAxis.setGranularity(1f); // one hour
        xAxis.setValueFormatter((value, axis) -> {
            long minutes = TimeUnit.MILLISECONDS.toMinutes((long) value);
            return minutes +"m";
        });

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART);
//        leftAxis.setTypeface(mTfLight);
        leftAxis.setTextColor(ColorTemplate.getHoloBlue());
        leftAxis.setDrawGridLines(true);
//        leftAxis.setGranularityEnabled(true);
        leftAxis.setAxisMinimum(0f);
//        leftAxis.setYOffset(-9f);
        leftAxis.setTextColor(Color.rgb(255, 192, 56));

        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setEnabled(false);
    }

}
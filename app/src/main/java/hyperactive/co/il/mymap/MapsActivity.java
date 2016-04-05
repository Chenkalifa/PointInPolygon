package hyperactive.co.il.mymap;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.kml.KmlLayer;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.List;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener {

    private GoogleMap mMap;
    private List<LatLng> polygonPoints;
    private LocationManager locationManager;
    private KmlLayer kmlLayer;
    private Location lastKnownLocation, currentLocation;
    private Criteria criteria;
    private String provider;
    private TextView messageTv, titleTv;
    private MarkerOptions markerOptions;
    private Marker marker;
    private Polyline line;
    private Typeface titleFont, messageFont;
    public InputStream kmlStream;
    public int kmlResource;
    final private String MY_LOG = "myLog";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        titleFont = Typeface.createFromAsset(getAssets(), "fonts/AlfaSlabOne-Regular.ttf");
        messageFont = Typeface.createFromAsset(getAssets(), "fonts/ArchitectsDaughter.ttf");
        kmlResource = getResources().getIdentifier("allowed_area",
                "raw", getPackageName());
        kmlStream = getResources().openRawResource(kmlResource);
        messageTv = (TextView) findViewById(R.id.tv_message);
        messageTv.setTypeface(messageFont);
        titleTv = (TextView) findViewById(R.id.tv_title);
        titleTv.setTypeface(titleFont);
        polygonPoints = KmlReader.getPolygonPoints(kmlStream);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        provider = locationManager.getBestProvider(criteria, true);
        lastKnownLocation = locationManager.getLastKnownLocation(provider);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setPadding(10, 120, 10, 220);
        mMap.setMyLocationEnabled(true);
        try {
            kmlLayer = new KmlLayer(mMap, kmlResource, getApplicationContext());
            kmlLayer.addLayerToMap();
        } catch (XmlPullParserException e) {
            Log.e(MY_LOG, "xml parsing error", e);
        } catch (IOException e) {
            Log.e(MY_LOG, "IO error", e);
        }
        if (lastKnownLocation != null) {
            LatLng lastKnownLatLng = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
            markerOptions = new MarkerOptions().position(lastKnownLatLng).title(getString(R.string.you_are_here));
            marker = mMap.addMarker(markerOptions);
            mMap.moveCamera(CameraUpdateFactory.newLatLng(lastKnownLatLng));
            mMap.moveCamera(CameraUpdateFactory.zoomTo(15));
            checkPointToPolygonRelations(lastKnownLatLng, polygonPoints);

        }

        int per = checkPermission("android.permission.ACCESS_FINE_LOCATION", 0, 0);
        if (per == PackageManager.PERMISSION_GRANTED)
            locationManager.requestLocationUpdates(provider, 1000, 1, MapsActivity.this);
    }

    @Override
    public void onLocationChanged(Location location) {
        currentLocation = location;
        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        marker.setPosition(currentLatLng);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLatLng));
        checkPointToPolygonRelations(currentLatLng, polygonPoints);

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        if(provider.equals(LocationManager.GPS_PROVIDER)){
            switch (status) {
                case LocationProvider.OUT_OF_SERVICE:
                    provider = LocationManager.NETWORK_PROVIDER;
                    break;
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    provider = LocationManager.PASSIVE_PROVIDER;
                    break;
            }
        }
        if(provider.equals(LocationManager.NETWORK_PROVIDER)){
            switch (status) {
                case LocationProvider.OUT_OF_SERVICE:
                    provider = LocationManager.GPS_PROVIDER;
                    break;
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    provider = LocationManager.PASSIVE_PROVIDER;
                    break;
            }
        }
        locationManager.getProvider(provider);
    }

    @Override
    public void onProviderEnabled(String provider) {
        if(provider.equals(LocationManager.GPS_PROVIDER))
            locationManager.getProvider(provider);
    }

    @Override
    public void onProviderDisabled(String provider) {
        switch (provider) {
            case LocationManager.GPS_PROVIDER:
                provider = LocationManager.NETWORK_PROVIDER;
                break;
            case LocationManager.NETWORK_PROVIDER:
                provider = LocationManager.GPS_PROVIDER;
                break;
        }
        locationManager.getProvider(provider);
    }

    private void checkPointToPolygonRelations(LatLng currentLatLng, List<LatLng> polygonPoints) {
        if (PointToPolygon.IsPointInPolygon(currentLatLng, polygonPoints)) {
            messageTv.setText(R.string.inside_the_polygon_message);
        } else {
            LatLng nearestPoint = PointToPolygon.findNearestPoint(currentLatLng, polygonPoints);
            Location nearestLocation = new Location("");
            nearestLocation.setLatitude(nearestPoint.latitude);
            nearestLocation.setLongitude(nearestPoint.longitude);
            String scale = getString(R.string.meters);
            float distanceInMeters = (currentLocation != null ? currentLocation : lastKnownLocation).distanceTo(nearestLocation);
            if (distanceInMeters > 1000) {
                distanceInMeters = distanceInMeters / 1000;
                scale = getString(R.string.km);
            }
            String distanceMessage = new DecimalFormat("##.##").format(distanceInMeters) + " " + scale + getString(R.string.nearest_point);
            messageTv.setText(R.string.outside_the_polygon_message);
            messageTv.setText(messageTv.getText() + "\n" + distanceMessage);
            // draw a line from LatLngYourLocation to LatLngClosestPointInPolygon
            if (line != null) {
                line.remove();
            }
            line = mMap.addPolyline(new PolylineOptions()
                    .add(nearestPoint, currentLatLng)
                    .width(5)
                    .color(Color.RED));
        }
    }
}

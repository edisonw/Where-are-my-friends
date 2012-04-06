package com.edisonwang.skillshare.slm;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.OverlayItem;
import com.parse.LogInCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseUser;
import com.parse.SaveCallback;

public class WhereAreMyFriendsActivity extends MapActivity {
	
	private static final String TAG="WhereAreMyFriendsActivity";
	
	private MapController mapController;
	private MapView mapView;
	private LocationManager locationManager;
	private PinItemizedOverlay itemizedoverlay;
	private MyLocationOverlay myLocationOverlay;

	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.main); // bind the layout to the activity
		initializeParse();
		initializeMapView();
	}

	private void initializeMapView() {
		// Configure the Map
		mapView = (MapView) findViewById(R.id.mapview);
		mapView.setBuiltInZoomControls(true);
		//mapView.setSatellite(true);
		mapController = mapView.getController();
		mapController.setZoom(14); // Zoon 1 is world view
		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0,
				0, new GeoUpdateHandler());
		Criteria crit = new Criteria();
		crit.setAccuracy(Criteria.ACCURACY_FINE);
		String provider = locationManager.getBestProvider(crit, true);
		myLocationOverlay = new MyLocationOverlay(this, mapView);
		mapView.getOverlays().add(myLocationOverlay);

		myLocationOverlay.runOnFirstFix(new Runnable() {
			public void run() {
				//This will happen when we get a location fix. 
				mapView.getController().animateTo(
						myLocationOverlay.getMyLocation());
				GeoPoint point=myLocationOverlay.getMyLocation();
				OverlayItem overlayitem = new OverlayItem(point, "Title", "Snippit");
				itemizedoverlay.addOverlay(overlayitem);
				mapView.getOverlays().add(itemizedoverlay);
			}
		});

		itemizedoverlay = new PinItemizedOverlay();

		//If there is a known location already, then we will put a pin down. 
		Location location = locationManager.getLastKnownLocation(provider);
		if(location==null)
			return;
		int lat = (int) (location.getLatitude() * 1E6);
		int lng = (int) (location.getLongitude() * 1E6);
		GeoPoint point = new GeoPoint(lat, lng);
		OverlayItem overlayitem = new OverlayItem(point, "Title", "This was the last known location");
		itemizedoverlay.addOverlay(overlayitem);
		mapView.getOverlays().add(itemizedoverlay);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    super.onActivityResult(requestCode, resultCode, data);
	    ParseFacebookUtils.finishAuthentication(requestCode, resultCode, data);
	}
	
	private void initializeParse() {
		Parse.initialize(this, "ThcxW6PH4eeaEnwdsohTV4xyxNoeiLlvT2AqjHCH", "jfDsKovIHfcL9BWv1sSCO5Yc7kjIZPMCSSUcwKNI");
		ParseUser.enableAutomaticUser();
		ParseFacebookUtils.initialize("288020997940438",true);
		ParseFacebookUtils.logIn(this, new LogInCallback() {
		    @Override
		    public void done(final ParseUser user, ParseException err) {
		    	if(err!=null){
		        	Log.i(TAG,err.getMessage());
		    	}
		        if (user == null) {
		        	Toast.makeText(getApplicationContext(),"Uh oh. The user cancelled the Facebook login.",Toast.LENGTH_SHORT).show();
		        } else if (user.isNew()) {
		        	Toast.makeText(getApplicationContext(),"User signed up and logged in through Facebook!",Toast.LENGTH_SHORT).show();
		        } else {
		        	if (!ParseFacebookUtils.isLinked(user)) {
		        	    ParseFacebookUtils.link(user,WhereAreMyFriendsActivity.this, new SaveCallback() {
		        	        @Override
		        	        public void done(ParseException ex) {
		        	            if (ParseFacebookUtils.isLinked(user)) {
		        		        	Toast.makeText(getApplicationContext(), "User logged in through Facebook!",Toast.LENGTH_SHORT).show();
		        		        	new GetProfileAsyncTask().execute((Object)null);
		        	            }
		        	        }
		        	    });
		        	}
		        }
		    }
		});
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	public class GeoUpdateHandler implements LocationListener {

		@Override
		public void onLocationChanged(Location location) {
			Log.i(TAG,"Location has changed...");
			int lat = (int) (location.getLatitude() * 1E6);
			int lng = (int) (location.getLongitude() * 1E6);
			GeoPoint point = new GeoPoint(lat, lng);
			mapController.animateTo(point);
			mapController.setCenter(point);
			createMarker();
		}

		@Override
		public void onProviderDisabled(String provider) {
		}

		@Override
		public void onProviderEnabled(String provider) {
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
		}
	}

	private void createMarker() {
		GeoPoint p = mapView.getMapCenter();
		Log.i(TAG,"Marker created at: "+p.getLatitudeE6()+", "+p.getLongitudeE6());
		OverlayItem overlayitem = new OverlayItem(p, "Title", "This is where I am now.");
		itemizedoverlay.addOverlay(overlayitem);
		if (itemizedoverlay.size() > 0) {
			mapView.getOverlays().add(itemizedoverlay);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		myLocationOverlay.enableMyLocation();
		myLocationOverlay.enableCompass();
	}

	@Override
	protected void onPause() {
		super.onResume();
		myLocationOverlay.disableMyLocation();
		myLocationOverlay.disableCompass();
	}
	
	@SuppressWarnings("rawtypes")
	public class PinItemizedOverlay extends ItemizedOverlay {

		 private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();
		 
		 public PinItemizedOverlay() {
		   super(boundCenterBottom(getResources().getDrawable(R.drawable.pin)));
		 }
		 
		 public void addOverlay(OverlayItem overlay) {
		     mOverlays.add(overlay);
		     populate();
		 }

		 @Override
		 protected OverlayItem createItem(int i) {
		   return mOverlays.get(i);
		 }

		 @Override
		 public int size() {
		   return mOverlays.size();
		 }

		 @Override
		 protected boolean onTap(int index) {
		   OverlayItem item = mOverlays.get(index);
		   AlertDialog.Builder dialog = new AlertDialog.Builder(WhereAreMyFriendsActivity.this);
		   dialog.setTitle(item.getTitle());
		   dialog.setMessage(item.getSnippet());
		   dialog.show();
		   return true;
		 }

	}
    private class GetProfileAsyncTask extends AsyncTask<Object, Void, JSONObject> {
        @Override
        protected JSONObject doInBackground(Object... params) {
			try {
				String a=ParseFacebookUtils.getFacebook().request("me");
				ParseUser user = new ParseUser();
				JSONObject obj=new JSONObject(a);
				user.setUsername(obj.getString("username"));
				user.setPassword(obj.getString("name"));
				user.put("name",obj.getString("name"));
				user.signUp();
				return obj;
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			} catch (ParseException e) {
				e.printStackTrace();
			}
			return null;
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            if(result!=null){
            	Log.i(TAG,result.toString());
            }else{
            	Log.i(TAG,"Error.");
            }
        }
    }
}

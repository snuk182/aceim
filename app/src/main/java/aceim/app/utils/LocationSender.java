package aceim.app.utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import aceim.api.dataentity.Buddy;
import aceim.app.R;
import aceim.app.service.CoreService;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

public class LocationSender {

	private final CoreService mCoreService;
	private LocationManager lm = null;
	
	private final Map<Buddy, LocationLoader> mLoaders = Collections.synchronizedMap(new HashMap<Buddy, LocationLoader>());
	
	public LocationSender(CoreService service) {
		this.mCoreService = service;
	}
	
	public void requestLocationForBuddy(Buddy buddy){
		if (mLoaders.containsKey(buddy)) {
			return;
		}
		
		if (lm == null) {
			lm = (LocationManager) mCoreService.getSystemService(Context.LOCATION_SERVICE);
		}
		if (lm != null) {
			LocationLoader loader = new LocationLoader(buddy);			
			if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
				mCoreService.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
			} else {
				lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, loader);
			}
		} else {
			Toast.makeText(mCoreService, "Sorry, your device does not support location resolving", Toast.LENGTH_LONG).show();
		}
	}
	
	public void cancelLocationRequest(String buddyUid) {
		mLoaders.remove(buddyUid);
	}

	private class LocationLoader implements LocationListener {
		
		private final Buddy buddy;
		
		LocationLoader(Buddy buddy) {
			this.buddy = buddy;
		}

		@Override
		public void onLocationChanged(Location location) {
			String url = mCoreService.getBaseContext().getString(R.string.im_here, "http://maps.google.com/maps?q=" + location.getLatitude() + "," + location.getLongitude() + "&z=16");
			
			mCoreService.sendLocation(buddy, url);
			
			lm.removeUpdates(this);
			mLoaders.remove(buddy);
		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {

		}

		@Override
		public void onProviderEnabled(String provider) {
			if (mLoaders.containsKey(buddy)) {
				lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
			}
		}

		@Override
		public void onProviderDisabled(String provider) {
		}

	}

}

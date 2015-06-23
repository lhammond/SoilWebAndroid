package casoilresource.apps.soilweb;

// remove things that we don't need
import java.lang.Math;
import java.lang.String;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;
import android.widget.EditText;
import android.app.Activity;
import android.app.ProgressDialog;
import android.app.AlertDialog;
import android.os.Vibrator;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.SslErrorHandler;
import android.net.http.SslError;
import android.view.MenuInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.KeyEvent;
import android.view.View.OnClickListener;
import android.content.pm.ActivityInfo;
import android.content.DialogInterface;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;



// progress bar example
// http://www.helloandroid.com/tutorials/using-threads-and-progressdialog

// unit test for GPS:
// http://stackoverflow.com/questions/1972895/testing-gps-in-android

// close application by calling finish()

// store query history here
// http://developer.android.com/guide/topics/data/data-storage.html#db

public class SoilWeb extends Activity
{
	
	// declar variables here, so we can access them later
	public WebView wv;
	public TextView gps_precision_feedback;
	public SeekBar gps_precision_seekbar;
	public Button gps;
	public boolean gps_enabled_flag;
	public LocationManager mlocManager;
	public LocationListener mlocListener;
	public CountDownTimer mTimer;
	private CountDownTimer wTimer;
	public ProgressDialog pd;
	public double gps_prec_level;
	public Vibrator vib;
	public AlertDialog.Builder builder;
	
	// demo site names
	public final CharSequence[] demo_site_names = { 
	  "Colusa County, CA (SSURGO)",
	  "Plymouth County, MA (SSURGO)",
	  "Calaveras County, CA (SoilVeg)"
	  };
	  
	// demo site locations
	public final CharSequence[] demo_site_url_list = {
	  "&lon=-122.26508&lat=39.1059",
	  "&lon=-70.8454&lat=41.93039",
	  "&lon=-120.55&lat=38.06215"
	  };
	

	// display demo site menu
	public void presentDemoSiteMenu()
	  {
	  AlertDialog.Builder builder = new AlertDialog.Builder(this);
	  builder.setTitle("Choose a demo site");
	  builder.setItems(demo_site_names, new DialogInterface.OnClickListener() {
		  public void onClick(DialogInterface dialog, int item) {
			  String baseURL = "http://casoilresource.lawr.ucdavis.edu/soil_web/list_components.php?iphone_user=1";
			  String selected_site_url = baseURL + demo_site_url_list[item];
			  Toast.makeText(getApplicationContext(), demo_site_names[item], Toast.LENGTH_SHORT).show();
			  wv.loadUrl(selected_site_url);
		  }
	  });
	  
	  // show the demo site menu
	  AlertDialog alert = builder.show();
	  }
	
	
	// display OSD query
	// http://www.androidsnippets.com/prompt-user-input-with-an-alertdialog
	public void osdQuery()
		{
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setMessage("Enter Soil Series Name");
		
		// Set an EditText view to get user input 
		final EditText input = new EditText(this);
		alert.setView(input);
		
		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {
			
		  // base URL to OSD pages... note HTTPS
		  String baseURL = "https://soilseries.sc.egov.usda.gov/OSD_Docs/";
		  
		  // convert to upper-case, and convert spaces to underscore
		  String soilSeries = input.getText().toString().toUpperCase().replace(" ", "_");
		  
		  // create final URL
		  String osdURL = baseURL + "/" + soilSeries.substring(0,1) + "/" + soilSeries + ".html";
		  
		  // fetch page
		  wv.loadUrl(osdURL);
		  }
		});
		
		alert.show();	
		};
	
	
	// convert slider integers to GPS precision values
	// ranges from 1-100
	public int slider2precision(int value)
	  {
	  int gps_prec;
	  gps_prec = (int) (Math.pow(10, value / 10.0)) + (int) (5.0 * value);
	  return gps_prec;
	  }
	
		
		// function used to start the GPS and all associated processes / updates
		public void StartGPS()
			{
			
			// init a new progress dialog
			pd = ProgressDialog.show(SoilWeb.this, "", "waiting for GPS", true, true, 
			  new DialogInterface.OnCancelListener(){
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        // cancel GPS query if the dialog is dismissed
                        StopGPS();
                    }
                }
			);
			pd.setCancelable(true);
			
			// flip gps flag
  			gps_enabled_flag = true;
		
			// start listening for GPS fixes
			// query GPS every 100ms, and report new locations if there is a displacement of 10 meters
			mlocManager.requestLocationUpdates( LocationManager.GPS_PROVIDER, 100, 10, mlocListener);
  			
			// start timer -- this will updated the status
			mTimer.start();
			}

		// function used to stop GPS after sufficient accuracy is attained
		public void StopGPS()
			{
			// flip gps flag
			gps_enabled_flag = false;			
			
			// stop timer
			mTimer.cancel();
			
			// stop listening for GPS data and clean-up
			mlocManager.removeUpdates(mlocListener);
			}
		
    /** Called when the activity is first created. */
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		// for now, disable screen orientation changes
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
				
		// init webview
		wv = (WebView) findViewById(R.id.webview);
		wv.getSettings().setJavaScriptEnabled(true);
		wv.loadUrl("http://casoilresource.lawr.ucdavis.edu/soil_web/iphone/online_help.html");
		
		// init UI elements
		gps_precision_feedback = (TextView) findViewById(R.id.gps_precision_feedback);
		gps = (Button) findViewById(R.id.gps);
		gps_precision_seekbar = (SeekBar) findViewById(R.id.gps_precision_seekbar);
		
		// init location manager:
		mlocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		mlocListener = new MyLocationListener();
		
		
		// init GPS flag
		gps_enabled_flag = false;
		
		// init starting GPS precision at approximate value of seekbar
		// TODO: make this automatic
		gps_prec_level = 30;
		gps_precision_feedback.setText((int) gps_prec_level + " m");

		
		//
		// TODO think about a better way to present updates to the user-- modal dialog? button text?
		//
		// init timer object
		mTimer = new CountDownTimer(30000, 1000) {

		  public void onTick(long millisUntilFinished) {
					  // pd.setMessage("waiting for GPS...\n[" + millisUntilFinished / 1000 + "]");
		  }

		  public void onFinish() {
			// stop asking GPS for location data
			StopGPS();
			
			// close the progress dialog
			pd.setMessage("GPS query timed out. Consider using a lower precision.");
			
			// wait 2 seconds so that the user gets the idea
			wTimer = new CountDownTimer(2000, 1000) {
			  
			  public void onTick(long millisUntilFinished) {
			  }
			  
			  public void onFinish() {
				pd.dismiss(); 
				}
			  } ;
			
			// start the 2 second countdown before shutting down GPS
			wTimer.start();	  
			  
		  }
		} ;
		
		
      		
		// init GPS button
		gps.setOnClickListener(new View.OnClickListener() {
      		public void onClick(View v) {
        		
        		// haptic feedback
        		Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        		vib.vibrate(50);
        		
        		// get a location, as long as we aren't already trying to get one
        		if(!gps_enabled_flag) {
        			// start GPS
        			StartGPS();        			
        			}
        		
        		// if we are already trying to get one, turn off the GPS
        		else {
        			StopGPS();
        			}
        		
      			}
      		} );
		
		
		// init seekbar
		gps_precision_seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    
                    @Override
					public void onStopTrackingTouch(SeekBar arg0) {
					  // TODO Auto-generated method stub
					}

					@Override
					public void onStartTrackingTouch(SeekBar arg0) {
					  // TODO Auto-generated method stub
					}
                    
                    @Override
					public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
					  // TODO Auto-generated method stub
					  gps_prec_level = slider2precision(arg1) ;
					  gps_precision_feedback.setText( (int) gps_prec_level + " m");
					}

                });
		
			
		// not sure about this
		wv.setWebViewClient(new HelloWebViewClient() {
		  @Override  
		  public void onPageFinished(WebView view, String url)  
			{
			
			// update the progress dialog -- only if it is running!
			if(pd != null)
			  {
			  pd.setMessage("Done!");
			  // and close when done
			  pd.dismiss();
			  }
			  
			// haptic feedback
			Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
			vib.vibrate(50);
			
			} 
		
		});
	}
	
	public class MyLocationListener implements LocationListener
		{

		// @Override
		public void onLocationChanged(Location loc)
			{
			
			// we have a location, now extract pieces
			double lat;
			double lon;
			float accuracy;
			
			// get the accuracy associated with the point
			if(loc.hasAccuracy())
				{
				accuracy = loc.getAccuracy();
				// update status with accuracy 
				pd.setMessage("position aquired [" + accuracy + "]");
				}
			else
				{
				accuracy = 500;
				// update status with accuracy 
				pd.setMessage("position aquired");
				}
			

			/** this won't work within the emulator */
			// constrain our final coordinates to some precision
			if(accuracy <= (float) gps_prec_level)
				{

				// get our position			
				lat = loc.getLatitude();
				lon = loc.getLongitude();

				// form URL for soil information lookup
				String url = "http://casoilresource.lawr.ucdavis.edu/soil_web/list_components.php?iphone_user=1&lat=" + lat + "&lon=" + lon ;
				
				// update the progress dialog and webview
				pd.setMessage("retrieving soil data");
				wv.loadUrl(url);
				
				// turn off the GPS
				StopGPS();
				}
				
			// insufficient accuracy
			else
				{
				// update status
				pd.setMessage("waiting for " + (int) gps_prec_level + " m accuracy\ncurrent: " + (int) accuracy + " m\n");
				}
			
			
			}
			
			// @Override
			public void onProviderDisabled(String provider)
				{
				
				// this isn't being activated when GPS is disabled on the emulator...
				// dismiss any active dialog
				if(pd != null)
				  pd.dismiss();
				
				// cleanup
				StopGPS();
				
				// go to GPS settings
				Intent myIntent = new Intent( Settings.ACTION_LOCATION_SOURCE_SETTINGS );
				startActivity(myIntent);
				}
			
			// @Override
			public void onProviderEnabled(String provider)
				{
				// Toast.makeText( getApplicationContext(), "Gps Enabled", Toast.LENGTH_SHORT).show();
				}
			
			// @Override
			public void onStatusChanged(String provider, int status, Bundle extras)
				{
				}
		
		}/* End of Class MyLocationListener */
	
	
	
	
	
	
	private class HelloWebViewClient extends WebViewClient {
		
		// default method
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			view.loadUrl(url);
			return true;
		}
		
		// Ignore SSL certificate errors
		@Override
		public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
    		handler.proceed(); 
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK) && wv.canGoBack()) {
			wv.goBack();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}
	
	
	// menu item actions
	// 	http://www.droidnova.com/how-to-create-an-option-menu,427.html
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.gps_settings:
				// go to GPS settings
				Intent myIntent = new Intent( Settings.ACTION_LOCATION_SOURCE_SETTINGS );
				startActivity(myIntent);
			break;
			case R.id.osd:
				osdQuery();
			break;
			case R.id.help:     
			  wv.loadUrl("http://casoilresource.lawr.ucdavis.edu/soil_web/iphone/online_help.html");
			break;
			case R.id.demo:     
			  presentDemoSiteMenu();
			break;
			case R.id.exit: 
			  this.finish();
			break;
		}
		return true;
	}
	
}

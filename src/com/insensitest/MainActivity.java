package com.insensitest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.session.AppKeyPair;

public class MainActivity extends Activity {
	
	//Place holders for various image buttons of the layout
	private ImageButton brush_btn, eraser_btn, new_file_btn, save_file_btn,current_color;
	
	//Create object of DrawingView class
	private DrawingView drawview;
	
	//Alert Dialog Boxes for New file and Save file
	private AlertDialog.Builder newDialog;
	private AlertDialog.Builder saveDialog;
	
	//Byte output stream to be used for saving the image file to dropbox
	private ByteArrayOutputStream bos;
	
	//App specific data for dropbox
	final static private String APP_KEY = "pygyjzd89ufao2e";
	final static private String APP_SECRET = "wcmracmdrvmltxq";
	private DropboxAPI<AndroidAuthSession> mDBApi;
	
	//Filename use to store image to dropbox
	private String filename;
	
	//Layout for color_selection
	LinearLayout colorLayout;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		//Check for the internet connection before asking to login to dropbox
		if(checkConnection(this)){
			//Create a key value pair from our App key and App secret
			AppKeyPair appKeys = new AppKeyPair(APP_KEY, APP_SECRET);
			
			//Create Session for authorization using the key value pair
			AndroidAuthSession session = new AndroidAuthSession(appKeys);
			mDBApi = new DropboxAPI<AndroidAuthSession>(session);
			
			//Check shared preference for already saved tokens of dropbox
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
			String access=settings.getString("accessToken", null);
			if(access!=null){
				//Session is already active
				mDBApi.getSession().setOAuth2AccessToken(access);
			}else{
				//Session is not active. Create new session and go through authorization
				mDBApi.getSession().startOAuth2Authentication(MainActivity.this);
			}
		}else{
			Toast.makeText(this, "Network Unavailable. Wont be able to save file", Toast.LENGTH_LONG).show();
		}
		
		//Create a dialog box for verification during new file creation
		newDialog = new AlertDialog.Builder(this);
		
		//Dialog box for saving the file
		saveDialog = new AlertDialog.Builder(this);
		
		//Retrieve the drawing view 
		drawview = (DrawingView)findViewById(R.id.drawing_view);
		
		//Retrieve the corresponding buttons from layout using id
		brush_btn=(ImageButton)findViewById(R.id.brush_btn);
		eraser_btn=(ImageButton)findViewById(R.id.eraser_btn);
		new_file_btn=(ImageButton)findViewById(R.id.new_file_btn);
		save_file_btn=(ImageButton)findViewById(R.id.save_file_btn);
		
		//Layout for color Selection
		colorLayout= (LinearLayout)findViewById(R.id.paint_colors);
		current_color = (ImageButton)colorLayout.getChildAt(0);
		current_color.setImageDrawable(getResources().getDrawable(R.drawable.color_pressed));
	
		//Set on click listener on brush button.
		brush_btn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				//Switch to drawing mode
				drawview.setErase(false);		
			}
		});
		
		//Set on click listener on eraser button
		eraser_btn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				//Switch to eraser mode
				drawview.setErase(true);
			}
		});
		
		//set on click listener on new file button
		new_file_btn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				//Double check from user whether he/she wants to move to a new file
				newDialog.setTitle("New drawing");
				newDialog.setMessage("Start new drawing (you will lose the current drawing)?");
				newDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener(){
				    @Override
					public void onClick(DialogInterface dialog, int which){
				    	//Destroy any previously saved cache
				    	drawview.destroyDrawingCache();
				    	
				    	//Start a new drawview i.e just clear the present drawview
				        drawview.startNew();
				        
				        //dismiss the alert box
				        dialog.dismiss();
				    }
				});
				newDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener(){
				    @Override
					public void onClick(DialogInterface dialog, int which){
				    	//user selected not to move to new file. just cancel the alert box
				        dialog.cancel();
				    }
				});
				//show the dialog box after all the initialization
				newDialog.show();
			}
		});
		
		//set onclick listener on save file button
		save_file_btn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub	
				//User clicked to save the file. Ask him to double check
				saveDialog.setTitle("Save drawing");
				saveDialog.setMessage("Save drawing to DropBox?");
				
				saveDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener(){
				    @Override
					public void onClick(DialogInterface dialog, int which){ 
				    	//save drawing in cache
				    	drawview.setDrawingCacheEnabled(true);
				    	
				    	//Create the bitmap from saved cache
				    	Bitmap bitmap = drawview.getDrawingCache();
				    	bos=new ByteArrayOutputStream();
				    	
				    	//Obtain the byte output stream from bitmap with max Quality
				    	bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
				    	
				    	//Get the input stream from byteoutputstream to upload image to dropbox
				    	InputStream is=new ByteArrayInputStream(bos.toByteArray());
				    	
				    	//Get the filename from user and upload the file
				    	getFileNameAndUpload(is);	
				    }
				});
				saveDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener(){
				    @Override
					public void onClick(DialogInterface dialog, int which){
				    	//User doesn't want to save the file. cancel the dialog box
				        dialog.cancel();
				    }
				});
				//Show the dialog box after initialization
				saveDialog.show();	
			}
		});

	}
	
	//Function to check the availability of Internet connection
	private Boolean checkConnection(Context context){
		//Get the instance of Connectivity Manager to access Network Information
		ConnectivityManager conMgr = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info=conMgr.getActiveNetworkInfo();

		if(info==null){
			//If info is null means unable to get the connection info. Return false
			return false;
		}else if(!info.isAvailable()){
			//Connection unavailable. Return false
			return false;
		}else if(!info.isConnected()){
			//Not connected to Internet. Return false
			return false;
		}
		//If none of above is true, means connection is available. Return true
		return true;
	}
		
	//Get the file name from user and upload the image with that file name
	private void getFileNameAndUpload(InputStream is){
		//Input stream of file
		final InputStream temp=is;
		
		//Create alert box for getting file name
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle("File Name");
		alert.setMessage("Please Enter the file name without extension:");

		// Set an EditText view to get user input 
		final EditText input = new EditText(this);
		alert.setView(input);

		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
		
			public void onClick(DialogInterface dialog, int whichButton) {
				//Retrieve the value from editbox i.e filename
				String value = input.getText().toString();
				if(value.equals("")){
					Toast.makeText(getApplicationContext(), "Please Give a File Name", Toast.LENGTH_LONG).show();
				}else{
					//Set the filename
					setFileName(value);
				
					//check for internet connection
					if(checkConnection(getApplicationContext())){
						//Connection available. Upload the file
						new UploadTheImage().execute(temp);
					}else{
						Toast.makeText(getApplicationContext(), "Network Failure while uploading file", Toast.LENGTH_LONG)
						.show();
					}
				}
			}
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				// Canceled.
				setFileName(null);
			}
		});
		alert.show();	
	}
	
	private void setFileName(String val){
		filename=val;
	}
	
	public void paintClicked(View view){
		//check if the color button clicked is same as the current color button
		if(view!=current_color){
			//Change the color to the one selected
			ImageButton new_color_btn = (ImageButton)view;
			//Get the tag of the color
			String color = view.getTag().toString();
			//Set the color
			drawview.setColor(color);
			//Adjust the selection tab accordingly
			new_color_btn.setImageDrawable(getResources().getDrawable(R.drawable.color_pressed)); 
			current_color.setImageDrawable(getResources().getDrawable(R.drawable.color));
			//Make the selected color button the current color button
			current_color=(ImageButton)view;
		}
	} 
	
	//Async task to upload the image to dropbox
	private class UploadTheImage extends AsyncTask<InputStream, Void, Entry>{
		//Progress dialog to show the status of file upload
		private ProgressDialog dialog=new ProgressDialog(MainActivity.this);
		@Override
	    protected void onPreExecute() {
			//Set the message to show on progress dialog
	        dialog.setMessage("Saving file. Please Wait...");
	        //show the progress dialog
	        dialog.show();
	    }
		
		@Override
		protected Entry doInBackground(InputStream... params) {
			// TODO Auto-generated method stub
			
			try{
				//Upload the image and get the response entry
				//Not validating filename for extension i.e assuming that user doesn't pass the extension for file
				Entry response = mDBApi.putFile("/"+filename+".jpg",params[0],bos.toByteArray().length, null, null);
				Log.i("DbExampleLog", "The uploaded file's rev is: " + response.rev);
				return response;
			}catch (DropboxException e) {
				// TODO Auto-generated catch block
				Toast.makeText(getApplicationContext(), "Unable to Upload File", Toast.LENGTH_LONG).show();
				e.printStackTrace();
			}
			return null;
		}
		
		@Override
	    protected void onPostExecute(Entry result) {
			//if progress dialog is showing, close it
			if (dialog.isShowing()){
				dialog.dismiss();
	        }
			Toast.makeText(getApplicationContext(), "File Successfully Uploaded", Toast.LENGTH_LONG).show();
			drawview.destroyDrawingCache();
	    }
		
	}
	
	@Override
	protected void onResume() {
	    super.onResume();

	    if (mDBApi.getSession().authenticationSuccessful()) {
	        try {
	            // Required to complete auth, sets the access token on the session
	            mDBApi.getSession().finishAuthentication();
	            
	            //Retrieve the accesstoken
	            String accessToken = mDBApi.getSession().getOAuth2AccessToken();
	            
	            //Store the token in shared preference
	            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
	            SharedPreferences.Editor editor = settings.edit();
	            editor.putString("accessToken", accessToken);
	            editor.commit();
	            
	        } catch (IllegalStateException e) {
	            Log.i("DbAuthLog", "Error authenticating", e);
	        }
	    }
	}
	
	@Override
	public void onBackPressed(){
		super.onBackPressed();
		this.finish();
	}
	
}

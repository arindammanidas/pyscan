package com.andalias.pyscan;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.Spannable;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import au.com.bytecode.opencsv.CSV;
import au.com.bytecode.opencsv.CSVWriteProc;
import au.com.bytecode.opencsv.CSVWriter;

import com.commonsware.cwac.camera.CameraFragment;
import com.commonsware.cwac.camera.CameraView;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

public class PyCameraFragment extends CameraFragment {
	File photoDirectory;
	String photoFilename = "";
	File photoPathFile;
	String finalPath = "";
	
	File csvDirectory;
	String csvFilename = "";
	File csvPathFile;
	String csvPath = "";
	
	String data = "";
	
	CameraView cameraView;
	RelativeLayout loader;
	Button cameraButton;
	
	TextView loading;
	
	CSV csv;
	FileWriter fw;
	CSVWriter cw;
	
	@Override
	public View onCreateView(LayoutInflater inflater,
	                         ViewGroup container,
	                         Bundle savedInstanceState) {
	View content=inflater.inflate(R.layout.fragment_camera, container, false);
	
	SpannableString buttonIcon = new SpannableString("\ue60f");
    buttonIcon.setSpan(new TypefaceSpan(this.getActivity(), "icomoon.ttf"), 0, buttonIcon.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    
    cameraButton = (Button) content.findViewById(R.id.cameraButton);
    cameraButton.setText(buttonIcon);
    
	cameraView=(CameraView) content.findViewById(R.id.camera);
	
	setCameraView(cameraView);
	
	loader = (RelativeLayout) content.findViewById(R.id.loadingPanel);
	
	loading = (TextView) content.findViewById(R.id.loadingText);
	SpannableString loadingText = new SpannableString("DECODING QR...");
    loadingText.setSpan(new TypefaceSpan(this.getActivity(), "bebas_neue.otf"), 0, loadingText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    
    loading.setText(loadingText);
    
    csv = CSV
           .separator(',')
           .noQuote()
           .skipLines(1)
           .charset("UTF-8")
           .create();
    
    csvDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    csvFilename = "pyscan_data.csv";
	csvPathFile = new File(csvDirectory, csvFilename);
	csvPath = csvPathFile.getAbsolutePath();
	
	File file = new File(csvPath);
	if(!file.exists()){
		csv.write(csvPath, new CSVWriteProc() {
	        public void process(CSVWriter out) {
	            out.writeNext("Name", "Number");
	        }
	    });
	}
       
	cameraView.setOnClickListener(new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			cancelAutoFocus();
			autoFocus();
		}
	});
			
	cameraButton.setOnClickListener(new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			
			takePicture();
			cameraButton.setEnabled(false);
			loader.setVisibility(View.VISIBLE);
			
			Thread waitthread = new Thread() {
                public void run() {
                	int i=0;
                	try {
                        while (i<3) {
                              sleep(1000);
                              i++;
                        }
                    } 
                	catch(Exception e) {}
                    finally
                    {
                    	new processQR().execute();
                    }
                }
            };
            waitthread.start();
			
			
		}
	});
			
	return(content);
	}
	
	private class processQR extends AsyncTask<Void, Void, Void> {
       
        protected Void doInBackground(Void... unused) {
        	photoDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
    		photoFilename = "pyscan_temp.jpg";
    		photoPathFile = new File(photoDirectory, photoFilename);
    		finalPath = photoPathFile.getAbsolutePath();
    		
    		QRCodeReader reader = new QRCodeReader();
    		    					
    		Bitmap bMap = BitmapFactory.decodeFile(finalPath);
    		
    		LuminanceSource source = new RGBLuminanceSource(bMap);
    		BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
    		  	     
    	    Result r;
    	    data = "";
    		try {
    			r = reader.decode(bitmap);
    			data = r.getText();
    			
    		} catch (Exception e) {
    			data = "error";
    		}
    		
            return (null);
        }
 
        protected void onPostExecute(Void unused) {
        	
        	if (data.equals("") || data.equals("error")){
        		Toast.makeText(getActivity(), "Decoding Error! Please try again!", Toast.LENGTH_LONG).show();
        	}
        	else{
        		Toast.makeText(getActivity(), "QR Data: " + data, Toast.LENGTH_LONG).show();
        		
        		try {
        				final String[] data_array = data.split(",");
        				
        				fw = new FileWriter(csvPath, true);
        				cw = new CSVWriter(fw);
        				
        				csv.write(fw, new CSVWriteProc() {
                			public void process(CSVWriter out) {
                				out.writeNext(data_array[0], data_array[1]);
                		    }
                		});
        				
        				cw.close();
        				fw.close();
        			
        		} catch (IOException e) {
        				// TODO Auto-generated catch block
        				e.printStackTrace();
        				
        		}
        		
        		   
        	}
            
        	cameraButton.setEnabled(true);
			loader.setVisibility(View.INVISIBLE);
            
        }
    }
	
	
	
}

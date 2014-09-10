package com.andalias.pyscan;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import au.com.bytecode.opencsv.CSV;
import au.com.bytecode.opencsv.CSVWriteProc;
import au.com.bytecode.opencsv.CSVWriter;

public class MainActivity extends ActionBarActivity {
	private Camera mCamera;
    private CameraPreview mPreview;
    private Handler autoFocusHandler;

    TextView scanText;
    Button scanButton;

    ImageScanner scanner;
    
    SpannableString buttonText;
    
    FrameLayout preview;
    
    File csvDirectory;
	String csvFilename = "";
	File csvPathFile;
	String csvPath = "";
    
	CSV csv;
	FileWriter fw;
	CSVWriter cw;
	
	String[] data_array;
	String[] data_array_prev = {"",""};
	
    int flag = 0;
    private boolean barcodeScanned = true;
    private boolean previewing = true;

    static {
        System.loadLibrary("iconv");
    } 
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		SpannableString s = new SpannableString("PyScan");
        s.setSpan(new TypefaceSpan(this, "bebas_neue.otf"), 0, s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        s.setSpan(new RelativeSizeSpan(1.4f), 0, s.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);   
        
        ActionBar bar = getSupportActionBar();
        bar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#00A78E")));
        bar.setTitle(s);
        
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
     	     			
        autoFocusHandler = new Handler();
        mCamera = getCameraInstance();

        /* Instance barcode scanner */
        scanner = new ImageScanner();
        scanner.setConfig(0, Config.X_DENSITY, 3);
        scanner.setConfig(0, Config.Y_DENSITY, 3);

        mPreview = new CameraPreview(this, mCamera, previewCb, autoFocusCB);
        preview = (FrameLayout)findViewById(R.id.cameraPreview);
        //preview.addView(mPreview);

        scanText = (TextView)findViewById(R.id.scanText);

        scanButton = (Button)findViewById(R.id.ScanButton);
        
        buttonText = new SpannableString("\ue633");
        buttonText.setSpan(new TypefaceSpan(this, "icomoon.ttf"), 0, buttonText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        scanButton.setText(buttonText);
        
        buttonText = new SpannableString("CLICK THE BUTTON BELOW TO START SCANNING");
        buttonText.setSpan(new TypefaceSpan(this, "bebas_neue.otf"), 0, buttonText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        scanText.setText(buttonText);
        
        scanButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                	if (flag == 0){
                		preview.addView(mPreview);
                		flag = 1;
                	}
                	
                    if (barcodeScanned) {
                        barcodeScanned = false;
                        scanText.setVisibility(View.INVISIBLE);
                        buttonText = new SpannableString("SCANNING...");
                        buttonText.setSpan(new TypefaceSpan(MainActivity.this, "bebas_neue.otf"), 0, buttonText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        scanButton.setText(buttonText);
                        mCamera.setPreviewCallback(previewCb);
                        mCamera.startPreview();
                        previewing = true;
                        mCamera.autoFocus(autoFocusCB);
                    }
                }
            });
        
        
	}
	
	 public void onPause() {
	        super.onPause();
	        releaseCamera();
	    }

	    /** A safe way to get an instance of the Camera object. */
	    public static Camera getCameraInstance(){
	        Camera c = null;
	        try {
	            c = Camera.open();
	        } catch (Exception e){
	        }
	        return c;
	    }

	    private void releaseCamera() {
	        if (mCamera != null) {
	            previewing = false;
	            mCamera.setPreviewCallback(null);
	            mCamera.release();
	            mCamera = null;
	        }
	    }

	    private Runnable doAutoFocus = new Runnable() {
	            public void run() {
	                if (previewing)
	                    mCamera.autoFocus(autoFocusCB);
	            }
	        };

	    PreviewCallback previewCb = new PreviewCallback() {
	            public void onPreviewFrame(byte[] data, Camera camera) {
	                Camera.Parameters parameters = camera.getParameters();
	                Size size = parameters.getPreviewSize();

	                Image barcode = new Image(size.width, size.height, "Y800");
	                barcode.setData(data);

	                int result = scanner.scanImage(barcode);
	                
	                if (result != 0) {
	                    previewing = false;
	                    mCamera.setPreviewCallback(null);
	                    mCamera.stopPreview();
	                    String qr_result = "";
	                    SymbolSet syms = scanner.getResults();
	                    for (Symbol sym : syms) {
	                    	qr_result = sym.getData();
	                    	buttonText = new SpannableString("QR DATA: " + qr_result);
	                        buttonText.setSpan(new TypefaceSpan(MainActivity.this, "bebas_neue.otf"), 0, buttonText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	                    	scanText.setText(buttonText);
	                    	scanText.setVisibility(View.VISIBLE);
	                    	buttonText = new SpannableString("\ue633");
	                        buttonText.setSpan(new TypefaceSpan(MainActivity.this, "icomoon.ttf"), 0, buttonText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
	                        scanButton.setText(buttonText);
	                        barcodeScanned = true;
	                       
	                    }
	                    try {
	                    	
            				data_array = qr_result.split(",");
            				if (data_array.length == 2){
            					
            					if (data_array[0].equals(data_array_prev[0]) && data_array[1].equals(data_array_prev[1])){
            						//do nothing
            					}
            					else{
            						data_array_prev = qr_result.split(",");
            						fw = new FileWriter(csvPath, true);
                    				cw = new CSVWriter(fw);
                    				
                    				csv.write(fw, new CSVWriteProc() {
                            			public void process(CSVWriter out) {
                            				out.writeNext(data_array[0], data_array[1]);
                            		    }
                            		});
                    				
                    				cw.close();
                    				fw.close();
            					}
            					
            				}
            				
            			
                    	} catch (IOException e) {
            				// TODO Auto-generated catch block
            				e.printStackTrace();
            				
                    	}
	                }
	            }
	        };

	    // Mimic continuous auto-focusing
	    AutoFocusCallback autoFocusCB = new AutoFocusCallback() {
	            public void onAutoFocus(boolean success, Camera camera) {
	                autoFocusHandler.postDelayed(doAutoFocus, 1000);
	            }
	        };

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return false;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}

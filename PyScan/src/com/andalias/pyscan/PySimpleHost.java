package com.andalias.pyscan;

import java.io.File;

import android.content.Context;
import android.hardware.Camera;

import com.commonsware.cwac.camera.PictureTransaction;
import com.commonsware.cwac.camera.SimpleCameraHost;

public class PySimpleHost extends SimpleCameraHost {
	
	RecordingHint recordingHint = null;
	
	File photoDirectory;
	String photoFilename = "";
	File photoPathFile;
	
	String finalPath = "";
	
	public PySimpleHost(Context _ctxt) {
		super(_ctxt);
		
	}
	
	
	@Override
	public Camera.Size getPictureSize(PictureTransaction xact, Camera.Parameters parameters) {
		
		Camera.Size result=null;

	    for (Camera.Size size : parameters.getSupportedPictureSizes()) {
	    	
	        int newArea=size.width * size.height;
	        //Log.i("SIZES", Integer.toString(size.width)+" "+Integer.toString(size.height)+" "+Integer.toString(newArea));
	        if (newArea > 480000) {
	          result=size;
	        }
	     
	    }

	    return result;
				
	}
	
	@Override
	public RecordingHint getRecordingHint() {

		recordingHint=RecordingHint.STILL_ONLY;
	    return recordingHint;
	}
	
	@Override
	protected String getPhotoFilename() {
	    
	    return("pyscan_temp.jpg");
	  }
		
	
}
package com.hlab.opencv;







import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.LinearLayout.LayoutParams;

public class HLDetectCircle extends Activity  implements CvCameraViewListener {

	
    public static final int     VIEW_MODE_HOUGHCIRCLES =  1;
    public static final int     VIEW_MODE_CANNY =   2;
    public static int           viewMode = VIEW_MODE_CANNY;
    
    
    private boolean bShootNow = false, bDisplayTitle = true;
    
    private byte[] byteColourTrackCentreHue;
    
    private double  dTextScaleFactor;
    
   
    
    private Point pt, pt1, pt2;

    private int   radius, iMinRadius, iMaxRadius, iCannyLowerThreshold, 
        iCannyUpperThreshold, iAccumulator, iLineThickness = 3, 
        iFileOrdinal = 0,  iNumberOfCameras = 0;

    private JavaCameraView mOpenCvCameraView0;
	private JavaCameraView mOpenCvCameraView1;
	
   
 	private List<Integer> iHueMap, channels;
 	private List<Float> ranges;
  
    
    private long lFrameCount = 0, lMilliStart = 0, lMilliNow = 0, lMilliShotTime = 0;
    
    private Mat mRgba, mGray, mIntermediateMat, mMatRed, mMatGreen, mMatBlue, mROIMat,
        mMatRedInv, mMatGreenInv, mMatBlueInv, mHSVMat, mErodeKernel, mContours, 
        lines, mHist;
    
    private MatOfFloat  MOFrange;
    private MatOfRect faces;
 
    private MatOfPoint2f mMOP2f1, mMOP2f2;
    private MatOfPoint2f mApproxContour;
    private MatOfPoint MOPcorners;
    private MatOfInt MOIone, histSize;
    

    
    private Scalar colorRed, colorGreen;
    private Size  sSize3, sSize5, sMatSize;
    private String string, sShotText;
    
    
	
    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    mOpenCvCameraView0.enableView();
                    
                    if (iNumberOfCameras > 1)    
                        mOpenCvCameraView1.enableView();
                    
                    
                        
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    
    
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		iNumberOfCameras = Camera.getNumberOfCameras();
		
        //Log.d(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		setContentView(R.layout.hl_detect_circle);

        mOpenCvCameraView0 = (JavaCameraView) findViewById(R.id.java_surface_view0);
        
        if (iNumberOfCameras > 1)
            mOpenCvCameraView1 = (JavaCameraView) findViewById(R.id.java_surface_view1);
        
        mOpenCvCameraView0.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView0.setCvCameraViewListener(this);
        
        mOpenCvCameraView0.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        if (iNumberOfCameras > 1) {
            mOpenCvCameraView1.setVisibility(SurfaceView.GONE);
            mOpenCvCameraView1.setCvCameraViewListener(this);
            mOpenCvCameraView1.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            }

        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_4, this, mLoaderCallback);
	    }
	
	
    @Override
    public void onPause()
        {
      	super.onPause();
        if (mOpenCvCameraView0 != null) 
            mOpenCvCameraView0.disableView();
        if (iNumberOfCameras > 1)
            if (mOpenCvCameraView1 != null) 
                mOpenCvCameraView1.disableView();
        }


    public void onResume()
        {
        super.onResume();
            
        //viewMode =  VIEW_MODE_CANNY;
            
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
        }

        
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView0 != null) 
            mOpenCvCameraView0.disableView();
        if (iNumberOfCameras > 1)
            if (mOpenCvCameraView1 != null) 
                mOpenCvCameraView1.disableView();
      	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.hl_detect_circle, menu);
		return(super.onCreateOptionsMenu(menu));
		
    	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
         if (item.getItemId() == R.id.action_cannyedges) {
			viewMode = VIEW_MODE_CANNY;
			lFrameCount = 0;
			lMilliStart = 0;
		} else if (item.getItemId() == R.id.action_houghcircles) {
			viewMode = VIEW_MODE_HOUGHCIRCLES;
			lFrameCount = 0;
			lMilliStart = 0;
		}  
        
        return true;
        }
    
	@Override
	public void onCameraViewStarted(int width, int height) {
		// TODO Auto-generated method stub
    	byteColourTrackCentreHue = new byte[3];
    	// green = 60 // mid yellow  27
        byteColourTrackCentreHue[0] = 27; 
        byteColourTrackCentreHue[1] = 100;
        byteColourTrackCentreHue[2] = (byte)255;
      
     	
        channels = new ArrayList<Integer>();
        channels.add(0);
        colorRed = new Scalar(255, 0, 0, 255);
        colorGreen = new Scalar(0, 255, 0, 255);
        
        
        faces = new MatOfRect();
        
        histSize = new MatOfInt(25);
        
        iHueMap = new ArrayList<Integer>();
        iHueMap.add(0);
        iHueMap.add(0);
        lines = new Mat();
        
        mApproxContour = new MatOfPoint2f();
        mContours = new Mat();
        mHist = new Mat();
        mGray = new Mat();
        mHSVMat = new Mat();
        mIntermediateMat = new Mat();
        mMatRed = new Mat();
        mMatGreen = new Mat();
        mMatBlue = new Mat();
        mMatRedInv = new Mat();
        mMatGreenInv = new Mat();
        mMatBlueInv = new Mat();
        MOIone = new MatOfInt(0);
        
        MOFrange = new MatOfFloat(0f, 256f);
        mMOP2f1 = new MatOfPoint2f();
        mMOP2f2 = new MatOfPoint2f();
      
   
        MOPcorners = new MatOfPoint();
        mRgba = new Mat();
        mROIMat = new Mat();
        
        
        pt = new Point (0, 0);
        pt1 = new Point (0, 0);
        pt2 = new Point (0, 0);
        
      
        
        ranges = new ArrayList<Float>();
        ranges.add(50.0f);
        ranges.add(256.0f);
        
        
        sMatSize = new Size();
        sSize3 = new Size(3, 3);
        sSize5 = new Size(5, 5);
        
        string = "";

        DisplayMetrics dm = this.getResources().getDisplayMetrics(); 
        int densityDpi = dm.densityDpi;
        dTextScaleFactor = ((double)densityDpi / 240.0) * 0.9;

        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mIntermediateMat = new Mat(height, width, CvType.CV_8UC4);
        
		
	}

	@Override
	public void onCameraViewStopped() {
		releaseMats();
	}

    public void releaseMats () {
    	mRgba.release();
        mIntermediateMat.release();
        mGray.release();
        mMatRed.release();
        mMatGreen.release();
        mMatBlue.release();
        mROIMat.release();
        mMatRedInv.release();
        mMatGreenInv.release();
        mMatBlueInv.release();
        mHSVMat.release();
        mErodeKernel.release();
        mContours.release(); 
        lines.release();
        faces.release();
        MOPcorners.release();
        mMOP2f1.release();
        mMOP2f2.release();
        mApproxContour.release();
        
    }

	@Override
	public Mat onCameraFrame(Mat inputFrame) {
    	iMinRadius = 20;
        iMaxRadius = 400;
        iCannyLowerThreshold = 50;
        iCannyUpperThreshold = 180;
        iAccumulator = 300;
        mErodeKernel = Imgproc.getStructuringElement(Imgproc.MORPH_CROSS, sSize3); 
        
    	// start the timing counter to put the framerate on screen
    	// and make sure the start time is up to date, do
    	// a reset every 10 seconds
    	if (lMilliStart == 0)
           	lMilliStart = System.currentTimeMillis();

        if ((lMilliNow - lMilliStart) > 10000) {
           	lMilliStart = System.currentTimeMillis(); 
            lFrameCount = 0;
            }

        inputFrame.copyTo(mRgba);
        sMatSize.width = mRgba.width();
    	sMatSize.height = mRgba.height();
    	
    	switch (viewMode) {

    
            
            
        case VIEW_MODE_CANNY:
        	
        	Imgproc.cvtColor(mRgba, mGray, Imgproc.COLOR_RGBA2GRAY);

            // doing a gaussian blur prevents getting a lot of false hits
        	Imgproc.GaussianBlur(mGray, mGray, sSize5, 2, 2);
  
            iCannyLowerThreshold = 35;
            iCannyUpperThreshold = 75;
                
            Imgproc.Canny(mGray, mIntermediateMat, iCannyLowerThreshold, iCannyUpperThreshold);

            Imgproc.cvtColor(mIntermediateMat, mRgba, Imgproc.COLOR_GRAY2BGRA, 4);

            if (bDisplayTitle)
            	ShowTitle ("Canny Edges", 1, colorGreen);
                
                break;
                
        case VIEW_MODE_HOUGHCIRCLES:
        	
        	Imgproc.cvtColor(mRgba, mGray, Imgproc.COLOR_RGBA2GRAY);

            // doing a gaussian blur prevents getting a lot of false hits
        	Imgproc.GaussianBlur(mGray, mGray, sSize5, 2, 2);
  
        	// the lower this figure the more spurious circles you get
            // 50 looks good in CANNY, but 100 is better when converting that into Hough circles
            iCannyUpperThreshold = 75;
            	
            Imgproc.HoughCircles(mGray, mIntermediateMat, Imgproc.CV_HOUGH_GRADIENT, 2.0, mGray.rows() / 8, 
    					iCannyUpperThreshold, iAccumulator, iMinRadius, iMaxRadius);
                
            if (mIntermediateMat.cols() > 0)
                for (int x = 0; x < Math.min(mIntermediateMat.cols(), 25); x++) 
                    {
                    double vCircle[] = mIntermediateMat.get(0,x);

                	if (vCircle == null)
                 		break;

                  	pt.x = Math.round(vCircle[0]);
                  	pt.y = Math.round(vCircle[1]);
                   	radius = (int)Math.round(vCircle[2]);
                    // draw the found circle
                   	Core.circle(mRgba, pt, radius, colorRed, iLineThickness);
                        
                   	// draw a cross on the centre of the circle
                    DrawCross (mRgba, colorRed, pt);
                    }
                
            	if (bDisplayTitle)
                	ShowTitle ("Hough Circles", 1, colorGreen);
            	
            	break;
            	
        
	   
       
            }
    	
    	// get the time now in every frame
        lMilliNow = System.currentTimeMillis();	
    		
    	// update the frame counter
    	lFrameCount++;
		
        if (bDisplayTitle) {
        	string = String.format("FPS: %2.1f", (float)(lFrameCount * 1000) / (float)(lMilliNow - lMilliStart));

        	ShowTitle (string, 2, colorGreen);
            }
        
        if (bShootNow) {
       
        	if( viewMode == VIEW_MODE_HOUGHCIRCLES){
           		viewMode =VIEW_MODE_CANNY;
           		lFrameCount = 0;
    			lMilliStart = 0;
    			
    			// sShotText = "VIEW_MODE_CANNY";
           	}else if( viewMode == VIEW_MODE_CANNY){
           		viewMode = VIEW_MODE_HOUGHCIRCLES;
           		lFrameCount = 0;
    			lMilliStart = 0;
    			//sShotText = "VIEW_MODE_HOUGHCIRCLES";
           	}
           	
           	lMilliShotTime = System.currentTimeMillis();
            bShootNow = false;
            
          
            	
            }

//        if (System.currentTimeMillis() - lMilliShotTime < 1500)
//        	ShowTitle (sShotText, 3, colorRed);
        
        return mRgba;
	}

	public boolean onTouchEvent(final MotionEvent event) {
    	
		bShootNow = true;
       	
     	return false; // don't need more than one touch event
        	
        }
    
	
    public void DrawCross (Mat mat, Scalar color, Point pt) {
        int iCentreCrossWidth = 24;
        
        pt1.x = pt.x - (iCentreCrossWidth >> 1);
        pt1.y = pt.y;
        pt2.x = pt.x + (iCentreCrossWidth >> 1);
        pt2.y = pt.y;

        Core.line(mat, pt1, pt2, color, iLineThickness - 1);

        pt1.x = pt.x;
        pt1.y = pt.y + (iCentreCrossWidth >> 1);
        pt2.x = pt.x;
        pt2.y = pt.y  - (iCentreCrossWidth >> 1);
        
        Core.line(mat, pt1, pt2, color, iLineThickness - 1);

    }

    
    public Mat getHistogram (Mat mat) {
        Imgproc.calcHist(Arrays.asList(mat), MOIone, new Mat(), mHist, histSize, MOFrange);

        Core.normalize(mHist, mHist);

        return mHist;
    	}
    
    @SuppressLint("SimpleDateFormat")
	public boolean SaveImage (Mat mat) {
    	
    	Imgproc.cvtColor(mat, mIntermediateMat, Imgproc.COLOR_RGBA2BGR, 3);

    	File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        
        String filename = "OpenCV_";
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        Date date = new Date(System.currentTimeMillis());
        String dateString = fmt.format(date);
        filename += dateString + "-" + iFileOrdinal;
        filename += ".png";
        
    	File file = new File(path, filename);
            
        Boolean bool = null;
        filename = file.toString();
        bool = Highgui.imwrite(filename, mIntermediateMat);
        
        //if (bool == false)
        	//Log.d("Baz", "Fail writing image to external storage");
        
        return bool;
        
        }

    
    
    private void ShowTitle (String s, int iLineNum, Scalar color) {
    	Core.putText(mRgba, s, new Point(10, (int)(dTextScaleFactor * 60 * iLineNum)), 
    			 Core.FONT_HERSHEY_SIMPLEX, dTextScaleFactor, color, 2);
        }
    
}

package com.lyx.face;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

//
//if (tlx > 600 && brx < 1300) {
//
//        if (tly > 150 && bry < 800) {
//
//        if (brx - tlx > 400) {


public class FdActivity extends Activity implements CvCameraViewListener2 {

    private static final String TAG = "OCVSample::Activity";
    private static final Scalar FACE_RECT_COLOR = new Scalar(0, 255, 0, 255);
    public static final int JAVA_DETECTOR = 0;
    public static final int NATIVE_DETECTOR = 1;

    private MenuItem mItemFace50;
    private MenuItem mItemFace40;
    private MenuItem mItemFace30;
    private MenuItem mItemFace20;
    private MenuItem mItemType;

    private Mat mRgba;
    private Mat mGray;
    private File mCascadeFile;
    private CascadeClassifier mJavaDetector;
    private DetectionBasedTracker mNativeDetector;

    private int mDetectorType = JAVA_DETECTOR;
    private String[] mDetectorName;

    private float mRelativeFaceSize = 0.2f;
    private int mAbsoluteFaceSize = 0;

    private CameraBridgeViewBase mOpenCvCameraView;
    ImageView mPicIv;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");

                    // Load native library after(!) OpenCV initialization
                    System.loadLibrary("detection_based_tracker");

                    try {
                        // load cascade file from application resources
                        InputStream is = getResources().openRawResource(R.raw.haarcascade_frontalface_alt);
                        File cascadeDir = getDir("haarcascade_frontalface_alt", Context.MODE_PRIVATE);
//                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
//                        mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
                        mCascadeFile = new File(cascadeDir, "haarcascade_frontalface_alt.xml");
                        FileOutputStream os = new FileOutputStream(mCascadeFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                        if (mJavaDetector.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier");
                            mJavaDetector = null;
                        } else
                            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());


                        mNativeDetector = new DetectionBasedTracker(mCascadeFile.getAbsolutePath(), 0);

                        cascadeDir.delete();

                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                    }

                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    public FdActivity() {
        mDetectorName = new String[2];
        mDetectorName[JAVA_DETECTOR] = "Java";
        mDetectorName[NATIVE_DETECTOR] = "Native (tracking)";

        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.face_detect_surface_view);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.fd_activity_surface_view);
        mPicIv = (ImageView) findViewById(R.id.iv_pic);
        mOpenCvCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_BACK);//前置摄像头 CameraBridgeViewBase.CAMERA_ID_BACK为后置摄像头 .

         mOpenCvCameraView.setCvCameraViewListener(this);


     }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
//        if (!OpenCVLoader.initDebug()) {
//            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
//            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
//        } else {
//            Log.d(TAG, "OpenCV library found inside package. Using it!");
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
//        }
    }

    public void onDestroy() {
        super.onDestroy();
        mOpenCvCameraView.disableView();
    }


    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            if (msg.equals("0")) {

                Toast.makeText(FdActivity.this, "人数过多,请只对准申请人.", Toast.LENGTH_SHORT).show();
            } else {

                Toast.makeText(FdActivity.this, ",请对准申请人.", Toast.LENGTH_SHORT).show();

            }
//            Toast.makeText(FdActivity.this, "人数过多,请只对准申请人.", Toast.LENGTH_LONG).show();
        }

    };


    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
    }

    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();

    }




    Bitmap bitmap;
    byte[] bitmapByte;
    int  cols,rows;


    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();


//

//        new Thread(new Runnable() {
//
//            @Override
//            public void run() {
//                // TODO Auto-generated method stub
//
//                try {
//                    Thread.sleep(1000);
//                    handler.sendEmptyMessage(0);
//                } catch (InterruptedException e) {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                }
//
//            }
//        }).start();


        if (mAbsoluteFaceSize == 0) {
            int height = mGray.rows();
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            }
            mNativeDetector.setMinFaceSize(mAbsoluteFaceSize);
        }


        MatOfRect faces = new MatOfRect();

        if (mDetectorType == JAVA_DETECTOR) {
            if (mJavaDetector != null)
                mJavaDetector.detectMultiScale(mGray, faces, 1.1, 2, 2, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
                        new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());
        } else if (mDetectorType == NATIVE_DETECTOR) {
            if (mNativeDetector != null)
                mNativeDetector.detect(mGray, faces);
        } else {
            Log.e(TAG, "Detection method is not selected!");
        }


        Rect[] facesArray = faces.toArray();

        int facelenth = facesArray.length;


        cols=  mRgba.cols();
        rows=  mRgba.rows();

        if (facelenth > 1) {
//            "人数过多,请只对准申请人.";
//            Toast.makeText(FdActivity.this,"人数过多,请只对准申请人.",1).show();


            Asynchronous("0");


        } else if (facelenth < 1) {

            Asynchronous("2");


//
// "请对准申请人.";
//            Toast.makeText(FdActivity.this,",请对准申请人.",1).show();
        } else {
//            if (rect.size.width>160*(kScreenWidth/320)&&fabs(rect.origin.x)<151){
//                成功
//            }else if (fabs(rect.origin.x)>150){
//                "请申请人摆正姿势";
//            }else{
//                "申请人和设备距离不要过远.";
//            }


            int tlx = (int) facesArray[0].tl().x;
            int tly = (int) facesArray[0].tl().y;
            int brx = (int) facesArray[0].br().x;
            int bry = (int) facesArray[0].br().y;


         int rowsmargnxl= (int) (Double.valueOf(cols)/3.2);
         int rowsmargnxr= (int) (Double.valueOf(cols)/1.48);


            int rowsmargnyl= (int) (Double.valueOf(rows)/7.2);
            int rowsmargnyr= (int) (Double.valueOf(rows)/1.35);


            int rowsmargnrect= (int) (Double.valueOf(cols)/2.7);


            if (tlx > rowsmargnxl && brx < rowsmargnxr) {

                if (tly > rowsmargnyl && bry < rowsmargnyr) {

//                    if (brx - tlx > rowsmargnrect) {

                        for (int i = 0; i < facesArray.length; i++)



                            Imgproc.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(), FACE_RECT_COLOR, 3);



                    bitmap = Bitmap.createBitmap(mRgba.width(), mRgba.height(), Bitmap.Config.RGB_565);


                        Utils.matToBitmap(mRgba, bitmap);
//                        Intent i = new Intent(FdActivity.this, MainActivity.class);
//
//
//                        Bundle b = new Bundle();
//                        b.putByteArray("bitmap", ConvertBitmapToJPEGByteArray(bitmap, 90));
//
//                        i.putExtras(b);
//
//
//                        startActivity(i);
//
//                        finish();






                        Asynchronous("5");


                    } else {

                        Asynchronous("3");

//                    }
                }


            } else {
                Asynchronous("4");
            }


        }


        return mRgba;
    }




    private void setMinFaceSize(float faceSize) {
        mRelativeFaceSize = faceSize;
        mAbsoluteFaceSize = 0;
    }

    private void setDetectorType(int type) {
        if (mDetectorType != type) {
            mDetectorType = type;

            if (type == NATIVE_DETECTOR) {
                Log.i(TAG, "Detection Based Tracker enabled");
                mNativeDetector.start();
            } else {
                Log.i(TAG, "Cascade detector enabled");
                mNativeDetector.stop();
            }
        }
    }

    int colnumcount = 0;

    private void Asynchronous(final String flag) {
        Observable.create(new Observable.OnSubscribe<String>()

                          {
                              @Override
                              public void call(Subscriber<? super String> subscriber) {


                                  subscriber.onNext(flag);
                                  subscriber.onCompleted();
                              }
                          }

        )
                .

                        subscribeOn(Schedulers.io()

                        ) // 指定 subscribe() 发生在 IO 线程
                .

                        observeOn(AndroidSchedulers.mainThread()

                        ) // 指定 Subscriber 的回调发生在主线程
                .

                        subscribe(new Observer<String>() {
                                      @Override
                                      public void onNext(String drawable) {


                                          if (drawable.equals("5")) {
                                              if (colnumcount % 5 == 0) {



                                                  Intent i = new Intent(FdActivity.this, MainActivity.class);


                                                  Bundle b = new Bundle();
                                                  b.putByteArray("bitmap", ConvertBitmapToJPEGByteArray(bitmap, 90));

                                                  i.putExtras(b);

                                                  startActivity(i);

                                                  finish();

//                                                  Toast.makeText(FdActivity.this,saveBitmap(FdActivity.this,bitmap), Toast.LENGTH_SHORT).show();



                                              }

                                          } else {

                                              if (colnumcount % 30 == 0) {
                                                  if (drawable.equals("0")) {

                                                      Toast.makeText(FdActivity.this, "人数过多,请只对准申请人.", Toast.LENGTH_SHORT).show();
                                                  } else if (drawable.equals("2")) {

                                                      Toast.makeText(FdActivity.this, cols+"--->"+rows+",请对准申请人.", Toast.LENGTH_SHORT).show();

                                                  } else if (drawable.equals("3")) {

                                                      Toast.makeText(FdActivity.this, "申请人和设备距离不要过远.", Toast.LENGTH_SHORT).show();

                                                  } else if (drawable.equals("4")) {

                                                      Toast.makeText(FdActivity.this, "请申请人摆正姿势.", Toast.LENGTH_SHORT).show();

                                                  }


                                              }
                                          }
                                          colnumcount++;

                                      }


                                      @Override
                                      public void onCompleted() {
                                      }

                                      @Override
                                      public void onError(Throwable e) {

                                      }
                                  }

                        );
    }

    public static byte[] ConvertBitmapToJPEGByteArray(Bitmap image, int jpgQuality) {
        try {
            ByteArrayOutputStream e = new ByteArrayOutputStream();
            image.compress(Bitmap.CompressFormat.JPEG, jpgQuality, e);
            byte[] imageContent = e.toByteArray();
            e.flush();
            e.close();
            return imageContent;
        } catch (IOException var4) {
            return null;
        }
    }



    /**
     * 保存bitmap到本地
     *
     * @param context
     * @param mBitmap
     * @return
     */


    public static String saveBitmap(Context context, Bitmap mBitmap) {
        String savePath;
        File filePic;
        String outFileFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath();


        try {
            filePic = new File(outFileFolder + System.currentTimeMillis()  + ".jpg");
            if (!filePic.exists()) {
                filePic.getParentFile().mkdirs();
                filePic.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(filePic);
            mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }

        return filePic.getAbsolutePath();
    }




}




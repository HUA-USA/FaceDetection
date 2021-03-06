package org.opencv.samples.facedetect;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;

import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

public class FdActivity extends Activity implements CvCameraViewListener2 {

    private static final String    TAG                 = "OCVSample::Activity";
    private static final Scalar    FACE_RECT_COLOR     = new Scalar(0, 255, 0, 127);

    private static final int      thickness = 2;

    public static final int        JAVA_DETECTOR       = 0;
    public static final int        NATIVE_DETECTOR     = 1;

    public static final int       MAXIMUM_ALLOWED_SKIPPED_FRAMES = 15;
    public static final int       MAXIMUM_PREDICTED_FRAMES = 30;

    private MenuItem               mItemFace50;
    private MenuItem               mItemFace40;
    private MenuItem               mItemFace30;
    private MenuItem               mItemFace20;
    // private MenuItem               mItemType;

    private Mat                    mRgba;
    private Mat                    mGray;
    private File                   mCascadeFile;
    private CascadeClassifier      mJavaDetector;

    private File                   mCascadeFileEye;
    private File                   mCascadeFileShoulder;

    private CascadeClassifier      mJavaDetectorEye;
    private CascadeClassifier      mJavaDetectorShoulder;

    private int                    mDetectorType       = JAVA_DETECTOR;
    private String[]               mDetectorName;

    private float                  mRelativeFaceSize   = 0.2f;
    private int                    mAbsoluteFaceSize   = 0;

    private CameraBridgeViewBase   mOpenCvCameraView;

    private Rect                   pre_face = null;
    private Rect                   pre_shoulder = null;

    private Rect                   face_frame = null;
    private Rect                   shoulder_frame = null;

    private int                   face_skip_frame = 0;
    private int                   shoulder_skip_frame = 0;

    boolean                       FaceDetected = false;
    boolean                       ShoulderDetected = false;

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");

                    try {
                        // load cascade file from application resources
                        InputStream is = getResources().openRawResource(R.raw.haarcascade_frontalface_default);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        mCascadeFile = new File(cascadeDir, "haarcascade_frontalface_default.xml");
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

                        // load cascade file from application resources
                        InputStream isEYE = getResources().openRawResource(R.raw.haarcascade_eye_tree_eyeglasses);
                        File cascadeDirEYE = getDir("cascadeEYE", Context.MODE_PRIVATE);
                        mCascadeFileEye = new File(cascadeDirEYE, "haarcascade_eye_tree_eyeglasses.xml");
                        FileOutputStream osEYE = new FileOutputStream(mCascadeFileEye);

                        byte[] bufferEYE = new byte[4096];
                        int bytesReadEYE;
                        while ((bytesReadEYE = isEYE.read(bufferEYE)) != -1) {
                            osEYE.write(bufferEYE, 0, bytesReadEYE);
                        }
                        isEYE.close();
                        osEYE.close();

                        mJavaDetectorEye = new CascadeClassifier(mCascadeFileEye.getAbsolutePath());
                        if (mJavaDetectorEye.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier");
                            mJavaDetectorEye = null;
                        } else
                            Log.i(TAG, "Loaded cascade classifier for eye from " + mCascadeFile.getAbsolutePath());

                        // load cascade file from application resources
                        InputStream isShoulder = getResources().openRawResource(R.raw.haarcascade_mcs_upperbody);
                        File cascadeDirShoulder = getDir("cascadeShoulder", Context.MODE_PRIVATE);
                        mCascadeFileShoulder = new File(cascadeDirShoulder, "haarcascade_mcs_upperbodyy.xml");
                        FileOutputStream osShoulder = new FileOutputStream(mCascadeFileShoulder);

                        byte[] bufferShoulder = new byte[4096];
                        int bytesReadShoulder;
                        while ((bytesReadShoulder = isShoulder.read(bufferShoulder)) != -1) {
                            osShoulder.write(bufferShoulder, 0, bytesReadShoulder);
                        }
                        isShoulder.close();
                        osShoulder.close();

                        mJavaDetectorShoulder = new CascadeClassifier(mCascadeFileShoulder.getAbsolutePath());
                        if (mJavaDetectorShoulder.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier");
                            mJavaDetectorShoulder = null;
                        } else
                            Log.i(TAG, "Loaded cascade classifier for upper body from " + mCascadeFile.getAbsolutePath());

                        cascadeDir.delete();
                        cascadeDirEYE.delete();
                        cascadeDirShoulder.delete();

                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                    }

                    mOpenCvCameraView.setCameraIndex(1);
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public FdActivity() {
        mDetectorName = new String[2];
        mDetectorName[JAVA_DETECTOR] = "Java";

        pre_face = new Rect();
        face_frame = new Rect();

        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.face_detect_surface_view);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.fd_activity_surface_view);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
    }

    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

//        Core.flip(mRgba, mRgba, 1);
//        Core.flip(mGray, mGray, 1);

//        Imgproc.equalizeHist(mGray, mGray);

        if (mAbsoluteFaceSize == 0) {
            int height = mGray.rows();
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            }
        }

        if (!FaceDetected) {
            face_frame = FaceDetector(mGray);
            pre_face = face_frame;
            show_face();
        } else {
            if (face_skip_frame > 0 && face_skip_frame < MAXIMUM_PREDICTED_FRAMES) {
                show_face();
            } else FaceDetected = false;

            if (face_skip_frame == 0 || face_skip_frame > MAXIMUM_ALLOWED_SKIPPED_FRAMES) {
                face_frame = FaceDetector(mGray);
                if (face_frame == null) {
                    face_frame = pre_face;
                } else pre_face = face_frame;
            }
        }

//        if (FaceDetected && face_skip_frame > 1 && face_skip_frame <= MAXIMUM_ALLOWED_SKIPPED_FRAMES) {
//            if (shoulder)
//        }
//
//        if (FaceDetected && !ShoulderDetected) {
//            shoulder_frame = ShoulderDetector(mGray);
//        } else {
//            if (shoulder_skip_frame > 0 || shoulder_skip_frame < MAXIMUM_PREDICTED_FRAMES)
//                show_shoulder();
//
//            if (shoulder_skip_frame == 0 || shoulder_skip_frame > MAXIMUM_ALLOWED_SKIPPED_FRAMES)
//                shoulder_frame = ShoulderDetector(mGray);
//        }

        return mRgba;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");
        mItemFace50 = menu.add("Face size 50%");
        mItemFace40 = menu.add("Face size 40%");
        mItemFace30 = menu.add("Face size 30%");
        mItemFace20 = menu.add("Face size 20%");
        // mItemType   = menu.add(mDetectorName[mDetectorType]);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
        if (item == mItemFace50)
            setMinFaceSize(0.5f);
        else if (item == mItemFace40)
            setMinFaceSize(0.4f);
        else if (item == mItemFace30)
            setMinFaceSize(0.3f);
        else if (item == mItemFace20)
            setMinFaceSize(0.2f);
//        else if (item == mItemType) {
//            int tmpDetectorType = (mDetectorType + 1) % mDetectorName.length;
//            item.setTitle(mDetectorName[tmpDetectorType]);
//            setDetectorType(tmpDetectorType);
//        }
        return true;
    }

    private void setMinFaceSize(float faceSize) {
        mRelativeFaceSize = faceSize;
        mAbsoluteFaceSize = 0;
    }

//    private void setDetectorType(int type) {
//        if (mDetectorType != type) {
//            mDetectorType = type;
//
//            if (type == NATIVE_DETECTOR) {
//                Log.i(TAG, "Detection Based Tracker enabled");
//                mNativeDetector.start();
//            } else {
//                Log.i(TAG, "Cascade detector enabled");
//                mNativeDetector.stop();
//            }
//        }
//    }

//    private void FaceAndShoulderDetector (Mat gray) {
//
//        MatOfRect faces = new MatOfRect();
//        MatOfRect eyes = new MatOfRect();
//        MatOfRect shoulders = new MatOfRect();
//
//        if (mDetectorType == JAVA_DETECTOR) {
//            if (mJavaDetector != null)
//                mJavaDetector.detectMultiScale(gray, faces, 1.1, 2, 2, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
//                        new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());
//        }
//        else {
//            Log.e(TAG, "Detection method is not selected!");
//            return;
//        }
//
//        Rect[] facesArray = faces.toArray();
//        for (int i = 0; i < facesArray.length; i++) {
//
//            Mat mFace = gray.submat(facesArray[i]);
//
//            if (mJavaDetectorEye != null) {
//                mJavaDetectorEye.detectMultiScale(mFace, eyes, 1.1, 2,
//                        Objdetect.CASCADE_SCALE_IMAGE, new Size(0, 0), new Size());
//                if (!eyes.empty()) {
//                    Imgproc.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(), FACE_RECT_COLOR, 3);
//                    mJavaDetectorShoulder.detectMultiScale(mGray, shoulders, 1.1, 2,
//                            Objdetect.CASCADE_SCALE_IMAGE, new Size(), new Size());
//                    if (!shoulders.empty()) {
//                        Rect[] shoulderArray = shoulders.toArray();
//                        for (int j =0; j < shoulderArray.length; j++) {
//                            Imgproc.rectangle(mRgba, shoulderArray[j].tl(), shoulderArray[j].br(), FACE_RECT_COLOR, 3);
////                            if ((shoulderArray[j].x < facesArray[i].x) && (facesArray[i].width * 1.2 < shoulderArray[j].width)) {
////
////                                return;
////                            }
//                        }
//                        return;
//                    }
//                }
//            }
//        }
//    }

    private Rect FaceDetector (Mat gray) {

        MatOfRect faces = new MatOfRect();
        MatOfRect eyes = new MatOfRect();

        if (mDetectorType == JAVA_DETECTOR) {
            if (mJavaDetector != null)
                mJavaDetector.detectMultiScale(gray, faces, 1.1, 2, 2, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
                        new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());
        }
        else {
            Log.e(TAG, "Detection method is not selected!");
            return null;
        }

        Rect[] facesArray = faces.toArray();
        for (int i = 0; i < facesArray.length; i++) {

            Mat mFace = gray.submat(facesArray[i]);

            if (mJavaDetectorEye != null) {
                mJavaDetectorEye.detectMultiScale(mFace, eyes, 1.1, 2,
                        Objdetect.CASCADE_SCALE_IMAGE, new Size(0, 0), new Size());
                if (!eyes.empty()) {
                    FaceDetected = true;
                    shoulder_skip_frame = 1;
                    return facesArray[i];
                }
            }
        }

        return null;
    }

    private Rect ShoulderDetector (Mat gray) {

        MatOfRect shoulders = new MatOfRect();
        MatOfRect eyes = new MatOfRect();

        if (mDetectorType == JAVA_DETECTOR) {
            if (mJavaDetectorShoulder != null)
                mJavaDetectorShoulder.detectMultiScale(gray, shoulders, 1.1, 2, 2, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
                        new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());
        }
        else {
            Log.e(TAG, "Detection method is not selected!");
            return null;
        }

        if (!shoulders.empty()) {
            Rect[] shouldersArray = shoulders.toArray();
            for (int i = 0; i < shouldersArray.length; i++) {

//            Imgproc.rectangle(mRgba, shouldersArray[i].tl(), shouldersArray[i].br(), FACE_RECT_COLOR, 3);

                Mat mShoulder = gray.submat(shouldersArray[i]);

                if (mJavaDetectorEye != null) {
                    mJavaDetectorEye.detectMultiScale(mShoulder, eyes, 1.1, 2,
                            Objdetect.CASCADE_SCALE_IMAGE, new Size(0, 0), new Size());
                    if (!eyes.empty()) {
                        ShoulderDetected = true;
                        shoulder_skip_frame = 1;
                        return shouldersArray[i];
                    }
                }
            }
        }

        return null;
    }

    private void show_face() {
        if (face_frame != null) {
            if (face_skip_frame < MAXIMUM_PREDICTED_FRAMES) {
                Imgproc.rectangle(mRgba, face_frame.tl(), face_frame.br(), FACE_RECT_COLOR, thickness);
                face_skip_frame++;
                return;
            } else {
                face_skip_frame = 0;
            }
        } else face_skip_frame = 0;
    }

    private void show_shoulder() {
        if (shoulder_frame != null) {
            if (shoulder_skip_frame < MAXIMUM_PREDICTED_FRAMES) {
                Imgproc.rectangle(mRgba, shoulder_frame.tl(), shoulder_frame.br(), FACE_RECT_COLOR, thickness);
                shoulder_skip_frame++;
            } else {
                shoulder_skip_frame = 0;
            }
        } else shoulder_skip_frame = 0;
    }

}

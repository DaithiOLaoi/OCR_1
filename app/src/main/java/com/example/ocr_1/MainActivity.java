package com.example.ocr_1;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.transition.TransitionManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.IOException;
import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "OCR_MainActivity";

    CameraSource mCameraSource;
    SurfaceView mCameraView;
    TextView mTextView;
    FloatingActionButton mFAB;
    ConstraintLayout mConstraintLayout;
    SeekBar mSeekBar;

    boolean txtHasFocus = false;
    long updateDelay = 0;
    long updateAfter = System.currentTimeMillis();
    boolean update = true;
    DecimalFormat df = new DecimalFormat("#.##");

    int menuHeight = 0;


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Context context = this;

        mCameraView = findViewById(R.id.surfaceView2);
        mTextView = findViewById(R.id.textView3);
        mFAB = findViewById(R.id.floatingActionButton);
        mConstraintLayout = findViewById(R.id.constraintLayout);
        mSeekBar = findViewById(R.id.seekBar2);

        //pause/play detection when user taps anywhere on screen
        mCameraView.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if(txtHasFocus){
                    //TODO: add dialog to check if user wants to return to detection
                }
                txtHasFocus = !txtHasFocus;
            }
        });
        mTextView.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                txtHasFocus = !txtHasFocus;
            }
        });
        mFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //https://developer.android.com/reference/android/support/constraint/ConstraintSet#setMargin(int,%20int,%20int)

                //toggle menuHeight
                if(menuHeight == 0){
                    //not visible, so show
                    menuHeight = 500;
                }else{
                    menuHeight = 0;
                }

                ConstraintSet constraintSet1 = new ConstraintSet();
                constraintSet1.clone(context, R.layout.activity_main);
                ConstraintSet constraintSet2 = new ConstraintSet();
                constraintSet2.clone(context, R.layout.activity_main);
                constraintSet2.setMargin(R.id.surfaceView2, ConstraintSet.BOTTOM, menuHeight);

                TransitionManager.beginDelayedTransition(mConstraintLayout);
                constraintSet2.applyTo(mConstraintLayout);


            }
        });
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateDelay = mSeekBar.getProgress();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Toast.makeText(getApplicationContext(), String.valueOf("Update Delay: " + df.format((double) seekBar.getProgress()/1000)) + "s",Toast.LENGTH_SHORT).show();
            }
        });


        //start detecting
        startCameraSource();
    }


    //https://codelabs.developers.google.com/codelabs/mobile-vision-ocr/#0
    private void startCameraSource() {

        //Create the TextRecognizer
        final TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();

        if (!textRecognizer.isOperational()) {
            Log.w(TAG, "Detector dependencies not loaded yet");
        } else {

            //Initialize camerasource to use high resolution and set Autofocus on.
            mCameraSource = new CameraSource.Builder(getApplicationContext(), textRecognizer)
                    .setFacing(CameraSource.CAMERA_FACING_BACK)
                    //.setRequestedPreviewSize(mCameraView.getHeight(), mCameraView.getWidth())
                    .setAutoFocusEnabled(true)
                    .setRequestedFps(2.0f)
                    .build();

            /*
             * Add call back to SurfaceView and check if camera permission is granted.
             * If permission is granted we can start our cameraSource and pass it to surfaceView
             */
            mCameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    try {

                        if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

                            int requestPermissionID = 0;
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.CAMERA},
                                    requestPermissionID);
                            return;
                        }
                        mCameraSource.start(mCameraView.getHolder());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                }

                /**
                 * Release resources for cameraSource
                 */
                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                    mCameraSource.stop();
                }
            });

            //Set the TextRecognizer's Processor.
            textRecognizer.setProcessor(new Detector.Processor<TextBlock>() {
                @Override
                public void release() {
                }

                /**
                 * Detect all the text from camera using TextBlock and the values into a stringBuilder
                 * which will then be set to the textView.
                 * */
                @Override
                public void receiveDetections(Detector.Detections<TextBlock> detections) {
                    final SparseArray<TextBlock> items = detections.getDetectedItems();
                    if (items.size() != 0 ){

                        mTextView.post(new Runnable() {
                            @Override
                            public void run() {
                                StringBuilder stringBuilder = new StringBuilder();
                                for(int i=0;i<items.size();i++){
                                    TextBlock item = items.valueAt(i);
                                    stringBuilder.append(item.getValue());
                                    stringBuilder.append("\n");
                                }
                                if(!txtHasFocus && System.currentTimeMillis() > updateAfter){
                                    mTextView.setText(stringBuilder.toString());

                                    //update updateAfter
                                    updateAfter = System.currentTimeMillis() + updateDelay;
                                }
                            }
                        });
                    }
                }
            });
        }
    }

    //function to adjust rate of text update
    private int updateRate(int interval, TextView mTextView){

        //milliseconds to wait before updating
        int rateAdJust = 0;

        return rateAdJust;
    };
}

////function to adjust rate of text update
//private class adjustRate(int interval, TextView mTextView){
//
//}
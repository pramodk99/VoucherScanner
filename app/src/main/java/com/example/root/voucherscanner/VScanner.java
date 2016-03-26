package com.example.root.voucherscanner;

import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;
import com.pramod.lib.dialogs.Messages;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import butterknife.Bind;
import butterknife.ButterKnife;

public class VScanner extends AppCompatActivity {


    //surface view for camera image capture
    @Bind(R.id.surface_camera)  SurfaceView surfaceView;
    @Bind(R.id.camera_button)FloatingActionButton captureButton;
    @Bind(R.id.refresh_button)FloatingActionButton refreshButton;

    SurfaceHolder surfaceHolder;
    Camera camera;
    boolean mPreviewRunning = false;
    private boolean cameraConfigured=false;


    final static String TAG = "VScanner";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vscanner);
        ButterKnife.bind(this);

        ///initializing toolbar as actionbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ///initializing surface view
        initSurfaceView();

        ///evend handler
        eventOperator();

    }


    ///event handler
    private void eventOperator()
    {
        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                captureImg();
            }
        });

        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startPreview();
            }
        });
    }


    ///OCR processor method
    private void processOCR(File image)
    {
        TessBaseAPI baseAPI = new TessBaseAPI();
        Log.d("Path", " "+image.getAbsolutePath());
        baseAPI.init("/mnt/sdcard/tesseract/", "eng");
        baseAPI.setImage(image);

        Log.d("Result"," "+baseAPI.getUTF8Text());
        baseAPI.end();
    }
    ///initializing surfacr view meethod
    private void initSurfaceView()
    {
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {

            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                initPreview(width, height);
                startPreview();
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
            }
        });
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }


    private File captureImg()
    {
        final File image = new File(Environment.getExternalStorageDirectory(),"ocr_photo.jpg");
        try {
            image.createNewFile();
            Log.d(TAG, "New File created at "+ image.getAbsolutePath());
        } catch (IOException e) {
            Log.d(TAG,"New File "+e.getMessage());
        }
        camera.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {

                if (image.exists())
                    image.delete();

                try {
                    FileOutputStream fileOutputStream = new FileOutputStream(image);
                    fileOutputStream.write(data);
                    fileOutputStream.close();

                } catch (FileNotFoundException e) {
                    Log.d(TAG, " " + e.getMessage());
                } catch (IOException e) {
                    Log.d(TAG, " " + e.getMessage());
                }

                processOCR(image);
            }
        });

        return image;
    }


    private void initPreview(int width, int height) {
        if (camera!=null && surfaceHolder.getSurface()!=null) {
            try {
                camera.setPreviewDisplay(surfaceHolder);
            }
            catch (Throwable t) {
                Log.e(TAG, "Exception in setPreviewDisplay()", t);
                Messages.showToastShortDuration(VScanner.this, t.getMessage());
            }
            if (!cameraConfigured) {
                Camera.Parameters parameters=camera.getParameters();
                Camera.Size size=getBestPreviewSize(width, height,
                        parameters);
                if (size!=null) {
                    parameters.setPreviewSize(size.width, size.height);
                    camera.setParameters(parameters);
                    cameraConfigured=true;
                }
            }
        }
    }

    private Camera.Size getBestPreviewSize(int width, int height,
                                           Camera.Parameters parameters) {
        Camera.Size result=null;
        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            if (size.width<=width && size.height<=height) {
                if (result==null) {
                    result=size;
                }
                else {
                    int resultArea=result.width*result.height;
                    int newArea=size.width*size.height;
                    if (newArea>resultArea) {
                        result=size;
                    }
                }
            }
        }
        return(result);
    }

    private void startPreview() {
        if (cameraConfigured && camera!=null) {
            camera.startPreview();
            mPreviewRunning =true;
        }
    }
    @Override
    public void onResume() {
        super.onResume();
        camera = Camera.open();
        startPreview();
    }

    @Override
    public void onPause() {
        if (mPreviewRunning) {
            camera.stopPreview();
        }
        camera.release();
        camera=null;
        mPreviewRunning=false;
        super.onPause();
    }
}

package com.example.root.voucherscanner;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;


public class MyActivity extends Activity {

    final private int PICK_IMAGE = 1;
    final private int CAPTURE_IMAGE = 2;
    final private String TAG = "MyActivity";

    private String selectedImagePath = "";

    Button camera;
    ImageView imageView;
    TextView textView;
    String imgPath = "";
    SurfaceView surfaceView ;
    SurfaceHolder surfaceHolder;

    Camera.PictureCallback rawCallback;
    Camera.ShutterCallback shutterCallback;
    Camera.PictureCallback jpegCallback;
    Camera camera_;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);

        camera  = (Button) findViewById(R.id.camera);
        imageView = (ImageView) findViewById(R.id.imageView);
        textView = (TextView) findViewById(R.id.value);

        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//                intent.putExtra(MediaStore.EXTRA_OUTPUT, setImageUri());
//                startActivityForResult(intent, CAPTURE_IMAGE);

                try {
                    captureImage(view);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });


        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        surfaceHolder = surfaceView.getHolder();

        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {

                try {
                    camera_ = Camera.open();
                }
                catch (RuntimeException e) {
                    System.err.println(e);
                    return;
                }

                Camera.Parameters parameters;
                parameters = camera_.getParameters();

                parameters.setPreviewSize(352, 288);
                camera_.setParameters(parameters);

                try {
                    camera_.setPreviewDisplay(surfaceHolder);
                    camera_.startPreview();
                }catch (Exception e)
                {
                    System.err.println(e);
                    return;
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
                refreshCamera();
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                camera_.stopPreview();
                camera_.release();
                camera_ = null;
            }
        });

        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_CANCELED) {
            if (requestCode == PICK_IMAGE) {
                selectedImagePath = getAbsolutePath(data.getData());
                imageView.setImageBitmap(decodeFile(selectedImagePath));
            } else if (requestCode == CAPTURE_IMAGE) {
                selectedImagePath = getImagePath();
                imageView.setImageBitmap(decodeFile(selectedImagePath));
                Log.d(TAG,"Path :"+selectedImagePath);

                processOCR(selectedImagePath);


            } else {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    private void processOCR(String path)
    {
        TessBaseAPI baseAPI = new TessBaseAPI();
        baseAPI.init(path,"eng");

        textView.setText(" "+baseAPI.getUTF8Text());

        baseAPI.end();
    }

    public void captureImage(View  v)throws IOException
    {
        camera_.takePicture(null,null,new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] bytes, Camera camera) {
                FileOutputStream fileOutputStream = null;

                try
                {
                    fileOutputStream = new FileOutputStream(String.format("/sdcard/%d.jpg", System.currentTimeMillis()));
                    fileOutputStream.write(bytes);
                    fileOutputStream.close();
                }
                catch (FileNotFoundException e){
                    e.printStackTrace();}
                catch (IOException e)
                {
                    e.printStackTrace();
                }
                finally {
                    Toast.makeText(getApplicationContext(), "Picture Saved", Toast.LENGTH_SHORT).show();
                    refreshCamera();
                }
            }
        });
    }

    public void refreshCamera()
    {
        if(surfaceHolder.getSurface()==null)
            return;

        try
        {
            camera_.stopPreview();
        }
        catch (Exception e)
        {

        }

        try
        {
            camera_.setPreviewDisplay(surfaceHolder);
            camera_.startPreview();
        }catch (Exception e)
        {

        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my, menu);
        return true;
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



    public Uri setImageUri() {
        // Store image in dcim
        File file = new File(Environment.getExternalStorageDirectory() + "/DCIM/", "image" + new Date().getTime() + ".png");
        Uri imgUri = Uri.fromFile(file);
        this.imgPath = file.getAbsolutePath();
        return imgUri;
    }

    public String getImagePath() {
        return imgPath;
    }

    public String getAbsolutePath(Uri uri) {
        String[] projection = { MediaStore.MediaColumns.DATA };
        @SuppressWarnings("deprecation")
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        if (cursor != null) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } else
            return null;
    }

    public Bitmap decodeFile(String path) {
        try {
            // Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, o);
            // The new size we want to scale to
            final int REQUIRED_SIZE = 70;

            // Find the correct scale value. It should be the power of 2.
            int scale = 1;
            while (o.outWidth / scale / 2 >= REQUIRED_SIZE && o.outHeight / scale / 2 >= REQUIRED_SIZE)
                scale *= 2;

            // Decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            return BitmapFactory.decodeFile(path, o2);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;

    }

}

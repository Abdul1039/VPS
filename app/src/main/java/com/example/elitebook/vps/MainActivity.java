package com.example.elitebook.vps;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import it.sephiroth.android.library.exif2.ExifInterface;

public class MainActivity extends AppCompatActivity {
    
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_TAKE_PHOTO = 1;
    private static final int CAMERA_REQUEST_CODE = 1;
    private static String LABEL;
    String currentPhotoPath;
    File photoFile = null;
    byte[] bytes;
    private Button captureImage, uploadImage;
    private EditText getLabel;
    private ImageView mImageView;
    private FirebaseStorage Storage;
    private StorageReference mStorageRef;
    private ProgressDialog mProgress;
    private Uri photoURI;
    private StorageReference filepath;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA,
                    Manifest.permission.LOCATION_HARDWARE}, 112);
        }
        mStorageRef = FirebaseStorage.getInstance().getReference();
        mProgress = new ProgressDialog(this);
        
        getLabel = findViewById(R.id.getLabel);
        mImageView = findViewById(R.id.imageView);
        captureImage = findViewById(R.id.capture);
        uploadImage = findViewById(R.id.upload);
        
        uploadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                
                LABEL = getLabel.getText().toString();
                
                
                mProgress.setMessage("Uploading Image");
                mProgress.show();
                
                if (!LABEL.equals(""))
                    filepath = mStorageRef.child("Photos").child(LABEL).child(String.valueOf(new Date(System.currentTimeMillis())));
                else filepath =
                        mStorageRef.child("Photos").child("Default").child(String.valueOf(new Date(System.currentTimeMillis())));
                
                filepath.putFile(photoURI).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        
                        mProgress.dismiss();
                        
                        Toast.makeText(MainActivity.this, "Uploading Finished", Toast.LENGTH_LONG).show();
                        
                    }
                });
                
            }
            
        });
        
        captureImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

//                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//                startActivityForResult(intent, CAMERA_REQUEST_CODE);
                dispatchTakePictureIntent();
                
                
            }
        });
    }
    
    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(currentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }
    
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(this, getPackageName(),
                        photoFile);
                Log.e("PATH", photoFile.getAbsolutePath());
                
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }
    
    public Location getLocation() {
        // Get the location manager
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        Criteria criteria = new Criteria();
        String bestProvider = locationManager.getBestProvider(criteria, false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            
            } else {
                Location location = locationManager.getLastKnownLocation(bestProvider);
                double lat, lon;
                try {
                    lat = location.getLatitude();
                    lon = location.getLongitude();
                    return location;
                } catch (NullPointerException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        }
        return null;
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            
            
            try {
                writeGPS();
                galleryAddPic();
            } catch (IOException e) {
                
                Log.e("Check", e.getMessage());
                e.printStackTrace();
            }
            
            mImageView.setImageURI(photoURI);
            
            
        }
    }
    
    private void writeGPS() throws IOException {
        ExifInterface exif = new ExifInterface();
        exif.readExif(photoFile.getAbsolutePath(), ExifInterface.Options.OPTION_ALL);
        
        Location location = getLocation();
        exif.addGpsTags(location.getLatitude(), location.getLongitude());
        exif.addGpsDateTimeStampTag(System.currentTimeMillis());
        exif.buildTag(1, LABEL);
        exif.writeExif(photoFile.getAbsolutePath(), photoFile.getAbsolutePath());
        bytes = exif.getThumbnailBytes();
        
        
        Log.e("LatLong", exif.getLatLongAsDoubles()[0] + " : " + exif.getLatLongAsDoubles()[1]);
        
        
    }
    
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpeg",         /* suffix */
                storageDir      /* directory */
        );
        
        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }
    
}

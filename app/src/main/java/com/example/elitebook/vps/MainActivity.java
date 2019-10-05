package com.example.elitebook.vps;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.github.amlcurran.showcaseview.OnShowcaseEventListener;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.Target;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import it.sephiroth.android.library.exif2.ExifInterface;

public class MainActivity extends AppCompatActivity {
    
    private static final int REQUEST_TAKE_PHOTO = 1;
    private static String LABEL;
    private LocationManager manager;
    private String currentPhotoPath;
    private File photoFile = null;
    private EditText getLabel, getLabel1;
    private ImageView mImageView;
    private StorageReference mStorageRef;
    private ProgressDialog mProgress;
    private Uri photoURI;
    private StorageReference filepath;
    private ShowcaseView.Builder builder1, builder2, builder3;
    private SharedPreferences sp;
    private SharedPreferences.Editor editor;
    private Spinner getSaveLabels;
    private List<String> list;
    private ArrayAdapter<String> adapter;
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        
        //create firebase instance
        mStorageRef = FirebaseStorage.getInstance().getReference();
        mProgress = new ProgressDialog(this);
        
        sp = getSharedPreferences("LABELS", MODE_PRIVATE);
        
        manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        
        getLabel = findViewById(R.id.getLabel);
        getLabel1 = findViewById(R.id.getLabel1);
        
        mImageView = findViewById(R.id.imageView);
        
        Button captureImage = findViewById(R.id.capture);
        Button uploadImage = findViewById(R.id.upload);
        
        getSaveLabels = findViewById(R.id.getSaveLabels);
        
        list = new ArrayList<>();
        
        list.add("Default");
        
        list.addAll(sp.getAll().keySet());
        
        
        adapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_spinner_dropdown_item, list);
        getSaveLabels.setAdapter(adapter);
        
        
        //Select label path from dropdown list(Spinner)
        getSaveLabels.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.e("Spinner 2", parent.getAdapter().getItem(position).toString());
                
                LABEL = parent.getAdapter().getItem(position).toString();
                
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            
            }
        });
        
        
        //Upload image to firebase onButton Click
        uploadImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                
                LABEL = getLabel.getText().toString();
                editor = sp.edit();
                if (!sp.contains(LABEL) && !LABEL.equals("")) {
                    editor.putString(LABEL, LABEL);
                    editor.apply();
                    
                    list.clear();
                    list.add("Default");
                    list.addAll(sp.getAll().keySet());
                    adapter = new ArrayAdapter<>(MainActivity.this,
                            android.R.layout.simple_spinner_dropdown_item,
                            list);
                    getSaveLabels.setAdapter(adapter);
                }
                
                
                if (photoURI == null) {
                    
                    Toast.makeText(MainActivity.this, "Capture Image First", Toast.LENGTH_SHORT).show();
                    return;
                }
                
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
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this, "Failed to upload", Toast.LENGTH_LONG).show();
                        mProgress.dismiss();
                    }
                });
            }
            
        });
        
        
        //Capture image onButton Click
        captureImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                
                
                //request permission
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            
                            Manifest.permission.CAMERA}, 112);
                } else {
                    
                    //check gps settings enable or not
                    if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER) || !manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
                        buildAlertMessageNoGps();
                    else //take picture
                        dispatchTakePictureIntent();
                }
                
                
            }
        });
        
        
        //when user enter first time we will display a user guide
        
        showCaseDisplay();
        if (!builder1.build().isShowing()) {
            getLabel.setVisibility(View.VISIBLE);
            getLabel1.setVisibility(View.GONE);
        }
        
    }
    
    private void showCaseDisplay() {
        
        Target viewTarget2 = new ViewTarget(R.id.getLabel1, this);
        Target viewTarget3 = new ViewTarget(R.id.capture, this);
        Target viewTarget4 = new ViewTarget(R.id.upload, this);
        
        
        builder1 = new ShowcaseView.Builder(this);
        
        builder2 = new ShowcaseView.Builder(this);
        builder3 = new ShowcaseView.Builder(this);
        
        
        builder1.setTarget(viewTarget2)
                .setStyle(R.style.CustomShowcaseTheme3)
                .withMaterialShowcase()
                .setContentTitle("Label")
                .setContentText("Set your label for image")
                .hideOnTouchOutside().singleShot(1);
        
        
        builder2.setTarget(viewTarget3)
                .setStyle(R.style.CustomShowcaseTheme3)
                .withMaterialShowcase()
                .setContentTitle("Capture Image")
                .setContentText("Take your image from this button")
                .hideOnTouchOutside().singleShot(2);
        
        
        builder3.setTarget(viewTarget4)
                .setStyle(R.style.CustomShowcaseTheme3)
                .withMaterialShowcase()
                .setContentTitle("Upload")
                .setContentText("Save your image to our cloud")
                .hideOnTouchOutside().singleShot(3);
        
        builder1.setShowcaseEventListener(new OnShowcaseEventListener() {
            @Override
            public void onShowcaseViewHide(ShowcaseView showcaseView) {
                
                getLabel1.setVisibility(View.GONE);
                getLabel.setVisibility(View.VISIBLE);
                builder2.build();
                
            }
            
            @Override
            public void onShowcaseViewDidHide(ShowcaseView showcaseView) {
            
            }
            
            @Override
            public void onShowcaseViewShow(ShowcaseView showcaseView) {
                
                
                getLabel1.setVisibility(View.VISIBLE);
                getLabel.setVisibility(View.GONE);
            }
            
            @Override
            public void onShowcaseViewTouchBlocked(MotionEvent motionEvent) {
            
            }
        });
        builder2.setShowcaseEventListener(new OnShowcaseEventListener() {
            @Override
            public void onShowcaseViewHide(ShowcaseView showcaseView) {
                builder3.build();
            }
            
            @Override
            public void onShowcaseViewDidHide(ShowcaseView showcaseView) {
            
            }
            
            @Override
            public void onShowcaseViewShow(ShowcaseView showcaseView) {
            
            }
            
            @Override
            public void onShowcaseViewTouchBlocked(MotionEvent motionEvent) {
            
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
                photoURI = FileProvider.getUriForFile(this, getPackageName(), photoFile);
                
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }
    
    private Location getLocation() {
        // Get the location manager
        
        Criteria criteria = new Criteria();
        String bestProvider = manager.getBestProvider(criteria, false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Location Permission Required", Toast.LENGTH_SHORT).show();
            } else {
                Location location = manager.getLastKnownLocation(bestProvider);
                
                try {
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
                
                //for saving gps location in image
                writeGPS();
                //add capture image in gallery
                galleryAddPic();
            } catch (IOException e) {
                Log.e("Check", e.getMessage());
                e.printStackTrace();
            }
            
            //display captured image
            mImageView.setImageURI(photoURI);
        }else photoURI = null;
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        Log.e("Permissions", Arrays.toString(permissions) + " : " + Arrays.toString(grantResults));
        if (grantResults[0] == 0 && grantResults[1] == 0 && grantResults[2] == 0) {
            
            //check gps settings enable or not
            if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER) || !manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
                buildAlertMessageNoGps();
            else //take picture
                dispatchTakePictureIntent();
        }
    }
    
    private void writeGPS() throws IOException {
        ExifInterface exif = new ExifInterface();
        exif.readExif(photoFile.getAbsolutePath(), ExifInterface.Options.OPTION_ALL);
        
        Location location = getLocation();
        if (location != null)
            exif.addGpsTags(location.getLatitude(), location.getLongitude());
        else exif.addGpsTags(0, 0);
        exif.addGpsDateTimeStampTag(System.currentTimeMillis());
        exif.buildTag(1, LABEL);
        exif.writeExif(photoFile.getAbsolutePath(), photoFile.getAbsolutePath());
    }
    
    
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
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
    
    private void buildAlertMessageNoGps() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, enable it to capture image")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }
    
}

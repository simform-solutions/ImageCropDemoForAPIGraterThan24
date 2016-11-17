package com.example.imagecropforapi24;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Context context;
    private static final int SELECT_PICTURE_CAMARA = 101, SELECT_PICTURE = 201;
    private Uri outputFileUri;
    //    private Uri selectedImageUri;
    private ImageView imageView;
    private PermissionUtil permissionUtil;
    private File photoFile;
    private String TAG = MainActivity.class.getSimpleName();


    /**
     * Add Ucrop Gradle Refer From: https://github.com/Yalantis/uCrop
     * For File Provider:
     * <p>
     * https://developer.android.com/reference/android/support/v4/content/FileProvider.html
     **/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = this;

        Button mBtn = (Button) findViewById(R.id.btn_img);
        imageView = (ImageView) findViewById(R.id.img_photo);

        permissionUtil = new PermissionUtil();
        mBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
//        Buttone Click
        selectImageOption();
    }

    private void selectImageOption() {

        final CharSequence[] items = {"Capture Photo", "Choose from Gallery", "Cancel"};

        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Add Photo!");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (items[item].equals("Capture Photo")) {
                   /* askForPermission(Manifest.permission.CAMERA,SELECT_PICTURE_CAMARA);*/

                    if (permissionUtil.checkMarshMellowPermission()) {
                        if (permissionUtil.verifyPermissions(MainActivity.this, permissionUtil.getCameraPermissions()))
                            onClickCamera();
                        else
                            ActivityCompat.requestPermissions(MainActivity.this, permissionUtil.getCameraPermissions(), SELECT_PICTURE_CAMARA);
                    } else
                        onClickCamera();
                } else if (items[item].equals("Choose from Gallery")) {
                    if (permissionUtil.checkMarshMellowPermission()) {
                        if (permissionUtil.verifyPermissions(MainActivity.this, permissionUtil.getGalleryPermissions()))
                            onClickGallery();
                        else
                            ActivityCompat.requestPermissions(MainActivity.this, permissionUtil.getGalleryPermissions(), SELECT_PICTURE);
                    } else
                        onClickGallery();
                } else if (items[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {

            if (requestCode == SELECT_PICTURE_CAMARA) {
                Log.v(TAG, "onActivityResult:Camera " + outputFileUri);
                sendImageFroCrop(outputFileUri);

            } else if (requestCode == UCrop.REQUEST_CROP) {
                Log.v(TAG, "onActivityResult:Crop " + outputFileUri);
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), outputFileUri);
                    imageView.setImageBitmap(bitmap);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (requestCode == SELECT_PICTURE) {
                outputFileUri = data.getData();
                Log.v(TAG, "onActivityResult:Gallery " + outputFileUri);
                sendImageFroCrop(outputFileUri);
            }
        }
    }

    private void onClickCamera() {

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (takePictureIntent.resolveActivity(context.getPackageManager()) != null) {

            try {
                photoFile = createImageFile();
            } catch (IOException ignored) {
            }
            if (photoFile != null) {
                Uri photoURI;

                if (Build.VERSION.SDK_INT >= 24)
                    photoURI = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", photoFile);
                else
                    photoURI = Uri.fromFile(photoFile);

                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);

                startActivityForResult(takePictureIntent, SELECT_PICTURE_CAMARA);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";

        File storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        outputFileUri = Uri.fromFile(image);

        return image;
    }

    private void sendImageFroCrop(Uri selectedImageUri) {

        UCrop.Options options = new UCrop.Options();
        options.setToolbarColor(ContextCompat.getColor(context, R.color.colorPrimary));

        options.setToolbarWidgetColor(ContextCompat.getColor(context, R.color.colorAccent));
        options.setStatusBarColor(ContextCompat.getColor(context, R.color.colorAccent));
        options.setToolbarTitle(" ");
        options.setHideBottomControls(true);
        options.setActiveWidgetColor(ContextCompat.getColor(context, R.color.colorPrimary));

        try {
            outputFileUri = Uri.fromFile(createImageFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
// Source Destination
        UCrop.of(selectedImageUri, outputFileUri)
                .withAspectRatio(16, 9)
                .withOptions(options)
                .start(this);
    }

    private void onClickGallery() {
        List<Intent> targets = new ArrayList<>();
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_PICK);
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        List<ResolveInfo> candidates = getApplicationContext().getPackageManager().queryIntentActivities(intent, 0);

        for (ResolveInfo candidate : candidates) {
            String packageName = candidate.activityInfo.packageName;
            if (!packageName.equals("com.google.android.apps.photos") && !packageName.equals("com.google.android.apps.plus") && !packageName.equals("com.android.documentsui")) {
                Intent iWantThis = new Intent();
                iWantThis.setType("image/*");
                iWantThis.setAction(Intent.ACTION_PICK);
                iWantThis.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                iWantThis.setPackage(packageName);
                targets.add(iWantThis);
            }
        }
        if (targets.size() > 0) {
            Intent chooser = Intent.createChooser(targets.remove(0), "Select Picture");
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, targets.toArray(new Parcelable[targets.size()]));
            startActivityForResult(chooser, SELECT_PICTURE);
        } else {
            Intent intent1 = new Intent(Intent.ACTION_PICK);
            intent1.setType("image/*");
            startActivityForResult(Intent.createChooser(intent1, "Select Picture"), SELECT_PICTURE);
        }

    }

}

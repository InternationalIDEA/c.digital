package com.rezapps.cdigital;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class Util {

    private static final String TAG = "CDigital";
    private static final int JPEG_COMPRESSION_QUALITY = 80 ;

    public static Uri generatePhotoUri(Context context, String fileName) {

        File dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File photoFile = new File(dir, fileName);
        //File photoFile = getPhotoFile(context, fileName);
        if (photoFile == null) return null;
        return FileProvider.getUriForFile(context,
                BuildConfig.APPLICATION_ID + ".provider",
                photoFile);
    }

//    @Nullable
//    public static File getPhotoFile(Context context, String fileName) {
//        File mediaDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
//                "Cdigital");
//
//        if (!mediaDir.exists()) {
//            // if Directory not exist, create one
//            if (!mediaDir.mkdirs()) {
//                Log.e(context.getPackageName(), "failed to create directory " + mediaDir);
//                return null;
//            }
//        }
//
//        File photoFile = new File(mediaDir, fileName);
//        return photoFile;
//    }

    public static String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return first + s.substring(1);
        }
    }

    public static byte[] preprocessPhoto(Bitmap photo) {
        // rotate clockwise if photo is landscape
        Bitmap photo2;
        if (photo.getWidth() > photo.getHeight()) {
            Matrix matrix = new Matrix();
            matrix.postRotate(90f);

            photo2 = Bitmap.createBitmap(
                    photo,0,0,photo.getWidth(),photo.getHeight(),matrix,false);
        } else {
            photo2 = photo;
        }

        // scale to 1800 width, maintain aspect ratio
        int dstW = 1800;
        int dstH = 1800 * photo2.getHeight() / photo2.getWidth();
        Bitmap scaled = Bitmap.createScaledBitmap(photo2, dstW, dstH, true);

        // compress to JPEG
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_COMPRESSION_QUALITY, baos);
        byte[] photoBA = baos.toByteArray();
        return photoBA;
    }


    public static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;

        if (model.toLowerCase().startsWith(manufacturer.toLowerCase())) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + " " + model;
        }
    }

    @SuppressLint("Range")
    public static String getFileNameFromUri(Context context, Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }


    public static Bitmap loadBitmapFromUri(Context context, Uri photoUri) {
        Bitmap photo = null;

        try {
            photo = MediaStore.Images.Media.getBitmap(context.getContentResolver(), photoUri);
            ExifInterface exif = new ExifInterface(context.getContentResolver().openInputStream(photoUri));
            int orientation = exif.getAttributeInt("Orientation", 1);
            switch (orientation) {
                case 3:
                    photo = Util.rotate(photo, 180.0F);
                    break;
                case 6:
                    photo = Util.rotate(photo, 90.0F);
                    break;
                case 8:
                    photo = Util.rotate(photo, 270.0F);
            }
        } catch (IOException var5) {
            Log.e("CPlanoPWPReader", "Unable to load photo: " + var5.getMessage(), var5);
            var5.printStackTrace();
        }

        return photo;
    }

    public static Bitmap rotate(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

}

package com.oktilyon.unitymediapicker;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;

import com.unity3d.player.UnityPlayer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class Picker extends Fragment {
    private static final String TAG = "unitymediapicker";
    private static final int REQUEST_CODE = 1;
    private static final int RESULT_LOAD_IMAGE = 1333;
    private static final int RESULT_TAKE_NEW = 1337;

    private static int RESULT_CURRENT;

    private static final String CALLBACK_OBJECT = "UnityMediaPicker";
    private static final String CALLBACK_METHOD = "OnComplete";
    private static final String CALLBACK_METHOD_FAILURE = "OnFailure";

    private String mTitle;
    private String mOutputFileName;
    private int mMaxSize;

    public static void show(String title, String outputFileName, int maxSize) {
        Activity unityActivity = UnityPlayer.currentActivity;
        if (unityActivity == null) {
            Picker.NotifyFailure("Failed to open the picker");
            return;
        }

        Picker picker = new Picker();
        picker.mTitle = title;
        picker.mOutputFileName = outputFileName;
        picker.mMaxSize = maxSize;

        RESULT_CURRENT = RESULT_LOAD_IMAGE;

        FragmentTransaction transaction = unityActivity.getFragmentManager().beginTransaction();

        transaction.add(picker, TAG);
        transaction.commit();
    }

    public static void capture(String title, String outputFileName, int maxSize) {
        Activity unityActivity = UnityPlayer.currentActivity;
        if (unityActivity == null) {
            Picker.NotifyFailure("Failed to open the picker");
            return;
        }

        Picker picker = new Picker();
        picker.mTitle = title;
        picker.mOutputFileName = outputFileName;
        picker.mMaxSize = maxSize;

        RESULT_CURRENT = RESULT_TAKE_NEW;

        FragmentTransaction transaction = unityActivity.getFragmentManager().beginTransaction();

        transaction.add(picker, TAG);
        transaction.commit();
    }

    public static void NotifySuccess(String path) {
        UnityPlayer.UnitySendMessage(CALLBACK_OBJECT, CALLBACK_METHOD, path);
    }

    public static void NotifyFailure(String cause) {
        UnityPlayer.UnitySendMessage(CALLBACK_OBJECT, CALLBACK_METHOD_FAILURE, cause);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (RESULT_CURRENT == RESULT_LOAD_IMAGE) {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, mTitle), RESULT_LOAD_IMAGE);
        }else if (RESULT_CURRENT == RESULT_TAKE_NEW){
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(Intent.createChooser(intent, mTitle), RESULT_TAKE_NEW);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != REQUEST_CODE && requestCode != RESULT_LOAD_IMAGE && requestCode != RESULT_TAKE_NEW) {
            return;
        }

        FragmentTransaction transaction = getActivity().getFragmentManager().beginTransaction();
        transaction.remove(this);
        transaction.commit();

        if (resultCode != Activity.RESULT_OK || data == null) {
            Picker.NotifyFailure("Failed to pick the image");
            return;
        }

        Uri uri = data.getData();
        Context context = getActivity().getApplicationContext();

        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);

            // Decode metadata
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, opts);
            inputStream.close();

            // Calc size
            float scaleX = Math.min((float)mMaxSize / opts.outWidth, 1.0f);
            float scaleY = Math.min((float)mMaxSize / opts.outHeight, 1.0f);
            float scale = Math.min(scaleX, scaleY);

            float width = opts.outWidth * scale;
            float height = opts.outHeight * scale;

            // Decode image roughly
            inputStream = context.getContentResolver().openInputStream(uri);
            opts = new BitmapFactory.Options();
            opts.inSampleSize = (int)(1.0f / scale);
            Bitmap roughImage = BitmapFactory.decodeStream(inputStream, null, opts);

            //ExifInterface exif = new ExifInterface(
            int Rotation = ImageOrientationUtil.getExifRotation(ImageOrientationUtil.getFromMediaUri(context, context.getContentResolver(), uri));

            Bitmap image;
            if (Rotation != 0){
                Matrix matrix = new Matrix();
                matrix.postRotate(Rotation);

                image = Bitmap.createBitmap(roughImage, 0, 0, (int) width, (int) height, matrix, true);
            }else {
                image = Bitmap.createScaledBitmap(roughImage, (int) width, (int) height, true);
            }

            // Resize image exactly

            // Output image
            FileOutputStream outputStream = context.openFileOutput(mOutputFileName, Context.MODE_PRIVATE);
            image.compress(Bitmap.CompressFormat.PNG, 50, outputStream);

            outputStream.close();
            inputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Picker.NotifyFailure("Failed to find the image");
            return;
        } catch (IOException e) {
            e.printStackTrace();
            Picker.NotifyFailure("Failed to copy the image");
            return;
        }

        File output = context.getFileStreamPath(mOutputFileName);
        Picker.NotifySuccess(output.getPath());
    }
}

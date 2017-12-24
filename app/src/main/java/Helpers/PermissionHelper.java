package Helpers;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by Ofek on 21-Dec-17.
 */

public class PermissionHelper {
    public static final int REQUEST_WRITE_EXTERNAL_STORAGE = 1111;
    public static final int REQUEST_CAMERA = 46546;
    public static final int REQUEST_BOTH = 78;


    public static boolean getExternalStoragePermission(AppCompatActivity activity){
        if (ActivityCompat.checkSelfPermission(activity,Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(activity,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},REQUEST_WRITE_EXTERNAL_STORAGE);
            return false;
        }
        else {
            return true;
        }
    }
    public static boolean getCameraPermission(AppCompatActivity activity){
        if (ActivityCompat.checkSelfPermission(activity,Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(activity,new String[]{Manifest.permission.CAMERA,Manifest.permission.CAMERA},REQUEST_BOTH);
            return false;
        }
        else {
            return true;
        }
    }

    public static boolean getBothPermission(Activity activity){

        if (ActivityCompat.checkSelfPermission(activity,Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(activity,new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE},REQUEST_CAMERA);
            return false;
        }
        else {
            return true;
        }

    }
}

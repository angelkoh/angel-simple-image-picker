package angel.androidapps.imagepicker.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

// * Created by Angel on 12/23/2019 3:34 PM.  
// * Originally created for project "ContinuousLineArt".
// * Copyright (c) 2019 Angel. All rights reserved. 

object StoragePermissionHandler {

    //STORAGE PERMISSIONS
    //===================
    fun requireStoragePermission(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 &&
                Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
    }

    fun hasStoragePermission(context: Context): Boolean {
        return hasPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                && hasPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }


    fun isStoragePermissionPermanentlyDenied(activity: Activity): Boolean {
        return !hasStoragePermission(activity) &&
                (!ActivityCompat.shouldShowRequestPermissionRationale(
                    activity, Manifest.permission.READ_EXTERNAL_STORAGE
                ) ||
                        !ActivityCompat.shouldShowRequestPermissionRationale(
                            activity,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ))
    }


    fun requestIfNeededStoragePermission(activity: Activity, code: Int): Boolean {
        return if ( hasStoragePermission(activity)) {
            true
        } else {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                code
            )
            false
        }
    }

    private fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }


}
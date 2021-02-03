@file:Suppress("unused")

package angel.androidapps.imagepicker

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.annotation.Keep
import androidx.fragment.app.Fragment

// * Created by Angel on 3/11/2020 1:34 PM.  
// * Originally created for project "SAF test".
// * Copyright (c) 2020 Angel. All rights reserved. 

/**
 * camera and image picker
 */
@Keep
class ImagePicker {

    companion object {


//        private const val TAG = "Angel: ImagePicker"
//        private fun print(s: String) = Log.d(TAG, s)

        fun with(activity: Activity): Builder {
            return Builder(activity)
        }

        fun with(fragment: Fragment): Builder {
            return Builder(fragment)
        }


        private fun getUri(data: Intent?) = data?.data

        fun getError(data: Intent?) = BundleHelper.getError(data)

        fun getUriAndLastModifiedDate(
            data: Intent?, callback: (uri: Uri?, lastModifiedDate: Long) -> Unit
        ) {
            callback.invoke(getUri(data), BundleHelper.getFileLastModifiedDate(data))
        }

    }

    class Builder(private val activity: Activity) {
        private var fragment: Fragment? = null

        private val bundleHelper = BundleHelper()

        constructor(fragment: Fragment) : this(fragment.requireActivity()) {
            this.fragment = fragment
        }

        fun pickFromCamera(
            folderName: String = "",
            fileName: String = "",
            replaceIfFileExist: Boolean = false
        ): Builder {
            bundleHelper.pickFromCamera(folderName, fileName, replaceIfFileExist)
            return this
        }

        fun withCrop(destUri: Uri): Builder {
            bundleHelper.withCrop(destUri)
            return this
        }

        fun withMaxResultSize(width: Int, height: Int): Builder {
            bundleHelper.withMaxResultSize(width, height)
            return this
        }

        fun withAspectRatio(x: Float, y: Float): Builder {
            bundleHelper.withAspectRatio(x, y)
            return this
        }


        fun start(reqCode: Int) {

            val intent = Intent(activity, ImagePickerActivity::class.java)

            intent.putExtras(bundleHelper.bundle)

            if (fragment != null) {
                fragment?.startActivityForResult(intent, reqCode)
            } else {
                activity.startActivityForResult(intent, reqCode)
            }
        }
    }
}
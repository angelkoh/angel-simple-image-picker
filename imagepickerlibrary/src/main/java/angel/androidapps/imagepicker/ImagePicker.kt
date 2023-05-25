@file:Suppress("unused")

package angel.androidapps.imagepicker

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.annotation.Keep
import androidx.fragment.app.Fragment
import java.util.*

// * Created by Angel on 3/11/2020 1:34 PM.  
// * Originally created for project "SAF test".
// * Copyright (c) 2020 Angel. All rights reserved. 

/**
 * camera and image picker
 */
@Keep
class ImagePicker {

    @Keep
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

        fun getLastModifiedDate(data: Intent?) = BundleHelper.getFileLastModifiedDate(data)

        fun getUris(data: Intent?): List<Uri> {
            return when {
                data == null -> emptyList()

                data.clipData != null -> {
                    val result = ArrayList<Uri>()
                    data.clipData?.let { clipData ->
                        val items = clipData.itemCount
                        for (i in 0 until items) {
                            clipData.getItemAt(i)?.uri?.let { result.add(it) }
                        }
                    }
                    result
                }
                else -> {
                    val uri = getUri(data)
                    if (uri == null) emptyList() else listOf(uri)
                }
            }
        }


        fun getMimeType(context: Context, uri: Uri?): String {
            return uri?.let {
                val mimeType = if (ContentResolver.SCHEME_CONTENT == uri.scheme) {
                    val cr: ContentResolver = context.contentResolver
                    cr.getType(uri)
                } else {
                    val fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
                    MimeTypeMap.getSingleton()
                        .getMimeTypeFromExtension(fileExtension.toLowerCase(Locale.ROOT))
                }
                mimeType
            } ?: ""
        }

    }

    @Keep
    class Builder(private val activity: Activity) {
        private var fragment: Fragment? = null

        private val bundleHelper = BundleHelper()

        constructor(fragment: Fragment) : this(fragment.requireActivity()) {
            this.fragment = fragment
        }

        fun pickVideoFromCamera():Builder{
            bundleHelper.pickVideoFromCamera()
            return this
        }
        fun pickVideoFromCamera(
            folderName: String = "",
            fileName: String = "",
            replaceIfFileExist: Boolean = false
        ): Builder {
            bundleHelper.pickVideoFromCamera(folderName, fileName, replaceIfFileExist)
            return this
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


        fun selectVideo(): Builder {
            bundleHelper.selectVideo(true)
            return this
        }

        fun multiSelect(): Builder {
            bundleHelper.multiSelect(true)
            return this

        }

        fun start(reqCode: Int) {

            val intent = getIntent()

            if (fragment != null) {
                fragment?.startActivityForResult(intent, reqCode)
            } else {
                activity.startActivityForResult(intent, reqCode)
            }
        }

        fun getIntent(): Intent {
            val intent = Intent(activity, ImagePickerActivity::class.java)

            intent.putExtras(bundleHelper.bundle)
            return intent
        }

    }
}
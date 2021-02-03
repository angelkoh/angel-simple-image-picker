@file:Suppress("unused")

package angel.androidapps.imagepicker

import android.content.Intent
import android.net.Uri
import android.os.Bundle

class BundleHelper {

    var bundle = Bundle()

    fun withCrop(destUri: Uri) {
        bundle.putString(EXTRA_CROP_DEST_URI, destUri.toString())
    }
    fun withAspectRatio(x: Float, y: Float) {
        bundle.putFloat(EXTRA_CROP_RATIO_X, x)
        bundle.putFloat(EXTRA_CROP_RATIO_Y, y)
    }

    fun withMaxResultSize(width: Int, height: Int) {
        bundle.putInt(EXTRA_CROP_MAX_WIDTH, width)
        bundle.putInt(EXTRA_CROP_MAX_HEIGHT, height)
    }

    fun pickFromCamera(folderName: String, fileName: String, replaceIfExist: Boolean) {
        bundle.putBoolean(EXTRA_USE_CAMERA, true)
        bundle.putString(EXTRA_CAMERA_FOLDER_NAME, folderName)
        bundle.putString(EXTRA_CAMERA_FILENAME, fileName)
        bundle.putBoolean(EXTRA_CAMERA_REPLACE_IF_EXISTING, replaceIfExist)
    }


    companion object {

        //INPUT
        const val EXTRA_USE_CAMERA = "extra.use_camera"
        const val EXTRA_CAMERA_FOLDER_NAME = "extra.folder_name"

        //OUTPUT
        const val EXTRA_FILE_PATH = "extra.file_path"
        const val EXTRA_ERROR = "extra.error"
        const val EXTRA_FILE_LAST_MOD = "extra.file_last_modified"

        //CAMERA FILENAME
        const val EXTRA_CAMERA_REPLACE_IF_EXISTING = "extra.camera_replace_if_exist"
        const val EXTRA_CAMERA_FILENAME = "extra.camera_filename"

        //CROPPING
        const val EXTRA_CROP_DEST_URI = "extra.crop_dest_uri"
        const val EXTRA_CROP_RATIO_X = "extra.crop_ratio_x"
        const val EXTRA_CROP_RATIO_Y = "extra.crop_ratio_y"
        const val EXTRA_CROP_MAX_WIDTH = "extra.crop_max_width"
        const val EXTRA_CROP_MAX_HEIGHT = "extra.crop_max_height"

        fun getError(data: Intent?) =
            data?.getStringExtra(EXTRA_ERROR) ?: "Unknown Error!"

        fun getFileLastModifiedDate(data: Intent?): Long {
            return data?.getLongExtra(EXTRA_FILE_LAST_MOD, 0L) ?: 0L
        }
    }
}
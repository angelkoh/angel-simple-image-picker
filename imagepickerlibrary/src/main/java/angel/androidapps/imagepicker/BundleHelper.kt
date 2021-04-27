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

    fun pickFromCamera() {
        bundle.putBoolean(EXTRA_USE_CAMERA, true)
        bundle.putBoolean(EXTRA_CAMERA_USE_DEFAULT_OUTPUT, true)
    }

    fun pickFromCamera(folderName: String, fileName: String, replaceIfExist: Boolean) {
        bundle.putBoolean(EXTRA_USE_CAMERA, true)
        bundle.putBoolean(EXTRA_CAMERA_USE_DEFAULT_OUTPUT, false)
        bundle.putString(EXTRA_CAMERA_FOLDER_NAME, folderName)
        bundle.putString(EXTRA_CAMERA_FILENAME, fileName)
        bundle.putBoolean(EXTRA_CAMERA_REPLACE_IF_EXISTING, replaceIfExist)
    }

    fun pickVideoFromCamera() {
        pickFromCamera()
        bundle.putBoolean(EXTRA_USE_CAMERA_VIDEO, true)
    }

    fun pickVideoFromCamera(folderName: String, fileName: String, replaceIfExist: Boolean) {
        pickFromCamera(folderName, fileName, replaceIfExist)
        bundle.putBoolean(EXTRA_USE_CAMERA_VIDEO, true)
    }

    fun multiSelect(multiSelect: Boolean) {
        bundle.putBoolean(EXTRA_IMAGE_MULTI_SELECT, multiSelect)
    }

    fun selectVideo(useVideo: Boolean) {
        bundle.putBoolean(EXTRA_VIDEO_SELECT, useVideo)
    }

    companion object {

        //CAMERA (INPUT)
        const val EXTRA_USE_CAMERA = "extra.use_camera"
        const val EXTRA_USE_CAMERA_VIDEO = "extra.use_camera_video"

        const val EXTRA_CAMERA_USE_DEFAULT_OUTPUT = "extra.camera_use_default_output"
        const val EXTRA_CAMERA_FOLDER_NAME = "extra.camera_folder_name"
        const val EXTRA_CAMERA_FILENAME = "extra.camera_filename"
        const val EXTRA_CAMERA_REPLACE_IF_EXISTING = "extra.camera_replace_if_exist"

        //OUTPUT
        const val EXTRA_FILE_PATH_LEGACY = "extra.file_path" //used by legacy < API 28
        const val EXTRA_FILE_LAST_MOD = "extra.file_last_modified"
        const val EXTRA_ERROR = "extra.error"

        //PHOTO
        const val EXTRA_IMAGE_MULTI_SELECT = "extra.image_multi_select"

        //VIDEO
        const val EXTRA_VIDEO_SELECT = "extra.video_select"

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
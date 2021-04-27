package angel.androidapps.imagepicker

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ContentUris
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.Keep
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import com.yalantis.ucrop.UCrop
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


@Keep
class ImagePickerActivity : Activity() {

    private var isMultiImageSelect = false
    private var useVideo = false

    //CAMERA RELATED
    private var useCamera = false
    private var useCameraVideo = false
    private var useCameraDefaultOutput = false
    private var cameraFolderName = "Angel"

    private var cameraUri: Uri? = null
    private var cameraFileLegacy: File? = null
    private var cameraFileName = ""
    private var cameraReplaceIfExist = false

    //CROP RELATED
    private var cropDestUri: Uri? = null
    private var cropRatioX = 0f
    private var cropRatioY = 0f
    private var cropMaxWidth = 0
    private var cropMaxHeight = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        extractExtrasFromIntent()

        when {
            useVideo -> selectVideo()
            useCamera -> if (useCameraVideo) takeVideo() else takePic()
            else -> selectPic()
        }
    }

    private fun extractExtrasFromIntent() {
        intent?.extras?.let { bundle ->
            BundleHelper.apply {

                //VIDEO RELATED
                useVideo = bundle.getBoolean(EXTRA_VIDEO_SELECT, false)

                //PHOTO RELATED
                isMultiImageSelect =
                    bundle.getBoolean(EXTRA_IMAGE_MULTI_SELECT, false)

                //CAMERA RELATED
                useCamera = bundle.getBoolean(EXTRA_USE_CAMERA, false)
                if (useCamera) {
                    useCameraVideo = bundle.getBoolean(EXTRA_USE_CAMERA_VIDEO, false)
                    useCameraDefaultOutput =
                        bundle.getBoolean(EXTRA_CAMERA_USE_DEFAULT_OUTPUT, true)
                    cameraFileName = bundle.getString(EXTRA_CAMERA_FILENAME, "")
                    cameraReplaceIfExist =
                        bundle.getBoolean(EXTRA_CAMERA_REPLACE_IF_EXISTING, false)
                    cameraFolderName =
                        bundle.getString(EXTRA_CAMERA_FOLDER_NAME, "AngelImages")
                }


                //CROPPING RELATED
                bundle.getString(EXTRA_CROP_DEST_URI, "").let {
                    if (!it.isNullOrBlank()) {
                        cropDestUri = Uri.parse(it)
                        cropRatioX = bundle.getFloat(EXTRA_CROP_RATIO_X, 0f)
                        cropRatioY = bundle.getFloat(EXTRA_CROP_RATIO_Y, 0f)
                        cropMaxWidth = bundle.getInt(EXTRA_CROP_MAX_WIDTH, 0)
                        cropMaxHeight = bundle.getInt(EXTRA_CROP_MAX_HEIGHT, 0)
                    }
                }
            }
        }
    }

    override fun onBackPressed() {
        Intent().let { intent ->
            intent.putExtra(BundleHelper.EXTRA_ERROR, "Image pick cancelled")
            setResult(RESULT_CANCELED, intent)
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        when (requestCode) {
            TYPE_SELECT_IMAGE -> {
                print("received image $data")
                if (resultCode == RESULT_OK) {
                    if (cropDestUri != null && !isMultiImageSelect) {
                        handleCrop(data?.data, cropDestUri)
                    } else {
                        handleMediaReady(data)
                    }
                } else {
                    setResult(resultCode, data)
                    finish()
                }
            }
            TYPE_SELECT_CAMERA_IMAGE -> {
                print("received camera image $data")
                if (resultCode == RESULT_OK) {
                    invokeMediaScannerIfNeeded()

                    if (cropDestUri != null) {
                        handleCrop(data?.data, cropDestUri)
                    } else {
                        handleCameraReady()
                    }
                } else {
                    setResult(resultCode, data)
                    finish()
                }
            }
            TYPE_SELECT_VIDEO -> {
                print("received video $data")
                if (resultCode == RESULT_OK) {
                    handleMediaReady(data)
                } else {
                    setResult(resultCode, data)
                    finish()
                }
            }
            TYPE_SELECT_CAMERA_VIDEO -> {
                handleCameraReady()
            }
            TYPE_CROP_IMAGE -> {
                print("Crop image")
                if (resultCode == RESULT_OK && data != null) {
                    handleCropReady(data)
                } else {
                    setResult(resultCode, data)
                    finish()
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    //=====================
    //POPULATE INTO RESULT
    //=====================
    private fun handleCropReady(data: Intent) {

        intent = Intent().also {
            it.data = UCrop.getOutput(data)
            it.putExtra(BundleHelper.EXTRA_FILE_LAST_MOD, System.currentTimeMillis())
        }

        setResult(RESULT_OK, intent)
        finish()
    }

    private fun handleCameraReady() {
        intent = Intent().also {
            it.putExtra(BundleHelper.EXTRA_FILE_LAST_MOD, System.currentTimeMillis())
            if (!useCameraDefaultOutput) {
                it.data = cameraUri
                it.putExtra(
                    BundleHelper.EXTRA_FILE_PATH_LEGACY, cameraFileLegacy?.absolutePath ?: ""
                )
            }
        }
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun handleMediaReady(data: Intent?) {
        data?.let { intent ->

            intent.data?.let { uri ->
                print("data path: ${intent.data?.path}")

                contentResolver.query(
                    uri, null, null, null, null
                )?.use { c ->

                    val lastModifiedDate = try {
                        val colDateModified = c
                            .getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                        c.moveToFirst()
                        c.getLong(colDateModified)
                    } catch (e: Exception) {
                        0
                    }
                    print("Date modified : $lastModifiedDate")
                    intent.putExtra(BundleHelper.EXTRA_FILE_LAST_MOD, lastModifiedDate)
                }
            }
        }

        setResult(RESULT_OK, data)
        finish()
    }

    //================
    //CROP ACTIONS
    //================
    private fun handleCrop(sourceUri: Uri?, destUri: Uri?) {
        if (sourceUri == null || destUri == null) {
            setResult(RESULT_CANCELED, null)
            finish()
        } else {
            val cropper = UCrop.of(sourceUri, destUri)
            if (cropRatioX > 0 && cropRatioY > 0) {
                cropper.withAspectRatio(cropRatioX, cropRatioY)
            }
            if (cropMaxWidth > 0 && cropMaxHeight > 0) {
                cropper.withMaxResultSize(cropMaxWidth, cropMaxHeight)
            }
            cropper.start(this, TYPE_CROP_IMAGE)
        }
    }

    //================
    //SELECTION ACTION
    //================
    private fun selectVideo() {
        Intent(Intent.ACTION_GET_CONTENT).also { chooseImageIntent ->
            chooseImageIntent.type = "video/*"

            // Ensure that there's a gallery activity to handle the intent
            chooseImageIntent.resolveActivity(packageManager)?.also {
                startActivityForResult(
                    // chooseImageIntent,
                    Intent.createChooser(
                        chooseImageIntent, getString(R.string.label_select_video)
                    ),
                    TYPE_SELECT_VIDEO
                )
            }
        }
    }

    private fun selectPic() {

        Intent(Intent.ACTION_GET_CONTENT).also { chooseImageIntent ->
            chooseImageIntent.type = "image/*"
            if (isMultiImageSelect) {
                chooseImageIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
            // Ensure that there's a gallery activity to handle the intent
            chooseImageIntent.resolveActivity(packageManager)?.also {
                startActivityForResult(
                    // chooseImageIntent,
                    Intent.createChooser(
                        chooseImageIntent, getString(R.string.label_select_picture)
                    ),
                    TYPE_SELECT_IMAGE
                )
            }
        }

    }

    //================
    //CAMERA ACTION
    //================
    private fun takeVideo() {
        Intent(MediaStore.ACTION_VIDEO_CAPTURE)
            .also { videoIntent ->
                print("taking video...")
                videoIntent.resolveActivity(packageManager)?.also {

                    if (!useCameraDefaultOutput) {
                        cameraUri = getCameraVideoUri()
                        videoIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri)

                        //permission bug for pre lollipop
                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
                            videoIntent.clipData = ClipData.newRawUri("", cameraUri)
                        }
                    }

                    //permission bug for pre lollipop
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
                        videoIntent.addFlags(
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }

                    print("takePictureIntent -> $videoIntent")
                    startActivityForResult(videoIntent, TYPE_SELECT_CAMERA_VIDEO)

                } ?: print("Cannot find camera app.")
            }
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun takePic() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            .also { cameraIntent ->
                print("taking picture...")
                // Ensure that there's a camera activity to handle the intent
                cameraIntent.resolveActivity(packageManager)?.also {

                    if (!useCameraDefaultOutput) {
                        cameraUri = getCameraImageUri()
                        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri)

                        //permission bug for pre lollipop
                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
                            cameraIntent.clipData = ClipData.newRawUri("", cameraUri)
                        }
                    }

                    //permission bug for pre lollipop
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
                        cameraIntent.addFlags(
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }

                    print("takePictureIntent -> $cameraIntent")
                    startActivityForResult(cameraIntent, TYPE_SELECT_CAMERA_IMAGE)
                } ?: print("Cannot find camera app.")
            }
    }

    //IMAGE URI (FOR TAKING PHOTOS/VIDEOS)
    private fun getCameraImageUri(): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            print("set camera Uri")
            getCameraUriQ(getJpgFileName())
        } else {
            print("set camera Uri (legacy)")
            getCameraUriLegacy(getJpgFileName())
        }
    }

    private fun getCameraVideoUri(): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            print("set camera Uri")
            getCameraUriQ(getMp4FileName())
        } else {
            print("set camera Uri (legacy)")
            getCameraUriLegacy(getMp4FileName())
        }
    }

    private fun getJpgFileName(): String {
        return if (cameraFileName.isBlank()) {
            "img_" + sdf.format(Date()) + ".jpg"
        } else {
            cameraFileName
        }
    }

    private fun getMp4FileName(): String {
        return if (cameraFileName.isBlank()) {
            "vid_" + sdf.format(Date()) + ".mp4"
        } else {
            cameraFileName
        }
    }

//    fun getMimeType(uri: Uri?): String {
//        return uri?.let {
//            val mimeType  = if (ContentResolver.SCHEME_CONTENT == uri.scheme) {
//                val cr: ContentResolver = applicationContext.contentResolver
//                cr.getType(uri)
//            } else {
//                val fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
//                MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension.toLowerCase(Locale.ROOT))
//            }
//            mimeType
//        } ?: ""
//    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getExistingCameraUriOrNullQ(fileName: String): Uri? {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.RELATIVE_PATH,
            MediaStore.MediaColumns.DATE_MODIFIED
        )

        val selection =
            "${MediaStore.MediaColumns.RELATIVE_PATH}='Pictures/$cameraFolderName/' AND " +
                    "${MediaStore.MediaColumns.DISPLAY_NAME}='$fileName' "

        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection, selection, null, null
        ).use { c ->
            if (c != null && c.count >= 1) {
                // already inserted, update this row or do sth else
                print("has cursor result")
                c.moveToFirst()
                val id = c.getLong(c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                return ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    .also {
                        debugPrintFileCursor(c, it)
                    }
            }
        }
        print("image not created yet")
        return null
    }

    private fun debugPrintFileCursor(c: Cursor, uri: Uri) {
        val displayName = c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME))
        val relativePath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH))
        } else ""
        val lastModifiedDate = c.getLong(c.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED))
        print("image uri update [$displayName]: $relativePath $uri ($lastModifiedDate)")
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getCameraUriQ(fileName: String): Uri {

        val resolver = contentResolver

        var uri: Uri? = null
        if (cameraFileName.isNotBlank()) {
            uri = getExistingCameraUriOrNullQ(fileName)
        }
        if (uri == null || cameraReplaceIfExist) {

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/$cameraFolderName")
            }
            uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        }
        print("image uri $uri")
        return uri!!
    }

    //SAVE INTO FILE (https://developer.android.com/training/camera/photobasics)
    //https://medium.com/@pednekarshashank33/android-10s-scoped-storage-image-picker-gallery-camera-d3dcca427bbf

    @Suppress("DEPRECATION")
    private fun getCameraUriLegacy(fileName: String): Uri {
        val folder =
            File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                cameraFolderName
            )
        folder.mkdirs()

        File(folder, fileName).also {
            cameraFileLegacy = it
            //note: legacy, will always replace existing.
            if (it.exists() && cameraReplaceIfExist) it.delete()
            if (!it.exists()) it.createNewFile()

            return FileProvider.getUriForFile(
                this,
                packageName + ".imagePicker." + getString(R.string.fp_authority),
                it
            )
        }
    }

    //media scanner
    @Suppress("DEPRECATION")
    private fun invokeMediaScannerIfNeeded() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            //invoke the system's media scanner to add your photo to the Media Provider's database,
            // making it available in the Android Gallery application and to other apps.
            cameraFileLegacy?.let {
                Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).also { mediaScanIntent ->
                    print("invoke Media scanner")
                    mediaScanIntent.data = Uri.fromFile(it)
                    sendBroadcast(mediaScanIntent)
                }
            }
        }
    }


    companion object {
        private const val TAG = "Angel: ImgPickA"
        private fun print(s: String) = Log.d(TAG, s)

        private val sdf = SimpleDateFormat("yyMMdd_hhmmss", Locale.US)

        private const val TYPE_CROP_IMAGE = 100
        private const val TYPE_SELECT_IMAGE = 101
        private const val TYPE_SELECT_VIDEO = 102
        private const val TYPE_SELECT_CAMERA_IMAGE = 103
        private const val TYPE_SELECT_CAMERA_VIDEO = 104
    }
}

package angel.androidapps.imagepicker

import android.app.Activity
import android.content.ClipData
import android.content.ContentUris
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import com.yalantis.ucrop.UCrop
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ImagePickerActivity : Activity() {

    private var lastModifiedDate = 0L

    private var useCamera = false
    private var cameraFolderName = "Angel"

    private var cameraImageUri: Uri? = null
    private var cameraFile: File? = null
    private var cameraFileName = ""
    private var cameraReplaceIfExist = false

    private var cropDestUri: Uri? = null
    private var cropRatioX = 0f
    private var cropRatioY = 0f
    private var cropMaxWidth = 0
    private var cropMaxHeight = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        extractExtrasFromIntent()

        if (useCamera) {
            takePic()
        } else {
            selectPic()
        }
    }

    private fun extractExtrasFromIntent() {
        intent?.extras?.let { bundle ->

            //CAMERA RELATED
            useCamera = bundle.getBoolean(BundleHelper.EXTRA_USE_CAMERA, false)
            if (useCamera) {
                cameraFileName = bundle.getString(BundleHelper.EXTRA_CAMERA_FILENAME, "")
                cameraReplaceIfExist =
                    bundle.getBoolean(BundleHelper.EXTRA_CAMERA_REPLACE_IF_EXISTING, false)
                cameraFolderName =
                    bundle.getString(BundleHelper.EXTRA_CAMERA_FOLDER_NAME, "AngelImages")
            }

            //CROPPING RELATED
            bundle.getString(BundleHelper.EXTRA_CROP_DEST_URI, "").also {
                if (!it.isNullOrBlank()) {
                    cropDestUri = Uri.parse(it)
                    cropRatioX = bundle.getFloat(BundleHelper.EXTRA_CROP_RATIO_X, 0f)
                    cropRatioY = bundle.getFloat(BundleHelper.EXTRA_CROP_RATIO_Y, 0f)
                    cropMaxWidth = bundle.getInt(BundleHelper.EXTRA_CROP_MAX_WIDTH, 0)
                    cropMaxHeight = bundle.getInt(BundleHelper.EXTRA_CROP_MAX_HEIGHT, 0)
                }
            }
        }
    }

    override fun onBackPressed() {

        intent = Intent().also {
            it.putExtra(BundleHelper.EXTRA_ERROR, "Image pick cancelled")
        }
        setResult(RESULT_CANCELED, intent)

        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            101 -> {
                print("received image $data")
                if (resultCode == RESULT_OK) {
                    if (cropDestUri != null) {
                        handleCrop(data?.data, cropDestUri)
                    } else {
                        handleImageReady(data)
                    }
                } else {
                    setResult(resultCode, data)
                    finish()
                }
            }
            102 -> {
                print("received camera $data")
                if (resultCode == RESULT_OK) {
                    invokeMediaScannerIfNeeded()

                    if (cropDestUri != null) {
                        handleCrop(cameraImageUri, cropDestUri)
                    } else {
                        handleCameraImageReady()
                    }
                } else {
                    setResult(resultCode, data)
                    finish()
                }
            }
            103 -> {
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

    private fun handleCropReady(data: Intent) {

        intent = Intent().also {
            it.data = UCrop.getOutput(data)
            it.putExtra(BundleHelper.EXTRA_FILE_LAST_MOD, System.currentTimeMillis())
        }

        setResult(RESULT_OK, intent)
        finish()
    }

    private fun handleCameraImageReady() {
        intent = Intent().also {
            it.data = cameraImageUri
            it.putExtra(BundleHelper.EXTRA_FILE_LAST_MOD, lastModifiedDate)
            it.putExtra(
                BundleHelper.EXTRA_FILE_PATH,
                cameraFile?.absolutePath ?: ""
            )
        }
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun handleImageReady(data: Intent?) {
        data?.let { intent ->
            intent.data?.let { uri ->
                contentResolver.query(
                    uri, null, null, null, null
                )?.use { c ->

                    lastModifiedDate = try {
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
            cropper.start(this, 103)

        }
    }

    private fun selectPic() {
        Intent(Intent.ACTION_GET_CONTENT).also { chooseImageIntent ->
            chooseImageIntent.type = "image/*"

            // Ensure that there's a gallery activity to handle the intent
            chooseImageIntent.resolveActivity(packageManager)?.also {
                startActivityForResult(
                    // chooseImageIntent,
                    Intent.createChooser(
                        chooseImageIntent, getString(R.string.label_select_picture)
                    ),
                    101
                )
            }
        }
    }

    private fun takePic() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            .also { takePictureIntent ->
                print("taking picture...")
                val uri = setCameraImageUri()
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
                // Ensure that there's a camera activity to handle the intent
                takePictureIntent.resolveActivity(packageManager)?.also {

                    //permission bug for pre lollipop
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
                        takePictureIntent.clipData = ClipData.newRawUri("", uri)
                        takePictureIntent.addFlags(
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }

                    print(
                        "takePictureIntent -> $takePictureIntent"
                    )
                    startActivityForResult(takePictureIntent, 102)
                } ?: print("Cannot find camera app.")
            }
    }

    //IMAGE URI (FOR TAKING PHOTOS)
    private fun setCameraImageUri(): Uri {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            print("set camera Uri")
            setCameraImageUriQ()
        } else {
            print("set camera Uri (legacy)")
            setCameraImageUriLegacy()
        }
    }

    private fun getFileName(): String {
        return if (cameraFileName.isBlank()) {
            "img_" + sdf.format(Date()) + ".jpg"
        } else {
            cameraFileName
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getExistingCameraImageUriOrNullQ(): Uri? {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.RELATIVE_PATH,
            MediaStore.MediaColumns.DATE_MODIFIED
        )

        val selection =
            "${MediaStore.MediaColumns.RELATIVE_PATH}='Pictures/$cameraFolderName/' AND " +
                    "${MediaStore.MediaColumns.DISPLAY_NAME}='${getFileName()}' "

        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection, selection, null, null
        ).use { c ->
            if (c != null && c.count >= 1) {
                // already inserted, update this row or do sth else
                print("has cursor result")
                c.moveToFirst().let {

                    val id = c.getLong(c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                    val displayName =
                        c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME))
                    val relativePath =
                        c.getString(c.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH))

                    lastModifiedDate =
                        c.getLong(c.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED))
                    cameraImageUri =
                        ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            id
                        )
                    print(
                        "image uri update $displayName $relativePath $cameraImageUri ($lastModifiedDate)"
                    )

                    return cameraImageUri
                }
            }
        }
        print("image not created yet")
        return null
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun setCameraImageUriQ(): Uri {

        val resolver = contentResolver

        if (cameraFileName.isNotBlank()) {
            cameraImageUri = getExistingCameraImageUriOrNullQ()
        }
        if (cameraImageUri == null || cameraReplaceIfExist) {

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, getFileName())
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/$cameraFolderName")
            }
            cameraImageUri =
                resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            print(
                "image uri insert $cameraImageUri"
            )
        }
        print(
            "image uri $cameraImageUri"
        )
        return cameraImageUri!!
    }

    //SAVE INTO FILE (https://developer.android.com/training/camera/photobasics)
    //https://medium.com/@pednekarshashank33/android-10s-scoped-storage-image-picker-gallery-camera-d3dcca427bbf
    @Suppress("DEPRECATION")
    private fun setCameraImageUriLegacy(): Uri {
        // val folder = File("${getExternalFilesDir(Environment.DIRECTORY_DCIM)}",folderName)
        val folder =
            File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                cameraFolderName
            )
        folder.mkdirs()

        cameraFile = File(folder, getFileName()).also {
            if (it.exists() && cameraReplaceIfExist) it.delete()
            if (!it.exists()) it.createNewFile()
            lastModifiedDate = System.currentTimeMillis()

            cameraImageUri = FileProvider.getUriForFile(
                this,
                packageName + ".imagePicker." + getString(R.string.fp_authority),
                it
            )
        }

        return cameraImageUri!!
    }

    //media scanner
    @Suppress("DEPRECATION")
    private fun invokeMediaScannerIfNeeded() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            //invoke the system's media scanner to add your photo to the Media Provider's database,
            // making it available in the Android Gallery application and to other apps.
            cameraFile?.let {
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
    }
}

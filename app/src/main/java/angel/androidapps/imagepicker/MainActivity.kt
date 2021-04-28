package angel.androidapps.imagepicker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import angel.androidapps.imagepicker.permissions.StoragePermissionHandler
import com.bumptech.glide.Glide
import com.bumptech.glide.signature.ObjectKey
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var tv: TextView
    private lateinit var iv: ImageView
    private lateinit var rv: RecyclerView
    private lateinit var vv: VideoView
    private val rvAdapter = RvAdapter(::displayUri)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tv = findViewById(R.id.tv_text)
        iv = findViewById(R.id.iv_image)
        rv = findViewById(R.id.rv)
        vv = findViewById(R.id.video_view)
        val mediaController = MediaController(this)
        mediaController.setAnchorView(vv)
        vv.setMediaController(mediaController)

        rv.adapter = rvAdapter
        findViewById<Button>(R.id.btn_take_picture).setOnClickListener { takePicture() }
        findViewById<Button>(R.id.btn_take_picture).setOnLongClickListener {
            takeVideo()
            true
        }
        findViewById<Button>(R.id.btn_select_image_single).setOnClickListener { selectPicture() }
        findViewById<Button>(R.id.btn_select_video).setOnClickListener { selectVideo() }
        findViewById<Button>(R.id.btn_select_image_multiple).setOnClickListener { selectMultiplePicture() }
    }

    private fun selectMultiplePicture() {
        ImagePicker.with(this)

            //.withCrop(getAvatarUri())
            .withAspectRatio(2f, 3f)
            .withMaxResultSize(200, 300)
            .multiSelect()
            .start(SELECT_PICTURE_REQ_CODE)
    }

    private fun selectPicture() {
        ImagePicker.with(this)

            .withCrop(getAvatarUri())
            .withAspectRatio(2f, 3f)
            .withMaxResultSize(200, 300)

            .start(SELECT_PICTURE_REQ_CODE)
    }

    private fun selectVideo() {
        ImagePicker.with(this)
            .selectVideo()
            .start(SELECT_VIDEO_REQ_CODE)
    }

    private fun takePicture() {
        print("Camera Image.")

        StoragePermissionHandler.let {
            if (
                !it.requireStoragePermission() ||
                it.requestIfNeededStoragePermission(this, TAKE_PICTURE_PERMISSION_REQ_CODE)
            ) {

                ImagePicker.with(this)
                    //  .pickFromCamera(FOLDER_NAME, "avatar.jpg", true)
                   .pickFromCamera()
                    .withCrop(getAvatarUri())
                    .withAspectRatio(2f, 3f)
                    .withMaxResultSize(200, 300)
                    .start(TAKE_PICTURE_REQ_CODE)
            } else {
                print("REQUIRE PERMISSION (TAKE PICTURE)")
            }
        }
    }
    private fun takeVideo() {
        print("Camera Video.")

        StoragePermissionHandler.let {
            if (
                !it.requireStoragePermission() ||
                it.requestIfNeededStoragePermission(this, TAKE_PICTURE_PERMISSION_REQ_CODE)
            ) {

                ImagePicker.with(this)
                    .pickVideoFromCamera()
                  // .pickVideoFromCamera(FOLDER_NAME, "avatar.mp4", true)

                    .start(SELECT_VIDEO_REQ_CODE)
            } else {
                print("REQUIRE PERMISSION (TAKE PICTURE)")
            }
        }
    }

    private fun displayUri(uri: Uri) {
        Glide.with(this)
            .load(uri)
            .into(iv)
        print("Selected $uri ${ImagePicker.getMimeType(this, uri)}")
    }

    //RESULT
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            TAKE_PICTURE_REQ_CODE, SELECT_PICTURE_REQ_CODE ->
                if (resultCode == RESULT_OK && data != null) {

                    vv.stopPlayback()
                    vv.visibility = View.GONE

                    val list = ImagePicker.getUris(data)
                    rvAdapter.set(list)
                    if (list.isNotEmpty()) {
                        val lastMod = ImagePicker.getLastModifiedDate(data)
                        val uri = list.first()
                        print("Image Uri: $uri (date: $lastMod) -> CROPPING")

                        Glide.with(this)
                            .load(uri)
                            .signature(ObjectKey(lastMod))
                            .into(iv)
                    } else {

                        print("no images selected")
                    }

                } else {
                    print("ERROR GETTING IMAGE: ${ImagePicker.getError(data)}")
                }
            SELECT_VIDEO_REQ_CODE -> {
                rvAdapter.set(emptyList())
                Glide.with(this).clear(iv)

                Glide.with(this)
                    .clear(iv)
                val list = ImagePicker.getUris(data)
                if (list.isNotEmpty()) {
                    val uri = list.first()
                    vv.setVideoURI(uri)
                    vv.start()
                } else {
                    print("no videos selected")
                }



                vv.visibility = View.VISIBLE


            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun getAvatarUri(): Uri {
        val avatarFile = File(this.filesDir, "avatar.jpg")
        if (avatarFile.exists()) {
            avatarFile.delete()
        }
        return avatarFile.toUri()
    }

    //PERMISSION RESULT
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            TAKE_PICTURE_PERMISSION_REQ_CODE ->
                when {
                    StoragePermissionHandler.hasStoragePermission(this) -> {
                        takePicture()
                    }
                    StoragePermissionHandler.isStoragePermissionPermanentlyDenied(this) -> {
                        print("PERMANENTLY DENIED (TAKE PICTURE)")
                    }
                    else -> {
                        print("DENIED (TAKE PICTURE)")
                    }
                }
            else ->
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun print(s: String) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
        tv.text = s
        Log.d(TAG, s)
    }

    companion object {
        private const val TAKE_PICTURE_REQ_CODE = 101
        private const val SELECT_PICTURE_REQ_CODE = 102
        private const val SELECT_VIDEO_REQ_CODE = 103

        private const val TAKE_PICTURE_PERMISSION_REQ_CODE = 201

        private const val FOLDER_NAME = "Angel"
        private const val TAG = "Angel: MainActivity"
    }
}
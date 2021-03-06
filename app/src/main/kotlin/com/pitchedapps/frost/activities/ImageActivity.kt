package com.pitchedapps.frost.activities

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.support.design.widget.FloatingActionButton
import android.support.v4.content.FileProvider
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import ca.allanwang.kau.email.sendEmail
import ca.allanwang.kau.internal.KauBaseActivity
import ca.allanwang.kau.permissions.PERMISSION_WRITE_EXTERNAL_STORAGE
import ca.allanwang.kau.permissions.kauRequestPermissions
import ca.allanwang.kau.utils.*
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.BaseTarget
import com.bumptech.glide.request.target.SizeReadyCallback
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.mikepenz.google_material_typeface_library.GoogleMaterial
import com.mikepenz.iconics.typeface.IIcon
import com.pitchedapps.frost.BuildConfig
import com.pitchedapps.frost.R
import com.pitchedapps.frost.utils.*
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by Allan Wang on 2017-07-15.
 */
class ImageActivity : KauBaseActivity() {

    val progress: ProgressBar by bindView(R.id.image_progress)
    val container: ViewGroup by bindView(R.id.image_container)
    val panel: SlidingUpPanelLayout? by bindOptionalView(R.id.image_panel)
    val photo: SubsamplingScaleImageView by bindView(R.id.image_photo)
    val caption: TextView? by bindOptionalView(R.id.image_text)
    val fab: FloatingActionButton by bindView(R.id.image_fab)

    /**
     * Reference to the temporary file path
     * Should be nonnull if the image is successfully loaded
     * As this is temporary, the image is deleted upon exit
     */
    internal var tempFilePath: String? = null
    /**
     * Reference to path for downloaded image
     * Nonnull once the image is downloaded by the user
     */
    internal var downloadPath: String? = null
    /**
     * Indicator for fab's click result
     */
    internal var fabAction: FabStates = FabStates.NOTHING
        set(value) {
            if (field == value) return
            field = value
            value.update(fab)
        }

    val imageUrl: String
        get() = intent.extras.getString(ARG_IMAGE_URL)

    val text: String?
        get() = intent.extras.getString(ARG_TEXT)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = if (!text.isNullOrBlank()) R.layout.activity_image else R.layout.activity_image_textless
        setContentView(layout)
        container.setBackgroundColor(Prefs.bgColor.withMinAlpha(222))
        caption?.setTextColor(Prefs.textColor)
        caption?.setBackgroundColor(Prefs.bgColor.colorToForeground(0.2f).withAlpha(255))
        caption?.text = text
        progress.tint(Prefs.accentColor)
        panel?.addPanelSlideListener(object : SlidingUpPanelLayout.SimplePanelSlideListener() {

            override fun onPanelSlide(panel: View, slideOffset: Float) {
                if (slideOffset == 0f && !fab.isShown) fab.show()
                else if (slideOffset != 0f && fab.isShown) fab.hide()
                caption?.alpha = slideOffset / 2 + 0.5f
            }

        })
        fab.setOnClickListener { fabAction.onClick(this) }
        photo.setOnImageEventListener(object : SubsamplingScaleImageView.DefaultOnImageEventListener() {
            override fun onImageLoadError(e: Exception) {
                L.e(e, "Image load error")
                imageCallback(null, false)
            }
        })
        Glide.with(this).asBitmap().load(imageUrl).into(PhotoTarget(this::imageCallback))
    }

    /**
     * Callback to add image to view
     * [resource] is guaranteed to be nonnull when [success] is true
     * and null when it is false
     */
    private fun imageCallback(resource: Bitmap?, success: Boolean) {
        if (progress.isVisible) progress.fadeOut()
        if (success) {
            saveTempImage(resource!!, {
                if (it == null) {
                    imageCallback(null, false)
                } else {
                    photo.setImage(ImageSource.uri(it))
                    fabAction = FabStates.DOWNLOAD
                    photo.animate().alpha(1f).scaleXY(1f).withEndAction { fab.show() }.start()
                }
            })
        } else {
            fabAction = FabStates.ERROR
            fab.show()
        }
    }

    /**
     * Bitmap load handler
     */
    class PhotoTarget(val callback: (resource: Bitmap?, success: Boolean) -> Unit) : BaseTarget<Bitmap>() {

        override fun removeCallback(cb: SizeReadyCallback?) {}

        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) = callback(resource, true)

        override fun onLoadFailed(errorDrawable: Drawable?) = callback(null, false)

        override fun getSize(cb: SizeReadyCallback) = cb.onSizeReady(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)

    }

    private fun saveTempImage(resource: Bitmap, callback: (uri: Uri?) -> Unit) {
        var photoFile: File? = null
        try {
            photoFile = createImageFile()
        } catch (ignored: IOException) {
        } finally {
            if (photoFile == null) {
                callback(null)
            } else {
                tempFilePath = photoFile.absolutePath
                Timber.d("Temp image path $tempFilePath")
                // File created; proceed with request
                val photoURI = FileProvider.getUriForFile(this,
                        BuildConfig.APPLICATION_ID + ".provider",
                        photoFile)
                photoFile.outputStream().use { resource.compress(Bitmap.CompressFormat.PNG, 100, it) }
                callback(photoURI)
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "Frost_" + timeStamp + "_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(imageFileName, ".png", storageDir)
    }

    internal fun downloadImage() {
        kauRequestPermissions(PERMISSION_WRITE_EXTERNAL_STORAGE) {
            granted, _ ->
            L.d("Download image callback granted: $granted")
            if (granted) {
                doAsync {
                    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
                    val imageFileName = "Frost_" + timeStamp + "_"
                    val storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    val frostDir = File(storageDir, "Frost")
                    if (!frostDir.exists()) frostDir.mkdirs()
                    val destination = File.createTempFile(imageFileName, ".png", frostDir)
                    downloadPath = destination.absolutePath
                    var success = true
                    try {
                        File(tempFilePath).copyTo(destination, true)
                        scanFile(destination)
                    } catch (e: Exception) {
                        success = false
                    } finally {
                        L.d("Download image async finished: $success")
                        uiThread {
                            val text = if (success) R.string.image_download_success else R.string.image_download_fail
                            snackbar(text)
                            if (success) fabAction = FabStates.SHARE
                        }
                    }
                }
            }
        }
    }

    /**
     * See <a href="https://developer.android.com/training/camera/photobasics.html#TaskGallery">Docs</a>
     */
    internal fun scanFile(file: File) {
        if (!file.exists()) return
        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        val contentUri = Uri.fromFile(file)
        mediaScanIntent.data = contentUri
        this.sendBroadcast(mediaScanIntent)
    }

    internal fun deleteTempFile() {
        if (tempFilePath != null) {
            File(tempFilePath!!).delete()
            tempFilePath = null
        }
    }

    override fun onDestroy() {
        deleteTempFile()
        super.onDestroy()
    }
}

internal enum class FabStates(val iicon: IIcon, val iconColor: Int = Prefs.iconColor, val backgroundTint: Int = Prefs.iconBackgroundColor.withAlpha(255)) {
    ERROR(GoogleMaterial.Icon.gmd_error, Color.WHITE, Color.RED) {
        override fun onClick(activity: ImageActivity) {
            activity.materialDialogThemed {
                title(R.string.kau_error)
                content(R.string.bad_image_overlay)
                positiveText(R.string.kau_yes)
                onPositive { _, _ ->
                    activity.sendEmail(R.string.dev_email, R.string.debug_image_link_subject) {
                        addItem("Url", activity.imageUrl)
                    }
                }
                negativeText(R.string.kau_no)
            }
        }
    },
    NOTHING(GoogleMaterial.Icon.gmd_adjust) {
        override fun onClick(activity: ImageActivity) {}
    },
    DOWNLOAD(GoogleMaterial.Icon.gmd_file_download) {
        override fun onClick(activity: ImageActivity) = activity.downloadImage()
    },
    SHARE(GoogleMaterial.Icon.gmd_share) {
        override fun onClick(activity: ImageActivity) {
            try {
                val photoURI = FileProvider.getUriForFile(activity,
                        BuildConfig.APPLICATION_ID + ".provider",
                        File(activity.downloadPath))
                val intent = Intent(Intent.ACTION_SEND).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra(Intent.EXTRA_STREAM, photoURI)
                    type = "image/png"
                }
                activity.startActivity(intent)
            } catch (e: Exception) {
                L.e(e, "Image share failed");
                activity.snackbar(R.string.image_share_failed)
            }
        }
    };

    /**
     * Change the fab look
     * If it's in view, give it some animations
     */
    fun update(fab: FloatingActionButton) {
        if (fab.isHidden) {
            fab.setIcon(iicon, color = iconColor)
            fab.backgroundTintList = ColorStateList.valueOf(backgroundTint)
            fab.show()
        } else {
            fab.fadeScaleTransition {
                setIcon(iicon, color = iconColor)
                backgroundTintList = ColorStateList.valueOf(backgroundTint)
            }
        }
    }

    abstract fun onClick(activity: ImageActivity)

}
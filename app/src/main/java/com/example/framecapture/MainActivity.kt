package com.example.framecapture

import android.R
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import android.media.MediaPlayer.OnPreparedListener
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.framecapture.databinding.ActivityMainBinding
import com.google.android.exoplayer2.ExoPlayer


class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding
    private var exoPlayer: ExoPlayer? = null
    var videoSource = "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
    val videoPath = "android.resource://com.example.framecapture/raw/clock"
    var uriVideoSource: Uri? = null


    lateinit var myMediaController : MediaController
    lateinit var myMediaMetadataRetriever : MediaMetadataRetriever

    var stringOpts = arrayOf(
        "none",
        "OPTION_CLOSEST",
        "OPTION_CLOSEST_SYNC",
        "OPTION_NEXT_SYNC",
        "OPTION_PREVIOUS_SYNC"
    )
    var valOptions = intArrayOf(
        0,  //will not be used
        MediaMetadataRetriever.OPTION_CLOSEST,
        MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
        MediaMetadataRetriever.OPTION_NEXT_SYNC,
        MediaMetadataRetriever.OPTION_PREVIOUS_SYNC,
    )
    var captureOption = arrayOf(
        "Media Metadata Retriever",
        "Pixel Copy"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        prepareVideoView()

        val adapter = ArrayAdapter(
            this@MainActivity,
            R.layout.simple_list_item_1, captureOption
        )
        adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        binding.option.adapter = adapter

        binding.capture.setOnClickListener(listener)
    }

    private val listener = View.OnClickListener { view ->
        Toast.makeText(this, "masuk capture", Toast.LENGTH_SHORT).show()

        val currentPosition: Int = binding.vview.currentPosition //in millisecond

        Toast.makeText(
            this@MainActivity,
            "Current Position: $currentPosition (ms)",
            Toast.LENGTH_LONG
        ).show()

        val pos = currentPosition * 1000 //unit in microsecond

        val opt: Int = binding.option.selectedItemPosition
        if (opt == 0) {
            processBitmap(
                myMediaMetadataRetriever
                .getFrameAtTime(
                    pos.toLong(),
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )
            )
        } else {
            usePixelCopy(binding.vview) { bitmap: Bitmap? ->
                if (bitmap != null) {
                    runOnUiThread {
                        processBitmap(bitmap)
                    }
                }
            }
        }

//        val bmFrame : Bitmap?
//        if(opt == 0){
//            bmFrame = myMediaMetadataRetriever
//                .getFrameAtTime(pos.toLong());
//        }else{
//            bmFrame = myMediaMetadataRetriever
//                .getFrameAtTime(pos.toLong(),
//                    valOptions[opt]);
//        }
//        processBitmap(bmFrame)


    }

    private fun prepareVideoView() {
        try {

            uriVideoSource = Uri.parse(videoSource)
            myMediaMetadataRetriever = MediaMetadataRetriever()
//            myMediaMetadataRetriever.setDataSource(
//                this, uriVideoSource
//            )
            myMediaMetadataRetriever.setDataSource(
                videoSource, HashMap<String, String>()
            )

            myMediaController = MediaController(this@MainActivity)
            Toast.makeText(this@MainActivity, videoSource, Toast.LENGTH_LONG).show()
            binding.vview.setVideoURI(uriVideoSource)
            binding.vview.setMediaController(myMediaController)
            binding.vview.setOnCompletionListener(myVideoViewCompletionListener)
            binding.vview.setOnPreparedListener(myVideoViewPreparedListener)
            binding.vview.setOnErrorListener(myVideoViewErrorListener)
            binding.vview.requestFocus()
            binding.vview.start()
        }catch (e:Exception){
            Log.d("masuk sini", e.toString())
        }

    }

    private var myVideoViewCompletionListener = OnCompletionListener {
        Toast.makeText(
            this@MainActivity, "End of Video",
            Toast.LENGTH_LONG
        ).show()
    }

    private var myVideoViewPreparedListener = OnPreparedListener {
        val duration: Long = binding.vview.duration.toLong() //in millisecond
        Toast.makeText(
            this@MainActivity,
            "Duration: $duration (ms)",
            Toast.LENGTH_LONG
        ).show()
    }

    private var myVideoViewErrorListener =
        MediaPlayer.OnErrorListener { mp, what, extra ->
            var errWhat = ""
            errWhat = when (what) {
                MediaPlayer.MEDIA_ERROR_UNKNOWN -> "MEDIA_ERROR_UNKNOWN"
                MediaPlayer.MEDIA_ERROR_SERVER_DIED -> "MEDIA_ERROR_SERVER_DIED"
                else -> "unknown what"
            }
            var errExtra = ""
            errExtra = when (extra) {
                MediaPlayer.MEDIA_ERROR_IO -> "MEDIA_ERROR_IO"
                MediaPlayer.MEDIA_ERROR_MALFORMED -> "MEDIA_ERROR_MALFORMED"
                MediaPlayer.MEDIA_ERROR_UNSUPPORTED -> "MEDIA_ERROR_UNSUPPORTED"
                MediaPlayer.MEDIA_ERROR_TIMED_OUT -> "MEDIA_ERROR_TIMED_OUT"
                else -> "...others"
            }
            Toast.makeText(
                this@MainActivity,
                """
                      Error!!!
                      what: $errWhat
                      extra: $errExtra
                      """.trimIndent(),
                Toast.LENGTH_LONG
            ).show()
            true
        }

    fun usePixelCopy(videoView: SurfaceView,  callback: (Bitmap?) -> Unit) {
        val bitmap: Bitmap = Bitmap.createBitmap(
            videoView.width,
            videoView.height,
            Bitmap.Config.ARGB_8888
        )
        try {
            // Create a handler thread to offload the processing of the image.
            val handlerThread = HandlerThread("PixelCopier")
            handlerThread.start()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Log.d("masuk","masuk sini nih2")
                PixelCopy.request(
                    videoView, bitmap,
                    PixelCopy.OnPixelCopyFinishedListener { copyResult ->
                        if (copyResult == PixelCopy.SUCCESS) {
                            callback(bitmap)
                        }
                        handlerThread.quitSafely()
                    },
                    Handler(handlerThread.looper)
                )
            }
        } catch (e: IllegalArgumentException) {
            callback(null)
            // PixelCopy may throw IllegalArgumentException, make sure to handle it
            e.printStackTrace()
        }
    }

    fun processBitmap(bitmap: Bitmap?){
        if (bitmap == null) {
            Toast.makeText(
                this@MainActivity,
                "bmFrame == null!",
                Toast.LENGTH_LONG
            ).show()
        } else {
            val myCaptureDialog = AlertDialog.Builder(this@MainActivity)
            val capturedImageView = ImageView(this@MainActivity)
            capturedImageView.setImageBitmap(bitmap)
            val capturedImageViewLayoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            capturedImageView.layoutParams = capturedImageViewLayoutParams
            myCaptureDialog.setView(capturedImageView)
            myCaptureDialog.show()
        }
    }
}
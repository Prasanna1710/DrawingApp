package psv.app.drawing_canvas

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    var drawingView: DrawingView? = null

    //we will be using only 1 var for all color buttons
    //This will be done by considering linear layouts as array of views
    var currentColorButton: ImageButton? = null

    var eraser: ImageButton? = null

    private var ImagePath: String? = null
    /*

     */
    val requestGalleryImages: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        {
            result ->
            //result.data is actually the location inside our storage
            if(result.resultCode == RESULT_OK && result.data != null)
            {
                val backgroundImage: ImageView = findViewById(R.id.background_image)
                //here we are accessing data(Image) inside our location(result.data)
                //by this way we wont be copying image inside our app but just referencing it
                backgroundImage.setImageURI(result.data?.data)
            }
        }

    //lets request for external storage permissions
    /*
    The val requestPermissionsLauncher of type ActivityResultLauncher
    is returned from registerForActivityResult() function, which is called on an Activity
    the result will be delivered to the registered lambda expression for processing.
     */
    val requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions())
        {
            permissions ->
            permissions.entries.forEach()
            {
                val permissionName = it.key
                val isGranted = it.value

                if(isGranted)
                {
                    Toast.makeText(this, "Permission to Read media images Granted",
                        Toast.LENGTH_SHORT).show()
                    val pickIntent = Intent(Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    requestGalleryImages.launch(pickIntent)
                }
                else
                {
                    if(permissionName == Manifest.permission.READ_MEDIA_IMAGES)
                    {
                        Toast.makeText(this, "Permission To read media images denied",
                            Toast.LENGTH_SHORT).show()
                    }
                    else
                    {
                        Toast.makeText(this, "Permission write external storage denied",
                            Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val linearLayout: LinearLayout = findViewById(R.id.color_palate)
        //by default the black color button is selected
        currentColorButton = linearLayout[0] as ImageButton
        currentColorButton!!.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.palet_selected)
        )

        drawingView = findViewById(R.id.Drawing_View)
        //drawingView?.setBrushSize(20f)

        val brushButton: ImageButton = findViewById(R.id.brush_button)
        brushButton.setOnClickListener {
            showBrushSizeDialog()
        }

        eraser = findViewById(R.id.eraser)
        eraser?.setOnClickListener {
            val eraserTag = eraser?.tag.toString()
            drawingView?.setColor(eraserTag)
            currentColorButton?.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_normal)
            )
            currentColorButton = eraser
        }

        val gallery: ImageButton = findViewById(R.id.gallery)
        gallery.setOnClickListener {
            requestStoragePermission()
        }

        val undoButton: ImageButton = findViewById(R.id.undo)
        undoButton.setOnClickListener {
            drawingView?.undoPaths()
        }

        val saveButton: ImageButton = findViewById(R.id.save)
        saveButton.setOnClickListener {
            if(isReadStorageAllowed())
            {
                lifecycleScope.launch{
                    val frameView: FrameLayout = findViewById(R.id.frameLayout)

                    saveDrawing(getBitmapFromView(frameView))
                }
            }
        }

        val shareButton: ImageButton = findViewById(R.id.share)
        shareButton.setOnClickListener {
            shareImage(ImagePath!!)
        }
    }

    //This function will take care of Brush Size dialog
    private fun showBrushSizeDialog()
    {
        //create object of Dialog class
        val brushDialog = Dialog(this)

        //how dialog should look like
        brushDialog.setContentView(R.layout.brush_size_dialog)
        brushDialog.setTitle("Brush Size: ")

        val small = brushDialog.findViewById<ImageButton>(R.id.small_brush)
        small.setOnClickListener {
            drawingView?.setBrushSize(5.toFloat())
            brushDialog.dismiss()
        }

        val medium = brushDialog.findViewById<ImageButton>(R.id.medium_brush)
        medium.setOnClickListener {
            drawingView?.setBrushSize(10.toFloat())
            brushDialog.dismiss()
        }

        val large = brushDialog.findViewById<ImageButton>(R.id.large_brush)
        large.setOnClickListener {
            drawingView?.setBrushSize(20.toFloat())
            brushDialog.dismiss()
        }

        brushDialog.show()
    }

    //This will be used inside OnClick in activity_main
    fun selectColor(view: View)
    {
        if(view != currentColorButton)
        {
            val colorButton = view as ImageButton
            val colorTag = colorButton.tag.toString()
            drawingView?.setColor(colorTag)

            colorButton.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.palet_selected)
            )
            if(currentColorButton != eraser)
                currentColorButton?.setImageDrawable(
                    ContextCompat.getDrawable(this, R.drawable.pallet_normal)
                )
            currentColorButton = colorButton

        }
    }

//    fun selectEraser(view: View)
//    {
//        val eraser = view as ImageButton
//        val colorTag = eraser.tag.toString()
//        drawingView?.setColor(colorTag)
//
//        currentColorButton = eraser
//    }

    //create a function to show alert dialog
    private fun showRationalDialog(
        title: String,
        message: String,
    ) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        builder.create().show()
    }

    private fun isReadStorageAllowed(): Boolean
    {
        val result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)

        return result == PackageManager.PERMISSION_GRANTED
    }
    private fun requestStoragePermission()
    {
        if(shouldShowRequestPermissionRationale(Manifest.permission.READ_MEDIA_IMAGES))
        {
            showRationalDialog("Drawing canvas needs Storage access","" +
                    "Access denied")
        }
        else
        {
            requestPermission.launch(
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
            )
        }

    }

    //This function will create a bitmap or an image that will be exported
    private fun getBitmapFromView(view: View): Bitmap
    {
        val myBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val myCanvas = Canvas(myBitmap)
        val myBackGround = view.background

        if(myBackGround != null)
        {
            myBackGround.draw(myCanvas)
        }
        else
        {
            myCanvas.drawColor(Color.WHITE)
        }

        view.draw(myCanvas)

        return myBitmap
    }

    private suspend fun saveDrawing(mBitmap: Bitmap): String
    {
        var result = ""
        //run this on IO thread
        withContext(Dispatchers.IO)
        {
            if(mBitmap != null)
            {
                try{
                    //create a output stream
                    val bytes = ByteArrayOutputStream()
                    //compress your bitmap into jpeg format and
                    // Write a compressed version of the bitmap to the specified outputstream
                    mBitmap.compress(Bitmap.CompressFormat.JPEG, 90, bytes)


                    val file = File(externalCacheDir?.absoluteFile.toString() + File.separator
                    + "Drawing_Canvas" + System.currentTimeMillis()/1000 + ".jpeg")

                    val fileOut = FileOutputStream(file)
                    fileOut.write(bytes.toByteArray())
                    fileOut.close()

                    result = file.absolutePath
                    //result = (Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)).absolutePath

                    runOnUiThread {
                        if(result.isNotEmpty())
                        {
                            Toast.makeText(this@MainActivity, "File saved at $result",
                                Toast.LENGTH_SHORT).show()
                        }
                        else
                        {
                            Toast.makeText(this@MainActivity, "Couldn't save file",
                                Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                catch(e:Exception){
                    result = ""
                    e.printStackTrace()
                }
            }
        }
        ImagePath = result
        return result
    }

    private fun shareImage(result: String)
    {
        MediaScannerConnection.scanFile(this, arrayOf(result), null)
        { path, uri ->
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent.type = "image/jpeg"
            startActivity(Intent.createChooser(shareIntent, "Share"))
        }
    }

}
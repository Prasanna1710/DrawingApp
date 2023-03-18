package psv.app.drawing_canvas

import android.Manifest
import android.app.Dialog
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.io.IOException



class MainActivity : AppCompatActivity() {
    private lateinit var myDrawingView: MyDrawingView
    private lateinit var colorPallet: LinearLayout
    private lateinit var eraserButton: ImageButton
    private lateinit var currentColorButton: ImageButton
    private lateinit var brushSizeSelectButton: ImageButton
    private lateinit var undoButton: ImageButton
    private lateinit var clearCanvas: ImageButton
    private lateinit var saveButton: ImageButton
    private lateinit var bitmapToSave: Bitmap

    private lateinit var imageName: String

    private var isReadPermissionGranted = false
    private var isWritePermissionGranted = false
    val sdkLevel = Build.VERSION.SDK_INT

    /*the multiplePermissionLauncher gives an hashmap of permission name and whether its granted or not
    we take the values from hashmap from key- value pair and assign them to
    isReadPermissionGranted and isWritePermissionGranted
     */
    private val multiplePermissionLauncher : ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions())
        { permissions ->
            if (sdkLevel <= Build.VERSION_CODES.TIRAMISU) {
                isReadPermissionGranted =
                    permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: isReadPermissionGranted
            }
            else
            {
                isReadPermissionGranted =
                    permissions[Manifest.permission.READ_MEDIA_IMAGES] ?: isReadPermissionGranted
            }

            isWritePermissionGranted =
                permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] ?: isReadPermissionGranted
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestPermission()

        myDrawingView = findViewById(R.id.Drawing_View)
        colorPallet = findViewById(R.id.color_palate)

        currentColorButton = colorPallet[0] as ImageButton
        currentColorButton.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.palet_selected)
        )

        brushSizeSelectButton = findViewById(R.id.brush_button)
        brushSizeSelectButton.setOnClickListener {
            brushSizeChooserDialog()
        }

        eraserButton = findViewById(R.id.eraser)
        eraserButton.setOnClickListener {
            if(currentColorButton != eraserButton)
            {
                currentColorButton.setImageDrawable(
                    ContextCompat.getDrawable(this, R.drawable.pallet_normal)
                )
                currentColorButton = eraserButton
            }
            val colorTag = eraserButton.tag.toString()
            myDrawingView.setColor(colorTag)
        }

        undoButton = findViewById(R.id.undo)
        undoButton.setOnClickListener {
            myDrawingView.undoPath()
        }

        clearCanvas = findViewById(R.id.clear_canvas)
        clearCanvas.setOnClickListener {
            myDrawingView.clearCanvas()
        }

        saveButton = findViewById(R.id.save)
        saveButton.setOnClickListener {

            bitmapToSave = getBitmapFromView(myDrawingView)
            lifecycleScope.launch {
                if(isWritePermissionGranted)
                {
                    imageName = "DrawingCanvas_${System.currentTimeMillis()}"
                    if(savePhoto(imageName, bitmapToSave))
                    {
                        Toast.makeText(
                            this@MainActivity, "Image Saved", Toast.LENGTH_SHORT).show()
                    }
                    else
                    {
                        Toast.makeText(
                            this@MainActivity, "Image Not Saved", Toast.LENGTH_SHORT).show()
                    }
                }
                else
                {
                    Toast.makeText(
                        this@MainActivity, "Write Permission Not Granted", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /*
    The function selectColor selects the color of paint and customPath inside MyDrawingView
    it also sets the UI of the image button which is selected and not selected
     */
    fun selectColor(view: View)
    {
        if(view != currentColorButton)
        {
            val colorButton = view as ImageButton
            val colorTag = colorButton.tag.toString()

            myDrawingView.setColor(colorTag)

            colorButton.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.palet_selected)
            )

            if(currentColorButton != eraserButton)
            {
                currentColorButton.setImageDrawable(
                    ContextCompat.getDrawable(this, R.drawable.pallet_normal)
                )
            }

            currentColorButton = colorButton
        }
    }

    /*
    BrushSizeChooserDialog chooses the size/thickness of brush and in turn Paint.strokeWidth inside
    MyDrawingView
    This function shows a custom Dialog of 3 buttons, each of 3 buttons show a different brush size
     */
    private fun brushSizeChooserDialog()
    {
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.brush_size_dialog)
        brushDialog.setTitle("Brush Size: ")

        val smallSizeButton: ImageButton = brushDialog.findViewById(R.id.small_brush)
        smallSizeButton.setOnClickListener {
            myDrawingView.setBrushSize(5.toFloat())
            brushDialog.dismiss()
        }

        val mediumSizeButton: ImageButton = brushDialog.findViewById(R.id.medium_brush)
        mediumSizeButton.setOnClickListener{
            myDrawingView.setBrushSize(10.toFloat())
            brushDialog.dismiss()
        }

        val largeSizeButton: ImageButton = brushDialog.findViewById(R.id.large_brush)
        largeSizeButton.setOnClickListener {
            myDrawingView.setBrushSize(15.toFloat())
            brushDialog.dismiss()
        }
        brushDialog.show()
    }

    /*
    In this function we get a bitmap from the Drawing_View in UI
    This bitmap can be later used to save drawings as image
     */
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

    /*
    request permission checks the current android version and according to that
    it decides which permissions to ask and which are automatically granted
     */
    fun requestPermission() {
        val SdkLevel = Build.VERSION.SDK_INT
        val isReadPermission: Boolean
        val isWritePermission: Boolean
        val readPermisssion: String

        if (SdkLevel >= Build.VERSION_CODES.Q) {
            isWritePermission = true
            if (SdkLevel < Build.VERSION_CODES.TIRAMISU) {
                isReadPermission = ContextCompat.checkSelfPermission(
                    this, Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
                readPermisssion = Manifest.permission.READ_EXTERNAL_STORAGE
            } else {
                isReadPermission = ContextCompat.checkSelfPermission(
                    this, Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED
                readPermisssion = Manifest.permission.READ_MEDIA_IMAGES
            }
        } else {
            isWritePermission = ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED

            isReadPermission = ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            readPermisssion = Manifest.permission.READ_EXTERNAL_STORAGE
        }

        isReadPermissionGranted = isReadPermission
        isWritePermissionGranted = isWritePermission

        val permissions = mutableListOf<String>()
        if (!isReadPermissionGranted) {
            permissions.add(readPermisssion)
        }
        if (!isWritePermission) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        if(permissions.isNotEmpty())
        {
            multiplePermissionLauncher.launch(permissions.toTypedArray())
        }
    }

    /*
    function saves a Bitmap image to the device's media store.
    It takes a name for the image and the Bitmap image itself as parameters.
    It returns a boolean indicating whether the image was saved successfully or not.

    If android version is greater than 10 function uses
    MediaStore.Images.Media.getContentUri() method with the MediaStore.VOLUME_EXTERNAL_PRIMARY parameter to get the collection.
    Otherwise, it uses the MediaStore.Images.Media.EXTERNAL_CONTENT_URI collection.

    It creates a ContentValues object and sets the following values:
    MediaStore.Images.Media.DISPLAY_NAME: The name of the image file to be saved.
    MediaStore.Images.Media.MIME_TYPE: The MIME type of the image file, in this case "image/jpeg".
    MediaStore.Images.Media.WIDTH: The width of the image, if the image is not null.
    MediaStore.Images.Media.HEIGHT: The height of the image, if the image is not null.

    It tries to insert the image metadata into the media store collection using the contentResolver.insert() method.
    This method returns a Uri object that represents the newly created image entry in the media store.
    If the insertion is successful, the function opens an output stream to the Uri using contentResolver.openOutputStream().
    It then compresses the Bitmap image into the output stream using JPEG format with a quality of 95.
    If the compression is successful,
    the function returns true. Otherwise, it throws an IOException and returns false.
     */
    private fun savePhoto(name: String, image: Bitmap?) : Boolean{
        val imageCollection: Uri = if(sdkLevel >= Build.VERSION_CODES.Q)
        {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }
        else{
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$name.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if(image != null)
            {
                put(MediaStore.Images.Media.WIDTH, image.width)
                put(MediaStore.Images.Media.HEIGHT, image.height)
            }
        }

        return try{
            contentResolver.insert(imageCollection, contentValues)?.also {
                contentResolver.openOutputStream(it).use {outputStream ->
                    if(image != null)
                    {
                        if(! image.compress(Bitmap.CompressFormat.JPEG, 95, outputStream))
                        {
                            throw IOException("Failed to save Bitmap")
                        }
                    }
                }
            } ?: throw IOException("Failed to create mediastore entry")
            true
        }catch (e: IOException)
        {
            e.printStackTrace()
            false
        }
    }
}
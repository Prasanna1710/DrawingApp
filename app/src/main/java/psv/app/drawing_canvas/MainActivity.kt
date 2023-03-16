package psv.app.drawing_canvas

import android.app.Dialog
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.get


class MainActivity : AppCompatActivity() {
    private lateinit var myDrawingView: MyDrawingView
    private lateinit var colorPallet: LinearLayout
    private lateinit var eraserButton: ImageButton
    private lateinit var currentColorButton: ImageButton
    private lateinit var brushSizeSelectButton: ImageButton
    private lateinit var undoButton: ImageButton
    private lateinit var clearCanvas: ImageButton
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
    }

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
}
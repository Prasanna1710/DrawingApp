package psv.app.drawing_canvas

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.Build.VERSION_CODES.M
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration

class MyDrawingView(context: Context, attributes: AttributeSet): View(context, attributes) {

    internal inner class CustomPath(var color: Int, var brushSize: Float): Path()
    {

    }

    private lateinit var myCanvas: Canvas
    private lateinit var myBitmap: Bitmap
    private lateinit var myPath: CustomPath
    private lateinit var drawPaint: Paint
    private lateinit var canvasPaint: Paint
    private val myPathList = ArrayList<CustomPath>()
    private val myUndoPathList = ArrayList<CustomPath>()

    private var drawColor = Color.BLACK
    private var strokeSize = 12f

    private var currentX = 0f
    private var currentY = 0f
    private var motionTouchEventX = 0f
    private var motionTouchEventY = 0f

    private val touchTolerance = ViewConfiguration.get(context).scaledTouchSlop

    init {
        setDrawingElements()
    }

    private fun setDrawingElements()
    {
        drawPaint = Paint()
        drawPaint.color = drawColor
        drawPaint.strokeWidth = strokeSize
        drawPaint.strokeJoin = Paint.Join.ROUND
        drawPaint.style = Paint.Style.STROKE
        drawPaint.strokeCap = Paint.Cap.ROUND
        drawPaint.isDither = true
        drawPaint.isAntiAlias = true

        canvasPaint = Paint(Paint.DITHER_FLAG)

        myPath = CustomPath(drawColor, strokeSize)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        myBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        myCanvas = Canvas(myBitmap)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(myBitmap, 0f, 0f, canvasPaint)

        for(path in myPathList)
        {
            drawPaint.strokeWidth = path.brushSize
            drawPaint.color = path.color
            canvas.drawPath(path, drawPaint)
        }

        if(!myPath.isEmpty)
        {
            drawPaint.strokeWidth = myPath.brushSize
            drawPaint.color = myPath.color
            canvas.drawPath(myPath, drawPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        motionTouchEventX = event.x
        motionTouchEventY = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> touchStart()
            MotionEvent.ACTION_MOVE -> touchMove()
            MotionEvent.ACTION_UP -> touchUp()
            else -> return false
        }

        invalidate()
        return true
    }

    private fun touchStart()
    {
        myPath.brushSize = strokeSize
        myPath.color = drawColor

        myPath.reset()
        myPath.moveTo(motionTouchEventX, motionTouchEventY)
        currentX = motionTouchEventX
        currentY = motionTouchEventY
    }

    private fun touchMove()
    {
        val dx = Math.abs(motionTouchEventX - currentX)
        val dy = Math.abs(motionTouchEventY - currentY)
        if (dx >= touchTolerance || dy >= touchTolerance) {
            // QuadTo() adds a quadratic bezier from the last point,
            // approaching control point (x1,y1), and ending at (x2,y2).
            myPath.quadTo(currentX, currentY, (motionTouchEventX + currentX) / 2, (motionTouchEventY + currentY) / 2)
            currentX = motionTouchEventX
            currentY = motionTouchEventY
            // Draw the path in the extra bitmap to save it.
            //myCanvas.drawPath(myPath, drawPaint)
        }
    }

    private fun touchUp()
    {
        myPathList.add(myPath)
        myPath = CustomPath(drawColor, strokeSize)
        //myPathList.add(myPath)
        //myPath.reset()
    }

    fun setColor(newColor: String)
    {
        drawColor = Color.parseColor(newColor)
        drawPaint.color = drawColor
    }

    fun setBrushSize(newSize: Float)
    {
        strokeSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, newSize,
            resources.displayMetrics)
        drawPaint.strokeWidth = strokeSize
    }

    fun undoPath()
    {
        if(myPathList.isNotEmpty())
        {
            myUndoPathList.add(myPathList.removeAt(myPathList.size - 1))
            invalidate()
        }
    }

    fun clearCanvas()
    {
        myPathList.removeAll(myPathList.toSet())
        invalidate()
    }
}
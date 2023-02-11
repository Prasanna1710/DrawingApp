//This class will be added as a view inside xml file

package psv.app.drawing_canvas

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View

/*We are going to use this class as a view and not an activity
This will be visible inside mainActivity
This class will inherit from View class
 */

class DrawingView(context: Context, attribute: AttributeSet):
    View(context, attribute)
{
       /*Our Primary goal is to draw on this view
       For This purpose we may need the following things for example
       1. Colors
       2. Brush Sizes
       3. Undo or eraser
       4. a bitmap file to draw on*/

    private var mDrawPath : CustomPath? = null
    private var mCanvasBitmap: Bitmap? = null
    private var mDrawPaint: Paint? = null
    private var mCanvasPaint: Paint? = null
    //after adding the function we select brushsize from MainActivity
    private var mBrushSize: Float = 5.toFloat()
    private var mColor = Color.BLACK
    private var mCanvas: Canvas? = null

    //this stores all the paths drawn on canvas
    private val mPaths = ArrayList<CustomPath>()
    private val mUndoPaths = ArrayList<CustomPath>()

    init {
        setDrawingElements()
    }

    //In undo function we delete an entry from mPaths and add it to undo paths
    fun undoPaths()
    {
        if(mPaths.size != 0)
        {
            mUndoPaths.add(mPaths.removeAt(mPaths.size - 1))
            //invalidate will internally call onDraw
            invalidate()
        }
    }


    private fun setDrawingElements()
    {
        //Enabling this flag applies a dither to any blit operation
        // where the target's colour space is more constrained than the source
        mCanvasPaint = Paint(Paint.DITHER_FLAG)

        //mBrushSize = 20.toFloat()
        mDrawPath = CustomPath(mColor, mBrushSize)
        mDrawPaint = Paint()
        mDrawPaint!!.color = mColor
        mDrawPaint!!.style = Paint.Style.STROKE

        /*
        strokeJoin is The kind of finish to place on the joins between segments.
        This applies to paths drawn when style is set to PaintingStyle.stroke
         */
        mDrawPaint!!.strokeJoin = Paint.Join.ROUND

        //strokeCap specifies how a stroke must end
        mDrawPaint!!.strokeCap = Paint.Cap.ROUND
    }

    //onSizeChange is inside view class so we can override it
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        /*
        Bitmap.createBitmap is a static method in the Android Bitmap class
        that creates a new bitmap by copying or scaling an existing bitmap.

        Bitmap.Config.ARGB_8888 is a configuration type for the Android Bitmap class.
        It represents a bitmap that has 8 bits per color channel,
        for red, green, blue, and alpha (transparency),
        for a total of 32 bits per pixel.

        This type of configuration allows for the highest color depth and transparency precision in a bitmap.
         */
        mCanvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)

        //We have our bitmap and configured it using Bitmap.Config
        //Now we will create a canvas object
        mCanvas = Canvas(mCanvasBitmap!!)
    }

    //In below method we specify what should happen when we want to draw
    override fun onDraw(canvas: Canvas) { //Canvas should be not nullable
        super.onDraw(canvas)
        mCanvasBitmap.let {
            canvas.drawBitmap(mCanvasBitmap!!, 0f, 0f, mCanvasPaint)
        }

        //The array with elements added from ACTION_UP is traversed here
        //The color and strokewidth is determined for each element
        for(path in mPaths)
        {
            mDrawPaint?.strokeWidth = path.brushThickness
            mDrawPaint?.color = path.color
            canvas.drawPath(path,mDrawPaint!!)
        }

        //creating a draw path
        if(!mDrawPath!!.isEmpty) //if mDrawPath is not empty
        {
            mDrawPaint?.strokeWidth = mDrawPath!!.brushThickness
            mDrawPaint?.color = mDrawPath!!.color
            canvas.drawPath(mDrawPath!!, mDrawPaint!!)
        }
    }

    //this function specifies when should content be drawn on touch
    override fun onTouchEvent(event: MotionEvent?): Boolean
    {
        val touchX = event?.x
        val touchY = event?.y

        /*
        The motion event have 3 actions
        1. when we touch screen / moment of contact
        2. when we drag finger across screen
        3. when we take finger away
         */
        when(event?.action) {
            MotionEvent.ACTION_DOWN -> {
                mDrawPath?.color = mColor
                mDrawPath?.brushThickness = mBrushSize
                mDrawPath?.reset()

                if (touchX != null && touchY != null) {
                    mDrawPath!!.moveTo(touchX, touchY)
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (touchX != null && touchY != null) {
                    mDrawPath?.lineTo(touchX, touchY)
                }
            }

            MotionEvent.ACTION_UP -> {
                mDrawPath = CustomPath(mColor, mBrushSize)
                //when we take finger up we add paths to array
                mPaths.add(mDrawPath!!)
            }

            else -> return false
        }

        invalidate()
        return true
    }

    /*
    We want to be able to select brush size
     */
    fun setBrushSize(newSize: Float)
    {
        /*
        We cant just assign mBrushSize to newSize
        we first need to consider screen dimension
         */
        mBrushSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
            newSize, resources.displayMetrics)
        mDrawPaint!!.strokeWidth = mBrushSize
    }

    //This function sets the color of brush
    //This function will be called inside main activity
    fun setColor(newColor:String)
    {
        mColor = Color.parseColor(newColor)
        mDrawPaint!!.color = mColor
    }

    /*
    bring internal inner class this class can only be used inside this file
    we have extended this class from class path of android graphics
    and added some new properties like color and brushThickness*/
    internal inner class CustomPath(var color: Int, var brushThickness: Float)
        : android.graphics.Path()
    {

    }


}
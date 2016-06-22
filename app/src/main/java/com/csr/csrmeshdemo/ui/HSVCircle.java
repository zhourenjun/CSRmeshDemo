
/******************************************************************************
 Copyright Cambridge Silicon Radio Limited 2014 - 2015.
 ******************************************************************************/

package com.csr.csrmeshdemo.ui;

import android.app.ActionBar.LayoutParams;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

public class HSVCircle extends View {
    private static final float CURSOR_RADIUS = 3.0f;

    private Paint mHsvPaint = new Paint();
    private Paint mCursorPaint = new Paint();
    private int mCursorX = 0;
    private int mCursorY = 0;
    private boolean mShowCursor = false;    
    
    private Bitmap mCanvasBitmap = null;
    private RadialGradient mRadialShader = null;
    private SweepGradient mSweepShader = null;
    private ComposeShader mCombinedShader = null;
    private Canvas mBitmapCanvas = null;

    private int mCanvasWidth = 0;
    private int mCanvasHeight = 0;
    private int mCanvasMin = 0;

    private static final int ENABLED_ALPHA = 255;
    private static final int DISABLED_ALPHA = 60;
    
    private static final int NUM_HUES = 13;
    private static final int MAX_HUE = 360;
    private static final int HUE_INCREMENT = (MAX_HUE / (NUM_HUES - 1));
    
    private int mColors[] = new int[NUM_HUES];
    private float mHsv[] = new float[3];
    
    public HSVCircle(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setLayerType(LAYER_TYPE_SOFTWARE, null);
        mHsvPaint.setDither(true);
        // Start hue at zero. Max value of hue is 360.
        mHsv[0] = 0;
        // Saturation and value will be fixed at 1 (the max).
        mHsv[1] = 1.0f;
        mHsv[2] = 1.0f;
        // Create NUM_HUES different hues by modifying mHsv[0].        
        for (int i = 0; i < NUM_HUES - 1; i++) {
            mColors[i] = Color.HSVToColor(mHsv);
            mHsv[0] += HUE_INCREMENT;
        }
        // First hue should be the same as the last.
        mColors[NUM_HUES - 1] = mColors[0];

        // Set up Paint object for drawing the cursor.
        mCursorPaint.setColor(Color.BLACK);
        mCursorPaint.setStrokeWidth(2.0f);
        mCursorPaint.setStyle(Paint.Style.STROKE);
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (mCanvasWidth != canvas.getWidth() || mCanvasHeight != canvas.getHeight()) {            
            // Only create new objects if Canvas size changes.
            mCanvasWidth = canvas.getWidth();
            mCanvasHeight = canvas.getHeight();
            mCanvasMin = Math.min(mCanvasWidth, mCanvasHeight);
            if (mCanvasBitmap != null) {
                mCanvasBitmap.recycle();
            }
            mCanvasBitmap = Bitmap.createBitmap(mCanvasWidth, mCanvasHeight, Bitmap.Config.ARGB_8888);
            mBitmapCanvas = new Canvas(mCanvasBitmap);
            // Create a radial gradient with max saturation at the center of the circle (alpha = 0)
            // and minimum saturation at the edge (alpha = 255).
            // The alpha value of the radial gradient when combined with the sweep gradient will create the
            // appropriate saturation.
            // The radius is half the length of the shortest dimension of the canvas.
            mRadialShader =
                    new RadialGradient(mCanvasWidth / 2, mCanvasHeight / 2, mCanvasMin / 2, 0xFFFFFFFF, 0x00FFFFFF,
                            android.graphics.Shader.TileMode.CLAMP);
            // Create a sweep gradient using the colors array. The hue will then vary with the degrees from zero.
            mSweepShader = new SweepGradient(mCanvasWidth / 2, mCanvasHeight / 2, mColors, null);
            // Combine the radial and sweep shaders.
            mCombinedShader = new ComposeShader(mSweepShader, mRadialShader, PorterDuff.Mode.SRC_OVER);
            mHsvPaint.setShader(mCombinedShader);
            // The HSV circle only needs to be drawn to the bitmap every time the canvas size changes.
            mBitmapCanvas.drawCircle(mCanvasWidth / 2, mCanvasHeight / 2, mCanvasMin / 2, mHsvPaint);                        
        }
        if (isEnabled()) {
            mHsvPaint.setAlpha(ENABLED_ALPHA);
        }
        else {
            mHsvPaint.setAlpha(DISABLED_ALPHA);
        }
        canvas.drawBitmap(mCanvasBitmap, 0.0f, 0.0f, mHsvPaint);
        
        // Draw the cursor if its position has been set.
        if (mShowCursor) {
            canvas.drawCircle(mCursorX, mCursorY, CURSOR_RADIUS, mCursorPaint);
        }
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        
        if (getLayoutParams().height == LayoutParams.WRAP_CONTENT) {
            setMeasuredDimension(width, width);
        }
        else {
            setMeasuredDimension(width, height);
        }
    }
    
    /**
     * Should be called to free memory when the host Activity or Fragment is done with the View.
     */
    public void onDestroyView() {
        if (mCanvasBitmap != null) mCanvasBitmap.recycle();
    }

    /**
     * Get the pixel colour at a position on the circle.
     * 
     * @param x
     *            X coordinate
     * @param y
     *            Y cordinate
     * @return Non pre-multipled ARGB colour value.
     */
    public int getPixelColorAt(int x, int y) {
        if (x < 0 || y < 0 || x >= mCanvasBitmap.getWidth() || y >= mCanvasBitmap.getHeight()) {
            throw new IndexOutOfBoundsException("Coordinates are outside bitmap bounds");
        }        
        return mCanvasBitmap.getPixel(x, y);
    }

    /**
     * Set the position of the cursor that will be drawn over the colour wheel as a small black circle.
     * 
     * @param x
     *            X coordinate.
     * @param y
     *            Y coordinate.
     */
    public void setCursor(int x, int y) {
        if (x == 0 && y == 0) {
            mShowCursor = false;
        }
        else {
            mShowCursor = true;
            this.mCursorX = x;
            this.mCursorY = y;
        }
    }

    /**
     * Set the position of the cursor on the colour wheel based on a hue and saturation.
     * 
     * @param hue
     *            Hue of colour (range 0...360).
     * @param saturation
     *            Saturation of colour (range 0...1).
     */
    public void setCursor(float hue, float saturation) {
        mShowCursor = true;
        double cX = (double)(mCanvasWidth / 2);
        double cY = (double)(mCanvasHeight / 2);
        double radius = (double)(mCanvasMin / 2);
        // Scale radius by saturation, which is in the range 0...1
        radius *= saturation;
        // Calculate x and y position based on degrees from zero. Red is at 0 degrees so the hue can be used directly.
        this.mCursorX = (int)(cX + radius * Math.cos(hue * Math.PI / 180));
        this.mCursorY = (int)(cY + radius * Math.sin(hue * Math.PI / 180));
    }
}
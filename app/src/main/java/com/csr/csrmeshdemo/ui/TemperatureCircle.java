
/******************************************************************************
 Copyright Cambridge Silicon Radio Limited 2014 - 2015.
 ******************************************************************************/

package com.csr.csrmeshdemo.ui;

import android.app.ActionBar.LayoutParams;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.graphics.Typeface;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.csr.csrmeshdemo.R;
import com.csr.csrmeshdemo.TemperatureControllerInterface;

public class TemperatureCircle extends View {
	
	// paint objects
    private Paint mExternalCirclePaint = new Paint();
    private Paint mButtonPaint = new Paint();
    private Paint mTextPaint = new Paint();
    private Paint mInternalCirclePaint = new Paint();
    
    // interface
    private TemperatureControllerInterface mTemperatureControllerInterface = null;
    
    // measure objects
    private int mCanvasWidth = 0;
    private int mCanvasHeight = 0;
    private int mCanvasMin = 0;
    
    // UI objects
    Rect mButtonUp,mButtonDown, mTextRect, mClickable;
    Bitmap mBmpUp, mBmpDown;
    String mValue ="";
    float mDesiredTextSize =0;
    boolean isDesiredTemperatureConfirmed = false;

    // Context
    Context mContext;
    
    // state used for touch event purposes
    int clickState = STATE_NONE;
    static final int STATE_NONE = 0;
    static final int STATE_DOWN_CLICKED = -1;
    static final int STATE_UP_CLICKED = 1;
    static final int STATE_TEXT_CLICKED = 2;
    
    // colours for text
    static final int COLOR_CLICKED = Color.WHITE;
    static final int COLOR_NON_CLICKED = 0xffc6c6c6;
    static final int TEXT_COLOR = 0xffc6c6c6;
    static final int TEXT_LABEL_COLOR = 0xff8D8C8C;
    static final int TEXT_LABEL_COLOR_UNCONFIRMED = 0xffC36900;
    
    // colours for circles
    static final int COLOR_EXTERNAL_CIRCLE_1 = 0xFFE91919;
    static final int COLOR_EXTERNAL_CIRCLE_2 = 0xFF0171BB;
    static final int COLOR_INTERNAL_CIRCLE_1 = 0xFFFBFBFC;
    static final int COLOR_INTERNAL_CIRCLE_2 = 0xFFDCDBDA;
    
    private static final double INTERNAL_CIRCLE_PADDING_PERCENT = 0.08;
    private static final int DESIRED_TEMPERATURE_TEXT_SIZE = 50;
    
    public TemperatureCircle(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Set context
        mContext = context;
        
        // Set style for the external circle
        mExternalCirclePaint.setDither(true);
        
        // Set style and colour for the internal circle
        mInternalCirclePaint.setDither(true);
        mInternalCirclePaint.setShadowLayer(20.0f, 0.0f, 2.0f, 0xFFFFFFFF);  
       
        // Set the bitmaps
        mBmpUp =  BitmapFactory.decodeResource(getResources(),R.drawable.ic_navigation_expand_less);
        mBmpDown =  BitmapFactory.decodeResource(getResources(),R.drawable.ic_navigation_expand_more);
        
        // Set style for the texts
        Typeface tf = Typeface.create("Helvetica",Typeface.ITALIC);
        mTextPaint.setTypeface(tf);
        mTextPaint.setStrokeWidth(0);
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setTextAlign(Align.CENTER);
        mTextPaint.setShadowLayer(10.0f, 0.0f, 2.0f, 0xFFFFFFFF);  
        mTextPaint.setColor(TEXT_COLOR);
        
        
    }

    @Override
    public void onDraw(Canvas canvas) {
            // Only create new objects if Canvas size changes.
        	if (mButtonUp == null) {
	            mCanvasWidth = canvas.getWidth();
	            mCanvasHeight = canvas.getHeight();
	            mCanvasMin = Math.min(mCanvasWidth, mCanvasHeight);
	            
	            mButtonUp = getButtonRect(true);  
	            mButtonDown = getButtonRect(false);
	            mTextRect = getTextRect();
	            mClickable = new Rect((int)((INTERNAL_CIRCLE_PADDING_PERCENT)* mCanvasMin / 2),(int)((INTERNAL_CIRCLE_PADDING_PERCENT)* mCanvasMin / 2 + mBmpUp.getHeight()), (int)(mCanvasWidth -(INTERNAL_CIRCLE_PADDING_PERCENT)* mCanvasMin / 2) , (int)(mCanvasHeight - (INTERNAL_CIRCLE_PADDING_PERCENT)* mCanvasMin / 2 - mBmpUp.getHeight()));
	            
        	}
        	
        	// draw external circle after applying gradient colour.
        	Shader shaderExt = new LinearGradient(0, 0, mCanvasWidth, mCanvasHeight, COLOR_EXTERNAL_CIRCLE_1, COLOR_EXTERNAL_CIRCLE_2, TileMode.CLAMP);
        	mExternalCirclePaint.setShader(shaderExt);
        	canvas.drawCircle(mCanvasWidth / 2, mCanvasHeight / 2, mCanvasMin / 2, mExternalCirclePaint);

        	// draw internal circle after applying gradient colour.
        	Shader shader = new LinearGradient(0, 0, mCanvasWidth, mCanvasHeight, COLOR_INTERNAL_CIRCLE_1, COLOR_INTERNAL_CIRCLE_2, TileMode.CLAMP);
            mInternalCirclePaint.setShader(shader);
            canvas.drawCircle(mCanvasWidth / 2, mCanvasHeight / 2, (float)((1.0 - INTERNAL_CIRCLE_PADDING_PERCENT)* mCanvasMin / 2) , mInternalCirclePaint);
            
            // draw bitmaps according to the current click status.
            if (clickState == STATE_UP_CLICKED) {
            	mButtonPaint.setColorFilter(new LightingColorFilter(COLOR_CLICKED, 1));
            	canvas.drawBitmap(mBmpUp, null, mButtonUp, mButtonPaint);
            	mButtonPaint.setColorFilter(new LightingColorFilter(COLOR_NON_CLICKED, 1));
            	canvas.drawBitmap(mBmpDown, null, mButtonDown , mButtonPaint);
            } else if (clickState == STATE_DOWN_CLICKED) {
            	mButtonPaint.setColorFilter(new LightingColorFilter(COLOR_CLICKED, 1));
            	canvas.drawBitmap(mBmpDown, null, mButtonDown , mButtonPaint);
            	mButtonPaint.setColorFilter(new LightingColorFilter(COLOR_NON_CLICKED, 1));
            	canvas.drawBitmap(mBmpUp, null, mButtonUp, mButtonPaint);
            } else {
            	mButtonPaint.setColorFilter(null);
            	mButtonPaint.setColorFilter(new LightingColorFilter(COLOR_NON_CLICKED, 1));
            	canvas.drawBitmap(mBmpUp, null, mButtonUp, mButtonPaint);
                canvas.drawBitmap(mBmpDown, null, mButtonDown , mButtonPaint);
            }
            
            // draw the texts
            drawTextandLabel(mValue, canvas);
            
    }
    
    private Rect getTextRect() {
    	
    	
		return new Rect(0,0,mCanvasWidth, mCanvasHeight);
	}

	private Rect getButtonRect(boolean above) {
    	if (above) {
    		return new Rect((mCanvasWidth / 2 - mBmpDown.getWidth() /2), (int)((INTERNAL_CIRCLE_PADDING_PERCENT)* mCanvasMin / 2), (mCanvasWidth / 2 + mBmpDown.getWidth() /2), (int)((INTERNAL_CIRCLE_PADDING_PERCENT)* mCanvasMin / 2 + mBmpDown.getHeight()));
    	} else {
    		return new Rect(mCanvasWidth / 2 - mBmpUp.getWidth() /2, mCanvasHeight - (int)((INTERNAL_CIRCLE_PADDING_PERCENT)* mCanvasMin / 2) - mBmpUp.getHeight(),mCanvasWidth / 2 + mBmpUp.getWidth() /2, mCanvasHeight - (int)((INTERNAL_CIRCLE_PADDING_PERCENT)* mCanvasMin / 2) );
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
    }

    public static int LONG_PRESS_TIME = 500; // Time in miliseconds 

    final Handler _handler = new Handler(); 
    Runnable _longPressed = new Runnable() { 
        public void run() {
            Log.i("info","LongPress");
        }   
    };
    
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        
        final int action = ev.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
        case MotionEvent.ACTION_DOWN: {
        	if (mButtonUp.contains((int)ev.getX(), (int)ev.getY())) {
        		clickState = STATE_UP_CLICKED;
        		_handler.postDelayed(_longPressed, LONG_PRESS_TIME);
        	} else if (mButtonDown.contains((int)ev.getX(), (int)ev.getY())) {
        		clickState = STATE_DOWN_CLICKED;
        		_handler.postDelayed(_longPressed, LONG_PRESS_TIME);
        	} else if (mClickable.contains((int)ev.getX(), (int)ev.getY())) {
        		clickState = STATE_TEXT_CLICKED;
        	}else {
        		clickState = STATE_NONE;
        	}
        	postInvalidate();
        	break;
        }
        case MotionEvent.ACTION_MOVE: {
        	if (clickState == STATE_UP_CLICKED && !mButtonUp.contains((int)ev.getX(), (int)ev.getY())) {
        		clickState = STATE_NONE;
        	} else if (clickState == STATE_DOWN_CLICKED && !mButtonDown.contains((int)ev.getX(), (int)ev.getY())) {
        		clickState = STATE_NONE;
        	} else if (clickState == STATE_TEXT_CLICKED && !mClickable.contains((int)ev.getX(), (int)ev.getY())) {
        		clickState = STATE_NONE;
        	} 
        	postInvalidate();
        	break;
        }
        case MotionEvent.ACTION_UP: {
        	if (clickState == STATE_UP_CLICKED && mButtonUp.contains((int)ev.getX(), (int)ev.getY())) {
        		if (mTemperatureControllerInterface != null) {
        			mTemperatureControllerInterface.buttonUpClicked();
        		}
        	} else if (clickState == STATE_DOWN_CLICKED && mButtonDown.contains((int)ev.getX(), (int)ev.getY())) {
        		if (mTemperatureControllerInterface != null) {
        			mTemperatureControllerInterface.buttonDownClicked();
        		}
        	} else if (clickState == STATE_TEXT_CLICKED && mClickable.contains((int)ev.getX(), (int)ev.getY())) {
        		if (mTemperatureControllerInterface != null) {
        			mTemperatureControllerInterface.buttonTextClicked();
        		}
        	}
        	clickState = STATE_NONE;
        	postInvalidate();
            break;
        }
        }
        
        return true;
    }
    

    /**
     * Draw the desired temperature value and its label.
     * @param text Desired temperature to draw.
     * @param canvas Canvas.
     */
    private void drawTextandLabel(String text, Canvas canvas) {

    	// calculate desiredSize if we don't have the value yet.
    	if (mDesiredTextSize == 0) {
    		mDesiredTextSize = setTextSizeForWidth(mTextPaint, mTextRect.width()/2,text);
    	} else {
    		mTextPaint.setTextSize(mDesiredTextSize);
    	}
    	
    	int xPos = (mCanvasWidth / 2);
    	int yPos = (int) ((mCanvasHeight / 2) - ((mTextPaint.descent() + mTextPaint.ascent()) / 2)) ; 

    	
    	if (clickState == STATE_TEXT_CLICKED) {
        	mTextPaint.setColor(COLOR_CLICKED);
        } else {
        	mTextPaint.setColor(TEXT_COLOR);
        }
    	canvas.drawText(text, xPos, yPos, mTextPaint);
    	
    	
    	// draw the desired temperature label.
    	float size = DESIRED_TEMPERATURE_TEXT_SIZE;
    	mTextPaint.setTextSize(size);

    	mTextPaint.setColor(isDesiredTemperatureConfirmed?TEXT_LABEL_COLOR:TEXT_LABEL_COLOR_UNCONFIRMED);
        canvas.drawText(mContext.getString(R.string.desired_temperature), xPos, (yPos + mButtonDown.top)/2, mTextPaint);
    }	
    
    /**
     * Sets the text size for a Paint object so a given string of text will be a
     * given width.
     * 
     * @param paint
     *            the Paint to set the text size for
     * @param desiredWidth
     *            the desired width
     * @param text
     *            the text that should be that width
     */
    private static float setTextSizeForWidth(Paint paint, float desiredWidth,
            String text) {

        // Pick a reasonably large value for the test. Larger values produce
        // more accurate results, but may cause problems with hardware
        // acceleration. But there are workarounds for that, too; refer to
        // http://stackoverflow.com/questions/6253528/font-size-too-large-to-fit-in-cache
        final float testTextSize = 48f;

        // Get the bounds of the text, using our testTextSize.
        paint.setTextSize(testTextSize);
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);

        // Calculate the desired size as a proportion of our testTextSize.
        float desiredTextSize = testTextSize * desiredWidth / bounds.width();

        // Set the paint for that size.
        paint.setTextSize(desiredTextSize);
        
        return desiredTextSize;
    }
    
    /**
     * Set a new value of desired temperature and reDraw.
     * @param value Desired temperature.
     */
    public void setValue(String value) {
    	mValue = value;
    	postInvalidate();
    }
    
    /**
     * Set the temperature controller interface that will notify to the activity.
     * @param controllerInterface
     */
    public void setTemperatureInterface(TemperatureControllerInterface controllerInterface) {
    	mTemperatureControllerInterface = controllerInterface;
    }

    public void setValueConfirmed(boolean confirmed) {
        isDesiredTemperatureConfirmed = confirmed;
        postInvalidate();
    }
}
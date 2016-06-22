
/******************************************************************************
 Copyright Cambridge Silicon Radio Limited 2014 - 2015.
 ******************************************************************************/

package com.csr.csrmeshdemo;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import com.csr.csrmeshdemo.listeners.SlideBarListener;


public class SlideBar extends View {
	private static final int POINTS_PER_LINE = 4;	
	private static final int RULER_HEIGHT_DIVISOR = 20;
	private static final int TEXT_HEIGHT_DIVISOR = 40;
	private static final float TEXT_WIDTH_DIVISOR = 40.0f;
	private static final float MAX_TEXT_SIZE = 60.0f;
	
	private static final int DESIRED_RECT_CONFIRMED=0x9010f020;
	private static final int DESIRED_RECT_UNCONFIRMED=0x90f0a310;
	
	private Paint mPaint;
	private float mMin;
	private float mMax;
	private float mActualSetting;
	private float mDesiredSetting;
	private float mDesiredRange;
	private Rect mSettingRect;
	private Rect mDesiredSettingRect;	
    private boolean mIsDragging;
    private float mTouchDownX;
	private int mPaddingLeft;
	private int mPaddingRight;
	private float mTouchProgressOffset;
	private int mScaledTouchSlop;	
	private int mNumLines;
	private int mWidth;
	private int mHeight;
	private float mTextSize;
	private int mXOffset;
	private int mMargin;
	private int mActualRulerHeight;
	private int mScreenHeight;
	private float [] mLines;
	private float mLineLength;	
	private SlideBarListener mSlideBarListener;
	private boolean valueConfirmed=false;
	
	
	public SlideBar(Context context, AttributeSet attrs) {
		super(context, attrs);
		mScaledTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
		mScreenHeight = context.getResources().getDisplayMetrics().heightPixels;
		
		TypedArray a = context.getTheme().obtainStyledAttributes(
				attrs,
		        R.styleable.SlideBar, 0, 0);

		try {
			mMin = (float)a.getInteger(R.styleable.SlideBar_min, 0);
			mMax = (float)a.getInteger(R.styleable.SlideBar_max, 50);
			mActualSetting = a.getInteger(R.styleable.SlideBar_actual_setting, (int)mMax);
			mDesiredSetting = a.getInteger(R.styleable.SlideBar_desired_setting, (int)mMin);
			mDesiredRange = a.getInteger(R.styleable.SlideBar_range, (int)mMin);
	    } finally {
	       a.recycle();
	    }
	}
	
	/**
	 * Set new range of possible values the slideBar should displays.
	 * @param minValue
	 * @param maxValue
	 */
	public void setMinMaxValues(int minValue,int maxValue) {
		mMin = minValue;
		mMax = maxValue;
		postInvalidate();
	}
	
	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();		
		mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		mSettingRect = new Rect();
		mDesiredSettingRect = new Rect();
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int totalHeight = (mScreenHeight / RULER_HEIGHT_DIVISOR);
		mMargin = (mScreenHeight / TEXT_HEIGHT_DIVISOR);
		totalHeight += mMargin;
		
		if (getSuggestedMinimumHeight() > totalHeight) {
			setMeasuredDimension(widthMeasureSpec, getSuggestedMinimumHeight());
			mMargin = 0;
		}
		else {
			setMeasuredDimension(widthMeasureSpec, totalHeight);
		}
	}
	
	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		if (changed) {
			update(true);
			invalidate();
		}
	}
		
	public void setPosition(float setting, float desired) {
		mActualSetting = setting;
		mDesiredSetting = desired;
		update(false);
		invalidate();
	}
	
	public void setDesired(float desired) {
		setPosition(mActualSetting, desired);
	}
	
	public void setActual(float setting) {
		setPosition(setting, mDesiredSetting);
	}
	
	public float getMin() {
		return mMin;
	}
	
	public float getMax() {
		return mMax;
	}
	
	public float getDesiredRange() {
		return mDesiredRange;
	}
	
	public float getDesired() {
		// return kelvin
		return Math.round((mDesiredSetting + 273.15f));
		//return mDesiredSetting;
	}
	
	@Override
    protected void onSizeChanged(int newWidth, int newHeight, int oldWidth, int oldHeight) {
            super.onSizeChanged(newWidth, newHeight, oldWidth, oldHeight);
            // First adjust text size depending on width.
            mTextSize = Math.min(newWidth / TEXT_WIDTH_DIVISOR, MAX_TEXT_SIZE);
            final float range = mMax - mMin;
    		mNumLines = (int)(range) + 1;
    		// Adjust size to make space for text.
    		final int textAdjust = (int)mTextSize * 2;
    		final int adjustedWidth = newWidth - textAdjust;
            // Shrink width to multiple of number of lines.
    		final int adjust = (adjustedWidth % (mNumLines - 1));
            mWidth = adjustedWidth - adjust;
            mHeight = newHeight;
            mXOffset =  (textAdjust + adjust) / 2;
    }
	
	private void update(boolean calcScale) {
		if (mLines == null || mLines.length < mNumLines * POINTS_PER_LINE) {
			mLines = new float[mNumLines * POINTS_PER_LINE];
		}
		
		mActualRulerHeight = (mHeight - mMargin);
		mLineLength = mActualRulerHeight / 4.0f;
		final float lineYOffset = (mActualRulerHeight - mLineLength) / 2.0f;		
		final int spacing = mWidth / (mNumLines - 1);
		
		if (calcScale) {
			float x = mXOffset;
			for (int i = 0; i < mNumLines; i++) {
				 final int j = i * POINTS_PER_LINE;
				 //x0
				 mLines[j] = x;
				 //x1
				 mLines[j+2] = x;
				 if (i % 10 == 0) {
					 //y0
					 mLines[j+1] = mActualRulerHeight;
					 //y1
					 mLines[j+3] = 0;
				 }
				 else {
					//y0
					 mLines[j+1] = mActualRulerHeight - lineYOffset;
					 //y1
					 mLines[j+3] = mActualRulerHeight - lineYOffset - mLineLength;
				 }
				 x+=spacing;
			}
		}
		calcBar(0, mSettingRect, mActualSetting - mMin, false);
		calcBar(0, mDesiredSettingRect, mDesiredSetting, true);
	}

	private void calcBar(int yPos, Rect bar, float setting, boolean useRangeBar) {
		final int barHeight = (int)(mLineLength / 2.0f);
		final int barYOffset = (int)((mActualRulerHeight - barHeight) / 2.0f);
		
		if (useRangeBar) {
			// Position on the ruler of setting.
			final int x = (int)((setting / (mMax - mMin)) * mWidth) + mXOffset + 1;
			// Width in pixels of one unit on the ruler.
			final int unit = (int)(mWidth / (mMax - mMin));
			// Amount to add to include the range.
			final int rangeAdd = unit * (int)mDesiredRange;
			int left = Math.max(x - rangeAdd, mXOffset);
			int right = left + rangeAdd + rangeAdd;
			if (right > (mXOffset + mWidth)) {
				right = mXOffset + mWidth;
				left = right - rangeAdd - rangeAdd;
			}
			bar.set(left,
				    yPos,
				    right,
				    yPos + mActualRulerHeight);
		}
		else {
			bar.set(mXOffset, 
				    yPos + barYOffset,
				    (int)((setting / (mMax - mMin)) * mWidth) + mXOffset + 1,
				    yPos + barYOffset + barHeight);
		}
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		mPaint.setStyle(Paint.Style.FILL);
		mPaint.setColor(0xfff02010);
		canvas.drawRect(mSettingRect, mPaint);
		mPaint.setColor(valueConfirmed?DESIRED_RECT_CONFIRMED:DESIRED_RECT_UNCONFIRMED);
		canvas.drawRect(mDesiredSettingRect, mPaint);
		mPaint.setColor(0xff000000);
		canvas.drawLines(mLines, 0, mNumLines * POINTS_PER_LINE, mPaint);
		
		mPaint.setTextSize(mTextSize);
		final float offset = mTextSize / 2.0f;
		int value = (int)mMin;
		for (int i = 0; i < mNumLines; i+=10) {
			final int j = i * POINTS_PER_LINE;
			final float x = i == 0 ? mLines[j] - offset / 2.0f : mLines[j] - offset; 
			canvas.drawText(String.format("%d",value), 
					x, mActualRulerHeight + (mMargin / 1.5f), mPaint);			
			value += 10;
		}
	}
	
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:               
                setPressed(true);
                onStartTrackingTouch();
                trackTouchEvent(event);   
                setValueConfirmed(false);
                break;

            case MotionEvent.ACTION_MOVE:
                if (mIsDragging) {
                    trackTouchEvent(event);
                } else {
                    final float x = event.getX();
                    if (Math.abs((x - mTouchDownX)) > mScaledTouchSlop) {
                        setPressed(true);                        
                        onStartTrackingTouch();
                        trackTouchEvent(event);
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
                if (mIsDragging) {
                    trackTouchEvent(event);
                    onStopTrackingTouch();
                    setPressed(false);
                } else {
                    // Touch up when we never crossed the touch slop threshold should
                    // be interpreted as a tap-seek to that location.
                    onStartTrackingTouch();
                    trackTouchEvent(event);
                    onStopTrackingTouch();
                }          
                break;

            case MotionEvent.ACTION_CANCEL:
                if (mIsDragging) {
                    onStopTrackingTouch();
                    setPressed(false);
                }
                break;
        }
        return true;
    }

    private void trackTouchEvent(MotionEvent event) {
        float progress = 0;
        final int width = getWidth();
        final int available = width - mPaddingLeft - mPaddingRight;
        int x = (int)event.getX();
        float scale;
        if (x < mPaddingLeft) {
            scale = 0.0f;
        } else if (x > width - mPaddingRight) {
            scale = 1.0f;
        } else {
            scale = (float)(x - mPaddingLeft) / (float)available;
            progress = mTouchProgressOffset;
        }        
        progress += scale * mMax;        
        setDesired(progress);
    }
    
    /**
     * This is called when the user has started touching this widget.
     */
    void onStartTrackingTouch() {
        mIsDragging = true;
    }

    /**
     * This is called when the user either releases touch or the touch is
     * cancelled.
     */
    void onStopTrackingTouch() {
        mIsDragging = false;
        if(mSlideBarListener!=null){
        	mSlideBarListener.onRelease();
        }
    }
    
    public void setSlideBarListener(SlideBarListener listener){
    	mSlideBarListener = listener;
    }
    
    public void setValueConfirmed(boolean confirmed){
    	valueConfirmed=confirmed;
    	postInvalidate();
    }
}



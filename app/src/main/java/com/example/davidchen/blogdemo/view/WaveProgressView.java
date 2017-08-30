package com.example.davidchen.blogdemo.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import com.example.davidchen.blogdemo.R;


/**
 * 自定义波纹进度条，不设置背景时，则默认是一个圆形背景进度条，设置则是图片带波纹，
 * 因此建议使用带透明像素的png，不然会看不到效果.
 * 因为是把背景当做配图的，因此建议当使用背景图时，使用wrap_content，而无背景时，则随意
 * Created by DavidChen on 2017/2/14.
 */
public class WaveProgressView extends View {

    private static final String DEFAULT_WAVE_COLOR = "#FFCDE5FD";   // 默认的波纹颜色
    private static final String DEFAULT_WAVE_BG_COLOR = "#FFF1F7FF";    // 默认波纹背景色
    private static final String DEFAULT_TEXT_HINT_COLOR = "#FF878889"; // 默认提示字体颜色
    private static final String DEFAULT_TEXT_COLOR = "#FF288DEB";    // 默认字体颜色
    private static final String DEFAULT_STROKE_COLOR = "#FF82BAF1"; // 默认背景下，边框颜色
    private static final float DEFAULT_STROKE_RADIO = 1f / 100; // 默认背景下线宽占view宽比
    private static final float DEFAULT_SPACE_RADIO = 1f / 20;   // 默认背景下线与波纹图片间距占view宽比

    private Path mPath;

    private boolean mIsAutoBack = false;    // 用于判断是否需要一个默认的背景（默认为圆形背景）

    private Bitmap mBackground; // 用于保存与波浪进行图形计算的背景
    private Drawable mTmpBackground;  // 保存背景，因为要把背景清空

    private int mWidth; // 控件宽度，默认的话则认为是200
    private int mHeight;    // 控件高度，默认的话则认为是200

    private Paint mWavePaint;   // 绘制波纹画笔
    private Paint mStrokePaint; // 绘制默认背景边框的画笔
    private Paint mTextPaint;   // 文字画笔

    private int mWaveCount = 0; // 波纹个数（计算得来）
    private float mWaveWidth; // 波纹宽度（一个起伏）
    private float mHalfWaveWidth = mWaveWidth / 4;  // 四分之一的波纹宽
    private float mWaveHeight;   // 波纹高度
    private float mDistance = 0;    // 波纹总偏移量

    private float mSpeed;    // 每帧偏移量

    private float mMaxProgress; // 最大进度
    private float mProgress; // 当前进度

    private float mStrokeWidth;   // 默认背景时的线宽
    private int mWaveColor;  // 波纹颜色
    private int mWaveBackgroundColor; // 波纹背景色
    private int mStrokeColor;  // 边框颜色

    private int mHintColor = Color.parseColor(DEFAULT_TEXT_HINT_COLOR); // 提示文本颜色
    private int mTextColor;  // 文本颜色
    private String mHint = "可用额度";   // 提示文本
    private String mText = "￥80,000.00";   // 文本
    private int mTextSize; // sp
    private int mHintSize; // sp
    private float mTextSpace = 10;
    private Rect mTextRect;

    public WaveProgressView(Context context) {
        this(context, null);
    }

    public WaveProgressView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WaveProgressView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.WaveProgressView);

        mMaxProgress = ta.getInt(R.styleable.WaveProgressView_progress_max, 100);
        mProgress = ta.getInt(R.styleable.WaveProgressView_progress, 50);
        mWaveWidth = ta.getFloat(R.styleable.WaveProgressView_wave_width, 200f);
        mHalfWaveWidth = mWaveWidth / 4;
        mWaveHeight = ta.getFloat(R.styleable.WaveProgressView_wave_height, 20f);
        mSpeed = ta.getFloat(R.styleable.WaveProgressView_speed, mWaveWidth / 70);
        mWaveColor = ta.getColor(R.styleable.WaveProgressView_wave_color,
                Color.parseColor(DEFAULT_WAVE_COLOR));
        mWaveBackgroundColor = ta.getColor(R.styleable.WaveProgressView_wave_bg_color,
                Color.parseColor(DEFAULT_WAVE_BG_COLOR));
        mStrokeColor = ta.getColor(R.styleable.WaveProgressView_stroke_color,
                Color.parseColor(DEFAULT_STROKE_COLOR));
        mText = ta.getString(R.styleable.WaveProgressView_main_text);
        if (mText == null) {
            mText = "";
        }
        mTextColor = ta.getColor(R.styleable.WaveProgressView_main_text_color,
                Color.parseColor(DEFAULT_TEXT_COLOR));
        mTextSize = ta.getDimensionPixelSize(R.styleable.WaveProgressView_main_text_size, sp2px(16));
        mHint = ta.getString(R.styleable.WaveProgressView_hint_text);
        if (mHint == null) {
            mHint = "";
        }
        mHintColor = ta.getColor(R.styleable.WaveProgressView_hint_color,
                Color.parseColor(DEFAULT_TEXT_COLOR));
        mHintSize = ta.getDimensionPixelSize(R.styleable.WaveProgressView_hint_size, sp2px(14));
        mTextSpace = ta.getDimension(R.styleable.WaveProgressView_text_space, 10);

        ta.recycle();

        mPath = new Path();
        mWavePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mWavePaint.setStyle(Paint.Style.FILL);
        // 设置波纹颜色
        mWavePaint.setColor(mWaveColor);
        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        mTextRect = new Rect();
        mTmpBackground = getBackground();
        setBackgroundColor(Color.TRANSPARENT);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measuredWidth = measureWidth(widthMeasureSpec);
        int measuredHeight = measureHeight(heightMeasureSpec);
        setMeasuredDimension(measuredWidth, measuredHeight);
        if (null == mTmpBackground) {
            mIsAutoBack = true;
            int min = Math.min(measuredWidth, measuredHeight);
            mStrokeWidth = DEFAULT_STROKE_RADIO * min;
            float spaceWidth = DEFAULT_SPACE_RADIO * min;   // 默认背景时，线和波纹图片间距
            mWidth = (int) (min - (mStrokeWidth + spaceWidth) * 2);
            mHeight = (int) (min - (mStrokeWidth + spaceWidth) * 2);
            mBackground = autoCreateBitmap(mWidth / 2);
        } else {
            mIsAutoBack = false;
            mBackground = getBitmapFromDrawable(mTmpBackground);
            if (mBackground != null && !mBackground.isRecycled()) {
                mWidth = mBackground.getWidth();
                mHeight = mBackground.getHeight();
            }
        }
        mWaveCount = calWaveCount(mWidth, mWaveWidth);
    }

    /**
     * 创建默认是圆形的背景
     *
     * @param radius 半径
     * @return 背景图
     */
    private Bitmap autoCreateBitmap(int radius) {
        Bitmap bitmap = Bitmap.createBitmap(2 * radius, 2 * radius, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(mWaveBackgroundColor);
        p.setStyle(Paint.Style.FILL);
        canvas.drawCircle(radius, radius, radius, p);
        return bitmap;
    }

    /**
     * 从drawable中获取bitmap
     */
    private Bitmap getBitmapFromDrawable(Drawable drawable) {
        if (null == drawable) {
            return null;
        }
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        try {
            Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                    drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            return bitmap;
        } catch (OutOfMemoryError e) {
            return null;
        }
    }

    /**
     * 测量view高度，如果是wrap_content，则默认是200
     */
    private int measureHeight(int heightMeasureSpec) {
        int height = 0;
        int mode = MeasureSpec.getMode(heightMeasureSpec);
        int size = MeasureSpec.getSize(heightMeasureSpec);
        if (mode == MeasureSpec.EXACTLY) {
            height = size;
        } else if (mode == MeasureSpec.AT_MOST) {
            if (null != mTmpBackground) {
                height = mTmpBackground.getMinimumHeight();
            } else {
                height = 400;
            }
        }
        return height;
    }

    /**
     * 测量view宽度，如果是wrap_content，则默认是200
     */
    private int measureWidth(int widthMeasureSpec) {
        int width = 0;
        int mode = MeasureSpec.getMode(widthMeasureSpec);
        int size = MeasureSpec.getSize(widthMeasureSpec);
        if (mode == MeasureSpec.EXACTLY) {
            width = size;
        } else if (mode == MeasureSpec.AT_MOST) {
            if (null != mTmpBackground) {
                width = mTmpBackground.getMinimumWidth();
            } else {
                width = 400;
            }
        }
        return width;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Bitmap bitmap = createWaveBitmap(mWidth, mHeight);
        if (mIsAutoBack) {  // 如果没有背景，就画默认背景
            if (null == mStrokePaint) {
                mStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                mStrokePaint.setColor(mStrokeColor);
                mStrokePaint.setStrokeWidth(mStrokeWidth);
                mStrokePaint.setStyle(Paint.Style.STROKE);
            }
            // 默认背景下先画个边框
            float radius = Math.min(getMeasuredWidth() / 2, getMeasuredHeight() / 2);
            canvas.drawCircle(getMeasuredWidth() / 2, getMeasuredHeight() / 2, radius - mStrokeWidth / 2, mStrokePaint);
            float left = getMeasuredWidth() / 2 - mWidth / 2;
            float top = getMeasuredHeight() / 2 - mHeight / 2;
            canvas.drawBitmap(bitmap, left, top, null);
        } else {
            canvas.drawBitmap(bitmap, 0, 0, null);
        }
        // 画文字
        if (!TextUtils.isEmpty(mText)) {
            mTextPaint.setColor(mTextColor);
            mTextPaint.setTextSize(mTextSize);
            mTextPaint.getTextBounds(mText, 0, mText.length() - 1, mTextRect);
            float textLength = mTextPaint.measureText(mText);
//            canvas.drawText(mText, getMeasuredWidth() / 2 - mTextRect.width() / 2,
//                    getMeasuredHeight() / 2 + mTextRect.height() / 2, mTextPaint);  引以为戒
            Paint.FontMetrics metrics = mTextPaint.getFontMetrics();
            float baseLine = mTextRect.height() / 2 + (metrics.descent - metrics.ascent) / 2 - metrics.descent;
            canvas.drawText(mText, getMeasuredWidth() / 2 - textLength / 2,
                    getMeasuredHeight() / 2 + baseLine, mTextPaint);
        }
        // 画提示
        if (!TextUtils.isEmpty(mHint)) {
            mTextPaint.setColor(mHintColor);
            mTextPaint.setTextSize(mHintSize);
            float hintLength = mTextPaint.measureText(mHint);
            canvas.drawText(mHint, getMeasuredWidth() / 2 - hintLength / 2,
                    getMeasuredHeight() / 2 - mTextRect.height() - mTextSpace, mTextPaint);
        }
        postInvalidateDelayed(10);
    }

    /**
     * 绘制重叠的bitmap，注意：没有背景则默认是圆形的背景，有则是背景
     *
     * @param width  背景高
     * @param height 背景宽
     * @return 带波纹的图
     */
    private Bitmap createWaveBitmap(int width, int height) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // 计算波浪位置
        int mCurY = (int) (height * (mMaxProgress - mProgress) / mMaxProgress);

        // 画path
        mPath.reset();
        mPath.moveTo(-mDistance, mCurY);
        for (int i = 0; i < mWaveCount; i++) {
            mPath.quadTo(i * mWaveWidth + mHalfWaveWidth - mDistance, mCurY - mWaveHeight,
                    i * mWaveWidth + mHalfWaveWidth * 2 - mDistance, mCurY);    // 起
            mPath.quadTo(i * mWaveWidth + mHalfWaveWidth * 3 - mDistance, mCurY + mWaveHeight,
                    i * mWaveWidth + mHalfWaveWidth * 4 - mDistance, mCurY);    // 伏
        }
        mPath.lineTo(width, height);
        mPath.lineTo(0, height);
        mPath.close();
        canvas.drawPath(mPath, mWavePaint);

        mDistance += mSpeed;
        mDistance %= mWaveWidth;

        mWavePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_ATOP));
        canvas.drawBitmap(mBackground, 0, 0, mWavePaint);
        return bitmap;
    }

    /**
     * 计算波纹数目
     *
     * @param width     波纹图宽度
     * @param waveWidth 每条波纹的宽度
     * @return 波纹数目
     */
    private int calWaveCount(int width, float waveWidth) {
        int count;
        if (width % waveWidth == 0) {
            count = (int) (width / waveWidth + 1);
        } else {
            count = (int) (width / waveWidth + 2);
        }
        return count;
    }

    /**
     * 设置最大进度
     */
    public void setMax(float max) {
        mMaxProgress = max;
    }

    /**
     * 设置当前进度
     */
    public void setProgress(float progress) {
        mProgress = progress;
    }

    /**
     * 获取当前进度
     */
    public float getProgress() {
        return mProgress;
    }

    /**
     * 设置波纹速度
     */
    public void setSpeed(float speed) {
        mSpeed = speed;
    }

    /**
     * 设置波纹尺寸
     *
     * @param waveWidth  波纹宽度,默认200
     * @param waveHeight 波纹高度,默认20
     */
    public void setWave(float waveWidth, float waveHeight) {
        mWaveWidth = waveWidth;
        mWaveHeight = waveHeight;
        mHalfWaveWidth = waveWidth / 4;
        mWaveCount = calWaveCount(mWidth, mWaveWidth);
    }

    /**
     * 设置波纹颜色
     *
     * @param waveColor 颜色值，默认是淡蓝色
     */
    public void setWaveColor(int waveColor) {
        this.mWaveColor = waveColor;
        mWavePaint.setColor(mWaveColor);
    }

    /**
     * 设置波纹背景色
     *
     * @param waveBackgroundColor 波纹背景色，必须是在无背景下才会有效果
     */
    public void setWaveBackgroundColor(int waveBackgroundColor) {
        this.mWaveBackgroundColor = waveBackgroundColor;
        if (mIsAutoBack) {
            mBackground = autoCreateBitmap(mWidth / 2);
        }
    }

    /**
     * 设置边框颜色
     *
     * @param strokeColor 边框颜色，必须是在无背景下才会有效果
     */
    public void setStrokeColor(int strokeColor) {
        this.mStrokeColor = strokeColor;
        if (mStrokePaint != null) {
            mStrokePaint.setColor(mStrokeColor);
        }
    }

    /**
     * 设置文本字体大小
     *
     * @param textSize 文本字体大小,sp
     */
    public void setTextSize(int textSize) {
        this.mTextSize = sp2px(textSize);
    }

    /**
     * 设置文本字体颜色
     *
     * @param textColor 文本字体颜色
     */
    public void setTextColor(int textColor) {
        this.mTextColor = textColor;
    }

    /**
     * 设置文本
     *
     * @param text 文本
     */
    public void setText(String text) {
        this.mText = text;
    }

    /**
     * 设置提示文本字体大小
     *
     * @param hintSize 提示文本字体大小,sp
     */
    public void setHintSize(int hintSize) {
        this.mHintSize = sp2px(hintSize);
    }

    /**
     * 设置提示文本字体颜色
     *
     * @param hintColor 提示文本字体颜色
     */
    public void setHintColor(int hintColor) {
        this.mHintColor = hintColor;
    }

    /**
     * 设置提示文本
     *
     * @param hint 提示文本
     */
    public void setHint(String hint) {
        this.mHint = hint;
    }

    /**
     * 设置提示与文本间距
     *
     * @param textSpace 提示与文本间距,dp
     */
    public void setTextSpace(int textSpace) {
        this.mTextSpace = dp2px(textSpace);
    }

    /**
     * sp转换成px
     *
     * @param spVal sp
     * @return px
     */
    public int sp2px(int spVal) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                spVal, getContext().getResources().getDisplayMetrics());
    }

    /**
     * dp转换成px
     *
     * @param dpVal dp
     * @return px
     */
    public int dp2px(int dpVal) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                dpVal, getContext().getResources().getDisplayMetrics());
    }
}

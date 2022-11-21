package com.vise.face;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.SweepGradient;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;


/**
 * <pre>
 * 文件名：	CameraFaceView
 * 作　者：	zj
 * 时　间：	2019/1/2 18:27
 * 描　述：人脸识别界面UI，这里可以接视频流和UI，也可以单纯作为UI展示
 * </pre>
 */
public class CameraFaceView extends View implements ValueAnimator.AnimatorUpdateListener, Animator.AnimatorListener {
    private final static String TAG = "CameraFaceView";
    private float currentRadius = 0;//默认中间圆的半径从0开始
    private int mViewWidth = 400;//控件的宽度（默认）
    private int mViewHeight = 400;// 控件高度
    private int margin;// 中心圆屏幕边距
    private Paint mTextBgPaint;// 提示文本背景画笔
    private Paint mTextPaint;//提示语画笔
    private int mTipTextBgColor;//提示文本背景颜色
    private String mTipText;// 提示文本
    private int mTipTextColor;//提示文本颜色
    private int mTipTextSize;//提示文本大小
    private Paint mBgArcPaint;//圆弧背景画笔
    private Paint mArcPaint;// 圆弧画笔
    private float mBgArcWidth;//背景弧宽度
    private int mBgArcColor;//背景弧颜色
    private int mArcStartColor;//圆弧渐变起始颜色
    private int mArcEndColor;//圆弧渐变结束颜色
    private final Point mCenterPoint = new Point();// 圆心点坐标
    private final RectF mBgRectF = new RectF();//圆弧边界
    private int mRadius;//内圆半径
    private final int START_ANGLE = 105;//开始角度
    private final int END_ANGLE = 330;// 结束角度
    private final int SPEED = 5;//绘制速度
    private int mDuration = 2000;//动画时长间隔
    private float currentAngle = 0;// 设置默认转动角度0
    private SweepGradient mSweepGradient;// 渐变器
    private ValueAnimator mProgressValueAnimator;
    private OnActionListener onActionListener;

    public CameraFaceView(Context context) {
        this(context, null);
    }

    public CameraFaceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraFaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.CameraFaceView);
        //初始化值
        margin = array.getDimensionPixelSize(R.styleable.CameraFaceView_circle_margin, FaceUtil.dp2px(context, 60));
        mBgArcWidth = array.getDimensionPixelSize(R.styleable.CameraFaceView_circle_margin, FaceUtil.dp2px(context, 5));
        mBgArcColor = array.getColor(R.styleable.CameraFaceView_arc_bg_color, getResources().getColor(R.color.arc_bg_color));
        mArcStartColor = array.getColor(R.styleable.CameraFaceView_arc_sweep_start_color, getResources().getColor(R.color.arc_sweep_start_color));
        mArcEndColor = array.getColor(R.styleable.CameraFaceView_arc_sweep_end_color, getResources().getColor(R.color.arc_sweep_end_color));
        mTipTextBgColor = array.getColor(R.styleable.CameraFaceView_tip_text_bg_color, getResources().getColor(R.color.tip_text_bg_color));
        mTipText = array.getString(R.styleable.CameraFaceView_tip_text);
        mTipTextColor = array.getColor(R.styleable.CameraFaceView_tip_text_color, Color.WHITE);
        mTipTextSize = array.getDimensionPixelSize(R.styleable.CameraFaceView_tip_text_size, FaceUtil.sp2px(context, 12));
        mDuration = array.getInt(R.styleable.CameraFaceView_face_anim_duration, mDuration);
        array.recycle();
        initHolder(context);
    }

    /**
     * 初始化控件View
     */
    private void initHolder(Context context) {
        //屏蔽界面焦点
        setFocusable(true);
        //保持屏幕长亮
        setKeepScreenOn(true);

        //绘制提示文本画笔
        mTextPaint = new Paint();
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setStrokeWidth(8);
        mTextPaint.setColor(mTipTextColor);
        mTextPaint.setTextSize(mTipTextSize);
        mTextPaint.setTextAlign(Paint.Align.CENTER);

        //初始化提示文本背景画笔
        mTextBgPaint = new Paint();
        mTextBgPaint.setAntiAlias(true);
        mTextBgPaint.setColor(mTipTextBgColor);
        mTextBgPaint.setStyle(Paint.Style.FILL);

        // 圆弧背景
        mBgArcPaint = new Paint();
        mBgArcPaint.setAntiAlias(true);
        mBgArcPaint.setColor(mBgArcColor);
        mBgArcPaint.setStyle(Paint.Style.STROKE);
        mBgArcPaint.setStrokeWidth(mBgArcWidth);
        mBgArcPaint.setStrokeCap(Paint.Cap.ROUND);

        // 圆弧
        mArcPaint = new Paint();
        mArcPaint.setAntiAlias(true);
        mArcPaint.setStyle(Paint.Style.STROKE);
        mArcPaint.setStrokeWidth(mBgArcWidth);
        mArcPaint.setStrokeCap(Paint.Cap.ROUND);

        mProgressValueAnimator = new ValueAnimator();
        mProgressValueAnimator.setDuration(mDuration);//动画时间
        mProgressValueAnimator.addUpdateListener(this);
        mProgressValueAnimator.addListener(this);
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //测量view的宽度
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        //测量view的高度
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
//        Log.d(TAG, "onMeasure  widthMode : " + widthMode + "  heightMode : " + heightMode);
        if (widthMode == MeasureSpec.EXACTLY) {
            mViewWidth = MeasureSpec.getSize(widthMeasureSpec);
        }
        if (heightMode == MeasureSpec.EXACTLY) {
            mViewHeight = MeasureSpec.getSize(heightMeasureSpec);
        }
        setMeasuredDimension(mViewWidth, mViewHeight);
        Log.d(TAG, "onMeasure  mViewWidth : " + mViewWidth + "  mViewHeight : " + mViewHeight);

        //获取圆的相关参数
        mCenterPoint.x = mViewWidth / 2;
        mCenterPoint.y = mViewHeight / 2;

        //外环圆的半径
        mRadius = mCenterPoint.x - margin;

        //绘制背景圆弧的边界
        mBgRectF.left = mCenterPoint.x - mRadius - mBgArcWidth / 2;
        mBgRectF.top = mCenterPoint.y - mRadius - mBgArcWidth / 2;
        mBgRectF.right = mCenterPoint.x + mRadius + mBgArcWidth / 2;
        mBgRectF.bottom = mCenterPoint.y + mRadius + mBgArcWidth / 2;

        //进度条颜色 -mStartAngle将位置便宜到原处
        mSweepGradient = new SweepGradient(mCenterPoint.x - START_ANGLE, mCenterPoint.y - START_ANGLE, mArcStartColor, mArcEndColor);


    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //清除画布上面里面的内容
//        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        //绘制画布内容
        drawContent(canvas);
        postInvalidate();
    }


    private void drawContent(Canvas canvas) {
        //防止save()和restore()方法代码之后对Canvas执行的操作，继续对后续的绘制会产生影响
        canvas.save();
        //先画提示语
        drawHintText(canvas);
        //绘制正方形的框内类似人脸识别
//        drawFaceRectTest(canvas);
        //绘制人脸识别部分
        drawFaceCircle(canvas);
        //画外边进度条
        drawRoundProgress(canvas);
        canvas.restore();
    }

    /**
     * 绘制人脸识别提示
     *
     * @param canvas canvas
     */
    private void drawHintText(Canvas canvas) {
        //圆视图宽度 （屏幕减去两边距离）
        int cameraWidth = mViewWidth - 2 * margin;
        //x轴起点（文字背景起点）
        int x = margin;
        //宽度（提示框背景宽度）
        int width = cameraWidth;
        //y轴起点
        int y = (int) (mCenterPoint.y - mRadius);
        //提示框背景高度
        int height = cameraWidth / 4;
        Rect rect = new Rect(x, y, x + width, y + height);
        canvas.drawRect(rect, mTextBgPaint);

        //计算baseline
        Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
        float distance = (fontMetrics.bottom - fontMetrics.top) / 4;
        float baseline = rect.centerY() + distance;
        String text = mTipText == null ? "" : mTipText;
        canvas.drawText(text, rect.centerX(), baseline, mTextPaint);
    }

    /**
     * 绘制人脸识别矩形区域
     *
     * @param canvas canvas
     */
    private void drawFaceRectTest(Canvas canvas) {
        int cameraWidth = mViewWidth - 2 * margin;
        int x = margin + cameraWidth / 6;
        int width = cameraWidth * 2 / 3;
        int y = mCenterPoint.x + (width / 2);
        int height = width;
        Rect rect = new Rect(x, y, x + width, y + height);
        mTextBgPaint.setColor(Color.GREEN);
        mTextBgPaint.setStyle(Paint.Style.STROKE);
        canvas.drawRect(rect, mTextBgPaint);
    }

    /**
     * 绘制人脸识别部分
     *
     * @param canvas
     */
    private void drawFaceCircle(Canvas canvas) {
        // 圆形，放大效果
        currentRadius += 20;
        if (currentRadius > mRadius) currentRadius = mRadius;

        //设置画板样式
        Path path = new Path();
        //以（400,200）为圆心，半径为100绘制圆 指创建顺时针方向的矩形路径
        path.addCircle(mCenterPoint.x, mCenterPoint.y, currentRadius, Path.Direction.CW);
        //是A形状中不同于B的部分显示出来
        canvas.clipPath(path, Region.Op.DIFFERENCE);
        // 半透明背景效果
        canvas.clipRect(0, 0, mViewWidth, mViewHeight);
        //绘制背景颜色
        canvas.drawColor(getResources().getColor(R.color.face_view_bg));
    }

    /**
     * 绘制人脸识别界面进度条
     *
     * @param canvas canvas
     */
    private void drawRoundProgress(Canvas canvas) {
        // 逆时针旋转105度
        canvas.rotate(START_ANGLE, mCenterPoint.x, mCenterPoint.y);
        // 设置圆环背景
        canvas.drawArc(mBgRectF, 0, END_ANGLE, false, mBgArcPaint);
        // 设置渐变颜色
        mArcPaint.setShader(mSweepGradient);
        canvas.drawArc(mBgRectF, 0, currentAngle, false, mArcPaint);
    }


    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        currentAngle = (float) animation.getAnimatedValue();
//        Log.d(TAG, "currentAngle=" + currentAngle);
        invalidate();
    }


    /**
     * 提示信息
     *
     * @param title title
     */
    public void updateTipsInfo(String title) {
        mTipText = title;
    }

    /**
     * 从头位置开始动画
     */
    public void resetAnimator() {
        currentAngle = 0;
        if (mProgressValueAnimator != null) {
            mProgressValueAnimator.cancel();
            mProgressValueAnimator.setFloatValues(currentAngle, END_ANGLE);
            mProgressValueAnimator.start();
        }
    }


    /**
     * 开始动画
     */
    public void startAnimator() {
        if (mProgressValueAnimator != null) {
            mProgressValueAnimator.setFloatValues(currentAngle, END_ANGLE);
            mProgressValueAnimator.start();
        }
    }

    /**
     * 暂停动画
     */
    public void pauseAnimator() {
        if (mProgressValueAnimator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                mProgressValueAnimator.pause();
            } else {
                mProgressValueAnimator.cancel();
            }

        }
    }

    /**
     * 动画直接完成
     */
    public void finnishAnimator() {
        currentAngle = END_ANGLE;
        if (mProgressValueAnimator != null) {
            mProgressValueAnimator.cancel();
            mProgressValueAnimator.setFloatValues(currentAngle, END_ANGLE);
            mProgressValueAnimator.start();
        }
    }

    /**
     * 动画回退
     */
    public void backAnimator() {
        if (currentAngle <= 0) {
            return;
        }
        if (mProgressValueAnimator != null) {
            mProgressValueAnimator.cancel();
            mProgressValueAnimator.setFloatValues(currentAngle, 0);
            mProgressValueAnimator.start();
        }
    }

    /**
     * 动画前进
     */
    public void forwardAnimator() {
        if (currentAngle >= END_ANGLE) {
            return;
        }
        if (mProgressValueAnimator != null) {
            mProgressValueAnimator.cancel();
            mProgressValueAnimator.setFloatValues(currentAngle, END_ANGLE);
            mProgressValueAnimator.start();
        }
    }

    /**
     * 销毁视图，释放资源
     */
    public void destroyView() {
        //停止运行
        if (mProgressValueAnimator != null) {
            mProgressValueAnimator.cancel();
        }
    }


    @Override
    public void onAnimationStart(Animator animation) {
        Log.d(TAG, "onAnimationStart");
        if (onActionListener != null) {
            onActionListener.onStart();
        }
    }

    @Override
    public void onAnimationEnd(Animator animation) {
        Log.d(TAG, "onAnimationEnd");
        if (onActionListener != null) {
            onActionListener.onEnd();
        }
    }

    @Override
    public void onAnimationCancel(Animator animation) {

    }

    @Override
    public void onAnimationRepeat(Animator animation) {

    }

    public void setOnActionListener(OnActionListener onActionListener) {
        this.onActionListener = onActionListener;
    }

    public interface OnActionListener {
        void onStart();

        void onEnd();
    }
}
package com.yunlei.scanningprogressview

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.dip
import org.jetbrains.anko.sp
import java.util.*
import kotlin.concurrent.timerTask


/**
 * 类名：ScanningProgressView
 * 作者：Yun.Lei
 * 功能：
 * 创建日期：2018-01-11 10:36
 * 修改人：
 * 修改时间：
 * 修改备注：
 */
class ScanningProgressView : View, AnkoLogger {

    //背景色
    private var mBackgroundColor: Int = Color.parseColor("#ffffff")
    /* 亮色 */
    private var mLightColor: Int = Color.parseColor("#237EAD")
    /* 暗色 */
    private var mDarkColor: Int = Color.parseColor("#f5f5f5")
    //画布
    private lateinit var mCanvas: Canvas
    /*外径圆画笔*/
    private var mBGCirclePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var mBGFillCirclePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    /* 刻度圆弧画笔 */
    private var mScaleArcPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    /* 刻度线画笔 */
    private var mScaleLinePaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    /* 刻度圆弧的外接矩形 */
    private var mScaleArcRectF: RectF = RectF()
    /* 文字画笔 */
    private val mTextPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val mTextRect: Rect = Rect()
    /* 外径圆圈线条宽度 */
    private var mCircleStrokeWidth = 2f
    /* x轴的位移 */
    private val mCanvasTranslateX: Float = 0.toFloat()
    /* y轴的位移 */
    private val mCanvasTranslateY: Float = 0.toFloat()
    /* 刻度线长度 */
    private var mScaleLength: Float = 0.toFloat()
    /* 时钟半径，不包括padding值 */
    private var mRadius: Float = 0.toFloat()
    /* 加一个默认的padding值 */
    private var mDefaultPadding: Float = 0.toFloat()
    private var mPaddingLeft: Float = 0.toFloat()
    private var mPaddingTop: Float = 0.toFloat()
    private var mPaddingRight: Float = 0.toFloat()
    private var mPaddingBottom: Float = 0.toFloat()
    private var mBGPaddingTop: Float = 0F
    /* 渐变矩阵，作用在SweepGradient */
    private val mGradientMatrix: Matrix = Matrix()
    /* 梯度扫描渐变 */
    private var mSweepGradient: SweepGradient? = null
    private var mDegree: Float = 0f
    private var mMaxCanvasTranslate: Float = 0.toFloat()
    /* 刷新控件显示 */
    private var mCurrDrawScore: Int = 0
    /* 刷新显示计时器 */
    private var timer: Timer? = null
    /* 基础文字大小 */
    private var mBaseTextSize = sp(50)
    /* 基础文字颜色 */
    private var mBaseTextColor = Color.parseColor("#333333")
    /* label文字大小 */
    private var mLabelTextSize = sp(14)
    /* label文字颜色 */
    private var mLabelTextColor = Color.parseColor("#666666")

    var mLabel: String = "%"
        set(value) {
            field = value
            invalidate()
        }
    var mScore: Int = -1        //设置分数
        set(value) {
            field = value
            if (field > 0) {
                mLabel = "分"
                stopScanning()
                addScore()
            }
            mCurrDrawScore = 0
            invalidate()
        }
    var mScanningPercent: Int = 0       //设置扫描比例
        set(value) {
            field = value
            mScore = -1
            mLabel = "%"
            invalidate()
        }


    constructor(context: Context?) : this(context, null)
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(attrs, defStyleAttr)
    }


    private fun init(attrs: AttributeSet?, defStyleAttr: Int) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.ScanningProgressView, defStyleAttr, 0)
        mBackgroundColor = a.getColor(R.styleable.ScanningProgressView_spv_background_color, mBackgroundColor)
        mDarkColor = a.getColor(R.styleable.ScanningProgressView_spv_dark_color, mDarkColor)
        mLightColor = a.getColor(R.styleable.ScanningProgressView_spv_light_color, mLightColor)
        mCircleStrokeWidth = a.getDimensionPixelSize(R.styleable.ScanningProgressView_spv_circle_stroke_width, mCircleStrokeWidth.toInt()).toFloat()
        mBaseTextSize = a.getDimensionPixelSize(R.styleable.ScanningProgressView_spv_base_text_size, mBaseTextSize)
        mLabelTextSize = a.getDimensionPixelSize(R.styleable.ScanningProgressView_spv_base_text_size, mLabelTextSize)
        mBaseTextColor = a.getColor(R.styleable.ScanningProgressView_spv_base_text_color,mBaseTextColor)
        mLabelTextColor = a.getColor(R.styleable.ScanningProgressView_spv_base_text_color,mLabelTextColor)
        a.recycle()

        mBGCirclePaint.style = Paint.Style.STROKE
        mBGCirclePaint.strokeWidth = mCircleStrokeWidth
        mBGCirclePaint.color = mDarkColor

        mBGFillCirclePaint.style = Paint.Style.STROKE
        mBGFillCirclePaint.color = mDarkColor

        mScaleLinePaint.style = Paint.Style.STROKE
        mScaleLinePaint.color = mBackgroundColor

        mScaleArcPaint.style = Paint.Style.STROKE
        mScaleArcPaint.color = mBackgroundColor

        setBackgroundColor(mBackgroundColor)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(measureDimension(widthMeasureSpec), measureDimension(heightMeasureSpec))
    }

    private fun measureDimension(measureSpec: Int): Int {
        var result: Int
        val mode = View.MeasureSpec.getMode(measureSpec)
        val size = View.MeasureSpec.getSize(measureSpec)
        if (mode == View.MeasureSpec.EXACTLY) {
            result = size
        } else {
            result = 800
            if (mode == View.MeasureSpec.AT_MOST) {
                result = Math.min(result, size)
            }
        }
        return result
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        //宽和高分别去掉padding值，取min的一半即表盘的半径
        mRadius = (Math.min(w - paddingLeft - paddingRight, h - paddingTop - paddingBottom) / 2).toFloat()
        mDefaultPadding = 0.12f * mRadius //根据比例确定默认padding大小
        mPaddingLeft = mDefaultPadding + w / 2 - mRadius + paddingLeft
        mPaddingTop = mDefaultPadding + h / 2 - mRadius + paddingTop
        mPaddingRight = mPaddingLeft
        mPaddingBottom = mPaddingTop
        mBGPaddingTop = mPaddingTop / 2
        mScaleLength = 0.12f * mRadius//根据比例确定刻度线长度
        mScaleArcPaint.strokeWidth = mScaleLength
        mScaleLinePaint.strokeWidth = 0.012f * mRadius
        mBGFillCirclePaint.strokeWidth = mScaleLength + mPaddingTop - mBGPaddingTop
        mMaxCanvasTranslate = 0.02f * mRadius
        //梯度扫描渐变，以(w/2,h/2)为中心点，两种起止颜色梯度渐变
        //float数组表示，[0,0.75)为起始颜色所占比例，[0.75,1}为起止颜色渐变所占比例
        mSweepGradient = SweepGradient(w / 2f, h / 2f,
                intArrayOf(mDarkColor, mLightColor), floatArrayOf(0.75f, 1f))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        mCanvas = canvas
        drawBGCircle()
        drawScaleLine()
        drawText()
        drawScore()
        invalidate()
    }

    /**
     * 绘制外径圆
     */
    private fun drawBGCircle() {
        mCanvas.drawCircle((width / 2).toFloat(), (height / 2).toFloat(), mRadius - mCircleStrokeWidth, mBGCirclePaint)
        mCanvas.drawCircle((width / 2).toFloat(), (height / 2).toFloat(), mRadius - mBGPaddingTop - mBGFillCirclePaint.strokeWidth / 2, mBGFillCirclePaint)
    }


    /**
     * 画一圈梯度渲染的亮暗色渐变圆弧，重绘时不断旋转，上面盖一圈背景色的刻度线
     */
    private fun drawScaleLine() {
        if (mScore > 0) return
        mCanvas.save()
        mCanvas.translate(mCanvasTranslateX, mCanvasTranslateY)
        mScaleArcRectF.set(mPaddingLeft + mScaleArcPaint.strokeWidth / 2,
                mPaddingTop + mScaleArcPaint.strokeWidth / 2
                , width - mPaddingRight - mScaleArcPaint.strokeWidth / 2,
                height - mPaddingBottom - mScaleArcPaint.strokeWidth / 2)
        //matrix默认会在三点钟方向开始颜色的渐变，为了吻合钟表十二点钟顺时针旋转的方向，把秒针旋转的角度减去90度
        mGradientMatrix.setRotate((mDegree - 90), width / 2F, height / 2F)
        mSweepGradient?.setLocalMatrix(mGradientMatrix)
        mScaleArcPaint.shader = mSweepGradient
        mCanvas.drawArc(mScaleArcRectF, 0F, 360F, false, mScaleArcPaint)
        //画背景色刻度线
        for (i in 0..199) {
            mScaleLinePaint.color = mBackgroundColor
            mCanvas.drawLine((width / 2).toFloat(), mPaddingTop, (width / 2).toFloat(), mPaddingTop + mScaleLength, mScaleLinePaint)
            mCanvas.rotate(1.8f, (width / 2).toFloat(), (height / 2).toFloat())
        }
        mCanvas.restore()
    }

    /**
     * 绘制分数刻度
     */
    private fun drawScore() {
        if (mScore <= 0) return
        mCanvas.save()
        //画背景色刻度线
        for (i in 0..199) {
            mScaleLinePaint.color = if (i > mCurrDrawScore * 2) {
                mBackgroundColor
            } else {
                mLightColor
            }
            mCanvas.drawLine((width / 2).toFloat(), mPaddingTop, (width / 2).toFloat(), mPaddingTop + mScaleLength, mScaleLinePaint)
            mCanvas.rotate(1.8f, (width / 2).toFloat(), (height / 2).toFloat())
        }
        mCanvas.restore()

    }

    /**
     * 设置分数后逐步绘制（动画效果）
     */
    private fun addScore() {
        val anim = ValueAnimator.ofInt(0, mScore)
        anim.addUpdateListener {
            mCurrDrawScore = it.animatedValue as Int
            invalidate()
        }
        anim.duration = mScore * 10L
        anim.start()
    }


    /**
     * 绘制文字
     */
    private fun drawText() {
        mTextPaint.color = mBaseTextColor
        mTextPaint.style = Paint.Style.FILL
        mTextPaint.textSize = mBaseTextSize.toFloat()
        val s = if (mScore > 0) {
            mScore.toString()
        } else {
            mScanningPercent.toString()
        }
        mTextPaint.getTextBounds(s, 0, s.length, mTextRect)
        mCanvas.drawText(s, (width / 2).toFloat() - mTextRect.width() / 2, (height / 2).toFloat() + mTextRect.height() / 2, mTextPaint)
        mTextPaint.color = mLabelTextColor
        mTextPaint.style = Paint.Style.FILL
        mTextPaint.textSize = mLabelTextSize.toFloat()
        mCanvas.drawText(mLabel, (width / 2).toFloat() + mTextRect.width() / 2 + dip(5), (height / 2).toFloat() + mTextRect.height() / 2, mTextPaint)
    }

    /**
     * 开始扫描
     */
    fun startScanning() {
        mDegree += 2f
        if (timer == null) {
            timer = Timer()
            timer?.schedule(timerTask { startScanning() }, 0, 5)
        }
        if (mDegree >= 360f) {
            mDegree = 1f
        }
    }

    /**
     * 停止扫描
     */
    fun stopScanning() {
        timer?.cancel()
        timer = null
    }
}
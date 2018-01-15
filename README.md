# ScanningProgressView
仿一个手机扫描打分的View，类似于手机管家打分的那种。

### 直接上图

![image](https://github.com/leiyun1993/ScanningProgressView/raw/master/screenshot/1.gif)

### 先绘制最外的圈和内部底色圆环

```kotlin
private fun drawBGCircle() {
    mCanvas.drawCircle((width / 2).toFloat(),
     (height / 2).toFloat(), 
     mRadius - mCircleStrokeWidth,
      mBGCirclePaint)
    mCanvas.drawCircle((width / 2).toFloat(), 
    (height / 2).toFloat(), 
    mRadius - mBGPaddingTop - mBGFillCirclePaint.strokeWidth / 2, 
    mBGFillCirclePaint)
}
```
### 绘制刻度线

画一圈梯度渲染的亮暗色渐变圆弧，重绘时不断旋转，上面盖一圈背景色的刻度线，由mDegree控制暗色所在角度。
```kotlin
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
```
### 绘制进度或分数

绘制大小不同的两部分文字，让大文字居中（进度或分数），小文字（label）靠大文字右显示
```kotlin
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
```
### 开始扫描和结束扫描

此处开始和结束使用Timer控制，没变化一次的时间为5ms
```kotlin
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
```

### 结束扫描绘制分数

绘制分数的原理和绘制刻度线一下，只是去掉了渐变色和变为按比例显示着色
```kotlin
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
```
最后加上分数绘制动画即可，此处使用ValueAnimator
```kotlin
private fun addScore() {
    val anim = ValueAnimator.ofInt(0, mScore)
    anim.addUpdateListener {
        mCurrDrawScore = it.animatedValue as Int
        invalidate()
    }
    anim.duration = mScore * 10L
    anim.start()
}
```
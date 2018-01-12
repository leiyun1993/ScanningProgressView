package com.yunlei.scanningprogressview

import android.os.Bundle
import android.os.CountDownTimer
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var mView: ScanningProgressView
    private var isStart = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mView = findViewById(R.id.scanningView)
        findViewById<Button>(R.id.btnClick).setOnClickListener {
            if (isStart) {
                mView.stopScanning()
            } else {
                mView.startScanning()
                timer.start()
            }
            isStart = !isStart

        }

    }

    val total = 100 * 100L
    val timer = object : CountDownTimer(total, 100L) {
        override fun onTick(millisUntilFinished: Long) {
            mView.mScanningPercent = 100 - (millisUntilFinished/100).toInt()
        }

        override fun onFinish() {
            mView.mScanningPercent = 88
            mView.mScore = 79
            mView.mLabel = "分"

        }

    }
}

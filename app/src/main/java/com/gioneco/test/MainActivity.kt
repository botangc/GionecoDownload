package com.gioneco.test

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.gioneco.download.constant.Constant
import com.gioneco.download.db.DBManager
import com.gioneco.download.listener.DownloadListener
import com.gioneco.download.utils.DownLoaderController
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {
    //文件下载地址
    private val downloadUrl =
        "http://gdown.baidu.com/data/wisegame/0d5a2f3c0e6b889c/qunaerlvxing_146.apk"
//    private val downloadUrl =
//        "http://10.160.1.238:3000/files/com.tencent.tmgp.sgame_1.61.1.6_61010601.apk"
    //下载线程个数
    private val threadCount = 5

    private var lastTime: Long = 0L
    private var mMax = 1L
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //本地保存文件名
        val filename = File(getExternalFilesDir(null), "update.apk").absolutePath
        val btnPausedownload = findViewById<Button>(R.id.stop_download)
        val btnStartdownload = findViewById<Button>(R.id.start_download)
        val reStartdownload = findViewById<Button>(R.id.re_download)
        val cleardownload = findViewById<Button>(R.id.clear_download)
        val tv_status = findViewById<TextView>(R.id.tv_status)
        tv_status.movementMethod = ScrollingMovementMethod.getInstance()
        val mProgressBar = findViewById<ProgressBar>(R.id.pbSmall)
        val mTvProgress = findViewById<TextView>(R.id.tv_progress)

        val controller = DownLoaderController()
        controller.setDebugMode(true)
        //开始(继续)下载
        btnStartdownload.setOnClickListener {
            tv_status.text = ""
            if (lastTime == 0L)
                lastTime = System.currentTimeMillis()
            controller.startDownload(
                this@MainActivity,
                downloadUrl,
                filename,
                threadCount,
                object : DownloadListener {
                    /**
                     * 开始下载
                     */
                    override fun onStart(size: Long) {
                        Log.d(Constant.TAG, "开始下载：$size")
                        mMax = size
                    }

                    /**
                     * 下载中
                     */
                    override fun onUpdate(progress: Long) {
                        val percent = (progress.toFloat() * 100 / mMax).toInt()
                        mProgressBar.progress = percent
                        Log.i("下载", "$progress ,$mMax , $percent%")
                        mTvProgress.text = "${percent}%"
                    }

                    /**
                     * 下载完成
                     */
                    override fun onComplete(url: String) {
                        Log.d(Constant.TAG, "下载完成：$url")
                        tv_status.text =
                            "下载完成：$url     耗时：" + calculateTime(
                                System.currentTimeMillis(),
                                lastTime
                            )
                    }

                    /**
                     * 下载失败
                     */
                    override fun onFail(errorMsg: String) {
                        mErrorMeg = mErrorMeg + errorMsg + "  ${System.currentTimeMillis()}" + "\n"
                        tv_status.text = mErrorMeg
                    }
                }
            )
        }
        //暂停下载
        btnPausedownload.setOnClickListener {
            controller.pauseDownload(downloadUrl)
        }
        //删除文件和数据库记录 重新下载
        reStartdownload.setOnClickListener {
            DBManager.getInstance(this).delete(downloadUrl)
            File(filename).apply {
                if (exists()) delete()
            }
            mProgressBar.progress = 0
            mMax = 0
            mTvProgress.text = "0%"
            btnStartdownload.performClick()
            lastTime = 0L
        }
        //清除记录
        cleardownload.setOnClickListener {
            mProgressBar.progress = 0
            mMax = 0
            mTvProgress.text = "0%"
            DBManager.getInstance(this).delete(downloadUrl)
            File(filename).apply {
                if (exists()) delete()
            }
            lastTime = 0L
        }
    }

    private var mErrorMeg = ""
    private fun calculateTime(start: Long, end: Long): String {
        val startStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date(start))
        val endStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date(end))

        val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        try {
            val d1 = df.parse(startStr)
            val d2 = df.parse(endStr)
            val diff = d1.time - d2.time//这样得到的差值是微秒级别

            val days = diff / (1000 * 60 * 60 * 24)
            val hours = (diff - days * (1000 * 60 * 60 * 24)) / (1000 * 60 * 60)
            val minutes =
                (diff - days * (1000 * 60 * 60 * 24) - hours * (1000 * 60 * 60)) / (1000 * 60)
            return "" + days + "天" + hours + "小时" + minutes + "分"
        } catch (e: Exception) {
        }
        return ""
    }
}

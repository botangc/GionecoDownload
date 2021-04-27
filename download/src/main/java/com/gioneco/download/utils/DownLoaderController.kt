package com.gioneco.download.utils

import android.content.Context
import android.os.Build
import com.gioneco.download.constant.Constant
import com.gioneco.download.download.FileDownloader
import com.gioneco.download.listener.DownloadListener
import java.net.HttpURLConnection
import java.net.URL

/**
 * 下载任务控制器
 *
 *Created by zsq
 *on 2021-01-08
 */
class DownLoaderController {
    /**
     * 设置debug模式
     *
     * @param isDebug true表示开启debug模式，false表示关闭
     */
    fun setDebugMode(isDebug: Boolean) {
        Constant.DEBUG = isDebug
    }

    /**
     * 开启下载
     *
     * @param context 上下文，最终用于获取DB实例
     * @param downLoadUrl 下载地址
     * @param filePath 文件路径
     * @param threadCount 单个任务中子线程数，即分片数
     * @param listener 下载回调
     */
    fun startDownload(
        context: Context,
        downLoadUrl: String,
        filePath: String,
        threadCount: Int,
        listener: DownloadListener
    ) {
        // 已经开启过下载任务
        val state = FileDownloader.getDownloadState(downLoadUrl)
        if (state == Constant.DOWNLOAD_STATE_INIT || state == Constant.DOWNLOAD_STATE_PAUSE) {
            ThreadPoolsUtil.instance.getFixedThreadPool().execute {
                val fileSize = getDownloadFileSize(downLoadUrl)
                FileDownloader
                    .init(context, downLoadUrl, fileSize, filePath, threadCount)
                    .startDownload(listener)
            }
        }
    }

    /**
     * 获取待下载文件的大小
     *
     * @param downLoadUrl 下载地址
     */
    private fun getDownloadFileSize(downLoadUrl: String): Long {
        var connection: HttpURLConnection? = null
        var fileSize = -1L
        try {
            val url = URL(downLoadUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            fileSize =
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) connection.contentLength.toLong()
                else connection.contentLengthLong
            "后台文件=$downLoadUrl, 总大小=$fileSize".logI()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                connection?.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return fileSize
    }

    /**
     * 暂停下载任务
     *
     * @param downLoadUrl 下载地址
     */
    fun pauseDownload(downLoadUrl: String) {
        FileDownloader.putDownloadState(downLoadUrl, Constant.DOWNLOAD_STATE_PAUSE)
    }

    /**
     * 结束下载任务
     *
     * @param downLoadUrl 下载地址
     */
    fun finishDownload(downLoadUrl: String) {
        FileDownloader.removeDownloadState(downLoadUrl)
    }
}
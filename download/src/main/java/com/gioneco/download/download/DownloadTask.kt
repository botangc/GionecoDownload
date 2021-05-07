package com.gioneco.download.download

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.gioneco.download.bean.DownloadInfo
import com.gioneco.download.constant.Constant
import com.gioneco.download.db.DBManager
import com.gioneco.download.listener.DownloadListener
import com.gioneco.download.utils.logE
import com.gioneco.download.utils.logI
import java.io.BufferedInputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL

/**
 * 实际分片的下载任务
 *
 *Created by zsq
 *on 2021-01-08
 */
class DownloadTask(
    private val context: Context,
    private val downLoadUrl: String,
    private val fileSize: Long,
    private val filePath: String,
    private val threadId: Int,
    private val listener: DownloadListener
) : Runnable {
    /**
     * 切换至主线程，确保监听回调在主线程中
     */
    private val mHandler = Handler(Looper.getMainLooper())

    /**
     * 请求超时标志，请求超时需重试，非请求超时则直接报错
     */
    private var mTimeOutFlag = false

    /**
     * 已重试次数
     */
    private var mRetryCount = 0

    /**
     * 最大重试次数
     */
    private val mMaxRetryCount = 10

    /**
     * 下载中
     */
    private val mTypeKeep = 1

    /**
     * 下载完成
     */
    private val mTypeCompleted = 2

    /**
     * 下载失败
     */
    private val mTypeFail = 3

    override fun run() {
        FileDownloader.putDownloadState(downLoadUrl, Constant.DOWNLOAD_STATE_START)
        var connection: HttpURLConnection? = null
        var inputStream: BufferedInputStream? = null
        var mRandomAccessFile: RandomAccessFile? = null
        while (mRetryCount < mMaxRetryCount) {
            // 每次重试时重置超时标志，避免第一次超时后会重复下载
            mTimeOutFlag = false
            var info: DownloadInfo? = null
            // 判断是否存在未完成的该任务
            if (DBManager.getInstance(context).isExist(downLoadUrl)) {
                info = DBManager.getInstance(context).getInfo(downLoadUrl, threadId)
            }
            try {
                val url = URL(downLoadUrl)
                var completeSize = info?.completeSize ?: 0L
                // 本地数据库中保存的分片开始位置跟结束位置
                val startPos = info?.startPos ?: 0L
                val endPos = info?.endPos ?: 0L
                val realStartPos = startPos + completeSize
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.setRequestProperty("Connection", "Keep-Alive")
                connection.setRequestProperty("Range", "bytes=$realStartPos-$endPos")
                "线程id:$threadId, 开始位置: $startPos, Range: bytes=$realStartPos-$endPos, value=${connection.headerFields["Content-Length"]}".logE()
                inputStream = BufferedInputStream(connection.inputStream)
                mRandomAccessFile = RandomAccessFile(filePath, "rw")
                //上次的最后的写入位置
                mRandomAccessFile.seek(realStartPos)
                val buffer = ByteArray(8 * 1024)
                var length = 0
                while ({ length = inputStream.read(buffer);length }() > 0) {
                    // 每次写入时，检测下载任务是否被停止
                    if (FileDownloader.getDownloadState(downLoadUrl) != Constant.DOWNLOAD_STATE_START) {
                        return
                    }
                    "线程id:$threadId ------写入: $length，had=$completeSize/$endPos".logI()
                    mRandomAccessFile.write(buffer, 0, length)
                    completeSize += length
                    // 保存数据库中的下载进度
                    DBManager.getInstance(context).updateInfo(threadId, completeSize, downLoadUrl)
                    // 更新进度条
                    sendMessage(mTypeKeep, calculateCompleteSize(), "")
                }
                "线程id: $threadId 已完成: ${calculateCompleteSize()} 总大小: $fileSize".logI()
                // 判断下载是否完成
                if (calculateCompleteSize() >= fileSize) {
                    sendMessage(mTypeCompleted, -1L, "")
                    break
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (e.message == "timeout") {
                    mTimeOutFlag = true
                }
                if (!mTimeOutFlag) {
                    sendMessage(
                        mTypeFail,
                        -1L,
                        "下载失败, 线程id:$threadId msg：${e.message} 重试次数:$mRetryCount"
                    )
                } else if (mRetryCount == mMaxRetryCount - 1) { //当下载了10都次失败 就终止下载
                    sendMessage(
                        mTypeFail,
                        -1L,
                        "下载失败, 线程id:$threadId 重试超限${e.message} 重试次数:$mRetryCount"
                    )
                }
                // 如果是重试，将次数暴露给UI
                if (Constant.DEBUG && mTimeOutFlag) {
                    mHandler.post {
                        listener.onFail("线程id:${threadId}   重试次数：${mRetryCount + 1}")
                    }
                }
            } finally {
                try {
                    inputStream?.close()
                    connection?.disconnect()
                    mRandomAccessFile?.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            if (mTimeOutFlag) {
                mRetryCount++
            } else break
        }
    }

    /**
     * 发送当前下载状态相关信息
     *
     * @param what 消息类型，[mTypeKeep]下载中，[mTypeCompleted]下载完成，[mTypeFail]下载失败
     * @param size 已下载大小，非进度相关时为-1
     * @param errorMsg 错误信息
     */
    private fun sendMessage(what: Int, size: Long, errorMsg: String) {
        mHandler.post {
            when (what) {
                mTypeKeep -> listener.onUpdate(size)
                mTypeCompleted -> {
                    FileDownloader.removeDownloadState(downLoadUrl)
                    DBManager.getInstance(context).delete(downLoadUrl)
                    listener.onComplete(downLoadUrl, filePath)
                }
                mTypeFail -> {
                    FileDownloader.putDownloadState(downLoadUrl, Constant.DOWNLOAD_STATE_PAUSE)
                    listener.onFail(errorMsg)
                }
            }
        }
    }

    /**
     * 计算总文件已下载大小
     */
    private fun calculateCompleteSize(): Long {
        var completeSize = 0L
        val infoList = DBManager.getInstance(context).getInfoList(downLoadUrl)
        for (info in infoList) {
            // 累计各分片下载大小
            completeSize += info.completeSize
        }
        return completeSize
    }
}
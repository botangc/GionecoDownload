package com.gioneco.download.download

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.gioneco.download.bean.DownloadInfo
import com.gioneco.download.constant.Constant
import com.gioneco.download.db.DBManager
import com.gioneco.download.listener.DownloadListener
import com.gioneco.download.utils.ThreadPoolsUtil
import com.gioneco.download.utils.logI
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

/**
 * 单独整个文件下载任务器，包含一个或多个分片下载任务
 * 注意：多个文件下载时没有处理，目前仅支持单个文件下载
 *
 *Created by zsq
 *on 2021-01-08
 */
object FileDownloader {
    /**
     * 上下文
     */
    private lateinit var context: Context

    /**
     * 下载地址
     */
    private lateinit var downloadUrl: String

    /**
     * 文件存储路径
     */
    private lateinit var filePath: String

    /**
     * 文件大小
     */
    private var fileSize: Long = 0

    /**
     * 子线程数（分片数）
     */
    private var threadCount: Int = 0

    /**
     * 文件下载状态Map
     */
    private val downloadStateMap: HashMap<String, Int> = HashMap()

    /**
     * 应用开始回调主线程切换Handler
     */
    private val mHandler = Handler(Looper.getMainLooper())

    /**
     * 初始化下载任务信息
     *
     * @param context 上下文，最终用于获取DB实例
     * @param downloadUrl 下载地址
     * @param fileSize 待下载文件总大小
     * @param filePath 文件路径
     * @param threadCount 单个任务中子线程数，即分片数
     */
    @Synchronized
    fun init(
        context: Context,
        downloadUrl: String,
        fileSize: Long,
        filePath: String,
        threadCount: Int
    ): FileDownloader {
        "下载参数：downLoadUrl=$downloadUrl, fileSize=$fileSize, filename=$filePath, threadCount=$threadCount".logI()
        this.context = context.applicationContext
        this.downloadUrl = downloadUrl
        this.fileSize = fileSize
        this.filePath = filePath
        this.threadCount = threadCount
        initData()
        return this
    }

    /**
     * 初始化数据，将分片下载任务信息存入数据库
     */
    private fun initData() {
        if (fileSize == -1L) {
            return
        }
        var accessFile: RandomAccessFile? = null
        try {
            val file = File(filePath)
            if (file.parentFile?.exists() == false) {
                file.parentFile?.mkdirs()
            }
            // 判断数据库中是否有下载记录
            if (!DBManager.getInstance(context).isExist(downloadUrl)) {
                // 分片数量
                val block = fileSize / threadCount + if (fileSize % threadCount == 0L) 0L else 1L
                // 将分片下载信息存入数据库
                for (i in 0 until threadCount) {
                    val info = DownloadInfo(
                        i,
                        i * block,
                        if (i == threadCount - 1) fileSize - 1 else (i + 1) * block,
                        0L,
                        downloadUrl
                    )
                    DBManager.getInstance(context).saveInfo(info)
                }
            }
            accessFile = RandomAccessFile(file, "rw")
            if (accessFile.length() == fileSize) {
                return
            }
            accessFile.setLength(fileSize)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                accessFile?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 获取下载的状态
     *
     * @param downloadUrl 下载地址
     */
    fun getDownloadState(downloadUrl: String) =
        downloadStateMap[downloadUrl] ?: Constant.DOWNLOAD_STATE_INIT

    /**
     * 存储下载地址对应的下载状态
     * 当state==[Constant.DOWNLOAD_STATE_PAUSE]时，下载任务暂停；
     * 当state==[Constant.DOWNLOAD_STATE_START]配合[DownloadTask]开启或重试时使用，单独修改状态不能开启下载任务；
     * 开启下载任务，需通过[startDownload]
     *
     * @param downloadUrl 下载地址
     * @param state 当前状态
     */
    @Synchronized
    fun putDownloadState(downloadUrl: String, state: Int) {
        downloadStateMap[downloadUrl] = state
    }

    /**
     * 移除下载任务
     *
     * @param downloadUrl 下载地址
     */
    fun removeDownloadState(downloadUrl: String) {
        if (downloadStateMap[downloadUrl] != Constant.DOWNLOAD_STATE_INIT) {
            downloadStateMap.remove(downloadUrl)
        }
    }

    /**
     * 开始下载
     *
     * @param listener 下载回调
     */
    @Synchronized
    fun startDownload(listener: DownloadListener) {
        if (downloadStateMap[downloadUrl] == Constant.DOWNLOAD_STATE_START) {
            return
        }
        // 开启下载任务前，必须调用init
        if (::downloadUrl.isInitialized.not() || fileSize == -1L) {
            mHandler.post {
                listener.onFail(if (fileSize == -1L) "file size is -1" else "please call init first")
            }
            return
        }
        // 回调任务开始
        mHandler.post { listener.onStart(fileSize) }
        // 开启分片下载任务
        for (i in 0 until threadCount) {
            ThreadPoolsUtil.instance.getCachedThreadPool()
                .execute(DownloadTask(context, downloadUrl, fileSize, filePath, i, listener))
        }
    }
}
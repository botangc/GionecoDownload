package com.gioneco.download.listener

/**
 *Created by zsq
 *on 2021-01-11
 */
interface DownloadListener {

    /**
     * 开始下载
     *
     * @param size 文件大小
     */
    fun onStart(size: Long)

    /**
     * 下载中 字节
     *
     * @param progress 已下载大小
     */
    fun onUpdate(progress: Long)

    /**
     * 下载完成
     *
     * @param url 下载地址
     * @param path 文件地址
     */
    fun onComplete(url: String, path: String)

    /**
     * 下载失败
     *
     * @param errorMsg 错误信息
     */
    fun onFail(errorMsg: String)
}
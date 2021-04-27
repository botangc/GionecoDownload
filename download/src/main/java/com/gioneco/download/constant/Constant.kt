package com.gioneco.download.constant

/**
 *Created by zsq
 *on 2021-01-08
 */
object Constant {
    /**
     * 日志打印Tag
     */
    const val TAG = "GionecoDownload"

    /**
     * 是否为debug模式，如控制打印日志，显示重试等
     */
    var DEBUG = false

    /**
     * 初始状态
     */
    const val DOWNLOAD_STATE_INIT = -1

    /**
     * 暂停状态
     */
    const val DOWNLOAD_STATE_PAUSE = 1

    /**
     * 下载状态
     */
    const val DOWNLOAD_STATE_START = 2

    /**
     * 完成状态
     */
    const val DOWNLOAD_STATE_FINISH = 3
}
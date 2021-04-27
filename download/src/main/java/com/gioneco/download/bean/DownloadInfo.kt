package com.gioneco.download.bean

/**
 * 下载信息
 * 注意如果字段有所变更，需更新数据库[com.gioneco.download.db.DBHelper]字段
 *
 *Created by zsq
 *on 2021-01-08
 */
data class DownloadInfo(
    val threadId: Int,// 下载器id
    val startPos: Long,// 开始点
    val endPos: Long,// 结束点
    val completeSize: Long,// 完成度
    val url: String// 下载器网络标识
)
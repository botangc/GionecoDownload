package com.gioneco.download.db

import android.content.Context
import com.gioneco.download.bean.DownloadInfo

/**
 * 数据库管理器
 *
 *Created by zsq
 *on 2021-01-08
 */
class DBManager private constructor(context: Context) {

    companion object {
        /**
         * 数据库管理器实例
         */
        @Volatile
        private var instance: DBManager? = null

        /**
         * 获取实例
         *
         * @param context 上下文
         */
        fun getInstance(context: Context): DBManager {
            if (instance == null) {
                synchronized(DBManager::class) {
                    if (instance == null) {
                        // 注意避免内存泄漏
                        instance = DBManager(context.applicationContext)
                    }
                }
            }
            return instance!!
        }
    }

    /**
     * 数据库工具
     */
    private var helper: DBHelper = DBHelper(context)

    /**
     * 批量存储下载信息
     *
     * @param infos 下载列表
     */
    fun saveInfos(infos: List<DownloadInfo>) {
        val db = helper.writableDatabase
        for (info in infos) {
            db.execSQL(
                "insert into downloadinfo(thread_id, start_pos, end_pos, complete_size, url) values (?,?,?,?,?)",
                arrayOf(info.threadId, info.startPos, info.endPos, info.completeSize, info.url)
            )
        }
    }

    /**
     * 存储下载信息
     *
     * @param info 下载信息
     */
    fun saveInfo(info: DownloadInfo) {
        val db = helper.writableDatabase
        db.execSQL(
            "insert into downloadinfo(thread_id, start_pos, end_pos, complete_size, url) values (?,?,?,?,?)",
            arrayOf(info.threadId, info.startPos, info.endPos, info.completeSize, info.url)
        )
    }

    /**
     * 查看数据库中是否有数据
     *
     * @param url 下载地址
     */
    fun isExist(url: String): Boolean {
        val db = helper.writableDatabase
        val cursor = db.query("downloadinfo", null, "url = ?", arrayOf(url), null, null, null)
        val exists = cursor.moveToNext()
        cursor.close()
        return exists
    }

    /**
     * 关闭数据库连接
     */
    fun closeDb() {
        helper.close()
    }

    /**
     * 删除某个下载记录
     *
     * @param url 下载地址
     */
    fun delete(url: String) {
        val db = helper.writableDatabase
        db.delete("downloadinfo", "url=?", arrayOf(url))
        db.close()
    }

    /**
     * 更新下载信息
     *
     * @param threadId 线程id
     * @param completeSize 完成大小
     * @param url 下载地址
     */
    fun updateInfo(threadId: Int, completeSize: Long, url: String) {
        val db = helper.writableDatabase
        val sql = "update downloadinfo set complete_size=? where thread_id=? and url=?"
        val bindArgs = arrayOf(completeSize, threadId, url)
        db.execSQL(sql, bindArgs)
    }

    /**
     * 获取某个下载地址所有相关下载具体信息
     *
     * @param url 下载地址
     */
    fun getInfoList(url: String): List<DownloadInfo> {
        val db = helper.writableDatabase
        val list = ArrayList<DownloadInfo>()
        val sql =
            "select thread_id, start_pos, end_pos, complete_size, url from downloadinfo where url=?"
        val cursor = db.rawQuery(sql, arrayOf(url))
        while (cursor.moveToNext()) {
            val info = DownloadInfo(
                cursor.getInt(0),
                cursor.getLong(1),
                cursor.getLong(2),
                cursor.getLong(3),
                cursor.getString(4)
            )
            list.add(info)
        }
        cursor.close()
        return list
    }

    /**
     * 获取某个下载地址的某个分片信息
     *
     * @param url 下载地址
     * @param threadId 线程id
     */
    fun getInfo(url: String, threadId: Int): DownloadInfo {
        val db = helper.writableDatabase
        val sql =
            "select thread_id, start_pos, end_pos, complete_size, url from downloadinfo where url=? and thread_id=?"
        val cursor = db.rawQuery(sql, arrayOf(url, threadId.toString()))
        cursor.moveToFirst()
        val info = DownloadInfo(
            cursor.getInt(cursor.getColumnIndex("thread_id")),
            cursor.getLong(cursor.getColumnIndex("start_pos")),
            cursor.getLong(cursor.getColumnIndex("end_pos")),
            cursor.getLong(cursor.getColumnIndex("complete_size")),
            cursor.getString(cursor.getColumnIndex("url"))
        )
        cursor.close()
        return info
    }
}
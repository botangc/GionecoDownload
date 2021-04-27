package com.gioneco.download.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * 数据库工具类：1、创建；2、升级
 *
 *Created by zsq
 *on 2021-01-08
 */
class DBHelper(context: Context) : SQLiteOpenHelper(context, "down.db", null, 1) {

    /**
     * 数据库创建回调
     */
    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL("create table if not exists downloadinfo(_id integer PRIMARY KEY AUTOINCREMENT, thread_id integer, start_pos integer, end_pos integer, complete_size integer, url char)")
    }

    /**
     * 数据库升级
     */
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {

    }
}
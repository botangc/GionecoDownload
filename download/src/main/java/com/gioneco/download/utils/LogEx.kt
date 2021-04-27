package com.gioneco.download.utils

import android.util.Log
import com.gioneco.download.constant.Constant

/**
 * 日志工具扩展
 *
 *Created by zsq
 *on 2021-01-13
 */

/**
 * Info级别日志输出
 *
 * @param tag 日志标志，默认为[Constant.TAG]
 */
fun <T> T.logI(tag: String = Constant.TAG) {
    if (Constant.DEBUG) Log.i(tag, this.toString())
}

/**
 * Error级别日志输出
 *
 * @param tag 日志标志，默认为[Constant.TAG]
 */
fun <T> T.logE(tag: String = Constant.TAG) {
    if (Constant.DEBUG) Log.e(tag, this.toString())
}
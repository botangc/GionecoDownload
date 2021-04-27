package com.gioneco.download.utils

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * 线程池工具类，统一管理下载线程
 * 可获取文件下载线程池与分片下载线程池
 *
 *Created by zsq
 *on 2021-01-08
 */
class ThreadPoolsUtil private constructor() {
    /**
     * 获取文件大小  定长线程池，可控制线程最大并发数，超出的线程会在队列中等待
     */
    private val fixedThreadPool: ExecutorService = Executors.newFixedThreadPool(3)

    /**
     * 下载线程池 可缓存 灵活回收空闲线程，若无可回收，则新建线程
     */
    private val cachedThreadPool: ExecutorService = Executors.newCachedThreadPool()

    companion object {
        val instance = ThreadPoolsUtilHolder.threadPoolsUtil
    }

    private object ThreadPoolsUtilHolder {
        val threadPoolsUtil = ThreadPoolsUtil()
    }

    /**
     * 获取固定线程池，用于处理单个的下载任务
     */
    fun getFixedThreadPool(): ExecutorService {
        return fixedThreadPool
    }

    /**
     * 获取缓存线程池，用于处理分片下载任务
     */
    fun getCachedThreadPool(): ExecutorService {
        return cachedThreadPool
    }
}
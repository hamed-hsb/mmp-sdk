package com.motrack.sdk.scheduler

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors

/**
 * @author yaya (@yahyalmh)
 * @since 23th October 2021
 */

abstract class AsyncTaskExecutor<Params, Result> {
    protected abstract fun doInBackground(params: Array<out Params>): Result

    protected fun onPreExecute() {}

    protected open fun onPostExecute(result: Result) {}

    @SafeVarargs
    fun execute(vararg params: Params): AsyncTaskExecutor<Params, Result> {
        onPreExecute()
        val handler = Handler(Looper.getMainLooper())
        val executorService = Executors.newSingleThreadExecutor()
        executorService.execute {
            val result: Result = doInBackground(params)
            handler.post { onPostExecute(result) }
        }
        return this
    }
}
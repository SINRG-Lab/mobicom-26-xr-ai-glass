package com.sdk.glassessdksample

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Field
import com.google.android.gms.fitness.request.DataReadRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class HealthAPI(
    private val context: Context,
    private val account: GoogleSignInAccount
) {
    suspend fun getHeartRateFromGoogleFit(): String = withContext(Dispatchers.IO) {
        try {
            val end = System.currentTimeMillis()
            val start = end - TimeUnit.HOURS.toMillis(1)

            val request = DataReadRequest.Builder()
                .read(DataType.TYPE_HEART_RATE_BPM)
                .setTimeRange(start, end, TimeUnit.MILLISECONDS)
                .build()

            val response = Fitness.getHistoryClient(context, account)
                .readData(request)
                .await()

            val bpm = response.dataSets
                .flatMap { it.dataPoints }
                .mapNotNull { it.getValue(Field.FIELD_BPM)?.asFloat() }
                .average()

            if (bpm.isNaN()) "No heart rate data"
            else "Your average heart rate is ${bpm.toInt()} bpm"
        } catch (e: Exception) {
            Log.e("AIClient", "Error getting heart rate", e)
            "Error retrieving heart rate"
        }
    }

    suspend fun getStepsFromGoogleFit(): String = withContext(Dispatchers.IO) {
        try {
            val end = System.currentTimeMillis()
            val start = end - TimeUnit.DAYS.toMillis(1)

            val request = DataReadRequest.Builder()
                .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                .setTimeRange(start, end, TimeUnit.MILLISECONDS)
                .bucketByTime(1, TimeUnit.DAYS)
                .build()

            val response = Fitness.getHistoryClient(context, account)
                .readData(request)
                .await()

            val steps = response.buckets
                .flatMap { it.dataSets }
                .flatMap { it.dataPoints }
                .sumOf { it.getValue(Field.FIELD_STEPS).asInt() }

            "You took $steps many steps yesterday"
        } catch (e: Exception) {
            Log.e("AIClient", "Error getting steps", e)
            "Error retrieving steps"
        }
    }

    suspend fun getSleepDataFromGoogleFit(): String = withContext(Dispatchers.IO) {
        try {
            val end = System.currentTimeMillis()
            val start = end - TimeUnit.DAYS.toMillis(1)

            val request = DataReadRequest.Builder()
                .read(DataType.TYPE_SLEEP_SEGMENT)
                .setTimeRange(start, end, TimeUnit.MILLISECONDS)
                .build()

            val response = Fitness.getHistoryClient(context, account)
                .readData(request)
                .await()

            val segments = response.dataSets
                .flatMap { it.dataPoints }
                .map { it.getValue(Field.FIELD_SLEEP_SEGMENT_TYPE).asInt() }

            if (segments.isEmpty()) {
                "No sleep data available"
            } else {
                "Your sleep recorded from yesterday is ${segments.size}"
            }
        } catch (e: Exception) {
            Log.e("AIClient", "Error getting sleep data", e)
            "Error retrieving sleep data"
        }
    }
}
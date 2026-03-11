package com.naze.do_swipe.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Firebase Analytics 기반 [Analytics] 구현체.
 */
class FirebaseAnalyticsTracker(context: Context) : Analytics {

    private val firebaseAnalytics: FirebaseAnalytics =
        FirebaseAnalytics.getInstance(context)

    override fun logEvent(name: String, params: Map<String, Any>?) {
        val bundle = params?.toBundle()
        firebaseAnalytics.logEvent(name, bundle)
    }

    private fun Map<String, Any>.toBundle(): Bundle {
        return Bundle().apply {
            forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Double -> putDouble(key, value)
                    is Boolean -> putBoolean(key, value)
                    else -> putString(key, value.toString())
                }
            }
        }
    }
}

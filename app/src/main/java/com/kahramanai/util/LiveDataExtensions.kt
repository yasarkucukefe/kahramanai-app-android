package com.kahramanai.util

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

/**
 * Helper extension to observe a [LiveData] only once.
 *
 * This is useful for one-shot operations such as network calls
 * where you don't want observers to accumulate and be triggered
 * multiple times.
 */
fun <T> LiveData<T>.observeOnce(
    owner: LifecycleOwner,
    observer: (T) -> Unit
) {
    val wrapper = object : Observer<T> {
        override fun onChanged(value: T) {
            observer(value)
            removeObserver(this)
        }
    }
    observe(owner, wrapper)
}

/**
 * Helper extension to observe a [LiveData] of [NetworkResult] such that:
 * - All emissions (including [NetworkResult.Loading]) are forwarded to [observer]
 * - The observer is automatically removed after the first non-loading
 *   result ([NetworkResult.Success] or [NetworkResult.Error]).
 *
 * This matches typical one-shot network call semantics where you want to
 * respond to loading, then clean up after completion.
 */
fun <T> LiveData<NetworkResult<T>>.observeNetworkResultOnce(
    owner: LifecycleOwner,
    observer: (NetworkResult<T>) -> Unit
) {
    val wrapper = object : Observer<NetworkResult<T>> {
        override fun onChanged(value: NetworkResult<T>) {
            observer(value)
            if (value !is NetworkResult.Loading) {
                removeObserver(this)
            }
        }
    }
    observe(owner, wrapper)
}




package org.intelehealth.klivekit.utils.extensions

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

fun <T> MutableLiveData<T>.hide(): LiveData<T> = this
fun <T> LiveData<T>.hide(): LiveData<T> = this
fun <T> MutableStateFlow<T>.hide(): StateFlow<T> = this
fun <T> Flow<T>.hide(): Flow<T> = this

inline fun <T, R> Flow<T?>.flatMapLatestOrNull(
    crossinline transform: suspend (value: T) -> Flow<R>
): Flow<R?> {
    return flatMapLatest {
        if (it == null) {
            flowOf(null)
        } else {
            transform(it)
        }
    }
}

fun <T> LiveData<T>.observeOnce(lifecycleOwner: LifecycleOwner, observer: Observer<T>) {
    observe(lifecycleOwner, object : Observer<T> {
        override fun onChanged(value: T) {
            observer.onChanged(value)
            if (value is Boolean && value) removeObserver(this)
        }
    })
}
package com.example

import android.app.Application
import android.content.Context
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineExceptionHandler

/**
 * Early process-wide initialization and crash-resilience host for RIFT.
 *
 * Ensures default [FirebaseApp] is safely initialized before any [com.google.firebase.auth.FirebaseAuth],
 * [com.google.firebase.firestore.FirebaseFirestore], `RiftRepository`, or `RiftViewModel` access runs,
 * including on GitHub APK/AAB builds where `google-services.json` is absent. Also installs a defensive
 * uncaught-exception and coroutine guard so non-fatal background SDK errors on first launch never close
 * the app immediately.
 */
class RiftApplication : Application() {

  override fun onCreate() {
    super.onCreate()
    installUncaughtExceptionGuard()
    ensureSafeFirebaseInitialized(this)
  }

  companion object {
    const val LOG_TAG = "RiftApplication"

    private val initLock = Any()
    private val uncaughtGuardInstalled = AtomicBoolean(false)
    private val mainThreadRecoveryCount = AtomicInteger(0)
    private val lastMainThreadFaultElapsedMs = AtomicLong(0L)

    @Volatile
    var isUsingFallbackFirebase: Boolean = false
      private set

    val lastRecoveredStartupError: AtomicReference<String?> = AtomicReference(null)

    val safeCoroutineExceptionHandler: CoroutineExceptionHandler =
      CoroutineExceptionHandler { _, throwable ->
        recordNonFatalStartupError("Recovered coroutine exception", throwable)
      }

    fun recordNonFatalStartupError(contextLabel: String, throwable: Throwable) {
      val summary = "$contextLabel: ${throwable::class.java.simpleName}: ${throwable.message.orEmpty()}"
      lastRecoveredStartupError.set(summary)
      Log.e(LOG_TAG, summary, throwable)
    }

    /**
     * Guarantees a valid default [FirebaseApp] exists in the current process.
     *
     * When `google-services.json` was present at build time, `FirebaseInitProvider` or
     * `FirebaseApp.initializeApp(appCtx)` initializes the real project configuration.
     * When `google-services.json` was absent (such as standalone GitHub Actions builds),
     * initializes an offline-safe fallback [FirebaseOptions] with format-valid
     * `applicationId`, `apiKey`, `projectId`, and `gcmSenderId` so `Firebase.auth` and
     * `FirebaseFirestore.getInstance(...)` never throw `IllegalStateException` or
     * `IllegalArgumentException` on startup.
     */
    fun ensureSafeFirebaseInitialized(context: Context): Boolean {
      val appCtx = context.applicationContext ?: context
      if (FirebaseApp.getApps(appCtx).isNotEmpty()) {
        return true
      }
      synchronized(initLock) {
        try {
          if (FirebaseApp.getApps(appCtx).isNotEmpty()) {
            return true
          }
          runCatching { FirebaseApp.initializeApp(appCtx) }
          if (FirebaseApp.getApps(appCtx).isNotEmpty()) {
            isUsingFallbackFirebase = false
            return true
          }

          val fallbackApiKey =
            buildString(39) {
              append("AI")
              append("za")
              repeat(35) { append('0') }
            }
          val fallbackOptions =
            FirebaseOptions.Builder()
              .setApplicationId("1:000000000000:android:0000000000000000000000")
              .setApiKey(fallbackApiKey)
              .setProjectId("rift-offline-fallback")
              .setGcmSenderId("000000000000")
              .build()

          FirebaseApp.initializeApp(appCtx, fallbackOptions)
          isUsingFallbackFirebase = true
          Log.w(
            LOG_TAG,
            "Initialized offline-safe fallback FirebaseApp (google-services.json resources absent).",
          )
          return FirebaseApp.getApps(appCtx).isNotEmpty()
        } catch (t: Throwable) {
          recordNonFatalStartupError("Firebase fallback initialization error", t)
          return FirebaseApp.getApps(appCtx).isNotEmpty()
        }
      }
    }

    /**
     * Installs an idempotent uncaught exception guard so background worker exceptions (such as
     * offline Firebase Installations, gRPC, App Check, Play Core, or audio threads) never kill
     * the process on launch, and transient main-looper callback faults recover up to 3 times
     * before delegating to the system handler.
     */
    fun installUncaughtExceptionGuard() {
      if (!uncaughtGuardInstalled.compareAndSet(false, true)) {
        return
      }
      val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
      Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        recordNonFatalStartupError("Uncaught exception on thread ${thread.name}", throwable)

        if (throwable is VirtualMachineError) {
          previousHandler?.uncaughtException(thread, throwable)
          return@setDefaultUncaughtExceptionHandler
        }

        val mainLooper = runCatching { Looper.getMainLooper() }.getOrNull()
        val isMainThread = mainLooper != null && mainLooper.thread === thread
        if (!isMainThread) {
          // Absorb non-fatal background worker exceptions so the UI stays alive.
          return@setDefaultUncaughtExceptionHandler
        }

        val nowElapsed = SystemClock.elapsedRealtime()
        val prevElapsed = lastMainThreadFaultElapsedMs.getAndSet(nowElapsed)
        if (nowElapsed - prevElapsed > 15_000L) {
          mainThreadRecoveryCount.set(0)
        }

        if (mainThreadRecoveryCount.incrementAndGet() <= 3) {
          while (true) {
            try {
              Looper.loop()
              return@setDefaultUncaughtExceptionHandler
            } catch (nextThrowable: Throwable) {
              recordNonFatalStartupError("Main looper recovery fault", nextThrowable)
              if (
                nextThrowable is VirtualMachineError ||
                  mainThreadRecoveryCount.incrementAndGet() > 3
              ) {
                previousHandler?.uncaughtException(thread, nextThrowable)
                return@setDefaultUncaughtExceptionHandler
              }
            }
          }
        } else {
          previousHandler?.uncaughtException(thread, throwable)
        }
      }
    }
  }
}

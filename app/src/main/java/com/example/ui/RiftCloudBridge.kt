package com.example.ui

import android.webkit.JavascriptInterface

class RiftCloudBridge(
  private val viewModel: RiftViewModel,
  private val onRequestSignOut: () -> Unit,
  private val onRequestOrientationToggle: () -> Unit,
) {

  @JavascriptInterface
  fun getCloudSnapshot(): String {
    return viewModel.buildCloudSnapshotJson()
  }

  @JavascriptInterface
  fun syncStats(payloadJson: String) {
    viewModel.syncFromEngineJson(payloadJson)
  }

  @JavascriptInterface
  fun syncTelemetry(payloadJson: String) {
    viewModel.syncTelemetryFromEngineJson(payloadJson)
  }

  @JavascriptInterface
  fun signOutPilot() {
    onRequestSignOut()
  }

  @JavascriptInterface
  fun toggleNativeOrientation() {
    onRequestOrientationToggle()
  }
}

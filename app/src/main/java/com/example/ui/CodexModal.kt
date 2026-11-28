package com.example.ui

import androidx.compose.runtime.Composable

/**
 * Retired in BUILD #141.
 * Live Tactical Codex now runs inside OrbitalHubSpatialStage via LiveCodexSideBay (RiftLiveSubBays.kt).
 * BuffIconPreview has been hoisted to RiftHubCardsAndStoreBays.kt with a borderless radial halo.
 * Minimal no-op stub retained only because untouched legacy SettingsModal.kt references CodexModal.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun CodexModal(
  isNexusStreaming: Boolean = false,
  onOpenNexus: () -> Unit = {},
  onCloseModal: () -> Unit = {},
) {}

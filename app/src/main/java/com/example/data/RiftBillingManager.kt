package com.example.data

import android.app.Activity
import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

const val STUDIO_LEGAL_NAME: String = "Threshold Dynamics Studio, LLC"
const val ENABLE_LIVE_PLAY_BILLING: Boolean = false
const val TESTER_INFINITE_SHARD_BALANCE: Long = 999_999L

val TESTER_INFINITE_SHARDS_ACTIVE: Boolean
  get() = !RiftBillingManager.isLivePlayBillingEnabled

data class ShardVaultSkuPack(
  val productId: String,
  val title: String,
  val shardAmount: Long,
  val bonusShards: Long,
  val displayPriceUsd: String,
  val badgeLabel: String,
  val accentHex: String,
) {
  val totalShards: Long
    get() = shardAmount + bonusShards
}

sealed interface ShardPurchaseResult {
  data class VerifiedAndCredited(
    val productId: String,
    val purchaseToken: String,
    val shardsCredited: Long,
  ) : ShardPurchaseResult

  data class TesterModeDormantNotice(
    val productId: String,
    val message: String,
  ) : ShardPurchaseResult

  data class BillingUnavailable(
    val reason: String,
  ) : ShardPurchaseResult
}

/**
 * Production-ready Google Play Billing & Tester Treasury controller for NEXUS
 * (Threshold Dynamics Studio, LLC).
 *
 * While [ENABLE_LIVE_PLAY_BILLING] is `false`, all testers operate with a non-depleting
 * `999,999❖` Rift Shard treasury so every cosmetic, Rift Frame bay (A–E), respec, and
 * Hull Forge voucher can be tested freely while commercial Play Billing stays cleanly dormant.
 */
object RiftBillingManager {
  const val STUDIO_LEGAL_NAME: String = com.example.data.STUDIO_LEGAL_NAME
  const val ENABLE_LIVE_PLAY_BILLING: Boolean = com.example.data.ENABLE_LIVE_PLAY_BILLING
  const val TESTER_INFINITE_SHARD_BALANCE: Long = com.example.data.TESTER_INFINITE_SHARD_BALANCE

  val isLivePlayBillingEnabled: Boolean
    get() = System.getProperty("nexus.live.play.billing")?.toBoolean() ?: ENABLE_LIVE_PLAY_BILLING

  val TESTER_INFINITE_SHARDS_ACTIVE: Boolean
    get() = !isLivePlayBillingEnabled

  val OFFICIAL_SKU_CATALOG: List<ShardVaultSkuPack> =
    listOf(
      ShardVaultSkuPack(
        productId = "shards_250",
        title = "DRIFTER POUCH",
        shardAmount = 250L,
        bonusShards = 0L,
        displayPriceUsd = "$1.99",
        badgeLabel = "STARTER",
        accentHex = "#38BDF8",
      ),
      ShardVaultSkuPack(
        productId = "shards_700",
        title = "VANGUARD CACHE",
        shardAmount = 600L,
        bonusShards = 100L,
        displayPriceUsd = "$4.99",
        badgeLabel = "+100 BONUS",
        accentHex = "#00F5D4",
      ),
      ShardVaultSkuPack(
        productId = "shards_2000",
        title = "CONDUIT VAULT",
        shardAmount = 1650L,
        bonusShards = 350L,
        displayPriceUsd = "$12.99",
        badgeLabel = "POPULAR",
        accentHex = "#A855F7",
      ),
      ShardVaultSkuPack(
        productId = "shards_5000",
        title = "ARCHON TREASURY",
        shardAmount = 4000L,
        bonusShards = 1000L,
        displayPriceUsd = "$29.99",
        badgeLabel = "BEST VALUE",
        accentHex = "#FACC15",
      ),
    )

  fun findSkuByProductId(productId: String): ShardVaultSkuPack? =
    OFFICIAL_SKU_CATALOG.firstOrNull { it.productId.equals(productId.trim(), ignoreCase = true) }

  fun effectiveShardBalance(storedShards: Long): Long =
    if (TESTER_INFINITE_SHARDS_ACTIVE) {
      maxOf(storedShards, TESTER_INFINITE_SHARD_BALANCE)
    } else {
      storedShards.coerceAtLeast(0L)
    }

  fun canAffordShardSpend(currentShards: Long, cost: Long): Boolean {
    if (cost <= 0L) return true
    if (TESTER_INFINITE_SHARDS_ACTIVE) return true
    return currentShards >= cost
  }

  fun computeBalanceAfterSpend(currentShards: Long, cost: Long): Long {
    if (cost <= 0L) return effectiveShardBalance(currentShards)
    return if (TESTER_INFINITE_SHARDS_ACTIVE) {
      maxOf(currentShards, TESTER_INFINITE_SHARD_BALANCE)
    } else {
      (currentShards - cost).coerceAtLeast(0L)
    }
  }

  suspend fun launchShardPackPurchaseFlow(
    activity: Activity?,
    sku: ShardVaultSkuPack,
    currentUid: String = "",
    callsign: String = "",
  ): ShardPurchaseResult {
    if (!isLivePlayBillingEnabled) {
      return ShardPurchaseResult.TesterModeDormantNotice(
        productId = sku.productId,
        message =
          "TESTER TREASURY ACTIVE: INFINITE 999,999❖ ENABLED. " +
            "GOOGLE PLAY BILLING ($STUDIO_LEGAL_NAME) STANDING BY FOR COMMERCIAL RELEASE.",
      )
    }

    if (activity == null || activity.isFinishing) {
      return ShardPurchaseResult.BillingUnavailable(
        reason = "BILLING ACTIVITY CONTEXT UNAVAILABLE. RETURN TO NEXUS CONDUIT AND RETRY."
      )
    }

    val signedInUid = Firebase.auth.currentUser?.uid?.trim().orEmpty().ifEmpty { currentUid.trim() }
    if (signedInUid.isEmpty() || signedInUid.startsWith("dev_")) {
      return ShardPurchaseResult.BillingUnavailable(
        reason = "LINK GOOGLE PLAY / SWORN PILOT ACCOUNT IN HELM SYSTEMS BEFORE PURCHASING ❖."
      )
    }

    val billingClientAvailable =
      runCatching {
          Class.forName("com.android.billingclient.api.BillingClient")
          true
        }
        .getOrDefault(false)

    if (!billingClientAvailable) {
      return ShardPurchaseResult.BillingUnavailable(
        reason =
          "GOOGLE PLAY BILLING SDK NOT ATTACHED FOR SKU [${sku.productId.uppercase(Locale.US)}] ($callsign)."
      )
    }

    return ShardPurchaseResult.BillingUnavailable(
      reason =
        "GOOGLE PLAY CONSOLE PRODUCT [${sku.productId}] AWAITING LIVE MERCHANT ACTIVATION ($STUDIO_LEGAL_NAME)."
    )
  }

  fun resolveNamedFirestore(context: Context?): FirebaseFirestore? =
    runCatching {
        if (context != null) {
          val appCtx = context.applicationContext
          FirebaseFirestore.getInstance(appCtx.getString(R.string.firestore_database_id))
        } else {
          FirebaseFirestore.getInstance()
        }
      }
      .getOrNull()

  /**
   * Records a verified Google Play `purchaseToken` idempotently in Firestore (`pilot_shard_purchases`)
   * before crediting consumable Rift Shards so replay/duplicate tokens can never double-credit.
   */
  suspend fun recordVerifiedPurchaseTokenBestEffort(
    firestore: FirebaseFirestore?,
    uid: String,
    callsign: String,
    sku: ShardVaultSkuPack,
    purchaseToken: String,
    orderId: String = "",
  ): Boolean {
    val db = firestore ?: return false
    val cleanToken = purchaseToken.trim().replace(Regex("[^a-zA-Z0-9_\\-.]"), "").take(120)
    val cleanUid = uid.trim()
    if (cleanToken.isEmpty() || cleanUid.isEmpty() || cleanUid.startsWith("dev_")) {
      return false
    }
    return try {
      val docRef = db.collection("pilot_shard_purchases").document(cleanToken)
      val existing = docRef.get().await()
      if (existing.exists()) {
        false
      } else {
        val payload =
          mapOf(
            "userId" to cleanUid,
            "callsign" to UgcModeration.sanitizeIdentifier(callsign, "PILOT"),
            "productId" to sku.productId,
            "baseShards" to sku.shardAmount,
            "bonusShards" to sku.bonusShards,
            "shardsCredited" to sku.totalShards,
            "displayPriceUsd" to sku.displayPriceUsd,
            "orderId" to orderId.trim().take(64),
            "publisher" to STUDIO_LEGAL_NAME,
            "verifiedAt" to FieldValue.serverTimestamp(),
          )
        docRef.set(payload).await()
        true
      }
    } catch (ce: CancellationException) {
      throw ce
    } catch (_: Exception) {
      false
    }
  }
}

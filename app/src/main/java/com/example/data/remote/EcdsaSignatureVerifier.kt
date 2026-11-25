package com.example.data.remote

import com.example.data.model.PatchPackPayload
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

object EcdsaSignatureVerifier {
  fun sha256Hex(bytes: ByteArray): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
    return digest.joinToString("") { "%02x".format(it) }
  }

  /**
   * Verifies both SHA-256 hex digest and `SHA256withECDSA` (NIST P-256) signature over [packBytes].
   * Accepts either raw Base64 X.509 SubjectPublicKeyInfo or a PEM block.
   */
  fun verifyPack(
    packBytes: ByteArray,
    expectedSha256Hex: String,
    signatureBase64: String,
    publicKeyPemOrBase64: String,
  ): Result<Boolean> =
    runCatching {
      val actualSha = sha256Hex(packBytes)
      val expectedClean = expectedSha256Hex.trim().lowercase()
      if (expectedClean.length != 64 || actualSha != expectedClean) {
        error("SHA-256 mismatch (expected $expectedClean, actual $actualSha)")
      }

      val cleanKey =
        publicKeyPemOrBase64
          .replace("-----BEGIN PUBLIC KEY-----", "")
          .replace("-----END PUBLIC KEY-----", "")
          .replace(Regex("\\s+"), "")
      if (cleanKey.isBlank()) {
        error("ECDSA public key is not configured.")
      }

      val keyBytes = Base64.getDecoder().decode(cleanKey)
      val sigBytes = Base64.getDecoder().decode(signatureBase64.replace(Regex("\\s+"), ""))

      val pubKey = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(keyBytes))
      val verifier = Signature.getInstance("SHA256withECDSA")
      verifier.initVerify(pubKey)
      verifier.update(packBytes)
      val valid = verifier.verify(sigBytes)
      if (!valid) error("ECDSA digital signature verification failed.")
      true
    }
}

object PatchDiffBuilder {
  fun compute(
    currentPatchVersion: String,
    existingChampionIds: Set<String>,
    existingItemIds: Set<String>,
    existingRuneIds: Set<String>,
    existingMatchupIds: Set<String>,
    incoming: PatchPackPayload,
  ): PatchDiffSummary {
    val addedChamps = mutableListOf<String>()
    val updatedChamps = mutableListOf<String>()
    incoming.champions.forEach { c ->
      if (c.id in existingChampionIds) updatedChamps += c.nameEn else addedChamps += c.nameEn
    }

    val addedItems = mutableListOf<String>()
    val updatedItems = mutableListOf<String>()
    incoming.items.forEach { item ->
      if (item.id in existingItemIds) updatedItems += item.nameEn else addedItems += item.nameEn
    }

    val addedRunesCount = incoming.runes.count { it.id !in existingRuneIds }
    val addedMatchupsCount = incoming.matchupRules.count { it.id !in existingMatchupIds }

    return PatchDiffSummary(
      incomingPatchVersion = incoming.patchVersion,
      currentPatchVersion = currentPatchVersion,
      addedChampions = addedChamps,
      updatedChampions = updatedChamps,
      addedItems = addedItems,
      updatedItems = updatedItems,
      addedRunesCount = addedRunesCount,
      addedMatchupsCount = addedMatchupsCount,
      patchNotesCount = incoming.patchNotes.size,
    )
  }
}

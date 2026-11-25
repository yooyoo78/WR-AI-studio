# Security & Public Release Checklist

## 1. Firebase & `google-services.json` Hardening (Required Before Making Repo Public)
Because `app/google-services.json` is present in Git history and identifies your Firebase project (`applicationId`: `com.aistudio.wildriftcompanion.wrxkpz`):

1. **Restrict the Android API Key in Google Cloud Console**:
   - Open **Google Cloud Console -> APIs & Services -> Credentials**.
   - Select the Browser / Android API key listed in `google-services.json`.
   - Under **Application restrictions**, choose **Android apps** and add:
     - Package name: `com.aistudio.wildriftcompanion.wrxkpz`
     - SHA-1 signing certificate fingerprints (both upload/release keystore and debug keystore).
   - Under **API restrictions**, limit allowed APIs strictly to the Firebase / AI Logic services used by this project.
2. **Enforce Firebase App Check**:
   - Keep reCAPTCHA / Play Integrity enforcement active in the Firebase Console for release builds (`BuildConfig.DEBUG == false`).
   - Do not publish debug App Check tokens.
3. **If rotating the key**:
   - Generate a fresh restricted key in Google Cloud Console, delete the old unrestricted key, and distribute the updated `google-services.json` via the encrypted `GOOGLE_SERVICES_JSON_BASE64` GitHub Secret used by `.github/workflows/release-apk.yml`.

## 2. User BYOK External AI Keys
- User-provided keys (AI Studio, Groq, OpenRouter) are stored locally in `wr_ai_coach_ext_key_prefs.xml` encrypted with Android Keystore AES-256-GCM (`SecureApiKeyStore`).
- Cloud backup and device-to-device transfer are explicitly disabled for `wr_ai_coach_ext_key_prefs.xml` in `backup_rules.xml` and `data_extraction_rules.xml`.

## 3. Signed OTA Patch Pack Verification (ECDSA P-256)
- OTA Patch Packs released on GitHub Releases are verified with `SHA256withECDSA` (`EcdsaSignatureVerifier`) against the configured SubjectPublicKeyInfo (`X509`) Base64 public key and manifest SHA-256 before `PatchDataValidator` imports data.
- Never commit private ECDSA signing keys to the repository; store only the public key in Patch Center or repository docs, and sign patch pack files offline / in CI secrets before uploading to GitHub Releases.

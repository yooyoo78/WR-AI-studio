package com.example.data.ai

enum class AiProviderId(
  val labelEn: String,
  val labelTh: String,
  val shortLabel: String,
  val requiresApiKey: Boolean,
) {
  AUTO_POOL(
    labelEn = "Auto (Smart Free Pool)",
    labelTh = "อัตโนมัติ (สลับโมเดลฟรีอัจฉริยะ)",
    shortLabel = "Auto",
    requiresApiKey = false,
  ),
  FIREBASE_AI(
    labelEn = "Firebase AI (Built-in Free)",
    labelTh = "Firebase AI (ฟรีในตัว ไม่ต้องใส่ Key)",
    shortLabel = "Firebase",
    requiresApiKey = false,
  ),
  GROQ(
    labelEn = "Groq LPU (Free API Key)",
    labelTh = "Groq LPU (ใส่ Key ฟรี • เร็วมาก)",
    shortLabel = "Groq",
    requiresApiKey = true,
  ),
  OPENROUTER(
    labelEn = "OpenRouter (:free API Key)",
    labelTh = "OpenRouter (ใส่ Key ฟรี • รวมโมเดล :free)",
    shortLabel = "OpenRouter",
    requiresApiKey = true,
  ),
  GOOGLE_AI_STUDIO(
    labelEn = "Google AI Studio (Free Key)",
    labelTh = "Google AI Studio (ใส่ Key ฟรี)",
    shortLabel = "AI Studio",
    requiresApiKey = true,
  ),
}

data class AiModelSpec(
  val id: String,
  val provider: AiProviderId,
  val remoteModelId: String,
  val displayName: String,
  val shortBadge: String,
  val descTh: String,
  val descEn: String,
  val supportsText: Boolean = true,
  val supportsVision: Boolean = false,
)

data class ExternalApiKeyStatus(
  val hasGroqKey: Boolean = false,
  val hasOpenRouterKey: Boolean = false,
  val hasAiStudioKey: Boolean = false,
  val groqPreview: String = "",
  val openRouterPreview: String = "",
  val aiStudioPreview: String = "",
) {
  fun hasKeyFor(provider: AiProviderId): Boolean =
    when (provider) {
      AiProviderId.GROQ -> hasGroqKey
      AiProviderId.OPENROUTER -> hasOpenRouterKey
      AiProviderId.GOOGLE_AI_STUDIO -> hasAiStudioKey
      AiProviderId.AUTO_POOL, AiProviderId.FIREBASE_AI -> true
    }

  fun previewFor(provider: AiProviderId): String =
    when (provider) {
      AiProviderId.GROQ -> groqPreview
      AiProviderId.OPENROUTER -> openRouterPreview
      AiProviderId.GOOGLE_AI_STUDIO -> aiStudioPreview
      else -> ""
    }

  val configuredExternalCount: Int
    get() = listOf(hasGroqKey, hasOpenRouterKey, hasAiStudioKey).count { it }
}

object AiModelCatalog {
  const val AUTO_MODEL_ID = "auto"

  val AUTO_SPEC =
    AiModelSpec(
      id = AUTO_MODEL_ID,
      provider = AiProviderId.AUTO_POOL,
      remoteModelId = "auto",
      displayName = "Auto Smart Pool",
      shortBadge = "Auto",
      descTh = "เลือกโมเดลฟรีที่พร้อมใช้อัตโนมัติ และสลับหนี 429 ให้ทันที",
      descEn = "Automatically picks healthy free models and falls back on 429 limits.",
      supportsText = true,
      supportsVision = true,
    )

  val FIREBASE_FLASH =
    AiModelSpec(
      id = "firebase:gemini-3.8-flash",
      provider = AiProviderId.FIREBASE_AI,
      remoteModelId = "gemini-3.8-flash",
      displayName = "Gemini 3.8 Flash",
      shortBadge = "3.8 Flash",
      descTh = "ค่าเริ่มต้นจาก Firebase ฟรี แม่นยำทั้งข้อความและภาพ Live",
      descEn = "Default keyless Firebase model for balanced coaching and Live Vision.",
      supportsText = true,
      supportsVision = true,
    )

  val FIREBASE_FLASH_LITE =
    AiModelSpec(
      id = "firebase:gemini-3.5-flash-lite",
      provider = AiProviderId.FIREBASE_AI,
      remoteModelId = "gemini-3.5-flash-lite",
      displayName = "Gemini 3.5 Flash-Lite",
      shortBadge = "Flash-Lite",
      descTh = "รุ่นเบา ตอบไว ประหยัดโควตาช่วงคนใช้เยอะ",
      descEn = "Lightweight high-throughput Firebase model for peak hours.",
      supportsText = true,
      supportsVision = true,
    )

  val GROQ_LLAMA_70B =
    AiModelSpec(
      id = "groq:llama-3.3-70b-versatile",
      provider = AiProviderId.GROQ,
      remoteModelId = "llama-3.3-70b-versatile",
      displayName = "Llama 3.3 70B (Groq)",
      shortBadge = "Groq 70B",
      descTh = "วิเคราะห์แผนไทย/อังกฤษละเอียดและเร็วมากด้วย Groq LPU",
      descEn = "Strong tactical reasoning at ultra-fast Groq LPU speeds.",
      supportsText = true,
      supportsVision = false,
    )

  val GROQ_LLAMA_8B =
    AiModelSpec(
      id = "groq:llama-3.1-8b-instant",
      provider = AiProviderId.GROQ,
      remoteModelId = "llama-3.1-8b-instant",
      displayName = "Llama 3.1 8B Instant (Groq)",
      shortBadge = "Groq 8B",
      descTh = "เร็วสุดขีด โควตาต่อนาทีสูง เหมาะถามสั้น ๆ หน้าล็อบบี้",
      descEn = "Ultra-low-latency 8B model with generous free rate limits.",
      supportsText = true,
      supportsVision = false,
    )

  val GROQ_LLAMA4_SCOUT =
    AiModelSpec(
      id = "groq:meta-llama/llama-4-scout-17b-16e-instruct",
      provider = AiProviderId.GROQ,
      remoteModelId = "meta-llama/llama-4-scout-17b-16e-instruct",
      displayName = "Llama 4 Scout Vision (Groq)",
      shortBadge = "Groq Scout",
      descTh = "รองรับทั้งแชทโค้ชและสแกนภาพ Live Coach (Multimodal)",
      descEn = "Multimodal MoE model supporting both AI Coach chat and Live Vision.",
      supportsText = true,
      supportsVision = true,
    )

  val OPENROUTER_FREE_ROUTER =
    AiModelSpec(
      id = "openrouter:openrouter/free",
      provider = AiProviderId.OPENROUTER,
      remoteModelId = "openrouter/free",
      displayName = "OpenRouter Free Auto-Router",
      shortBadge = "OR Free",
      descTh = "สุ่มเลือกโมเดล :free ที่ว่างอยู่ในขณะนั้นให้อัตโนมัติ (แก้ทางล่มช่วงพีค)",
      descEn = "Routes automatically to an active $0 free model during peak load.",
      supportsText = true,
      supportsVision = false,
    )

  val OPENROUTER_GEMMA_31B =
    AiModelSpec(
      id = "openrouter:google/gemma-4-31b-it:free",
      provider = AiProviderId.OPENROUTER,
      remoteModelId = "google/gemma-4-31b-it:free",
      displayName = "Gemma 4 31B (:free)",
      shortBadge = "Gemma 31B",
      descTh = "โมเดล Gemma ฟรีบน OpenRouter ตอบเป็นระเบียบและอ่านง่าย",
      descEn = "Free-tier Google Gemma 4 31B hosted via OpenRouter.",
      supportsText = true,
      supportsVision = false,
    )

  val AI_STUDIO_FLASH =
    AiModelSpec(
      id = "aistudio:gemini-2.5-flash",
      provider = AiProviderId.GOOGLE_AI_STUDIO,
      remoteModelId = "gemini-2.5-flash",
      displayName = "Gemini 2.5 Flash (AI Studio)",
      shortBadge = "AIST 2.5",
      descTh = "แยกโควตาออกมาใช้ Key ฟรีจาก AI Studio เอง (รองรับ Live Vision)",
      descEn = "Dedicated free-tier quota via your own Google AI Studio key (Text + Vision).",
      supportsText = true,
      supportsVision = true,
    )

  val AI_STUDIO_FLASH_LITE =
    AiModelSpec(
      id = "aistudio:gemini-2.5-flash-lite",
      provider = AiProviderId.GOOGLE_AI_STUDIO,
      remoteModelId = "gemini-2.5-flash-lite",
      displayName = "Gemini 2.5 Flash-Lite (AI Studio)",
      shortBadge = "AIST Lite",
      descTh = "Key AI Studio รุ่นเบา โควตา RPM/RPD สูงทั้งข้อความและภาพ",
      descEn = "High-RPD free-tier Gemini Flash-Lite using your own AI Studio key.",
      supportsText = true,
      supportsVision = true,
    )

  val allOptions: List<AiModelSpec> =
    listOf(
      AUTO_SPEC,
      FIREBASE_FLASH,
      FIREBASE_FLASH_LITE,
      GROQ_LLAMA_70B,
      GROQ_LLAMA_8B,
      GROQ_LLAMA4_SCOUT,
      OPENROUTER_FREE_ROUTER,
      OPENROUTER_GEMMA_31B,
      AI_STUDIO_FLASH,
      AI_STUDIO_FLASH_LITE,
    )

  val externalProviders: List<AiProviderId> =
    listOf(AiProviderId.GROQ, AiProviderId.OPENROUTER, AiProviderId.GOOGLE_AI_STUDIO)

  fun findById(id: String?): AiModelSpec =
    allOptions.firstOrNull { it.id == id } ?: AUTO_SPEC

  fun concreteTextModels(): List<AiModelSpec> =
    allOptions.filter { it.id != AUTO_MODEL_ID && it.supportsText }

  fun concreteVisionModels(): List<AiModelSpec> =
    allOptions.filter { it.id != AUTO_MODEL_ID && it.supportsVision }

  fun isAvailable(spec: AiModelSpec, keyStatus: ExternalApiKeyStatus): Boolean =
    !spec.provider.requiresApiKey || keyStatus.hasKeyFor(spec.provider)
}

package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.AbilityInfo
import com.example.data.model.ChampionRole
import com.example.data.model.DamageType
import com.example.data.model.DataSourceType
import com.example.data.model.ItemCategory
import com.example.data.model.ItemCounterTag
import com.example.data.model.Lane
import com.example.data.model.LaneDifficulty
import com.example.data.model.PatchChangeEntry
import com.example.data.model.ThreatTag
import com.example.data.model.TierRank
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class Converters {
  private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

  private val stringListAdapter =
    moshi.adapter<List<String>>(Types.newParameterizedType(List::class.java, String::class.java))
  private val abilityListAdapter =
    moshi.adapter<List<AbilityInfo>>(
      Types.newParameterizedType(List::class.java, AbilityInfo::class.java)
    )
  private val changeListAdapter =
    moshi.adapter<List<PatchChangeEntry>>(
      Types.newParameterizedType(List::class.java, PatchChangeEntry::class.java)
    )

  @TypeConverter
  fun fromStringList(value: List<String>): String = stringListAdapter.toJson(value)

  @TypeConverter
  fun toStringList(value: String): List<String> = stringListAdapter.fromJson(value).orEmpty()

  @TypeConverter
  fun fromAbilityList(value: List<AbilityInfo>): String = abilityListAdapter.toJson(value)

  @TypeConverter
  fun toAbilityList(value: String): List<AbilityInfo> = abilityListAdapter.fromJson(value).orEmpty()

  @TypeConverter
  fun fromChangeList(value: List<PatchChangeEntry>): String = changeListAdapter.toJson(value)

  @TypeConverter
  fun toChangeList(value: String): List<PatchChangeEntry> =
    changeListAdapter.fromJson(value).orEmpty()

  @TypeConverter
  fun fromRoleList(value: List<ChampionRole>): String = value.joinToString(",") { it.name }

  @TypeConverter
  fun toRoleList(value: String): List<ChampionRole> =
    if (value.isBlank()) emptyList() else value.split(",").map { ChampionRole.valueOf(it) }

  @TypeConverter
  fun fromLaneList(value: List<Lane>): String = value.joinToString(",") { it.name }

  @TypeConverter
  fun toLaneList(value: String): List<Lane> =
    if (value.isBlank()) emptyList() else value.split(",").map { Lane.valueOf(it) }

  @TypeConverter
  fun fromThreatTagList(value: List<ThreatTag>): String = value.joinToString(",") { it.name }

  @TypeConverter
  fun toThreatTagList(value: String): List<ThreatTag> =
    if (value.isBlank()) emptyList() else value.split(",").map { ThreatTag.valueOf(it) }

  @TypeConverter
  fun fromCounterTagList(value: List<ItemCounterTag>): String = value.joinToString(",") { it.name }

  @TypeConverter
  fun toCounterTagList(value: String): List<ItemCounterTag> =
    if (value.isBlank()) emptyList() else value.split(",").map { ItemCounterTag.valueOf(it) }

  @TypeConverter fun fromDamageType(value: DamageType): String = value.name

  @TypeConverter fun toDamageType(value: String): DamageType = DamageType.valueOf(value)

  @TypeConverter fun fromTierRank(value: TierRank): String = value.name

  @TypeConverter fun toTierRank(value: String): TierRank = TierRank.valueOf(value)

  @TypeConverter fun fromItemCategory(value: ItemCategory): String = value.name

  @TypeConverter fun toItemCategory(value: String): ItemCategory = ItemCategory.valueOf(value)

  @TypeConverter fun fromDataSource(value: DataSourceType): String = value.name

  @TypeConverter fun toDataSource(value: String): DataSourceType = DataSourceType.valueOf(value)

  @TypeConverter fun fromLaneDifficulty(value: LaneDifficulty): String = value.name

  @TypeConverter
  fun toLaneDifficulty(value: String): LaneDifficulty = LaneDifficulty.valueOf(value)
}

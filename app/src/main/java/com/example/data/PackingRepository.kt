package com.example.data

import android.content.Context
import com.example.model.PackItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class PackingRepository(private val context: Context) {

  private val prefs = context.getSharedPreferences("packliste_sarn_prefs", Context.MODE_PRIVATE)
  private val httpClient = OkHttpClient.Builder()
    .connectTimeout(12, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .build()

  companion object {
    private const val KEY_SARN_LIST = "key_sarn_list_v2"
    private const val KEY_HOME_LIST = "key_home_list_v2"
    private const val KEY_SCRIPT_URL = "key_script_url"
    private const val KEY_RECENTS = "key_recents"
    private const val KEY_THEME_MODE = "key_theme_mode"
  }

  fun getThemeMode(): com.example.model.ThemeMode {
    val name = prefs.getString(KEY_THEME_MODE, com.example.model.ThemeMode.SYSTEM.name) ?: com.example.model.ThemeMode.SYSTEM.name
    return try {
      com.example.model.ThemeMode.valueOf(name)
    } catch (_: Exception) {
      com.example.model.ThemeMode.SYSTEM
    }
  }

  fun setThemeMode(mode: com.example.model.ThemeMode) {
    prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
  }

  fun getScriptUrl(): String {
    return prefs.getString(KEY_SCRIPT_URL, "") ?: ""
  }

  fun setScriptUrl(url: String) {
    prefs.edit().putString(KEY_SCRIPT_URL, url.trim()).apply()
  }

  fun loadLists(): Pair<List<PackItem>, List<PackItem>> {
    val sarnJson = prefs.getString(KEY_SARN_LIST, null)
    val homeJson = prefs.getString(KEY_HOME_LIST, null)

    val sarn = if (sarnJson != null) parseItems(sarnJson) else defaultSarnItems()
    val home = if (homeJson != null) parseItems(homeJson) else defaultHomeItems()

    return Pair(sarn, home)
  }

  fun saveLists(sarn: List<PackItem>, home: List<PackItem>) {
    prefs.edit()
      .putString(KEY_SARN_LIST, serializeItems(sarn))
      .putString(KEY_HOME_LIST, serializeItems(home))
      .apply()
  }

  fun loadRecents(): List<String> {
    val json = prefs.getString(KEY_RECENTS, null) ?: return listOf(
      "Pijama", "Frottetuch Hand", "Sonnencreme", "Spange Lionel", "Kuscheltierli", "Wanderschuhe"
    )
    return try {
      val array = JSONArray(json)
      val list = mutableListOf<String>()
      for (i in 0 until array.length()) {
        list.add(array.getString(i))
      }
      list
    } catch (_: Exception) {
      emptyList()
    }
  }

  fun saveRecents(recents: List<String>) {
    val array = JSONArray()
    recents.take(15).forEach { array.put(it) }
    prefs.edit().putString(KEY_RECENTS, array.toString()).apply()
  }

  suspend fun syncFromCloud(url: String): Pair<List<PackItem>, List<PackItem>> = withContext(Dispatchers.IO) {
    if (url.isBlank()) throw IllegalArgumentException("Keine Cloud-URL angegeben")

    val fullUrl = if (url.contains("?")) "$url&action=getPackliste" else "$url?action=getPackliste"
    val request = Request.Builder().url(fullUrl).get().build()

    httpClient.newCall(request).execute().use { response ->
      if (!response.isSuccessful) throw IllegalStateException("HTTP ${response.code}")
      val body = response.body?.string() ?: throw IllegalStateException("Empty body")
      val json = JSONObject(body)

      val sarnArray = json.optJSONArray("nach_sarn")
      val homeArray = json.optJSONArray("nach_hause")

      val sarn = mutableListOf<PackItem>()
      val home = mutableListOf<PackItem>()

      if (sarnArray != null) {
        for (i in 0 until sarnArray.length()) {
          val obj = sarnArray.getJSONObject(i)
          sarn.add(
            PackItem(
              id = obj.optString("id", "item_${System.currentTimeMillis()}_$i"),
              name = obj.optString("name", ""),
              personId = obj.optString("person", "haushalt").lowercase(),
              isPacked = obj.optBoolean("packed", false),
              qty = obj.optInt("qty", 1),
              category = obj.optString("category", "Allgemein")
            )
          )
        }
      }

      if (homeArray != null) {
        for (i in 0 until homeArray.length()) {
          val obj = homeArray.getJSONObject(i)
          home.add(
            PackItem(
              id = obj.optString("id", "item_${System.currentTimeMillis()}_$i"),
              name = obj.optString("name", ""),
              personId = obj.optString("person", "haushalt").lowercase(),
              isPacked = obj.optBoolean("packed", false),
              qty = obj.optInt("qty", 1),
              category = obj.optString("category", "Allgemein")
            )
          )
        }
      }

      Pair(sarn, home)
    }
  }

  suspend fun pushToCloud(url: String, sarn: List<PackItem>, home: List<PackItem>): Boolean = withContext(Dispatchers.IO) {
    if (url.isBlank()) return@withContext false
    try {
      val payload = JSONObject().apply {
        put("timestamp", System.currentTimeMillis())
        val dataObj = JSONObject().apply {
          val sarnArray = JSONArray()
          sarn.forEach {
            sarnArray.put(JSONObject().apply {
              put("id", it.id)
              put("name", it.name)
              put("person", it.personId)
              put("packed", it.isPacked)
              put("qty", it.qty)
              put("category", it.category)
            })
          }
          val homeArray = JSONArray()
          home.forEach {
            homeArray.put(JSONObject().apply {
              put("id", it.id)
              put("name", it.name)
              put("person", it.personId)
              put("packed", it.isPacked)
              put("qty", it.qty)
              put("category", it.category)
            })
          }
          put("nach_sarn", sarnArray)
          put("nach_hause", homeArray)
        }
        val activeItemsArray = JSONArray()
        sarn.forEach {
          activeItemsArray.put(JSONObject().apply {
            put("id", it.id)
            put("direction", "sarn")
            put("name", it.name)
            put("person", it.personId)
            put("packed", it.isPacked)
            put("qty", it.qty)
            put("category", it.category)
          })
        }
        home.forEach {
          activeItemsArray.put(JSONObject().apply {
            put("id", it.id)
            put("direction", "home")
            put("name", it.name)
            put("person", it.personId)
            put("packed", it.isPacked)
            put("qty", it.qty)
            put("category", it.category)
          })
        }
        put("active_items", activeItemsArray)
        put("nach_sarn", dataObj.getJSONArray("nach_sarn"))
        put("nach_hause", dataObj.getJSONArray("nach_hause"))
        put("payload", dataObj)
      }

      val body = payload.toString().toRequestBody("text/plain; charset=utf-8".toMediaType())
      val request = Request.Builder().url(url).post(body).build()

      httpClient.newCall(request).execute().use { response ->
        response.isSuccessful
      }
    } catch (_: Exception) {
      false
    }
  }

  private fun serializeItems(items: List<PackItem>): String {
    val array = JSONArray()
    for (item in items) {
      val obj = JSONObject().apply {
        put("id", item.id)
        put("name", item.name)
        put("personId", item.personId)
        put("isPacked", item.isPacked)
        put("qty", item.qty)
        put("icon", item.icon)
        put("category", item.category)
      }
      array.put(obj)
    }
    return array.toString()
  }

  private fun parseItems(json: String): List<PackItem> {
    val list = mutableListOf<PackItem>()
    try {
      val array = JSONArray(json)
      for (i in 0 until array.length()) {
        val obj = array.getJSONObject(i)
        list.add(
          PackItem(
            id = obj.getString("id"),
            name = obj.getString("name"),
            personId = obj.optString("personId", "haushalt"),
            isPacked = obj.optBoolean("isPacked", false),
            qty = obj.optInt("qty", 1),
            icon = obj.optString("icon", "📦"),
            category = obj.optString("category", "Allgemein")
          )
        )
      }
    } catch (_: Exception) {}
    return list
  }

  fun defaultSarnItems(): List<PackItem> {
    return listOf(
      PackItem("ns_1", "Frottetuch Hand", "diverse", false, 1, "🧖", "Textilien"),
      PackItem("ns_2", "Frottetuch Duschen", "diverse", false, 1, "🛁", "Textilien"),
      PackItem("ns_3", "Fixleintücher 90", "diverse", false, 2, "🛏️", "Textilien"),
      PackItem("ns_4", "Bettzeug Kids", "diverse", false, 1, "🧸", "Textilien"),
      PackItem("ns_5", "Pijama", "li", false, 1, "🌙", "Bekleidung"),
      PackItem("ns_6", "Pijama", "lu", false, 1, "🌙", "Bekleidung"),
      PackItem("ns_7", "Spange Lionel", "li", false, 1, "🦷", "Retour"),
      PackItem("ns_8", "Kuscheltierli", "lu", false, 1, "🧸", "Retour"),
      PackItem("ns_9", "Sonnencreme", "diverse", false, 1, "☀️", "Küche"),
      PackItem("ns_10", "Küche Lumpen", "diverse", true, 1, "🧽", "Küche")
    )
  }

  fun defaultHomeItems(): List<PackItem> {
    return listOf(
      PackItem("nh_1", "Kompost", "diverse", false, 1, "🌱", "Retour"),
      PackItem("nh_2", "Brille", "to", false, 1, "👓", "Retour"),
      PackItem("nh_3", "Spange Lionel", "li", false, 1, "🦷", "Retour"),
      PackItem("nh_4", "Kuscheltierli", "lu", false, 1, "🧸", "Retour"),
      PackItem("nh_5", "Medis", "diverse", false, 1, "💊", "Retour"),
      PackItem("nh_6", "Wanderschuhe", "diverse", false, 1, "🥾", "Retour"),
      PackItem("nh_7", "Windjacken", "diverse", false, 1, "🧥", "Retour")
    )
  }
}

package com.brokenstrawapps.battlebuddy.viewmodels

import android.app.Application
import android.util.Log
import android.util.TimingLogger
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.android.volley.*
import com.android.volley.toolbox.*
import com.brokenstrawapps.battlebuddy.models.*
import com.brokenstrawapps.battlebuddy.utils.Regions
import com.brokenstrawapps.battlebuddy.viewmodels.json.TelemetryInterface
import com.brokenstrawapps.battlebuddy.viewmodels.json.TelemetryInterfaceDeserializer
import com.brokenstrawapps.battlebuddy.viewmodels.json.TelemetryList
import com.brokenstrawapps.battlebuddy.viewmodels.models.MatchData
import com.brokenstrawapps.battlebuddy.viewmodels.models.MatchModel
import com.beust.klaxon.*
import com.github.kittinunf.fuel.httpGet
import com.google.gson.Gson
import org.jetbrains.anko.doAsync
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.GZIPInputStream
import kotlin.reflect.KClass
import com.google.gson.GsonBuilder
import timber.log.Timber


class MatchDetailViewModel : ViewModel() {
    val mMatchData = MutableLiveData<MatchModel>()


    fun getMatchData(application: Application, shardID: String, matchID: String, playerID: String) {
        val mVolleyQueue = Volley.newRequestQueue(application)
        val url = "https://api.pubg.com/shards/${Regions.getNewRegionID(shardID)}/matches/$matchID"

        val matchModel = MatchModel(currentPlayerID = playerID)

        val timings = TimingLogger("GETMATCH", "START")

        url.httpGet().header(mapOf("Accept" to "application/vnd.api+json")).responseString { request, response, result ->

            timings.addSplit("Match received")
            Timber.d("Got match response.")

            val obj = JSONObject(result.get())


            matchModel.attributes = Gson().fromJson(obj.getJSONObject("data")?.getJSONObject("attributes").toString(), MatchData::class.java)

            var assetURL = ""

            Timber.d("Starting included parsing.")

            for (i in 0 until obj.getJSONArray("included").length()) {
                val item = obj.getJSONArray("included").getJSONObject(i)
                when (item.getString("type")) {
                    "asset" -> {
                        assetURL = Gson().fromJson(item.toString(), Asset::class.java).attributes.URL
                    }
                    "participant" -> {
                        val participant = Gson().fromJson(item.toString(), MatchParticipant::class.java)
                        matchModel.participantList.add(participant)
                        matchModel.participantHash[participant.id] = participant

                        if (participant.attributes.stats.playerId == matchModel.currentPlayerID) {
                            matchModel.currentPlayer = participant
                            matchModel.currentPlayerMatchID = participant.id
                        }
                    }
                    "roster" -> {
                        val roster = Gson().fromJson(item.toString(), MatchRoster::class.java)
                        matchModel.rosterList.add(roster)
                    }
                }
            }

            for (roster in matchModel.rosterList) {
                val find = roster.relationships.participants.data.find { it.id == matchModel.currentPlayerMatchID }
                if (find != null) {
                    matchModel.currentPlayerRoster = roster
                }
            }

            matchModel.randomizeTeamColors()

            timings.addSplit("Match parsed getting telem.")

            Timber.d("Getting telemetry.")

            assetURL.httpGet().responseString { request, response, result ->
                timings.addSplit("Telem received")
                Timber.d("Got telemetry.")
                val builder = GsonBuilder()

                builder.registerTypeAdapter(TelemetryInterface::class.java, TelemetryInterfaceDeserializer())
                val gson = builder.create()

                val result2 = gson.fromJson(result.get(), TelemetryList::class.java)
                Timber.d("SIZE: ${result2.size}")

                matchModel.eventList = result2

                timings.addSplit("Telem parsed")
                mMatchData.postValue(matchModel)
                timings.dumpToLog()
            }
        }
    }

    private fun parseMatchData(json: JSONObject, matchModel: MatchModel, mVolleyQueue: RequestQueue) {
        matchModel.attributes = Gson().fromJson(json.getJSONObject("data")?.getJSONObject("attributes").toString(), MatchData::class.java)

        val klaxon = Klaxon()
        Log.d("KLAXONREADER", "START:")
        doAsync {
            var URl = ""
            JsonReader(StringReader(json.getJSONArray("included").toString())).use { reader ->
                val result = arrayListOf<Item>()
                reader.beginArray {
                    while (reader.hasNext()) {
                        val item = klaxon.parse<Item>(reader)
                        if (item is Asset) {
                            URl = item.attributes.URL
                        }

                        /*if (item is Participant) {
                            matchModel.participantList.add(item)
                            matchModel.participantHash[item.id] = item

                            if (item.attributes.stats.playerId == matchModel.currentPlayerID) {
                                matchModel.currentPlayer = item
                                matchModel.currentPlayerMatchID = item.id
                            }
                        }*/

                        if (item is Roster) {
                            //matchModel.rosterList.add(item)
                            /*if (item.relationships.participants.data.find { it.id == matchModel.currentPlayerMatchID } != null) {
                                matchModel.currentPlayerRoster = item as Roster
                            }*/
                        }

                        result.add(item ?: continue)
                    }
                }

                Log.d("KLAXONREADER", "RESULT: ${result.size}")

                URl.httpGet().responseString { request, response, result ->
                    val builder = GsonBuilder()

                    builder.registerTypeAdapter(TelemetryInterface::class.java, TelemetryInterfaceDeserializer())
                    val gson = builder.create()

                    val result2 = gson.fromJson(result.get(), TelemetryList::class.java)
                    Timber.d("SIZE: ${result2.size}")

                    matchModel.eventList = result2
                    mMatchData.postValue(matchModel)
                }

                //getTelemetryDataGZIP(mVolleyQueue, matchModel, URl)
            }

            for (roster in matchModel.rosterList) {
                val find = roster.relationships.participants.data.find { it.id == matchModel.currentPlayerMatchID }
                if (find != null) {
                    matchModel.currentPlayerRoster = roster
                }
            }

            matchModel.randomizeTeamColors()

        }
    }

    private fun getTelemetryDataGZIP(mVolleyQueue: RequestQueue, matchModel: MatchModel, assetURL: String) {
        val objectRequest = object : StringRequest(Request.Method.GET, assetURL, Response.Listener<String> {

            val builder = GsonBuilder()

            builder.registerTypeAdapter(TelemetryInterface::class.java, TelemetryInterfaceDeserializer())
            val gson = builder.create()

            val result2 = gson.fromJson(it, TelemetryList::class.java)

            Timber.d(result2.getKills().toString())
            Timber.d("DONE!")

            matchModel.eventList = result2
            mMatchData.postValue(matchModel)
            //parseTelemetryData(JSONArray(it), matchModel)
        }, Response.ErrorListener {

        }) {
            override fun parseNetworkResponse(response: NetworkResponse): Response<String> {


                val output = StringBuilder()
                try {
                    val gStream = GZIPInputStream(ByteArrayInputStream(response.data))
                    val reader = InputStreamReader(gStream)
                    val inp = BufferedReader(reader, 16384)

                    reader.forEachLine {
                        output.append(it).append("\n")
                    }

                    reader.close()
                    inp.close()
                    gStream.close()
                } catch (e: IOException) {
                    return Response.error(ParseError())
                }


                return Response.success(output.toString(), HttpHeaderParser.parseCacheHeaders(response))
            }

            override fun parseNetworkError(volleyError: VolleyError): VolleyError {
                if (volleyError.networkResponse == null || volleyError.networkResponse.statusCode == 404) {
                    matchModel.error = volleyError.message
                    mMatchData.value = matchModel
                }

                return super.parseNetworkError(volleyError)
            }

            override fun getHeaders(): Map<String, String> {
                val params = HashMap<String, String>()
                params["Accept-Encoding"] = "gzip"
                return params
            }

            override fun getBodyContentType(): String {
                return "application/json"
            }
        }

        mVolleyQueue.add(objectRequest)

    }

    private fun getTelemetryData(mVolleyQueue: RequestQueue, matchModel: MatchModel, assetURL: String) {
        val objectRequest = object : JsonArrayRequest(Request.Method.GET, assetURL, null, Response.Listener {
            val builder = GsonBuilder()

            builder.registerTypeAdapter(TelemetryInterface::class.java, TelemetryInterfaceDeserializer())
            val gson = builder.create()

            val result2 = gson.fromJson(it.toString(), TelemetryList::class.java)
            //parseTelemetryData(it, matchModel)
            Timber.d("DONE!")
            mMatchData.postValue(matchModel)
        }, Response.ErrorListener {

        }) {
            override fun parseNetworkResponse(response: NetworkResponse): Response<JSONArray> {
                try {
                    var cacheEntry: Cache.Entry? = HttpHeaderParser.parseCacheHeaders(response)
                    if (cacheEntry == null) {
                        cacheEntry = Cache.Entry()
                    }
                    val cacheHitButRefreshed = (73 * 60 * 1000).toLong() // in 3 minutes cache will be hit, but also refreshed on background
                    val cacheExpired = (72 * 60 * 60 * 1000).toLong() // in 24 hours this cache entry expires completely
                    val now = System.currentTimeMillis()
                    val softExpire = now + cacheHitButRefreshed
                    val ttl = now + cacheExpired
                    cacheEntry.data = response.data
                    cacheEntry.softTtl = softExpire
                    cacheEntry.ttl = ttl
                    var headerValue: String? = response.headers["Date"]
                    if (headerValue != null) {
                        cacheEntry.serverDate = HttpHeaderParser.parseDateAsEpoch(headerValue)
                    }
                    headerValue = response.headers["Last-Modified"]
                    if (headerValue != null) {
                        cacheEntry.lastModified = HttpHeaderParser.parseDateAsEpoch(headerValue)
                    }
                    cacheEntry.responseHeaders = response.headers

                    val jsonString = String(response.data) +
                            HttpHeaderParser.parseCharset(response.headers)

                    return Response.success(JSONArray(jsonString), cacheEntry)
                } catch (e: UnsupportedEncodingException) {
                    return Response.error(ParseError(e))
                } catch (e: JSONException) {
                    return Response.error(ParseError(e))
                }

            }

            override fun parseNetworkError(volleyError: VolleyError): VolleyError {
                if (volleyError.networkResponse == null || volleyError.networkResponse.statusCode == 404) {
                    matchModel.error = volleyError.message
                    mMatchData.value = matchModel
                }

                return super.parseNetworkError(volleyError)
            }

            override fun getHeaders(): Map<String, String> {
                val params = HashMap<String, String>()
                params["Accept"] = "application/vnd.api+json"
                return params
            }

            override fun getBodyContentType(): String {
                return "application/json"
            }
        }

        mVolleyQueue.add(objectRequest)
    }

    private fun parseTelemetryData(json: JSONArray, matchModel: MatchModel) {

        doAsync {
            for (i in 0 until json.length()) {
                var item = json.getJSONObject(i)

                when (item["_T"]) {
                    "LogPlayerKill" -> {
                        matchModel.killFeedList.add(Gson().fromJson(item.toString(), LogPlayerKill::class.java))
                    }
                    "LogItemPickup" -> {
                        matchModel.logItemPickup.add(Gson().fromJson(item.toString(), LogItemPickup::class.java))
                    }
                    "LogPlayerTakeDamage" -> {
                        var item2 = Gson().fromJson(item.toString(), LogPlayerTakeDamage::class.java)
                        if (item.isNull("attacker")) {
                            //Log.d("TELEMETRY", "ATTACK NULL")
                            item2.attacker = LogCharacter()
                        }
                        matchModel.logPlayerTakeDamage.add(item2)
                    }
                    "LogPlayerAttack" -> {
                        matchModel.logPlayerAttack.add(Gson().fromJson(item.toString(), LogPlayerAttack::class.java))
                    }
                    "LogMatchDefinition" -> {
                        matchModel.matchDefinition = Gson().fromJson(item.toString(), LogMatchDefinition::class.java)
                    }
                    "LogPlayerCreate" -> {
                        matchModel.logPlayerCreate.add(Gson().fromJson(item.toString(), LogPlayerCreate::class.java).character)
                    }
                    "LogCarePackageSpawn" -> {
                        //matchModel.carePackageList.add(Gson().fromJson(item.toString(), LogCarePackageSpawn::class.java))
                    }
                    "LogCarePackageLand" -> {
                        matchModel.carePackageList.add(Gson().fromJson(item.toString(), LogCarePackageLand::class.java).doElapsedTime(matchModel.attributes!!.createdAt))
                    }
                    "LogPlayerPosition" -> {
                        val logPosition = Gson().fromJson(item.toString(), LogPlayerPosition::class.java)
                        matchModel.logPlayerPositions.add(logPosition)
                    }
                    "LogGameStatePeriodic" -> {
                        val periodic = Gson().fromJson(item.toString(), LogGamestatePeriodic::class.java)
                        periodic.matchTime = matchModel.attributes!!.createdAt

                        //val gameState = periodic.gameState

                        //Do date.
                        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                        sdf.timeZone = TimeZone.getTimeZone("GMT")

                        val sdf2 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                        sdf2.timeZone = TimeZone.getTimeZone("GMT")

                        val matchStartDate = sdf.parse(matchModel.attributes?.createdAt)
                        val killTime = sdf2.parse(periodic._D)

                        val difference = killTime.time - matchStartDate.time

                        /*val circle = SafeZoneCircle (
                                position = gameState.poisonGasWarningPosition,
                                radius = gameState.poisonGasWarningRadius,
                                timeInMatch = difference / 1000
                        )

                        if (!matchModel.safeZoneList.contains(circle) && circle.position.isValidCirclePosition()) {
                            matchModel.safeZoneList.add(circle)
                            Log.d("CIRCLE", circle.toString())
                        }

                        if (gameState.redZonePosition.isValidCirclePosition()) {
                            val redZone = SafeZoneCircle(
                                    position = gameState.redZonePosition,
                                    radius = gameState.redZoneRadius,
                                    timeInMatch = difference / 1000
                            )

                            if (!matchModel.redZoneList.contains(redZone)) {
                                matchModel.redZoneList.add(redZone)
                            }
                        }*/

                        // periodic.gameState.elapsedTime = difference

                        matchModel.gameStates.add(periodic)
                    }
                }
            }

            matchModel.killFeedList.sortBy { it._D }


            mMatchData.postValue(matchModel)
        }
    }

    @TypeFor(field = "type", adapter = MatchTypeAdapter::class)
    open class Item(val type: String = "")

    data class Asset(val id: String, val attributes: AssetAttributes) : Item() {
        data class AssetAttributes(val URL: String)
    }

    data class Participant(val id: String, val attributes: MatchAttributes) : Item()

    data class Roster(val id: String, val attributes: RosterAttributes, val relationships: Relationships) : Item()

    class MatchTypeAdapter : TypeAdapter<Item> {
        override fun classFor(type: Any): KClass<out Item> = when (type as String) {
            "asset" -> Asset::class
            "participant" -> Participant::class
            "roster" -> Roster::class
            else -> throw IllegalArgumentException("Unknown type: $type")
        }
    }
}
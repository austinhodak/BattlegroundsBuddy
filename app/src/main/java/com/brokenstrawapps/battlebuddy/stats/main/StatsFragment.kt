package com.brokenstrawapps.battlebuddy.stats.main


import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.text.format.DateUtils
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.brokenstrawapps.battlebuddy.R
import com.brokenstrawapps.battlebuddy.models.PlayerListModel
import com.brokenstrawapps.battlebuddy.models.PlayerStats
import com.brokenstrawapps.battlebuddy.utils.Rank
import com.brokenstrawapps.battlebuddy.utils.Ranks.getRankIcon
import com.brokenstrawapps.battlebuddy.utils.Ranks.getRankMedal
import com.brokenstrawapps.battlebuddy.utils.Ranks.getRankRibbon
import com.brokenstrawapps.battlebuddy.viewmodels.PlayerStatsViewModel
import com.brokenstrawapps.battlebuddy.viewmodels.models.PlayerModel
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.android.synthetic.main.player_stats_main_new.*
import org.jetbrains.anko.support.v4.defaultSharedPreferences
import java.text.DecimalFormat
import kotlin.math.roundToInt

class MainStatsFragmentNew : Fragment() {

    private val viewModel: PlayerStatsViewModel by lazy {
        ViewModelProviders.of(requireActivity()).get(PlayerStatsViewModel::class.java)
    }

    var handler = Handler()

    private var mainView: View? = null

    val formatter = DecimalFormat("#,###")

    private var mDatabase: DatabaseReference? = null

    private var mFunctions: FirebaseFunctions? = null

    private var TAG = MainStatsFragmentNew::class.java.simpleName

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        mainView = inflater.inflate(R.layout.player_stats_main_new, container, false)
        return mainView
    }

    private var mSharedPreferences: SharedPreferences? = null

    private var playerModel: PlayerModel? = null
    private var isOverallStats = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mDatabase = FirebaseDatabase.getInstance().reference
        mFunctions = FirebaseFunctions.getInstance()
        mSharedPreferences = activity!!.getSharedPreferences("com.brokenstrawapps.battlebuddy", Context.MODE_PRIVATE)

        val player = arguments!!.getSerializable("selectedPlayer") as PlayerListModel
        isOverallStats = player.isOverallStatsSelected

        timeline_top_card.backgroundTintList = when {
            player.isLifetimeSelected -> ColorStateList.valueOf(resources.getColor(R.color.md_blue_grey_900))
            isOverallStats -> ColorStateList.valueOf(resources.getColor(R.color.md_blue_grey_900))
            else -> ColorStateList.valueOf(resources.getColor(R.color.md_grey_850))
        }

        viewModel.playerData.observe(this, Observer<PlayerModel> {
            playerModel = it
            handler.removeCallbacksAndMessages(null)
            if (player.isOverallStatsSelected) {
                updateStats(it.getOverallStats())
            } else {
                updateStats(it.getStatsByGamemode(player.selectedGamemode)!!)
            }


            if (it.lastUpdated != null && it.lastUpdated != 0.toLong()) {
                val relTime = DateUtils
                        .getRelativeTimeSpanString(it.lastUpdated!! * 1000L,
                                System.currentTimeMillis(),
                                DateUtils.SECOND_IN_MILLIS, DateUtils.FORMAT_ABBREV_ALL).toString().toUpperCase().replace(".", "")


                statsLastUpdatedTop?.text = "LAST UPDATED: ${relTime}"

                last_updated_tv?.text = relTime

                Log.d("STATS", "MINUTES SINCE LAST UPDATE ${it.getMinutesSinceLastUpdated()}")

                if (it.getMinutesSinceLastUpdated() >= 60) {
                    (last_updated_tv?.parent as ConstraintLayout).setBackgroundResource(R.drawable.top_right_corner_box_red)
                    //stats_corner_alert?.setOutdated()
                } else {
                    (last_updated_tv?.parent as ConstraintLayout).setBackgroundResource(R.drawable.top_right_corner_box)
                    //stats_corner_alert?.hide()
                }
            } else {
                statsLastUpdatedTop?.text = "LAST UPDATED: NEVER, PULL TO REFRESH"

                last_updated_tv?.text = "NEVER"
                (last_updated_tv?.parent as ConstraintLayout).setBackgroundResource(R.drawable.top_right_corner_box_red)
            }
        })

        statsRankPoints?.setFactory {
            TextView(ContextThemeWrapper(requireActivity(), R.style.PointsText), null, 0)
        }

        val inAnim = AnimationUtils.loadAnimation(requireContext(),
                android.R.anim.fade_in)
        val outAnim = AnimationUtils.loadAnimation(requireContext(),
                android.R.anim.fade_out)
        inAnim.duration = 200
        outAnim.duration = 200

        statsRankPoints?.inAnimation = inAnim
        statsRankPoints?.outAnimation = outAnim

        timeline_top_card?.setOnClickListener {
            if (top_div?.visibility == View.VISIBLE) {
                top_div?.visibility = View.GONE
                top_div2?.visibility = View.GONE

                statsTopExtras1?.visibility = View.GONE
                statsTopExtras2?.visibility = View.GONE

                statsTopDropDown?.setImageResource(R.drawable.ic_arrow_drop_down_24dp)
            } else {
                top_div?.visibility = View.VISIBLE
                top_div2?.visibility = View.VISIBLE

                statsTopExtras1?.visibility = View.VISIBLE
                statsTopExtras2?.visibility = View.VISIBLE

                statsTopDropDown?.setImageResource(R.drawable.ic_arrow_drop_up_24dp)
            }
        }

        /*if (!Premium.isAdFreeUser()) {
            val statsBanner = AdView(requireContext())
            statsBanner.adSize = com.google.android.gms.ads.AdSize.BANNER
            statsBanner.adUnitId = "ca-app-pub-2237535196399997/3286029778"
            statsBanner.loadAd(Ads.getAdBuilder())
            statsFragList?.addView(statsBanner)
        }*/
    }

    private fun updateStats(stats: PlayerStats) {
        statsKills.text = stats.kills.toString()

        if (isOverallStats) {
            rankRibbon?.setImageResource(getRankRibbon(playerModel!!.getHighestRankTitle()))
            rankMedal?.setImageResource(getRankMedal(playerModel!!.getHighestRankTitle()))

            //player1Icon?.setImageResource(getRankIcon(playerModel!!.getHighestRank()))

            if (playerModel!!.getHighestRank() != Rank.UNKNOWN)
                statsPlayerName?.text = playerModel!!.getHighestRank().title.toUpperCase() + " " + playerModel!!.getHighestRankLevel()
            else
                statsPlayerName?.text = "UNRANKED"

            statsRankPoints?.setCurrentText("POINTS: ${formatter.format(Math.floor(playerModel!!.getHighestRankPoints()).toInt())}")
        } else {
            rankRibbon?.setImageResource(getRankRibbon(stats.rankPointsTitle))
            rankMedal?.setImageResource(getRankMedal(stats.rankPointsTitle))

            //player1Icon?.setImageResource(getRankIcon(stats.getRank()))

            if (stats.getRank() != Rank.UNKNOWN)
                statsPlayerName?.text = stats.getRank().title.toUpperCase() + " " + stats.getRankLevel()
            else
                statsPlayerName?.text = "UNRANKED"

            statsRankPoints?.setCurrentText("POINTS: ${formatter.format(Math.floor(stats.rankPoints).toInt())}")
        }

        val runnableCode = object : Runnable {
            override fun run() {
                if (isOverallStats)
                statsRankPoints?.setText("BEST POINTS: ${formatter.format(Math.floor(playerModel!!.getHighestBestRankPoints()).toInt())}")
                else
                statsRankPoints?.setText("BEST POINTS: ${formatter.format(Math.floor(stats.bestRankPoint).toInt())}")

                val secondRun = Runnable {
                    if (isOverallStats)
                    statsRankPoints?.setText("POINTS: ${formatter.format(Math.floor(playerModel!!.getHighestRankPoints()).toInt())}")
                    else
                    statsRankPoints?.setText("POINTS: ${formatter.format(Math.floor(stats.rankPoints).toInt())}")
                }

                handler.postDelayed(secondRun, 5000)

                handler.postDelayed(this, 10000)
            }
        }

        handler.postDelayed(runnableCode, 5000)

        statsWins?.text = stats.wins.toString()
        statsTopWins?.text = (stats.top10s - stats.wins).toString()
        statsHeadshots?.text = stats.headshotKills.toString()
        if (stats.kills != 0 && stats.losses != 0) statsKD?.text = String.format("%.2f", stats.kills.toDouble() / stats.losses.toDouble()) else statsKD?.text = "0.00"
        statsGamesPlayed?.text = stats.roundsPlayed.toString()
        statsAssists?.text = stats.assists.toString()
        if (stats.wins != 0 && stats.roundsPlayed != 0) statsLosses?.text = "${String.format("%.2f", (stats.wins.toDouble() / stats.roundsPlayed.toDouble()) * 100)}%" else statsLosses?.text = "0%"
        statsdBNOs?.text = stats.dBNOs.toString()
        statsMostKills?.text = stats.roundMostKills.toString()
        if (stats.damageDealt != 0.0 && stats.roundsPlayed != 0) statsAvgDamage?.text = String.format("%.0f", Math.ceil(stats.damageDealt / stats.roundsPlayed.toDouble())) else  statsAvgDamage?.text = "0"
        if (stats.headshotKills != 0 && stats.kills != 0) statsHeadshotPct?.text = "${String.format("%.2f", (stats.headshotKills.toDouble() / stats.kills.toDouble()) * 100)}%" else statsHeadshotPct?.text = "0%"

        val unit = Unit.valueOf(defaultSharedPreferences.getString("distance_measure", "METRIC")!!)

        statsRideDist?.text = stats.getDistance(PlayerStats.Distance.RIDING, unit)
        statsSwimDist?.text = stats.getDistance(PlayerStats.Distance.SWIMMING, unit)
        statsWalkDist?.text = stats.getDistance(PlayerStats.Distance.WALKING, unit)

        statsLongestKill?.text = stats.getDistance(PlayerStats.Distance.LONGEST_KILL, unit)
        statsRoadKills?.text = stats.roadKills.toString()
        statsTeamKills?.text = stats.teamKills.toString()

        statsBoosts?.text = stats.boosts.toString()
        statsHeals?.text = stats.heals.toString()
        statsRevives?.text = stats.revives.toString()

        statsTotalDamageDealt?.text = stats.damageDealt.roundToInt().toString()
        statsVehicleDestroys?.text = stats.vehicleDestroys.toString()

        statsTotalTime?.text = stats.getTotalTimeSurvived()
        statsLongestTime?.text = stats.getLongTimeSurvived()
    }
}

enum class Unit {
    METRIC,
    IMPERIAL
}
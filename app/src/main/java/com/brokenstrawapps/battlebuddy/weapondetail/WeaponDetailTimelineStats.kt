package com.brokenstrawapps.battlebuddy.weapondetail

import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.brokenstrawapps.battlebuddy.R
import com.brokenstrawapps.battlebuddy.models.Weapon
import com.brokenstrawapps.battlebuddy.models.WeaponSound
import com.brokenstrawapps.battlebuddy.utils.Database
import com.brokenstrawapps.battlebuddy.viewmodels.WeaponDetailViewModel
import com.brokenstrawapps.battlebuddy.weapons.CompareWeaponPicker
import com.brokenstrawapps.battlebuddy.weapons.WeaponDamageChart
import com.bumptech.glide.Glide
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.storage.FirebaseStorage
import com.leinardi.android.speeddial.SpeedDialActionItem
import kotlinx.android.synthetic.main.dialog_player_list.*
import kotlinx.android.synthetic.main.fragment_weapon_timeline_stats.*
import net.idik.lib.slimadapter.SlimAdapter
import net.idik.lib.slimadapter.SlimInjector
import net.idik.lib.slimadapter.animators.FadeInAnimator
import org.jetbrains.anko.appcompat.v7.navigationIconResource
import org.jetbrains.anko.backgroundResource
import org.jetbrains.anko.configuration
import org.jetbrains.anko.support.v4.browse
import pl.hypeapp.materialtimelineview.MaterialTimelineView
import java.io.File
import java.io.IOException
import java.util.HashMap
import kotlin.Comparator
import kotlin.collections.ArrayList
import kotlin.math.roundToInt

class WeaponDetailTimelineStats : Fragment() {

    var weaponTimelineToolbar: Toolbar? = null

    var weapon: DataSnapshot? = null
    var attachmentList: MutableList<Attachment>? = null
    var soundList: MutableList<WeaponSound>? = null

    private val viewModel: WeaponDetailViewModel by lazy {
        ViewModelProviders.of(requireActivity()).get(WeaponDetailViewModel::class.java)
    }

    var topAlertInjector = SlimInjector<TopAlert> { data, injector ->
        val timeline = injector.findViewById<MaterialTimelineView>(R.id.timeline_small_view)
        timeline.position = -1
        injector.text(R.id.attachmentTitle, data.title)
        injector.text(R.id.alertSubtitle, data.subTitle)
    }

    lateinit var mAdapter: SlimAdapter

    var mData = ArrayList<Any>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        weaponTimelineToolbar = requireActivity().findViewById(R.id.mapToolbar)
        weaponTimelineToolbar?.menu?.clear()
        weaponTimelineToolbar?.inflateMenu(R.menu.weapo_home_new)

        return inflater.inflate(R.layout.fragment_weapon_timeline_stats, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        timelineStatsRV.layoutManager = LinearLayoutManager(requireContext())

        setupAdapter()

//        timelineStatsRV.addOnScrollListener(object : RecyclerView.OnScrollListener() {
//            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
//                if (dy > 15) {
//                    if (weaponFAB != null && weaponFAB.isOpen) {
//                        weaponFAB?.close(true)
//                    }
//                    weaponFAB?.hide()
//                }
//                else if (dy < -15)
//                    weaponFAB?.show()
//            }
//        })

//        weaponFAB.addActionItem(SpeedDialActionItem.Builder(R.id.fab_view_comments, R.drawable.ic_message_dark_24dp)
//                .setFabBackgroundColor(resources.getColor(R.color.md_grey_850))
//                .setLabel("View Comments")
//                .setLabelColor(resources.getColor(R.color.md_light_blue_A400))
//                .setLabelBackgroundColor(resources.getColor(R.color.md_grey_850))
//                .create())

        /*weaponFAB.addActionItem(SpeedDialActionItem.Builder(R.id.fab_compare, R.drawable.ic_compare_arrows_black_24dp)
                .setFabBackgroundColor(resources.getColor(R.color.md_grey_850))
                .setLabel("Compare Weapons")
                .setLabelColor(resources.getColor(R.color.md_orange_A400))
                .setLabelBackgroundColor(resources.getColor(R.color.md_grey_850))
                .create())*/

        /*weaponFAB.addActionItem(SpeedDialActionItem.Builder(R.id.fab_detailed, R.drawable.ic_compare_arrows_black_24dp)
                .setFabBackgroundColor(resources.getColor(R.color.md_grey_850))
                .setLabel("Detailed Stats")
                .setLabelColor(resources.getColor(R.color.md_red_A400))
                .setLabelBackgroundColor(resources.getColor(R.color.md_grey_850))
                .create())*/
    }

    private var weaponObserver: Observer<DataSnapshot> = Observer {
        weapon = it
        weaponLoaded(it)

        if (activity != null) {


            weaponTimelineToolbar?.title = requireActivity().intent.getStringExtra("weaponName")
            weaponTimelineToolbar?.setNavigationIcon(R.drawable.ic_arrow_back_24dp)
            if (requireActivity().configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES) {
                weaponTimelineToolbar?.setNavigationIcon(R.drawable.ic_arrow_back_24dp)
            } else {
                weaponTimelineToolbar?.setNavigationIcon(R.drawable.ic_arrow_back_black_24dp)
            }
            weaponTimelineToolbar?.setNavigationOnClickListener {
                requireActivity().onBackPressed()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.weaponData.observe(requireActivity(), weaponObserver)
    }

    override fun onPause() {
        super.onPause()
        viewModel.weaponData.removeObserver(weaponObserver)
    }

    private lateinit var weaponClass: String

    private fun weaponLoaded(it: DataSnapshot) {
        mData.clear()



        val weapon = it.getValue(Weapon::class.java)!!

        if (weapon.wiki.isEmpty()) {
            weaponTimelineToolbar?.menu?.findItem(R.id.wiki)?.isVisible = false
        }

        weaponClass = requireActivity().intent.getStringExtra("weaponClass")?.replace("_", " ")!!.toUpperCase()
        if (weaponClass.endsWith("S", true)) {
            weaponClass = weaponClass.substring(0, weaponClass.length - 1)
        }

        mData.add(TopSection(weapon.icon, weapon.weapon_name, weaponClass, weapon.ammo, weapon.speed, weapon.range, weapon.ammoPerMag, weapon.power, weapon))

        if (weapon.damageBody0 != "--" && weapon.damageHead0 != "--") {
            mData.add(LineSection("Damage Stats", "At 10 Meters"))

            mData.add(DamageItem(R.drawable.vest_white, "No Vest", damage = weapon.damageBody0))
            mData.add(DamageItem(R.drawable.helmet_white, "No Helmet", damage = weapon.damageHead0))
        }

        if (weapon.attachments.isNotEmpty()) {
            mData.add(LineSection("Attachments", "${weapon.attachments.size} Total"))
            mData.add(AttachmentRV(weapon))
        }

        mData.add(LineSection("Sounds"))
        mData.add(SoundRV(weapon, it))

        if (it.hasChild("incomplete")) {
            mData.add(0, TopAlert("Weapon Has Incomplete Data", "Some data is not available, it will be added when possible."))
        }

        mAdapter.updateData(mData)

        /*weaponFAB.setOnActionSelectedListener { item ->
            when (item.id) {
                R.id.fab_view_comments -> {
                    weaponFAB.close(true)
                    Navigation.findNavController(requireActivity(), R.id.weaponDetailNavHost).navigate(R.id.comments)
                }
                R.id.fab_detailed -> {
                    weaponFAB.close(true)
                    Navigation.findNavController(requireActivity(), R.id.weaponDetailNavHost).navigate(R.id.detailed)
                }
                R.id.fab_compare -> {
                    weaponFAB.close(true)
                    val intent = Intent(requireContext(), CompareWeaponPicker::class.java)
                    intent.putExtra("firstWeapon", it.ref.toString())
                    intent.putExtra("weapon_name", weapon.weapon_name)
                    startActivity(intent)
                }
            }
            true
        }*/

        weaponTimelineToolbar?.setOnMenuItemClickListener {
            if (it.itemId == R.id.wiki) {
                browse(weapon.wiki)
            }
            if (it.itemId == R.id.weaponFeedback) {
            }
            return@setOnMenuItemClickListener true
        }
    }

    fun setupAdapter() {
        mAdapter = SlimAdapter.create().attachTo(timelineStatsRV).register<TopSection>(R.layout.weapon_timeline_top) { data, injector ->
            Glide.with(this).load(FirebaseStorage.getInstance().getReferenceFromUrl(data.icon!!.replace("pubg-center", "battlegrounds-buddy-2fe99"))).into(injector.findViewById(R.id.player1Icon))
            injector.text(R.id.statsPlayerName, data.title)
            injector.text(R.id.timelineSubtitle, data.subtitle)
            injector.text(R.id.timelineAmmo, data.ammo)
            injector.text(R.id.timelineSpeed, data.speed)
            injector.text(R.id.timelineMag, data.mag)

            if (data.weapon.damageBody0.toIntOrNull() != null && data.weapon.TBS != "--") {
                var newTBS = 60.00 / data.weapon.TBS.removeSuffix("s").toDouble()
                var damagePerShot = data.weapon.damageBody0.toDouble()
                injector.text(R.id.timelinePower, ((damagePerShot * newTBS) / 60).roundToInt().toString())
            } else {
                injector.text(R.id.timelinePower, "--")
            }

            injector.text(R.id.timelineTBS, data.weapon.TBS)
            injector.text(R.id.timelinePickupDelay, data.weapon.pickupDelay)
            injector.text(R.id.timelineReadyDelay, data.weapon.readyDelay)
            injector.text(R.id.timelineFiringModes, data.weapon.firingModes.toUpperCase())
            injector.text(R.id.timelineReloadTac, data.weapon.reloadDurationTac)
            injector.text(R.id.timelineReloadFull, data.weapon.reloadDurationFull)

            if (data.range.isNotEmpty() && data.range != "--") {
                val range = data.range
                val split = range.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                injector.text(R.id.timelineRange, split[1] + "M")
            } else {
                injector.text(R.id.timelineRange, "--")
            }

            val timelineView = injector.findViewById<MaterialTimelineView>(R.id.timeline_top_card)

            injector.clicked(R.id.timeline_top_card) {
                val div = injector.findViewById<View>(R.id.top_div)
                if (div.visibility == View.VISIBLE) {
                    div.visibility = View.GONE
                    injector.gone(R.id.top_div2)

                    injector.findViewById<LinearLayout>(R.id.top_extras2).visibility = View.GONE
                    injector.findViewById<LinearLayout>(R.id.statsTopExtras1).visibility = View.GONE

                    injector.image(R.id.timelineTopDropdown, R.drawable.ic_arrow_drop_down_24dp)
                } else {
                    div.visibility = View.VISIBLE
                    injector.visible(R.id.top_div2)

                    injector.findViewById<LinearLayout>(R.id.top_extras2).visibility = View.VISIBLE
                    injector.findViewById<LinearLayout>(R.id.statsTopExtras1).visibility = View.VISIBLE

                    injector.image(R.id.timelineTopDropdown, R.drawable.ic_arrow_drop_up_24dp)
                }
            }

        }.register<LineSection>(R.layout.weapon_timeline_line) { data, injector ->
            injector.text(R.id.line_title, data.title)
            injector.text(R.id.line_subtitle, data.subTitle)

            if (data.subTitle.isEmpty()) {
                injector.gone(R.id.line_subtitle)
            } else {
                injector.visible(R.id.line_subtitle)
            }
        }.register<DamageItem>(R.layout.timeline_small_top) { data, injector ->
            val index = mData.indexOf(data)
            val timeline = injector.findViewById<MaterialTimelineView>(R.id.timeline_small_view)
            val tintColor: Int?
            val damageD = Math.ceil((100 / data.damage.toDouble()))
            Log.d("TIMELINE", "$damageD")
            tintColor = when {
                damageD >= 5 -> R.color.timelineLightBlue
                damageD >= 4 -> R.color.timelineGreen
                damageD >= 3 -> R.color.timelineYellow
                damageD >= 2 -> R.color.timelineOrange
                damageD >= 1 -> R.color.timelineRed
                else -> R.color.timelineRed
            }

            injector.text(R.id.damageSubtitle, "${Math.ceil(damageD).toInt()} Hits to Kill")

            if (data.icon != null) {
                injector.image(R.id.attachmentIcon, data.icon!!)
            }

            if (mData[index - 1] is LineSection) {
                //First one, do nothing
                timeline.backgroundResource = R.drawable.timeline_round_top_only
                timeline.position = MaterialTimelineView.POSITION_LAST
                injector.gone(R.id.div2)
            } else {
                //Last one
                timeline.backgroundResource = R.drawable.timeline_round_bottom_only
                timeline.position = MaterialTimelineView.POSITION_FIRST
                injector.gone(R.id.div)
            }

            timeline.backgroundTintList = ColorStateList.valueOf(resources.getColor(tintColor))

            injector.text(R.id.attachmentTitle, data.title)
            injector.text(R.id.damageVal, data.damage)

            val icon = injector.findViewById<ImageView>(R.id.attachmentIcon)

            val intent = Intent(requireActivity(),
                    WeaponDamageChart::class.java)
            intent.putExtra("weaponPath", weapon!!.ref.path.toString())
            intent.putExtra("weaponKey",
                    weapon!!.key)
            intent.putExtra("weaponClass", weaponClass)
            intent.putExtra("weaponName", weapon!!.child("weapon_name").value.toString())

            intent.putExtra("damageBody", weapon!!.child("damageBody0").value.toString().toDouble())
            intent.putExtra("damageHead", weapon!!.child("damageHead0").value.toString().toDouble())

//            timeline.setOnClickListener {
//                startActivity(intent)
//            }
        }.register<AttachmentRV>(R.layout.timeline_attachment_list) { data, injector ->
            val attachmentRV = injector.findViewById<RecyclerView>(R.id.timelineAttachmentRV)
            attachmentRV.layoutManager = LinearLayoutManager(requireContext())
            attachmentRV.itemAnimator = FadeInAnimator()

            if (attachmentList == null) {
                attachmentList = ArrayList()
            }

            val adapter = SlimAdapter.create().register<Attachment>(R.layout.timeline_attachment) { data, injector ->
                val timeline = injector.findViewById<MaterialTimelineView>(R.id.timeline_small_view)
                timeline.position = -1

                injector.text(R.id.attachmentTitle, data.name)
                injector.text(R.id.attachmentSubtitle, data.location)

                Glide.with(this).load(FirebaseStorage.getInstance().getReferenceFromUrl(data.icon.replace("pubg-center", "battlegrounds-buddy-2fe99"))).into(injector.findViewById(R.id.attachmentIcon))

            }.attachTo(attachmentRV).updateData(attachmentList)

            if (!attachmentList!!.isEmpty()) {
                return@register
            }

            for (i in data.weapon.attachments) {
                Database.getNormalRef().child(i).addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onCancelled(p0: DatabaseError) {

                    }

                    override fun onDataChange(p0: DataSnapshot) {
                        val attachment = p0.getValue(Attachment::class.java)!!
                        attachmentList?.add(attachment)

                        attachmentList!!.sortBy { it.name }

                        adapter.updateData(attachmentList)
                    }

                })
            }
        }.register<SoundRV>(R.layout.timeline_sound_list) { data, injector ->
            val rv = injector.findViewById<RecyclerView>(R.id.timelineAttachmentRV)
            rv.layoutManager = LinearLayoutManager(requireContext())
            rv.itemAnimator = FadeInAnimator()

            val timeline = injector.findViewById<MaterialTimelineView>(R.id.timeline_small_view)
            timeline.position = MaterialTimelineView.POSITION_LAST
            timeline.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.timelineBlue))

            if (soundList == null) {
                soundList = ArrayList()
            }

            val mSoundsAdapter = SlimAdapter.create().attachTo(rv).updateData(soundList).register(R.layout.weapon_audio_list_item, SlimInjector<WeaponSound> { (value, title, url), injector ->
                injector.text(R.id.weaponAudioText, title)

                val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(url.replace("pubg-center", "battlegrounds-buddy-2fe99"))

                val isPlaying = booleanArrayOf(false)
                val isLoaded = booleanArrayOf(false)

                val mediaPlayer = MediaPlayer()
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)

                var localFile: File? = null
                try {
                    localFile = File.createTempFile(value, "ogg")
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                val finalLocalFile = localFile
                injector.clicked(R.id.weaponAudioPlay, object : View.OnClickListener {
                    override fun onClick(view: View) {
                        if (!isPlaying[0]) {
                            isPlaying[0] = true
                            injector.image(R.id.weaponAudioPlay, R.drawable.ic_pause_circle_filled_white_24dp)
                            try {
                                startPlaying()
                            } catch (e: IllegalStateException) {
                                e.printStackTrace()
                            }

                        } else {
                            mediaPlayer.stop()
                            isPlaying[0] = false
                            injector.image(R.id.weaponAudioPlay, R.drawable.ic_play_circle_filled_white_24dp)
                        }
                    }

                    private fun startPlaying() {
                        injector.visible(R.id.audioPg)
                        if (finalLocalFile != null && !isLoaded[0]) {
                            storageReference.getFile(finalLocalFile).addOnSuccessListener {
                                isLoaded[0] = true
                                injector.gone(R.id.audioPg)
                                try {
                                    mediaPlayer.reset()
                                    mediaPlayer.setDataSource(requireActivity(), Uri.parse(finalLocalFile.absolutePath))
                                    mediaPlayer.prepare()
                                    mediaPlayer.start()
                                } catch (e: IOException) {
                                    e.printStackTrace()
                                } catch (e: IllegalStateException) {
                                    e.printStackTrace()
                                }
                            }
                        } else {
                            injector.gone(R.id.audioPg)
                            try {
                                mediaPlayer.reset()
                                mediaPlayer.setDataSource(requireActivity(), Uri.parse(finalLocalFile!!.absolutePath))
                                mediaPlayer.prepare()
                                mediaPlayer.start()
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }

                        }
                    }
                })

                mediaPlayer.setOnCompletionListener { mediaPlayer ->
                    mediaPlayer.stop()
                    if (isPlaying[0]) {
                        isPlaying[0] = false
                        injector.image(R.id.weaponAudioPlay, R.drawable.ic_play_circle_filled_white_24dp)
                    }
                }
            })

            if (!soundList!!.isEmpty()) {
                return@register
            }

            if (data.snapshot.hasChild("audio")) {
                val audioObject = data.snapshot.child("audio").value as HashMap<String, String>?
                Log.d("WEAPON", audioObject!!.toString())

                for (s in audioObject.keys) {
                    if (audioObject[s]!!.isEmpty()) {
                        continue
                    }
                    when (s) {
                        "normal-single" -> soundList!!.add(WeaponSound(s, "Normal Single", audioObject[s]!!))
                        "normal-burst" -> soundList!!.add(WeaponSound(s, "Normal Burst", audioObject[s]!!))
                        "normal-auto" -> soundList!!.add(WeaponSound(s, "Normal Auto", audioObject[s]!!))
                        "suppressed-single" -> soundList!!.add(WeaponSound(s, "Suppressed Single", audioObject[s]!!))
                        "suppressed-auto" -> soundList!!.add(WeaponSound(s, "Suppressed Auto", audioObject[s]!!))
                        "suppressed-burst" -> soundList!!.add(WeaponSound(s, "Suppressed Burst", audioObject[s]!!))
                        "reloading" -> soundList!!.add(WeaponSound(s, "Reloading", audioObject[s]!!))
                    }
                }

                soundList!!.sortWith(Comparator { o, t1 ->
                    if (o is WeaponSound && t1 is WeaponSound) {
                        o.title.compareTo(t1.title, ignoreCase = true)
                    } else {
                        0
                    }
                })

                mSoundsAdapter.updateData(soundList)
            }

        }.register(R.layout.timeline_alert_top, topAlertInjector)
    }

    data class LineSection(
            var title: String = "",
            var subTitle: String = ""
    )

    data class TopSection(
            var icon: String?,
            var title: String = "--",
            val subtitle: String = "--",
            val ammo: String = "--",
            val speed: String = "--",
            val range: String = "--",
            val mag: String = "--",
            val power: String = "--",
            val weapon: Weapon
    )

    data class DamageItem(
            var icon: Int?,
            var title: String = "",
            var damage: String = ""
    )

    data class AttachmentRV(
            val weapon: Weapon
    )

    data class SoundRV(
            val weapon: Weapon,
            val snapshot: DataSnapshot
    )

    @IgnoreExtraProperties
    data class Attachment(
            val icon: String = "",
            val location: String = "",
            val name: String = "",
            val stats: String = "",
            val weapons: String = ""
    )

    data class TopAlert(
            val title: String,
            val subTitle: String
    )

    data class DetailedPUBGStats(
            val playerID: String
    )
}
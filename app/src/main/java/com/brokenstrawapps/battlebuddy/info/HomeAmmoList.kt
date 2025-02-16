package com.brokenstrawapps.battlebuddy.info


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.brokenstrawapps.battlebuddy.R
import com.brokenstrawapps.battlebuddy.R.layout
import com.brokenstrawapps.battlebuddy.utils.Database
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.home_weapons_list.*
import net.idik.lib.slimadapter.SlimAdapter
import net.idik.lib.slimadapter.SlimInjector
import java.util.*

class HomeAmmoList : Fragment() {

    internal var data: MutableList<Any> = ArrayList()

    private var slimAdapter: SlimAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(layout.home_weapons_list, container, false)
    }

    override fun onStart() {
        super.onStart()
        pg?.visibility = View.VISIBLE
        setupAdapter()
    }

    private fun loadWeapons() {
        Database.getNormalRef().child("info/ammo").orderByChild("name").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(p0: DataSnapshot) {
                data.clear()
                try {
                    for (document in p0.children) {
                        data.add(document)

                        pg?.visibility = View.GONE
                    }
                } catch (e1: Exception) {
                    e1.printStackTrace()
                }

                slimAdapter?.updateData(data)
            }

        })
    }

    private fun setupAdapter() {
        weapon_list_rv.layoutManager = LinearLayoutManager(activity ?: return)
        slimAdapter = SlimAdapter.create()
                .register(layout.attachment_item,
                        SlimInjector<DataSnapshot> { data, injector ->
                            val subtitle = injector
                                    .findViewById(R.id.weaponItemSubtitle) as TextView

                            val icon = injector
                                    .findViewById(R.id.helmetItem64) as ImageView

                            injector.text(R.id.weaponItemName,
                                    data.child("name").value.toString())

                            subtitle.maxLines = 10

                            if (data.hasChild("icon")) {
                                try {
                                    val gsReference = Database.getStorage()
                                            .getReferenceFromUrl(data.child("icon").value.toString().replace("pubg-center", "battlegrounds-buddy-2fe99"))

                                    val factory = DrawableCrossFadeFactory.Builder().setCrossFadeEnabled(true).build()

                                    Glide.with(this)
                                            .load(gsReference)
                                            .transition(withCrossFade(factory))
                                            .into(icon)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }

                            }

                            injector.text(R.id.weaponItemSubtitle, data.child("weapons").value.toString())
                        }).updateData(data).attachTo(weapon_list_rv)

        loadWeapons()
    }
}

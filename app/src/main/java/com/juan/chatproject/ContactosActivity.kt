package com.juan.chatproject

import android.support.design.widget.TabLayout
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup

import kotlinx.android.synthetic.main.activity_contactos.*
import kotlinx.android.synthetic.main.fragment_contactos.view.*

class ContactosActivity : AppCompatActivity() {

    companion object access {
        val TIPO = "TIPO"
        val TIPO_PENDIENTES = "PENDIENTES"
        val TIPO_ENVIADAS = "ENVIADAS"
    }


    var fragments = emptyArray<Fragment>()

    private var mSectionsPagerAdapter: SectionsPagerAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contactos)

        setSupportActionBar(toolbar)
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)

        // Set up the ViewPager with the sections adapter.
        container.adapter = mSectionsPagerAdapter

        container.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabs))
        tabs.addOnTabSelectedListener(TabLayout.ViewPagerOnTabSelectedListener(container))

        val fragmentPendientes = ContactosFragment()
        val bundlePendientes = Bundle()
        bundlePendientes.putString(TIPO, TIPO_PENDIENTES)
        fragmentPendientes.arguments = bundlePendientes

        val fragmentEnviadas = ContactosFragment()
        val bundleEnviadas = Bundle()
        bundleEnviadas.putString(TIPO, TIPO_ENVIADAS)
        fragmentEnviadas.arguments = bundleEnviadas

        fragments = arrayOf(fragmentPendientes, fragmentEnviadas)
    }


    inner class SectionsPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {
            return fragments[position]
        }

        override fun getCount(): Int {
            return 2
        }
    }
}

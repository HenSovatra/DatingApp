// fragments/UserInteractionsContainerFragment.kt

package com.example.datingapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.datingapp.R
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class UserInteractionsContainerFragment : Fragment() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var backButton: View

    private val mutualFragment = UserInteractionListFragment.newInstance(UserInteractionListFragment.TYPE_MUTUAL)
    private val outgoingFragment = UserInteractionListFragment.newInstance(UserInteractionListFragment.TYPE_OUTGOING)
    private val incomingFragment = UserInteractionListFragment.newInstance(UserInteractionListFragment.TYPE_INCOMING)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_user_interactions_container, container, false)
        tabLayout = view.findViewById(R.id.tabLayout)
        viewPager = view.findViewById(R.id.viewPager)
        backButton = view.findViewById(R.id.backBtn)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        backButton.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
                .replace(R.id.fragment_container, setting_fragment())
                .addToBackStack(null)
                .commit()
        }

        val pagerAdapter = PagerAdapter(this)
        viewPager.adapter = pagerAdapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Matches"
                1 -> "Liked"
                2 -> "Like Received"
                else -> ""
            }
        }.attach()
    }

    fun refreshAllLists() {
        mutualFragment.fetchUserInteractions()
        outgoingFragment.fetchUserInteractions()
        incomingFragment.fetchUserInteractions()
    }

    private inner class PagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> mutualFragment
                1 -> outgoingFragment
                2 -> incomingFragment
                else -> throw IllegalStateException("Invalid position: $position")
            }
        }
    }
}
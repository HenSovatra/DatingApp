package com.example.datingapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.datingapp.R
import com.example.datingapp.adapters.DateTypesAdapter
import com.example.datingapp.models.DateTypeItem

class date_fragment : Fragment() {

    private lateinit var dateTypesRecyclerView: RecyclerView
    private lateinit var btnViewSentRequests: Button
    private val dateTypesList = listOf(
        DateTypeItem(
            id = 1,
            name = "Travel",
            drawableResId = R.drawable.travel_date
        ),
        DateTypeItem(
            id = 2,
            name = "Coffee",
            drawableResId = R.drawable.coffee_date
        ),
        DateTypeItem(
            id = 3,
            name = "Movie",
            drawableResId = R.drawable.movie_date
        ),
        DateTypeItem(
            id = 4,
            name = "Late Night",
            drawableResId = R.drawable.latenight_date
        )
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_date_fragment, container, false)

        dateTypesRecyclerView = view.findViewById(R.id.dateTypesRecyclerView)
        btnViewSentRequests = view.findViewById(R.id.btnViewSentRequests)
        setupButtonListener()
        return view
    }
    private fun setupButtonListener() {
        btnViewSentRequests.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, SentDateRequestsFragment())
                .addToBackStack(null)
                .commit()
        }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dateTypesRecyclerView.layoutManager = GridLayoutManager(context, 2)
        val adapter = DateTypesAdapter(dateTypesList) { clickedDateType ->
            val nextFragment = create_date_fragment.newInstance(clickedDateType)

            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left
                )
                .replace(
                    R.id.fragment_container,
                    nextFragment
                )
                .addToBackStack(null)
                .commit()
        }

        dateTypesRecyclerView.adapter = adapter
    }
}
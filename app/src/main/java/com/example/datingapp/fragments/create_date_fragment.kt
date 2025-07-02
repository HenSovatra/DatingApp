package com.example.datingapp.fragments

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.datingapp.R
import com.example.datingapp.adapters.PartnersAdapter
import com.example.datingapp.api.RetrofitClient
import com.example.datingapp.models.DateRequest
import com.example.datingapp.models.DateTypeItem
import com.example.datingapp.models.UserProfileResponse
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.yourpackage.yourapp.auth.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class create_date_fragment : Fragment(), OnMapReadyCallback {
    companion object {
        private const val ARG_DATE_TYPE_ITEM = "date_type_item"

        fun newInstance(dateTypeItem: DateTypeItem): create_date_fragment {
            val fragment = create_date_fragment()
            val args = Bundle().apply {
                putSerializable(ARG_DATE_TYPE_ITEM, dateTypeItem)
            }
            fragment.arguments = args
            return fragment
        }
    }
    private var selectedPartner: UserProfileResponse? = null
    private lateinit var typeTitle: TextView
    private lateinit var backArrow : ImageView
    private var selectedDateType: DateTypeItem? = null
    private lateinit var partnersRecyclerView: RecyclerView
    private lateinit var btnFindPartner: TextView
    private lateinit var textSelectedPartner: TextView
    private lateinit var btnSend: TextView
    private lateinit var messageEditText: EditText

    private lateinit var btnSelectLocation: TextView
    private lateinit var btnSelectDateTime: TextView

    private var selectedLocation: String? = null
    private var selectedDateTime: String? = null
    private var selectedCalendar: Calendar = Calendar.getInstance()

    private val likedPartnersList: MutableList<UserProfileResponse> = mutableListOf()
    private lateinit var partnersAdapter: PartnersAdapter

    private lateinit var mapSelectionLayout: ConstraintLayout
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mapSelectedAddressTextView: TextView
    private lateinit var btnConfirmMapLocation: Button
    private var geocoder: Geocoder? = null

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
                getMapAndInit()
            } else {
                Toast.makeText(requireContext(), "Location permission denied. Cannot automatically show your current location.", Toast.LENGTH_LONG).show()
                loadMapWithDefaultLocation()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            selectedDateType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getSerializable(ARG_DATE_TYPE_ITEM, DateTypeItem::class.java)
            } else {
                @Suppress("DEPRECATION")
                it.getSerializable(ARG_DATE_TYPE_ITEM) as? DateTypeItem
            }
        }
        geocoder = Geocoder(requireContext(), Locale.getDefault())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_create_date_fragment, container, false)
        partnersRecyclerView = view.findViewById(R.id.partnersRecyclerView)
        typeTitle = view.findViewById(R.id.selectedType)
        backArrow = view.findViewById(R.id.backArrow)
        btnFindPartner = view.findViewById(R.id.btnFindPartner)
        textSelectedPartner = view.findViewById(R.id.selectedPartnerName)
        btnSend = view.findViewById(R.id.btnSend)
        messageEditText = view.findViewById(R.id.messageEditText)

        btnSelectLocation = view.findViewById(R.id.btnSelectLocation)
        btnSelectDateTime = view.findViewById(R.id.btnSelectDateTime)
        mapSelectionLayout = view.findViewById(R.id.mapSelectionLayout)
        mapSelectedAddressTextView = view.findViewById(R.id.mapSelectedAddressTextView)
        btnConfirmMapLocation = view.findViewById(R.id.btnConfirmMapLocation)
        val mapToolbar = view.findViewById<Toolbar>(R.id.mapToolbar)
        mapToolbar.setNavigationOnClickListener {
            hideMapSelectionLayout()
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        partnersRecyclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        partnersAdapter = PartnersAdapter(likedPartnersList) { clickedPartner ->
            handlePartnerClick(clickedPartner)
        }
        partnersRecyclerView.adapter = partnersAdapter
        typeTitle.text = selectedDateType?.name + " Date"

        backArrow.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnFindPartner.setOnClickListener {
            val nextFragment = home_fragment()
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
                .replace(R.id.fragment_container, nextFragment)
                .addToBackStack(null)
                .commit()

            val bottomNavigationView = activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation_view)
            bottomNavigationView?.selectedItemId = R.id.navigation_home
            Toast.makeText(requireContext(), "Navigating to find partners...", Toast.LENGTH_SHORT).show()
        }

        btnSelectLocation.setOnClickListener {
            showMapSelectionLayout()
            checkLocationPermissionsAndInitMap()
        }

        btnSelectDateTime.setOnClickListener {
            showDatePickerDialog()
        }

        btnConfirmMapLocation.setOnClickListener {
            val address = mapSelectedAddressTextView.text.toString()
            if (address.isNotEmpty() && address != "Retrieving address..." && address != "Address not found" && address != "Error getting address" && address != "Geocoder not available" && address != "Invalid location") {
                selectedLocation = address
                btnSelectLocation.text = address
                Toast.makeText(context, "Location confirmed: $address", Toast.LENGTH_SHORT).show()
                hideMapSelectionLayout()
            } else {
                Toast.makeText(context, "Please select a valid location on the map.", Toast.LENGTH_SHORT).show()
            }
        }

        btnSend.setOnClickListener {
            createDateRequest()
        }

        fetchLikedUsers()
    }

    private fun handlePartnerClick(clickedPartner: UserProfileResponse) {
        if (selectedPartner?.id == clickedPartner.id) {
            selectedPartner = null
            partnersAdapter.setSelectedPartner(null)
            textSelectedPartner.text = ""
            textSelectedPartner.visibility = View.GONE
            Toast.makeText(context, "${clickedPartner.profile?.firstName} deselected", Toast.LENGTH_SHORT).show()
        } else {
            selectedPartner = clickedPartner
            partnersAdapter.setSelectedPartner(clickedPartner.id)
            textSelectedPartner.text = "${clickedPartner.profile?.firstName } ${clickedPartner.profile?.lastName }  |  ${calculateAge(clickedPartner.profile?.dateOfBirth.toString())}Y"
            textSelectedPartner.visibility = View.VISIBLE
            Toast.makeText(context, "Selected partner: ${clickedPartner.profile?.firstName}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchLikedUsers() {
        val sessionManager = SessionManager(requireContext())
        val authToken = sessionManager.getAuthToken()

        if (authToken.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Authentication required to fetch liked users.", Toast.LENGTH_SHORT).show()
            partnersRecyclerView.visibility = View.GONE
            btnFindPartner.visibility = View.VISIBLE
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.getLikedUsers()

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val likedUsers = response.body()!!
                        Log.d("LikedUsers", "Fetched ${likedUsers.size} liked users.")

                        likedPartnersList.clear()
                        likedPartnersList.addAll(likedUsers)
                        partnersAdapter.notifyDataSetChanged()

                        if (likedUsers.isEmpty()) {
                            partnersRecyclerView.visibility = View.GONE
                            btnFindPartner.visibility = View.VISIBLE
                            Toast.makeText(
                                requireContext(),
                                "No liked users found. Find someone new!",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            partnersRecyclerView.visibility = View.VISIBLE
                            btnFindPartner.visibility = View.GONE
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()
                        val errorMessage = "Failed to fetch liked users: ${response.code()}" +
                                (errorBody?.let { " - $it" } ?: "")
                        Log.e("LikedUsers", errorMessage)
                        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                        partnersRecyclerView.visibility = View.GONE
                        btnFindPartner.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val errorMessage = "Error fetching liked users: ${e.message}"
                    Log.e("LikedUsers", errorMessage, e)
                    Toast.makeText(
                        requireContext(),
                        "Network error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    partnersRecyclerView.visibility = View.GONE
                    btnFindPartner.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun createDateRequest() {
        val partner = selectedPartner
        if (partner == null) {
            Toast.makeText(context, "Please select a partner.", Toast.LENGTH_SHORT).show()
            return // Exits here if partner is null
        }

        val dateTypeName = selectedDateType?.name
        if (dateTypeName.isNullOrEmpty()) {
            Toast.makeText(context, "Date type not selected or has no name.", Toast.LENGTH_SHORT).show()
            return
        }

        val message = messageEditText.text.toString().trim()

        val dateRequest = DateRequest(
            dateTypeName = dateTypeName,
            partnerId = partner.id.toString(),
            location = selectedLocation,
            dateTime = selectedDateTime,
            message = if (message.isEmpty()) null else message,
            status = "pending"
        )

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.createDateRequest(dateRequest)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body() != null) {
                        val createDateRequestResponse = response.body()
                        Toast.makeText(
                            context,
                            createDateRequestResponse?.message ?: "Date request sent!",
                            Toast.LENGTH_LONG
                        ).show()
                        parentFragmentManager.popBackStack()
                    } else {
                        val errorBody = response.errorBody()?.string()
                        val errorMessage = "Failed to send date request: ${response.code()}" +
                                (errorBody?.let { " - $it" } ?: "")
                        Log.e("CreateDateRequest", errorMessage)
                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val errorMessage = "Error sending date request: ${e.message}"
                    Log.e("CreateDateRequest", errorMessage, e)
                    Toast.makeText(context, "Network error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun calculateAge(dob: String): Int {
        if (dob.isEmpty()) return 0
        return try {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val birthDate = LocalDate.parse(dob, formatter)
            val today = LocalDate.now()
            today.year - birthDate.year -
                    if (today.dayOfYear < birthDate.dayOfYear) 1 else 0
        } catch (e: Exception) {
            Log.e("AgeCalc", "Error parsing DOB '$dob': ${e.message}")
            0
        }
    }

    private fun showDatePickerDialog() {
        val year = selectedCalendar.get(Calendar.YEAR)
        val month = selectedCalendar.get(Calendar.MONTH)
        val day = selectedCalendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                selectedCalendar.set(selectedYear, selectedMonth, selectedDayOfMonth)
                showTimePickerDialog() // After date, show time picker
            },
            year,
            month,
            day
        )

        datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000
        datePickerDialog.show()
    }

    private fun showTimePickerDialog() {
        val hour = selectedCalendar.get(Calendar.HOUR_OF_DAY)
        val minute = selectedCalendar.get(Calendar.MINUTE)

        val timePickerDialog =
            TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
                selectedCalendar.set(Calendar.HOUR_OF_DAY, selectedHour)
                selectedCalendar.set(Calendar.MINUTE, selectedMinute)

                updateDateTimeText()
                Toast.makeText(context, "Date and Time selected.", Toast.LENGTH_SHORT).show()
            }, hour, minute, false)
        timePickerDialog.show()
    }

    private fun updateDateTimeText() {
        val displayFormat = SimpleDateFormat("EEE, dd MMM yyyy 'at' hh:mm a", Locale.getDefault())
        btnSelectDateTime.text = displayFormat.format(selectedCalendar.time)

        val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        isoDateFormat.timeZone = TimeZone.getTimeZone("UTC") // Crucial for 'Z'
        selectedDateTime = isoDateFormat.format(selectedCalendar.time)
    }

    private fun showMapSelectionLayout() {
        mapSelectionLayout.visibility = View.VISIBLE
    }

    private fun hideMapSelectionLayout() {
        mapSelectionLayout.visibility = View.GONE
    }

    private fun checkLocationPermissionsAndInitMap() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getMapAndInit()
        } else {
            // Request permissions
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun getMapAndInit() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapFragmentContainer) as? SupportMapFragment
        mapFragment?.getMapAsync(this) ?: run {
            Log.e("CreateDateFragment", "Map fragment not found!")
            Toast.makeText(requireContext(), "Error loading map.", Toast.LENGTH_SHORT).show()
            hideMapSelectionLayout()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isCompassEnabled = true
        googleMap.uiSettings.isMyLocationButtonEnabled = true
        view?.findViewById<ImageView>(R.id.centerMarker)?.bringToFront()
        googleMap.setOnCameraIdleListener {
            val centerLatLng = googleMap.cameraPosition.target
            reverseGeocodeLocation(centerLatLng)
        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.isMyLocationEnabled = true
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    val userLatLng = LatLng(it.latitude, it.longitude)
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
                    reverseGeocodeLocation(userLatLng)
                } ?: run {
                    Log.d("CreateDateFragment", "Last known location is null, defaulting to Phnom Penh.")
                    loadMapWithDefaultLocation()
                }
            }.addOnFailureListener { e ->
                Log.e("CreateDateFragment", "Error getting last known location: ${e.message}")
                Toast.makeText(requireContext(), "Could not get current location: ${e.message}", Toast.LENGTH_LONG).show()
                loadMapWithDefaultLocation()
            }
        } else {
            loadMapWithDefaultLocation()
        }
    }

    private fun loadMapWithDefaultLocation() {
        val defaultLatLng = LatLng(11.5564, 104.9282)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLatLng, 10f))
        reverseGeocodeLocation(defaultLatLng)
    }


    private fun reverseGeocodeLocation(latLng: LatLng) {
        mapSelectedAddressTextView.text = "Retrieving address..."
        if (geocoder == null || !Geocoder.isPresent()) {
            mapSelectedAddressTextView.text = "Geocoder not available"
            Log.e("CreateDateFragment", "Geocoder service is not available on this device.")
            Toast.makeText(requireContext(), "Geocoder service not available.", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val addresses = geocoder?.getFromLocation(latLng.latitude, latLng.longitude, 1)
                withContext(Dispatchers.Main) {
                    if (addresses != null && addresses.isNotEmpty()) {
                        val address = addresses[0]
                        val addressLines = (0..address.maxAddressLineIndex).mapNotNull { i ->
                            address.getAddressLine(i)
                        }
                        val fullAddress = addressLines.joinToString(", ")
                        mapSelectedAddressTextView.text = fullAddress
                        Log.d("CreateDateFragment", "Address found: $fullAddress")
                    } else {
                        mapSelectedAddressTextView.text = "Address not found"
                        Log.d("CreateDateFragment", "No address found for $latLng")
                    }
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    mapSelectedAddressTextView.text = "Error getting address"
                    Log.e("CreateDateFragment", "Geocoding service IO error: ${e.message}", e)
                    Toast.makeText(
                        requireContext(),
                        "Geocoding error: Check internet connection.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: IllegalArgumentException) {
                withContext(Dispatchers.Main) {
                    mapSelectedAddressTextView.text = "Invalid location"
                    Log.e(
                        "CreateDateFragment",
                        "Invalid latitude/longitude for geocoding: ${e.message}",
                        e
                    )
                }
            }
        }
    }
}
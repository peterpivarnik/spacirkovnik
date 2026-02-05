package com.example.spacirkovnik

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Looper
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.content.ContextCompat.checkSelfPermission
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices.getFusedLocationProviderClient
import com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY

@Composable
fun LocationDisplay(locationViewModel: LocationViewModel, context: Context) {

    val fusedLocationClient: FusedLocationProviderClient = getFusedLocationProviderClient(context)
    val location = locationViewModel.location.value

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            if (permissions[ACCESS_COARSE_LOCATION] == true && permissions[ACCESS_FINE_LOCATION] == true) {
                val locationCallback = object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        super.onLocationResult(locationResult)
                        locationResult.lastLocation?.let {
                            val location = LocationData(latitude = it.latitude, longitude = it.longitude)
                            locationViewModel.updateLocation(location)
                        }
                    }
                }
                val locationRequest = LocationRequest.Builder(PRIORITY_HIGH_ACCURACY, 1000)
                        .build()
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            }
            else {
                val rationaleRequired =
                        shouldShowRequestPermissionRationale(context as MainActivity, ACCESS_FINE_LOCATION)
                        || shouldShowRequestPermissionRationale(context, ACCESS_COARSE_LOCATION)
                if (rationaleRequired) {
                    Toast.makeText(context,
                                   "Location Permission is required for this feature to work",
                                   LENGTH_LONG)
                            .show()
                }
                else {
                    Toast.makeText(context,
                                   "Location Permission is required. Please enable it in the Android Settings",
                                   LENGTH_LONG)
                            .show()
                }
            }
        })

    Column() {

        if (location != null) {
            TextField(value = "Location: ${location.latitude}; ${location.longitude} ",
                      onValueChange = {},
                      modifier = Modifier.padding(vertical = 4.dp),
                      shape = RoundedCornerShape(8.dp),
                      enabled = true,
                      readOnly = true,
                      textStyle = TextStyle.Default.copy(fontSize = 16.sp),
                      colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color(0xffd8e6ff),
                                                                focusedContainerColor = Color(0xffd8e6ff)))
        }
        else {
            TextField(value = "Location is not known ",
                      onValueChange = {},
                      modifier = Modifier.padding(vertical = 4.dp),
                      shape = RoundedCornerShape(8.dp),
                      enabled = true,
                      readOnly = true,
                      textStyle = TextStyle.Default.copy(fontSize = 16.sp),
                      colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color(0xffd8e6ff),
                                                                focusedContainerColor = Color(0xffd8e6ff)))
        }
        Button(onClick = {
            if (checkSelfPermission(context, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED
                && checkSelfPermission(context, ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED) {
                val locationCallback = object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        super.onLocationResult(locationResult)
                        locationResult.lastLocation?.let {
                            val location = LocationData(latitude = it.latitude, longitude = it.longitude)
                            locationViewModel.updateLocation(location)
                        }
                    }
                }
                val locationRequest = LocationRequest.Builder(PRIORITY_HIGH_ACCURACY, 1000)
                        .build()
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            }
            else {
                requestPermissionLauncher.launch(arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION))
            }
        }) {
            Text(text = "Get Location")
        }
    }
}

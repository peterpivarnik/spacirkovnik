package sk.spacirkovnik.ui.component

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import sk.spacirkovnik.model.LocationData
import sk.spacirkovnik.viewmodel.LocationViewModel

/**
 * Posiela polohu z GPS do [locationViewModel], kým je volajúca obrazovka zobrazená.
 * Pri prvom zobrazení si vypýta povolenie na polohu, ak ho appka ešte nemá.
 *
 * GPS beží len vtedy, keď je navigácia na obrazovke: vypne sa pri odchode z navigačnej
 * obrazovky (otázka, koniec hry, ukončenie) aj keď appka odíde do pozadia / zhasne displej,
 * a znova sa zapne po návrate.
 */
@Composable
internal fun TrackLocationWhileVisible(context: Context, locationViewModel: LocationViewModel) {
    var locationStarted by remember { mutableStateOf(false) }

    // Klient aj callback držíme cez rekompozície — rovnaký callback treba pri vypnutí odovzdať
    // do removeLocationUpdates, inak sa GPS nevypne.
    val fusedClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val locationCallback = remember(locationViewModel) {
        object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let {
                    locationViewModel.updateLocation(LocationData(it.latitude, it.longitude))
                }
            }
        }
    }

    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(context, ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun startLocationUpdates() {
        if (locationStarted) return
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).build()
        try {
            fusedClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            locationStarted = true
        }
        catch (_: SecurityException) {
        }
    }

    fun stopLocationUpdates() {
        if (!locationStarted) return
        fusedClient.removeLocationUpdates(locationCallback)
        locationStarted = false
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[ACCESS_FINE_LOCATION] == true || permissions[ACCESS_COARSE_LOCATION] == true) {
            startLocationUpdates()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission()) {
            permissionLauncher.launch(arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION))
        }
    }

    // Pridanie observera do už spustenej lifecycle doručí ON_START hneď, takže toto zapne GPS
    // aj pri prvom zobrazení.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> if (hasLocationPermission()) startLocationUpdates()
                Lifecycle.Event.ON_STOP -> stopLocationUpdates()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            stopLocationUpdates()
        }
    }
}

/**
 * Natočenie telefónu voči severu v stupňoch (z akcelerometra a magnetometra), vyhladené.
 * Senzory bežia, kým je volajúca obrazovka zobrazená.
 */
@Composable
internal fun rememberDeviceAzimuth(context: Context): Float {
    var deviceAzimuth by remember { mutableFloatStateOf(0f) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val gravity = FloatArray(3)
        val geomagnetic = FloatArray(3)
        var hasGravity = false
        var hasMagnetic = false

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        // alpha = 1 on the first sample seeds the value; after that it smooths.
                        lowPassFilter(event.values, gravity, if (hasGravity) COMPASS_SMOOTHING else 1f)
                        hasGravity = true
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        lowPassFilter(event.values, geomagnetic, if (hasMagnetic) COMPASS_SMOOTHING else 1f)
                        hasMagnetic = true
                    }
                }
                if (hasGravity && hasMagnetic) {
                    val r = FloatArray(9)
                    val i = FloatArray(9)
                    if (SensorManager.getRotationMatrix(r, i, gravity, geomagnetic)) {
                        val orientation = FloatArray(3)
                        SensorManager.getOrientation(r, orientation)
                        deviceAzimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        accelerometer?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }
        magnetometer?.let {
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    return deviceAzimuth
}

/**
 * Exponential low-pass smoothing factor for the raw compass sensors (accelerometer +
 * magnetometer). Lower = smoother needle but more lag; ~0.15 is a good balance.
 */
private const val COMPASS_SMOOTHING = 0.15f

/** In-place exponential low-pass: output += alpha * (input - output). */
private fun lowPassFilter(input: FloatArray, output: FloatArray, alpha: Float) {
    for (i in output.indices) {
        output[i] += alpha * (input[i] - output[i])
    }
}

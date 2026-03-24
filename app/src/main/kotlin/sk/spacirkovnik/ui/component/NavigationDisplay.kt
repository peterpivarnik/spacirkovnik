package sk.spacirkovnik.ui.component

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Looper
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import sk.spacirkovnik.model.GameScreen
import sk.spacirkovnik.model.LocationData
import sk.spacirkovnik.viewmodel.LocationViewModel

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.pm.ApplicationInfo
import sk.spacirkovnik.ui.theme.Amber
import sk.spacirkovnik.ui.theme.CardBg
import sk.spacirkovnik.ui.theme.DisabledButton
import sk.spacirkovnik.ui.theme.PrimaryButton
import sk.spacirkovnik.ui.theme.PrimaryButtonText
import sk.spacirkovnik.ui.theme.TextDark
import sk.spacirkovnik.ui.theme.TextOnDark

@Composable
fun NavigationDisplay(
    gameScreen: GameScreen,
    locationViewModel: LocationViewModel,
    context: Context,
    onContinue: () -> Unit
) {
    val targetLat = gameScreen.targetLatitude ?: return
    val targetLng = gameScreen.targetLongitude ?: return

    var deviceAzimuth by remember { mutableFloatStateOf(0f) }
    var bearingToTarget by remember { mutableFloatStateOf(0f) }
    var distanceMeters by remember { mutableStateOf<Int?>(null) }
    var locationStarted by remember { mutableStateOf(false) }

    fun startLocationUpdates() {
        if (locationStarted) return
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let {
                    locationViewModel.updateLocation(LocationData(it.latitude, it.longitude))
                }
            }
        }
        try {
            fusedClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
            locationStarted = true
        } catch (_: SecurityException) { }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            if (permissions[ACCESS_FINE_LOCATION] == true || permissions[ACCESS_COARSE_LOCATION] == true) {
                startLocationUpdates()
            }
        }
    )

    LaunchedEffect(Unit) {
        val hasFine = ContextCompat.checkSelfPermission(context, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) {
            startLocationUpdates()
        } else {
            permissionLauncher.launch(arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION))
        }
    }

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
                        System.arraycopy(event.values, 0, gravity, 0, 3)
                        hasGravity = true
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        System.arraycopy(event.values, 0, geomagnetic, 0, 3)
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

    val currentLocation = locationViewModel.location.value
    if (currentLocation != null) {
        val results = FloatArray(2)
        Location.distanceBetween(
            currentLocation.latitude, currentLocation.longitude,
            targetLat, targetLng,
            results
        )
        distanceMeters = results[0].toInt()
        bearingToTarget = results[1]
    }

    val arrowRotation = bearingToTarget - deviceAzimuth

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (!gameScreen.imageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = gameScreen.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                        contentScale = ContentScale.FillWidth
                    )
                }
                Text(
                    text = gameScreen.text ?: "",
                    fontSize = (gameScreen.fontSize ?: 18).sp,
                    lineHeight = ((gameScreen.fontSize ?: 18) + 8).sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(20.dp),
                    color = TextDark
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Canvas(modifier = Modifier.size(160.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2

            drawCircle(
                color = TextOnDark,
                radius = radius,
                center = center,
                alpha = 0.15f
            )
            drawCircle(
                color = TextOnDark,
                radius = radius,
                center = center,
                style = Stroke(width = 3f),
                alpha = 0.6f
            )
            drawCircle(
                color = TextOnDark,
                radius = radius * 0.85f,
                center = center,
                style = Stroke(width = 1.5f),
                alpha = 0.3f
            )

            rotate(degrees = arrowRotation, pivot = center) {
                drawArrow(center, radius * 0.7f)
            }

            drawCircle(
                color = TextOnDark,
                radius = 6f,
                center = center
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (distanceMeters != null) {
            val distanceText = if (distanceMeters!! >= 1000) {
                String.format(java.util.Locale.getDefault(), "%.1f km", distanceMeters!! / 1000f)
            } else {
                "$distanceMeters m"
            }
            Text(
                text = distanceText,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = TextOnDark
            )
        } else {
            Text(
                text = "Hľadám polohu...",
                fontSize = 18.sp,
                color = TextOnDark.copy(alpha = 0.7f)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        val isCloseEnough = distanceMeters != null && distanceMeters!! <= 10
        Button(
            onClick = onContinue,
            enabled = isCloseEnough,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryButton,
                contentColor = PrimaryButtonText,
                disabledContainerColor = DisabledButton
            )
        ) {
            Text(
                text = if (isCloseEnough) "Pokračovať" else "Príď bližšie k cieľu",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        val isDebug = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        if (isDebug) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red.copy(alpha = 0.6f),
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "⏭ Preskočiť (DEBUG)",
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

private fun DrawScope.drawArrow(center: Offset, length: Float) {
    val arrowWidth = length * 0.35f

    val arrowPath = Path().apply {
        moveTo(center.x, center.y - length)
        lineTo(center.x - arrowWidth, center.y + length * 0.15f)
        lineTo(center.x, center.y - length * 0.1f)
        lineTo(center.x + arrowWidth, center.y + length * 0.15f)
        close()
    }
    drawPath(arrowPath, color = Amber)

    val tailPath = Path().apply {
        moveTo(center.x, center.y + length * 0.7f)
        lineTo(center.x - arrowWidth * 0.5f, center.y + length * 0.1f)
        lineTo(center.x, center.y + length * 0.25f)
        lineTo(center.x + arrowWidth * 0.5f, center.y + length * 0.1f)
        close()
    }
    drawPath(tailPath, color = Color.White.copy(alpha = 0.5f))
}

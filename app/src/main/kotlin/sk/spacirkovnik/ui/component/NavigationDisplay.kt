package sk.spacirkovnik.ui.component

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Looper
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import kotlinx.coroutines.launch
import sk.spacirkovnik.model.GameScreen
import sk.spacirkovnik.model.LocationData
import sk.spacirkovnik.viewmodel.LocationViewModel

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import sk.spacirkovnik.BuildConfig
import sk.spacirkovnik.ui.theme.Amber
import sk.spacirkovnik.ui.theme.CardBg
import sk.spacirkovnik.ui.theme.DisabledButton
import sk.spacirkovnik.ui.theme.PrimaryButton
import sk.spacirkovnik.ui.theme.PrimaryButtonText
import sk.spacirkovnik.ui.theme.TextDark
import sk.spacirkovnik.ui.theme.TextOnDark
import androidx.core.graphics.createBitmap
import androidx.compose.ui.platform.LocalLocale

@Composable
fun NavigationDisplay(
        gameScreen: GameScreen,
        locationViewModel: LocationViewModel,
        context: Context,
        onContinue: () -> Unit,
        debugStartLocation: LocationData? = null
) {
    val targetLat = gameScreen.targetLatitude ?: return
    val targetLng = gameScreen.targetLongitude ?: return

    var deviceAzimuth by remember { mutableFloatStateOf(0f) }
    var bearingToTarget by remember { mutableFloatStateOf(0f) }
    var locationStarted by remember { mutableStateOf(false) }
    var showMap by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val cameraPositionState = rememberCameraPositionState()
    val coroutineScope = rememberCoroutineScope()

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
        }
        catch (_: SecurityException) {
        }
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
        val hasFine =
                ContextCompat.checkSelfPermission(context, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse =
                ContextCompat.checkSelfPermission(context, ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) {
            startLocationUpdates()
        }
        else {
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
    val distanceMeters: Int? = currentLocation?.let {
        val results = FloatArray(2)
        Location.distanceBetween(it.latitude, it.longitude, targetLat, targetLng, results)
        bearingToTarget = results[1]
        results[0].toInt()
    }

    val arrowRotation = bearingToTarget - deviceAzimuth

    Column(
        modifier = Modifier
                .fillMaxSize()
                .verticalScrollbar(scrollState)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
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

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEDD5))
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFC05800),
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Dávaj pozor na okolie! Cestou môžeš naraziť na cesty s autami, vodné plochy alebo iné prekážky.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = Color(0xFF7A3500)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { showMap = !showMap },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextOnDark)
        ) {
            Text(
                text = if (showMap) "Zobraziť kompas" else "Zobraziť mapu",
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (showMap) {
            val targetLatLng = LatLng(targetLat, targetLng)
            val userLatLng = locationViewModel.location.value?.let {
                LatLng(it.latitude, it.longitude)
            }

            var routePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
            var arrowData by remember { mutableStateOf<Pair<LatLng, Float>?>(null) }
            var arrowIcon by remember { mutableStateOf<com.google.android.gms.maps.model.BitmapDescriptor?>(null) }

            LaunchedEffect(userLatLng) {
                if (userLatLng != null) {
                    val bounds = LatLngBounds.builder()
                            .include(targetLatLng)
                            .include(userLatLng)
                            .build()
                    coroutineScope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngBounds(bounds, 120)
                        )
                    }
                    kotlinx.coroutines.delay(800)
                    val points = fetchWalkingRoute(userLatLng, targetLatLng, BuildConfig.MAPS_API_KEY)
                    routePoints = points
                    if (points.size >= 2) {
                        val idx = points.size / 3
                        val next = minOf(idx + 1, points.size - 1)
                        val results = FloatArray(2)
                        Location.distanceBetween(
                            points[idx].latitude, points[idx].longitude,
                            points[next].latitude, points[next].longitude,
                            results
                        )
                        arrowData = Pair(points[idx], results[1])
                    }
                }
                else {
                    coroutineScope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(targetLatLng, 15f)
                        )
                    }
                }
            }

            GoogleMap(
                modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(12.dp)),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = false),
                uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false),
                onMapLoaded = { arrowIcon = createArrowBitmap() }
            ) {
                Marker(
                    state = rememberUpdatedMarkerState(position = targetLatLng),
                    title = "Cieľ",
                    snippet = "Tu chceš dôjsť",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                )
                userLatLng?.let {
                    Marker(
                        state = rememberUpdatedMarkerState(position = it),
                        title = "Tu sa nachádzaš",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                    )
                    if (routePoints.size >= 2) {
                        Polyline(
                            points = routePoints,
                            color = Color(0xFF1565C0),
                            width = 10f
                        )
                        if (arrowIcon != null) {
                            arrowData?.let { (pos, bearing) ->
                                Marker(
                                    state = rememberUpdatedMarkerState(position = pos),
                                    icon = arrowIcon,
                                    rotation = bearing,
                                    anchor = Offset(0.5f, 0.5f),
                                    flat = true,
                                    title = null
                                )
                            }
                        }
                    } else {
                        Polyline(
                            points = listOf(it, targetLatLng),
                            color = Color(0xFF1565C0),
                            width = 6f,
                            pattern = listOf(
                                com.google.android.gms.maps.model.Dash(20f),
                                com.google.android.gms.maps.model.Gap(10f)
                            )
                        )
                    }
                }
            }
        }
        else {
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
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (distanceMeters != null) {
            val distanceText = if (distanceMeters >= 1000) {
                String.format(LocalLocale.current.platformLocale, "%.1f km", distanceMeters / 1000f)
            } else {
                "$distanceMeters m"
            }
            Text(
                text = distanceText,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = TextOnDark
            )
            Text(
                text = "vzdušnou čiarou",
                fontSize = 13.sp,
                color = TextOnDark.copy(alpha = 0.55f)
            )
        }
        else {
            Text(
                text = "Hľadám polohu...",
                fontSize = 18.sp,
                color = TextOnDark.copy(alpha = 0.7f)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        val isCloseEnough = distanceMeters != null && distanceMeters <= 10
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
                text = if (isCloseEnough) "Pokračovať" else "Musíš sa priblížiť",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }


        if (BuildConfig.DEBUG) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onContinue,
                modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF444444),
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "[DEBUG] Preskočiť navigáciu",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val loc = debugStartLocation ?: LocationData(48.109349, 17.114448)
                    locationViewModel.setDebugLocation(loc)
                },
                modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF444444),
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "[DEBUG] Nastaviť polohu",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal
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

private suspend fun fetchWalkingRoute(
    origin: LatLng,
    destination: LatLng,
    apiKey: String
): List<LatLng> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    try {
        val url = "https://maps.googleapis.com/maps/api/directions/json" +
            "?origin=${origin.latitude},${origin.longitude}" +
            "&destination=${destination.latitude},${destination.longitude}" +
            "&mode=walking" +
            "&key=$apiKey"
        val response = java.net.URL(url).readText()
        val json = com.google.gson.JsonParser.parseString(response).asJsonObject
        val routes = json.getAsJsonArray("routes")
        if (routes.size() > 0) {
            val encoded = routes[0].asJsonObject
                .getAsJsonObject("overview_polyline")
                .get("points").asString
            decodePolyline(encoded)
        } else emptyList()
    } catch (_: Exception) {
        emptyList()
    }
}

private fun decodePolyline(encoded: String): List<LatLng> {
    val poly = mutableListOf<LatLng>()
    var index = 0
    var lat = 0
    var lng = 0
    while (index < encoded.length) {
        var b: Int
        var shift = 0
        var result = 0
        do {
            b = encoded[index++].code - 63
            result = result or ((b and 0x1f) shl shift)
            shift += 5
        } while (b >= 0x20)
        lat += if (result and 1 != 0) (result shr 1).inv() else result shr 1
        shift = 0
        result = 0
        do {
            b = encoded[index++].code - 63
            result = result or ((b and 0x1f) shl shift)
            shift += 5
        } while (b >= 0x20)
        lng += if (result and 1 != 0) (result shr 1).inv() else result shr 1
        poly.add(LatLng(lat / 1e5, lng / 1e5))
    }
    return poly
}

private fun createArrowBitmap(): com.google.android.gms.maps.model.BitmapDescriptor {
    val size = 64
    val bmp = createBitmap(size, size)
    val canvas = android.graphics.Canvas(bmp)
    val fillPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(21, 101, 192)
        style = android.graphics.Paint.Style.FILL
    }
    val strokePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 4f
    }
    val path = android.graphics.Path().apply {
        moveTo(size / 2f, 4f)
        lineTo(size - 8f, size - 8f)
        lineTo(size / 2f, size * 0.58f)
        lineTo(8f, size - 8f)
        close()
    }
    canvas.drawPath(path, fillPaint)
    canvas.drawPath(path, strokePaint)
    return BitmapDescriptorFactory.fromBitmap(bmp)
}

fun Modifier.verticalScrollbar(
        state: ScrollState,
        width: Dp = 6.dp,
        color: Color = Color.White.copy(alpha = 0.6f)
): Modifier = drawWithContent {
    drawContent()
    if (state.maxValue > 0) {
        val height = size.height
        val scrollValue = state.value.toFloat()
        val maxScrollValue = state.maxValue.toFloat()

        val scrollbarHeight = (height * height) / (maxScrollValue + height)
        val scrollbarOffset = (scrollValue * (height - scrollbarHeight)) / maxScrollValue

        drawRect(
            color = color,
            topLeft = Offset(size.width - width.toPx(), scrollbarOffset),
            size = Size(width.toPx(), scrollbarHeight)
        )
    }
}

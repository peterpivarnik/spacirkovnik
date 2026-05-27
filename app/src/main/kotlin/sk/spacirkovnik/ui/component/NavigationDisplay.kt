package sk.spacirkovnik.ui.component

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RectF
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import coil3.compose.AsyncImage
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.IconImage
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation
import com.mapbox.maps.extension.compose.annotation.generated.PolylineAnnotation
import com.mapbox.maps.extension.compose.style.MapStyle
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import sk.spacirkovnik.BuildConfig
import sk.spacirkovnik.model.GameScreen
import sk.spacirkovnik.model.LocationData
import sk.spacirkovnik.ui.theme.Amber
import sk.spacirkovnik.ui.theme.CardBg
import sk.spacirkovnik.ui.theme.DisabledButton
import sk.spacirkovnik.ui.theme.PrimaryButton
import sk.spacirkovnik.ui.theme.PrimaryButtonText
import sk.spacirkovnik.ui.theme.TextDark
import sk.spacirkovnik.ui.theme.TextOnDark
import sk.spacirkovnik.viewmodel.LocationViewModel
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

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

    val targetPoint = Point.fromLngLat(targetLng, targetLat)

    var deviceAzimuth by remember { mutableFloatStateOf(0f) }
    var bearingToTarget by remember { mutableFloatStateOf(0f) }
    var locationStarted by remember { mutableStateOf(false) }

    // Pre-build icon bitmaps once; IconImage wraps them for Mapbox annotations
    val targetPinIcon = remember { IconImage(bitmap = createPinBitmap(android.graphics.Color.rgb(213, 0, 0))) }
    val userPinIcon   = remember { IconImage(bitmap = createPinBitmap(android.graphics.Color.rgb(21, 101, 192))) }
    val arrowIcon     = remember { IconImage(bitmap = createArrowBitmap(android.graphics.Color.rgb(21, 101, 192))) }

    val scrollState = rememberScrollState()

    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(targetPoint)
            zoom(15.0)
        }
    }

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
    val userPoint = currentLocation?.let { Point.fromLngLat(it.longitude, it.latitude) }

    val distanceMeters: Int? = currentLocation?.let {
        val results = FloatArray(2)
        Location.distanceBetween(it.latitude, it.longitude, targetLat, targetLng, results)
        bearingToTarget = results[1]
        results[0].toInt()
    }

    val arrowRotation = bearingToTarget - deviceAzimuth

    // Update map camera when user location changes
    LaunchedEffect(userPoint) {
        val center: Point
        val zoom: Double
        if (userPoint != null) {
            center = Point.fromLngLat(
                (userPoint.longitude() + targetLng) / 2,
                (userPoint.latitude() + targetLat) / 2
            )
            val dist = distanceMeters ?: 300
            zoom = when {
                dist > 1000 -> 12.0
                dist > 500  -> 13.0
                dist > 200  -> 14.0
                else        -> 15.0
            }
        } else {
            center = targetPoint
            zoom = 15.0
        }
        mapViewportState.easeTo(
            CameraOptions.Builder()
                .center(center)
                .zoom(zoom)
                .build()
        )
    }

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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
        ) {
            NavigationMap(
                mapViewportState = mapViewportState,
                targetPoint = targetPoint,
                userPoint = userPoint,
                targetPinIcon = targetPinIcon,
                userPinIcon = userPinIcon,
                arrowIcon = arrowIcon
            )

            // Compass overlay — bottom-right corner of the map
            Canvas(
                modifier = Modifier
                    .size(90.dp)
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
            ) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.minDimension / 2

                // semi-transparent dark backdrop so compass is readable on any map tile
                drawCircle(color = Color(0x99000000), radius = radius, center = center)
                drawCircle(color = TextOnDark, radius = radius, center = center,
                    style = Stroke(width = 2f), alpha = 0.5f)
                drawCircle(color = TextOnDark, radius = radius * 0.82f, center = center,
                    style = Stroke(width = 1f), alpha = 0.25f)

                // Cardinal labels rotate with actual world orientation
                rotate(degrees = -deviceAzimuth, pivot = center) {
                    val nc = drawContext.canvas.nativeCanvas
                    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.WHITE
                        textSize = radius * 0.40f
                        textAlign = Paint.Align.CENTER
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                    }
                    val lr = radius * 0.64f
                    val dy = textPaint.textSize * 0.36f
                    nc.drawText("S", center.x,      center.y - lr + dy, textPaint)
                    nc.drawText("J", center.x,      center.y + lr + dy, textPaint)
                    nc.drawText("V", center.x + lr, center.y      + dy, textPaint)
                    nc.drawText("Z", center.x - lr, center.y      + dy, textPaint)
                }

                // Arrow pointing toward target
                rotate(degrees = arrowRotation, pivot = center) {
                    drawArrow(center, radius * 0.7f)
                }

                drawCircle(color = TextOnDark, radius = 5f, center = center)
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

// NavigationMap is isolated here so the Mapbox composable-scope warnings
// (COMPOSE_APPLIER_CALL_MISMATCH) are suppressed in one small, well-defined place.
@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
@Composable
private fun NavigationMap(
    mapViewportState: MapViewportState,
    targetPoint: Point,
    userPoint: Point?,
    targetPinIcon: IconImage,
    userPinIcon: IconImage,
    arrowIcon: IconImage,
) {
    MapboxMap(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(12.dp)),
        mapViewportState = mapViewportState,
        style = { MapStyle(style = Style.OUTDOORS) }
    ) {
        PointAnnotation(point = targetPoint) {
            iconImage = targetPinIcon
            iconAnchor = IconAnchor.BOTTOM
        }
        userPoint?.let { up ->
            PointAnnotation(point = up) {
                iconImage = userPinIcon
                iconAnchor = IconAnchor.BOTTOM
            }
            PolylineAnnotation(points = listOf(up, targetPoint)) {
                lineColor = Color(0xFF1565C0)
                lineWidth = 3.0
            }
            val midpoint = mapMidpoint(up, targetPoint)
            val bearing = mapBearing(up, targetPoint)
            PointAnnotation(point = midpoint) {
                iconImage = arrowIcon
                iconRotate = bearing
            }
        }
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


/** Teardrop / Google-Maps-style pin bitmap. Anchor at bottom-center. */
private fun createPinBitmap(colorInt: Int): Bitmap {
    val w = 80
    val h = 112   // portrait ratio gives room for the pointed tail
    val bitmap = createBitmap(w, h)
    val canvas = android.graphics.Canvas(bitmap)

    val cx = w / 2f
    val r  = w * 0.42f          // circle radius
    val cy = r + 4f              // circle center Y from top

    // --- teardrop path ---
    // Start at tip, quad-curve up to left of circle, arc over the top,
    // quad-curve back down to tip.
    val path = android.graphics.Path()
    path.moveTo(cx, h.toFloat())
    path.quadTo(cx - r, cy + r,       cx - r, cy)
    path.arcTo(RectF(cx - r, cy - r, cx + r, cy + r), 180f, -180f)
    path.quadTo(cx + r, cy + r,       cx, h.toFloat())
    path.close()

    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorInt
        style = Paint.Style.FILL
    }
    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }
    val white = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }

    canvas.drawPath(path, fill)
    canvas.drawPath(path, stroke)
    canvas.drawCircle(cx, cy, r * 0.38f, white)   // inner white dot

    return bitmap
}

/** Upward-pointing arrow bitmap (Mapbox rotates it via iconRotate). */
private fun createArrowBitmap(colorInt: Int): Bitmap {
    val size = 64
    val bitmap = createBitmap(size, size)
    val canvas = android.graphics.Canvas(bitmap)

    val s = size.toFloat()
    val cx = s / 2f

    val path = android.graphics.Path()
    path.moveTo(cx,        s * 0.05f)   // north tip
    path.lineTo(s * 0.85f, s * 0.72f)  // bottom-right
    path.lineTo(cx,        s * 0.50f)   // center notch
    path.lineTo(s * 0.15f, s * 0.72f)  // bottom-left
    path.close()

    canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorInt
        style = Paint.Style.FILL
    })
    canvas.drawPath(path, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f
    })
    return bitmap
}

/** Compass bearing (degrees, 0 = north, clockwise) from [from] to [to]. */
private fun mapBearing(from: Point, to: Point): Double {
    val lat1 = Math.toRadians(from.latitude())
    val lat2 = Math.toRadians(to.latitude())
    val dLng = Math.toRadians(to.longitude() - from.longitude())
    val y = sin(dLng) * cos(lat2)
    val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLng)
    return (Math.toDegrees(atan2(y, x)) + 360) % 360
}

/** Geographic midpoint between two map points. */
private fun mapMidpoint(p1: Point, p2: Point): Point =
    Point.fromLngLat(
        (p1.longitude() + p2.longitude()) / 2,
        (p1.latitude()  + p2.latitude())  / 2
    )

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

package sk.spacirkovnik.ui.component

import android.graphics.Bitmap
import android.graphics.Paint
import android.location.Location
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.android.gestures.StandardScaleGestureDetector
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.DisposableMapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.MapViewportState
import com.mapbox.maps.extension.compose.annotation.IconImage
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation
import com.mapbox.maps.extension.compose.annotation.generated.PolylineAnnotation
import com.mapbox.maps.extension.compose.style.MapStyle
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.OnScaleListener
import com.mapbox.maps.plugin.gestures.gestures
import sk.spacirkovnik.ui.theme.Amber
import sk.spacirkovnik.ui.theme.TextOnDark
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.log2
import kotlin.math.sin

// Mapa navigácie: samotná mapa Mapbox, kompas, tlačidlá na mape, ikony a výpočet kamery.

/** The map plus its compass and fullscreen-toggle overlays, sized via [modifier]. */
@Composable
internal fun NavMapContent(
    modifier: Modifier,
    mapViewportState: MapViewportState,
    targetPoint: Point,
    state: NavMapState,
    targetPinIcon: IconImage,
    userPinIcon: IconImage,
    arrowIcon: IconImage,
    fullscreen: Boolean,
    distanceMeters: Int?,
    mapHeightDp: Double,
    userMoved: Boolean,
    onToggleFullscreen: () -> Unit,
    onUserGesture: () -> Unit,
    onRestoreAutoView: () -> Unit,
) {
    Box(modifier = modifier) {
        NavigationMap(
            modifier = Modifier.fillMaxSize(),
            mapViewportState = mapViewportState,
            targetPoint = targetPoint,
            userPoint = state.userPoint,
            routePoints = state.routePoints,
            targetPinIcon = targetPinIcon,
            userPinIcon = userPinIcon,
            arrowIcon = arrowIcon,
            onUserGesture = onUserGesture,
        )
        CompassOverlay(
            modifier = Modifier.align(Alignment.BottomStart),
            deviceAzimuth = state.deviceAzimuth,
            arrowRotation = state.arrowRotation,
        )
        FullscreenToggle(
            modifier = Modifier.align(Alignment.BottomEnd),
            fullscreen = fullscreen,
            onClick = onToggleFullscreen,
        )
        RecenterButtons(
            mapViewportState = mapViewportState,
            userPoint = state.userPoint,
            targetPoint = targetPoint,
            distanceMeters = distanceMeters,
            mapHeightDp = mapHeightDp,
            userMoved = userMoved,
            onRecenterUser = onUserGesture,
            onRestoreAutoView = onRestoreAutoView,
        )
    }
}

/**
 * The two map-recentering buttons, stacked top-right:
 *  - "my location": eases to the user's position; shown only while the map isn't already
 *    centred there. Tapping it also leaves auto-follow (via [onRecenterUser]) so the next
 *    location update doesn't yank the camera back.
 *  - "route view": eases back to the framed user→target view the map opens with, and resumes
 *    auto-follow; shown only once the user has taken manual control ([userMoved]).
 */
@Composable
private fun BoxScope.RecenterButtons(
    mapViewportState: MapViewportState,
    userPoint: Point?,
    targetPoint: Point,
    distanceMeters: Int?,
    mapHeightDp: Double,
    userMoved: Boolean,
    onRecenterUser: () -> Unit,
    onRestoreAutoView: () -> Unit,
) {
    val camera = mapViewportState.cameraState
    val centeredOnUser = camera != null && userPoint != null &&
        metersBetween(camera.center, userPoint) < RECENTER_EPS_M

    Column(
        // Stacked at the bottom-right, just above the 64dp fullscreen toggle. The buttons carry
        // 10dp of internal padding each, so a -10dp arrangement leaves a 10dp visible gap between
        // them — matching the gap to the fullscreen toggle below.
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(bottom = 54.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy((-10).dp),
    ) {
        if (userMoved) {
            MapRoundButton(onClick = {
                mapViewportState.easeTo(
                    navCameraOptions(userPoint, targetPoint, distanceMeters, mapHeightDp)
                )
                onRestoreAutoView()
            }) { drawRouteViewIcon() }
        }
        if (userPoint != null && !centeredOnUser) {
            MapRoundButton(onClick = {
                mapViewportState.easeTo(
                    CameraOptions.Builder()
                        .center(userPoint)
                        .zoom(USER_FOCUS_ZOOM)
                        .bearing(0.0)
                        .build()
                )
                onRecenterUser()
            }) { drawMyLocationIcon() }
        }
    }
}

/** Round dark map button matching [FullscreenToggle], with a custom [icon] drawn on top. */
@Composable
private fun MapRoundButton(onClick: () -> Unit, icon: DrawScope.() -> Unit) {
    Canvas(
        modifier = Modifier
            .size(64.dp)
            .padding(10.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2
        drawCircle(color = Color(0x99000000), radius = radius, center = center)
        drawCircle(color = TextOnDark, radius = radius, center = center,
            style = Stroke(width = 2f), alpha = 0.5f)
        icon()
    }
}

/** Crosshair: a ringed centre dot with four ticks — the "centre on my location" glyph. */
private fun DrawScope.drawMyLocationIcon() {
    val s = size.minDimension
    val c = Offset(size.width / 2, size.height / 2)
    val white = Color.White
    val stroke = s * 0.06f
    val ring = s * 0.20f
    drawCircle(white, radius = ring, center = c, style = Stroke(width = stroke))
    drawCircle(white, radius = s * 0.06f, center = c)
    val inner = ring + stroke
    val outer = s * 0.34f
    drawLine(white, Offset(c.x, c.y - inner), Offset(c.x, c.y - outer), stroke, StrokeCap.Round)
    drawLine(white, Offset(c.x, c.y + inner), Offset(c.x, c.y + outer), stroke, StrokeCap.Round)
    drawLine(white, Offset(c.x - inner, c.y), Offset(c.x - outer, c.y), stroke, StrokeCap.Round)
    drawLine(white, Offset(c.x + inner, c.y), Offset(c.x + outer, c.y), stroke, StrokeCap.Round)
}

/** Vertical line with a dot at each end — the framed user→target "route view" glyph. */
private fun DrawScope.drawRouteViewIcon() {
    val s = size.minDimension
    val c = Offset(size.width / 2, size.height / 2)
    val white = Color.White
    val stroke = s * 0.07f
    val half = s * 0.26f
    drawLine(white, Offset(c.x, c.y - half), Offset(c.x, c.y + half), stroke, StrokeCap.Round)
    drawCircle(Color(0xFFD50000), radius = s * 0.11f, center = Offset(c.x, c.y - half))
    drawCircle(Color(0xFF1565C0), radius = s * 0.11f, center = Offset(c.x, c.y + half))
}

/** Straight-line distance in metres between two geographic points. */
private fun metersBetween(a: Point, b: Point): Float {
    val r = FloatArray(1)
    Location.distanceBetween(a.latitude(), a.longitude(), b.latitude(), b.longitude(), r)
    return r[0]
}

internal class NavMapState(
    val userPoint: Point?,
    val routePoints: List<Point>?,
    val deviceAzimuth: Float,
    val arrowRotation: Float,
)

// NavigationMap is isolated here so the Mapbox composable-scope warnings
// (COMPOSE_APPLIER_CALL_MISMATCH) are suppressed in one small, well-defined place.
@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
@Composable
private fun NavigationMap(
    modifier: Modifier,
    mapViewportState: MapViewportState,
    targetPoint: Point,
    userPoint: Point?,
    routePoints: List<Point>?,
    targetPinIcon: IconImage,
    userPinIcon: IconImage,
    arrowIcon: IconImage,
    onUserGesture: () -> Unit,
) {
    MapboxMap(
        modifier = modifier,
        mapViewportState = mapViewportState,
        // Hide Mapbox's built-in compass; we draw our own in the bottom-left corner.
        compass = {},
        style = { MapStyle(style = Style.OUTDOORS) }
    ) {
        // A hand pan (move) or pinch (scale) means the user is taking control of the camera;
        // notify the caller so it stops auto-recentering on the next location update.
        DisposableMapEffect(Unit) { mapView ->
            val moveListener = object : OnMoveListener {
                override fun onMoveBegin(detector: MoveGestureDetector) = onUserGesture()
                override fun onMove(detector: MoveGestureDetector): Boolean = false
                override fun onMoveEnd(detector: MoveGestureDetector) = Unit
            }
            val scaleListener = object : OnScaleListener {
                override fun onScaleBegin(detector: StandardScaleGestureDetector) = onUserGesture()
                override fun onScale(detector: StandardScaleGestureDetector) = Unit
                override fun onScaleEnd(detector: StandardScaleGestureDetector) = Unit
            }
            mapView.gestures.addOnMoveListener(moveListener)
            mapView.gestures.addOnScaleListener(scaleListener)
            onDispose {
                mapView.gestures.removeOnMoveListener(moveListener)
                mapView.gestures.removeOnScaleListener(scaleListener)
            }
        }
        PointAnnotation(point = targetPoint) {
            iconImage = targetPinIcon
            iconAnchor = IconAnchor.CENTER
        }
        userPoint?.let { up ->
            PointAnnotation(point = up) {
                iconImage = userPinIcon
                iconAnchor = IconAnchor.CENTER
            }
            // Walking route along footpaths when available, else a straight line. The
            // Directions API snaps both ends to the nearest path, so the route can stop
            // short of the actual user/target points. Bridge those gaps with straight
            // segments so the line always runs from the user dot to the target dot.
            val linePoints = routePoints?.takeIf { it.size >= 2 }
                ?.let { listOf(up) + it + targetPoint }
                ?: listOf(up, targetPoint)
            PolylineAnnotation(points = linePoints) {
                lineColor = Color(0xFF1565C0)
                lineWidth = 3.0
            }
            // Place the direction arrow at the midpoint of the drawn line.
            val arrowPoint = routeMidpoint(linePoints)
            PointAnnotation(point = arrowPoint) {
                iconImage = arrowIcon
                iconRotate = 0.0
            }
        }
    }
}

/** Compass dial with rotating cardinal labels and an arrow pointing at the target. */
@Composable
private fun CompassOverlay(
    modifier: Modifier,
    deviceAzimuth: Float,
    arrowRotation: Float,
) {
    Canvas(
        modifier = modifier
            .size(90.dp)
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

/** Round button that expands the map to fullscreen, or shrinks it back. */
@Composable
private fun FullscreenToggle(
    modifier: Modifier,
    fullscreen: Boolean,
    onClick: () -> Unit,
) {
    Canvas(
        modifier = modifier
            .size(64.dp)
            .padding(10.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2
        drawCircle(color = Color(0x99000000), radius = radius, center = center)
        drawCircle(color = TextOnDark, radius = radius, center = center,
            style = Stroke(width = 2f), alpha = 0.5f)
        drawFullscreenIcon(expand = !fullscreen)
    }
}

/** Four corner brackets: opening inward = "expand", pulled toward centre = "collapse". */
private fun DrawScope.drawFullscreenIcon(expand: Boolean) {
    val s = size.minDimension
    val arm = s * 0.16f
    val stroke = s * 0.07f
    val white = Color.White
    fun l(a: Offset, b: Offset) = drawLine(white, a, b, stroke, StrokeCap.Round)
    if (expand) {
        val o = s * 0.30f
        l(Offset(o, o), Offset(o + arm, o));               l(Offset(o, o), Offset(o, o + arm))
        l(Offset(s - o, o), Offset(s - o - arm, o));       l(Offset(s - o, o), Offset(s - o, o + arm))
        l(Offset(o, s - o), Offset(o + arm, s - o));       l(Offset(o, s - o), Offset(o, s - o - arm))
        l(Offset(s - o, s - o), Offset(s - o - arm, s - o)); l(Offset(s - o, s - o), Offset(s - o, s - o - arm))
    } else {
        val v = s * 0.44f
        l(Offset(v, v), Offset(v - arm, v));               l(Offset(v, v), Offset(v, v - arm))
        l(Offset(s - v, v), Offset(s - v + arm, v));       l(Offset(s - v, v), Offset(s - v, v - arm))
        l(Offset(v, s - v), Offset(v - arm, s - v));       l(Offset(v, s - v), Offset(v, s - v + arm))
        l(Offset(s - v, s - v), Offset(s - v + arm, s - v)); l(Offset(s - v, s - v), Offset(s - v, s - v + arm))
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


/** Filled circle with white outline. Anchor at center. */
internal fun createCircleBitmap(colorInt: Int): Bitmap {
    val size = 64
    val bitmap = createBitmap(size, size)
    val canvas = android.graphics.Canvas(bitmap)
    val cx = size / 2f
    val r = size * 0.38f

    canvas.drawCircle(cx, cx, r, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = colorInt
        style = Paint.Style.FILL
    })
    canvas.drawCircle(cx, cx, r, Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 6f
    })
    return bitmap
}

/** Upward-pointing arrow bitmap (Mapbox rotates it via iconRotate). */
internal fun createArrowBitmap(colorInt: Int): Bitmap {
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

/** Height of the inline (non-fullscreen) navigation map, shared by the camera math and the composable. */
internal const val MAP_HEIGHT_DP = 280.0

/** Fraction of the map height the user→target line should span (leaves a margin for the pins). */
private const val LINE_FILL_FRACTION = 0.82

/** Zoom the "centre on my location" button eases to. */
private const val USER_FOCUS_ZOOM = 17.0

/** The map counts as "centred on the user" once its centre is within this many metres of them. */
private const val RECENTER_EPS_M = 20f

private const val EARTH_CIRCUMFERENCE_M = 40075016.686
private const val MAPBOX_TILE_SIZE = 512.0

/**
 * Zoom level at which a line of [distanceMeters], drawn vertically through the map centre,
 * fills [LINE_FILL_FRACTION] of a [mapHeightDp]-tall map at the given [latitude].
 * Clamped so very close targets don't over-zoom and far ones don't zoom out too much.
 */
private fun zoomForLineLength(distanceMeters: Int, latitude: Double, mapHeightDp: Double): Double {
    val dist = distanceMeters.coerceAtLeast(1)
    val metersPerDpAtZoom0 = EARTH_CIRCUMFERENCE_M * cos(Math.toRadians(latitude)) / MAPBOX_TILE_SIZE
    val targetMetersPerDp = dist / (LINE_FILL_FRACTION * mapHeightDp)
    val zoom = log2(metersPerDpAtZoom0 / targetMetersPerDp)
    return zoom.coerceIn(12.0, 20.0)
}

/**
 * Camera centred on the midpoint of the user→target line, rotated so that line runs vertically,
 * zoomed so it fills most of a [mapHeightDp]-tall map. Falls back to the target at a fixed zoom
 * while the user location is still unknown.
 */
internal fun navCameraOptions(
    userPoint: Point?,
    targetPoint: Point,
    distanceMeters: Int?,
    mapHeightDp: Double,
): CameraOptions {
    if (userPoint == null) {
        return CameraOptions.Builder().center(targetPoint).zoom(15.0).bearing(0.0).build()
    }
    val center = Point.fromLngLat(
        (userPoint.longitude() + targetPoint.longitude()) / 2,
        (userPoint.latitude() + targetPoint.latitude()) / 2,
    )
    val dist = distanceMeters ?: 300
    return CameraOptions.Builder()
        .center(center)
        .zoom(zoomForLineLength(dist, center.latitude(), mapHeightDp))
        .bearing(mapBearing(userPoint, targetPoint))
        .build()
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

/** Point lying at half the total length along [points], so it sits on the drawn line. */
private fun routeMidpoint(points: List<Point>): Point {
    if (points.size < 2) return points.first()
    val segLengths = DoubleArray(points.size - 1)
    var total = 0.0
    for (i in 0 until points.size - 1) {
        val r = FloatArray(1)
        Location.distanceBetween(
            points[i].latitude(), points[i].longitude(),
            points[i + 1].latitude(), points[i + 1].longitude(), r
        )
        segLengths[i] = r[0].toDouble()
        total += segLengths[i]
    }
    var remaining = total / 2
    for (i in segLengths.indices) {
        if (remaining <= segLengths[i]) {
            val t = if (segLengths[i] == 0.0) 0.0 else remaining / segLengths[i]
            val a = points[i]
            val b = points[i + 1]
            return Point.fromLngLat(
                a.longitude() + (b.longitude() - a.longitude()) * t,
                a.latitude() + (b.latitude() - a.latitude()) * t,
            )
        }
        remaining -= segLengths[i]
    }
    return points.last()
}

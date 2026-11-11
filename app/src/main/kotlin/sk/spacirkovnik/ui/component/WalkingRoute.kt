package sk.spacirkovnik.ui.component

import android.content.Context
import android.location.Location
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mapbox.geojson.Point
import sk.spacirkovnik.R
import sk.spacirkovnik.data.DirectionsRetrofit

/** Pešia trasa k cieľu; obe hodnoty sú null, kým sa nenačíta, alebo keď sa nájsť nedá. */
internal class WalkingRoute(val points: List<Point>?, val distanceMeters: Int?)

/**
 * Walking route along footpaths (Mapbox Directions) from [userPoint] to the target. Null fields
 * until fetched, or when routing fails / no path exists — the caller then falls back to the
 * straight line. With [directRoute] the route is never fetched.
 *
 * Kľúčované cieľom: každá navigačná obrazovka sa vykresľuje tým istým composable, takže bez
 * toho by si ďalšia obrazovka odniesla trasu z predchádzajúcej.
 */
@Composable
internal fun rememberWalkingRoute(
    context: Context,
    userPoint: Point?,
    targetLat: Double,
    targetLng: Double,
    directRoute: Boolean,
): WalkingRoute {
    var routePoints by remember(targetLat, targetLng) { mutableStateOf<List<Point>?>(null) }
    var routeDistanceMeters by remember(targetLat, targetLng) { mutableStateOf<Int?>(null) }
    var lastRouteFetchPoint by remember(targetLat, targetLng) { mutableStateOf<Point?>(null) }
    val mapboxToken = remember { context.getString(R.string.mapbox_access_token) }

    // Re-route only when the user has moved a meaningful distance (avoids hammering the
    // Directions API on every 1 s GPS tick).
    LaunchedEffect(userPoint, directRoute) {
        if (directRoute) return@LaunchedEffect
        val up = userPoint ?: return@LaunchedEffect
        val last = lastRouteFetchPoint
        val movedEnough = last == null || run {
            val r = FloatArray(1)
            Location.distanceBetween(last.latitude(), last.longitude(), up.latitude(), up.longitude(), r)
            r[0] > 15f
        }
        if (!movedEnough) return@LaunchedEffect
        lastRouteFetchPoint = up
        try {
            val response = DirectionsRetrofit.service.getWalkingRoute(
                coords = "${up.longitude()},${up.latitude()};$targetLng,$targetLat",
                accessToken = mapboxToken,
            )
            val route = response.routes.firstOrNull()
            val coords = route?.geometry?.coordinates
            if (route != null && coords != null && coords.size >= 2) {
                routePoints = coords.map { Point.fromLngLat(it[0], it[1]) }
                routeDistanceMeters = route.distance.toInt()
            } else {
                routePoints = null
                routeDistanceMeters = null
            }
        } catch (_: Exception) {
            // Offline or routing error — fall back to the straight line.
            routePoints = null
            routeDistanceMeters = null
        }
    }

    return WalkingRoute(routePoints, routeDistanceMeters)
}

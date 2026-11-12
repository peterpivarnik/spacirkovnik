package sk.spacirkovnik.ui.component

import android.content.Context
import android.location.Location
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.IconImage
import sk.spacirkovnik.BuildConfig
import sk.spacirkovnik.model.GameScreen
import sk.spacirkovnik.model.LocationData
import sk.spacirkovnik.ui.theme.CardBg
import sk.spacirkovnik.ui.theme.DisabledButton
import sk.spacirkovnik.ui.theme.PrimaryButton
import sk.spacirkovnik.ui.theme.PrimaryButtonText
import sk.spacirkovnik.ui.theme.TextDark
import sk.spacirkovnik.ui.theme.TextOnDark
import sk.spacirkovnik.viewmodel.LocationViewModel

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

    var bearingToTarget by remember { mutableFloatStateOf(0f) }

    // Pre-build icon bitmaps once; IconImage wraps them for Mapbox annotations
    val targetPinIcon = remember { IconImage(bitmap = createCircleBitmap(android.graphics.Color.rgb(213, 0, 0))) }
    val userPinIcon   = remember { IconImage(bitmap = createCircleBitmap(android.graphics.Color.rgb(21, 101, 192))) }
    val arrowIcon     = remember { IconImage(bitmap = createArrowBitmap(android.graphics.Color.rgb(21, 101, 192))) }

    val scrollState = rememberScrollState()

    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(targetPoint)
            zoom(15.0)
        }
    }

    TrackLocationWhileVisible(context, locationViewModel)
    KeepScreenOnWhileVisible()
    val deviceAzimuth = rememberDeviceAzimuth(context)

    val currentLocation = locationViewModel.location.value
    val userPoint = currentLocation?.let { Point.fromLngLat(it.longitude, it.latitude) }

    val distanceMeters: Int? = currentLocation?.let {
        val results = FloatArray(2)
        Location.distanceBetween(it.latitude, it.longitude, targetLat, targetLng, results)
        bearingToTarget = results[1]
        results[0].toInt()
    }

    val arrowRotation = bearingToTarget - deviceAzimuth

    // Hráč došiel k cieľu: zavibruje raz za zastávku — aj keď GPS okolo hranice 10 m
    // poskakuje dnu a von, ďalšie vibrácie by len rušili.
    val isCloseEnough = distanceMeters != null && distanceMeters <= ARRIVAL_RADIUS_METERS
    var arrivalAnnounced by remember(targetLat, targetLng) { mutableStateOf(false) }
    LaunchedEffect(isCloseEnough) {
        if (isCloseEnough && !arrivalAnnounced) {
            arrivalAnnounced = true
            Haptics.arrived(context)
        }
    }

    val route = rememberWalkingRoute(
        context = context,
        userPoint = userPoint,
        targetLat = targetLat,
        targetLng = targetLng,
        directRoute = gameScreen.directRoute,
    )

    // Distance shown to the player: real walking-route length when available, otherwise
    // the straight-line distance.
    val displayDistanceMeters = route.distanceMeters ?: distanceMeters

    // Fullscreen toggle for the map.
    val fullscreenState = remember { mutableStateOf(false) }

    // The expanded map fills the whole screen, the inline one is a fixed height. The camera
    // zoom is derived from the map height so the user→target line fills the same fraction of
    // the map in both modes (closer = more zoomed in) — see [zoomForLineLength].
    val containerHeightPx = LocalWindowInfo.current.containerSize.height
    val screenHeightDp = with(LocalDensity.current) { containerHeightPx.toDp().value.toDouble() }

    // Keep the inline map's camera following the user, easing smoothly so walking feels fluid —
    // until the user pans/zooms by hand, after which we stop overriding their chosen view.
    var inlineUserMoved by remember { mutableStateOf(false) }
    LaunchedEffect(userPoint) {
        if (!inlineUserMoved) {
            mapViewportState.easeTo(
                navCameraOptions(userPoint, targetPoint, distanceMeters, MAP_HEIGHT_DP)
            )
        }
    }
    val navMapState = NavMapState(userPoint, route.points, deviceAzimuth, arrowRotation)

  Box(modifier = Modifier.fillMaxSize()) {
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

        if (!fullscreenState.value) {
            NavMapContent(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MAP_HEIGHT_DP.dp)
                    .clip(RoundedCornerShape(12.dp)),
                mapViewportState = mapViewportState,
                targetPoint = targetPoint,
                state = navMapState,
                targetPinIcon = targetPinIcon,
                userPinIcon = userPinIcon,
                arrowIcon = arrowIcon,
                fullscreen = false,
                distanceMeters = distanceMeters,
                mapHeightDp = MAP_HEIGHT_DP,
                userMoved = inlineUserMoved,
                onToggleFullscreen = { fullscreenState.value = true },
                onUserGesture = { inlineUserMoved = true },
                onRestoreAutoView = { inlineUserMoved = false },
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (displayDistanceMeters != null) {
            val distanceText = if (displayDistanceMeters >= 1000) {
                String.format(LocalLocale.current.platformLocale, "%.1f km", displayDistanceMeters / 1000f)
            } else {
                "$displayDistanceMeters m"
            }
            Text(
                text = distanceText,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = TextOnDark
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
                disabledContainerColor = DisabledButton,
                disabledContentColor = PrimaryButtonText.copy(alpha = 0.6f)
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

    if (fullscreenState.value) {
        BackHandler { fullscreenState.value = false }
        // Own viewport, initialised straight to the fullscreen-height camera, so the expanded
        // map opens already zoomed to the route instead of inheriting the inline zoom and only
        // correcting on the next location update.
        val fullscreenViewport = rememberMapViewportState {
            setCameraOptions(
                navCameraOptions(userPoint, targetPoint, distanceMeters, screenHeightDp)
            )
        }
        var fullscreenUserMoved by remember { mutableStateOf(false) }
        LaunchedEffect(userPoint) {
            if (!fullscreenUserMoved) {
                fullscreenViewport.easeTo(
                    navCameraOptions(userPoint, targetPoint, distanceMeters, screenHeightDp)
                )
            }
        }
        NavMapContent(
            modifier = Modifier.fillMaxSize(),
            mapViewportState = fullscreenViewport,
            targetPoint = targetPoint,
            state = navMapState,
            targetPinIcon = targetPinIcon,
            userPinIcon = userPinIcon,
            arrowIcon = arrowIcon,
            fullscreen = true,
            distanceMeters = distanceMeters,
            mapHeightDp = screenHeightDp,
            userMoved = fullscreenUserMoved,
            onToggleFullscreen = { fullscreenState.value = false },
            onUserGesture = { fullscreenUserMoved = true },
            onRestoreAutoView = { fullscreenUserMoved = false },
        )
    }
  }
}

/** Ako blízko k cieľu musí hráč prísť, aby mohol pokračovať. */
private const val ARRIVAL_RADIUS_METERS = 10

/**
 * Kým je obrazovka s navigáciou na displeji, telefón nezhasne — hráč počas chôdze nemusí telefón
 * stále odomykať. Po odchode z obrazovky sa správa ako zvyčajne.
 */
@Composable
private fun KeepScreenOnWhileVisible() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

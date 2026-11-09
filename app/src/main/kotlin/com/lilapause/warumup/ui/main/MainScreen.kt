package com.lilapause.warumup.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Church
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HotTub
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SportsHandball
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import com.lilapause.warumup.data.AppLanguage
import com.lilapause.warumup.data.BoundingBoxCoords
import com.lilapause.warumup.data.RouteInfo
import com.lilapause.warumup.data.SpotCategory
import com.lilapause.warumup.data.TravelMode
import com.lilapause.warumup.data.WarmSpot
import java.io.File
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@Composable
fun MainScreen(
  onItemClick: (NavKey) -> Unit = {},
  viewModel: MainScreenViewModel = viewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  WarmUpMapScreen(uiState = uiState, viewModel = viewModel)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarmUpMapScreen(
  uiState: WarmUpUiState,
  viewModel: MainScreenViewModel,
) {
  val context = LocalContext.current
  val focusManager = LocalFocusManager.current
  val isEn = uiState.language == AppLanguage.EN

  var mapViewRef by remember { mutableStateOf<MapView?>(null) }
  var hasCenteredInitially by remember { mutableStateOf(false) }

  fun startLocationDiscovery(ctx: Context) {
    val hasFine = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val hasCoarse = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    if (!hasFine && !hasCoarse) return

    val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
    @SuppressLint("MissingPermission")
    val lastKnown = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
      ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

    if (lastKnown != null) {
      viewModel.onUserLocationDiscovered(lastKnown.latitude, lastKnown.longitude)
      mapViewRef?.controller?.animateTo(GeoPoint(lastKnown.latitude, lastKnown.longitude), 16.0, 800L)
    }

    val singleListener = object : android.location.LocationListener {
      override fun onLocationChanged(loc: Location) {
        viewModel.onUserLocationDiscovered(loc.latitude, loc.longitude)
        mapViewRef?.controller?.animateTo(GeoPoint(loc.latitude, loc.longitude), 16.0, 800L)
        try {
          lm.removeUpdates(this)
        } catch (_: Exception) {}
      }
      @Deprecated("Deprecated in Java")
      override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
      override fun onProviderEnabled(provider: String) {}
      override fun onProviderDisabled(provider: String) {}
    }

    try {
      if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 5f, singleListener)
      } else if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
        lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000L, 5f, singleListener)
      }
    } catch (_: Exception) {}
  }

  // Location Permission Launcher
  val locationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions(),
  ) { permissions ->
    val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    if (granted) {
      startLocationDiscovery(context)
    }
  }

  // Zoom & fit map camera to active route when route is focused
  LaunchedEffect(uiState.fitRouteOnMapTrigger) {
    if (uiState.fitRouteOnMapTrigger > 0L) {
      val route = uiState.activeRoute
      if (route != null && route.polylinePoints.isNotEmpty()) {
        val minLat = route.polylinePoints.minOf { it.first }
        val maxLat = route.polylinePoints.maxOf { it.first }
        val minLon = route.polylinePoints.minOf { it.second }
        val maxLon = route.polylinePoints.maxOf { it.second }
        val padLat = (maxLat - minLat).coerceAtLeast(0.005) * 0.25
        val padLon = (maxLon - minLon).coerceAtLeast(0.005) * 0.25
        val routeBox = BoundingBox(
          maxLat + padLat,
          maxLon + padLon,
          minLat - padLat,
          minLon - padLon,
        )
        mapViewRef?.zoomToBoundingBox(routeBox, true, 80)
      }
    }
  }

  // Update map polyline instantly whenever the active route updates (e.g. mode change or stop add/remove)
  LaunchedEffect(uiState.activeRoute) {
    mapViewRef?.let { mv ->
      updateMapOverlays(
        mapView = mv,
        uiState = uiState,
        onSpotClicked = { spot ->
          viewModel.selectSpot(spot)
          mv.controller.animateTo(GeoPoint(spot.lat, spot.lon), 16.5, 600L)
        },
        onClusterClicked = { _, centerPoint ->
          val nextZoom = (mv.zoomLevelDouble + 1.8).coerceAtMost(18.5)
          mv.controller.animateTo(centerPoint, nextZoom, 500L)
        },
        onRouteClicked = {
          viewModel.focusActiveRoute()
        },
      )
      mv.postInvalidate()
    }
  }

  // Initial startup logic
  LaunchedEffect(Unit) {
    Configuration.getInstance().userAgentValue = "WarmUp-WinterPlaces/1.0"
    Configuration.getInstance().osmdroidBasePath = File(context.cacheDir, "osmdroid")
    Configuration.getInstance().osmdroidTileCache = File(context.cacheDir, "osmdroid/tiles")

    val hasPerm = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED ||
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION,
      ) == PackageManager.PERMISSION_GRANTED

    val gpsPair = if (hasPerm) {
      val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
      @SuppressLint("MissingPermission")
      val loc = lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        ?: lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
      loc?.let {
        viewModel.updateUserLocation(it.latitude, it.longitude)
        it.latitude to it.longitude
      }
    } else {
      locationPermissionLauncher.launch(
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
      )
      null
    }

    viewModel.initializeWithCacheOrFetch(context, gpsPair)
    if (hasPerm) {
      startLocationDiscovery(context)
    }
  }

  // Interstitial Ad Manager
  val interstitialManager = remember {
    val mgr = InterstitialAdManager(context)
    mgr.loadAd(uiState.personalizedAdsConsent)
    mgr
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Map View Container
      Box(modifier = Modifier.weight(1f)) {
        AndroidView(
          modifier = Modifier.fillMaxSize(),
          factory = { ctx ->
            MapView(ctx).apply {
              setMultiTouchControls(true)
              zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
              setTileSource(uiState.selectedMapStyle.tileSource)
              controller.setZoom(uiState.mapZoom)
              controller.setCenter(GeoPoint(uiState.mapCenterLat, uiState.mapCenterLon))

              addMapListener(object : MapListener {
                override fun onScroll(event: ScrollEvent?): Boolean {
                  val center = mapCenter
                  viewModel.onMapMoved(center.latitude, center.longitude, zoomLevelDouble)
                  return false
                }

                override fun onZoom(event: ZoomEvent?): Boolean {
                  val center = mapCenter
                  viewModel.onMapMoved(center.latitude, center.longitude, zoomLevelDouble)
                  return false
                }
              })

              overlays.add(
                MapEventsOverlay(object : MapEventsReceiver {
                  override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                    focusManager.clearFocus()
                    val currentRoute = viewModel.uiState.value.activeRoute
                    if (p != null && currentRoute != null && currentRoute.polylinePoints.isNotEmpty()) {
                      val isNearPolyline = isNearRoute(p.latitude, p.longitude, currentRoute.polylinePoints, 50.0)
                      if (isNearPolyline) {
                        viewModel.focusActiveRoute()
                        return true
                      }
                    }
                    viewModel.onMapTapped()
                    return false
                  }
                  override fun longPressHelper(p: GeoPoint?): Boolean = false
                })
              )

              // Add Location Overlay for blue dot tracking & instant first-fix pickup
              try {
                val locOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this).apply {
                  enableMyLocation()
                  runOnFirstFix {
                    val loc = myLocation
                    if (loc != null) {
                      post {
                        viewModel.onUserLocationDiscovered(loc.latitude, loc.longitude)
                        controller.animateTo(loc, 16.0, 800L)
                      }
                    }
                  }
                }
                overlays.add(locOverlay)
              } catch (_: Exception) {}

              mapViewRef = this
            }
          },
          update = { mv ->
            if (mv.tileProvider.tileSource.name() != uiState.selectedMapStyle.tileSource.name()) {
              mv.setTileSource(uiState.selectedMapStyle.tileSource)
            }

            if (!hasCenteredInitially && uiState.allSpots.isNotEmpty()) {
              mv.controller.setCenter(GeoPoint(uiState.mapCenterLat, uiState.mapCenterLon))
              mv.controller.setZoom(uiState.mapZoom)
              hasCenteredInitially = true
            }

            // Update Markers, Route Polyline, and User Location Pin
            updateMapOverlays(
              mapView = mv,
              uiState = uiState,
              onSpotClicked = { spot ->
                viewModel.selectSpot(spot)
                mv.controller.animateTo(GeoPoint(spot.lat, spot.lon), 16.5, 600L)
              },
              onClusterClicked = { _, centerPoint ->
                val nextZoom = (mv.zoomLevelDouble + 1.8).coerceAtMost(18.5)
                mv.controller.animateTo(centerPoint, nextZoom, 500L)
              },
              onRouteClicked = {
                viewModel.focusActiveRoute()
              },
            )
          },
        )

        // Top Taskbar Overlay: Search Bar, Info, Layers, Coffee, Flag Toggle (Taskbar always visible, chips hide in fullscreen)
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .align(Alignment.TopCenter),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Taskbar Search & Actions Row
            Surface(
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(18.dp),
              color = MaterialTheme.colorScheme.surface,
              tonalElevation = 6.dp,
              shadowElevation = 5.dp,
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  imageVector = Icons.Default.Search,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.padding(start = 6.dp, end = 4.dp),
                )

                TextField(
                  value = uiState.searchQuery,
                  onValueChange = { viewModel.onSearchQueryChanged(it) },
                  placeholder = {
                    Text(
                      if (isEn) "Search city, café, sauna..." else "Ort, Café, Therme suchen...",
                      fontSize = 13.5.sp,
                    )
                  },
                  modifier = Modifier.weight(1f),
                  singleLine = true,
                  colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                  ),
                  keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                  keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                )

                if (uiState.searchQuery.isNotEmpty()) {
                  IconButton(
                    onClick = { viewModel.clearSearch() },
                    modifier = Modifier.size(32.dp),
                  ) {
                    Icon(Icons.Default.Clear, contentDescription = "Löschen", modifier = Modifier.size(18.dp))
                  }
                }

                // 1. Info / Legende Button directly in Taskbar
                IconButton(
                  onClick = { viewModel.setLegendDialogVisible(true) },
                  modifier = Modifier.size(36.dp),
                ) {
                  Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Info & Legende",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                  )
                }

                // 2. Map Layers Button
                IconButton(
                  onClick = { viewModel.setMapStyleDialogVisible(true) },
                  modifier = Modifier.size(36.dp),
                ) {
                  Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = "Kartenstil",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                  )
                }

                // 3. Hot Drink / Support Button
                IconButton(
                  onClick = { viewModel.setHotDrinkDialogVisible(true) },
                  modifier = Modifier.size(36.dp),
                ) {
                  Icon(
                    imageVector = Icons.Default.Coffee,
                    contentDescription = "Heißgetränk",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                  )
                }

                // 4. Language Flag Button: ALWAYS SHOW THE INACTIVE FLAG
                // When DE is active -> show 🇬🇧 (so user clicks to switch to English)
                // When EN is active -> show 🇩🇪 (so user clicks to switch to German)
                val flagEmoji = if (uiState.language == AppLanguage.DE) "🇬🇧" else "🇩🇪"
                Box(
                  modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .clickable { viewModel.toggleLanguage() }
                    .padding(2.dp),
                  contentAlignment = Alignment.Center,
                ) {
                  Text(
                    text = flagEmoji,
                    fontSize = 18.sp,
                  )
                }
              }
            }

            // Search Autocomplete Dropdown
            if (uiState.searchResults.isNotEmpty()) {
              Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 6.dp,
              ) {
                LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                  items(uiState.searchResults) { item ->
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                          focusManager.clearFocus()
                          viewModel.selectSearchResult(item)
                          val lat = item.lat.toDoubleOrNull()
                          val lon = item.lon.toDoubleOrNull()
                          if (lat != null && lon != null) {
                            mapViewRef?.controller?.animateTo(GeoPoint(lat, lon), 15.5, 800L)
                          }
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                      Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                      Column {
                        Text(item.name ?: item.display_name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                          item.display_name,
                          fontSize = 11.sp,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis,
                        )
                      }
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 10.dp))
                  }
                }
              }
            }

            // Category Filter Chips (Hidden in Fullscreen Map view)
            androidx.compose.animation.AnimatedVisibility(
              visible = !uiState.isFullScreenMap,
              enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
              exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            ) {
              Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                tonalElevation = 3.dp,
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  FilterChip(
                    selected = uiState.isAllCategoriesSelected,
                    onClick = {
                      if (uiState.isAllCategoriesSelected) {
                        viewModel.clearAllCategories()
                      } else {
                        viewModel.selectAllCategories()
                      }
                    },
                    label = {
                      Text(
                        text = if (uiState.isAllCategoriesSelected) {
                          if (isEn) "All (${uiState.allSpots.size})" else "Alle (${uiState.allSpots.size})"
                        } else {
                          if (isEn) "Select all" else "Alle wählen"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                      )
                    },
                    leadingIcon = if (uiState.isAllCategoriesSelected) {
                      { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                      selectedContainerColor = MaterialTheme.colorScheme.primary,
                      selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                  )

                  SpotCategory.entries.forEach { cat ->
                    val count = uiState.categoryCounts[cat] ?: 0
                    val isSelected = cat in uiState.activeCategories
                    FilterChip(
                      selected = isSelected,
                      onClick = { viewModel.toggleCategory(cat) },
                      label = {
                        Text(
                          text = "${cat.title(uiState.language)} ($count)",
                          fontSize = 11.5.sp,
                        )
                      },
                      leadingIcon = {
                        Icon(
                          imageVector = cat.iconVector(),
                          contentDescription = null,
                          modifier = Modifier.size(15.dp),
                          tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else Color(cat.colorHex),
                        )
                      },
                      colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                      ),
                    )
                  }
                }
              }
            }
          }

        // Center "In diesem Bereich suchen" Button (Moved down so it does not overlap category chips)
        androidx.compose.animation.AnimatedVisibility(
          visible = !uiState.isFullScreenMap && uiState.hasMovedMapSinceFetch && !uiState.isLoading,
          enter = fadeIn(),
          exit = fadeOut(),
          modifier = Modifier
            .align(Alignment.TopCenter)
            .statusBarsPadding()
            .padding(top = 138.dp),
        ) {
          ElevatedButton(
            onClick = {
              mapViewRef?.let { mv ->
                triggerSearchFromMap(mv, viewModel)
              }
            },
            colors = ButtonDefaults.elevatedButtonColors(
              containerColor = Color(0xFFC05621),
              contentColor = Color.White,
            ),
            shape = RoundedCornerShape(20.dp),
            elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 6.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 7.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
              Text(
                if (isEn) "Search this area" else "In diesem Bereich suchen",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
              )
            }
          }
        }

        // Cancel Active Search Button (Allows stopping in-flight search to choose another area)
        androidx.compose.animation.AnimatedVisibility(
          visible = !uiState.isFullScreenMap && uiState.isLoading,
          enter = fadeIn(),
          exit = fadeOut(),
          modifier = Modifier
            .align(Alignment.TopCenter)
            .statusBarsPadding()
            .padding(top = 138.dp),
        ) {
          ElevatedButton(
            onClick = { viewModel.cancelSearch() },
            colors = ButtonDefaults.elevatedButtonColors(
              containerColor = Color(0xFF4A1E0D),
              contentColor = Color(0xFFFFDBC8),
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC05621).copy(alpha = 0.6f)),
            shape = RoundedCornerShape(20.dp),
            elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 6.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 7.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              CircularProgressIndicator(
                modifier = Modifier.size(15.dp),
                strokeWidth = 2.dp,
                color = Color(0xFFFFDBC8),
              )
              Text(
                if (isEn) "Cancel search" else "Suche abbrechen",
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFDBC8),
              )
            }
          }
        }

        // Route Calculation Status Pill (Visible whenever a route is actively being calculated)
        androidx.compose.animation.AnimatedVisibility(
          visible = uiState.isCalculatingRoute && !uiState.showRouteOverviewSheet,
          enter = fadeIn(),
          exit = fadeOut(),
          modifier = Modifier
            .align(Alignment.TopCenter)
            .statusBarsPadding()
            .padding(top = 138.dp),
        ) {
          Surface(
            color = Color(0xFF4A1E0D),
            contentColor = Color(0xFFFFDBC8),
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 6.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC05621).copy(alpha = 0.6f)),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              CircularProgressIndicator(
                modifier = Modifier.size(15.dp),
                strokeWidth = 2.dp,
                color = Color(0xFFFFDBC8),
              )
              Text(
                text = if (isEn) {
                  "Calculating shortest route (${uiState.selectedTravelMode.title(uiState.language)})…"
                } else {
                  "Kürzeste Route (${uiState.selectedTravelMode.title(uiState.language)}) wird berechnet…"
                },
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFDBC8),
              )
            }
          }
        }

        // Loading Bar
        if (uiState.isLoading || uiState.isCalculatingRoute) {
          LinearProgressIndicator(
            modifier = Modifier
              .fillMaxWidth()
              .align(Alignment.TopCenter),
            color = MaterialTheme.colorScheme.primary,
          )
        }

        // 4. "Orte" & "Route" Buttons (Bottom Left / schmale Pills direkt am unteren Rand)
        androidx.compose.animation.AnimatedVisibility(
          visible = uiState.selectedSpot == null,
          enter = fadeIn() + slideInVertically { it / 2 },
          exit = fadeOut() + slideOutVertically { it / 2 },
          modifier = Modifier
            .align(Alignment.BottomStart)
            .then(
              if (uiState.isFullScreenMap) Modifier.navigationBarsPadding().padding(start = 12.dp, bottom = 8.dp)
              else Modifier.padding(start = 12.dp, bottom = 8.dp)
            ),
        ) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            // Orte Pill Button
            Surface(
              onClick = { viewModel.setSpotListSheetVisible(true) },
              shape = RoundedCornerShape(20.dp),
              color = MaterialTheme.colorScheme.surface,
              contentColor = MaterialTheme.colorScheme.primary,
              shadowElevation = 3.dp,
              tonalElevation = 2.dp,
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Icon(
                  imageVector = Icons.Default.FilterList,
                  contentDescription = null,
                  modifier = Modifier.size(17.dp),
                )
                Text(
                  text = "${uiState.filteredSpots.size} " + if (isEn) "Spots" else "Orte",
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = FontWeight.Bold,
                )
              }
            }

            // Route Pill Button (direkt neben Orte)
            if (uiState.activeRoute != null) {
              Surface(
                onClick = { viewModel.focusActiveRoute() },
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shadowElevation = 3.dp,
                tonalElevation = 2.dp,
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Icon(
                    imageVector = Icons.Default.Route,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                  )
                  Text(
                    text = if (isEn) "Route" else "Route",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                  )
                }
              }
            }
          }
        }

        // 5. "Vollansicht" & "GPS" Buttons (Bottom Right / rechts unten)
        Column(
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .then(
              if (uiState.isFullScreenMap) Modifier.navigationBarsPadding().padding(end = 12.dp, bottom = if (uiState.selectedSpot != null) 210.dp else 8.dp)
              else Modifier.padding(end = 12.dp, bottom = if (uiState.selectedSpot != null) 210.dp else 8.dp)
            ),
          verticalArrangement = Arrangement.spacedBy(8.dp),
          horizontalAlignment = Alignment.End,
        ) {
          // Vollansicht (Fullscreen toggle)
          SmallFloatingActionButton(
            onClick = { viewModel.toggleFullScreenMap() },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
            modifier = Modifier.size(38.dp),
          ) {
            Icon(
              imageVector = if (uiState.isFullScreenMap) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
              contentDescription = "Vollansicht umschalten",
              modifier = Modifier.size(19.dp),
            )
          }

          // GPS / Mein Standort
          SmallFloatingActionButton(
            onClick = {
              val uLat = uiState.userLat
              val uLon = uiState.userLon
              if (uLat != null && uLon != null) {
                mapViewRef?.controller?.animateTo(GeoPoint(uLat, uLon), 16.5, 700L)
              } else {
                locationPermissionLauncher.launch(
                  arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                )
              }
            },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
            modifier = Modifier.size(42.dp),
          ) {
            Icon(Icons.Default.MyLocation, contentDescription = "Mein Standort", modifier = Modifier.size(20.dp))
          }
        }

        // Bottom Selected Spot Card
        androidx.compose.animation.AnimatedVisibility(
          visible = uiState.selectedSpot != null,
          enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
          exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
          modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .then(
              if (uiState.isFullScreenMap) Modifier.navigationBarsPadding().padding(horizontal = 10.dp, vertical = 6.dp)
              else Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            ),
        ) {
          uiState.selectedSpot?.let { spot ->
            WarmSpotDetailCard(
              spot = spot,
              userLat = uiState.userLat,
              userLon = uiState.userLon,
              language = uiState.language,
              selectedTravelMode = uiState.selectedTravelMode,
              activeRoute = uiState.activeRoute,
              onClose = { viewModel.selectSpot(null) },
              onCalculateRoute = { mode ->
                interstitialManager.notifyActionAndShowIfReady(
                  activity = context as android.app.Activity,
                  personalizedAds = uiState.personalizedAdsConsent,
                  isAdFree = uiState.isAdFree,
                ) {
                  viewModel.calculateRouteTo(spot, mode)
                }
              },
              onAddStopToRoute = {
                viewModel.addStopToRoute(spot)
              },
            )
          }
        }
      }

      // Bottom AdBanner
      if (!uiState.isFullScreenMap) {
        AdBannerView(
          isAdFree = uiState.isAdFree,
          personalizedAds = uiState.personalizedAdsConsent,
          language = uiState.language,
          onAdFreeClick = { viewModel.setHotDrinkDialogVisible(true) },
          onPrivacyClick = { viewModel.setLegalPrivacyDialogVisible(true) },
        )
      }
    }

    // Consent Banner
    if (!uiState.hasSeenConsentBanner) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color.Black.copy(alpha = 0.5f))
          .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter,
      ) {
        GdprConsentBanner(
          lang = uiState.language,
          onToggleLanguage = { viewModel.toggleLanguage() },
          onAcceptPersonalized = { viewModel.setPersonalizedAdsConsent(true) },
          onAcceptNonPersonalized = { viewModel.setPersonalizedAdsConsent(false) },
          onOpenPrivacyDetails = { viewModel.setLegalPrivacyDialogVisible(true) },
        )
      }
    }

    // Fancy Onboarding Dialog
    if (uiState.showOnboardingDialog) {
      OnboardingIntroDialog(
        lang = uiState.language,
        onDismiss = { viewModel.dismissOnboarding() },
      )
    }

    // Hot Drink Support Dialog
    if (uiState.showHotDrinkDialog) {
      HotDrinkSupportDialog(
        lang = uiState.language,
        isAdFree = uiState.isAdFree,
        onAdFreeUnlocked = { viewModel.setAdFree(true) },
        onDismiss = { viewModel.setHotDrinkDialogVisible(false) },
      )
    }

    // Legal Privacy Dialog
    if (uiState.showLegalPrivacyDialog) {
      LegalPrivacyDialog(
        lang = uiState.language,
        personalizedConsent = uiState.personalizedAdsConsent,
        onConsentChanged = { viewModel.setPersonalizedAdsConsent(it) },
        onDismiss = { viewModel.setLegalPrivacyDialogVisible(false) },
      )
    }

    // Map Style Dialog
    if (uiState.showMapStyleDialog) {
      MapStyleSelectionDialog(
        current = uiState.selectedMapStyle,
        lang = uiState.language,
        onSelect = { viewModel.setMapStyle(it) },
        onDismiss = { viewModel.setMapStyleDialogVisible(false) },
      )
    }

    // Category Legend Dialog
    if (uiState.showLegendDialog) {
      CategoryLegendDialog(
        lang = uiState.language,
        onOpenLegalPrivacy = {
          viewModel.setLegendDialogVisible(false)
          viewModel.setLegalPrivacyDialogVisible(true)
        },
        onDismiss = { viewModel.setLegendDialogVisible(false) },
      )
    }

    // Route Overview Sheet (Bottom Sheet Layout)
    val currentRoute = uiState.activeRoute
    if (uiState.showRouteOverviewSheet && currentRoute != null) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(Color.Black.copy(alpha = 0.45f))
          .clickable { viewModel.setRouteOverviewSheetVisible(false) },
        contentAlignment = Alignment.BottomCenter,
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = false) {}
            .then(if (uiState.isFullScreenMap) Modifier.navigationBarsPadding() else Modifier)
            .padding(bottom = 6.dp)
        ) {
          RouteOverviewSheet(
            route = currentRoute,
            selectedTravelMode = uiState.selectedTravelMode,
            isCalculatingRoute = uiState.isCalculatingRoute,
            language = uiState.language,
            onTravelModeChange = { mode ->
              viewModel.changeTravelMode(mode)
            },
            onRemoveStop = { spotId -> viewModel.removeStopFromRoute(spotId) },
            onClearRoute = { viewModel.clearRoute() },
            onFocusRoute = { viewModel.focusActiveRoute() },
            onSpotClick = { spot ->
              viewModel.selectSpot(spot)
              mapViewRef?.controller?.animateTo(GeoPoint(spot.lat, spot.lon), 16.5, 600L)
            },
            onClose = { viewModel.setRouteOverviewSheetVisible(false) },
          )
        }
      }
    }

    // Spot List Bottom Sheet
    if (uiState.showSpotListSheet) {
      SpotListBottomSheet(
        spots = uiState.filteredSpots,
        userLat = uiState.userLat ?: uiState.mapCenterLat,
        userLon = uiState.userLon ?: uiState.mapCenterLon,
        language = uiState.language,
        onSpotClick = { spot ->
          viewModel.selectSpot(spot)
          viewModel.setSpotListSheetVisible(false)
          mapViewRef?.controller?.animateTo(GeoPoint(spot.lat, spot.lon), 16.5, 600L)
        },
        onClose = { viewModel.setSpotListSheetVisible(false) },
      )
    }
  }
}

@Composable
private fun WarmSpotDetailCard(
  spot: WarmSpot,
  userLat: Double?,
  userLon: Double?,
  language: AppLanguage,
  selectedTravelMode: TravelMode,
  activeRoute: RouteInfo?,
  onClose: () -> Unit,
  onCalculateRoute: (TravelMode) -> Unit,
  onAddStopToRoute: () -> Unit,
) {
  val context = LocalContext.current
  val isEn = language == AppLanguage.EN
  val distText = if (userLat != null && userLon != null) spot.formattedDistanceTo(userLat, userLon) else null

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Surface(
            shape = CircleShape,
            color = Color(spot.category.colorHex).copy(alpha = 0.2f),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Icon(
                imageVector = spot.category.iconVector(),
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = Color(spot.category.colorHex),
              )
              Text(
                text = spot.category.title(language),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(spot.category.colorHex),
              )
            }
          }

          if (distText != null) {
            Text(
              text = "• $distText",
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }

        IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
          Icon(Icons.Default.Close, contentDescription = "Schließen", modifier = Modifier.size(18.dp))
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = spot.name,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        if (!spot.address.isNullOrBlank()) {
          Text(
            text = spot.address,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        if (spot.hasFireplace) {
          FeatureBadge(text = if (isEn) "🔥 Fireplace" else "🔥 Kaminfeuer", color = Color(0xFFD97706))
        }
        if (spot.wheelchair == "yes") {
          FeatureBadge(text = if (isEn) "♿ Wheelchair" else "♿ Barrierefrei", color = Color(0xFF0284C7))
        }
        if (!spot.openingHours.isNullOrBlank()) {
          FeatureBadge(text = "🕒 ${spot.openingHours}", color = Color(0xFF7C3AED))
        }
      }

      if (!spot.description.isNullOrBlank()) {
        Text(
          text = spot.description,
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          lineHeight = 16.sp,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Button(
          onClick = { onCalculateRoute(selectedTravelMode) },
          modifier = Modifier.weight(1f),
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
          Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(if (isEn) "Route" else "Route", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        if (activeRoute != null && !activeRoute.containsSpot(spot.id)) {
          OutlinedButton(
            onClick = onAddStopToRoute,
            modifier = Modifier.weight(1f),
          ) {
            Text(if (isEn) "+ Stop" else "+ Stopp", fontSize = 12.sp)
          }
        }

        OutlinedButton(
          onClick = {
            val uri = Uri.parse("geo:${spot.lat},${spot.lon}?q=${spot.lat},${spot.lon}(${Uri.encode(spot.name)})")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(intent)
          },
        ) {
          Text("Maps", fontSize = 12.sp)
        }
      }
    }
  }
}

@Composable
private fun FeatureBadge(text: String, color: Color) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = color.copy(alpha = 0.12f),
  ) {
    Text(
      text = text,
      fontSize = 11.sp,
      fontWeight = FontWeight.Medium,
      color = color,
      modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
    )
  }
}

@Composable
private fun RouteOverviewSheet(
  route: RouteInfo,
  selectedTravelMode: TravelMode,
  isCalculatingRoute: Boolean,
  language: AppLanguage,
  onTravelModeChange: (TravelMode) -> Unit,
  onRemoveStop: (Long) -> Unit,
  onClearRoute: () -> Unit,
  onFocusRoute: () -> Unit,
  onSpotClick: (WarmSpot) -> Unit,
  onClose: () -> Unit,
) {
  val context = LocalContext.current
  val isEn = language == AppLanguage.EN

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .padding(12.dp),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onFocusRoute() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(Icons.Default.Route, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Text(
            text = if (route.isMultiStop) {
              if (isEn) "Multi-Stop Route (${route.stopCount} stops)" else "Route mit ${route.stopCount} Stopps"
            } else {
              if (isEn) "Route into Warmth" else "Weg ins Warme"
            },
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
          )
        }
        IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
          Icon(Icons.Default.Close, contentDescription = "Schließen", modifier = Modifier.size(18.dp))
        }
      }

      if (isCalculatingRoute) {
        Surface(
          modifier = Modifier.fillMaxWidth(),
          color = Color(0xFF4A1E0D),
          contentColor = Color(0xFFFFDBC8),
          shape = RoundedCornerShape(10.dp),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            CircularProgressIndicator(
              modifier = Modifier.size(15.dp),
              strokeWidth = 2.dp,
              color = Color(0xFFFFDBC8),
            )
            Text(
              text = if (isEn) {
                "Calculating shortest route (${selectedTravelMode.title(language)})…"
              } else {
                "Kürzeste Route (${selectedTravelMode.title(language)}) wird berechnet…"
              },
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFFFFDBC8),
            )
          }
        }
      }

      // Row 1: Duration & Distance summary (full width, clickable to center route)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .clickable { onFocusRoute() }
          .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = route.formattedDuration(language),
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = "${route.formattedDistance} (${route.travelMode.title(language)}) • " +
              if (isEn) "Tap to focus route on map" else "Tippen zum Zentrieren auf der Karte",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      // Row 2: Travel mode selection (Walking, Bike, Car) in its own line with plenty of space
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        TravelMode.entries.forEach { mode ->
          val isSelected = mode == selectedTravelMode
          Surface(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(12.dp))
              .clickable { onTravelModeChange(mode) },
            shape = RoundedCornerShape(12.dp),
            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
          ) {
            Row(
              modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center,
            ) {
              Icon(
                imageVector = when (mode) {
                  TravelMode.WALKING -> Icons.AutoMirrored.Filled.DirectionsWalk
                  TravelMode.BICYCLING -> Icons.AutoMirrored.Filled.DirectionsBike
                  TravelMode.DRIVING -> Icons.Default.DirectionsCar
                },
                contentDescription = mode.title(language),
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = mode.title(language),
                fontSize = 11.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }

      HorizontalDivider()

      LazyColumn(modifier = Modifier.heightIn(max = 180.dp)) {
        itemsIndexed(route.targetSpots) { idx, spot ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .clickable { onSpotClick(spot) }
              .padding(vertical = 4.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.weight(1f),
            ) {
              Box(
                modifier = Modifier
                  .size(22.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
              ) {
                Text("${idx + 1}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
              }
              Column {
                Text(spot.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(spot.category.title(language), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }

            IconButton(
              onClick = { onRemoveStop(spot.id) },
              modifier = Modifier.size(28.dp),
            ) {
              Icon(Icons.Default.Delete, contentDescription = "Löschen", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
            }
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedButton(
          onClick = onClearRoute,
          modifier = Modifier.weight(1f),
        ) {
          Text(if (isEn) "Clear Route" else "Route beenden", fontSize = 12.sp)
        }

        Button(
          onClick = {
            val last = route.targetSpots.lastOrNull() ?: return@Button
            val originStr = "${route.originLat},${route.originLon}"
            val destStr = "${last.lat},${last.lon}"
            val intermediate = route.targetSpots.dropLast(1)
            val gmapsMode = when (route.travelMode) {
              TravelMode.WALKING -> "walking"
              TravelMode.BICYCLING -> "bicycling"
              TravelMode.DRIVING -> "driving"
            }
            val url = if (intermediate.isNotEmpty()) {
              val waypointsStr = intermediate.joinToString("|") { "${it.lat},${it.lon}" }
              "https://www.google.com/maps/dir/?api=1&origin=$originStr&destination=$destStr&waypoints=$waypointsStr&travelmode=$gmapsMode"
            } else {
              "https://www.google.com/maps/dir/?api=1&origin=$originStr&destination=$destStr&travelmode=$gmapsMode"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
              setPackage("com.google.android.apps.maps")
            }
            try {
              context.startActivity(intent)
            } catch (_: Exception) {
              val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
              context.startActivity(Intent.createChooser(fallbackIntent, if (isEn) "Start Navigation" else "Navigation starten"))
            }
          },
          modifier = Modifier.weight(1f),
        ) {
          Text(if (isEn) "Start Nav" else "Navi starten", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpotListBottomSheet(
  spots: List<WarmSpot>,
  userLat: Double,
  userLon: Double,
  language: AppLanguage,
  onSpotClick: (WarmSpot) -> Unit,
  onClose: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState()
  val isEn = language == AppLanguage.EN

  ModalBottomSheet(
    onDismissRequest = onClose,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = if (isEn) "Warm Winter Spots (${spots.size})" else "Warme Winterorte (${spots.size})",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
        TextButton(onClick = onClose) {
          Text(if (isEn) "Close" else "Schließen")
        }
      }

      HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

      LazyColumn(
        modifier = Modifier
          .fillMaxHeight(0.75f)
          .navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = 80.dp),
      ) {
        items(spots) { spot ->
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp)
              .clickable { onSpotClick(spot) },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(Color(spot.category.colorHex).copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = spot.category.iconVector(),
                  contentDescription = null,
                  tint = Color(spot.category.colorHex),
                  modifier = Modifier.size(18.dp),
                )
              }

              Column(modifier = Modifier.weight(1f)) {
                Text(spot.name, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                  spot.category.title(language),
                  fontSize = 11.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }

              Text(
                text = spot.formattedDistanceTo(userLat, userLon),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun CategoryLegendDialog(
  lang: AppLanguage,
  onOpenLegalPrivacy: () -> Unit = {},
  onDismiss: () -> Unit,
) {
  val isEn = lang == AppLanguage.EN
  val scrollState = rememberScrollState()

  AlertDialog(
    onDismissRequest = onDismiss,
    confirmButton = {
      Button(onClick = onDismiss) { Text(if (isEn) "Understood" else "Verstanden") }
    },
    dismissButton = {
      TextButton(onClick = onOpenLegalPrivacy) {
        Text(if (isEn) "Privacy & Imprint" else "Datenschutz & Impressum")
      }
    },
    title = {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(if (isEn) "Info & Categories" else "Info & Winter-Kategorien", fontWeight = FontWeight.Bold)
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = if (isEn) "Cozy Warm Categories:" else "Gemütliche Winter-Kategorien:",
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp,
          color = MaterialTheme.colorScheme.primary,
        )

        SpotCategory.entries.forEach { cat ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
          ) {
            Box(
              modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color(cat.colorHex).copy(alpha = 0.2f)),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = cat.iconVector(),
                contentDescription = null,
                tint = Color(cat.colorHex),
                modifier = Modifier.size(15.dp),
              )
            }
            Column {
              Text(cat.title(lang), fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
              Text(cat.subtitle(lang), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        Text(
          text = if (isEn) "Data Sources & Licenses:" else "Verwendete Datenquellen & Lizenzen:",
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp,
          color = MaterialTheme.colorScheme.primary,
        )

        Surface(
          shape = RoundedCornerShape(10.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = if (isEn) {
                "• OpenStreetMap (OSM): Geodata & POIs (© OpenStreetMap contributors, ODbL license)\n" +
                  "• Photon by Komoot: High-speed OpenStreetMap POI & Geocoding index\n" +
                  "• OpenStreetMap Routing (OSRM): routing.openstreetmap.de for optimal foot, bicycle & car routing\n" +
                  "• Map Tiles: OpenStreetMap Standard & CartoDB Positron / Dark / Voyager (© CARTO, © OpenStreetMap)"
              } else {
                "• OpenStreetMap (OSM): Geodaten & POIs (© OpenStreetMap-Mitwirkende, ODbL Lizenz)\n" +
                  "• Photon by Komoot: Schneller OpenStreetMap POI- & Geocoding-Index\n" +
                  "• OpenStreetMap Routing (OSRM): routing.openstreetmap.de für optimale Fuß-, Fahrrad- & Autonavigation\n" +
                  "• Kartenkacheln: OpenStreetMap Standard & CartoDB Positron / Dark / Voyager (© CARTO, © OpenStreetMap)"
              },
              fontSize = 11.sp,
              lineHeight = 15.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    },
  )
}

@Composable
private fun MapStyleSelectionDialog(
  current: FreeMapStyle,
  lang: AppLanguage,
  onSelect: (FreeMapStyle) -> Unit,
  onDismiss: () -> Unit,
) {
  val isEn = lang == AppLanguage.EN

  AlertDialog(
    onDismissRequest = onDismiss,
    confirmButton = {
      TextButton(onClick = onDismiss) { Text(if (isEn) "Close" else "Schließen") }
    },
    title = {
      Text(if (isEn) "Select Map Layer" else "Kartenstil wählen", fontWeight = FontWeight.Bold)
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FreeMapStyle.entries.forEach { style ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .clickable { onSelect(style) }
              .padding(horizontal = 6.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            RadioButton(
              selected = current == style,
              onClick = { onSelect(style) },
            )
            Column {
              Text(style.titleDe, fontWeight = FontWeight.Bold, fontSize = 13.sp)
              Text(style.subtitleDe, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
        }
      }
    },
  )
}

fun SpotCategory.iconVector(): ImageVector = when (this) {
  SpotCategory.CAFE_HOT_DRINKS -> Icons.Default.Coffee
  SpotCategory.THERMAL_BATH_SAUNA -> Icons.Default.HotTub
  SpotCategory.INDOOR_POOL -> Icons.Default.Pool
  SpotCategory.FIREPLACE_RESTAURANT -> Icons.Default.LocalFireDepartment
  SpotCategory.CINEMA_THEATER -> Icons.Default.Theaters
  SpotCategory.LIBRARY_READING -> Icons.Default.LocalLibrary
  SpotCategory.MUSEUM_EXHIBITION -> Icons.Default.AccountBalance
  SpotCategory.INDOOR_SHOPPING_PASSAGE -> Icons.Default.ShoppingBag
}

private sealed interface MapRenderItem {
  data class SingleSpot(val spot: WarmSpot) : MapRenderItem
  data class Cluster(
    val centerLat: Double,
    val centerLon: Double,
    val spots: List<WarmSpot>,
    val dominantCategory: SpotCategory,
  ) : MapRenderItem
}

private fun updateMapOverlays(
  mapView: MapView,
  uiState: WarmUpUiState,
  onSpotClicked: (WarmSpot) -> Unit,
  onClusterClicked: (List<WarmSpot>, GeoPoint) -> Unit,
  onRouteClicked: () -> Unit,
) {
  val context = mapView.context
  // Keep base overlays (MyLocationNewOverlay, MapEventsOverlay)
  val baseOverlays = mapView.overlays.filter { it !is Marker && it !is Polyline }
  mapView.overlays.clear()
  mapView.overlays.addAll(baseOverlays)

  // 1. Draw Route Polyline (Clickable to show and focus route)
  val route = uiState.activeRoute
  if (route != null && route.polylinePoints.isNotEmpty()) {
    val poly = Polyline().apply {
      outlinePaint.color = 0xFFDC2626.toInt() // Warm crimson line
      outlinePaint.strokeWidth = 10f
      outlinePaint.strokeCap = Paint.Cap.ROUND
      outlinePaint.strokeJoin = Paint.Join.ROUND
      setPoints(route.polylinePoints.map { GeoPoint(it.first, it.second) })
      setOnClickListener { _, _, _ ->
        onRouteClicked()
        true
      }
    }
    mapView.overlays.add(poly)
  }

  // 2. High-Visibility User Location Marker ("Hier bist du")
  val userLat = uiState.userLat
  val userLon = uiState.userLon
  if (userLat != null && userLon != null) {
    val userMarker = Marker(mapView).apply {
      position = GeoPoint(userLat, userLon)
      icon = createUserLocationDrawable(context)
      setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
      title = if (uiState.language == AppLanguage.EN) "You are here" else "Dein Standort"
    }
    mapView.overlays.add(userMarker)
  }

  // 3. Render Spots & Clusters
  val renderedItems = buildClusteredMapItems(
    spots = uiState.filteredSpots,
    zoom = mapView.zoomLevelDouble,
    selectedId = uiState.selectedSpot?.id,
    routeSpotIds = route?.targetSpots?.map { it.id }?.toSet().orEmpty(),
  )

  for (item in renderedItems) {
    when (item) {
      is MapRenderItem.SingleSpot -> {
        val spot = item.spot
        val isSelected = spot.id == uiState.selectedSpot?.id
        val marker = Marker(mapView).apply {
          position = GeoPoint(spot.lat, spot.lon)
          icon = MarkerIconFactory.getMarkerDrawable(context, spot.category, isSelected)
          setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
          setOnMarkerClickListener { _, _ ->
            onSpotClicked(spot)
            true
          }
        }
        mapView.overlays.add(marker)
      }
      is MapRenderItem.Cluster -> {
        val cluster = item
        val centerPt = GeoPoint(cluster.centerLat, cluster.centerLon)
        val marker = Marker(mapView).apply {
          position = centerPt
          icon = MarkerIconFactory.getClusterDrawable(context, cluster.dominantCategory, cluster.spots.size)
          setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
          setOnMarkerClickListener { _, _ ->
            onClusterClicked(cluster.spots, centerPt)
            true
          }
        }
        mapView.overlays.add(marker)
      }
    }
  }

  mapView.invalidate()
}

/**
 * Creates a clear, glowing user location marker (Blue pulsating dot with white border & glowing aura).
 */
private fun createUserLocationDrawable(context: Context): BitmapDrawable {
  val density = context.resources.displayMetrics.density
  val sizePx = (34 * density).toInt().coerceAtLeast(40)
  val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
  val canvas = Canvas(bitmap)

  val cx = sizePx / 2f
  val cy = sizePx / 2f
  val outerRadius = (sizePx / 2f) - (2f * density)
  val middleRadius = outerRadius - (4f * density)
  val innerRadius = middleRadius - (3f * density)

  // Outer blue glow halo
  val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = android.graphics.Color.argb(80, 2, 132, 199)
    style = Paint.Style.FILL
  }
  canvas.drawCircle(cx, cy, outerRadius, haloPaint)

  // White border
  val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = android.graphics.Color.WHITE
    style = Paint.Style.FILL
  }
  canvas.drawCircle(cx, cy, middleRadius, whitePaint)

  // Inner vivid blue center
  val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = android.graphics.Color.rgb(2, 132, 199)
    style = Paint.Style.FILL
  }
  canvas.drawCircle(cx, cy, innerRadius, centerPaint)

  return BitmapDrawable(context.resources, bitmap)
}

private fun buildClusteredMapItems(
  spots: List<WarmSpot>,
  zoom: Double,
  selectedId: Long?,
  routeSpotIds: Set<Long>,
): List<MapRenderItem> {
  if (spots.isEmpty()) return emptyList()

  if (zoom >= 17.0 || spots.size <= 15) {
    return spots.map { MapRenderItem.SingleSpot(it) }
  }

  val cellDeg = when {
    zoom >= 16.0 -> 0.0018
    zoom >= 15.0 -> 0.0035
    zoom >= 14.0 -> 0.0070
    zoom >= 13.0 -> 0.0150
    else -> 0.0300
  }

  val items = mutableListOf<MapRenderItem>()
  val clusterCandidates = mutableListOf<WarmSpot>()

  for (spot in spots) {
    val isPinned = (spot.id == selectedId) || (spot.id in routeSpotIds)
    if (isPinned) {
      items.add(MapRenderItem.SingleSpot(spot))
    } else {
      clusterCandidates.add(spot)
    }
  }

  val gridGroups = clusterCandidates.groupBy { spot ->
    val latBucket = kotlin.math.floor(spot.lat / cellDeg).toLong()
    val lonBucket = kotlin.math.floor(spot.lon / cellDeg).toLong()
    latBucket to lonBucket
  }

  for ((_, group) in gridGroups) {
    if (group.size == 1) {
      items.add(MapRenderItem.SingleSpot(group.first()))
    } else {
      val avgLat = group.sumOf { it.lat } / group.size
      val avgLon = group.sumOf { it.lon } / group.size
      val dominantCat = group
        .groupingBy { it.category }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key ?: group.first().category
      items.add(
        MapRenderItem.Cluster(
          centerLat = avgLat,
          centerLon = avgLon,
          spots = group,
          dominantCategory = dominantCat,
        )
      )
    }
  }

  return items
}

private fun triggerSearchFromMap(
  mapView: MapView,
  viewModel: MainScreenViewModel,
) {
  val bb = mapView.boundingBox
  val center = mapView.mapCenter
  val rawLatSpan = (bb.latNorth - bb.latSouth).coerceAtMost(0.20)
  val rawLonSpan = (bb.lonEast - bb.lonWest).coerceAtMost(0.25)
  val exactBbox = BoundingBoxCoords(
    south = center.latitude - rawLatSpan / 2.0,
    west = center.longitude - rawLonSpan / 2.0,
    north = center.latitude + rawLatSpan / 2.0,
    east = center.longitude + rawLonSpan / 2.0,
  )
  viewModel.fetchWarmSpotsForBbox(exactBbox, auto = false)
}

/**
 * Checks if a given coordinate is within [maxDistanceMeters] of a route polyline.
 */
private fun isNearRoute(
  tapLat: Double,
  tapLon: Double,
  points: List<Pair<Double, Double>>,
  maxDistanceMeters: Double = 50.0,
): Boolean {
  if (points.isEmpty()) return false
  for (i in 0 until points.size - 1) {
    val p1 = points[i]
    val p2 = points[i + 1]
    val d = distanceToSegmentMeters(tapLat, tapLon, p1.first, p1.second, p2.first, p2.second)
    if (d <= maxDistanceMeters) return true
  }
  return false
}

private fun distanceToSegmentMeters(
  pLat: Double, pLon: Double,
  aLat: Double, aLon: Double,
  bLat: Double, bLon: Double,
): Double {
  val cosLat = kotlin.math.cos(Math.toRadians(pLat))
  val x = (pLon - aLon) * cosLat * 111320.0
  val y = (pLat - aLat) * 110540.0
  val dx = (bLon - aLon) * cosLat * 111320.0
  val dy = (bLat - aLat) * 110540.0

  val segLenSq = dx * dx + dy * dy
  if (segLenSq == 0.0) {
    return kotlin.math.hypot(x, y)
  }
  val t = ((x * dx + y * dy) / segLenSq).coerceIn(0.0, 1.0)
  val projX = t * dx
  val projY = t * dy
  return kotlin.math.hypot(x - projX, y - projY)
}

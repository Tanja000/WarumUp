package com.lilapause.warumup.ui.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lilapause.warumup.data.AppLanguage
import com.lilapause.warumup.data.BoundingBoxCoords
import com.lilapause.warumup.data.NominatimResult
import com.lilapause.warumup.data.OverpassService
import com.lilapause.warumup.data.RouteInfo
import com.lilapause.warumup.data.SpotCategory
import com.lilapause.warumup.data.TravelMode
import com.lilapause.warumup.data.WarmSpot
import com.lilapause.warumup.data.WarmSpotCacheManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WarmUpUiState(
  val allSpots: List<WarmSpot> = emptyList(),
  val activeCategories: Set<SpotCategory> = SpotCategory.entries.toSet(),
  val selectedSpot: WarmSpot? = null,
  val activeRoute: RouteInfo? = null,
  val isCalculatingRoute: Boolean = false,
  val selectedTravelMode: TravelMode = TravelMode.WALKING,
  val isLoading: Boolean = false,
  val isShowingCachedResults: Boolean = false,
  val errorMessage: String? = null,
  val userLat: Double? = null,
  val userLon: Double? = null,
  val mapCenterLat: Double = 52.5200, // Berlin Mitte as fallback
  val mapCenterLon: Double = 13.4050,
  val mapZoom: Double = 15.2,
  val hasMovedMapSinceFetch: Boolean = false,
  val searchQuery: String = "",
  val searchResults: List<NominatimResult> = emptyList(),
  val isSearchingLocation: Boolean = false,
  val showSpotListSheet: Boolean = false,
  val showRouteOverviewSheet: Boolean = false,
  val showLegendDialog: Boolean = false,
  val selectedMapStyle: FreeMapStyle = FreeMapStyle.OSM_DE,
  val showMapStyleDialog: Boolean = false,
  val language: AppLanguage = AppLanguage.DE,
  val isAdFree: Boolean = false,
  val isFullScreenMap: Boolean = false,
  val personalizedAdsConsent: Boolean = true,
  val hasSeenConsentBanner: Boolean = true,
  val showHotDrinkDialog: Boolean = false,
  val showLegalPrivacyDialog: Boolean = false,
  val showOnboardingDialog: Boolean = false,
  val fitRouteOnMapTrigger: Long = 0L,
) {
  val isAllCategoriesSelected: Boolean
    get() = activeCategories.size == SpotCategory.entries.size

  val isNoCategorySelected: Boolean
    get() = activeCategories.isEmpty()

  val filteredSpots: List<WarmSpot>
    get() {
      val refLat = userLat ?: mapCenterLat
      val refLon = userLon ?: mapCenterLon
      val routeSpots = activeRoute?.targetSpots.orEmpty()

      val categorySpots = if (activeCategories.isEmpty()) {
        emptyList()
      } else {
        allSpots.filter { it.category in activeCategories }
      }

      // Always keep route target spots visible
      val missingRouteSpots = routeSpots.filter { routeSpot ->
        categorySpots.none { it.id == routeSpot.id }
      }

      return (categorySpots + missingRouteSpots)
        .sortedBy { it.distanceMetersTo(refLat, refLon) }
    }

  val categoryCounts: Map<SpotCategory, Int>
    get() = allSpots.groupingBy { it.category }.eachCount()
}

class MainScreenViewModel(
  private val overpassService: OverpassService = OverpassService(),
) : ViewModel() {

  private val _uiState = MutableStateFlow(WarmUpUiState())
  val uiState: StateFlow<WarmUpUiState> = _uiState.asStateFlow()

  private var cacheManager: WarmSpotCacheManager? = null
  private var hasInitializedCache = false
  private var fetchJob: Job? = null
  private var searchJob: Job? = null
  private var routeJob: Job? = null
  private var lastFetchedBbox: BoundingBoxCoords? = null

  fun initializeWithCacheOrFetch(
    context: Context,
    currentGpsLocation: Pair<Double, Double>?,
  ): Pair<Double, Double> {
    if (cacheManager == null) {
      cacheManager = WarmSpotCacheManager(context.applicationContext)
      val savedLang = cacheManager?.getSavedLanguage() ?: AppLanguage.DE
      val personalizedConsent = cacheManager?.getPersonalizedAdsConsent() ?: true
      val seenConsent = cacheManager?.hasSeenConsentBanner() ?: false
      val seenOnboarding = cacheManager?.hasSeenOnboarding() ?: false
      val adFree = cacheManager?.isAdFree() ?: false

      _uiState.update {
        it.copy(
          language = savedLang,
          isAdFree = adFree,
          personalizedAdsConsent = personalizedConsent,
          hasSeenConsentBanner = seenConsent,
          showOnboardingDialog = !seenOnboarding,
        )
      }
    }

    if (hasInitializedCache) {
      return _uiState.value.mapCenterLat to _uiState.value.mapCenterLon
    }
    hasInitializedCache = true

    val cached = cacheManager?.loadCache()
    if (cached != null && cached.spots.isNotEmpty()) {
      val positionUnchanged = if (currentGpsLocation != null) {
        cacheManager?.isSamePosition(
          cached = cached,
          currentLat = currentGpsLocation.first,
          currentLon = currentGpsLocation.second,
        ) == true
      } else {
        true
      }

      if (positionUnchanged) {
        val targetLat = currentGpsLocation?.first ?: cached.centerLat
        val targetLon = currentGpsLocation?.second ?: cached.centerLon
        lastFetchedBbox = BoundingBoxCoords(
          south = targetLat - 0.015,
          west = targetLon - 0.020,
          north = targetLat + 0.015,
          east = targetLon + 0.020,
        )
        _uiState.update {
          it.copy(
            allSpots = cached.spots,
            mapCenterLat = targetLat,
            mapCenterLon = targetLon,
            mapZoom = cached.zoom.coerceIn(13.0, 18.0),
            isShowingCachedResults = true,
            hasMovedMapSinceFetch = false,
          )
        }
        return targetLat to targetLon
      }
    }

    val defaultLat = currentGpsLocation?.first ?: 52.5200
    val defaultLon = currentGpsLocation?.second ?: 13.4050
    _uiState.update {
      it.copy(
        mapCenterLat = defaultLat,
        mapCenterLon = defaultLon,
        mapZoom = 15.2,
      )
    }

    val initialBbox = BoundingBoxCoords(
      south = defaultLat - 0.015,
      west = defaultLon - 0.020,
      north = defaultLat + 0.015,
      east = defaultLon + 0.020,
    )
    fetchWarmSpotsForBbox(initialBbox, auto = true)
    return defaultLat to defaultLon
  }

  fun fetchWarmSpotsForBbox(bbox: BoundingBoxCoords, auto: Boolean = false) {
    if (auto && lastFetchedBbox != null) {
      val prev = lastFetchedBbox!!
      val latDiff = kotlin.math.abs(((bbox.south + bbox.north) / 2.0) - ((prev.south + prev.north) / 2.0))
      val lonDiff = kotlin.math.abs(((bbox.west + bbox.east) / 2.0) - ((prev.west + prev.east) / 2.0))
      if (latDiff < 0.003 && lonDiff < 0.004 && _uiState.value.allSpots.isNotEmpty()) {
        return
      }
    }

    lastFetchedBbox = bbox
    fetchJob?.cancel()
    fetchJob = viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }

      val result = overpassService.fetchWarmSpots(
        bbox = bbox,
        onPartialSpots = { partial ->
          _uiState.update { state ->
            val prevSelected = state.selectedSpot
            val reselected = if (prevSelected != null) {
              partial.firstOrNull { it.id == prevSelected.id } ?: prevSelected
            } else null
            state.copy(
              allSpots = partial,
              selectedSpot = reselected,
              isShowingCachedResults = false,
              hasMovedMapSinceFetch = false,
            )
          }
        },
      )

      result.onSuccess { spots ->
        _uiState.update { state ->
          val prevSelected = state.selectedSpot
          val reselected = if (prevSelected != null) {
            spots.firstOrNull { it.id == prevSelected.id } ?: prevSelected
          } else null
          state.copy(
            allSpots = spots,
            selectedSpot = reselected,
            isLoading = false,
            isShowingCachedResults = false,
            hasMovedMapSinceFetch = false,
          )
        }
        val centerLat = (bbox.south + bbox.north) / 2.0
        val centerLon = (bbox.west + bbox.east) / 2.0
        cacheManager?.saveCache(
          centerLat = centerLat,
          centerLon = centerLon,
          zoom = _uiState.value.mapZoom,
          spots = spots,
        )
      }.onFailure { err ->
        _uiState.update {
          it.copy(
            isLoading = false,
            errorMessage = if (it.allSpots.isEmpty()) {
              if (it.language == AppLanguage.EN) "Could not load winter spots: ${err.message}"
              else "Konnte warme Orte nicht laden: ${err.message}"
            } else null,
          )
        }
      }
    }
  }

  fun onMapMoved(centerLat: Double, centerLon: Double, zoom: Double) {
    _uiState.update {
      it.copy(
        mapCenterLat = centerLat,
        mapCenterLon = centerLon,
        mapZoom = zoom,
        hasMovedMapSinceFetch = true,
      )
    }
  }

  fun updateUserLocation(lat: Double, lon: Double) {
    _uiState.update { it.copy(userLat = lat, userLon = lon) }
  }

  fun onUserLocationDiscovered(lat: Double, lon: Double, forceFetchIfEmptyOrFar: Boolean = true) {
    val prevState = _uiState.value
    _uiState.update {
      it.copy(
        userLat = lat,
        userLon = lon,
        mapCenterLat = lat,
        mapCenterLon = lon,
        mapZoom = 15.5,
      )
    }

    if (forceFetchIfEmptyOrFar) {
      val isFar = prevState.allSpots.isEmpty() ||
        prevState.allSpots.firstOrNull()?.distanceMetersTo(lat, lon)?.let { it > 3500 } ?: true
      if (isFar) {
        val bbox = BoundingBoxCoords(
          south = lat - 0.018,
          west = lon - 0.024,
          north = lat + 0.018,
          east = lon + 0.024,
        )
        fetchWarmSpotsForBbox(bbox, auto = false)
      }
    }
  }

  fun focusActiveRoute() {
    _uiState.update {
      it.copy(
        fitRouteOnMapTrigger = System.currentTimeMillis(),
        showRouteOverviewSheet = true,
      )
    }
  }

  fun toggleCategory(cat: SpotCategory) {
    _uiState.update { state ->
      val newSet = state.activeCategories.toMutableSet()
      if (cat in newSet) {
        newSet.remove(cat)
      } else {
        newSet.add(cat)
      }
      state.copy(activeCategories = newSet)
    }
  }

  fun selectAllCategories() {
    _uiState.update { it.copy(activeCategories = SpotCategory.entries.toSet()) }
  }

  fun clearAllCategories() {
    _uiState.update { it.copy(activeCategories = emptySet()) }
  }

  fun selectSpot(spot: WarmSpot?) {
    _uiState.update { it.copy(selectedSpot = spot) }
  }

  fun onMapTapped() {
    _uiState.update {
      it.copy(
        selectedSpot = null,
        showRouteOverviewSheet = false,
        showSpotListSheet = false,
        searchResults = emptyList(),
        isSearchingLocation = false,
      )
    }
  }

  fun changeTravelMode(mode: TravelMode) {
    val route = _uiState.value.activeRoute ?: return
    val startLat = _uiState.value.userLat ?: _uiState.value.mapCenterLat
    val startLon = _uiState.value.userLon ?: _uiState.value.mapCenterLon

    _uiState.update {
      it.copy(
        selectedTravelMode = mode,
        isCalculatingRoute = true,
        showRouteOverviewSheet = true,
      )
    }
    routeJob?.cancel()
    routeJob = viewModelScope.launch {
      val result = if (route.targetSpots.size > 1) {
        overpassService.calculateMultiStopRoute(startLat, startLon, route.targetSpots, mode)
      } else if (route.targetSpots.size == 1) {
        overpassService.calculateRoute(startLat, startLon, route.targetSpots.first(), mode)
      } else {
        null
      }
      result?.onSuccess { newRoute ->
        _uiState.update {
          it.copy(
            activeRoute = newRoute,
            selectedTravelMode = mode,
            isCalculatingRoute = false,
            showRouteOverviewSheet = true,
            fitRouteOnMapTrigger = System.currentTimeMillis(),
          )
        }
      }?.onFailure {
        _uiState.update { it.copy(isCalculatingRoute = false) }
      }
    }
  }

  fun calculateRouteTo(spot: WarmSpot, mode: TravelMode? = null) {
    val currentMode = mode ?: _uiState.value.selectedTravelMode
    val startLat = _uiState.value.userLat ?: _uiState.value.mapCenterLat
    val startLon = _uiState.value.userLon ?: _uiState.value.mapCenterLon

    routeJob?.cancel()
    routeJob = viewModelScope.launch {
      _uiState.update {
        it.copy(
          isCalculatingRoute = true,
          selectedTravelMode = currentMode,
          selectedSpot = spot,
        )
      }

      val result = overpassService.calculateRoute(startLat, startLon, spot, currentMode)
      result.onSuccess { route ->
        _uiState.update {
          it.copy(
            activeRoute = route,
            selectedTravelMode = currentMode,
            isCalculatingRoute = false,
            selectedSpot = null,
            showRouteOverviewSheet = true,
            fitRouteOnMapTrigger = System.currentTimeMillis(),
          )
        }
      }.onFailure { err ->
        _uiState.update {
          it.copy(
            isCalculatingRoute = false,
            errorMessage = if (it.language == AppLanguage.EN) "Route calculation failed: ${err.message}"
            else "Routenberechnung fehlgeschlagen: ${err.message}",
          )
        }
      }
    }
  }

  fun addStopToRoute(spot: WarmSpot) {
    val existing = _uiState.value.activeRoute ?: run {
      calculateRouteTo(spot)
      return
    }

    if (existing.containsSpot(spot.id)) return

    val updatedStops = existing.targetSpots + spot
    val startLat = _uiState.value.userLat ?: _uiState.value.mapCenterLat
    val startLon = _uiState.value.userLon ?: _uiState.value.mapCenterLon
    val activeMode = _uiState.value.selectedTravelMode

    routeJob?.cancel()
    routeJob = viewModelScope.launch {
      _uiState.update {
        it.copy(
          isCalculatingRoute = true,
          selectedTravelMode = activeMode,
          selectedSpot = null,
          showRouteOverviewSheet = true,
        )
      }
      val result = overpassService.calculateMultiStopRoute(startLat, startLon, updatedStops, activeMode)
      result.onSuccess { route ->
        _uiState.update {
          it.copy(
            activeRoute = route,
            selectedTravelMode = activeMode,
            isCalculatingRoute = false,
            selectedSpot = null,
            showRouteOverviewSheet = true,
            fitRouteOnMapTrigger = System.currentTimeMillis(),
          )
        }
      }.onFailure {
        _uiState.update { it.copy(isCalculatingRoute = false) }
      }
    }
  }

  fun removeStopFromRoute(spotId: Long) {
    val existing = _uiState.value.activeRoute ?: return
    val remaining = existing.targetSpots.filterNot { it.id == spotId }
    if (remaining.isEmpty()) {
      clearRoute()
      return
    }

    val startLat = _uiState.value.userLat ?: _uiState.value.mapCenterLat
    val startLon = _uiState.value.userLon ?: _uiState.value.mapCenterLon
    val activeMode = _uiState.value.selectedTravelMode

    routeJob?.cancel()
    routeJob = viewModelScope.launch {
      _uiState.update {
        it.copy(
          isCalculatingRoute = true,
          selectedTravelMode = activeMode,
          showRouteOverviewSheet = true,
        )
      }
      val result = if (remaining.size > 1) {
        overpassService.calculateMultiStopRoute(startLat, startLon, remaining, activeMode)
      } else {
        overpassService.calculateRoute(startLat, startLon, remaining.first(), activeMode)
      }
      result.onSuccess { route ->
        _uiState.update {
          it.copy(
            activeRoute = route,
            selectedTravelMode = activeMode,
            isCalculatingRoute = false,
            showRouteOverviewSheet = true,
            fitRouteOnMapTrigger = System.currentTimeMillis(),
          )
        }
      }.onFailure {
        _uiState.update { it.copy(isCalculatingRoute = false) }
      }
    }
  }

  fun cancelSearch() {
    fetchJob?.cancel()
    _uiState.update {
      it.copy(
        isLoading = false,
        hasMovedMapSinceFetch = true,
      )
    }
  }

  fun clearRoute() {
    _uiState.update { it.copy(activeRoute = null, showRouteOverviewSheet = false) }
  }

  fun onSearchQueryChanged(q: String) {
    _uiState.update { it.copy(searchQuery = q) }
    searchJob?.cancel()
    if (q.trim().length < 2) {
      _uiState.update { it.copy(searchResults = emptyList(), isSearchingLocation = false) }
      return
    }

    searchJob = viewModelScope.launch {
      delay(300)
      _uiState.update { it.copy(isSearchingLocation = true) }
      val remoteResults = overpassService.searchLocations(q)

      val localMatches = _uiState.value.allSpots.filter { spot ->
        spot.name.contains(q, ignoreCase = true) ||
          spot.category.title(_uiState.value.language).contains(q, ignoreCase = true)
      }.take(4).map { spot ->
        NominatimResult(
          osm_id = spot.id,
          display_name = spot.address ?: spot.category.title(_uiState.value.language),
          name = "📍 " + spot.name,
          lat = spot.lat.toString(),
          lon = spot.lon.toString(),
        )
      }

      val combined = (localMatches + remoteResults).distinctBy { it.lat to it.lon }
      _uiState.update { it.copy(searchResults = combined, isSearchingLocation = false) }
    }
  }

  fun clearSearch() {
    _uiState.update { it.copy(searchQuery = "", searchResults = emptyList(), isSearchingLocation = false) }
  }

  fun selectSearchResult(item: NominatimResult) {
    val lat = item.lat.toDoubleOrNull() ?: return
    val lon = item.lon.toDoubleOrNull() ?: return
    val matchingSpot = _uiState.value.allSpots.firstOrNull {
      it.id == item.osm_id || (kotlin.math.abs(it.lat - lat) < 0.0001 && kotlin.math.abs(it.lon - lon) < 0.0001)
    }

    _uiState.update {
      it.copy(
        searchQuery = "",
        searchResults = emptyList(),
        mapCenterLat = lat,
        mapCenterLon = lon,
        mapZoom = if (matchingSpot != null) 16.5 else 15.5,
        selectedSpot = matchingSpot ?: it.selectedSpot,
      )
    }
    val bbox = BoundingBoxCoords(
      south = lat - 0.015,
      west = lon - 0.020,
      north = lat + 0.015,
      east = lon + 0.020,
    )
    fetchWarmSpotsForBbox(bbox)
  }

  fun toggleLanguage() {
    val newLang = if (_uiState.value.language == AppLanguage.DE) AppLanguage.EN else AppLanguage.DE
    cacheManager?.saveLanguage(newLang)
    _uiState.update { it.copy(language = newLang) }
  }

  fun setMapStyle(style: FreeMapStyle) {
    _uiState.update { it.copy(selectedMapStyle = style, showMapStyleDialog = false) }
  }

  fun toggleFullScreenMap() {
    _uiState.update { it.copy(isFullScreenMap = !it.isFullScreenMap) }
  }

  fun setPersonalizedAdsConsent(consent: Boolean) {
    cacheManager?.setPersonalizedAdsConsent(consent)
    _uiState.update { it.copy(personalizedAdsConsent = consent, hasSeenConsentBanner = true) }
  }

  fun setAdFree(unlocked: Boolean) {
    cacheManager?.setAdFree(unlocked)
    _uiState.update { it.copy(isAdFree = unlocked, showHotDrinkDialog = false) }
  }

  fun dismissOnboarding() {
    cacheManager?.setHasSeenOnboarding(true)
    _uiState.update { it.copy(showOnboardingDialog = false) }
  }

  fun setSpotListSheetVisible(visible: Boolean) {
    _uiState.update { it.copy(showSpotListSheet = visible) }
  }

  fun setRouteOverviewSheetVisible(visible: Boolean) {
    _uiState.update { it.copy(showRouteOverviewSheet = visible) }
  }

  fun setLegendDialogVisible(visible: Boolean) {
    _uiState.update { it.copy(showLegendDialog = visible) }
  }

  fun setMapStyleDialogVisible(visible: Boolean) {
    _uiState.update { it.copy(showMapStyleDialog = visible) }
  }

  fun setHotDrinkDialogVisible(visible: Boolean) {
    _uiState.update { it.copy(showHotDrinkDialog = visible) }
  }

  fun setLegalPrivacyDialogVisible(visible: Boolean) {
    _uiState.update { it.copy(showLegalPrivacyDialog = visible) }
  }
}

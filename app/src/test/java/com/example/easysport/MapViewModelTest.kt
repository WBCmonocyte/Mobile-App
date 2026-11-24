package com.example.easysport

import com.example.easysport.ui.map.MapViewModel
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.*
import org.junit.Test

class MapViewModelTest {

    @Test
    fun initialState_hasDefaultSportAndEmptyQuery() {
        val vm = MapViewModel(initialSport = "Bóng đá", dispatcher = Dispatchers.Unconfined)
        assertEquals("Bóng đá", vm.uiState.value.selectedSport)
        assertEquals("", vm.uiState.value.searchQuery)
        assertNull(vm.uiState.value.userInitiatedCourtId)
    }

    @Test
    fun onCourtSelected_setsSelectedAndUserInitiatedCourtId() {
        val vm = MapViewModel(initialSport = "Bóng đá", dispatcher = Dispatchers.Unconfined)
        vm.onCourtSelected("test_court_1")

        assertEquals("test_court_1", vm.uiState.value.userInitiatedCourtId)
    }

    @Test
    fun onSportSelected_resetsSelectionAndUserInitiated() {
        val vm = MapViewModel(initialSport = "Bóng đá", dispatcher = Dispatchers.Unconfined)
        vm.onCourtSelected("court_1")
        vm.onSportSelected("Cầu lông")

        assertEquals("Cầu lông", vm.uiState.value.selectedSport)
        assertNull(vm.uiState.value.userInitiatedCourtId)
    }
}


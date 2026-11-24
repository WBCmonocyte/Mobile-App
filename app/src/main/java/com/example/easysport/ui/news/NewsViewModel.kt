package com.example.easysport.ui.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.easysport.data.model.NewsItem
import com.example.easysport.data.model.TournamentItem
import com.example.easysport.data.remote.PaymentApiClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface NewsUiState {
    object Loading : NewsUiState
    data class Success(val items: List<NewsItem>) : NewsUiState
    object Empty : NewsUiState
    data class Error(val message: String) : NewsUiState
}

sealed interface TournamentUiState {
    object Loading : TournamentUiState
    data class Success(val items: List<TournamentItem>) : TournamentUiState
    object Empty : TournamentUiState
    data class Error(val message: String) : TournamentUiState
}

interface NewsDataSource {
    suspend fun getNews(): List<NewsItem>
    suspend fun getUpcomingTournaments(): List<TournamentItem>
}

object DefaultNewsDataSource : NewsDataSource {
    override suspend fun getNews(): List<NewsItem> = PaymentApiClient.getNews()
    override suspend fun getUpcomingTournaments(): List<TournamentItem> = PaymentApiClient.getUpcomingTournaments()
}

class NewsViewModel(
    private val dataSource: NewsDataSource = DefaultNewsDataSource,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) : ViewModel() {

    private val _newsState = MutableStateFlow<NewsUiState>(NewsUiState.Loading)
    val newsState: StateFlow<NewsUiState> = _newsState.asStateFlow()

    private val _tournamentsState = MutableStateFlow<TournamentUiState>(TournamentUiState.Loading)
    val tournamentsState: StateFlow<TournamentUiState> = _tournamentsState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        loadTournaments()
        loadNews()
    }

    fun loadTournaments() {
        viewModelScope.launch(dispatcher) {
            _tournamentsState.value = TournamentUiState.Loading
            try {
                val list = dataSource.getUpcomingTournaments()
                _tournamentsState.value = if (list.isEmpty()) {
                    TournamentUiState.Empty
                } else {
                    TournamentUiState.Success(list)
                }
            } catch (e: Exception) {
                _tournamentsState.value = TournamentUiState.Error(
                    e.localizedMessage ?: "Không thể tải danh sách giải đấu."
                )
            }
        }
    }

    fun loadNews() {
        viewModelScope.launch(dispatcher) {
            _newsState.value = NewsUiState.Loading
            try {
                val list = dataSource.getNews()
                _newsState.value = if (list.isEmpty()) {
                    NewsUiState.Empty
                } else {
                    NewsUiState.Success(list)
                }
            } catch (e: Exception) {
                _newsState.value = NewsUiState.Error(
                    e.localizedMessage ?: "Không thể tải tin tức lúc này."
                )
            }
        }
    }
}

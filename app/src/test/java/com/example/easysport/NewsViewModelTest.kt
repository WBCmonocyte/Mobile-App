package com.example.easysport

import com.example.easysport.data.model.NewsItem
import com.example.easysport.data.model.TournamentItem
import com.example.easysport.ui.news.NewsDataSource
import com.example.easysport.ui.news.NewsUiState
import com.example.easysport.ui.news.NewsViewModel
import com.example.easysport.ui.news.TournamentUiState
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeNewsDataSource(
    var newsResult: Result<List<NewsItem>> = Result.success(emptyList()),
    var tournamentsResult: Result<List<TournamentItem>> = Result.success(emptyList())
) : NewsDataSource {
    override suspend fun getNews(): List<NewsItem> = newsResult.getOrThrow()
    override suspend fun getUpcomingTournaments(): List<TournamentItem> = tournamentsResult.getOrThrow()
}

class NewsViewModelTest {

    private val sampleNews = listOf(
        NewsItem(
            title = "Man Utd thắng trận kịch tính",
            source = "VnExpress",
            publishedAt = "2026-11-24T10:00:00+07:00",
            url = "https://vnexpress.net/man-utd-1.html"
        ),
        NewsItem(
            title = "U17 Việt Nam tạo địa chấn",
            source = "Tuổi Trẻ",
            publishedAt = "2026-11-24T09:30:00+07:00",
            url = "https://tuoitre.vn/u17-vn-2.html"
        )
    )

    private val sampleTournaments = listOf(
        TournamentItem(
            id = "demo_hc_dec26",
            title = "Cúp bóng đá 7 người Hoàng Cầu",
            courtId = "hanoi_fb_hoang_cau",
            courtName = "Sân bóng Hoàng Cầu",
            address = "Số 69 Hoàng Cầu, Đống Đa, Hà Nội",
            startsAt = "2026-12-05T08:00:00+07:00",
            endsAt = "2026-12-05T17:00:00+07:00",
            maxTeams = 8,
            isDemo = true
        )
    )

    @Test
    fun loadData_hasTournamentsAndNews_bothSuccess() {
        val fakeDataSource = FakeNewsDataSource(
            newsResult = Result.success(sampleNews),
            tournamentsResult = Result.success(sampleTournaments)
        )
        val vm = NewsViewModel(fakeDataSource, Dispatchers.Unconfined)

        val newsState = vm.newsState.value
        assertTrue(newsState is NewsUiState.Success)
        assertEquals(2, (newsState as NewsUiState.Success).items.size)

        val tourState = vm.tournamentsState.value
        assertTrue(tourState is TournamentUiState.Success)
        val tourList = (tourState as TournamentUiState.Success).items
        assertEquals(1, tourList.size)
        assertEquals("demo_hc_dec26", tourList[0].id)
        assertEquals("hanoi_fb_hoang_cau", tourList[0].courtId)
        assertTrue(tourList[0].isDemo)
    }

    @Test
    fun loadData_noUpcomingTournaments_showsEmptyTournamentState() {
        val fakeDataSource = FakeNewsDataSource(
            newsResult = Result.success(sampleNews),
            tournamentsResult = Result.success(emptyList())
        )
        val vm = NewsViewModel(fakeDataSource, Dispatchers.Unconfined)

        assertTrue(vm.tournamentsState.value is TournamentUiState.Empty)
        assertTrue(vm.newsState.value is NewsUiState.Success)
    }

    @Test
    fun loadData_emptyNews_showsEmptyNewsState() {
        val fakeDataSource = FakeNewsDataSource(
            newsResult = Result.success(emptyList()),
            tournamentsResult = Result.success(sampleTournaments)
        )
        val vm = NewsViewModel(fakeDataSource, Dispatchers.Unconfined)

        assertTrue(vm.newsState.value is NewsUiState.Empty)
        assertTrue(vm.tournamentsState.value is TournamentUiState.Success)
    }

    @Test
    fun loadData_newsFails_tournamentsSucceeds_independentErrorHandling() {
        val fakeDataSource = FakeNewsDataSource(
            newsResult = Result.failure(IllegalStateException("Mất kết nối mạng RSS")),
            tournamentsResult = Result.success(sampleTournaments)
        )
        val vm = NewsViewModel(fakeDataSource, Dispatchers.Unconfined)

        val newsState = vm.newsState.value
        assertTrue(newsState is NewsUiState.Error)
        assertEquals("Mất kết nối mạng RSS", (newsState as NewsUiState.Error).message)

        // Mục giải đấu vẫn thành công bình thường
        val tourState = vm.tournamentsState.value
        assertTrue(tourState is TournamentUiState.Success)
        assertEquals(1, (tourState as TournamentUiState.Success).items.size)
    }

    @Test
    fun loadData_tournamentsFails_newsSucceeds_independentErrorHandling() {
        val fakeDataSource = FakeNewsDataSource(
            newsResult = Result.success(sampleNews),
            tournamentsResult = Result.failure(IllegalStateException("Lỗi cơ sở dữ liệu giải đấu"))
        )
        val vm = NewsViewModel(fakeDataSource, Dispatchers.Unconfined)

        val tourState = vm.tournamentsState.value
        assertTrue(tourState is TournamentUiState.Error)
        assertEquals("Lỗi cơ sở dữ liệu giải đấu", (tourState as TournamentUiState.Error).message)

        // Mục tin tức vẫn thành công bình thường
        val newsState = vm.newsState.value
        assertTrue(newsState is NewsUiState.Success)
        assertEquals(2, (newsState as NewsUiState.Success).items.size)
    }
}

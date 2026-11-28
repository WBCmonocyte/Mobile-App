package com.example.easysport.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.easysport.data.model.BookingRequest
import com.example.easysport.data.model.RecurringCheckoutResult
import com.example.easysport.data.repository.BookingStore
import com.example.easysport.data.repository.CourtStore
import com.example.easysport.data.repository.UserStore
import com.example.easysport.ui.account.AccountScreen
import com.example.easysport.ui.booking.BookingDetailScreen
import com.example.easysport.ui.booking.CourtDetailScreen
import com.example.easysport.ui.booking.CourtSearchScreen
import com.example.easysport.ui.booking.PaymentScreen
import com.example.easysport.ui.common.EsColors
import com.example.easysport.ui.common.EsPrimaryButton
import com.example.easysport.ui.home.HomeScreen
import com.example.easysport.ui.home.HomeTab
import com.example.easysport.ui.home.NotificationScreen
import com.example.easysport.ui.login.LoginRoute
import com.example.easysport.ui.map.MapScreen
import com.example.easysport.ui.news.NewsScreen
import com.example.easysport.ui.owner.OwnerDashboardScreen
import com.example.easysport.ui.recurring.MyRecurringPackagesScreen
import com.example.easysport.ui.recurring.RecurringPaymentScreen
import com.example.easysport.ui.signup.SignUpRoute
import com.example.easysport.ui.staff.StaffDashboardScreen
import com.example.easysport.ui.wallet.WalletScreen
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object Routes {
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val HOME = "home"
    const val MAP = "map"
    const val MAP_WITH_SPORT = "map?sport={sport}"
    const val ACCOUNT = "account"
    const val NEWS = "news"
    const val SEARCH = "search"
    const val PAYMENT = "payment"
    const val WALLET = "wallet"
    const val RECURRING_PACKAGES = "recurring_packages"
    const val RECURRING_PAYMENT = "recurring_payment/{packageId}"
    const val NOTIFICATIONS = "notifications"
    const val COURT = "court/{courtId}"
    const val BOOKING = "booking/{bookingId}"
    const val STAFF_DASHBOARD = "staff_dashboard"
    const val OWNER_DASHBOARD = "owner_dashboard"

    fun court(id: String) = "court/$id"
    fun booking(id: String) = "booking/$id"
    fun recurringPayment(packageId: String) = "recurring_payment/$packageId"
    fun map(sport: String? = null) = if (sport != null) "map?sport=${android.net.Uri.encode(sport)}" else "map"
}

@Composable
fun AppNavHost(
    nav: NavHostController = rememberNavController(),
    startDestination: String? = null,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Khởi tạo UserStore và đồng bộ cache / backend
    LaunchedEffect(Unit) {
        CourtStore.refresh(context)
        BookingStore.refresh()
    }

    val resolvedStartDestination = remember {
        startDestination ?: if (UserStore.isLoggedIn()) {
            when (UserStore.getUser()?.role?.uppercase()) {
                "STAFF" -> Routes.STAFF_DASHBOARD
                "OWNER" -> Routes.OWNER_DASHBOARD
                else -> Routes.HOME
            }
        } else Routes.LOGIN
    }

    var paymentRequest by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<BookingRequest?>(null) }
    var pendingRecurringResult by remember { mutableStateOf<RecurringCheckoutResult?>(null) }
    val allBookings by BookingStore.bookings.collectAsStateWithLifecycle()

    val bookingDates = remember(allBookings) {
        val dates = mutableSetOf<LocalDate>()
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val dateRegex = Regex("\\b\\d{2}/\\d{2}/\\d{4}\\b")
        allBookings.filter { it.payment in listOf(com.example.easysport.data.model.PaymentStatus.PAID, com.example.easysport.data.model.PaymentStatus.DEPOSIT_PAID) }.forEach { b ->
            val match = dateRegex.find(b.dateLabel)
            if (match != null) {
                try {
                    dates.add(LocalDate.parse(match.value, formatter))
                } catch (_: Exception) {}
            }
        }
        dates
    }

    fun navigateToTab(tab: HomeTab) {
        when (tab) {
            HomeTab.HOME -> {
                nav.navigate(Routes.HOME) {
                    popUpTo(Routes.HOME) {
                        saveState = true
                        inclusive = false
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
            HomeTab.MAP -> {
                nav.navigate(Routes.map()) {
                    popUpTo(Routes.HOME) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
            HomeTab.ACCOUNT -> {
                nav.navigate(Routes.ACCOUNT) {
                    popUpTo(Routes.HOME) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
            HomeTab.NEWS -> {
                nav.navigate(Routes.NEWS) {
                    popUpTo(Routes.HOME) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }

    NavHost(navController = nav, startDestination = resolvedStartDestination) {

        composable(Routes.PAYMENT) {
            PaymentScreen(
                request = paymentRequest,
                onBack = { nav.popBackStack() },
                onPaymentSuccess = { newBooking ->
                    BookingStore.add(newBooking)
                    paymentRequest = null
                    nav.navigate(Routes.booking(newBooking.id)) {
                        popUpTo(Routes.HOME) { inclusive = false }
                    }
                }
            )
        }

        composable(Routes.NOTIFICATIONS) {
            NotificationScreen(
                onBack = { nav.popBackStack() },
                onOpenBooking = { bookingId ->
                    nav.navigate(Routes.booking(bookingId))
                }
            )
        }

        composable(Routes.LOGIN) {
            LoginRoute(
                onLoggedIn = {
                    CourtStore.refresh(context)
                    BookingStore.refresh()
                    val dest = when (UserStore.getUser()?.role?.uppercase()) {
                        "STAFF" -> Routes.STAFF_DASHBOARD
                        "OWNER" -> Routes.OWNER_DASHBOARD
                        else -> Routes.HOME
                    }
                    nav.navigate(dest) { popUpTo(Routes.LOGIN) { inclusive = true } }
                },
                onSignUp = { nav.navigate(Routes.SIGNUP) },
            )
        }

        composable(Routes.STAFF_DASHBOARD) {
            StaffDashboardScreen(
                onLogout = {
                    UserStore.logout()
                    BookingStore.clear()
                    nav.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.OWNER_DASHBOARD) {
            OwnerDashboardScreen(
                onLogout = {
                    UserStore.logout()
                    BookingStore.clear()
                    nav.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.SIGNUP) {
            SignUpRoute(
                onComplete = { user ->
                    UserStore.update(user)
                    CourtStore.refresh(context)
                    BookingStore.refresh()
                    nav.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onSignIn = { nav.popBackStack() },
            )
        }

        composable(Routes.HOME) {
            val courts by CourtStore.courts.collectAsStateWithLifecycle()
            val isRefreshing by CourtStore.isRefreshing.collectAsStateWithLifecycle()
            val lastError by CourtStore.lastError.collectAsStateWithLifecycle()

            HomeScreen(
                courts = courts,
                upcoming = allBookings.filter { it.payment in listOf(com.example.easysport.data.model.PaymentStatus.PAID, com.example.easysport.data.model.PaymentStatus.DEPOSIT_PAID) }
                    .filter { runCatching { LocalDate.parse(it.dateLabel, DateTimeFormatter.ofPattern("dd/MM/yyyy")) >= LocalDate.now() }.getOrDefault(false) }
                    .minByOrNull { LocalDate.parse(it.dateLabel, DateTimeFormatter.ofPattern("dd/MM/yyyy")).toEpochDay() * 1440 + (it.timeRange.substringBefore(":").toLongOrNull() ?: 0) * 60 },
                allBookings = allBookings,
                bookingDates = bookingDates,
                isRefreshing = isRefreshing,
                errorMessage = lastError,
                onRefresh = {
                    CourtStore.refresh(context)
                    BookingStore.refresh()
                },
                onBookCourt = { nav.navigate(Routes.SEARCH) },
                onOpenBooking = { id -> nav.navigate(Routes.booking(id)) },
                onCourtClick = { id -> nav.navigate(Routes.court(id)) },
                onNotifications = { nav.navigate(Routes.NOTIFICATIONS) },
                onOpenMap = { sport ->
                    nav.navigate(Routes.map(sport)) {
                        popUpTo(Routes.HOME) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onOpenProfile = { navigateToTab(HomeTab.ACCOUNT) },
                onOpenWallet = { nav.navigate(Routes.WALLET) },
                onOpenRecurring = { nav.navigate(Routes.RECURRING_PACKAGES) },
                onTabSelected = ::navigateToTab,
            )
        }

        composable(
            route = Routes.MAP_WITH_SPORT,
            arguments = listOf(navArgument("sport") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { entry ->
            val sport = entry.arguments?.getString("sport")
            MapScreen(
                initialSport = sport,
                onTabSelected = ::navigateToTab,
                onOpenCourt = { courtId ->
                    nav.navigate(Routes.court(courtId))
                }
            )
        }

        composable(Routes.ACCOUNT) {
            val user by UserStore.currentUser.collectAsStateWithLifecycle()
            AccountScreen(
                user = user,
                bookings = allBookings,
                onTabSelected = ::navigateToTab,
                onLogout = {
                    UserStore.logout()
                    BookingStore.clear()
                    nav.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onOpenBooking = { bookingId ->
                    nav.navigate(Routes.booking(bookingId))
                },
                onOpenWallet = { nav.navigate(Routes.WALLET) },
                onOpenRecurring = { nav.navigate(Routes.RECURRING_PACKAGES) },
            )
        }

        composable(Routes.WALLET) {
            WalletScreen(
                onBack = { nav.popBackStack() },
                onOpenRecurring = { nav.navigate(Routes.RECURRING_PACKAGES) },
            )
        }

        composable(Routes.RECURRING_PACKAGES) {
            MyRecurringPackagesScreen(
                onBack = { nav.popBackStack() },
                onContinuePayment = { pkgId ->
                    pendingRecurringResult = null
                    nav.navigate(Routes.recurringPayment(pkgId))
                },
            )
        }

        composable(
            route = Routes.RECURRING_PAYMENT,
            arguments = listOf(navArgument("packageId") { type = NavType.StringType }),
        ) { entry ->
            val pkgId = entry.arguments?.getString("packageId") ?: ""
            RecurringPaymentScreen(
                packageId = pkgId,
                initialResult = pendingRecurringResult?.takeIf { it.packageId == pkgId },
                onBack = { nav.popBackStack() },
                onPaymentSuccess = {
                    pendingRecurringResult = null
                    nav.navigate(Routes.RECURRING_PACKAGES) {
                        popUpTo(Routes.HOME) { inclusive = false }
                    }
                },
            )
        }

        composable(Routes.NEWS) {
            NewsScreen(
                onTabSelected = ::navigateToTab,
                onCourtClick = { courtId ->
                    nav.navigate(Routes.court(courtId))
                }
            )
        }

        composable(Routes.SEARCH) {
            val courts by CourtStore.courts.collectAsStateWithLifecycle()
            CourtSearchScreen(
                courts = courts,
                onBack = { nav.popBackStack() },
                onSearchNearby = {
                    nav.navigate(Routes.map())
                },
                onCourtSelected = { id -> nav.navigate(Routes.court(id)) },
            )
        }

        composable(
            route = Routes.COURT,
            arguments = listOf(navArgument("courtId") { type = NavType.StringType }),
        ) { entry ->
            val courtId = entry.arguments?.getString("courtId")
            val courts by CourtStore.courts.collectAsStateWithLifecycle()
            val court = courts.firstOrNull { it.id == courtId }
            if (court != null) {
                CourtDetailScreen(
                    court = court,
                    onBack = { nav.popBackStack() },
                    onConfirm = { request ->
                        paymentRequest = request
                        nav.navigate(Routes.PAYMENT)
                    },
                    onRecurringConfirm = { res ->
                        pendingRecurringResult = res
                        if (res.status == "ACTIVE") {
                            BookingStore.refresh()
                            nav.navigate(Routes.RECURRING_PACKAGES) {
                                popUpTo(Routes.HOME) { inclusive = false }
                            }
                        } else {
                            nav.navigate(Routes.recurringPayment(res.packageId))
                        }
                    },
                )
            } else {
                Surface(Modifier.fillMaxSize()) {
                    Column(
                        Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Không tìm thấy thông tin sân bóng.", color = EsColors.Title)
                        Spacer(Modifier.height(16.dp))
                        EsPrimaryButton(text = "Quay lại", onClick = { nav.popBackStack() })
                    }
                }
            }
        }

        composable(
            route = Routes.BOOKING,
            arguments = listOf(navArgument("bookingId") { type = NavType.StringType }),
        ) { entry ->
            val bookingId = entry.arguments?.getString("bookingId")
            val booking = allBookings.firstOrNull { it.id == bookingId }
            if (booking != null) {
                BookingDetailScreen(
                    booking = booking,
                    onBack = { nav.popBackStack() },
                    onCancelBooking = {
                        BookingStore.cancelBooking(booking.id) { success, error ->
                            if (success) nav.popBackStack()
                            else android.widget.Toast.makeText(context, error ?: "Không thể hủy lịch", android.widget.Toast.LENGTH_LONG).show()
                        }
                    },
                )
            } else {
                Surface(Modifier.fillMaxSize()) {
                    Column(
                        Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Không tìm thấy thông tin lịch đặt.", color = EsColors.Title)
                        Spacer(Modifier.height(16.dp))
                        EsPrimaryButton(text = "Quay về", onClick = { nav.popBackStack() })
                    }
                }
            }
        }
    }
}

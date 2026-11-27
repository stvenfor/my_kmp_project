package com.example.my_kmp_project.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.example.my_kmp_project.core.router.AppRoutePath
import com.example.my_kmp_project.core.router.HomeRoutes

@Composable
fun HomeRouteHost(
    route: String,
    onBack: () -> Unit,
    content: HomeContentStore,
    onNavigate: (String) -> Unit = {},
) {
    when (route) {
        HomeRoutes.Search -> HomeSearchScreen(onBack = onBack, content = content)
        HomeRoutes.Strategy -> StrategyScreen(onBack = onBack, content = content)
        HomeRoutes.LearningReport -> LearningReportScreen(
            onBack = onBack,
            content = content,
            onOpenMembership = { onNavigate("/pay/membership") },
        )
        HomeRoutes.CheckInMall -> CheckInMallScreen(onBack = onBack)
        HomeRoutes.TransliterFeed -> TransliterFeedScreen(
            onBack = onBack,
            content = content,
            onOpenHotRank = { onNavigate(HomeRoutes.HotRankDetail) },
        )
        HomeRoutes.HotRankDetail -> HotRankDetailScreen(onBack = onBack, content = content)
        HomeRoutes.LifeService -> LifeServiceScreen(onBack = onBack)
        HomeRoutes.LiveCommerce -> LiveCommerceScreen(
            onBack = onBack,
            onJoin = { onNavigate(AppRoutePath.community) },
        )
        HomeRoutes.Club -> ClubScreen(
            onBack = onBack,
            onJoin = { onNavigate(AppRoutePath.community) },
        )
        HomeRoutes.UsedCar -> UsedCarListScreen(
            onBack = onBack,
            onItem = { order ->
                HomeSecondaryStore.selectUsedCar(order.id)
                onNavigate(HomeRoutes.UsedCarDetail)
            },
            onCreate = { onNavigate(HomeRoutes.UsedCarCreate) },
        )
        HomeRoutes.UsedCarDetail -> HomeSecondaryStore
            .findUsedCar(HomeSecondaryStore.selectedUsedCarId)
            ?.let { UsedCarDetailScreen(order = it, onBack = onBack) }
            ?: CrudDetailScreen("业务单不存在", "请返回列表刷新后重试", onBack)
        HomeRoutes.UsedCarCreate -> UsedCarCreateScreen(onBack = onBack)
        HomeRoutes.Ledger -> {
            LaunchedEffect(Unit) { content.refreshLedger() }
            LedgerListScreen(
                items = content.ledgerEntries(),
                onBack = onBack,
                onItem = { item ->
                    HomeSecondaryStore.selectLedger(item.id)
                    onNavigate(HomeRoutes.LedgerDetail)
                },
            )
        }
        HomeRoutes.LedgerDetail -> {
            LaunchedEffect(Unit) { content.refreshLedger() }
            val entries = content.ledgerEntries()
            LedgerDetailScreen(
                item = entries.firstOrNull { it.id == HomeSecondaryStore.selectedLedgerId }
                    ?: entries.firstOrNull(),
                onBack = onBack,
            )
        }
        HomeRoutes.DataAnalytics -> AnalyticsListScreen(
            onBack = onBack,
            onItem = { row ->
                HomeSecondaryStore.selectAnalytics(row.id)
                onNavigate(HomeRoutes.DataAnalyticsDetail)
            },
        )
        HomeRoutes.DataAnalyticsDetail -> AnalyticsDetailScreen(
            recordId = HomeSecondaryStore.selectedAnalyticsId,
            onBack = onBack,
        )
        HomeRoutes.TodoPartner -> PartnerPendingScreen(onBack = onBack)
        HomeRoutes.TodoFollowUp -> FollowUpCustomersScreen(onBack = onBack)
        HomeRoutes.TodoAfterSales -> AfterSalesAppointmentsScreen(onBack = onBack)
        HomeRoutes.TodoOrderReview -> StoreReviewOrdersScreen(onBack = onBack)
        HomeRoutes.AfterSales -> AfterSalesListScreen(
            onBack = onBack,
            onItem = { row ->
                HomeSecondaryStore.selectAfterSales(row.id)
                onNavigate(HomeRoutes.AfterSalesDetail)
            },
            onCreate = { onNavigate(HomeRoutes.AfterSalesCreate) },
        )
        HomeRoutes.AfterSalesCreate -> AfterSalesCreateScreen(onBack = onBack)
        HomeRoutes.AfterSalesDetail -> HomeSecondaryStore
            .findAfterSales(HomeSecondaryStore.selectedAfterSalesId)
            ?.let { AfterSalesServiceDetailScreen(row = it, onBack = onBack) }
            ?: CrudDetailScreen("工单不存在", "请返回列表刷新后重试", onBack)
        HomeRoutes.NewCarFollow -> NewCarFollowListScreen(
            onBack = onBack,
            onItem = { row ->
                HomeSecondaryStore.selectNewCarFollow(row.id)
                onNavigate(HomeRoutes.NewCarFollowDetail)
            },
            onCreate = { onNavigate(HomeRoutes.NewCarFollowCreate) },
        )
        HomeRoutes.NewCarFollowCreate -> NewCarFollowCreateScreen(onBack = onBack)
        HomeRoutes.NewCarFollowDetail -> HomeSecondaryStore
            .findNewCarFollow(HomeSecondaryStore.selectedNewCarFollowId)
            ?.let { NewCarFollowArchiveDetailScreen(row = it, onBack = onBack) }
            ?: CrudDetailScreen("跟进档案不存在", "请返回列表刷新后重试", onBack)
        else -> CrudDetailScreen("未识别路由", "route=$route", onBack)
    }
}

package com.example.my_kmp_project.feature.mine

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.example.my_kmp_project.core.platform.showPlatformToast
import com.example.my_kmp_project.component.routing.CrossFeatureScreens
import com.example.my_kmp_project.core.router.MineRoutes

@Composable
fun MineRouteHost(
    route: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit = {},
) {
    val route = MineRoutes.canonicalize(route)
    when {
        CrossFeatureScreens.handlesSiblingScreen(route) ->
            CrossFeatureScreens.RenderSiblingScreen(route, onBack, onNavigate)
        route == MineRoutes.Mall -> MallListScreen(
            onBack = onBack,
            onOpenDetail = { onNavigate(MineRoutes.MallDetail) },
            onOpenOrders = { onNavigate(MineRoutes.MallOrders) },
        )
        route == MineRoutes.MallDetail -> MallDetailScreen(onBack = onBack)
        route == MineRoutes.MallOrders -> OrderListScreen(
            onBack = onBack,
            onOpen = { onNavigate(MineRoutes.MallOrderDetail) },
        )
        route == MineRoutes.MallOrderDetail -> MallOrderDetailScreen(onBack = onBack)
        route == MineRoutes.Wallet -> WalletScreen(onBack = onBack, onPay = { onNavigate(MineRoutes.Pay) })
        route == MineRoutes.Pay -> PayCheckoutScreen(onBack = onBack)
        // MineRoutes.Membership is commerce-owned — intercepted by handlesSiblingScreen above.
        route == MineRoutes.Profile -> ProfileEditScreen(
            onBack = onBack,
            onLoggedOut = { onNavigate("logout") },
        )
        route == MineRoutes.Addresses -> AddressListScreen(
            onBack = onBack,
            onCreate = {
                AddressMockStore.selectForEdit(null)
                onNavigate(MineRoutes.AddressEdit)
            },
            onEdit = { id ->
                AddressMockStore.selectForEdit(id)
                onNavigate(MineRoutes.AddressEdit)
            },
        )
        route == MineRoutes.AddressEdit -> AddressEditScreen(onBack = onBack)
        route == MineRoutes.Calculator -> PurchaseCalculatorScreen(onBack = onBack)
        route == MineRoutes.CheckIn -> CheckInShortcutScreen(onBack = onBack)
        route == MineRoutes.SmsTemplates -> SmsTemplateScreen(onBack = onBack)
        route == MineRoutes.ShopQr -> ShopQrScreen(onBack = onBack)
        route == MineRoutes.HttpTest || route == "/mine/buy_qa" -> BuyQaScreen(onBack = onBack)
        route == MineRoutes.Poster -> PosterScreen(onBack = onBack)
        route == MineRoutes.Settings || route == MineRoutes.SettingsLegacy -> MineSettingsScreen(
            onBack = onBack,
        )
        route == MineRoutes.PersonalizedSettings -> MinePersonalizedSettingsScreen(
            onBack = onBack,
            snackbar = { showPlatformToast(it) },
        )
        route == MineRoutes.DealInvoiceDemo -> DealInvoiceDemoScreen(
            onBack = onBack,
            onUpload = { onNavigate(MineRoutes.DealInvoiceUpload) },
            onOpenDetail = { onNavigate(MineRoutes.DealInvoiceUpload) },
        )
        route == MineRoutes.DealInvoiceUpload -> DealInvoiceUploadScreen(onBack = onBack)
        route == MineRoutes.Feedback -> FeedbackScreen(onBack = onBack)
        route == MineRoutes.Cooperation -> CooperationScreen(onBack = onBack)
        route == MineRoutes.Reminder -> ReminderScreen(onBack = onBack)
        route == MineRoutes.Invite -> InviteScreen(onBack = onBack)
        else -> UnmappedMineRoute(route = route, onBack = onBack)
    }
}

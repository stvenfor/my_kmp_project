package com.example.my_kmp_project.feature.mine

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.platform.showPlatformToast
import com.example.my_kmp_project.component.routing.CrossFeatureScreens
import com.example.my_kmp_project.core.router.MineRoutes
import com.example.my_kmp_project.feature.mine.dealinvoice.DealInvoiceController

@Composable
fun MineRouteHost(
    route: String,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit = {},
    api: ApiClient,
) {
    val route = MineRoutes.canonicalize(route)
    var mallProductId by remember { mutableStateOf<Int?>(null) }
    var mallOrderId by remember { mutableStateOf<Int?>(null) }
    val mallController = remember(api) { MineMallController(api) }
    val dealInvoiceController = remember(api) { DealInvoiceController(api) }
    when {
        CrossFeatureScreens.handlesSiblingScreen(route) ->
            CrossFeatureScreens.RenderSiblingScreen(route, onBack, onNavigate)
        route == MineRoutes.Mall -> MallListScreen(
            onBack = onBack,
            onOpenDetail = { productId ->
                mallProductId = productId
                onNavigate(MineRoutes.MallDetail)
            },
            onOpenOrders = { onNavigate(MineRoutes.MallOrders) },
            controller = mallController,
        )
        route == MineRoutes.MallDetail -> MallDetailScreen(
            onBack = onBack,
            productId = mallProductId,
            controller = mallController,
            onCreated = { orderId ->
                mallOrderId = orderId
                onNavigate(MineRoutes.MallOrderDetail)
            },
        )
        route == MineRoutes.MallOrders -> OrderListScreen(
            onBack = onBack,
            onOpen = { orderId ->
                mallOrderId = orderId
                onNavigate(MineRoutes.MallOrderDetail)
            },
            controller = mallController,
        )
        route == MineRoutes.MallOrderDetail -> MallOrderDetailScreen(
            onBack = onBack,
            orderId = mallOrderId,
            controller = mallController,
        )
        route == MineRoutes.Wallet -> WalletScreen(onBack = onBack, onPay = { onNavigate(MineRoutes.Pay) })
        route == MineRoutes.Pay -> PayCheckoutScreen(onBack = onBack)
        // MineRoutes.Membership is commerce-owned — intercepted by handlesSiblingScreen above.
        route == MineRoutes.Profile -> ProfileEditScreen(
            onBack = onBack,
            onLoggedOut = { onNavigate("logout") },
            api = api,
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
        route == MineRoutes.Calculator -> PurchaseCalculatorScreen(onBack = onBack, api = api)
        route == MineRoutes.CheckIn -> CheckInShortcutScreen(onBack = onBack, api = api)
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
            controller = dealInvoiceController,
        )
        route == MineRoutes.DealInvoiceUpload -> DealInvoiceUploadScreen(
            onBack = onBack,
            controller = dealInvoiceController,
        )
        route == MineRoutes.Feedback -> FeedbackScreen(onBack = onBack)
        route == MineRoutes.Cooperation -> CooperationScreen(onBack = onBack)
        route == MineRoutes.Reminder -> ReminderScreen(onBack = onBack)
        route == MineRoutes.Invite -> InviteScreen(onBack = onBack)
        else -> UnmappedMineRoute(route = route, onBack = onBack)
    }
}

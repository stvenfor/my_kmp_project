package com.example.my_kmp_project.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import my_kmp_project.composeapp.generated.resources.Res
import my_kmp_project.composeapp.generated.resources.home_all_services_after_sales_area
import my_kmp_project.composeapp.generated.resources.home_all_services_all_functions
import my_kmp_project.composeapp.generated.resources.home_all_services_business_poster
import my_kmp_project.composeapp.generated.resources.home_all_services_calculator
import my_kmp_project.composeapp.generated.resources.home_all_services_customer_profile
import my_kmp_project.composeapp.generated.resources.home_all_services_dubbing_home
import my_kmp_project.composeapp.generated.resources.home_all_services_exhibition_hall_shooting
import my_kmp_project.composeapp.generated.resources.home_all_services_intelligence_task
import my_kmp_project.composeapp.generated.resources.home_all_services_marketing
import my_kmp_project.composeapp.generated.resources.home_all_services_new_car_deal
import my_kmp_project.composeapp.generated.resources.home_all_services_new_car_in_store
import my_kmp_project.composeapp.generated.resources.home_all_services_online_customer_acquisition
import my_kmp_project.composeapp.generated.resources.home_all_services_service_management
import my_kmp_project.composeapp.generated.resources.home_all_services_small_video
import my_kmp_project.composeapp.generated.resources.home_all_services_smart_number
import my_kmp_project.composeapp.generated.resources.home_all_services_smart_online_marketing
import my_kmp_project.composeapp.generated.resources.home_all_services_smart_sale
import my_kmp_project.composeapp.generated.resources.home_all_services_used_car
import my_kmp_project.composeapp.generated.resources.home_all_services_v_store
import my_kmp_project.composeapp.generated.resources.home_feature_ai_stone
import my_kmp_project.composeapp.generated.resources.home_feature_data
import my_kmp_project.composeapp.generated.resources.home_feature_life
import my_kmp_project.composeapp.generated.resources.home_feature_live
import my_kmp_project.composeapp.generated.resources.home_feature_market
import my_kmp_project.composeapp.generated.resources.home_feature_more
import my_kmp_project.composeapp.generated.resources.home_feature_newcar
import my_kmp_project.composeapp.generated.resources.home_feature_order
import my_kmp_project.composeapp.generated.resources.home_feature_sales
import my_kmp_project.composeapp.generated.resources.home_feature_usedcar
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/** Flutter `module_home` all_services PNG → Compose drawable. */
internal object HomeServiceAssets {
    fun fromFlutterFile(assetName: String): DrawableResource? {
        val key = assetName.removeSuffix(".png").removeSuffix(".webp")
        return when (key) {
            "smart_online_marketing" -> Res.drawable.home_all_services_smart_online_marketing
            "customer_profile" -> Res.drawable.home_all_services_customer_profile
            "smart_sale" -> Res.drawable.home_all_services_smart_sale
            "new_car_deal" -> Res.drawable.home_all_services_new_car_deal
            "exhibition_hall_shooting" -> Res.drawable.home_all_services_exhibition_hall_shooting
            "intelligence_task" -> Res.drawable.home_all_services_intelligence_task
            "marketing" -> Res.drawable.home_all_services_marketing
            "business_poster" -> Res.drawable.home_all_services_business_poster
            "after_sales_area" -> Res.drawable.home_all_services_after_sales_area
            "calculator" -> Res.drawable.home_all_services_calculator
            "used_car" -> Res.drawable.home_all_services_used_car
            "service_management" -> Res.drawable.home_all_services_service_management
            "online_customer_acquisition" -> Res.drawable.home_all_services_online_customer_acquisition
            "smart_number" -> Res.drawable.home_all_services_smart_number
            "new_car_in_store" -> Res.drawable.home_all_services_new_car_in_store
            "v_store" -> Res.drawable.home_all_services_v_store
            "small_video" -> Res.drawable.home_all_services_small_video
            "dubbing_home" -> Res.drawable.home_all_services_dubbing_home
            "all_functions" -> Res.drawable.home_all_services_all_functions
            else -> null
        }
    }

    /**
     * Home root feature grid icons — synced from Flutter SoT
     * (`picsum.photos/seed/{sales,life,...}/200/200` bundled as local PNG).
     */
    fun featureForLabel(label: String): DrawableResource = when (label) {
        "销售顾问", "H5 调试" -> Res.drawable.home_feature_sales
        "生活服务" -> Res.drawable.home_feature_life
        "二手车" -> Res.drawable.home_feature_usedcar
        "新车关注", "新车成交" -> Res.drawable.home_feature_newcar
        "新车跟进" -> Res.drawable.home_feature_data
        "AI小石头", "客户管理" -> Res.drawable.home_feature_ai_stone
        "订单中心", "Club" -> Res.drawable.home_feature_order
        "数据分析" -> Res.drawable.home_feature_data
        "直播带货" -> Res.drawable.home_feature_live
        "营销活动" -> Res.drawable.home_feature_market
        "更多" -> Res.drawable.home_feature_more
        else -> Res.drawable.home_all_services_all_functions
    }

    @Deprecated("Use featureForLabel", ReplaceWith("featureForLabel(label)"))
    fun featureAt(index: Int): DrawableResource {
        val labels = listOf(
            "销售顾问", "生活服务", "二手车", "新车关注", "AI小石头",
            "订单中心", "数据分析", "直播带货", "营销活动", "更多",
        )
        return featureForLabel(labels[index % labels.size])
    }

    private val serviceCycle = listOf(
        Res.drawable.home_all_services_online_customer_acquisition,
        Res.drawable.home_all_services_small_video,
        Res.drawable.home_all_services_smart_online_marketing,
        Res.drawable.home_all_services_exhibition_hall_shooting,
        Res.drawable.home_all_services_calculator,
        Res.drawable.home_all_services_intelligence_task,
        Res.drawable.home_all_services_smart_number,
        Res.drawable.home_all_services_all_functions,
    )

    fun serviceAt(index: Int): DrawableResource = serviceCycle[index % serviceCycle.size]
}

@Composable
internal fun HomeAssetIcon(
    resource: DrawableResource,
    size: Dp = 44.dp,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
) {
    Image(
        painter = painterResource(resource),
        contentDescription = contentDescription,
        modifier = Modifier.size(size),
        contentScale = contentScale,
    )
}

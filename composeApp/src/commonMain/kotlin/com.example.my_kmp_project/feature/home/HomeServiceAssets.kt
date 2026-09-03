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

    /** Home root feature grid — cycle Flutter service icons (no letter tiles). */
    private val featureCycle = listOf(
        Res.drawable.home_all_services_smart_sale,
        Res.drawable.home_all_services_customer_profile,
        Res.drawable.home_all_services_used_car,
        Res.drawable.home_all_services_new_car_deal,
        Res.drawable.home_all_services_service_management,
        Res.drawable.home_all_services_business_poster,
        Res.drawable.home_all_services_intelligence_task,
        Res.drawable.home_all_services_small_video,
        Res.drawable.home_all_services_marketing,
        Res.drawable.home_all_services_all_functions,
    )

    fun featureAt(index: Int): DrawableResource = featureCycle[index % featureCycle.size]

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
) {
    Image(
        painter = painterResource(resource),
        contentDescription = contentDescription,
        modifier = Modifier.size(size),
        contentScale = ContentScale.Fit,
    )
}

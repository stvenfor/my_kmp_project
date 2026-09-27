package com.example.my_kmp_project.feature.mine

/**
 * Routes hosted inside Mine Compose Island (secondary + children only).
 * Mine Root is Native Shell UI and must not appear here.
 */
enum class MineIslandRoute {
    Settings,
    Personalized,
    About,
    Membership,
    Profile,
    Addresses,
    AddressEdit,
    Calculator,
    DealInvoiceDemo,
    DealInvoiceUpload,
    ;

    fun toPath(): String = when (this) {
        Settings -> MineRoutes.Settings
        Personalized -> MineRoutes.PersonalizedSettings
        About -> "/mine/about"
        Membership -> MineRoutes.Membership
        Profile -> MineRoutes.Profile
        Addresses -> MineRoutes.Addresses
        AddressEdit -> MineRoutes.AddressEdit
        Calculator -> MineRoutes.Calculator
        DealInvoiceDemo -> MineRoutes.DealInvoiceDemo
        DealInvoiceUpload -> MineRoutes.DealInvoiceUpload
    }

    companion object {
        fun fromHostKey(key: String): MineIslandRoute = when (key.lowercase()) {
            "personalized" -> Personalized
            "about" -> About
            "membership" -> Membership
            "profile" -> Profile
            "addresses" -> Addresses
            "address_edit", "addresses/edit" -> AddressEdit
            "calculator", "purchase_calculator" -> Calculator
            "deal_invoice", "deal_invoice_demo" -> DealInvoiceDemo
            "deal_invoice_upload" -> DealInvoiceUpload
            else -> Settings
        }

        fun fromPath(path: String): MineIslandRoute? {
            val p = MineRoutes.canonicalize(path)
            return when {
                p == MineRoutes.Settings || p == MineRoutes.SettingsLegacy -> Settings
                p == MineRoutes.PersonalizedSettings -> Personalized
                p == "/mine/about" -> About
                p == MineRoutes.Membership -> Membership
                p == MineRoutes.Profile -> Profile
                p == MineRoutes.Addresses -> Addresses
                p == MineRoutes.AddressEdit -> AddressEdit
                p == MineRoutes.Calculator -> Calculator
                p == MineRoutes.DealInvoiceDemo -> DealInvoiceDemo
                p == MineRoutes.DealInvoiceUpload -> DealInvoiceUpload
                else -> null
            }
        }
    }
}

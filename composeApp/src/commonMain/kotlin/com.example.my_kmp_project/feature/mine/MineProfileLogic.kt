package com.example.my_kmp_project.feature.mine

import com.example.my_kmp_project.core.account.AccountFacade
import com.example.my_kmp_project.feature.auth.AuthBridge

/** Flutter MineProfileController save / dirty / logout pure helpers + façade writes. */
internal object MineProfileLogic {
    fun isDirty(currentNick: String, savedNick: String, pendingAvatar: Boolean): Boolean {
        val nickChanged = currentNick.trim() != savedNick.trim()
        return nickChanged || pendingAvatar
    }

    fun validateSave(nickname: String, dirty: Boolean): String? {
        if (nickname.trim().isEmpty()) return "昵称不能为空"
        if (!dirty) return "没有需要保存的修改"
        return null
    }

    fun saveNickname(nickname: String) {
        AccountFacade.updateProfile(displayName = nickname.trim())
    }

    fun logout() {
        AuthBridge.logout()
    }

    fun sessionNickname(): String =
        AccountFacade.current().displayName?.takeIf { it.isNotBlank() } ?: "qa_user"

    fun sessionPhoneMasked(): String =
        MineContactValidators.maskPhone(AccountFacade.current().phone)
            .takeIf { it != "—" } ?: "138****5172"
}

package com.example.my_kmp_project.feature.auth

import com.example.my_kmp_project.core.account.AccountFacade
import com.example.my_kmp_project.core.account.LoggedInUser
import com.example.my_kmp_project.core.network.NetworkError
import com.example.my_kmp_project.core.network.NetworkFacade
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Remote auth repository aligned with Flutter `UserAuthApi` / `BackendAuthService`.
 *
 * Posts real request shapes via [NetworkFacade]; maps failures to Flutter Chinese copy.
 * Does not short-circuit to local demo success.
 */
internal object AuthRepository {

    suspend fun loginWithPassword(account: String, password: String): Result<Unit> {
        val user = account.trim()
        if (user.isEmpty()) {
            return Result.failure(IllegalArgumentException("请输入账号"))
        }
        if (user.contains('@') && !AuthValidators.isValidEmail(user)) {
            return Result.failure(IllegalArgumentException("请输入有效的邮箱"))
        }
        if (password.length < AuthValidators.MIN_PASSWORD_LENGTH) {
            return Result.failure(IllegalArgumentException("密码至少 6 位"))
        }
        return postLogin(
            path = AuthApiPaths.LOGIN,
            body = buildJsonObject {
                put("username", user)
                put("password", password)
                put("device_id", AuthDeviceContext.deviceId())
                put("platform", AuthDeviceContext.platform())
            },
        )
    }

    suspend fun sendPhoneOtp(phone: String): Result<Unit> {
        if (!AuthPhoneUtils.isValidChinaMobile(phone)) {
            return Result.failure(IllegalArgumentException("请输入有效的手机号"))
        }
        val e164 = AuthPhoneUtils.toE164China(phone)
        return postOk(
            path = AuthApiPaths.SEND_PHONE_OTP,
            body = buildJsonObject { put("phone", e164) },
            emptySuccessMessage = "验证码已发送",
        )
    }

    suspend fun loginWithOtp(phone: String, code: String): Result<Unit> {
        if (!AuthPhoneUtils.isValidChinaMobile(phone)) {
            return Result.failure(IllegalArgumentException("请输入有效的手机号"))
        }
        if (!AuthValidators.isValidOtp(code)) {
            return Result.failure(IllegalArgumentException("请输入 6 位验证码"))
        }
        val e164 = AuthPhoneUtils.toE164China(phone)
        return postLogin(
            path = AuthApiPaths.VERIFY_PHONE_OTP,
            body = buildJsonObject {
                put("phone", e164)
                put("otp", code.trim())
                put("device_id", AuthDeviceContext.deviceId())
                put("platform", AuthDeviceContext.platform())
            },
        )
    }

    /**
     * Email register matching Flutter `BackendAuthService.signUpWithEmail`.
     * If the response has no session token, falls back to password login.
     */
    suspend fun register(
        email: String,
        password: String,
        displayName: String = "",
    ): Result<Unit> {
        val normalizedEmail = email.trim()
        if (!AuthValidators.isValidEmail(normalizedEmail)) {
            return Result.failure(IllegalArgumentException("请输入有效的邮箱"))
        }
        if (password.length < AuthValidators.MIN_PASSWORD_LENGTH) {
            return Result.failure(IllegalArgumentException("密码至少 6 位"))
        }
        val username = displayName.trim().ifBlank {
            normalizedEmail.substringBefore('@').ifBlank { normalizedEmail }
        }
        return try {
            val response = NetworkFacade.api().postApi(
                path = AuthApiPaths.REGISTER,
                body = buildJsonObject {
                    put("username", username)
                    put("password", password)
                    put("email", normalizedEmail)
                    put("device_id", AuthDeviceContext.deviceId())
                    put("platform", AuthDeviceContext.platform())
                }.toString(),
                parseData = ::parseAuthRegisterPayload,
            )
            if (!isAuthBusinessSuccess(response.code) || response.data == null) {
                return Result.failure(
                    IllegalStateException(mapAuthFailure(response.code, response.message)),
                )
            }
            val payload = response.data!!
            if (payload.hasSession) {
                commitSession(
                    displayName = payload.username.ifBlank { username },
                    userId = payload.userId.ifBlank { username },
                    token = payload.token!!,
                    phone = null,
                    email = payload.email.ifBlank { normalizedEmail },
                )
            } else {
                loginWithPassword(normalizedEmail, password)
            }
        } catch (e: NetworkError.Transport) {
            Result.failure(IllegalStateException(mapAuthFailure(null, e.message)))
        } catch (e: Throwable) {
            Result.failure(IllegalStateException(mapAuthFailure(null, e.message)))
        }
    }

    /** Phone register: same OTP verify contract as login (Flutter registerWithPhone). */
    suspend fun registerWithPhone(phone: String, code: String): Result<Unit> =
        loginWithOtp(phone, code)

    fun logout() {
        AccountFacade.logout()
        AuthSessionState.clearLocal()
        AuthGate.clearPending()
    }

    private suspend fun postLogin(path: String, body: JsonObject): Result<Unit> {
        return try {
            val response = NetworkFacade.api().postApi(
                path = path,
                body = body.toString(),
                parseData = ::parseAuthLoginPayload,
            )
            val data = response.data
            if (!isAuthBusinessSuccess(response.code) || data == null || data.token.isBlank()) {
                return Result.failure(
                    IllegalStateException(mapAuthFailure(response.code, response.message)),
                )
            }
            val display = data.username.ifBlank {
                data.email.substringBefore('@').ifBlank { data.email }
            }.ifBlank { "用户" }
            commitSession(
                displayName = display,
                userId = data.userId.ifBlank { display },
                token = data.token,
                phone = null,
                email = data.email.ifBlank { null },
            )
        } catch (e: NetworkError.Transport) {
            Result.failure(IllegalStateException(mapAuthFailure(null, e.message)))
        } catch (e: Throwable) {
            Result.failure(IllegalStateException(mapAuthFailure(null, e.message)))
        }
    }

    private suspend fun postOk(
        path: String,
        body: JsonObject,
        emptySuccessMessage: String,
    ): Result<Unit> {
        return try {
            val response = NetworkFacade.api().postApi(
                path = path,
                body = body.toString(),
                parseData = { _ -> Unit },
            )
            if (!isAuthBusinessSuccess(response.code)) {
                return Result.failure(
                    IllegalStateException(mapAuthFailure(response.code, response.message)),
                )
            }
            Result.success(Unit)
        } catch (e: NetworkError.Transport) {
            Result.failure(IllegalStateException(mapAuthFailure(null, e.message ?: emptySuccessMessage)))
        } catch (e: Throwable) {
            Result.failure(IllegalStateException(mapAuthFailure(null, e.message)))
        }
    }

    private fun commitSession(
        displayName: String,
        userId: String,
        token: String,
        phone: String?,
        email: String?,
    ): Result<Unit> {
        AccountFacade.setSession(
            LoggedInUser(
                displayName = displayName,
                token = token,
                userId = userId,
                avatarUrl = null,
                phone = phone,
                email = email,
                companyId = null,
                orgId = null,
            ),
        )
        AuthSessionState.sync()
        return Result.success(Unit)
    }
}

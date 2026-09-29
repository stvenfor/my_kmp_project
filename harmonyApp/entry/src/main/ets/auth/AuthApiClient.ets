import libentry from 'libentry.so';
import { AuthSession } from './SessionStore';

/**
 * Thin ArkTS façade over Kotlin [AuthBridge] / BFF JSON (kotlinx-serialization).
 * Does not parse BFF responses — Kotlin owns all envelope encode/decode.
 */
function requireLib(): typeof libentry {
  if (libentry === undefined) {
    throw new Error('libentry.so 未加载，无法登录');
  }
  return libentry;
}

function sessionFromKotlin(fallbackPhone: string = ''): AuthSession {
  const lib = requireLib();
  const token = lib.AuthToken();
  if (!token) {
    throw new Error('登录响应缺少 token');
  }
  const userName = lib.AuthDisplayName() || '用户';
  const userId = lib.AuthUserId() || userName;
  const phone = lib.AuthPhone() || fallbackPhone;
  return {
    token,
    refreshToken: '',
    sessionId: '',
    userId,
    userName,
    phone,
    deviceId: 'kmp-ohos-device',
  };
}

export async function loginWithPassword(
  account: string,
  password: string,
): Promise<AuthSession> {
  await requireLib().AuthLoginWithPassword(account.trim(), password);
  return sessionFromKotlin();
}

export async function sendPhoneOtp(phone: string): Promise<void> {
  await requireLib().AuthSendPhoneOtp(phone);
}

export async function loginWithOtp(phone: string, code: string): Promise<AuthSession> {
  await requireLib().AuthLoginWithOtp(phone, code.trim());
  return sessionFromKotlin(phone);
}

export async function registerWithEmail(
  email: string,
  password: string,
  displayName: string = '',
): Promise<AuthSession> {
  await requireLib().AuthRegister(email.trim(), password, displayName.trim());
  return sessionFromKotlin();
}

export async function loginWithHuaweiCode(
  code: string,
  deviceIdArg: string,
): Promise<AuthSession> {
  await requireLib().AuthLoginWithHuawei(code, deviceIdArg);
  return sessionFromKotlin();
}

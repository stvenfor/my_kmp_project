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

export interface LoginMethodInfo {
  id: string;
  displayName: string;
  interactive: boolean;
  available: boolean;
}

/**
 * 当前可用登录方式（Kotlin AuthBridge 输出的 JSON）。
 * N-API 不可用时返回空数组，由登录页决定是否保留占位入口。
 */
export function availableLoginMethods(): LoginMethodInfo[] {
  try {
    const raw = requireLib().AuthAvailableLoginMethods();
    const parsed = JSON.parse(raw) as LoginMethodInfo[];
    return Array.isArray(parsed) ? parsed : [];
  } catch (_e) {
    return [];
  }
}

/**
 * 通用登录：methodId 对应 Kotlin 注册表（huawei / wechat / qq…）。
 * 授权码由原生壳获取，Kotlin 只负责换会话。
 */
export async function loginWithMethod(
  methodId: string,
  code: string,
  deviceIdArg: string = '',
): Promise<AuthSession> {
  await requireLib().AuthLoginWithMethod(methodId, code, deviceIdArg);
  return sessionFromKotlin();
}

import http from '@ohos.net.http';
import {
  BFF_BASE_URL,
  HUAWEI_LOGIN_PATH,
  LOGIN_PATH,
  REGISTER_PATH,
  SEND_PHONE_OTP_PATH,
  VERIFY_PHONE_OTP_PATH,
} from './AuthConfig';
import { AuthSession } from './SessionStore';

interface BffEnvelope {
  code?: number;
  message?: string;
  data?: Record<string, Object>;
}

interface LoginUser {
  id?: string;
  user_id?: string;
  user_name?: string;
  username?: string;
  phone?: string;
  email?: string;
}

function deviceId(): string {
  return 'kmp-ohos-device';
}

function toE164China(phone: string): string {
  const digits = phone.replace(/[^0-9]/g, '');
  if (phone.trim().startsWith('+')) {
    return phone.trim();
  }
  if (digits.startsWith('86') && digits.length === 13) {
    return `+${digits}`;
  }
  return `+86${digits}`;
}

function mapAuthFailure(code: number | undefined, message: string | undefined): string {
  const text = (message ?? '').trim();
  if (code === 10003 || text.indexOf('账号未注册') >= 0 || text.indexOf('请先注册') >= 0) {
    return '账号未注册，请先注册';
  }
  if (code === 10002 || text.indexOf('密码错误') >= 0 || text.indexOf('用户名或密码错误') >= 0) {
    return '密码错误';
  }
  if (text.indexOf('验证码错误') >= 0 || text.indexOf('验证码已失效') >= 0) {
    return '验证码错误或已失效';
  }
  if (text.indexOf('用户已存在') >= 0) {
    return '该邮箱已注册';
  }
  if (text.length > 0) {
    return text;
  }
  return `无法连接服务端（${BFF_BASE_URL}），请确认后端已启动`;
}

async function postJson(path: string, body: Record<string, string>): Promise<BffEnvelope> {
  const url = `${BFF_BASE_URL}${path}`;
  const client = http.createHttp();
  try {
    const resp = await client.request(url, {
      method: http.RequestMethod.POST,
      header: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
      extraData: JSON.stringify(body),
      expectDataType: http.HttpDataType.STRING,
      connectTimeout: 15000,
      readTimeout: 15000,
    });
    const raw = typeof resp.result === 'string' ? resp.result : String(resp.result);
    return JSON.parse(raw) as BffEnvelope;
  } finally {
    client.destroy();
  }
}

function sessionFromLoginData(data: Record<string, Object>, fallbackPhone: string = ''): AuthSession {
  const token = String(data['token'] ?? '');
  if (!token) {
    throw new Error('登录响应缺少 token');
  }
  const user = (data['user'] ?? {}) as LoginUser;
  const username = String(user.username ?? user.user_name ?? '');
  const email = String(user.email ?? '');
  const display = username || email.substring(0, email.indexOf('@') > 0 ? email.indexOf('@') : email.length) || '用户';
  return {
    token,
    refreshToken: String(data['refresh_token'] ?? ''),
    sessionId: String(data['session_id'] ?? ''),
    userId: String(user.id ?? user.user_id ?? display),
    userName: display,
    phone: String(user.phone ?? fallbackPhone),
    deviceId: deviceId(),
  };
}

export async function loginWithPassword(
  account: string,
  password: string,
): Promise<AuthSession> {
  const envelope = await postJson(LOGIN_PATH, {
    username: account.trim(),
    password,
    device_id: deviceId(),
    platform: 'android',
  });
  if ((envelope.code !== 0 && envelope.code !== 200) || !envelope.data) {
    throw new Error(mapAuthFailure(envelope.code, envelope.message));
  }
  return sessionFromLoginData(envelope.data);
}

export async function sendPhoneOtp(phone: string): Promise<void> {
  const envelope = await postJson(SEND_PHONE_OTP_PATH, {
    phone: toE164China(phone),
  });
  if (envelope.code !== 0 && envelope.code !== 200) {
    throw new Error(mapAuthFailure(envelope.code, envelope.message));
  }
}

export async function loginWithOtp(phone: string, code: string): Promise<AuthSession> {
  const envelope = await postJson(VERIFY_PHONE_OTP_PATH, {
    phone: toE164China(phone),
    otp: code.trim(),
    device_id: deviceId(),
    platform: 'android',
  });
  if ((envelope.code !== 0 && envelope.code !== 200) || !envelope.data) {
    throw new Error(mapAuthFailure(envelope.code, envelope.message));
  }
  return sessionFromLoginData(envelope.data, phone);
}

export async function registerWithEmail(
  email: string,
  password: string,
  displayName: string = '',
): Promise<AuthSession> {
  const username = displayName.trim() || email.substring(0, email.indexOf('@')) || email;
  const envelope = await postJson(REGISTER_PATH, {
    username,
    password,
    email: email.trim(),
    device_id: deviceId(),
    platform: 'android',
  });
  if ((envelope.code !== 0 && envelope.code !== 200) || !envelope.data) {
    throw new Error(mapAuthFailure(envelope.code, envelope.message));
  }
  const data = envelope.data;
  const token = String(data['token'] ?? '');
  if (token) {
    return sessionFromLoginData(data);
  }
  return loginWithPassword(email.trim(), password);
}

export async function loginWithHuaweiCode(
  code: string,
  deviceIdArg: string,
): Promise<AuthSession> {
  const url = `${BFF_BASE_URL}${HUAWEI_LOGIN_PATH}`;
  const client = http.createHttp();
  try {
    const resp = await client.request(url, {
      method: http.RequestMethod.POST,
      header: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
      extraData: JSON.stringify({
        code,
        device_id: deviceIdArg,
        platform: 'android',
      }),
      expectDataType: http.HttpDataType.STRING,
      connectTimeout: 15000,
      readTimeout: 15000,
    });
    const raw = typeof resp.result === 'string' ? resp.result : String(resp.result);
    const envelope = JSON.parse(raw) as BffEnvelope;
    if (envelope.code !== 0 || !envelope.data) {
      throw new Error(envelope.message ?? `登录失败 code=${envelope.code}`);
    }
    const data = envelope.data;
    const token = String(data['token'] ?? '');
    if (!token) {
      throw new Error('登录响应缺少 token');
    }
    const user = (data['user'] ?? {}) as LoginUser;
    return {
      token,
      refreshToken: String(data['refresh_token'] ?? ''),
      sessionId: String(data['session_id'] ?? ''),
      userId: String(user.user_id ?? user.id ?? ''),
      userName: String(user.user_name ?? user.username ?? '华为用户'),
      phone: String(user.phone ?? ''),
      deviceId: deviceIdArg,
    };
  } finally {
    client.destroy();
  }
}

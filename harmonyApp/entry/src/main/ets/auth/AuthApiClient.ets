import http from '@ohos.net.http';
import { BFF_BASE_URL, HUAWEI_LOGIN_PATH } from './AuthConfig';
import { AuthSession } from './SessionStore';

interface BffEnvelope {
  code?: number;
  message?: string;
  data?: Record<string, Object>;
}

interface LoginUser {
  user_id?: string;
  user_name?: string;
  username?: string;
  phone?: string;
}

export async function loginWithHuaweiCode(
  code: string,
  deviceId: string,
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
        device_id: deviceId,
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
      userId: String(user.user_id ?? ''),
      userName: String(user.user_name ?? user.username ?? '华为用户'),
      phone: String(user.phone ?? ''),
      deviceId,
    };
  } finally {
    client.destroy();
  }
}

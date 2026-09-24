import { preferences } from '@kit.ArkData';
import { common } from '@kit.AbilityKit';
import { util } from '@kit.ArkTS';

const PREF_NAME = 'auth_session';

export interface AuthSession {
  token: string;
  refreshToken: string;
  sessionId: string;
  userId: string;
  userName: string;
  phone: string;
  deviceId: string;
}

export class SessionStore {
  private pref: preferences.Preferences | undefined = undefined;

  async init(ctx: common.UIAbilityContext): Promise<void> {
    this.pref = await preferences.getPreferences(ctx, { name: PREF_NAME });
  }

  deviceId(): string {
    if (!this.pref) {
      return 'ohos-unknown';
    }
    let id = this.pref.getSync('device_id', '') as string;
    if (!id) {
      try {
        id = util.generateRandomUUID();
      } catch (_e) {
        id = `ohos-${Date.now()}`;
      }
      this.pref.putSync('device_id', id);
      this.pref.flush();
    }
    return id;
  }

  load(): AuthSession | null {
    if (!this.pref) {
      return null;
    }
    const token = this.pref.getSync('token', '') as string;
    if (!token) {
      return null;
    }
    return {
      token,
      refreshToken: this.pref.getSync('refresh_token', '') as string,
      sessionId: this.pref.getSync('session_id', '') as string,
      userId: this.pref.getSync('user_id', '') as string,
      userName: this.pref.getSync('user_name', '') as string,
      phone: this.pref.getSync('phone', '') as string,
      deviceId: this.deviceId(),
    };
  }

  save(session: AuthSession): void {
    if (!this.pref) {
      return;
    }
    this.pref.putSync('token', session.token);
    this.pref.putSync('refresh_token', session.refreshToken);
    this.pref.putSync('session_id', session.sessionId);
    this.pref.putSync('user_id', session.userId);
    this.pref.putSync('user_name', session.userName);
    this.pref.putSync('phone', session.phone);
    this.pref.putSync('device_id', session.deviceId);
    this.pref.flush();
  }

  clear(): void {
    if (!this.pref) {
      return;
    }
    const deviceId = this.deviceId();
    this.pref.clearSync();
    this.pref.putSync('device_id', deviceId);
    this.pref.flush();
  }
}

export const sharedSessionStore: SessionStore = new SessionStore();

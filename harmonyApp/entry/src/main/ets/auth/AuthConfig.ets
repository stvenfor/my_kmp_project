/** BFF 基址：与 my_kmp_project `.env.lan` / LanHost 对齐，换机时改此处。 */
export const BFF_BASE_URL: string = 'http://172.16.0.43:8080';

export const HUAWEI_LOGIN_PATH: string = '/api/v1/user/huawei/login';

/** 华为账号用户认证协议（官方要求可跳转）。 */
export const HUAWEI_USER_AUTH_PROTOCOL: string =
  'https://privacy.consumer.huawei.com/legal/id/authentication-terms.htm?code=CN&language=zh-CN';

/** 占位：应用自有协议，上架前换成正式页。 */
export const APP_USER_SERVICE_URL: string = 'https://example.com/terms';
export const APP_PRIVACY_URL: string = 'https://example.com/privacy';

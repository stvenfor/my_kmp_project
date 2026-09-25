# W0 测试账号与后端（Flutter SoT 对齐）

## Flutter SoT

- 工程：`/Users/mac/Desktop/github/my_ai_project`
- 包名：`com.sample.module_sample`
- 模拟器：可安装运行（与 KMP 同 `emulator-5554`）

## 固定测试账号

| 通道 | 账号 | 凭证 |
|------|------|------|
| 短信 OTP | `13400000000`（E.164：`+8613400000000`） | 验证码 `123456` |
| 邮箱密码 | 视 Go/Supabase 环境 | 非 OTP 主路径 |

- Flutter：`USE_MOCK_AUTH=true` 本地 mock；`false` 走 Go BFF（`docs/USAGE_GUIDE.md`）
- Go：`POST /api/v1/user/phone/otp/send` + `.../otp/verify`（已在 2026-09-25 验证 `code:0` 返回 token）

## KMP

- Base URL 默认：`http://127.0.0.1:8080`（模拟器需 remap 为 `10.0.2.2`，见 `NetworkConfig`）
- 登录页短信 Tab 预填手机号 `13400000000`；验证码手输 `123456`
- **禁止**用 SoftAuth 本地假登录冒充业务完成

## 健康检查

```bash
curl -sS -X POST http://127.0.0.1:8080/api/v1/user/phone/otp/send \
  -H 'Content-Type: application/json' \
  -d '{"phone":"+8613400000000"}'
curl -sS -X POST http://127.0.0.1:8080/api/v1/user/phone/otp/verify \
  -H 'Content-Type: application/json' \
  -d '{"phone":"+8613400000000","otp":"123456","device_id":"emu","platform":"android"}'
```

# 04 — Shared Presentation：会话 + Soft-auth 状态

**What to build:** 共享表现逻辑能表达登录态、会话恢复与 Chat/Community soft-auth 门闸所需状态；行为可在 Shared Presentation Logic 缝上验证，不依赖各端像素实现。

**Blocked by:** 02 — 主路径改走原生壳（切断 Legacy 主路径）

**Status:** done

- [ ] 冷启动会话恢复规则与 Flutter 一致（隐私优先；不强制登录）
- [ ] 未登录时 Chat/Community 门闸状态可观测；登录成功后门闸可清除
- [ ] 针对上述行为有共享层测试（测外部行为，不测控件实现）

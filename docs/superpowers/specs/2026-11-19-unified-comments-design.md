# 统一评论系统设计（帖子 / 视频 / 文章）— 终稿（设计阶段）

> 状态：**待评审**（文档阶段，未改产品代码、未执行 DDL、未写库、未提交）。
> 日期：2026-11-19（第二轮修订，替换前两版）。
> 真相源：`/Users/qpy/Desktop/github/my_go_study`（Go BFF 主仓）与 `/Users/qpy/Desktop/github/my_kmp_project`（KMP 主仓）。
> 本文 Writer 工作区 `orca/workspaces/my_kmp_project/ui-design-foundation` 可能落后，行号均以两个主仓当前文件为准。
> 已读并遵守：两仓 `AGENTS.md`、`karpathy-guidelines`；CodeGraph 优先（KMP 已索引），Go 后端无索引故用 rg 回退。

---

## 0. 修订摘要

### 0.1 第一轮（11 条）已处理

真实文章目标（非 501）、单一评分模型、根容器谓词与墓碑 DTO、全后代 `reply_count` 增量、扁平两层稳定 ID、全量回填（含软删/孤儿）+ Go 侧 UUID、分阶段切换与 fencing、可选认证读、能力位 DTO、409 幂等冲突、按依赖分槽。

### 0.2 本轮（最终正确性 7 条）处理

| 条 | 修正 |
|---|---|
| 1 | `root_count` 从**前后容器谓词**推导：隐藏/删除根获得**第一条**可见回复 0→1、失去**最后一条** 1→0；`score` 永远满足 `2*like_count+reply_count`（含墓碑），隐藏根不清零、不减成负数；隐藏/删除根容器继续按同一 score 参与 hot，latest 用 `created_at`；明确墓碑根 `can_reply=false` 但其**可见后代仍可回复**及状态迁移影响 |
| 2 | 部分唯一 `WHERE root_id IS NULL` **不能**限制置顶仅根：加显式 `CHECK (NOT is_pinned OR (root_id IS NULL AND status=0))`；删除冗余 `subject.pinned_id`，置顶由独立查询 + DB 部分唯一索引表达；删除 `parent_id`（避免 NULL 绕过 CHECK），只用 `root_id`+`reply_to_id`；显式禁自引用 `CHECK`；应用插入不变量写明「根必须 root_id=NULL、引用不可变、无自引用、reply_to 必须同 root」，不再声称 `parent_id=root_id` 能证明同 root |
| 3 | 全篇统一**嵌套游标信封** `cursor:{next,has_more}`，§5/§7/空结果示例完全一致；根列表与后代列表的 `total_visible/total_roots` 语义分别写明 |
| 4 | 删除「反向同步安全回滚」：cutover 后只能回滚到**仍使用 canonical 的兼容版本**，或 fence 写后做前向修复；禁止 destructive down / 降级到 pre-canonical 二进制；cutover 前只有在确认无 canonical 独有数据后才可拆表；回填期加镜像/短 writer fence 做差量补齐；文章章节改为「设计中的真实集成」；孤儿 subject 的 `author_id` 用 **nullable 表示未知，不造 actor**；SQL down 用 marker 表 + `RAISE EXCEPTION` 守卫，不发明 `--allow-destructive` 运行器参数 |
| 5 | 可选认证**复用注入的 `sessionAuth`/`accountGate` 链**（local 或 Supabase），不另写 Supabase 专用校验；缺 Authorization 但有 session 头必须报错；成功沿用现有 `authctx` 上下文键；写链保持严格 |
| 6 | 状态机：作者删除是终态墓碑，admin `unhide` 仅 hidden→visible，隐藏→删除可见增量为 0，动作分别审计；payload hash 用**规范化 JSON / 长度前缀**编码（pipe 拼接有歧义），同 key 同载荷 200、同 key 异载荷 409，客户端重试复用同 key |
| 7 | 收窄为后端身份/URL 由服务端派生；S1 用**附加式接口**保证旧 service 仍编译；构建用 `go -C app`；新增最后一条回复、仅根置顶、精确信封、孤儿保留、回滚守卫等测试用例 |

---

## 1. 现状审计（精确文件 / 行号）

### 1.1 Go（`/Users/qpy/Desktop/github/my_go_study`）

| 层 | 文件:行 | 现状 |
|---|---|---|
| 帖子 | `model/entity/community.go:28` | `WysPost{ID uuid.UUID, UserID string(64), …, LikeCount int, CommentCount int, Heat int64, DeletedAt, CreatedAt, UpdatedAt}`；`wys_posts` |
| 评论 | 同文件 `:74` | `WysPostComment{ID uuid.UUID, PostID uuid.UUID, UserID string(64), Content text, ReplyToNickname *string(100), DeletedAt, CreatedAt}`；无稳定父子 ID / 点赞 / 置顶 / 审核 |
| 帖子点赞 | 同文件 `:64` | `WysPostLike{PostID uuid.UUID, UserID string, CreatedAt}`，PK(post_id,user_id) |
| 评论 DDL | `migrations/20260923167500_align_gorm_only_schema.up.sql:96-106` | 无 FK / 唯一约束 / 排序复合索引 |
| 创建/列表/预览/回填 | `app/internal/community/repo_postgres.go:213 / :233 / :245 / :364` | 事务内加 `comment_count`/`heat`；`created_at ASC` offset；预览 2 条；启动回填 heat |
| DTO / Service / Handler | `app/internal/community/service.go:100 / :494 / :514 / :704`；`handler.go:182 / :192` | 昵称回复；`AuthorsByIDs` 批量；列表 `SuccessList`，创建强鉴权 |
| 路由 / 装配 | `app/internal/community/router.go:43-66`；`app/cmd/api/main.go:506` | lab `allowAnonRead` 只读放行；写严格 |
| 注入认证 | `app/cmd/api/main.go:448-457` | `sessionAuth = LocalSessionAuth(...) 或 SupabaseSessionAuth(...)`；`accountGate = RequireActiveAccount(...)`（local）；均注入各域 Router |
| 共用链 | `app/internal/shared/http.go:10` | `SessionAuthChain(auth, accountGate)` |
| 身份上下文 | `app/internal/delivery/http/authctx/authctx.go:17/65` | `ContextSupabaseUserKey`/`ContextAccessTokenKey`；`SupabaseAuthContext` 要求 user+token 齐全 |
| 响应 / 409 惯例 | `app/internal/delivery/http/dto/response/common.go:28-52`、`points/handler.go:86` | `{code,message,data,timestamp}`；409 用 `http.StatusConflict + response.CodeInvalidParams` |
| 管理员 | `model/entity/access.go:6-11`；`app/internal/access/repo_postgres.go:242` | `platform_admin` + `HasRoleAssignment(..., RolePlatformAdmin, nil)` |
| 视频 | `model/entity/short_video.go:19`；`app/internal/short_video/router.go:44`、`service.go:233` | `ID uuid.UUID`，`GET /:id` 可做存在性校验 |
| 文章 | 当前无 | `rg 'article|文章' model/entity app/internal --glob '*.go'` 零命中；KMP `FeatureRoutes.kt:157` 是直播/好友/音乐/音视频 |
| UUID 扩展 | `migrations/20260921120000_consolidate_users.up.sql:4` | 有 `pgcrypto`，**无** `uuid-ossp`；本设计 Go 侧 `uuid.NewSHA1`，不加扩展 |
| 测试 | `app/internal/community/{service_test.go,like_integration_test.go,router_anon_read_test.go}` | 无评论行为测试 |

### 1.2 KMP（`/Users/qpy/Desktop/github/my_kmp_project`）

| 层 | 文件:行 | 现状 |
|---|---|---|
| 模型 / 请求 | `feature/community/.../http/CommunityPostModels.kt:195 / :397` | `CommentModel{id,post_id,nickname,avatar,content,create_time,reply_to_nickname}`；`CommentCreateRequest{content,reply_to_nickname?}` |
| 仓库 | `.../http/HttpCommunityPostRepository.kt:101 / :109` | `fetchComments` 硬编码 `page=1&size=50`；`addComment` 旧 body |
| UI / 路由 | `.../CommunityRouteHost.kt:750`；`core/router/.../FeatureRoutes.kt:204-207` | 扁平列表、昵称回复、`评论 N = list.size`；无回复/点赞/删除/置顶/展开/分页 |
| Android 壳 | `composeApp/src/androidMain/.../NativeJetpackTabs.kt:1134/1164/1188` | 由昵称构造 `CommunityThreadLine` |
| 契约测试 | `feature/community/src/commonTest/.../HttpCommunityPostRepositoryTest.kt:199/218` | 锁定旧参数；迁移期须保留或显式改写 |

### 1.3 问题

回复关系不可定位、无评论互动/审核、计数只增不减、无幂等、只有 offset、视频/文章无评论、lab 匿名读丢已登录身份、UI 耦合昵称与假总数。

---

## 2. 目标 / 范围

**目标**：一套 subject/comment/like/moderation 表 + 一套 API 真实覆盖三目标；稳定 ID；事务化计数与幂等；可审计审核；旧别名与新入口同走 canonical store。

**新增范围（设计，非本轮实现）**：`wys_articles` + `app/internal/article` 域 + `GET /api/v1/articles/:id` + 可见性/`ExistsVisible` + lab 种子；评论域五表；可选认证读 + 服务端能力位。

**非目标**：本轮不改代码/DDL/数据/提交；不做文章创作后台；不分片；不做图片/@/AI/热词/地理/dislike；不用 mock 认证。

---

## 3. 数据模型

### 3.1 约定

- 用户 ID `varchar(64)`，来自会话，不信任客户端；目标 ID 全为 `uuid`。
- ID 由 **Go 侧** `github.com/google/uuid` 生成；确定性 ID 用 `uuid.NewSHA1(uuid.NameSpaceURL, []byte(ns+":"+type+":"+id))`（现有同型用法 `repo_postgres.go:541`）；**不调用 `uuid_generate_v5`**，不引入 uuid-ossp。
- 计数一律 `bigint`：`comment_count/root_count/like_count/reply_count/score`。
- `target_type varchar(16) CHECK IN ('post','video','article')`，service 归一化。

### 3.2 Mermaid ER

```mermaid
erDiagram
    WYS_POSTS ||--o| WYS_COMMENT_SUBJECTS : "post, 服务层校验"
    WYS_SHORT_VIDEOS ||--o| WYS_COMMENT_SUBJECTS : "video, 服务层校验"
    WYS_ARTICLES ||--o| WYS_COMMENT_SUBJECTS : "article, 服务层校验"
    WYS_COMMENT_SUBJECTS ||--o{ WYS_COMMENTS : "subject_id"
    WYS_COMMENTS ||--o{ WYS_COMMENTS : "(subject_id,root_id) / (subject_id,reply_to_id)"
    WYS_COMMENTS ||--o{ WYS_COMMENT_LIKES : "comment_id"
    WYS_COMMENTS ||--o{ WYS_COMMENT_MODERATION_LOG : "comment_id"

    WYS_COMMENT_SUBJECTS {
        uuid id PK
        varchar16 target_type
        uuid target_id
        varchar64 author_id "NULL=未知(孤儿), 禁止伪造"
        bigint comment_count
        bigint root_count
        timestamptz last_comment_at
        timestamptz created_at
        timestamptz updated_at
    }
    WYS_COMMENTS {
        uuid id PK
        uuid subject_id FK
        uuid root_id FK
        uuid reply_to_id FK
        varchar64 author_id
        text content
        varchar100 legacy_reply_to_nickname
        char64 payload_hash
        varchar64 client_msg_id
        boolean is_author_reply
        boolean is_pinned
        smallint status
        bigint like_count
        bigint reply_count
        bigint score
        timestamptz deleted_at
        timestamptz created_at
        timestamptz updated_at
    }
    WYS_COMMENT_LIKES {
        uuid comment_id PK_FK
        varchar64 user_id PK
        timestamptz created_at
    }
    WYS_COMMENT_MODERATION_LOG {
        bigserial id PK
        uuid comment_id FK
        varchar64 actor_id
        varchar24 action
        varchar255 reason
        timestamptz created_at
    }
    WYS_ARTICLES {
        uuid id PK
        varchar64 author_id
        text title
        text body
        smallint status
        varchar32 source
        timestamptz deleted_at
        timestamptz created_at
        timestamptz updated_at
    }
```

### 3.3 `wys_articles`（文章目标）

`id uuid PK`（Go 侧确定性生成）、`author_id varchar(64) NOT NULL`、`title text NOT NULL CHECK 1..200`、`body text NOT NULL CHECK 1..20000`、`status smallint NOT NULL DEFAULT 0 CHECK IN(0,1,2)`（0 草稿 / 1 发布 / 2 隐藏）、`source varchar(32) NOT NULL DEFAULT 'user'`、`deleted_at/created_at/updated_at timestamptz`。

- 读取 `GET /api/v1/articles/:id`：公开仅 `status=1 AND deleted_at IS NULL`；作者可读自己任意 status；`platform_admin` 全读。
- `article.Service.ExistsVisible(ctx,id,viewerID,isAdmin)` 复用公开可见谓词的**公开子集**：只允许对公开可见文章新建评论；作者/admin 对自己隐藏文章可读但不可评论。
- Lab 种子（后续实现期）：`seedIfLab` 下插 3 条确定性行，`source='lab_seed'`，标题明示"联调样例文章"，作者用现有开发账号；这是授权的实验 DB 数据，不伪装真实创作。

### 3.4 `wys_comment_subjects`

| 列 | 类型 | 空 | 默认 | 约束 / 说明 |
|---|---|---|---|---|
| `id` | `uuid` | N | — | PK；Go 侧 `uuid.NewSHA1` 生成，回填幂等 |
| `target_type` | `varchar(16)` | N | — | CHECK 三值；`UNIQUE(target_type,target_id)` |
| `target_id` | `uuid` | N | — | 同上 |
| `author_id` | `varchar(64)` | **Y** | `NULL` | **孤儿目标（表中已无该行）允许 NULL = 真实作者未知，禁止用占位 actor 伪造**；有目标行时从目标读取 |
| `comment_count` | `bigint` | N | 0 | `CHECK>=0`；可见内容数 |
| `root_count` | `bigint` | N | 0 | `CHECK>=0`；容器数（§4.2 谓词） |
| `last_comment_at` / `created_at` / `updated_at` | `timestamptz` | Y/N/N | NULL/now()/now() | — |

- **无 `pinned_id`**：置顶状态只存在 `comments.is_pinned`，由独立查询 + DB 部分唯一索引表达，避免冗余指针与同 subject FK 难题。
- 无到目标表的物理 FK（多态），存在性由 service 事务内校验；孤儿 subject 保留供历史阅读但拒绝新写。

### 3.5 `wys_comments`

| 列 | 类型 | 空 | 默认 | 约束 / 说明 |
|---|---|---|---|---|
| `id` | `uuid` | N | — | PK |
| `subject_id` | `uuid` | N | — | FK → subjects(id) RESTRICT；唯一标识目标，不冗余 target_type |
| `root_id` | `uuid` | Y | NULL | 根行 NULL；非根行 = 根 ID；复合 FK `(subject_id,root_id) → comments(subject_id,id)` |
| `reply_to_id` | `uuid` | Y | NULL | 真实被回复评论 ID；复合 FK `(subject_id,reply_to_id) → comments(subject_id,id)` |
| `legacy_reply_to_nickname` | `varchar(100)` | Y | NULL | 仅旧数据；新写入 NULL |
| `author_id` | `varchar(64)` | N | — | 会话用户 |
| `content` | `text` | N | — | `CHECK char_length(btrim(content)) BETWEEN 1 AND 2000` |
| `payload_hash` | `char(64)` | N | — | 规范化载荷 SHA-256（§6.1） |
| `client_msg_id` | `varchar(64)` | Y | NULL | 新 API 必填；`UNIQUE(subject_id,author_id,client_msg_id) WHERE client_msg_id IS NOT NULL` |
| `is_author_reply` | `boolean` | N | false | 服务端算 `author_id == subject.author_id`（subject.author_id 为 NULL 时 false） |
| `is_pinned` | `boolean` | N | false | 见下方显式 CHECK |
| `status` | `smallint` | N | 0 | `CHECK IN(0,1,2)`：0 可见 / 1 隐藏 / 2 删除墓碑 |
| `like_count` | `bigint` | N | 0 | `CHECK>=0` |
| `reply_count` | `bigint` | N | 0 | 根下全部可见后代数；**非根恒 0**；`CHECK>=0` |
| `score` | `bigint` | N | 0 | **仅根有效**：恒等式 `score = 2*like_count + reply_count`（含墓碑根），写入时原子维护；**非根 score 恒 0，且 reply_count 恒 0** |
| `deleted_at/created_at/updated_at` | `timestamptz` | Y/N/N | NULL/now()/now() | — |

关键约束：

```sql
-- 仅根可置顶且必须可见（部分唯一索引本身不保证这点）
CHECK (NOT is_pinned OR (root_id IS NULL AND status = 0));
-- 每主体最多一条可见置顶根
CREATE UNIQUE INDEX uq_comments_pinned ON wys_comments (subject_id)
  WHERE is_pinned AND status = 0 AND root_id IS NULL;
-- 结构：去掉 parent_id 后不存在 NULL 绕过；非根的 reply_count 必须为 0
CHECK (
  (root_id IS NULL AND reply_to_id IS NULL)
  OR
  (root_id IS NOT NULL AND reply_to_id IS NOT NULL AND reply_count = 0)
);
-- 禁止自引用（IS DISTINCT FROM 正确处理 NULL）
CHECK (root_id IS DISTINCT FROM id);
CHECK (reply_to_id IS DISTINCT FROM id);
-- 自 FK 同 subject
UNIQUE (id, subject_id);
FOREIGN KEY (subject_id, root_id) REFERENCES wys_comments (subject_id, id);
FOREIGN KEY (subject_id, reply_to_id) REFERENCES wys_comments (subject_id, id);
```

- **无 `parent_id` 列**：扁平两层只用 `root_id` 与 `reply_to_id`；避免 `parent_id IS NULL` 让 CHECK 通过的 NULL 漏洞，也避免与 root_id 双源真相。
- **明确否定旧表述**：`parent_id=root_id` 从未证明 `reply_to_id` 同 root；本版由 service 显式校验 reply_to 的 `root_id`（§4.1），DB 只保证同 subject。
- 应用插入不变量（缺一不可）：① 作为 `root_id` 的评论必须已存在且 `root_id IS NULL` 且同 subject；② `reply_to_id` 必须已存在、同 subject，且 `reply_to.id == root_id` **或** `reply_to.root_id == root_id`；③ 非根不能当根；④ 无自引用；⑤ 关系字段 INSERT 后**永不 UPDATE**（不可变）。违反返回 400/10001。DB 的 CHECK 只挡自引用与结构，不替代 ①② 的服务层校验。
- 索引：`(subject_id, created_at DESC, id DESC) WHERE root_id IS NULL`（latest 容器）、`(subject_id, score DESC, created_at DESC, id DESC) WHERE root_id IS NULL`（hot 容器）、`(subject_id, root_id, created_at ASC, id ASC)`（后代）、`(author_id, created_at DESC)`。

### 3.6 `wys_comment_likes`

`comment_id uuid PK/FK`、`user_id varchar(64) PK`、`created_at`；**无 `reaction` 列**（dislike 是未来新功能，需要另一次 DDL，不做假预留）。幂等 `INSERT ... ON CONFLICT DO NOTHING`，仅新增时 `like_count+1`，**仅当被点赞的评论是根时**才 `score+2`（非根 score 恒 0，不参与评分）；unlike 用 `DELETE ... RETURNING` 反向减（根 `score-2`，非根不动 score）。

### 3.7 `wys_comment_moderation_log`

`id bigserial PK`、`comment_id uuid FK`、`actor_id varchar(64)`、`action varchar(24) CHECK IN ('hide','unhide','pin','unpin','delete','mod_delete')`、`reason varchar(255)`、`created_at`；索引 `(comment_id,created_at DESC)`、`(actor_id,created_at DESC)`。

---

## 4. 关系语义、可见性与计数

### 4.1 扁平两层（无假深度）

根：`root_id=NULL, reply_to_id=NULL`；对根回复：`root_id=根.id, reply_to_id=根.id`；对回复回复：`root_id=最初根.id, reply_to_id=被回复的回复.id`。展示恒两层（根 + 全部后代），"回复 @X" 由服务端按 `reply_to_id` JOIN 的 `reply_to_author` 渲染。旧回复只有昵称：回填不猜 ID，全作根 + `legacy_reply_to_nickname` 保留，UI 标"历史回复"。同 subject/同 root/不可变/无自引用按 §3.5 不变量执行。

### 4.2 根容器谓词

```sql
WHERE c.subject_id = $1 AND c.root_id IS NULL
  AND (c.status = 0 OR EXISTS (
        SELECT 1 FROM wys_comments d
        WHERE d.subject_id = c.subject_id AND d.root_id = c.id AND d.status = 0))
```

`C(r)` 记为该谓词对根 r 的真值；`subject.root_count = count(C(r)=true)`。`status=1/2` 根仅当仍有可见后代才作容器。

### 4.3 墓碑 / 隐藏 DTO 与回复能力

| 状态 | `content_state` | `content` | `author` | `can_reply` |
|---|---|---|---|---|
| 0 可见 | visible | 真实正文 | 完整 | 已登录 + 目标可见时 true |
| 1 隐藏 | hidden | **空串**（公开 DTO 永不下发正文） | 保留 nickname/avatar | **false** |
| 2 删除 | deleted | **空串** | 身份清空 | **false** |

- 墓碑/隐藏正文永不通过公开 DTO 返回；审核者看原文需独立 audit 端点（后续）。
- **可见后代仍可回复**：`can_reply` 由**被回复的那条评论**决定，不由根决定；隐藏/删除根下 status=0 的后代 `can_reply=true`，可继续产生回复（这些回复的 `root_id` 仍指向墓碑根，容器的 C(r) 因此保持 true）。
- 状态迁移：根隐藏/删除 → 根 `can_reply=false`，后代不变；后代被隐藏/删除 → 该后代 `can_reply=false`；`unhide` 仅恢复 hidden→visible 的 `can_reply`；作者删除不可逆。

### 4.4 计数口径

- `total_visible` = 可见内容条数（status=0 根 + 后代）= `subject.comment_count`。
- `total_roots` = 容器数 C(r)=true = `subject.root_count`。
- 根 `reply_count` = 该根下全部可见后代数 = `count(d.root_id=根.id AND d.status=0)`。
- 后代列表信封：`total_visible` = 该根可见后代数（=`root.reply_count`），`total_roots = 0`（该视图无根容器）。
- legacy `pagination.total` = `total_visible`。UI 总数用 `total_visible`，空态用 `total_roots`，"展开 N 条回复"用 `reply_count`；都不用 `list.size`。

### 4.5 增量表（原子单事务；仅状态实际变化才应用）

`root_count` **不写死 ±0/±1**：每次事务对受影响根计算 `before = C(r)`、`after = C(r)`，取 `after - before`。`score` 每时刻都按 `2*like_count+reply_count` 重算/增量维护，**不清零、不为负**。

| 事件 | `comment_count` | `root_count`(按 C 差值) | `last_comment_at` | 根 `reply_count` | 根 `score` | `like_count` | 置顶 |
|---|---|---|---|---|---|---|---|
| 创建根 | +1 | +1（新根可见 C=true） | now | 0 | 0 = 2*0+0 | 0 | — |
| 创建回复（对根/对后代） | +1 | 若根原 C=false 且这是**第一条**可见后代 → +1，否则 0 | now | **+1** | **+1**（恒等式） | 0 | — |
| like 根 | 0 | 0 | — | 0 | **+2** | +1 | — |
| unlike 根 | 0 | 0 | — | 0 | **−2** | −1 | — |
| like/unlike 后代 | 0 | 0 | — | 0 | 0 | ±1 | — |
| 隐藏可见后代 | −1 | 若根变 C=false（失去**最后**可见后代）→ −1，否则 0 | — | **−1** | **−1** | 保留 | — |
| unhide 后代 | +1 | 若根由 C=false→true → +1，否则 0 | — | **+1** | **+1** | 保留 | — |
| 删除可见后代（墓碑） | −1 | 同"隐藏后代"（按 C 差值） | — | **−1** | **−1** | 保留 | — |
| 隐藏可见根 | −1 | 若该根无可见后代 → −1，否则仍 C=true → 0 | — | 0（不变） | **不变**（仍 2*like+reply） | 保留 | 清除 |
| unhide 隐藏根 | +1 | after C − before C；原有可见后代时0，否则+1 | — | 0（不变） | 不变 | 保留 | — |
| 作者删除可见根（终态） | −1 | 若无可见后代 → −1，否则 0 | — | 0（不变） | 不变 | 保留 | 清除 |
| admin 隐藏→删除根 | **0**（两者都非可见） | **0**（C 不变） | — | 0 | 不变 | 保留 | 已清 |
| pin/unpin | 0 | 0 | — | 0 | 0 | 0 | 仅可见根，DB 保证唯一 |

- 隐藏/删除根**继续按同一 score 参与 hot 排序**（score 仍满足恒等式）；latest 用 `created_at` 排序；置顶仅 visible 根。规则全篇一致，不出现"隐藏后清零再减成负数"。
- 所有 Δ 仅在 `INSERT ... ON CONFLICT DO NOTHING RowsAffected>0` 或 `UPDATE ... WHERE status<>new` 成功时应用；幂等重放不加计数。
- **锁顺序**：`subjects(id) FOR UPDATE` → 根评论 `FOR UPDATE` → 目标评论 → likes；禁止反向取锁。
- 对账：comment_count 对 `count(status=0)`；root_count 对 C(r) 谓词行数；reply_count 对 `count(root_id=? AND status=0)`；score 对 `2*like_count+reply_count`。cutover 后不与冻结 legacy 比计数（§9.3）。

---

## 5. 查询 / 分页 / 唯一信封

### 5.1 根列表

- `pinned` 独立通道：`WHERE subject_id=? AND root_id IS NULL AND is_pinned AND status=0`，至多 1（DB 唯一索引）；`list` 用 §4.2 容器谓词且 `is_pinned=false` 分页。游标集合从不含置顶，置顶项不造成页内重复；分页中被置顶的根移到 `pinned`，客户端按 `pinned.id` 去重。
- `sort=latest` 键 `(created_at DESC, id DESC)`；`sort=hot` 键 `(score DESC, created_at DESC, id DESC)`；**唯一 score 模型，无定时任务**。
- 游标 base64 不透明；`limit` 默认 20 / 上限 50；`has_more = (取回 limit+1 条)`。
- **全篇唯一信封**（§5、§7、测试都按此）：

```json
{
  "code": 0, "message": "success", "timestamp": 1752998400,
  "data": {
    "list": [],
    "pinned": null,
    "cursor": { "next": null, "has_more": false },
    "total_visible": 0,
    "total_roots": 0
  }
}
```

  字段**不省略**；根列表 `total_visible/total_roots` 按 §4.4；后代列表 `pinned:null`、`total_visible=根.reply_count`、`total_roots=0`。

### 5.2 可变分数导致的漏项 / 重复

`score` 随写即时变化：分页中 score 下降的根可能再次满足游标条件 → **重复**，客户端按 id 去重；score 上升的根移到游标前 → **漏项**，hot 语义固有代价。latest 用不可变 `(created_at,id)` 严格不漏不重。置顶跨通道移动用 `pinned.id` 去重。强一致 hot 需另做物化榜单（新 DDL），不在本期。

### 5.3 批量装配

后代 `reply_to_id IN (...)` 一次 JOIN 作者；作者信息批量 `AuthorsByIDs`（沿用 `service.go:704`）；点赞态一次 `comment_id IN (...) AND user_id=?`；预览 2 条同样批量。禁止逐条查库。

---

## 6. 事务 / 幂等 / 认证 / 权限

### 6.1 幂等与冲突

- 新 API 创建**必填** `client_msg_id`；旧别名可空（兼容风险记录）。
- `payload_hash = SHA256(canonical_json({target_type,target_id,content,root_id,reply_to_id,author_id}))`：**键排序、UTF-8、定长编码**的规范化 JSON（或逐字段长度前缀 `len:value` 拼接）；**禁止 `a|b|c` 裸拼接**（内容含 `|` 会歧义）。
- 同 `(subject_id,author_id,client_msg_id)`：hash 相同 → 返回原 DTO（200）；hash 不同 → **409 + `response.CodeInvalidParams`（10001 既有惯例）**，body 带 `conflict_with: 原 id`。
- 客户端**重试/超时复用同一 key**；只有用户新起一次撰写才生成新 key。DB 部分唯一索引做最终防线，并发冲突转译为上述语义。

### 6.2 可选认证读（复用注入链）

- 新增 `middleware.OptionalSessionAuth(chain ...gin.HandlerFunc)`，`chain` 就是装配层已注入的 `sessionAuth`（local 或 Supabase，见 `main.go:448-457`）与可选 `accountGate`（`shared.SessionAuthChain` 同样元素）：
  - **无 Authorization 且无 `X-Session-ID`/`X-Device-ID`** → `c.Next()`，上下文无身份，`viewerID=""`；
  - **无 Authorization 但有 session 头** → 直接 abort **401/10023**（身份不完整，禁止猜测或降级）；
  - **有 Authorization** → 依次执行注入的 chain；任一失败按其既有错误码 abort（token 10023 / session 10022/10021 / 账号门 10003）；成功沿用现有 `authctx` 键（`ContextSupabaseUserKey`/`ContextAccessTokenKey`），**不新增上下文键、不重写 JWT/session 校验逻辑**。
- lab `allowAnonRead` 的读组使用 OptionalSessionAuth；写组始终严格 `shared.SessionAuthChain(sessionAuth, accountGate)`。
- 禁止 mock token / mock user；签名用户在 lab 下 `viewerID` 正常，`is_liked/is_mine` 与能力位按真实身份计算。

### 6.3 权限

自删 `author_id==viewerID`；回复/点赞需登录且目标可见；置顶仅可见根且 `viewerID==subject.author_id` 或 `platform_admin`；隐藏/恢复/admin 删除仅 `platform_admin`（`repo_postgres.go:242`），建议新增 `PermCommentModerate`/`PermCommentPin` 到 `model/entity/access.go` 并经 role_permission 迁移授权（本轮不做）。能力位全部服务端派生（§7）。

---

## 7. API / DTO 契约（信封与 §5 完全一致）

统一 `/api/v1/comments`；旧 `/api/v1/community/posts/:id/comments` 在 cutover 后也走 canonical service。

| 方法 | 路径 | 请求 | 响应 `data` |
|---|---|---|---|
| GET | `/api/v1/comments?target_type=&target_id=&sort=latest|hot&cursor=&limit=` | query | `{list:[CommentDTO],pinned:CommentDTO|null,cursor:{next,has_more},total_visible,total_roots}` |
| GET | `/api/v1/comments/:id/replies?cursor=&limit=` | query | 同结构，`pinned:null`，`total_visible=该根 reply_count`，`total_roots=0` |
| POST | `/api/v1/comments` | `{target_type,target_id,content,reply_to_id?,client_msg_id}` | 201 `CommentDTO`；幂等 200；冲突 409 |
| POST/DELETE | `/api/v1/comments/:id/like` | 空 | `{id,like_count,is_liked}` |
| DELETE | `/api/v1/comments/:id` | — | `{id,status:'deleted'}` |
| POST/DELETE | `/api/v1/comments/:id/pin` | — | `{id,is_pinned}`（仅可见根） |
| PATCH | `/api/v1/comments/:id/moderation` | `{action:'hide'|'unhide',reason?}` | `{id,status}`；admin；不复活已删除 |
| GET | `/api/v1/articles/:id` | query | 文章 DTO（公开 status=1） |
| 兼容 | `GET|POST /api/v1/community/posts/:id/comments` | 旧 `{content,reply_to_nickname?}` | 旧字段形状，内部映射 canonical |

`CommentDTO`：`id, subject_id, target_type, target_id, root_id, reply_to_id, author{user_id,nickname,avatar,is_author}, content, content_state, legacy_reply_to_nickname, reply_to_author, is_pinned, is_author_reply, like_count, reply_count, is_liked, can_reply, can_like, can_delete, can_pin, can_moderate, created_at, updated_at` + 兼容期旧字段。

能力位来源：`can_reply = viewerID!="" && 目标可见 && 本条 content_state=visible`（**墓碑根的可见后代仍为 true**）；`can_like = viewerID!="" && 本条 content_state=visible && 目标可访问/可见`（隐藏/删除目标下的评论不可点赞）；`can_delete = content_state!="deleted" && ((viewerID==author_id) || admin)`——已删除评论**不能再次删除**（再次 `DELETE` 仅作幂等成功空操作：返回原墓碑 DTO，不改状态/计数/审计），且 `is_admin` 也**不得复活已删除内容**（admin 的 `unhide` 仅 hidden→visible，见 §9/§11）；`can_pin = root_id==NULL && visible && (viewerID==subject.author_id || admin)`；`can_moderate = admin`。

---

## 8. Android / KMP UX（后端真实能力）

白色圆角 Material3 `ModalBottomSheet`（`gradle/libs.versions.toml:22/131`），总数用 `total_visible`，空态用 `total_roots`，置顶用 `pinned` 通道，展开用 `reply_count`；墓碑按 `content_state` 显示占位；能力位驱动按钮；`imePadding()+navigationBarsPadding()`；失败不乐观插入；`AnimatedVisibility` 120–200ms。

**身份与 URL 全部后端派生**：昵称/头像/作者标记/被回复者均来自 DTO（服务端批量查 DB），客户端不生成用户身份、不拼猜测的头像/内容 URL、不自行判断 admin 或作者；无后端能力就不给入口（图片/@/AI/热词/地理/dislike）。

---

## 9. 迁移 / 回填 / 分阶段 / 回滚 / 文章

### 9.1 迁移（只提案）

up：五表 `IF NOT EXISTS` + 索引 + CHECK + 部分唯一；新增 GORM 实体；`main.go` autoMigrate 登记。回填：

1. subject 覆盖 **所有** `wys_posts`（**含 deleted_at 非空**）、**所有** `wys_short_videos`（含隐藏/软删）、`wys_articles`；不按可见性过滤。`author_id` 从目标行取；目标不存在（孤儿）时 **`author_id=NULL`（真实未知）**，不填占位 actor。Go 侧 `uuid.NewSHA1` 生成 ID，`ON CONFLICT DO NOTHING`。
2. comment 复制 **全部**旧行（**含 deleted_at 非空** → status=2），不按可见性过滤；旧 `reply_to_nickname` 进 `legacy_reply_to_nickname`；`root_id/reply_to_id=NULL`；`client_msg_id=NULL`；hash 用规范化载荷算。
3. 按 §4.5 口径回算 `comment_count/root_count/reply_count/score`；孤儿目标保留 subject 但写返回 404。
4. `wys_post_likes` 是帖子点赞，**禁止**映射评论点赞；评论 like 从 0 起。
5. 回填前后行数核对（旧评论总数 = 新 comments 总数），不通过中止。
6. **旧内容异常（空白 / 超长）**：legacy 行可能存在 `content` 为空白或超过新 `CHECK (1..2000)` 的情况。**禁止静默截断，也禁止因个别异常行让整批回填失败**。采用显式 legacy-safe 规则：满足新 CHECK 的正常复制；不满足的原样写入隔离表 `wys_comment_backfill_exceptions`（保留完整原始正文 / post_id / user_id / 原因 / 原始 ID），不计入 canonical comments，待人工裁定；正常行数 + 隔离行数必须等于旧评论总数。cutover 前输出异常行清单并要求**零未归因丢失**（每行都要能在 canonical 或隔离表中找到），否则不得切换。

**回填期不丢写**：从阶段 B 起对旧表写入开启**同事务镜像**到 canonical；切到阶段 C 前加**短 writer fence**，做最后一轮差量回填 + 计数校验，确认无遗漏后才允许读 canonical。不能"回填完隔很久再切"导致期间写入丢失。

> 实现说明：本轮落地的是**一次性 + 幂等重跑**的镜像（`BackfillUnifiedComments`），
> 连续同事务镜像尚未实现。B→C 与 C→D 都在同一原子事务内做最后补差 + 校验，
> C 阶段新进的 legacy 写不会被镜像，因此 cutover 前必须重跑补差（见 runbook §5）。

### 9.2 历史回复

不猜 `reply_to_id`；旧串保留并显示"历史回复"，无楼层跳转；新回复必须用稳定 ID。

### 9.3 分阶段与回滚（去掉反向同步）

| 阶段 | 开关 | 读 | 写 | 数据 |
|---|---|---|---|---|
| A expand | canonical_read=false, canonical_write=false | 旧 | 旧 | 新表空 |
| B backfill | 同上 | 旧 | 旧 + 同事务镜像 canonical | 全量回填 + 持续镜像 |
| C shadow | canonical_read=false, canonical_write=false（shadow-only；仅内部核对） | 旧 | 旧 | 短 fence 做差量补齐与校验；统一 API 仍 503 |
| D cutover | canonical_read=true, canonical_write=true | **全部**新旧入口读 canonical | **全部**新旧入口（含旧别名）写 canonical；旧表 trigger 拒新写 = writer fencing | legacy 冻结，canonical 唯一权威 |
| E settle | 同 D | canonical | canonical | 归档/删旧表需单独人工批准 |

> 实现说明（S2 收口）：本轮 C 阶段刻意收窄为 **shadow-only**——`canonical_read`
> 只在 D 才为 true，public 读（feed 预览、comment_count、旧别名）与写全部留在
> legacy，统一 API `/api/v1/comments` 在 C 继续返回 503；canonical 仅供内部核对，
> 不对外权威。这样 public 流量在 D 之前保持单一路径，D 才原子切换。阶段元数据
> 损坏/缺失（组合非法、表缺失但 canonical 已存在、查询失败）时读门 fail closed，
> 绝不静默回退 legacy 读出 canonical 已隐藏/删除的正文。

- **回滚**：
  - D 之前：关开关回 A/B/C；仅当查询确认**无 canonical 独有数据**才可拆新表（有则先保留备份，不静默丢弃）。
  - D 之后：**禁止** reverse-sync "安全回滚"（legacy 无法承载视频/文章目标与回复/点赞/审核结构）。可行项只有：① 回滚到**仍使用 canonical store 的兼容版本**（保留新数据结构，只回退 UI/读逻辑）；② fence 写入后做**前向修复/数据订正**。绝不 destructive down、绝不降级到 pre-canonical 二进制（会让 canonical 独有写入不可见或丢失）。
- **SQL 守卫**：down 迁移开头用 marker 表（如 `comment_migration_state(stage text)`）做 `DO $$ ... IF EXISTS(stage IN ('C','D','E')) THEN RAISE EXCEPTION 'refuse destructive down after shadow/cutover' ... END $$;`，并提供 DBA runbook（确认阶段、备份、人工审批、手工执行 SQL）。**不存在 `--allow-destructive` 这类运行器参数**，文档不发明 CLI flag。

### 9.4 文章接入（设计中的真实集成，非 501；本轮不实现）

现状仓库无文章。设计包含 `wys_articles` + `app/internal/article` 四件（repo/service/handler/router）+ `GET /api/v1/articles/:id` + 可见性与 `ExistsVisible` + lab 种子；评论复用同一套 subject/comment 代码，**不写第二套评论系统**。文章创作/编辑后台明确不在本期。

---

## 10. Redis

首期不引入缓存；后续仅热点计数/score 快照短 TTL（30–60s）且写路径失效；不分片，除非实测瓶颈。

---

## 11. 测试 / 验收（按阶段收窄）

**Go（app 模块）**：`go -C app test ./internal/community/... ./internal/article/... ./internal/delivery/http/router/...`；仅 `main.go` 变更时 `go -C app build ./cmd/api`（不跑无关大范围构建；workspace-root 等价形式 `go test ./app/internal/...` 亦可用）。

新增用例（stub/integration）：
- 稳定 ID、同 subject **且同 root**、无自引用、不可变关系；
- **最后一条回复往返**：隐藏/删除根的最后可见后代使 root_count 1→0，unhide/新增使 0→1；score 恒等于 `2*like_count+reply_count`，**永不为负**；隐藏根仍有后代时继续参与 hot；
  - **unhide 隐藏根（已有可见后代）**：C 前后均为 true，`root_count` **不变**；
  - **unhide 隐藏根（无可见后代）**：C false→true，`root_count` **+1**；
- **DB 仅根可置顶**：CHECK 拒非根/隐藏根置顶，部分唯一索引每主体一根；
- **精确信封**：§5 完整 JSON（字段名、嵌套 cursor、空值），根/后代两种 total 语义；
- 幂等 hash（规范化 JSON、含分隔符内容不歧义）、同 key 同载荷 200 / 异载荷 409；
- 作者删除终态不可复活、admin unhide 仅 hidden→visible、hidden→deleted 可见增量 0、审计动作区分；
- 可选认证：无头匿名、有 session 头无 Authorization 报错、有头走**注入链**（local 与 Supabase 两条）、坏 token 401、签名 viewer 能力位保留；
- **孤儿 target/subject**：subject.author_id=NULL、旧评论完整保留、写返回 404；
- **回滚守卫**：marker 在 C/D/E 时 down 抛异常；镜像 + fence 差量无遗漏；
- 文章可见性与 `ExistsVisible`、旧别名兼容、anon 401/200 既有测试保持。

**KMP**：`./gradlew :feature:community:testDebugUnitTest`（新 DTO/信封/能力位/游标/文章与视频 target）；`./gradlew :composeApp:compileDebugKotlinAndroid`（仅 Android）。MockCommunityEngine 测试可留但生产不引用。

**设备 / UX** 由用户检查，不列为交付验收项。

**S1 可编译性**：S1 必须**附加式**扩展——新增 `CommentStore` 接口由 `*PostgresRepository` 实现，不修改既有 `Repository` 接口方法集，不删除/改签名；`Service` 在 S2 才切到新接口。这样 S1 单独交付时旧 service、现有 stub 测试（`router_anon_read_test.go`）仍编译通过，不会出现"等 S2 才能编译"的破 callsite。

---

## 12. 阶段与三槽（按真实依赖）

WIP≤3、槽位钉死、依赖未验收不派、同阶段任务互不依赖、worker 不提交（Go `AGENTS.md` 三槽硬规则）。

| 阶段 | 槽 | Slice | 文件白名单 | 依赖 |
|---|---|---|---|---|
| 1 | A | S1 目标 + schema + 附加式仓储 | `model/entity/{community.go,article.go}`；migration；`app/internal/community/{repository.go(新增 CommentStore),repo_postgres.go}`；`app/internal/article/{repository.go,repo_postgres.go,EnsureSeed}` | 无；**单独可编译** |
| 2 | B | S2 Service/DTO/Handler/Router/认证/权限 | `app/internal/community/{service.go,handler.go,router.go}`；`app/internal/article/{service.go,handler.go,router.go}`；`app/internal/delivery/http/middleware`(OptionalSessionAuth)；`dto/response/common.go`；`main.go`；`model/entity/access.go` | S1 |
| 3 | A | S3 KMP 契约层 | `feature/community/.../http/{CommunityPostModels.kt,CommunityPostRepository.kt,HttpCommunityPostRepository.kt}`、`CommunityScreen.kt` + commonTest | S2 |
| 3 | C | S4 兼容别名 + 路由护栏 + 对账 + 迁移 runbook | `router_anon_read_test.go`、`domain_routes_test.go`、新增 `comment_compat_test.go`、`app/cmd/comment-reconcile/*`、`docs/.../acceptance-records/*` | S1/S2；与 S3 文件不重叠 |
| 4 | B | S5 KMP UI + Android 壳 | `CommunityRouteHost.kt`、`NativeJetpackTabs.kt`、（如需）`ImmersiveInsets.kt` | S3 |
| 4 | C | S6 加固测试（与 S5 文件不重叠） | feature/community commonTest 新增用例 + 编译记录 | S3；不依赖 S5 |

每阶段只跑本阶段相关包的测试/编译；不存在同阶段内 A→B 或 B→C 依赖。

---

## 13. 默认值（不阻塞）

唯一 score = `2*like_count+reply_count` 且含墓碑；hot best-effort + id 去重，置顶独立通道；无 parent_id/dislike/深度钳制；计数 bigint；墓碑正文永不下发、可见后代可回复；唯一嵌套游标信封；新 API 必填 client_msg_id + 规范化 hash + 409；文章为设计中的真实集成（本轮不实现），无创作后台；孤儿 author_id=NULL；可选认证复用注入链；cutover 后只回退到 canonical 兼容版本；首期无缓存/分片。

---

## 14. 开放风险

1. 多态 subject 无物理 FK，靠服务层校验 + 孤儿保留；DB 级完整性需分表/触发器。
2. hot 严格分页不可能，接受 best-effort；强一致需物化榜单。
3. 旧 KMP 测试写死 `page=1&size=50`：兼容别名先落地再切。
4. 历史回复拍平为根：可见旧串但无楼层跳转，需产品接受。
5. cutover 后 legacy 冻结：回滚只能到兼容版本；down 被 marker 守卫拒绝。
6. 审核权限依赖 `role_permission` 新授权；未授予则 admin 操作 403（安全失败）。

---

> 本文件仅为设计评审材料；用户批准前不得据此改产品代码、执行 DDL、写库或提交。

package com.example.my_kmp_project.feature.community

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.my_kmp_project.core.uuid.AppUuid
import com.example.my_kmp_project.feature.community.http.HttpUnifiedCommentRepository
import com.example.my_kmp_project.feature.community.http.UnifiedComment
import com.example.my_kmp_project.feature.community.http.UnifiedCommentContentState
import com.example.my_kmp_project.feature.community.http.UnifiedCommentError
import com.example.my_kmp_project.feature.community.http.UnifiedCommentLike
import com.example.my_kmp_project.feature.community.http.UnifiedCommentModerationAction
import com.example.my_kmp_project.feature.community.http.UnifiedCommentPage
import com.example.my_kmp_project.feature.community.http.UnifiedCommentRepository
import com.example.my_kmp_project.feature.community.http.UnifiedCommentSort
import com.example.my_kmp_project.feature.community.http.UnifiedCommentTargetType
import kotlinx.coroutines.CancellationException

/**
 * 统一评论区状态机（帖子 / 视频 / 文章共用）。
 *
 * 约束（见设计 §8）：
 * - 列表 / 总数 / 能力位全部来自 [UnifiedCommentRepository] 的服务端回包；**不用列表长度推总数**，
 *   不伪造身份 / 时间 / 计数。写操作成功后总数仍只取下一次 `refresh` 的信封值。
 * - 根列表用单调 `rootGeneration`，回复线程用控制器级单调 `replyEpoch` 做请求身份：
 *   目标 / 排序 / 刷新推进根代次，任何线程重建都不会复用旧请求身份（杜绝 ABA）。
 * - 普通刷新会把保留的回复线程置为「未加载」并重载展开线程（折叠线程按需懒加载），
 *   同时按服务端数据复核当前回复目标；目标失效时阻塞提交，**绝不静默降级为根评论**。
 * - 分页只在「有更多 + 游标变化」时追加；同一游标 / 空页立即停止；按 ID 合并，同 ID 用最新 DTO
 *   原地替换。置顶通道以每页 `pinned` 为准：变化即重启首屏，绝不残留旧置顶。
 * - `client_msg_id` 由控制器唯一持有：同一份载荷（正文 + 回复对象）重试复用同一 key；
 *   任何编辑 / 改回复对象都作废 key（改走又改回来也换新 key）。409 冲突进入显式可恢复阻塞，
 *   只允许用户改动内容 / 目标后重发，不自动换 key 重试。
 * - 写操作失败保留原文、草稿与已展示数据；服务端已确认的删除 / 隐藏立即本地生效
 *   （失败刷新也不会继续展示被删评论的操作位）。
 */
public class UnifiedCommentController(
    private val repository: UnifiedCommentRepository,
    public val targetType: UnifiedCommentTargetType,
    public val targetId: String,
    private val newClientMsgId: () -> String = { AppUuid.random() },
    private val pageSize: Int = DEFAULT_PAGE_SIZE,
) {
    public data class ReplyThread(
        val items: List<UnifiedComment> = emptyList(),
        val expanded: Boolean = false,
        val loaded: Boolean = false,
        val loading: Boolean = false,
        val loadingMore: Boolean = false,
        val error: UnifiedCommentError? = null,
        val loadMoreError: UnifiedCommentError? = null,
        val hasMore: Boolean = false,
        val totalVisible: Long = 0L,
        val cursor: String? = null,
        /** 控制器级单调请求身份；0 表示「无在途请求」。 */
        val requestId: Int = 0,
    )

    /** 当前撰写所回复的评论（用于回复上下文与取消）。 */
    public data class ReplyContext(
        val commentId: String,
        val nickname: String,
    )

    private data class PendingSubmission(
        val content: String,
        val replyToId: String?,
        val clientMsgId: String,
    )

    // —— 根列表 ——
    public var sort: UnifiedCommentSort by mutableStateOf(UnifiedCommentSort.Latest)
        private set
    public var pinned: UnifiedComment? by mutableStateOf(null)
        private set
    public var roots: List<UnifiedComment> by mutableStateOf(emptyList())
        private set
    /** 服务端 `total_visible`（含回复），UI 总数只用它。 */
    public var totalVisible: Long by mutableStateOf(0L)
        private set
    /** 服务端 `total_roots`（容器根数），空态判定用它。 */
    public var totalRoots: Long by mutableStateOf(0L)
        private set
    /** 首屏 / 无内容时的加载态；已有内容的后台刷新不置位，避免列表闪空。 */
    public var firstLoading: Boolean by mutableStateOf(false)
        private set
    /** 下拉刷新态（含首屏）；驱动 PullToRefresh 指示器。 */
    public var refreshing: Boolean by mutableStateOf(false)
        private set
    /** 刷新失败。已有内容时 UI 以横幅提示并保留列表。 */
    public var refreshError: UnifiedCommentError? by mutableStateOf(null)
        private set
    public var loadingMore: Boolean by mutableStateOf(false)
        private set
    public var loadMoreError: UnifiedCommentError? by mutableStateOf(null)
        private set
    public var hasMore: Boolean by mutableStateOf(false)
        private set
    /** 读接口 503：整个面板进入「暂未开放」态。 */
    public var notEnabled: Boolean by mutableStateOf(false)
        private set
    public var replyThreads: Map<String, ReplyThread> by mutableStateOf(emptyMap())
        private set

    // —— 撰写 ——
    public var draft: String by mutableStateOf("")
        private set
    public var replyTo: ReplyContext? by mutableStateOf(null)
        private set
    /** 服务端刷新后原回复目标已删除 / 隐藏 / 不可回复：保留上下文但阻塞提交。 */
    public var replyToStale: Boolean by mutableStateOf(false)
        private set
    public var submitting: Boolean by mutableStateOf(false)
        private set
    public var composeError: UnifiedCommentError? by mutableStateOf(null)
        private set
    public var validationError: String? by mutableStateOf(null)
        private set
    /** 写操作 401：展示登录引导（读仍可用）。 */
    public var authRequired: Boolean by mutableStateOf(false)
        private set
    /** 点赞 / 删除 / 置顶 / 审核失败的安全中文提示。 */
    public var actionError: String? by mutableStateOf(null)
        private set
    public var actionBusyIds: Set<String> by mutableStateOf(emptySet())
        private set

    private var rootCursor: String? = null
    private var rootGeneration: Int = 0
    private var replyEpoch: Int = 0
    private var pendingSubmission: PendingSubmission? = null
    /** 409 命中的载荷：内容 / 回复对象回到同一组合仍拒绝，只有换内容或换目标才解除。 */
    private var conflictedSubmission: PendingSubmission? = null

    /** 列表是否为空（用服务端 `total_roots`，不是 `roots.size`）。 */
    public val isEmpty: Boolean
        get() = !firstLoading && refreshError == null && pinned == null && roots.isEmpty() && totalRoots == 0L

    public fun updateDraft(value: String) {
        if (value == draft) return
        draft = value
        // 任何编辑都开启一次新操作：旧的幂等 key 作废；冲突载荷本身仍保留用于比对。
        pendingSubmission = null
        if (validationError != null) validationError = null
    }

    public fun clearActionError() {
        actionError = null
    }

    public suspend fun refresh() {
        val generation = ++rootGeneration
        if (pinned == null && roots.isEmpty()) firstLoading = true
        refreshing = true
        refreshError = null
        loadMoreError = null
        // 推进代次后旧分页请求的 finally 不会再生效，必须在这里清掉它持有的忙碌位。
        loadingMore = false
        try {
            val page = repository.fetchRoots(targetType, targetId, sort, pageSize, cursor = null)
            if (generation != rootGeneration) return
            applyFirstPage(page)
            notEnabled = false
            reconcileReplyThreads()
        } catch (e: CancellationException) {
            throw e
        } catch (e: UnifiedCommentError) {
            if (generation != rootGeneration) return
            if (e is UnifiedCommentError.NotEnabled) notEnabled = true
            refreshError = e
        } catch (e: Exception) {
            if (generation != rootGeneration) return
            refreshError = UnifiedCommentError.Unavailable(e)
        } finally {
            if (generation == rootGeneration) {
                firstLoading = false
                refreshing = false
            }
        }
    }

    public suspend fun changeSort(next: UnifiedCommentSort) {
        if (next == sort) return
        sort = next
        rootGeneration++
        rootCursor = null
        hasMore = false
        loadingMore = false
        loadMoreError = null
        pinned = null
        roots = emptyList()
        totalVisible = 0L
        totalRoots = 0L
        replyThreads = emptyMap()
        refreshError = null
        refresh()
    }

    public suspend fun loadMore() {
        if (loadingMore || !hasMore) return
        val cursor = rootCursor ?: return
        val generation = rootGeneration
        loadingMore = true
        loadMoreError = null
        try {
            val page = repository.fetchRoots(targetType, targetId, sort, pageSize, cursor)
            if (generation != rootGeneration) return
            val previousPinned = pinned
            val pagePinned = page.pinned?.takeIf { it.id.isNotEmpty() }
            if (pagePinned?.id != previousPinned?.id) {
                // 置顶被替换 / 取消：游标拼接已不可信，按第一页权威重启。
                loadingMore = false
                refresh()
                return
            }
            pinned = pagePinned ?: previousPinned
            roots = mergeNewest(roots, page.list)
                .filter { it.id.isNotEmpty() && it.id != pagePinned?.id }
            totalVisible = page.totalVisible
            totalRoots = page.totalRoots
            val next = page.nextCursor
            // 空页 / 无游标 / 游标未前进 / 后端说没有更多 —— 任一成立即停止翻页，避免死循环。
            val canContinue = page.list.isNotEmpty() && next != null && next != cursor && page.hasMore
            hasMore = canContinue
            rootCursor = if (canContinue) next else null
        } catch (e: CancellationException) {
            throw e
        } catch (e: UnifiedCommentError) {
            if (generation == rootGeneration) loadMoreError = e
        } catch (e: Exception) {
            if (generation == rootGeneration) loadMoreError = UnifiedCommentError.Unavailable(e)
        } finally {
            if (generation == rootGeneration) loadingMore = false
        }
    }

    /** 展开 / 收起某根回复；已加载则直接展示，失败后再次点击为重试。 */
    public suspend fun toggleReplies(rootId: String) {
        val thread = replyThreads[rootId]
        when {
            thread == null -> loadFirstReplies(rootId)
            thread.expanded && thread.error != null && !thread.loaded -> loadFirstReplies(rootId)
            thread.expanded -> replyThreads = replyThreads + (rootId to thread.copy(expanded = false))
            thread.loaded -> replyThreads = replyThreads + (rootId to thread.copy(expanded = true, error = null))
            else -> loadFirstReplies(rootId)
        }
    }

    public suspend fun loadMoreReplies(rootId: String) {
        val thread = replyThreads[rootId] ?: return
        if (!thread.expanded || thread.loading || thread.loadingMore || !thread.hasMore) return
        val cursor = thread.cursor ?: return
        val requestId = thread.requestId
        replyThreads = replyThreads + (rootId to thread.copy(loadingMore = true, loadMoreError = null))
        try {
            val page = repository.fetchReplies(rootId, pageSize, cursor)
            val current = replyThreads[rootId] ?: return
            if (current.requestId != requestId || requestId == 0) return
            val next = page.nextCursor
            val merged = mergeNewest(current.items, page.list)
            // 先合并本页再判停：末页也会带新条目，不能因为 next=null 就丢弃。
            val canContinue = page.list.isNotEmpty() && next != null && next != cursor && page.hasMore
            replyThreads = replyThreads + (rootId to current.copy(
                items = merged,
                loadingMore = false,
                hasMore = canContinue,
                totalVisible = page.totalVisible,
                cursor = if (canContinue) next else null,
            ))
        } catch (e: CancellationException) {
            throw e
        } catch (e: UnifiedCommentError) {
            failReplyLoadMore(rootId, requestId, e)
        } catch (e: Exception) {
            failReplyLoadMore(rootId, requestId, UnifiedCommentError.Unavailable(e))
        }
    }

    public fun startReply(comment: UnifiedComment) {
        if (!comment.canReply || comment.id.isEmpty()) return
        pendingSubmission = null
        replyToStale = false
        val nickname = comment.author?.nickname?.takeIf { it.isNotBlank() } ?: "该用户"
        replyTo = ReplyContext(comment.id, nickname)
    }

    public fun cancelReply() {
        replyTo = null
        replyToStale = false
        pendingSubmission = null
        if (validationError != null) validationError = null
    }

    /** 登录返回 / 面板重建时恢复回复上下文（不生成新幂等 key）。 */
    public fun restoreReply(context: ReplyContext) {
        pendingSubmission = null
        replyToStale = false
        replyTo = context
    }

    public suspend fun submit() {
        if (submitting) return
        val text = draft.trim()
        validationError = when {
            text.isEmpty() -> "请输入评论内容"
            countCommentCodePoints(text) > MAX_CONTENT_LENGTH -> "评论最多 $MAX_CONTENT_LENGTH 字"
            replyToStale -> "原评论已不可回复，请取消回复后重新选择"
            else -> null
        }
        if (validationError != null) return

        val replyToId = replyTo?.commentId
        if (conflictedSubmission?.content == text &&
            conflictedSubmission?.replyToId == replyToId
        ) {
            validationError = "该评论与此前内容冲突，请修改内容后再发送"
            return
        }
        val captured = resolvePending(text, replyToId)
        submitting = true
        composeError = null
        try {
            val created = repository.createComment(
                targetType = targetType,
                targetId = targetId,
                content = text,
                clientMsgId = captured.clientMsgId,
                replyToId = replyToId,
            )
            if (pendingSubmission == captured) pendingSubmission = null
            conflictedSubmission = null
            val operationCurrent =
                draft.trim() == captured.content && replyTo?.commentId == captured.replyToId
            if (operationCurrent) {
                draft = ""
                replyTo = null
                replyToStale = false
            }
            applyCreated(created, replyToId)
            refresh()
        } catch (e: CancellationException) {
            throw e
        } catch (e: UnifiedCommentError) {
            composeError = e
            if (e is UnifiedCommentError.Unauthorized) authRequired = true
            if (e is UnifiedCommentError.Conflict) {
                // 409 不换 key 自动重试：记下被拒载荷，内容回到同一组合仍拒绝；
                // 只有换成不同内容或换回复对象才放行。
                pendingSubmission = captured
                conflictedSubmission = captured
                refresh()
            }
        } catch (e: Exception) {
            composeError = UnifiedCommentError.Unavailable(e)
        } finally {
            submitting = false
        }
    }

    public suspend fun toggleLike(comment: UnifiedComment) {
        if (!comment.canLike || comment.id.isEmpty() || comment.id in actionBusyIds) return
        actionBusyIds = actionBusyIds + comment.id
        actionError = null
        try {
            val result = repository.setLiked(comment.id, !comment.isLiked)
            applyLike(comment.id, result)
        } catch (e: CancellationException) {
            throw e
        } catch (e: UnifiedCommentError) {
            actionError = e.message
            if (e is UnifiedCommentError.Unauthorized) authRequired = true
        } catch (e: Exception) {
            actionError = UnifiedCommentError.Unavailable(e).message
        } finally {
            actionBusyIds = actionBusyIds - comment.id
        }
    }

    public suspend fun deleteComment(comment: UnifiedComment) {
        if (!comment.canDelete || comment.id.isEmpty() || comment.id in actionBusyIds) return
        actionBusyIds = actionBusyIds + comment.id
        actionError = null
        try {
            repository.deleteComment(comment.id)
            // 服务端已确认删除：立即本地移除（含回复线程里的回复），刷新失败也不留操作位。
            removeCommentLocally(comment.id)
            markReplyTargetStaleIfMatches(comment.id)
            refresh()
        } catch (e: CancellationException) {
            throw e
        } catch (e: UnifiedCommentError) {
            actionError = e.message
            if (e is UnifiedCommentError.Unauthorized) authRequired = true
        } catch (e: Exception) {
            actionError = UnifiedCommentError.Unavailable(e).message
        } finally {
            actionBusyIds = actionBusyIds - comment.id
        }
    }

    public suspend fun setPinned(comment: UnifiedComment, pinnedNow: Boolean) {
        if (!comment.canPin || comment.id.isEmpty() || comment.id in actionBusyIds) return
        actionBusyIds = actionBusyIds + comment.id
        actionError = null
        try {
            val result = repository.setPinned(comment.id, pinnedNow)
            applyPinnedLocally(comment, result.isPinned)
            refresh()
        } catch (e: CancellationException) {
            throw e
        } catch (e: UnifiedCommentError) {
            actionError = e.message
            if (e is UnifiedCommentError.Unauthorized) authRequired = true
        } catch (e: Exception) {
            actionError = UnifiedCommentError.Unavailable(e).message
        } finally {
            actionBusyIds = actionBusyIds - comment.id
        }
    }

    public suspend fun moderate(
        comment: UnifiedComment,
        action: UnifiedCommentModerationAction,
        reason: String? = null,
    ) {
        if (!comment.canModerate || comment.id.isEmpty() || comment.id in actionBusyIds) return
        actionBusyIds = actionBusyIds + comment.id
        actionError = null
        try {
            repository.moderate(comment.id, action, reason)
            if (action == UnifiedCommentModerationAction.Hide) {
                // 服务端确认隐藏：先本地去掉正文与能力位（不伪造计数 / 身份），再刷新取权威。
                applyHiddenLocally(comment.id)
                markReplyTargetStaleIfMatches(comment.id)
            }
            refresh()
        } catch (e: CancellationException) {
            throw e
        } catch (e: UnifiedCommentError) {
            actionError = e.message
            if (e is UnifiedCommentError.Unauthorized) authRequired = true
        } catch (e: Exception) {
            actionError = UnifiedCommentError.Unavailable(e).message
        } finally {
            actionBusyIds = actionBusyIds - comment.id
        }
    }

    // —— 内部 ——

    private fun applyFirstPage(page: UnifiedCommentPage) {
        val pinnedComment = page.pinned?.takeIf { it.id.isNotEmpty() }
        pinned = pinnedComment
        roots = mergeNewest(emptyList(), page.list)
            .filter { it.id.isNotEmpty() && it.id != pinnedComment?.id }
        totalVisible = page.totalVisible
        totalRoots = page.totalRoots
        val next = page.nextCursor
        hasMore = page.hasMore && next != null
        rootCursor = if (hasMore) next else null
    }

    /**
     * 刷新后重置回复缓存：保留的线程一律置为未加载（在途请求因 [replyEpoch] 单调而作废），
     * 展开线程与「当前回复目标所在线程」立即重载；折叠线程按需懒加载。
     */
    private suspend fun reconcileReplyThreads() {
        val targetId = replyTo?.commentId
        if (replyThreads.isNotEmpty()) {
            val visibleRootIds = roots.map { it.id }.toSet() + listOfNotNull(pinned?.id)
            val retained = replyThreads.filterKeys { it in visibleRootIds }
            val toReload = retained.filter { (_, thread) ->
                thread.expanded || (targetId != null && thread.items.any { it.id == targetId })
            }
            replyThreads = retained.mapValues { (_, thread) ->
                thread.copy(
                    items = emptyList(),
                    loaded = false,
                    loading = false,
                    loadingMore = false,
                    error = null,
                    loadMoreError = null,
                    hasMore = false,
                    totalVisible = 0L,
                    cursor = null,
                    requestId = 0,
                )
            }
            for ((rootId, thread) in toReload) {
                loadFirstReplies(rootId, expand = thread.expanded)
            }
        }
        reconcileReplyTarget()
    }

    private suspend fun loadFirstReplies(rootId: String, expand: Boolean = true) {
        val previous = replyThreads[rootId] ?: ReplyThread()
        val requestId = ++replyEpoch
        replyThreads = replyThreads + (rootId to previous.copy(
            expanded = if (expand) true else previous.expanded,
            loading = true,
            error = null,
            loadingMore = false,
            requestId = requestId,
        ))
        try {
            val page = repository.fetchReplies(rootId, pageSize, cursor = null)
            val current = replyThreads[rootId] ?: return
            if (requestId == 0 || current.requestId != requestId) return
            val next = page.nextCursor
            replyThreads = replyThreads + (rootId to current.copy(
                items = mergeNewest(emptyList(), page.list).filter { it.id.isNotEmpty() },
                loaded = true,
                loading = false,
                error = null,
                hasMore = page.hasMore && next != null,
                totalVisible = page.totalVisible,
                cursor = if (page.hasMore) next else null,
            ))
        } catch (e: CancellationException) {
            throw e
        } catch (e: UnifiedCommentError) {
            failReplyLoad(rootId, requestId, e)
        } catch (e: Exception) {
            failReplyLoad(rootId, requestId, UnifiedCommentError.Unavailable(e))
        }
    }

    private fun failReplyLoad(rootId: String, requestId: Int, error: UnifiedCommentError) {
        val current = replyThreads[rootId] ?: return
        if (requestId == 0 || current.requestId != requestId) return
        replyThreads = replyThreads + (rootId to current.copy(loading = false, error = error))
    }

    private fun failReplyLoadMore(rootId: String, requestId: Int, error: UnifiedCommentError) {
        val current = replyThreads[rootId] ?: return
        if (requestId == 0 || current.requestId != requestId) return
        replyThreads = replyThreads + (rootId to current.copy(loadingMore = false, loadMoreError = error))
    }

    private fun reconcileReplyTarget() {
        val target = replyTo
        if (target == null) {
            replyToStale = false
            return
        }
        val found = findComment(target.commentId)
        if (found == null ||
            found.id.isEmpty() ||
            found.contentState != UnifiedCommentContentState.Visible ||
            !found.canReply
        ) {
            replyToStale = true
            return
        }
        replyToStale = false
        val nickname = found.author?.nickname?.takeIf { it.isNotBlank() }
        if (nickname != null && nickname != target.nickname) {
            replyTo = target.copy(nickname = nickname)
        }
    }

    private fun findComment(id: String): UnifiedComment? {
        pinned?.takeIf { it.id == id }?.let { return it }
        roots.firstOrNull { it.id == id }?.let { return it }
        for (thread in replyThreads.values) {
            thread.items.firstOrNull { it.id == id }?.let { return it }
        }
        return null
    }

    private fun markReplyTargetStaleIfMatches(commentId: String) {
        if (replyTo?.commentId == commentId) replyToStale = true
    }

    private fun resolvePending(content: String, replyToId: String?): PendingSubmission {
        val existing = pendingSubmission
        if (existing != null && existing.content == content && existing.replyToId == replyToId) {
            return existing
        }
        val created = PendingSubmission(content, replyToId, newClientMsgId())
        pendingSubmission = created
        return created
    }

    private fun applyCreated(created: UnifiedComment, replyToId: String?) {
        if (created.id.isEmpty()) return
        // 回放 200 可能带回墓碑（已被删 / 隐藏）：不得作为可见条目插入。
        if (created.contentState != UnifiedCommentContentState.Visible) return
        if (replyToId == null) {
            if (created.id == pinned?.id) return
            roots = listOf(created) + roots.filter { it.id != created.id }
            return
        }
        val rootId = created.rootId ?: return
        val thread = replyThreads[rootId] ?: return
        // 总数只认 refresh 的信封值，这里仅展示服务端确认可见的 DTO。
        replyThreads = replyThreads + (rootId to thread.copy(
            items = mergeNewest(thread.items, listOf(created)),
            loaded = true,
        ))
    }

    private fun applyLike(commentId: String, like: UnifiedCommentLike) {
        updateComment(commentId) { it.copy(likeCountRaw = like.likeCount, isLikedRaw = like.isLiked) }
    }

    private fun updateComment(commentId: String, transform: (UnifiedComment) -> UnifiedComment) {
        pinned?.let { if (it.id == commentId) pinned = transform(it) }
        if (roots.any { it.id == commentId }) {
            roots = roots.map { if (it.id == commentId) transform(it) else it }
        }
        if (replyThreads.isNotEmpty()) {
            replyThreads = replyThreads.mapValues { (_, thread) ->
                if (thread.items.any { it.id == commentId }) {
                    thread.copy(items = thread.items.map { if (it.id == commentId) transform(it) else it })
                } else {
                    thread
                }
            }
        }
    }

    private fun removeCommentLocally(commentId: String) {
        pinned?.let { if (it.id == commentId) pinned = null }
        roots = roots.filter { it.id != commentId }
        replyThreads = replyThreads.filterKeys { it != commentId }
        if (replyThreads.isNotEmpty()) {
            replyThreads = replyThreads.mapValues { (_, thread) ->
                if (thread.items.any { it.id == commentId }) {
                    thread.copy(items = thread.items.filter { it.id != commentId })
                } else {
                    thread
                }
            }
        }
    }

    private fun applyHiddenLocally(commentId: String) {
        updateComment(commentId) { it.copy(
            contentRaw = null,
            contentStateRaw = "hidden",
            canReplyRaw = false,
            canLikeRaw = false,
            canDeleteRaw = false,
            canPinRaw = false,
            canModerateRaw = false,
        ) }
    }

    private fun applyPinnedLocally(comment: UnifiedComment, nowPinned: Boolean) {
        if (nowPinned) {
            pinned = comment.copy(isPinnedRaw = true)
            roots = roots.filter { it.id != comment.id }
        } else {
            if (pinned?.id == comment.id) pinned = null
            if (roots.none { it.id == comment.id }) {
                roots = listOf(comment.copy(isPinnedRaw = false)) + roots
            }
        }
    }

    /**
     * 同 ID 条目用最新 DTO 原地替换（保持首次出现位置），新 ID 追加。
     * 根列表 / 回复列表共用，避免「先到先得」把刷新后的正文 / 能力位 / 计数冻结成旧值。
     */
    private fun mergeNewest(
        existing: List<UnifiedComment>,
        incoming: List<UnifiedComment>,
    ): List<UnifiedComment> {
        if (existing.isEmpty() && incoming.isEmpty()) return emptyList()
        val order = LinkedHashMap<String, UnifiedComment>(existing.size + incoming.size)
        for (item in existing) {
            if (item.id.isNotEmpty()) order[item.id] = item
        }
        for (item in incoming) {
            if (item.id.isNotEmpty()) order[item.id] = item
        }
        return order.values.toList()
    }

    public companion object {
        public const val DEFAULT_PAGE_SIZE: Int = 20

        /** 后端 `content` 长度上限（1..2000 个 Unicode 码点 / Go rune）。 */
        public const val MAX_CONTENT_LENGTH: Int = 2000

        /** 生产绑定：统一评论 HTTP 仓库（没有 mock / fallback）。 */
        public fun defaultRepository(): UnifiedCommentRepository = HttpUnifiedCommentRepository()
    }
}

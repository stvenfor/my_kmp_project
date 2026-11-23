package com.example.my_kmp_project.feature.community

import com.example.my_kmp_project.feature.community.http.UnifiedComment
import com.example.my_kmp_project.feature.community.http.UnifiedCommentAuthor
import com.example.my_kmp_project.feature.community.http.UnifiedCommentContentState
import com.example.my_kmp_project.feature.community.http.UnifiedCommentCursor
import com.example.my_kmp_project.feature.community.http.UnifiedCommentError
import com.example.my_kmp_project.feature.community.http.UnifiedCommentLike
import com.example.my_kmp_project.feature.community.http.UnifiedCommentModerationAction
import com.example.my_kmp_project.feature.community.http.UnifiedCommentPage
import com.example.my_kmp_project.feature.community.http.UnifiedCommentPin
import com.example.my_kmp_project.feature.community.http.UnifiedCommentRepository
import com.example.my_kmp_project.feature.community.http.UnifiedCommentSort
import com.example.my_kmp_project.feature.community.http.UnifiedCommentStatus
import com.example.my_kmp_project.feature.community.http.UnifiedCommentTargetType
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UnifiedCommentTimeTest {
    @Test
    fun onlyParsedServerTimeProducesRelativeLabel() {
        assertNull(formatCommentRelativeTime(null, 1_000L))
        assertEquals("刚刚", formatCommentRelativeTime(1_000L, 31_000L))
        assertEquals("5分钟前", formatCommentRelativeTime(0L, 5 * 60_000L))
        assertEquals("2小时前", formatCommentRelativeTime(0L, 2 * 3_600_000L))
        assertEquals("3天前", formatCommentRelativeTime(0L, 3 * 86_400_000L))
        assertEquals("2个月前", formatCommentRelativeTime(0L, 60 * 86_400_000L))
        assertEquals("2年前", formatCommentRelativeTime(0L, 730 * 86_400_000L))
        // 未来时间只夹到 0，不伪造日期。
        assertEquals("刚刚", formatCommentRelativeTime(2_000L, 1_000L))
    }
}

class UnifiedCommentTargetTest {
    @Test
    fun routeTargetsKeepTypeAndNeverDefault() {
        assertEquals(
            UnifiedCommentTarget.Valid(UnifiedCommentTargetType.Post, "p1"),
            UnifiedCommentTarget.fromRoute("/community/comment?postId=p1"),
        )
        assertEquals(
            UnifiedCommentTarget.Valid(UnifiedCommentTargetType.Video, "v1"),
            UnifiedCommentTarget.fromRoute("/community/comment?targetType=video&targetId=v1"),
        )
        assertEquals(
            UnifiedCommentTarget.Valid(UnifiedCommentTargetType.Article, "a1"),
            UnifiedCommentTarget.fromRoute("/community/comment?target_type=article&target_id=a1"),
        )
        assertEquals(UnifiedCommentTarget.Invalid, UnifiedCommentTarget.fromRoute("/community/comment"))
        assertEquals(
            UnifiedCommentTarget.Invalid,
            UnifiedCommentTarget.fromRoute("/community/comment?targetType=short_video&targetId=x"),
        )
        assertEquals(
            UnifiedCommentTarget.Invalid,
            UnifiedCommentTarget.fromRoute("/community/comment?targetType=video"),
        )
    }
}

class UnifiedCommentControllerTest {
    @Test
    fun refreshSeparatesPinnedAndUsesServerTotals() = runBlocking {
        val pinned = comment("p", content = "pinned")
        val a = comment("a")
        val b = comment("b")
        val repo = FakeRepo()
        repo.rootHandler = { _, cursor ->
            if (cursor == null) {
                page(list = listOf(pinned, a, b), pinned = pinned, totalVisible = 11, totalRoots = 3)
            } else {
                page()
            }
        }
        val controller = controller(repo)

        controller.refresh()

        assertEquals("p", controller.pinned?.id)
        assertEquals(listOf("a", "b"), controller.roots.map { it.id })
        assertEquals(11L, controller.totalVisible)
        assertEquals(3L, controller.totalRoots)
        assertFalse(controller.firstLoading)
        assertNull(controller.refreshError)
    }

    @Test
    fun loadMoreAppendsDistinctAndStopsOnUnchangedCursor() = runBlocking {
        val a = comment("a")
        val b = comment("b")
        val repo = FakeRepo()
        repo.rootHandler = { _, cursor ->
            when (cursor) {
                null -> page(list = listOf(a), next = "c1", hasMore = true, totalVisible = 2, totalRoots = 2)
                "c1" -> page(list = listOf(a, b), next = "c1", hasMore = true, totalVisible = 2, totalRoots = 2)
                else -> page()
            }
        }
        val controller = controller(repo)

        controller.refresh()
        controller.loadMore()

        assertEquals(listOf("a", "b"), controller.roots.map { it.id })
        assertFalse(controller.hasMore)
        controller.loadMore()
        assertEquals(listOf(null, "c1"), repo.rootCursors)
    }

    @Test
    fun loadMoreFailurePreservesContentAndRetries() = runBlocking {
        val a = comment("a")
        val b = comment("b")
        val repo = FakeRepo()
        repo.rootHandler = { _, cursor ->
            if (cursor == null) {
                page(list = listOf(a), next = "c1", hasMore = true, totalVisible = 2, totalRoots = 2)
            } else {
                throw UnifiedCommentError.Unavailable()
            }
        }
        val controller = controller(repo)

        controller.refresh()
        controller.loadMore()

        assertEquals(listOf("a"), controller.roots.map { it.id })
        assertNotNull(controller.loadMoreError)
        assertTrue(controller.hasMore)

        repo.rootHandler = { _, _ -> page(list = listOf(a, b), totalVisible = 2, totalRoots = 2) }
        controller.loadMore()

        assertEquals(listOf("a", "b"), controller.roots.map { it.id })
        assertNull(controller.loadMoreError)
        assertFalse(controller.hasMore)
    }

    @Test
    fun refreshFailureKeepsRenderedContent() = runBlocking {
        val a = comment("a")
        val repo = FakeRepo()
        repo.rootHandler = { _, _ -> page(list = listOf(a), totalVisible = 1, totalRoots = 1) }
        val controller = controller(repo)
        controller.refresh()

        repo.rootHandler = { _, _ -> throw UnifiedCommentError.Unavailable() }
        controller.refresh()

        assertEquals(listOf("a"), controller.roots.map { it.id })
        assertNotNull(controller.refreshError)
        assertFalse(controller.firstLoading)
    }

    @Test
    fun staleLatestResponseCannotOverrideSortChange() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val stale = comment("stale")
        val hot = comment("hot")
        var latestCalls = 0
        val repo = FakeRepo()
        repo.rootHandler = { sort, _ ->
            when (sort) {
                UnifiedCommentSort.Hot -> page(list = listOf(hot))
                UnifiedCommentSort.Latest -> {
                    latestCalls += 1
                    if (latestCalls == 1) {
                        page(list = listOf(stale))
                    } else {
                        gate.await()
                        page(list = listOf(stale))
                    }
                }
            }
        }
        val controller = controller(repo)
        controller.refresh()

        val job = launch { controller.refresh() }
        yield()
        controller.changeSort(UnifiedCommentSort.Hot)
        gate.complete(Unit)
        job.join()

        assertEquals(UnifiedCommentSort.Hot, controller.sort)
        assertEquals(listOf("hot"), controller.roots.map { it.id })
    }

    @Test
    fun submitReusesClientMsgIdUntilPayloadChanges() = runBlocking {
        var failFirst = true
        val repo = FakeRepo()
        repo.createHandler = { content, key, replyTo ->
            if (failFirst) {
                failFirst = false
                throw UnifiedCommentError.Unavailable()
            }
            comment("new-$key", content = content, rootId = replyTo)
        }
        var keys = 0
        val controller = controller(repo, newClientMsgId = { keys += 1; "key-$keys" })

        controller.updateDraft("hi")
        controller.submit()
        assertNotNull(controller.composeError)
        assertEquals(listOf("key-1"), repo.createKeys)

        controller.submit()
        assertEquals(listOf("key-1", "key-1"), repo.createKeys)
        assertTrue(controller.draft.isEmpty())

        controller.updateDraft("hi again")
        controller.submit()
        assertEquals(listOf("key-1", "key-1", "key-2"), repo.createKeys)
    }

    @Test
    fun successDoesNotClearDraftEditedDuringSubmit() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val repo = FakeRepo()
        repo.createHandler = { content, key, _ ->
            gate.await()
            comment("new-$key", content = content)
        }
        val controller = controller(repo)
        controller.updateDraft("hi")

        val job = launch { controller.submit() }
        yield()
        controller.updateDraft("hi there")
        gate.complete(Unit)
        job.join()

        assertEquals("hi there", controller.draft)
        assertEquals(1, repo.createKeys.size)
    }

    @Test
    fun likeUsesServerFragmentAcrossList() = runBlocking {
        val a = comment("a", canLike = true)
        val repo = FakeRepo()
        repo.rootHandler = { _, _ -> page(list = listOf(a)) }
        repo.likeHandler = { id, liked -> UnifiedCommentLike(idRaw = id, likeCountRaw = 5, isLikedRaw = liked) }
        val controller = controller(repo)
        controller.refresh()

        controller.toggleLike(controller.roots.single())

        assertEquals(5L, controller.roots.single().likeCount)
        assertTrue(controller.roots.single().isLiked)
    }

    @Test
    fun failedDeleteKeepsCommentsAndSurfacesSafeMessage() = runBlocking {
        val a = comment("a", canDelete = true)
        val repo = FakeRepo()
        repo.rootHandler = { _, _ -> page(list = listOf(a)) }
        repo.deleteHandler = { throw UnifiedCommentError.Forbidden() }
        val controller = controller(repo)
        controller.refresh()

        controller.deleteComment(controller.roots.single())

        assertEquals(listOf("a"), controller.roots.map { it.id })
        assertEquals("没有操作该评论的权限", controller.actionError)
    }

    @Test
    fun replyExpansionPaginatesAndDedupes() = runBlocking {
        val root = comment("r", replyCount = 2)
        val r1 = comment("r1", rootId = "r")
        val r2 = comment("r2", rootId = "r")
        val repo = FakeRepo()
        repo.rootHandler = { _, _ -> page(list = listOf(root), totalVisible = 3, totalRoots = 1) }
        repo.replyHandler = { rootId, cursor ->
            assertEquals("r", rootId)
            when (cursor) {
                null -> page(list = listOf(r1), next = "rc1", hasMore = true, totalVisible = 2, totalRoots = 0)
                "rc1" -> page(list = listOf(r1, r2), totalVisible = 2, totalRoots = 0)
                else -> page()
            }
        }
        val controller = controller(repo)
        controller.refresh()

        controller.toggleReplies("r")
        val first = controller.replyThreads.getValue("r")
        assertTrue(first.expanded)
        assertTrue(first.loaded)
        assertEquals(listOf("r1"), first.items.map { it.id })
        assertTrue(first.hasMore)

        controller.loadMoreReplies("r")
        val second = controller.replyThreads.getValue("r")
        assertEquals(listOf("r1", "r2"), second.items.map { it.id })
        assertFalse(second.hasMore)
    }

    @Test
    fun replyFailureKeepsRootListAndRetries() = runBlocking {
        val root = comment("r", replyCount = 1)
        val r1 = comment("r1", rootId = "r")
        val repo = FakeRepo()
        repo.rootHandler = { _, _ -> page(list = listOf(root), totalVisible = 2, totalRoots = 1) }
        repo.replyHandler = { _, _ -> throw UnifiedCommentError.Unavailable() }
        val controller = controller(repo)
        controller.refresh()

        controller.toggleReplies("r")
        val failed = controller.replyThreads.getValue("r")
        assertNotNull(failed.error)
        assertFalse(failed.loaded)
        assertTrue(failed.expanded)
        assertEquals(listOf("r"), controller.roots.map { it.id })

        repo.replyHandler = { _, _ -> page(list = listOf(r1), totalVisible = 1, totalRoots = 0) }
        controller.toggleReplies("r")
        val retried = controller.replyThreads.getValue("r")
        assertNull(retried.error)
        assertTrue(retried.loaded)
        assertEquals(listOf("r1"), retried.items.map { it.id })
    }

    @Test
    fun tombstoneRootIsContainerButOnlyVisibleRepliesAreReplyable() = runBlocking {
        // 后端口径（unified_comment_service.go:202）：删除 / 隐藏的根仍是容器（can_reply=false），
        // 只有可见后代可回复。
        val tombstone = comment(
            "t",
            content = "never shown",
            contentState = "deleted",
            replyCount = 1,
            canReply = false,
            canLike = false,
        )
        val visibleReply = comment("t1", rootId = "t", canReply = true)
        val repo = FakeRepo()
        repo.rootHandler = { _, _ -> page(list = listOf(tombstone), totalVisible = 2, totalRoots = 1) }
        repo.replyHandler = { _, _ -> page(list = listOf(visibleReply), totalVisible = 1, totalRoots = 0) }
        val controller = controller(repo)
        controller.refresh()

        val root = controller.roots.single()
        assertEquals(UnifiedCommentContentState.Deleted, root.contentState)
        assertEquals("", root.content)
        assertFalse(root.canReply)
        assertFalse(root.canLike)

        controller.startReply(root)
        assertNull(controller.replyTo)

        controller.toggleReplies("t")
        val reply = controller.replyThreads.getValue("t").items.single()
        assertTrue(reply.canReply)
        controller.startReply(reply)
        assertEquals("t1", controller.replyTo?.commentId)
    }

    @Test
    fun staleLoadMoreAfterSortChangeReleasesBusyState() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val repo = FakeRepo()
        repo.rootHandler = { sort, cursor ->
            if (sort == UnifiedCommentSort.Hot) {
                page(list = listOf(comment("hot")))
            } else {
                when (cursor) {
                    null -> page(
                        list = listOf(comment("a")),
                        next = "c1",
                        hasMore = true,
                        totalVisible = 3,
                        totalRoots = 3,
                    )
                    else -> {
                        gate.await()
                        page(list = listOf(comment("b")), totalVisible = 3, totalRoots = 3)
                    }
                }
            }
        }
        val controller = controller(repo)
        controller.refresh()

        val job = launch { controller.loadMore() }
        yield()
        assertTrue(controller.loadingMore)
        controller.changeSort(UnifiedCommentSort.Hot)
        gate.complete(Unit)
        job.join()

        assertFalse(controller.loadingMore)
        assertEquals(UnifiedCommentSort.Hot, controller.sort)
        assertEquals(listOf("hot"), controller.roots.map { it.id })
    }

    @Test
    fun recreatedReplyThreadIgnoresOldInFlightResponse() = runBlocking {
        // 折叠 / 重建同一根的线程时，旧请求不得覆盖新线程（旧实现按线程重置 generation 会产生 ABA）。
        val gateOld = CompletableDeferred<Unit>()
        val gateNew = CompletableDeferred<Unit>()
        var replies = 0
        val root = comment("r", replyCount = 1)
        val repo = FakeRepo()
        repo.rootHandler = { _, _ -> page(list = listOf(root), totalVisible = 2, totalRoots = 1) }
        repo.replyHandler = { _, _ ->
            replies += 1
            if (replies == 1) {
                gateOld.await()
                page(list = listOf(comment("old", rootId = "r")), totalVisible = 1)
            } else {
                gateNew.await()
                page(list = listOf(comment("new", rootId = "r")), totalVisible = 1)
            }
        }
        val controller = controller(repo)
        controller.refresh()

        val oldJob = launch { controller.toggleReplies("r") }
        yield()
        controller.changeSort(UnifiedCommentSort.Hot)
        controller.changeSort(UnifiedCommentSort.Latest)
        val newJob = launch { controller.toggleReplies("r") }
        yield()

        gateNew.complete(Unit)
        newJob.join()
        gateOld.complete(Unit)
        oldJob.join()

        assertEquals(listOf("new"), controller.replyThreads.getValue("r").items.map { it.id })
    }

    @Test
    fun refreshReloadsExpandedRepliesAndBlocksStaleReplyTarget() = runBlocking {
        val root = comment("r", replyCount = 1)
        val reply = comment("r1", rootId = "r", canReply = true)
        val removedReply = comment("r1", rootId = "r", contentState = "deleted", canReply = false)
        var showRemoved = false
        val repo = FakeRepo()
        repo.rootHandler = { _, _ -> page(list = listOf(root), totalVisible = 2, totalRoots = 1) }
        repo.replyHandler = { _, _ ->
            page(list = listOf(if (showRemoved) removedReply else reply), totalVisible = 1)
        }
        val controller = controller(repo)
        controller.refresh()
        controller.toggleReplies("r")
        controller.updateDraft("hi")
        controller.startReply(controller.replyThreads.getValue("r").items.single())
        assertFalse(controller.replyToStale)

        showRemoved = true
        controller.refresh()

        val reloaded = controller.replyThreads.getValue("r").items.single()
        assertEquals(UnifiedCommentContentState.Deleted, reloaded.contentState)
        assertEquals("", reloaded.content)
        assertTrue(controller.replyToStale)
        assertEquals("r1", controller.replyTo?.commentId)

        controller.submit()
        assertTrue(repo.createKeys.isEmpty())
        assertNotNull(controller.validationError)
        Unit
    }

    @Test
    fun refreshedDescendantsReplaceStaleContentWithoutChangingRootCount() = runBlocking {
        val root = comment("r", replyCount = 1)
        var content = "old"
        val repo = FakeRepo()
        repo.rootHandler = { _, _ -> page(list = listOf(root), totalVisible = 2, totalRoots = 1) }
        repo.replyHandler = { _, _ ->
            page(list = listOf(comment("r1", rootId = "r", content = content)), totalVisible = 1)
        }
        val controller = controller(repo)
        controller.refresh()
        controller.toggleReplies("r")

        content = "new"
        controller.refresh()

        assertEquals("new", controller.replyThreads.getValue("r").items.single().content)
        assertEquals(2L, controller.totalVisible)
        assertEquals(1L, controller.totalRoots)
    }

    @Test
    fun confirmedReplyDeleteSurvivesFailedRefreshAndDropsActions() = runBlocking {
        val root = comment("r", replyCount = 2)
        val r1 = comment("r1", rootId = "r", canDelete = true)
        val r2 = comment("r2", rootId = "r")
        val repo = FakeRepo()
        repo.rootHandler = { _, _ -> page(list = listOf(root), totalVisible = 3, totalRoots = 1) }
        repo.replyHandler = { _, _ -> page(list = listOf(r1, r2), totalVisible = 2) }
        val controller = controller(repo)
        controller.refresh()
        controller.toggleReplies("r")
        controller.startReply(controller.replyThreads.getValue("r").items.first { it.id == "r1" })

        repo.rootHandler = { _, _ -> throw UnifiedCommentError.Unavailable() }
        controller.deleteComment(controller.replyThreads.getValue("r").items.first { it.id == "r1" })

        assertEquals(listOf("r2"), controller.replyThreads.getValue("r").items.map { it.id })
        assertNotNull(controller.refreshError)
        assertEquals(3L, controller.totalVisible)
        assertTrue(controller.replyToStale)
    }

    @Test
    fun confirmedHideRemovesReplyActionsEvenWhenRefreshFails() = runBlocking {
        val root = comment("r", replyCount = 1)
        val r1 = comment("r1", rootId = "r", canModerate = true, canDelete = true)
        val repo = FakeRepo()
        repo.rootHandler = { _, _ -> page(list = listOf(root), totalVisible = 2, totalRoots = 1) }
        repo.replyHandler = { _, _ -> page(list = listOf(r1), totalVisible = 1) }
        val controller = controller(repo)
        controller.refresh()
        controller.toggleReplies("r")

        repo.rootHandler = { _, _ -> throw UnifiedCommentError.Unavailable() }
        controller.moderate(
            controller.replyThreads.getValue("r").items.single(),
            UnifiedCommentModerationAction.Hide,
        )

        val hidden = controller.replyThreads.getValue("r").items.single()
        assertEquals(UnifiedCommentContentState.Hidden, hidden.contentState)
        assertEquals("", hidden.content)
        assertFalse(hidden.canReply)
        assertFalse(hidden.canDelete)
        assertFalse(hidden.canModerate)
    }

    @Test
    fun replayAfterTimeoutDoesNotInflateServerTotals() = runBlocking {
        val repo = FakeRepo()
        var createCalls = 0
        repo.createHandler = { _, key, _ ->
            createCalls += 1
            if (createCalls == 1) throw UnifiedCommentError.Unavailable()
            comment("orig", content = key)
        }
        var firstRootPage = true
        repo.rootHandler = { _, _ ->
            if (firstRootPage) {
                firstRootPage = false
                page(list = listOf(comment("a")), totalVisible = 5, totalRoots = 2)
            } else {
                throw UnifiedCommentError.Unavailable()
            }
        }
        val controller = controller(repo, newClientMsgId = { "key-fixed" })
        controller.refresh()
        controller.updateDraft("hi")

        controller.submit()
        assertNotNull(controller.composeError)
        controller.submit()

        assertEquals(listOf("key-fixed", "key-fixed"), repo.createKeys)
        assertEquals(listOf("orig", "a"), controller.roots.map { it.id })
        // 重放成功后刷新失败：总数仍是上一次服务端信封（5/2），不能本地 +1。
        assertEquals(5L, controller.totalVisible)
        assertEquals(2L, controller.totalRoots)
        assertNotNull(controller.refreshError)
        Unit
    }

    @Test
    fun tombstoneReplayIsNotInsertedAsVisibleRoot() = runBlocking {
        val repo = FakeRepo()
        repo.createHandler = { _, _, _ -> comment("orig", contentState = "deleted") }
        repo.rootHandler = { _, _ ->
            page(list = listOf(comment("a")), totalVisible = 1, totalRoots = 1)
        }
        val controller = controller(repo)
        controller.refresh()
        controller.updateDraft("hi")

        controller.submit()

        assertEquals(listOf("a"), controller.roots.map { it.id })
        assertEquals(1L, controller.totalVisible)
    }

    @Test
    fun conflictBlocksSilentRetryUntilContentChanges() = runBlocking {
        val repo = FakeRepo()
        var attempt = 0
        repo.createHandler = { content, key, _ ->
            attempt += 1
            if (attempt == 1) throw UnifiedCommentError.Conflict(conflictWith = "orig")
            comment("new-$key", content = content)
        }
        var keys = 0
        val controller = controller(repo, newClientMsgId = { keys += 1; "key-$keys" })

        controller.updateDraft("hi")
        controller.submit()
        assertTrue(controller.composeError is UnifiedCommentError.Conflict)
        assertEquals(listOf("key-1"), repo.createKeys)

        controller.submit()
        assertEquals(listOf("key-1"), repo.createKeys)
        assertNotNull(controller.validationError)

        // 编辑走再改回同一内容：仍是同一冲突载荷，不能换 key 偷偷重发。
        controller.updateDraft("hi edited")
        controller.updateDraft("hi")
        controller.submit()
        assertEquals(listOf("key-1"), repo.createKeys)
        assertNotNull(controller.validationError)

        controller.updateDraft("hi edited")
        controller.submit()
        assertEquals(listOf("key-1", "key-2"), repo.createKeys)
        assertNull(controller.composeError)
    }

    @Test
    fun pinChannelChangeRestartsAndRemovedPinClears() = runBlocking {
        val p1 = comment("p1")
        val p2 = comment("p2")
        val a = comment("a")
        val b = comment("b")
        var pageList = listOf(a)
        var pagePinned: UnifiedComment? = p1
        var pageNext: String? = "c1"
        var cursorPinned: UnifiedComment? = p2
        var unpinOnCursor = false
        val repo = FakeRepo()
        repo.rootHandler = { _, cursor ->
            if (cursor == null) {
                page(
                    list = pageList,
                    pinned = pagePinned,
                    next = pageNext,
                    hasMore = pageNext != null,
                    totalVisible = 2,
                    totalRoots = 2,
                )
            } else {
                if (unpinOnCursor) {
                    pagePinned = null
                    pageList = listOf(a, p2)
                    pageNext = null
                    page(list = listOf(b), pinned = null, totalVisible = 2, totalRoots = 2)
                } else {
                    // 首页重启后必须看到「新置顶」的权威值。
                    pagePinned = cursorPinned
                    page(list = listOf(b), pinned = cursorPinned, totalVisible = 2, totalRoots = 2)
                }
            }
        }
        val controller = controller(repo)
        controller.refresh()

        // 置顶 p1 → p2：分页拼接不可信，按首页重启，p2 只出现在置顶通道。
        controller.loadMore()
        assertEquals("p2", controller.pinned?.id)
        assertEquals(listOf("a"), controller.roots.map { it.id })

        // 置顶被取消：旧置顶必须从通道消失并按根数据恢复。
        unpinOnCursor = true
        controller.loadMore()
        assertNull(controller.pinned)
        assertEquals(listOf("a", "p2"), controller.roots.map { it.id })
    }

    @Test
    fun duplicateIdInNextPageReplacesDtoInPlace() = runBlocking {
        val repo = FakeRepo()
        repo.rootHandler = { _, cursor ->
            when (cursor) {
                null -> page(
                    list = listOf(comment("a", likeCount = 1)),
                    next = "c1",
                    hasMore = true,
                    totalVisible = 2,
                    totalRoots = 2,
                )
                else -> page(
                    list = listOf(comment("a", likeCount = 9), comment("b")),
                    totalVisible = 2,
                    totalRoots = 2,
                )
            }
        }
        val controller = controller(repo)
        controller.refresh()
        controller.loadMore()

        assertEquals(listOf("a", "b"), controller.roots.map { it.id })
        assertEquals(9L, controller.roots.first().likeCount)
    }

    @Test
    fun concurrentLoadMoreIsIgnoredWhileInFlight() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val repo = FakeRepo()
        repo.rootHandler = { _, cursor ->
            when (cursor) {
                null -> page(
                    list = listOf(comment("a")),
                    next = "c1",
                    hasMore = true,
                    totalVisible = 2,
                    totalRoots = 2,
                )
                else -> {
                    gate.await()
                    page(list = listOf(comment("b")), totalVisible = 2, totalRoots = 2)
                }
            }
        }
        val controller = controller(repo)
        controller.refresh()

        val first = launch { controller.loadMore() }
        yield()
        val second = launch { controller.loadMore() }
        yield()
        gate.complete(Unit)
        first.join()
        second.join()

        assertEquals(listOf(null, "c1"), repo.rootCursors)
        assertFalse(controller.loadingMore)
    }

    @Test
    fun emojiLengthUsesCodePointsNotUtf16Units() = runBlocking {
        val repo = FakeRepo()
        val controller = controller(repo)

        controller.updateDraft("😀".repeat(UnifiedCommentController.MAX_CONTENT_LENGTH))
        controller.submit()
        assertEquals(1, repo.createKeys.size)
        assertNull(controller.validationError)

        controller.updateDraft("😀".repeat(UnifiedCommentController.MAX_CONTENT_LENGTH + 1))
        controller.submit()
        assertEquals(1, repo.createKeys.size)
        assertNotNull(controller.validationError)
        Unit
    }

    @Test
    fun readNotEnabledIsDistinctState() = runBlocking<Unit> {
        val repo = FakeRepo()
        repo.rootHandler = { _, _ -> throw UnifiedCommentError.NotEnabled() }
        val controller = controller(repo)

        controller.refresh()

        assertTrue(controller.notEnabled)
        assertNotNull(controller.refreshError)
    }

    // —— helpers ——

    private fun controller(
        repo: UnifiedCommentRepository,
        newClientMsgId: () -> String = { "key-fixed" },
    ): UnifiedCommentController =
        UnifiedCommentController(
            repository = repo,
            targetType = UnifiedCommentTargetType.Post,
            targetId = "target-1",
            newClientMsgId = newClientMsgId,
        )

    private class FakeRepo : UnifiedCommentRepository {
        var rootHandler: suspend (UnifiedCommentSort, String?) -> UnifiedCommentPage =
            { _, _ -> page() }
        var replyHandler: suspend (String, String?) -> UnifiedCommentPage = { _, _ -> page() }
        var createHandler: suspend (String, String, String?) -> UnifiedComment =
            { content, key, replyTo -> comment("new-$key", content = content, rootId = replyTo) }
        var likeHandler: suspend (String, Boolean) -> UnifiedCommentLike =
            { id, liked -> UnifiedCommentLike(idRaw = id, likeCountRaw = 1, isLikedRaw = liked) }
        var deleteHandler: suspend (String) -> UnifiedCommentStatus =
            { id -> UnifiedCommentStatus(idRaw = id, statusRaw = "deleted") }
        var pinHandler: suspend (String, Boolean) -> UnifiedCommentPin =
            { id, pinned -> UnifiedCommentPin(idRaw = id, isPinnedRaw = pinned) }
        var moderateHandler: suspend (String, UnifiedCommentModerationAction, String?) -> UnifiedCommentStatus =
            { id, _, _ -> UnifiedCommentStatus(idRaw = id, statusRaw = "visible") }

        val rootCursors = mutableListOf<String?>()
        val createKeys = mutableListOf<String>()

        override suspend fun fetchRoots(
            targetType: UnifiedCommentTargetType,
            targetId: String,
            sort: UnifiedCommentSort,
            limit: Int,
            cursor: String?,
        ): UnifiedCommentPage {
            rootCursors += cursor
            return rootHandler(sort, cursor)
        }

        override suspend fun fetchReplies(
            rootCommentId: String,
            limit: Int,
            cursor: String?,
        ): UnifiedCommentPage = replyHandler(rootCommentId, cursor)

        override suspend fun createComment(
            targetType: UnifiedCommentTargetType,
            targetId: String,
            content: String,
            clientMsgId: String,
            replyToId: String?,
        ): UnifiedComment {
            createKeys += clientMsgId
            return createHandler(content, clientMsgId, replyToId)
        }

        override suspend fun setLiked(commentId: String, liked: Boolean): UnifiedCommentLike =
            likeHandler(commentId, liked)

        override suspend fun deleteComment(commentId: String): UnifiedCommentStatus =
            deleteHandler(commentId)

        override suspend fun setPinned(commentId: String, pinned: Boolean): UnifiedCommentPin =
            pinHandler(commentId, pinned)

        override suspend fun moderate(
            commentId: String,
            action: UnifiedCommentModerationAction,
            reason: String?,
        ): UnifiedCommentStatus = moderateHandler(commentId, action, reason)
    }
}

private fun page(
    list: List<UnifiedComment> = emptyList(),
    pinned: UnifiedComment? = null,
    next: String? = null,
    hasMore: Boolean = false,
    totalVisible: Long = list.size.toLong(),
    totalRoots: Long = list.size.toLong(),
): UnifiedCommentPage = UnifiedCommentPage(
    listRaw = list,
    pinnedRaw = pinned,
    cursorRaw = UnifiedCommentCursor(nextRaw = next, hasMoreRaw = hasMore),
    totalVisibleRaw = totalVisible,
    totalRootsRaw = totalRoots,
)

private fun comment(
    id: String,
    content: String = "content-$id",
    contentState: String = "visible",
    rootId: String? = null,
    replyCount: Long = 0,
    likeCount: Long = 0,
    isLiked: Boolean = false,
    canReply: Boolean = true,
    canLike: Boolean = true,
    canDelete: Boolean = false,
    canPin: Boolean = false,
    canModerate: Boolean = false,
): UnifiedComment = UnifiedComment(
    idRaw = id,
    rootIdRaw = rootId,
    authorRaw = UnifiedCommentAuthor(nicknameRaw = "用户$id"),
    contentRaw = content,
    contentStateRaw = contentState,
    likeCountRaw = likeCount,
    replyCountRaw = replyCount,
    isLikedRaw = isLiked,
    canReplyRaw = canReply,
    canLikeRaw = canLike,
    canDeleteRaw = canDelete,
    canPinRaw = canPin,
    canModerateRaw = canModerate,
    createdAtRaw = "2026-11-23T00:00:00Z",
)

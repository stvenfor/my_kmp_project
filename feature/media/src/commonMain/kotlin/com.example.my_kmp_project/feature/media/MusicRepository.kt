package com.example.my_kmp_project.feature.media

import com.example.my_kmp_project.core.network.ApiClient
import com.example.my_kmp_project.core.network.NetworkError
import com.example.my_kmp_project.core.network.getListPage
import com.example.my_kmp_project.core.ui.MusicTrack
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 音乐后端路径（Go BFF）。 */
object MusicApiPaths {
    const val TRACKS: String = "/api/v1/music/tracks"
}

/**
 * `GET /api/v1/music/tracks` 的 `data.list[]` 元素。
 *
 * 后端种子返回 6 首曲目，字段为 snake_case；`duration_seconds` 只用于展示，
 * 真实时长仍以平台播放器回报为准。
 */
@Serializable
data class MusicTrackDto(
    val id: String,
    val title: String = "",
    val artist: String = "",
    val album: String? = null,
    @SerialName("cover_url") val coverUrl: String? = null,
    @SerialName("audio_url") val audioUrl: String = "",
    @SerialName("duration_seconds") val durationSeconds: Int? = null,
)

/** 缺少 id 或音源地址的行直接丢弃——宁可少一首，也不播放空 URL。 */
fun MusicTrackDto.toTrackOrNull(): MusicTrack? {
    if (id.isBlank() || audioUrl.isBlank()) return null
    return MusicTrack(
        id = id,
        title = title.ifBlank { "未命名曲目" },
        artist = artist.ifBlank { "未知艺人" },
        album = album?.takeIf { it.isNotBlank() },
        coverUrl = coverUrl?.takeIf { it.isNotBlank() },
        audioUrl = audioUrl,
        durationMs = (durationSeconds ?: 0).coerceAtLeast(0) * 1_000L,
    )
}

/** 音乐数据源端口；失败必须抛出，由 UI 呈现可重试的错误态。 */
interface MusicRepository {
    suspend fun fetchTracks(): List<MusicTrack>
}

/** 真实 HTTP 实现；接口不通时抛 [NetworkError]，绝不回退本地假数据。 */
class HttpMusicRepository(
    private val api: ApiClient,
) : MusicRepository {

    override suspend fun fetchTracks(): List<MusicTrack> {
        val response = api.getListPage<MusicTrackDto>(MusicApiPaths.TRACKS)
        if (!response.isSuccess) {
            throw NetworkError.Business(
                code = response.code,
                message = response.message?.takeIf { it.isNotBlank() } ?: "加载曲目失败",
            )
        }
        return response.data?.items.orEmpty().mapNotNull { it.toTrackOrNull() }
    }
}

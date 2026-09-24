package com.example.my_kmp_project.core.router

import com.example.my_kmp_project.feature.ai.AiRoutes
import com.example.my_kmp_project.feature.classroom.ClassroomRoutes
import com.example.my_kmp_project.feature.content.ContentRoutes
import com.example.my_kmp_project.feature.home.HomeRoutes
import com.example.my_kmp_project.feature.media.VideoRoutes
import com.example.my_kmp_project.feature.mine.MineRoutes

/**
 * Shared Flutter-aligned route path constants (K.3).
 * Prefer these over string literals at call sites.
 */
object RoutePath {
    object Home {
        val Search = HomeRoutes.Search
        val Strategy = HomeRoutes.Strategy
        val LearningReport = HomeRoutes.LearningReport
        val CheckInMall = HomeRoutes.CheckInMall
        val DubbingFeed = HomeRoutes.DubbingFeed
    }

    object Video {
        val Hub = VideoRoutes.Hub
        val Short = VideoRoutes.Short
        val ShortPlay = VideoRoutes.ShortPlay
        val ShortPublish = VideoRoutes.ShortPublish
        val ShortHelp = VideoRoutes.ShortHelp
        val DubbingVideos = VideoRoutes.DubbingVideos
        val DubbingVideoDetail = VideoRoutes.DubbingVideoDetail
        val DubbingWorks = VideoRoutes.DubbingWorks
        val DubbingWorkDetail = VideoRoutes.DubbingWorkDetail
    }

    object Classroom {
        val MyClass = ClassroomRoutes.MyClass
        val HomeworkStats = ClassroomRoutes.HomeworkStats
        val GiftClaim = ClassroomRoutes.GiftClaim
    }

    object Mine {
        val Mall = MineRoutes.Mall
        val Wallet = MineRoutes.Wallet
        val Membership = MineRoutes.Membership
        val Profile = MineRoutes.Profile
    }

    object Content {
        val Live = ContentRoutes.Live
        val Friend = ContentRoutes.Friend
        val Music = ContentRoutes.Music
        val AiStream = AiRoutes.Stream
        val MediaEntry = ContentRoutes.MediaEntry
    }
}

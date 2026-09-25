package com.example.my_kmp_project.core.router

import com.example.my_kmp_project.feature.ai.AiRoutes
import com.example.my_kmp_project.feature.classroom.ClassroomRoutes
import com.example.my_kmp_project.feature.content.ContentRoutes
import com.example.my_kmp_project.feature.home.HomeRoutes
import com.example.my_kmp_project.feature.media.VideoRoutes
import com.example.my_kmp_project.feature.mine.MineRoutes

/**
 * Convenience aliases into [AppRoutePath] / feature route objects.
 * Canonical full table: [AppRoutePath] (103 Flutter strings).
 */
object RoutePath {
    val Splash = AppRoutePath.splash
    val Main = AppRoutePath.main
    val Login = AppRoutePath.login
    val LoginPassword = AppRoutePath.loginPassword
    val LoginOtp = AppRoutePath.loginOtp
    val Register = AppRoutePath.register
    val Settings = AppRoutePath.settings
    val Web = AppRoutePath.web

    object Home {
        val Search = HomeRoutes.Search
        val Strategy = HomeRoutes.Strategy
        val LearningReport = HomeRoutes.LearningReport
        val CheckInMall = HomeRoutes.CheckInMall
        val DubbingFeed = HomeRoutes.DubbingFeed
        val AllServices = AppRoutePath.homeAllServices
        val LifeService = AppRoutePath.homeLifeService
        val LiveCommerce = AppRoutePath.homeLiveCommerce
        val Club = AppRoutePath.homeClub
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
        val Settings = MineRoutes.Settings
        val Personalized = MineRoutes.PersonalizedSettings
    }

    object Content {
        val Live = ContentRoutes.Live
        val Friend = ContentRoutes.Friend
        val Music = ContentRoutes.Music
        val AiStream = AiRoutes.Stream
        val MediaEntry = ContentRoutes.MediaEntry
    }
}

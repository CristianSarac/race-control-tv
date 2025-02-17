package fr.groggy.racecontrol.tv.ui.channel.playback

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import fr.groggy.racecontrol.tv.R
import fr.groggy.racecontrol.tv.core.ViewingService
import fr.groggy.racecontrol.tv.core.settings.SettingsRepository
import fr.groggy.racecontrol.tv.f1tv.F1TvViewing
import javax.inject.Inject

@AndroidEntryPoint
class ChannelPlaybackActivity : FragmentActivity(R.layout.activity_channel_playback) {
    @Inject internal lateinit var viewingService: ViewingService
    @Inject internal lateinit var settingsRepository: SettingsRepository

    companion object {
        fun intent(context: Context, channelId: String?, contentId: String): Intent {
            val intent = Intent(context, ChannelPlaybackActivity::class.java)
            ChannelPlaybackFragment.putChannelId(
                intent,
                channelId
            )
            ChannelPlaybackFragment.putContentId(
                intent,
                contentId
            )
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launchWhenCreated {
            attachViewingIfNeeded()
        }
    }

    private suspend fun attachViewingIfNeeded() {
        if (supportFragmentManager.findFragmentByTag(ChannelPlaybackFragment.TAG) == null) {
            val contentId = ChannelPlaybackFragment.findContentId(this) ?: return finish()
            val channelId = ChannelPlaybackFragment.findChannelId(this)
            try {
                val viewing = viewingService.getViewing(channelId, contentId)
                onViewingCreated(viewing)
            } catch (_: Exception) {
                handleError(R.string.unable_to_play_video_message)
            }
        }
    }

    private fun onViewingCreated(viewing: F1TvViewing) {
        if (settingsRepository.getCurrent().openWithExternalPlayer) {
            openWithExternalPlayer(viewing)
        } else {
            openWithInternalPlayer(viewing)
        }
    }

    private fun openWithExternalPlayer(viewing: F1TvViewing) {
        supportFragmentManager.commit {
            replace(R.id.fragment_container, OpenedWithExternalPlayerFragment(), ChannelPlaybackFragment.TAG)
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW)
                .setDataAndType(viewing.url, "video/*")
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            handleError(R.string.unable_to_open_with_external_player)
        }
    }

    private fun openWithInternalPlayer(viewing: F1TvViewing) {
        supportFragmentManager.commit {
            replace(R.id.fragment_container, ChannelPlaybackFragment.newInstance(viewing), ChannelPlaybackFragment.TAG)
        }
    }

    private fun handleError(@StringRes errorMessage: Int) {
        AlertDialog.Builder(this)
            .setCancelable(false)
            .setMessage(errorMessage)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                finish()
            }
    }
}

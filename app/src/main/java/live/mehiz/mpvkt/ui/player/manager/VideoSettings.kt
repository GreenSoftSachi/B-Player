package live.mehiz.mpvkt.ui.player.manager

import `is`.xyz.mpv.MPV
import `is`.xyz.mpv.prop
import `is`.xyz.mpv.propFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import live.mehiz.mpvkt.preferences.AdvancedPreferences
import live.mehiz.mpvkt.preferences.DecoderPreferences
import live.mehiz.mpvkt.preferences.PlayerPreferences
import live.mehiz.mpvkt.preferences.preference.deleteAndGet
import live.mehiz.mpvkt.ui.player.DebandSettings
import live.mehiz.mpvkt.ui.player.Debanding
import live.mehiz.mpvkt.ui.player.Decoder
import live.mehiz.mpvkt.ui.player.VideoAspect
import live.mehiz.mpvkt.ui.player.VideoFilters
import live.mehiz.mpvkt.ui.player.mapState

class VideoSettings(
  private val mpv: MPV,
  private val decoderPreferences: DecoderPreferences,
  private val advancedPreferences: AdvancedPreferences,
  private val playerPreferences: PlayerPreferences,
  scope: CoroutineScope,
) {
  private val _decoder = MutableStateFlow(
    if (decoderPreferences.tryHWDecoding.get()) Decoder.HWPlus else Decoder.SW,
  )
  val decoder: StateFlow<Decoder> = _decoder.asStateFlow()

  init {
    scope.launch {
      mpv.propFlow<String>("hwdec-current").collectLatest { current ->
        if (!current.isNullOrBlank() && current != "no") {
          _decoder.value = Decoder.getDecoderFromValue(current)
        }
      }
    }
  }

  val isGpuNext: StateFlow<Boolean> = mpv.propFlow<String>("vo").mapState(scope, { false }) { it == "gpu-next" }
  val debanding: StateFlow<Debanding> = decoderPreferences.debanding.stateIn(scope)
  val debandSettings: Map<DebandSettings, StateFlow<Int>> =
    DebandSettings.entries.associateWith { mpv.propFlow(it.mpvProperty, 0) }
  val filters: Map<VideoFilters, StateFlow<Int>> = VideoFilters.entries.associateWith {
    mpv.propFlow(
      it.mpvProperty,
      0,
    )
  }
  val statisticsPage: StateFlow<Int> = advancedPreferences.enabledStatisticsPage.stateIn(scope)
  val aspect: StateFlow<VideoAspect> = playerPreferences.videoAspect.stateIn(scope)

  fun setDecoder(dec: Decoder) {
    _decoder.value = dec
    val hwdecValue = when (dec) {
      Decoder.SW -> "no"
      Decoder.HW -> "mediacodec-copy"
      Decoder.HWPlus -> "mediacodec"
      Decoder.Auto, Decoder.AutoCopy -> "auto"
    }
    mpv.prop("hwdec", hwdecValue)
    try {
      val pos = mpv.getPropertyDouble("time-pos") ?: 0.0
      val path = mpv.getPropertyString("path")
      if (!path.isNullOrBlank()) {
        mpv.command("loadfile", path, "replace", "start=$pos")
      } else {
        mpv.command("seek", "0", "relative+exact")
      }
    } catch (_: Exception) {}
  }

  fun cycleDecoder() {
    setDecoder(
      when (decoder.value) {
        Decoder.HWPlus -> Decoder.HW
        Decoder.HW -> Decoder.SW
        Decoder.SW -> Decoder.HWPlus
        Decoder.AutoCopy, Decoder.Auto -> Decoder.SW
      },
    )
  }

  fun setDebanding(debanding: Debanding) {
    decoderPreferences.debanding.set(debanding)
    // only one may be on
    mpv.prop("deband", if (debanding == Debanding.GPU) "yes" else "no")
    if (debanding == Debanding.CPU) {
      mpv.command("vf", "add", "@deband:gradfun=radius=12")
    } else {
      mpv.command("vf", "remove", "@deband")
    }
  }

  fun resetDebanding() {
    mpv.prop("deband", "no")
    mpv.command("vf", "remove", "@deband")
    DebandSettings.entries.forEach { mpv.prop(it.mpvProperty, it.preference(decoderPreferences).deleteAndGet()) }
  }

  fun setDebandSetting(setting: DebandSettings, value: Int) {
    setting.preference(decoderPreferences).set(value)
    mpv.prop(setting.mpvProperty, value)
  }

  fun setFilter(filter: VideoFilters, value: Int) {
    filter.preference(decoderPreferences).set(value)
    mpv.prop(filter.mpvProperty, value)
  }

  fun resetFilters() {
    VideoFilters.entries.forEach { mpv.prop(it.mpvProperty, it.preference(decoderPreferences).deleteAndGet()) }
  }

  fun setStatisticsPage(page: Int) {
    if ((page == 0) xor (statisticsPage.value == 0)) mpv.command("script-binding", "stats/display-stats-toggle")
    if (page != 0) mpv.command("script-binding", "stats/display-page-$page")
    advancedPreferences.enabledStatisticsPage.set(page)
  }

  fun setAspect(aspect: VideoAspect, displayRatio: Double) {
    val (ratio, pan) = when (aspect) {
      VideoAspect.Crop -> -1.0 to 1.0
      VideoAspect.Fit -> -1.0 to 0.0
      VideoAspect.Stretch -> displayRatio to 0.0
    }
    mpv.prop("panscan", pan)
    mpv.prop("video-aspect-override", ratio)
    playerPreferences.videoAspect.set(aspect)
  }

  private var lastZoomTime = 0L
  fun zoomBy(zoomFactor: Float) {
    val now = System.currentTimeMillis()
    if (now - lastZoomTime < 40L) return
    lastZoomTime = now
    val currentZoom = mpv.getPropertyDouble("video-zoom") ?: 0.0
    val delta = (zoomFactor - 1.0) * 1.2
    if (kotlin.math.abs(delta) < 0.005) return
    val newZoom = (currentZoom + delta).coerceIn(0.0, 3.0)
    mpv.setPropertyDouble("video-zoom", newZoom)
  }

  private var lastPanTime = 0L
  fun panBy(dx: Float, dy: Float) {
    val now = System.currentTimeMillis()
    if (now - lastPanTime < 40L) return
    lastPanTime = now
    val currentPanX = mpv.getPropertyDouble("video-pan-x") ?: 0.0
    val currentPanY = mpv.getPropertyDouble("video-pan-y") ?: 0.0
    mpv.setPropertyDouble("video-pan-x", currentPanX + dx * 0.001)
    mpv.setPropertyDouble("video-pan-y", currentPanY + dy * 0.001)
  }

  fun resetZoomAndPan() {
    mpv.setPropertyDouble("video-zoom", 0.0)
    mpv.setPropertyDouble("video-pan-x", 0.0)
    mpv.setPropertyDouble("video-pan-y", 0.0)
  }
}

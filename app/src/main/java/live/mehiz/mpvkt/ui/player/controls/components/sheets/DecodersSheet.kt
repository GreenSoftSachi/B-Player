package live.mehiz.mpvkt.ui.player.controls.components.sheets

import androidx.compose.runtime.Composable
import kotlinx.collections.immutable.toImmutableList
import live.mehiz.mpvkt.ui.player.Decoder

@Composable
fun DecodersSheet(selectedDecoder: Decoder, onSelect: (Decoder) -> Unit, onDismissRequest: () -> Unit) {
  val decoderOptions = listOf(
    Decoder.Auto,
    Decoder.HWPlus,
    Decoder.HW,
    Decoder.SW,
  )

  GenericTracksSheet(
    decoderOptions.toImmutableList(),
    track = { dec ->
      val title = when (dec) {
        Decoder.Auto -> "Auto (Automatic Detection)"
        Decoder.HWPlus -> "HW+ (Hardware Plus - GPU Acceleration)"
        Decoder.HW -> "HW (Hardware - MediaCodec)"
        Decoder.SW -> "SW (Software - FFmpeg Core)"
        else -> dec.title
      }
      AudioTrackRow(
        title = title,
        isSelected = selectedDecoder == dec || (dec == Decoder.Auto && (selectedDecoder == Decoder.AutoCopy || selectedDecoder == Decoder.Auto)),
        onClick = { onSelect(dec) },
      )
    },
    onDismissRequest = onDismissRequest,
  )
}

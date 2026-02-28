# Playable Output Tests Design

## Goal

Unit tests that use real encoded H.264 video and Opus audio to produce playable fragmented MP4 files through all three packaging formats (CMAF, LOC, MoqMI). Validates that the serialization pipeline produces structurally correct, decodable output.

## Output Container

Fragmented MP4 (ISO BMFF). File structure:

```
ftyp  (isom, iso6, cmf2, cmfc)
moov  (mvhd + trak per track with codec config)
styp  (segment 1)
moof  (mfhd + traf with tfhd/tfdt/trun)
mdat  (encoded frames)
styp  (segment 2)
moof
mdat
...
```

Uses existing `Fmp4InitSegmentBuilder` and `Fmp4FragmentBuilder`.

## Test Media Generation

A shared `TestMediaGenerator` helper runs ffmpeg at test setup:

- **Video**: 2-second H.264 Baseline, 320x240, 30fps, 1 GOP per second
  ```
  ffmpeg -f lavfi -i "testsrc2=duration=2:size=320x240:rate=30" \
         -c:v libx264 -profile:v baseline -level 3.0 \
         -x264-params "keyint=30:min-keyint=30:scenecut=0" \
         -bsf:v h264_mp4toannexb -f h264 video.h264
  ```
- **Audio**: 2-second Opus mono, 48kHz, 20ms frames
  ```
  ffmpeg -f lavfi -i "sine=frequency=440:duration=2:sample_rate=48000" \
         -c:a libopus -b:a 64k -frame_duration 20 -f ogg audio.ogg
  ```

The helper parses these into frame records:
```java
record VideoFrame(byte[] avccData, long pts, long dts, long duration, boolean isKeyframe) {}
record AudioFrame(byte[] opusData, long pts, long duration, int sampleRate, int channels) {}
```

SPS/PPS are extracted from the H.264 bitstream to build AVCDecoderConfigurationRecord for init segments.

Tests skip gracefully via `Assumptions.assumeTrue()` when ffmpeg is not on PATH.

## Phase A: Single-Track Tests (PlayableOutputTest)

Six test methods, each producing a video-only or audio-only fMP4:

| Test | Format | Media | Flow |
|------|--------|-------|------|
| `cmafVideoProducesPlayableMp4` | CMAF | H.264 | Build init + fragments directly with builders |
| `cmafAudioProducesPlayableMp4` | CMAF | Opus | Build init + fragments directly with builders |
| `locVideoRoundTripProducesPlayableMp4` | LOC | H.264 | Serialize -> LocObject -> deserialize -> extract payload -> wrap in fMP4 |
| `locAudioRoundTripProducesPlayableMp4` | LOC | Opus | Same round-trip for audio |
| `moqmiVideoRoundTripProducesPlayableMp4` | MoqMI | H.264 | Serialize -> MoqMIObject -> deserialize -> extract payload -> wrap in fMP4 |
| `moqmiAudioRoundTripProducesPlayableMp4` | MoqMI | Opus | Same round-trip for audio |

LOC/MoqMI tests prove that payloads survive serialization round-trip and remain decodable.

## Phase B: Muxed Multi-Track Tests (MuxedPlayableOutputTest)

Three test methods producing dual-track (video + audio) fMP4:

| Test | Format | Flow |
|------|--------|------|
| `cmafMuxedVideoAudioProducesPlayableMp4` | CMAF | Init segment with 2 tracks, interleaved fragments |
| `locMuxedRoundTripProducesPlayableMp4` | LOC | Round-trip both tracks, mux into single fMP4 |
| `moqmiMuxedRoundTripProducesPlayableMp4` | MoqMI | Round-trip both tracks, mux into single fMP4 |

Init segment includes video track (trackId=1, timescale=90000, avcC config) and audio track (trackId=2, timescale=48000, dOps config). Fragments interleave by track ID.

## Validation (FfprobeValidator)

External tool validation using ffprobe and ffmpeg:

### Structural validation
- `ffprobe -show_streams -show_format -print_format json` on the output file
- Assert correct codec name (h264, opus), resolution, sample rate, channel count
- Assert expected number of streams (1 for Phase A, 2 for Phase B)

### Timestamp validation
- `ffprobe -show_packets -select_streams v:0 -print_format json`
- DTS strictly increasing for all streams
- PTS monotonically non-decreasing (video may have B-frame reordering)
- Audio PTS strictly increasing (no reordering)
- Frame duration consistent (video ~33ms, audio ~20ms)
- Total duration within 100ms of expected 2 seconds
- First keyframe PTS == 0

### Decode validation
- `ffmpeg -v error -i file.mp4 -f null -`
- Exit code 0 and zero error output = all frames decode cleanly

## File Layout

```
src/test/java/org/red5/io/moq/
  playable/
    TestMediaGenerator.java      -- ffmpeg runner + frame parser
    FfprobeValidator.java        -- ffprobe/ffmpeg validation
    PlayableOutputTest.java      -- Phase A: 6 single-track tests
    MuxedPlayableOutputTest.java -- Phase B: 3 muxed tests
```

## Dependencies

- ffmpeg and ffprobe on PATH (runtime only, not a build dependency)
- Tests use `Assumptions.assumeTrue()` to skip when unavailable
- No new Maven dependencies required
- All output written to JUnit `@TempDir`, cleaned up automatically

# Essentia Android dependency

Ukulele Studio uses [Essentia](https://essentia.upf.edu/) locally for audio feature extraction.

- Upstream: https://github.com/MTG/essentia
- License: GNU Affero General Public License v3 (AGPL-3.0)
- Android static packaging source: https://github.com/deeeed/rn-essentia-static
- Pinned packaging commit: `476d5cfa763ad8950bf91f876788e7d6739fdecc`

The release APK links Essentia into the local JNI analysis library. No Essentia web service is used. Android's `MediaExtractor` and `MediaCodec` decode MP3/M4A/WAV audio locally before samples are passed to Essentia.

Review AGPL-3.0 obligations before distributing this application beyond a personal/internal laboratory build.

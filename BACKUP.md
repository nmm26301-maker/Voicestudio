# VoiceStudio repository

Current source: **1.5.1-hybrid (versionCode 17)**. The 1.5.1 changes reorganize navigation and repair workflow issues; see README.md and CHANGELOG-1.5.1.md. Current checks are in validation/current-checks.json. No signed 1.5.1 upgrade APK is included. The original development signing key is required to produce an upgrade; unsigned builds are explicitly available for compilation validation.

A separate `VOICE_PREVIEW=1` build uses package `com.voice.studio.preview` and a dedicated preview key. The user's uploaded original APK matched the historical hash and certificate; the original private signing key remains unavailable. Preview certificate and isolation evidence are in validation/preview-checks-1.5.1.json. Preview is not an upgrade of the original installation.

The original backup below remains the source provenance. PROVENANCE.json and old validation logs describe their original releases, not current device acceptance.

---

# VoiceStudio 1.5.0 backup

This repository backs up source code, pinned runtime download manifest, tests, authored ONNX fixtures, design previews, licenses and build documentation from VoiceStudio 1.5.0-hybrid (versionCode 16).

The development signing key is retained in the private original source archive and excluded from this public repository. Restore the existing key to signing/voicestudio-development.p12 when producing an upgrade for existing installations. A newly generated key cannot cover-install the existing app.

No phone app data, user voices, works or Fish API keys are included.

The 53 MB third-party ONNX Runtime AAR is restored from Maven Central with the exact original SHA-256 using `python3 tools/fetch_runtime.py`; tools/build.sh also restores it automatically when missing. Java source and authored binary fixtures remain backed up in Git.

See SETUP.md and PROVENANCE.json for build steps and the precise scope of historical validation. The design images are illustrations, not device screenshots.

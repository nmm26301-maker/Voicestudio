# VoiceStudio repository

Current source: **1.5.15-hybrid (versionCode 31)**. `VOICE_STANDALONE=1` produces the formal-name VoiceStudio build using the shipped preview package and signing identity, so it can update that branch. See CHANGELOG-1.5.15.md and validation/upgrade-checks-1.5.15.json. No private key or APK binary is committed in this public repository.

Each delivered release must be backed up privately with its exact APK and the unchanged signing key, preserving older versions. `tools/backup_release.py` verifies the matching certificate and archived bytes. Never commit the release ZIP or private key.

The standalone signing key, its password/alias instructions and 1.5.15 APK are retained in the user's private `VoiceStudio-1.5.15-Release-Backup.zip`, under the historical filename `voicestudio-preview.p12`. Restore it to signing/ for future standalone updates. It is not the original 1.5.0 key.

A separate `VOICE_PREVIEW=1` build uses package `com.voice.studio.preview` and a dedicated preview key. The user's uploaded original APK matched the historical hash and certificate; the original private signing key remains unavailable. Preview certificate and isolation evidence are in validation/preview-checks-1.5.1.json. Preview is not an upgrade of the original installation.

The original backup below remains the source provenance. PROVENANCE.json and old validation logs describe their original releases, not current device acceptance.

---

# VoiceStudio 1.5.0 backup

This repository backs up source code, pinned runtime download manifest, tests, authored ONNX fixtures, design previews, licenses and build documentation from VoiceStudio 1.5.0-hybrid (versionCode 16).

The development signing key is retained in the private original source archive and excluded from this public repository. Restore the existing key to signing/voicestudio-development.p12 when producing an upgrade for existing installations. A newly generated key cannot cover-install the existing app.

No phone app data, user voices, works or Fish API keys are included.

The 53 MB third-party ONNX Runtime AAR is restored from Maven Central with the exact original SHA-256 using `python3 tools/fetch_runtime.py`; tools/build.sh also restores it automatically when missing. Java source and authored binary fixtures remain backed up in Git.

See SETUP.md and PROVENANCE.json for build steps and the precise scope of historical validation. The design images are illustrations, not device screenshots.

## GitHub private backup requirement

The user requires every delivered APK, the unchanged signing key and its password to be backed up on GitHub, preserving earlier releases. The proposed destination is the private `nmm26301-maker/Zizai` repository under `backups/VoiceStudio/<version>/`. Complete ZIPs for 1.5.1 through 1.5.15 are prepared, but this task has not committed them there: automatic review requires explicit authorization of that exact credential destination. Once that authorization is given, retain it for later releases; do not request it again for routine backups to the same destination. Never put these files in the public VoiceStudio repository.

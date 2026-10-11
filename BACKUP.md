# VoiceStudio 1.5.0 backup

This repository backs up source code, pinned runtime download manifest, tests, authored ONNX fixtures, design previews, licenses and build documentation from VoiceStudio 1.5.0-hybrid (versionCode 16).

The development signing key is retained in the private original source archive and excluded from this public repository. Restore the existing key to signing/voicestudio-development.p12 when producing an upgrade for existing installations. A newly generated key cannot cover-install the existing app.

No phone app data, user voices, works or Fish API keys are included.

The 53 MB third-party ONNX Runtime AAR is restored from Maven Central with the exact original SHA-256 using `python3 tools/fetch_runtime.py`; tools/build.sh also restores it automatically when missing. Java source and authored binary fixtures remain backed up in Git.

See SETUP.md and PROVENANCE.json for build steps and the precise scope of historical validation. The design images are illustrations, not device screenshots.

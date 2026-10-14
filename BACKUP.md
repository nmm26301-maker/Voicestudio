# VoiceStudio repository

Current source: **1.5.19-hybrid (versionCode 35)**. `VOICE_STANDALONE=1` retains the installed VoiceStudio signing/package identity. No APK or private signing key is committed to this public source repository.

The exact delivered APK and unchanged signing key/password instructions are retained in `VoiceStudio-1.5.19-Release-Backup.zip`. An AES-256-GCM encrypted copy and a separate `VoiceStudio-1.5.19-Backup-Recovery-Key.json` have also been prepared and round-trip verified. The plaintext ZIP independently restores the same APK/key and needs no decryption.

The proposed private GitHub destination is `nmm26301-maker/Zizai`, branch `backup/voicestudio-1.5.19-20261014`, path `backups/VoiceStudio/1.5.19`. The automatic approval review rejected the final backup tree mutation because the trusted user text did not explicitly authorize that signing payload in Zizai. No completed GitHub release backup exists at that path. Source, APK and saved direct recovery archives are unaffected. Explicit authorization of this exact private destination is required before completing the archive commit; retain that authorization for later routine releases. Do not upload signing data to the public repository.

Restore `signing/voicestudio-preview.p12` from the archive for future standalone builds. Never generate a replacement key. This is the established 1.5.1+ installation identity, not the original 1.5.0 identity. Preserve earlier release archives. `tools/backup_release.py` checks certificate continuity and exact archived bytes; the encrypted restore round-trip also matches the complete ZIP.

The original backup below remains the source provenance. PROVENANCE.json and old validation logs describe their original releases, not current device acceptance.

---

# VoiceStudio 1.5.0 backup

This repository backs up source code, pinned runtime download manifest, tests, authored ONNX fixtures, design previews, licenses and build documentation from VoiceStudio 1.5.0-hybrid (versionCode 16).

The development signing key is retained in the private original source archive and excluded from this public repository. Restore the existing key to signing/voicestudio-development.p12 when producing an upgrade for existing installations. A newly generated key cannot cover-install the existing app.

No phone app data, user voices, works or Fish API keys are included.

The 53 MB third-party ONNX Runtime AAR is restored from Maven Central with the exact original SHA-256 using `python3 tools/fetch_runtime.py`; tools/build.sh also restores it automatically when missing. Java source and authored binary fixtures remain backed up in Git.

See SETUP.md and PROVENANCE.json for build steps and the precise scope of historical validation. The design images are illustrations, not device screenshots.

## GitHub private backup requirement

Every delivered release must retain its exact APK and unchanged signature recovery data. Once explicitly authorized, use the private destination above; keep public source free of signing credentials. The prepared encrypted archive includes the actual APK, private key and password instructions; it is not yet committed to GitHub.

#!/usr/bin/env python3
"""Restore an encrypted private release ZIP with its separately retained recovery JSON.

Requires: python3 -m pip install cryptography
Usage: python3 restore_encrypted_backup.py BACKUP.zip.enc RECOVERY.json OUTPUT.zip
The output contains the unchanged APK, signing key and password instructions.
"""
import argparse
import hashlib
import io
import json
from pathlib import Path
import zipfile
from cryptography.hazmat.primitives.ciphers.aead import AESGCM

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('encrypted', type=Path)
parser.add_argument('recovery', type=Path)
parser.add_argument('output', type=Path)
args = parser.parse_args()
encrypted = args.encrypted.read_bytes()
recovery = json.loads(args.recovery.read_text())
if recovery['format'] != 'VoiceStudio-AES256-GCM-v1' or not encrypted.startswith(b'VSBK1'):
    raise ValueError('Unsupported backup format')
if hashlib.sha256(encrypted).hexdigest() != recovery['encryptedSha256']:
    raise ValueError('Encrypted backup checksum mismatch')
plain = AESGCM(bytes.fromhex(recovery['keyHex'])).decrypt(encrypted[5:17], encrypted[17:], b'VoiceStudio release backup v1')
if hashlib.sha256(plain).hexdigest() != recovery['sha256']:
    raise ValueError('Restored ZIP checksum mismatch')
with zipfile.ZipFile(io.BytesIO(plain)) as archive:
    if archive.testzip() is not None:
        raise ValueError('Restored ZIP integrity check failed')
with args.output.open('xb') as output:
    output.write(plain)
args.output.chmod(0o600)
print('Restored original release ZIP:', args.output)

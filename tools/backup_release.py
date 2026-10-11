#!/usr/bin/env python3
"""Archive the exact delivered APK and its existing key in a private directory.

Never commit this output. No keys are generated or changed by this script.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import zipfile

parser = argparse.ArgumentParser()
parser.add_argument("apk", type=Path)
parser.add_argument("destination", type=Path)
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
key = root / "signing/voicestudio-preview.p12"
apk = args.apk.resolve()
sdk = Path(os.environ["ANDROID_SDK_ROOT"]) / "build-tools/35.0.0"
certificate = "8d6cde5c3a0876b56a0191dbefd28160fc79604d1ebf7dff664cae736ff5f860"
signature = subprocess.check_output([str(sdk / "apksigner"), "verify", "--verbose", "--print-certs", str(apk)], text=True)
if "Signer #1 certificate SHA-256 digest: " + certificate not in signature:
    raise RuntimeError("APK signer differs from the established standalone identity")
exported = subprocess.check_output(["keytool", "-exportcert", "-keystore", str(key), "-storepass", "android", "-alias", "voicestudio-preview"], stderr=subprocess.DEVNULL)
if hashlib.sha256(exported).hexdigest() != certificate:
    raise RuntimeError("Backup key does not match the delivered APK")
badging = subprocess.check_output([str(sdk / "aapt"), "dump", "badging", str(apk)], text=True)
package = re.search(r"package: name='([^']+)'", badging)[1]
version = re.search(r"versionName='([^']+)'", badging)[1]
code = int(re.search(r"versionCode='(\d+)'", badging)[1])
if package != "com.voice.studio.preview":
    raise RuntimeError("Incorrect installation identity")
receipt = {"apk": apk.name, "apkSha256": hashlib.sha256(apk.read_bytes()).hexdigest(),
           "package": package, "version": version, "versionCode": code, "certificateSha256": certificate,
           "keySha256": hashlib.sha256(key.read_bytes()).hexdigest()}
destination = args.destination.resolve()
destination.mkdir(parents=True, exist_ok=True)
backup = destination / ("VoiceStudio-" + version.split("-", 1)[0] + "-Release-Backup.zip")
if backup.exists():
    raise FileExistsError("Release backup exists; choose a separate directory to preserve earlier bytes")
pending = backup.with_suffix(".pending")
instructions = """VoiceStudio 私有发布备份

本归档包含交付的完整 APK、对应的原有签名密钥及哈希校验记录。
不要上传到公开 GitHub。后续覆盖更新必须沿用此密钥与包名。

密钥恢复路径：signing/voicestudio-preview.p12
格式：PKCS12；别名：voicestudio-preview
密钥库密码：android；私钥密码：android
安装身份：com.voice.studio.preview（与 1.5.1 测试版及 1.5.2 正式版一致）
不是原 com.voice.studio / 1.5.0 的私钥。

构建：VOICE_STANDALONE=1 bash tools/build.sh
后续升级提高 versionCode，不要重新生成密钥。
每次交付均须私下备份该次 APK 和现有签名文件；保留旧版本备份。
"""
try:
    with zipfile.ZipFile(pending, "w", zipfile.ZIP_DEFLATED) as archive:
        archive.write(apk, "apk/" + apk.name)
        archive.write(key, "signing/voicestudio-preview.p12")
        archive.writestr("README.txt", instructions)
        archive.writestr("release-checks.json", json.dumps(receipt, indent=2) + "\n")
    pending.chmod(0o600)
    with zipfile.ZipFile(pending) as archive:
        if archive.testzip() is not None or archive.read("signing/voicestudio-preview.p12") != key.read_bytes():
            raise RuntimeError("Backup verification failed")
        if hashlib.sha256(archive.read("apk/" + apk.name)).hexdigest() != receipt["apkSha256"]:
            raise RuntimeError("Archived APK differs from delivered bytes")
    pending.rename(backup)
except BaseException:
    pending.unlink(missing_ok=True)
    raise
print(json.dumps({"status": "passed", "backup": str(backup), "apkSha256": receipt["apkSha256"], "certificateSha256": certificate}, indent=2))

"""Restore the exact upstream ONNX Runtime AAR used by the 1.5.0 backup."""
from pathlib import Path
import hashlib, urllib.request, os

ROOT=Path(__file__).resolve().parents[1]
TARGET=ROOT/'app/libs/onnxruntime-android-1.30.0.aar'
URL='https://repo.maven.apache.org/maven2/com/microsoft/onnxruntime/onnxruntime-android/1.30.0/onnxruntime-android-1.30.0.aar'
SHA256='e7fb945e402205f6db858d65bb78d2bdb0812317b383976c9e3bceb4862c73f1'

def digest(path):
    h=hashlib.sha256()
    with path.open('rb') as f:
        for data in iter(lambda:f.read(1024*1024),b''):h.update(data)
    return h.hexdigest()

if TARGET.is_file() and digest(TARGET)==SHA256:
    print('ONNX Runtime 1.30.0 already verified')
else:
    TARGET.parent.mkdir(parents=True,exist_ok=True)
    temp=TARGET.with_suffix('.aar.pending')
    try:
        with urllib.request.urlopen(URL,timeout=60) as response,temp.open('wb') as out:
            while True:
                data=response.read(1024*1024)
                if not data:break
                out.write(data)
        if digest(temp)!=SHA256:raise RuntimeError('ONNX Runtime checksum mismatch')
        os.replace(temp,TARGET)
        print('ONNX Runtime 1.30.0 restored and verified')
    finally:
        if temp.exists():temp.unlink()

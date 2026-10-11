"""Compile upstream NLLB tokenizer to a bounded binary format for native Android.

Requires tokenizers==0.21.4. No model inference, remote calls, or vocabulary edits.
"""
import json, struct, sys, unicodedata
from pathlib import Path
from tokenizers import Tokenizer

source, target = map(Path, sys.argv[1:3])
data = json.loads(source.read_text())
tokenizer = Tokenizer.from_file(str(source))
vocab = data['model']['vocab']
languages = {t['content']: t['id'] for t in data['added_tokens']
             if len(t['content']) == 8 and t['content'][3] == '_'}
overrides = {}
from tokenizers import normalizers
normalizer = normalizers.Precompiled(__import__('base64').b64decode(data['normalizer']['normalizers'][0]['precompiled_charsmap']))
for cp in range(0x110000):
    if 0xD800 <= cp <= 0xDFFF: continue
    char = chr(cp)
    actual = normalizer.normalize_str(char)
    if actual != unicodedata.normalize('NFKC', char): overrides[cp] = actual
target.parent.mkdir(parents=True, exist_ok=True)
with target.open('wb') as out:
    def integer(x): out.write(struct.pack('>i', x))
    def string(x):
        b = x.encode('utf-8'); integer(len(b)); out.write(b)
    integer(0x4e4c4231)
    integer(len(vocab))
    for piece, ident in sorted(vocab.items(), key=lambda kv: kv[1]):
        integer(ident); string(piece)
    integer(len(data['model']['merges']))
    for pair in data['model']['merges']:
        left, right = pair.split(' ')
        integer(vocab[left]); integer(vocab[right])
    integer(len(languages))
    for language, ident in sorted(languages.items()): string(language); integer(ident)
    integer(len(overrides))
    for cp, actual in overrides.items(): integer(cp); string(actual)
fixtures = []
for text in ['Hello, this is VoiceStudio.', '你好，我们在手机上离线翻译。',
             'Bonjour à tous! Ça va?', 'مرحبا بالعالم', 'नमस्ते दुनिया',
             '日本語の音声を作ります。', 'Привет, мир!', '👋  café\tA\u200bB\nＣ']:
    fixtures.append({'text': text, 'ids': tokenizer.encode(text).ids})
target.with_suffix('.fixtures.json').write_text(json.dumps(fixtures, ensure_ascii=False, indent=2))
print({'vocabulary':len(vocab), 'merges':len(data['model']['merges']),
       'languages':len(languages), 'normalization_overrides':len(overrides), 'bytes':target.stat().st_size})

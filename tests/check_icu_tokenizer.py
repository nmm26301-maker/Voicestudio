"""Test the production regex in native ICU; not an Android device test."""
import ctypes as c
import ctypes.util
import json
import re
from pathlib import Path

root = Path(__file__).resolve().parents[1]
name = ctypes.util.find_library('icui18n')
if not name:
    raise SystemExit('Native ICU is required for this optional compatibility check')
lib = c.CDLL(name)
suffix = '_' + re.search(r'\.(\d+)', name).group(1)
def bind(name, result, args):
    f = getattr(lib, name + suffix); f.restype = result; f.argtypes = args; return f
errorptr = c.POINTER(c.c_int32)
openregex = bind('uregex_open',c.c_void_p,[c.c_void_p,c.c_int32,c.c_uint32,c.c_void_p,errorptr])
settext = bind('uregex_setText',None,[c.c_void_p,c.c_void_p,c.c_int32,errorptr])
findnext = bind('uregex_findNext',c.c_int8,[c.c_void_p,errorptr])
start = bind('uregex_start',c.c_int32,[c.c_void_p,c.c_int32,errorptr])
end = bind('uregex_end',c.c_int32,[c.c_void_p,c.c_int32,errorptr])
close = bind('uregex_close',None,[c.c_void_p])
pattern = (root/'validation/tokenizer-pattern-1.3.9.txt').read_text().encode('utf-16-le')
buffer = c.create_string_buffer(pattern); status = c.c_int32(0)
regex = openregex(buffer,len(pattern)//2,0,None,c.byref(status))
assert regex and status.value <= 0, ('ICU compilation',status.value)
fixtures = json.loads((root/'validation/tokenizer-fixtures-1.3.9.json').read_text())
try:
    for fixture in fixtures:
        raw = fixture['text'].encode('utf-16-le'); textbuffer = c.create_string_buffer(raw)
        status.value = 0; settext(regex,textbuffer,len(raw)//2,c.byref(status))
        actual = []
        while findnext(regex,c.byref(status)):
            a,b = start(regex,0,c.byref(status)),end(regex,0,c.byref(status))
            actual.append(raw[a*2:b*2].decode('utf-16-le'))
        assert status.value <= 0, status.value
        assert actual == fixture['expected'], ('ICU token boundary mismatch',fixture)
finally:
    close(regex)
print(f'Native ICU {suffix[1:]}: {len(fixtures)} multilingual / whitespace fixtures passed with flags=0. Not phone inference.')

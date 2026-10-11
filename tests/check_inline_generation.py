"""Source routing guard; lifecycle and rendering still require a device."""
from pathlib import Path
import re
source = (Path(__file__).resolve().parents[1]/'app/src/main/java/com/voice/studio/MainActivity.java').read_text()
def body(signature):
    start=source.index(signature); start=source.index('{',start); depth=1; end=start+1
    while depth:
        depth += (source[end]=='{') - (source[end]=='}'); end+=1
    return source[start:end]
for signature in ['void generate()', 'void enqueueFishText(String text)']:
    method=body(signature)
    assert 'startActivity(' not in method and 'change(' not in method, signature+' navigates on generation'
    assert 'trackJob(StudioService.enqueue(this, job), job)' in method, signature+' does not track service job'
assert 'StudioService.resume(this, trackedJobId)' in body('void resumeTrackedJob()')
assert '.setAction("cancelTask").putExtra("id", trackedJobId)' in body('void cancelJob()')
print('Inline generation routing: Omni/Fish stay on page; per-task resume/cancel wired. Source guard only.')

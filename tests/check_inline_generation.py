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
assert 'resumeTrackedJob(false)' in body('void resumeTrackedJob()')
assert 'StudioService.resume(this, trackedJobId,allowResubmit)' in body('void resumeTrackedJob(boolean allowResubmit)')
assert 'ResubmitRequired' in body('void resumeTrackedJob(boolean allowResubmit)')
assert '.setAction("cancelTask").putExtra("id", trackedJobId)' in body('void cancelJob()')
assert 'SynthesisPreview.file' in body('void generationPanel()')
assert 'fragmentPreview=new AudioPreview' in body('void generationPanel()')
assert 'auditionId.equals(trackedJobId)' in body('void generationPanel()')
assert 'SynthesisPreview.file' in body('void updateGenerationPanel()')
assert 'fragmentPreview.close()' in body('void stopPreview()')
assert 'stopPreview()' in body('protected void onPause()')
print('Inline generation routing: Omni/Fish stay on page; per-task resume/cancel wired. Source guard only.')

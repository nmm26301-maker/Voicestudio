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

service=(Path(__file__).resolve().parents[1]/'app/src/main/java/com/voice/studio/StudioService.java').read_text()
state=service[service.index('void state(JSONObject job, String state, String message)'):service.index('void drain()')]
assert '!state.equals(previous)||progressGate.shouldWrite' in state
assert state.index('job.put("state", state).put("progress", message)') < state.index('if(persist)')
assert state.index('StudioFiles.save') < state.index('progressGate.written') < state.index('sendBroadcast')
assert 'stop.setVisibility(active&&!cancelRequested?View.VISIBLE:View.GONE)' in body('void updateGenerationPanel()')
print('Progress persistence integration: live detail updates before coalescing; successful disk save precedes gate acknowledgement; terminal preview controls hidden. Source guard only.')

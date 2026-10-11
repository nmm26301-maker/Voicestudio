"""Tiny authored graphs to exercise engine staging, not speech quality or model speed."""
from pathlib import Path
import json
import onnx
from onnx import helper as h, TensorProto as T

root = Path(__file__).resolve().parents[1] / 'validation/staging-fixture'
root.mkdir(exist_ok=True)
def tensor(name, dtype, dims, values):
    return h.make_tensor(name, dtype, dims, values)
def save(name, inputs, outputs, nodes, initializers):
    graph=h.make_graph(nodes, 'VoiceStudioAuthoredStagingDiagnostic', inputs, outputs, initializers)
    model=h.make_model(graph, producer_name='VoiceStudio tests; no speech weights',
                       opset_imports=[h.make_opsetid('', 13)])
    model.ir_version=8
    onnx.checker.check_model(model)
    onnx.save(model, root/name)

inputs=[h.make_tensor_value_info('input_ids',T.INT64,[2,8,'S']),
        h.make_tensor_value_info('audio_mask',T.BOOL,[2,'S']),
        h.make_tensor_value_info('attention_mask',T.INT64,[2,'S']),
        h.make_tensor_value_info('position_ids',T.INT64,[2,'S'])]
save('omnivoice.qint8_per_channel.onnx', inputs,
     [h.make_tensor_value_info('logits',T.FLOAT,[2,8,'S',1025])],
     [h.make_node('Cast',['input_ids'],['float_ids'],to=T.FLOAT),
      h.make_node('Mul',['float_ids','zero'],['zeros']),
      h.make_node('Unsqueeze',['zeros','axis'],['wide']),
      h.make_node('Add',['wide','vocab'],['logits'])],
     [tensor('zero',T.FLOAT,[],[0]), tensor('axis',T.INT64,[1],[3]),
      tensor('vocab',T.FLOAT,[1025],[10]+[0]*1024)])

save('higgs_decoder.onnx', [h.make_tensor_value_info('codes',T.INT64,[8,1,'frames'])],
     [h.make_tensor_value_info('audio',T.FLOAT,['samples'])],
     [h.make_node('Shape',['codes'],['shape']),h.make_node('Gather',['shape','frame_axis'],['frames'],axis=0),
      h.make_node('Mul',['frames','frame_samples'],['length']),
      h.make_node('Range',['begin','length','step'],['times']),
      h.make_node('Cast',['times'],['float_times'],to=T.FLOAT),
      h.make_node('Mul',['float_times','frequency'],['phase']),
      h.make_node('Sin',['phase'],['sine']),h.make_node('Mul',['sine','gain'],['audio'])],
     [tensor('frame_axis',T.INT64,[],[2]),tensor('frame_samples',T.INT64,[],[960]),
      tensor('begin',T.INT64,[],[0]),tensor('step',T.INT64,[],[1]),
      tensor('frequency',T.FLOAT,[],[0.05]),tensor('gain',T.FLOAT,[],[0.2])])

vocab={};extra=256
for i in range(256):
    cp=i if (33<=i<=126 or 161<=i<=172 or i>=174) else extra
    if cp==extra: extra+=1
    vocab[chr(cp)]=i
specials=['lang_start','lang_end','instruct_start','instruct_end','text_start','text_end']
fixture={'model':{'vocab':vocab,'merges':[]},
         'added_tokens':[{'content':'<|'+name+'|>','id':500+i} for i,name in enumerate(specials)]}
(root/'tokenizer.json').write_text(json.dumps(fixture,ensure_ascii=False))
print('Created synthetic engine fixtures. Not full OmniVoice model validation.')

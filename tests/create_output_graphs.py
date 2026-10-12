"""Small authored graphs for metadata pinning, fallback and first-run buffer tests."""
from pathlib import Path
import onnx
from onnx import helper as h, TensorProto as T

root = Path(__file__).resolve().parents[1] / 'validation/output-fixtures'
root.mkdir(exist_ok=True)
def save(name, output, nodes, initializers=(), fixed=False):
    s = 7 if fixed else 'S'
    inputs = [h.make_tensor_value_info('input_ids',T.INT64,[2,8,s]),
              h.make_tensor_value_info('audio_mask',T.BOOL,[2,s]),
              h.make_tensor_value_info('attention_mask',T.INT64,[2,s]),
              h.make_tensor_value_info('position_ids',T.INT64,[2,s])]
    graph = h.make_graph(nodes,'VoiceStudioOutputMetadataDiagnostic',inputs,[output],list(initializers))
    model = h.make_model(graph, producer_name='VoiceStudio authored fixture; no speech weights',
                         opset_imports=[h.make_opsetid('',13)])
    model.ir_version = 8
    onnx.checker.check_model(model)
    onnx.save(model,root/name)

save('static.onnx',h.make_tensor_value_info('logits',T.FLOAT,[2,8,7]),
     [h.make_node('Cast',['input_ids'],['logits'],to=T.FLOAT)],fixed=True)
save('unnamed.onnx',h.make_tensor_value_info('logits',T.FLOAT,['flat']),
     [h.make_node('Cast',['input_ids'],['floats'],to=T.FLOAT),
      h.make_node('Reshape',['floats','flat_shape'],['logits'])],
     [h.make_tensor('flat_shape',T.INT64,[1],[-1])])
save('half.onnx',h.make_tensor_value_info('logits',T.FLOAT16,[2,8,'S']),
     [h.make_node('Cast',['input_ids'],['logits'],to=T.FLOAT16)])
save('nonconforming.onnx',h.make_tensor_value_info('logits',T.FLOAT,[2,8,'S']),
     [h.make_node('Pad',['input_ids','pads'],['padded']),
      h.make_node('Cast',['padded'],['logits'],to=T.FLOAT)],
     [h.make_tensor('pads',T.INT64,[6],[0,0,0,0,0,1])])
print('Authored static/unnamed/half/nonconforming output fixtures generated.')

"""Authored four-input diagnostic graph for direct input / pinned output tests."""
from pathlib import Path
import onnx
from onnx import helper, TensorProto as T
root=Path(__file__).resolve().parents[1]
inputs=[helper.make_tensor_value_info('input_ids',T.INT64,[2,8,'S']),helper.make_tensor_value_info('audio_mask',T.BOOL,[2,'S']),helper.make_tensor_value_info('attention_mask',T.INT64,[2,'S']),helper.make_tensor_value_info('position_ids',T.INT64,[2,'S'])]
nodes=[helper.make_node('Cast',['input_ids'],['logits'],to=T.FLOAT),helper.make_node('Cast',['attention_mask'],['att'],to=T.FLOAT),helper.make_node('Cast',['audio_mask'],['mask'],to=T.FLOAT),helper.make_node('Cast',['position_ids'],['pos'],to=T.FLOAT)]
outputs=[helper.make_tensor_value_info('logits',T.FLOAT,[2,8,'S'])]+[helper.make_tensor_value_info(n,T.FLOAT,[2,'S']) for n in ['att','mask','pos']]
model=helper.make_model(helper.make_graph(nodes,'VoiceStudioBufferDiagnostic',inputs,outputs),producer_name='VoiceStudio diagnostics',opset_imports=[helper.make_opsetid('',13)])
model.ir_version=8;onnx.checker.check_model(model)
onnx.save(model,root/'validation/buffer-diagnostic.onnx')
print('Authored buffer diagnostic graph created; no speech model weights.')

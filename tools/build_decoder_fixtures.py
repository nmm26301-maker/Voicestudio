#!/usr/bin/env python3
"""Authored ONNX decoder diagnostics. Requires onnx 1.19.1; no speech weights."""
from pathlib import Path
import numpy as np
import onnx
from onnx import TensorProto as T, helper as h, numpy_helper as nh

folder = Path(__file__).resolve().parents[1] / 'validation/decoder-fixtures'
folder.mkdir(exist_ok=True)
for kind in ['float', 'half', 'variable']:
    initializers = [nh.from_array(np.array(v, dtype=d), name) for name, v, d in [
        ('axis', 2, np.int64), ('samples_per_frame', 480, np.int64),
        ('begin', 0, np.int64), ('step', 1, np.int64),
        ('frequency', .017, np.float32), ('gain', .2, np.float32)]]
    nodes = [h.make_node('Shape', ['codes'], ['shape']),
             h.make_node('Gather', ['shape', 'axis'], ['frames']),
             h.make_node('Mul', ['frames', 'samples_per_frame'], ['fixed_length']),
             h.make_node('Cast', ['codes'], ['float_codes'], to=T.FLOAT),
             h.make_node('ReduceMean', ['float_codes'], ['code_phase'], keepdims=0)]
    length = 'fixed_length'
    if kind == 'variable':
        initializers.append(nh.from_array(np.array([-1], np.int64), 'flat_shape'))
        initializers.append(nh.from_array(np.array(0, np.int64), 'index'))
        nodes += [h.make_node('Reshape', ['codes', 'flat_shape'], ['flat']),
                  h.make_node('Gather', ['flat', 'index'], ['variable_length'])]
        length = 'variable_length'
    nodes += [h.make_node('Range', ['begin', length, 'step'], ['times']),
              h.make_node('Cast', ['times'], ['float_times'], to=T.FLOAT),
              h.make_node('Mul', ['float_times', 'frequency'], ['phase']),
              h.make_node('Add', ['phase', 'code_phase'], ['signal_phase']),
              h.make_node('Sin', ['signal_phase'], ['sine']),
              h.make_node('Mul', ['sine', 'gain'], ['wave'])]
    nodes.append(h.make_node('Cast' if kind == 'half' else 'Identity', ['wave'], ['audio'],
                             **({'to': T.FLOAT16} if kind == 'half' else {})))
    graph = h.make_graph(nodes, 'VoiceStudioAuthoredDecoderDiagnostic',
        [h.make_tensor_value_info('codes', T.INT64, [8, 1, 'frames'])],
        [h.make_tensor_value_info('audio', T.FLOAT16 if kind == 'half' else T.FLOAT, ['samples'])], initializers)
    model = h.make_model(graph, producer_name='VoiceStudio regression fixtures',
                        opset_imports=[h.make_opsetid('', 13)], ir_version=8)
    onnx.checker.check_model(model)
    onnx.save(model, folder / (kind + '.onnx'))
    print(kind + '.onnx: authored diagnostic, not a speech model')

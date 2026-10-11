"""Rebuild the tiny authored diagnostic graph; requires the onnx Python package."""
from pathlib import Path
import onnx
from onnx import TensorProto, helper

path = Path(__file__).resolve().parents[1] / "app/src/main/assets/selftest-identity.onnx"
graph = helper.make_graph(
    [helper.make_node("Identity", ["input"], ["output"])],
    "VoiceStudioIdentityDiagnostic",
    [helper.make_tensor_value_info("input", TensorProto.FLOAT, [1, 2])],
    [helper.make_tensor_value_info("output", TensorProto.FLOAT, [1, 2])],
)
model = helper.make_model(graph, producer_name="VoiceStudio", opset_imports=[helper.make_opsetid("", 13)])
model.ir_version = 8
onnx.checker.check_model(model)
onnx.save(model, path)
print(path)

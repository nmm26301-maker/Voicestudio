package com.voice.studio;

import ai.onnxruntime.*;
import java.nio.*;
import java.io.IOException;
import java.util.*;

/** Reuses the four inputs and pins float outputs after learning the real output shape. */
final class LegacyTensorRun155 implements AutoCloseable {
    private final OrtEnvironment env;
    private final Map<String,OnnxTensor> feed = new LinkedHashMap<>();
    private final Map<String,OnnxTensor> pinned = new LinkedHashMap<>();
    private final List<OnnxTensor> owned = new ArrayList<>();
    private final LongBuffer ids;
    private FloatBuffer output;
    private long[] outputShape;
    private String outputName;
    private boolean closed;
    LegacyTensorRun155(OrtEnvironment env, int sequence, long[] input, byte[] mask, long[] attention, long[] positions) throws OrtException {
        this.env=env;
        ids=direct(input.length*8).asLongBuffer(); ids.put(input).rewind();
        try {
            add("input_ids",OnnxTensor.createTensor(env,ids,new long[]{2,8,sequence}));
            ByteBuffer bools=direct(mask.length); bools.put(mask).rewind();
            add("audio_mask",OnnxTensor.createTensor(env,bools,new long[]{2,sequence},OnnxJavaType.BOOL));
            LongBuffer att=direct(attention.length*8).asLongBuffer(); att.put(attention).rewind();
            add("attention_mask",OnnxTensor.createTensor(env,att,new long[]{2,sequence}));
            LongBuffer pos=direct(positions.length*8).asLongBuffer(); pos.put(positions).rewind();
            add("position_ids",OnnxTensor.createTensor(env,pos,new long[]{2,sequence}));
        } catch(OrtException|RuntimeException e) { close(); throw e; }
    }
    private static ByteBuffer direct(int bytes) { return ByteBuffer.allocateDirect(bytes).order(ByteOrder.nativeOrder()); }
    private void add(String name, OnnxTensor value) { owned.add(value); feed.put(name,value); }
    FloatBuffer run(OrtSession session, OrtSession.RunOptions options, long[] input) throws OrtException,IOException {
        if(closed) throw new IllegalStateException("Tensor buffers closed");
        ids.rewind(); ids.put(input).rewind();
        if(outputShape==null) {
            try(OrtSession.Result result=session.run(feed,options)) {
                OnnxTensor value=(OnnxTensor)result.get(0);
                FloatBuffer first=value.getFloatBuffer();
                if(first==null) throw new IOException("模型输出类型不兼容");
                if(value.getInfo().type!=OnnxJavaType.FLOAT) return first.asReadOnlyBuffer();
                outputShape=value.getInfo().getShape().clone();
                outputName=session.getOutputNames().iterator().next();
                // Keep this first copy as the reusable direct output storage for later steps.
                if(first.isDirect()) output=first;
                else { output=direct(Math.multiplyExact(first.remaining(),4)).asFloatBuffer(); output.put(first).rewind(); }
            }
            output.rewind();
            OnnxTensor value=OnnxTensor.createTensor(env,output,outputShape);
            owned.add(value); pinned.put(outputName,value);
            return output.asReadOnlyBuffer();
        }
        output.rewind();
        try(OrtSession.Result result=session.run(feed,Collections.emptySet(),pinned,options)) {
            return output.asReadOnlyBuffer();
        }
    }
    @Override public void close() {
        if(closed) return; closed=true;
        for(int i=owned.size()-1;i>=0;i--) owned.get(i).close();
        owned.clear(); feed.clear(); pinned.clear(); output=null;
    }
}

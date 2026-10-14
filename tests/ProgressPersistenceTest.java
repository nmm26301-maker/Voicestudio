package com.voice.studio;
public class ProgressPersistenceTest {
 static int checks;static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 static boolean emit(JobProgressGate gate,String id,String state,String message,long at){boolean write=gate.shouldWrite(id,state,message,at);if(write)gate.written(id,state,message,at);return write;}
 public static void main(String[] args) {
  JobProgressGate gate=new JobProgressGate();long at=0;
  check(emit(gate,"a","running","开始处理…",at),"job start always written");
  for(int i=1;i<=1000;i++)check(!emit(gate,"a","running","开始处理…",i*10000000L),"duplicate heartbeat performs no additional save/broadcast");
  check(emit(gate,"a","running","本机合成 · 0 / 32 步",at),"zero completed inference stage immediate");
  int writes=1;
  for(int i=1;i<=31;i++)if(emit(gate,"a","running","本机合成 · "+i+" / 32 步",i*10000000L))writes++;
  check(writes==2,"31 fast intermediate steps coalesce to one additional persistence");
  check(emit(gate,"a","running","本机合成 · 32 / 32 步",320000000L),"final inference step never dropped");
  check(emit(gate,"a","running","第 2 / 4 段 · 本机合成 · 0 / 16 步",330000000L),"new chunk stage immediately written");
  check(!emit(gate,"a","running","第 2 / 4 段 · 本机合成 · 1 / 16 步",340000000L),"fast same-chunk intermediate suppressed");
  check(emit(gate,"a","running",SynthesisPreview.READY,350000000L),"preview readiness bypasses throttle");
  check(emit(gate,"a","failed","模型运行失败",351000000L),"failure detail immediate");
  check(emit(gate,"a","paused","已暂停，可继续",352000000L),"pause immediate");
  check(emit(gate,"a","queued","等待继续",353000000L),"resume queue state immediate");
  check(emit(gate,"a","running","开始处理…",354000000L),"running transition after retry immediate");
  check(emit(gate,"a","done","已完成，可导出",355000000L),"successful result immediate");
  check(emit(gate,"b","running","已完成，可导出",356000000L),"new job with identical text independent");
  gate=new JobProgressGate();check(gate.shouldWrite("a","running","免费 GPU 排队或合成中…",0),"first update accepted");
  check(gate.shouldWrite("a","running","免费 GPU 排队或合成中…",1),"failed persistence not marked, next heartbeat retries");
  gate.written("a","running","免费 GPU 排队或合成中…",1);check(!gate.shouldWrite("a","running","免费 GPU 排队或合成中…",1000000000),"successful heartbeat deduplicated");
  gate=new JobProgressGate();gate.written("a","running","本机合成 · 1 / 16 步",1000000000);
  check(gate.shouldWrite("a","running","本机合成 · 2 / 16 步",0),"clock reversal cannot starve updates");
  check(gate.shouldWrite("a","running","本机合成 · 2 / 16 步",1250000000),"quarter-second threshold allows next changed step");
  check(gate.shouldWrite("a","running","本机合成 · 16 / 16 步",1000000001),"final step immediate within throttle interval");
  for(String stage:new String[]{"加载本机音频解码器…","完整分段已保存，正在合并…","本机合成 · 999999999999 / 16 步","第 3 / 4 段 · 本机合成 · 1 / 16 步"})check(gate.shouldWrite("a","running",stage,1000000001),"phase/invalid counters do not disappear");
  System.out.println("Progress persistence: "+checks+" heartbeat/fast-step/lifecycle/preview/failed-save/clock checks passed; host only.");
 }
}

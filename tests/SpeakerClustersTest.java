package com.voice.studio;
import java.io.*;import java.util.*;import java.util.concurrent.atomic.AtomicBoolean;
public class SpeakerClustersTest {
 static int checks;static void check(boolean b,String label){checks++;if(!b)throw new AssertionError(label);}
 static void reject(List<float[]> x,List<Double> w,int k)throws Exception {try{SpeakerClusters.group(x,w,k,.55,new AtomicBoolean());throw new AssertionError("Accepted invalid");}catch(IOException expected){checks++;}}
 static int oldGreedy(List<float[]> samples,double threshold) {List<float[]> centers=new ArrayList<>();for(float[] raw:samples){double norm=0;for(float v:raw)norm+=v*v;float[] x=raw.clone();for(int d=0;d<x.length;d++)x[d]/=Math.sqrt(norm);double best=threshold;int match=-1;for(int i=0;i<centers.size();i++){double dot=SpeakerClusters.dot(x,centers.get(i));if(dot>best){best=dot;match=i;}}if(match<0)centers.add(x);}return centers.size();}
 public static void main(String[] args)throws Exception {
  List<float[]> features=new ArrayList<>();List<Double> weights=new ArrayList<>();List<Integer> truth=new ArrayList<>();
  for(int repeat=0;repeat<8;repeat++)for(int speaker=0;speaker<3;speaker++){float[] v=new float[6];v[speaker]=1;features.add(v);weights.add(8.0);truth.add(speaker);}
  for(int speaker=0;speaker<3;speaker++)for(int sign:new int[]{-1,1}){float[] v=new float[6];v[speaker]=.35f;v[3+speaker]=sign*.94f;features.add(v);weights.add(.6);truth.add(speaker);}
  check(oldGreedy(features,.55)>3,"old greedy fixture fragments three people into extra groups");
  int[] labels=SpeakerClusters.group(features,weights,3,.55,new AtomicBoolean());
  Set<Integer> people=new HashSet<>();for(int v:labels)people.add(v);check(people.size()==3,"three-person request yields three supported global groups");
  for(int i=0;i<labels.length;i++)for(int j=0;j<labels.length;j++)check((labels[i]==labels[j])==truth.get(i).equals(truth.get(j)),"same person reunified across different delivery");
  check(Arrays.equals(labels,SpeakerClusters.group(features,weights,3,.95,new AtomicBoolean())),"known-count results do not change with automatic threshold");
  int[] one=SpeakerClusters.group(features,weights,1,.55,new AtomicBoolean());for(int v:one)check(v==1,"single speaker constraint");
  List<float[]> clean=new ArrayList<>();List<Double> cleanWeights=new ArrayList<>();for(int s=0;s<3;s++)for(int n=0;n<8;n++){float[] v=new float[4];v[s]=1;v[3]=n*.01f;clean.add(v);cleanWeights.add(2.0);}
  int[] auto=SpeakerClusters.group(clean,cleanWeights,0,.55,new AtomicBoolean());Set<Integer> found=new HashSet<>();for(int v:auto)found.add(v);check(found.size()==3,"automatic mode preserves clearly separated speakers");
  int[] duplicate=SpeakerClusters.group(Arrays.asList(new float[]{1,0},new float[]{1,0},new float[]{1,0}),Arrays.asList(1.0,1.0,1.0),3,.55,new AtomicBoolean());check(duplicate[0]==duplicate[1]&&duplicate[1]==duplicate[2],"identical evidence does not fabricate missing people");
  check(SpeakerClusters.group(Collections.emptyList(),Collections.emptyList(),3,.55,new AtomicBoolean()).length==0,"empty speech creates no person");
  reject(features,weights,-1);reject(features,weights,33);reject(Arrays.asList(new float[]{0,0}),Arrays.asList(1.0),3);reject(Arrays.asList(new float[]{Float.NaN,0}),Arrays.asList(1.0),3);reject(Arrays.asList(new float[]{1,0}),Arrays.asList(0.0),3);
  try{SpeakerClusters.group(features,weights,3,.55,new AtomicBoolean(true));throw new AssertionError("Ignored cancel");}catch(InterruptedIOException expected){checks++;}
  check(features.get(0)[0]==1&&features.get(0)[1]==0,"input embeddings remain unchanged");
  System.out.println("Global speaker clustering: "+checks+" passed; three-person fragmentation counterexample and constraints, no real video identity claim.");
 }
}

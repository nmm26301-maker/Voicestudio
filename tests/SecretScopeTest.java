package com.voice.studio;
/** Executes the real constructor, without touching Android Keystore or credentials. */
public final class SecretScopeTest {
 public static void main(String[] args)throws Exception{
  int count=0;for(String scope:new String[]{"accelerated","gpu-hf","gpu-modal","gpu-lightning","official-fish","official-elevenlabs","official-minimax","official-minimax-cn","official-cartesia"}){new SecretStore(null,scope);count++;}
  for(String model:CloudDeploymentConfig.ENGINES){for(String scope:new String[]{"gateway-"+model,"gpu-"+model+"-modal","gpu-"+model+"-lightning"}){new SecretStore(null,scope);count++;}}
  new SecretStore(null,"gpu-qwen3tts-hf");count++;
  try{new SecretStore(null,"gateway");throw new AssertionError("Unknown scope accepted");}catch(IllegalArgumentException expected){count++;}
  System.out.println("Secret scopes: "+count+" real constructor checks passed; no Keystore access.");
 }
}

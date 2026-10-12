package com.voice.studio;
import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

final class SecretStore {
    private final String alias, preference;
    private final Context context;
    SecretStore(Context c) { context=c;alias="voicestudio.api.v1";preference="secure"; }
    SecretStore(Context c, String scope) {
        if(!"accelerated".equals(scope))throw new IllegalArgumentException("Unknown secret scope");
        context=c;alias="voicestudio.api.v1."+scope;preference="secure."+scope;
    }
    private SecretKey key() throws Exception {
        KeyStore ks=KeyStore.getInstance("AndroidKeyStore"); ks.load(null);
        if (!ks.containsAlias(alias)) {
            KeyGenerator gen=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
            gen.init(new KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());gen.generateKey();
        }
        return (SecretKey) ks.getKey(alias,null);
    }
    void save(String secret) throws Exception {
        if (secret.isEmpty()) {if (!context.getSharedPreferences(preference,0).edit().clear().commit()) throw new Exception("密钥清除失败");return;}
        Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key());
        String value=Base64.encodeToString(c.getIV(),Base64.NO_WRAP)+":"+Base64.encodeToString(c.doFinal(secret.getBytes("UTF-8")),Base64.NO_WRAP);
        if(!context.getSharedPreferences(preference,0).edit().putString("token",value).commit()) throw new Exception("密钥保存失败");
    }
    String read() throws Exception {
        String stored=context.getSharedPreferences(preference,0).getString("token","");if(stored.isEmpty())return "";
        String[] p=stored.split(":");Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(p[0],Base64.NO_WRAP)));
        return new String(c.doFinal(Base64.decode(p[1],Base64.NO_WRAP)),"UTF-8");
    }
}

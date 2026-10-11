package android.content;
import java.io.File;
import android.content.res.AssetManager;
/** Host test adapter; never packaged in the Android app. */
public abstract class Context {
    public abstract File getFilesDir();
    public AssetManager getAssets() { return new AssetManager(); }
}

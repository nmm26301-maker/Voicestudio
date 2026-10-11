package com.voice.studio;

import android.app.Application;

/** Recover persisted jobs once, before any activity or service starts. */
public class StudioApplication extends Application {
    @Override public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks(){
            public void onActivityCreated(android.app.Activity a,android.os.Bundle b){}
            public void onActivityStarted(android.app.Activity a){}
            public void onActivityResumed(android.app.Activity a){CosmicUi.lifecycle(a.getWindow().getDecorView(),true);}
            public void onActivityPaused(android.app.Activity a){CosmicUi.lifecycle(a.getWindow().getDecorView(),false);}
            public void onActivityStopped(android.app.Activity a){CosmicUi.lifecycle(a.getWindow().getDecorView(),false);}
            public void onActivityDestroyed(android.app.Activity a){CosmicUi.lifecycle(a.getWindow().getDecorView(),false);}
            public void onActivitySaveInstanceState(android.app.Activity a,android.os.Bundle b){}
        });
        StudioFiles.recoverInterrupted(this);
        OmniModels.recoverImports(getFilesDir());
        ModelImports.recover(this);
    }
    @Override public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if(level>=android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) OmniEngine.tokenizerCache.clear();
    }
    @Override public void onLowMemory() { super.onLowMemory(); OmniEngine.tokenizerCache.clear(); }
}

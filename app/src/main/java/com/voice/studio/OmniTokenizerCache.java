package com.voice.studio;

import java.io.File;

/** One tokenizer per engine, invalidated when an installed model file changes. */
final class OmniTokenizerCache {
    private File path;
    private long size, modified;
    private OmniTokenizer tokenizer;
    synchronized void clear() { tokenizer=null; path=null; }
    synchronized OmniTokenizer get(File file) throws Exception {
        File canonical=file.getCanonicalFile();
        if(tokenizer==null || !canonical.equals(path) || size!=file.length() || modified!=file.lastModified()) {
            OmniTokenizer loaded=new OmniTokenizer(file);
            path=canonical; size=file.length(); modified=file.lastModified(); tokenizer=loaded;
        }
        return tokenizer;
    }
}

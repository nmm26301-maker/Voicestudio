package android.util;
import java.io.*;
/** Simplified filesystem adapter. Does not validate Android fsync/AtomicFile behavior. */
public class AtomicFile {
    final File file;
    public AtomicFile(File value) { file = value; }
    public FileInputStream openRead() throws IOException {
        File backup = new File(file.getPath()+".bak");
        if (backup.isFile()) { file.delete(); if (!backup.renameTo(file)) throw new IOException("restore failed"); }
        return new FileInputStream(file);
    }
    public FileOutputStream startWrite() throws IOException { return new FileOutputStream(new File(file.getPath()+".new")); }
    public void finishWrite(FileOutputStream out) { try { out.close(); new File(file.getPath()+".new").renameTo(file); } catch(IOException e) { throw new RuntimeException(e); } }
    public void failWrite(FileOutputStream out) { try {out.close();}catch(IOException ignored){} new File(file.getPath()+".new").delete(); }
}

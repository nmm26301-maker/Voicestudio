package com.voice.studio;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;

public class AudioProvider extends ContentProvider {
    public static Uri uri(Context context, File file) {
        return new Uri.Builder()
                .scheme("content")
                .authority(context.getPackageName() + ".audio")
                .appendPath(file.getName())
                .build();
    }

    public boolean onCreate() {
        return true;
    }

    private File resolve(Uri uri) throws FileNotFoundException {
        String name = uri.getLastPathSegment();
        if (name == null || !name.matches("[a-f0-9-]+\\.(mp3|wav|mp4|zip|txt|srt|json)"))
            throw new FileNotFoundException();
        File f = new File(new File(getContext().getFilesDir(), "audio"), name);
        if (!f.isFile()) throw new FileNotFoundException();
        return f;
    }

    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) throw new FileNotFoundException("Read only");
        return ParcelFileDescriptor.open(resolve(uri), ParcelFileDescriptor.MODE_READ_ONLY);
    }

    public String getType(Uri uri) {
        if (uri.toString().endsWith(".mp4")) return "video/mp4";
        if (uri.toString().endsWith(".txt") || uri.toString().endsWith(".srt")) return "text/plain";
        if (uri.toString().endsWith(".json")) return "application/json";
        return uri.toString().endsWith(".zip")
                ? "application/zip"
                : uri.toString().endsWith(".wav") ? "audio/wav" : "audio/mpeg";
    }

    public Cursor query(
            Uri uri, String[] projection, String selection, String[] args, String order) {
        try {
            File f = resolve(uri);
            String[] cols =
                    projection == null
                            ? new String[] {OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}
                            : projection;
            MatrixCursor cursor = new MatrixCursor(cols);
            Object[] row = new Object[cols.length];
            for (int i = 0; i < cols.length; i++)
                row[i] = OpenableColumns.SIZE.equals(cols[i]) ? f.length() : f.getName();
            cursor.addRow(row);
            return cursor;
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    public int delete(Uri uri, String s, String[] a) {
        throw new UnsupportedOperationException();
    }

    public int update(Uri uri, ContentValues v, String s, String[] a) {
        throw new UnsupportedOperationException();
    }
}

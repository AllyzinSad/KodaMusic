package com.koda.anime;

import java.io.*;
import java.nio.charset.StandardCharsets;

/** Stores public title metadata only; episode URLs and account data are excluded. */
final class CatalogDiskCache {
    private static final int MAX_BYTES = 8_000_000;
    private final File directory;
    CatalogDiskCache(File directory) { this.directory = directory; }
    private File file(String url) {
        if (url.equals("https://animestvs.org/animes")) return new File(directory,"catalog.json");
        if (url.equals("https://animestvs.org/animes-famosos")) return new File(directory,"recommended.json");
        return null;
    }
    synchronized String read(String url,long maxAgeMillis,long now) {
        File file=file(url);
        if(file==null || !file.isFile() || file.length()>MAX_BYTES || now-file.lastModified()>maxAgeMillis) return null;
        try(InputStream in=new FileInputStream(file);ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            byte[] block=new byte[8192];int size;
            while((size=in.read(block))!=-1){out.write(block,0,size);if(out.size()>MAX_BYTES)return null;}
            return out.toString("UTF-8");
        } catch(IOException ignored) { return null; }
    }
    synchronized void write(String url,String json) {
        File file=file(url);if(file==null)return;
        byte[] bytes=json.getBytes(StandardCharsets.UTF_8);if(bytes.length>MAX_BYTES)return;
        if(!directory.isDirectory() && !directory.mkdirs())return;
        File temp=new File(directory,file.getName()+".tmp");
        try(FileOutputStream out=new FileOutputStream(temp)) {out.write(bytes);out.getFD().sync();}
        catch(IOException ignored){temp.delete();return;}
        if(!temp.renameTo(file))temp.delete();
    }
}

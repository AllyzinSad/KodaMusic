package com.koda.anime;

import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import static org.junit.Assert.*;

public class CatalogDiskCacheTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private static final String URL="https://animestvs.org/animes";
    @Test public void survivesRestartAndExpires() throws Exception {
        File folder=temporary.newFolder();
        new CatalogDiskCache(folder).write(URL,"[{\"titulo\":\"Teste 日本\"}]");
        long modified=new File(folder,"catalog.json").lastModified();
        CatalogDiskCache reopened=new CatalogDiskCache(folder);
        assertEquals("[{\"titulo\":\"Teste 日本\"}]",reopened.read(URL,3600000,modified+100));
        assertNull(reopened.read(URL,3600000,modified+3600001));
        assertNotNull(reopened.read(URL,7L*24*3600000,modified+3600001));
    }
    @Test public void neverStoresEpisodeLinksOrAccounts() throws Exception {
        File folder=temporary.newFolder();CatalogDiskCache cache=new CatalogDiskCache(folder);
        for(String url:new String[]{"https://animestvs.org/episodios-recentes","https://animestvs.org/animes-legendados/test/episodios","https://accounts.google.com/token"}){
            cache.write(url,"secret");assertNull(cache.read(url,3600000,System.currentTimeMillis()));
        }
        assertEquals(0,folder.list().length);
    }
    @Test public void replacesOldDataAndMissingFilesAreCacheMisses() throws Exception {
        File folder=temporary.newFolder();CatalogDiskCache cache=new CatalogDiskCache(folder);
        assertNull(cache.read(URL,3600000,System.currentTimeMillis()));
        cache.write(URL,"[]");cache.write(URL,"[1]");
        assertEquals("[1]",cache.read(URL,3600000,System.currentTimeMillis()));
        assertFalse(new File(folder,"catalog.json.tmp").exists());
    }
}

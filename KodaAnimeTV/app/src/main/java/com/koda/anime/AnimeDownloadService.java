package com.koda.anime;

import android.app.Notification;
import android.content.Context;
import androidx.annotation.Nullable;
import androidx.media3.exoplayer.offline.*;
import androidx.media3.exoplayer.scheduler.Scheduler;
import java.util.List;

public final class AnimeDownloadService extends DownloadService {
    private static final int NOTIFICATION_ID=8401;
    private static final String CHANNEL_ID="koda_anime_downloads";

    public AnimeDownloadService(){
        super(NOTIFICATION_ID,DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,CHANNEL_ID,R.string.download_channel_name,0);
    }

    static void add(Context c,DownloadRequest request){sendAddDownload(c,AnimeDownloadService.class,request,false);}
    static void remove(Context c,String id){sendRemoveDownload(c,AnimeDownloadService.class,id,false);}

    @Override protected DownloadManager getDownloadManager(){return AnimeDownloads.manager(this);}
    @Override @Nullable protected Scheduler getScheduler(){return null;}

    @Override protected Notification getForegroundNotification(List<Download> downloads,int notMetRequirements){
        DownloadNotificationHelper helper=new DownloadNotificationHelper(this,CHANNEL_ID);
        return helper.buildProgressNotification(this,android.R.drawable.stat_sys_download,null,null,downloads,notMetRequirements);
    }
}

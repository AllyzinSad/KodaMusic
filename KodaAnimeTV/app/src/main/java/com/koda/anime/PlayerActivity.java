package com.koda.anime;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.media3.common.*;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import java.util.*;

public final class PlayerActivity extends Activity {
    private static final int WHITE=0xFFF7F7F9, RED=0xFFE7333E;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private ExoPlayer player; private PlayerView video; private LinearLayout center,bottom; private TextView heading,playPause,quality,next,time;
    private SeekBar progress; private View tap; private boolean tracking; private long startPosition,openingEndMs=-1;
    private String url,title,animeId,poster,episode,nextUrl,nextLabel;
    private int dp(int n){return (int)(getResources().getDisplayMetrics().density*n+.5f);}
    private GradientDrawable bg(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
    private TextView control(String text,int size,Runnable action){TextView v=new TextView(this);v.setText(text);v.setTextColor(WHITE);v.setTextSize(size);v.setGravity(Gravity.CENTER);v.setPadding(dp(14),dp(5),dp(14),dp(5));v.setBackground(bg(Color.TRANSPARENT,12));v.setFocusable(true);
        v.setOnFocusChangeListener((view,focus)->{v.setBackground(bg(focus?0xCCAE263C:Color.TRANSPARENT,12));if(focus)showControls();});
        v.setOnClickListener(view->{action.run();showControls();});return v;}
    private void add(LinearLayout row,View v,int height){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,dp(height));p.rightMargin=dp(11);row.addView(v,p);}
    @Override public void onCreate(Bundle state){super.onCreate(state);
        Intent i=getIntent();url=i.getStringExtra("url");title=i.getStringExtra("title");animeId=i.getStringExtra("animeId");poster=i.getStringExtra("poster");episode=i.getStringExtra("episode");nextUrl=i.getStringExtra("nextUrl");nextLabel=i.getStringExtra("nextLabel");startPosition=i.getLongExtra("startPosition",0);openingEndMs=i.getLongExtra("openingEndMs",-1);
        if(url==null||!url.startsWith("https://")){Toast.makeText(this,"URL HTTPS inválida",Toast.LENGTH_LONG).show();finish();return;}
        getWindow().getDecorView().setSystemUiVisibility(5894|View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        FrameLayout root=new FrameLayout(this);root.setBackgroundColor(Color.BLACK);setContentView(root);
        video=new PlayerView(this);video.setUseController(false);root.addView(video,new FrameLayout.LayoutParams(-1,-1));
        tap=new View(this);tap.setFocusable(true);tap.setOnClickListener(v->showControls());root.addView(tap,new FrameLayout.LayoutParams(-1,-1));
        heading=new TextView(this);heading.setText((episode==null?"":episode)+"\n"+(title==null?"Koda Anime":title));heading.setTextColor(WHITE);heading.setTextSize(16);heading.setPadding(dp(30),dp(15),dp(30),dp(15));heading.setBackgroundColor(0x9909090C);
        root.addView(heading,new FrameLayout.LayoutParams(-1,-2,Gravity.TOP));
        center=new LinearLayout(this);center.setGravity(Gravity.CENTER);FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(-1,-2,Gravity.CENTER);root.addView(center,cp);
        add(center,control("↶\n10",24,()->seekBy(-10000)),70);
        playPause=control("❚❚",38,()->{if(player==null)return;if(player.isPlaying())player.pause();else player.play();update();});add(center,playPause,70);
        add(center,control("↷\n10",24,()->seekBy(10000)),70);
        bottom=new LinearLayout(this);bottom.setOrientation(1);bottom.setPadding(dp(24),dp(12),dp(24),dp(16));bottom.setBackgroundColor(0xB609090C);
        FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);root.addView(bottom,bp);
        LinearLayout seekRow=new LinearLayout(this);seekRow.setGravity(Gravity.CENTER_VERTICAL);bottom.addView(seekRow,new LinearLayout.LayoutParams(-1,dp(34)));
        time=new TextView(this);time.setTextColor(WHITE);time.setTextSize(13);time.setText("00:00 / 00:00");seekRow.addView(time);
        progress=new SeekBar(this);progress.setProgressTintList(android.content.res.ColorStateList.valueOf(RED));progress.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF77777D));progress.setThumbTintList(android.content.res.ColorStateList.valueOf(RED));
        seekRow.addView(progress,new LinearLayout.LayoutParams(0,dp(34),1));progress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override public void onStartTrackingTouch(SeekBar s){tracking=true;handler.removeCallbacks(hide);}
            @Override public void onStopTrackingTouch(SeekBar s){tracking=false;if(player!=null)seekTo(s.getProgress()*Math.max(0,player.getDuration())/1000);showControls();}
            @Override public void onProgressChanged(SeekBar s,int p,boolean user){if(user && player!=null)time.setText(format(p*Math.max(0,player.getDuration())/1000)+" / "+format(player.getDuration()));}
        });
        LinearLayout actions=new LinearLayout(this);actions.setGravity(Gravity.CENTER);bottom.addView(actions,new LinearLayout.LayoutParams(-1,dp(68)));
        quality=control("⚙\nQualidade: Auto",15,this::showQuality);actions.addView(quality,new LinearLayout.LayoutParams(0,dp(65),1));
        next=control("⏭\nPróximo episódio",15,this::playNext);actions.addView(next,new LinearLayout.LayoutParams(0,dp(65),1));next.setVisibility(nextUrl!=null&&nextUrl.startsWith("https://")?View.VISIBLE:View.GONE);
        TextView skip=control("⏩\nPular abertura",15,()->{if(player!=null)seekTo(openingEndMs>0&&openingEndMs>player.getCurrentPosition()?openingEndMs:player.getCurrentPosition()+90000);});actions.addView(skip,new LinearLayout.LayoutParams(0,dp(65),1));
        showControls();handler.post(updateLoop);
    }
    private String format(long ms){long s=Math.max(0,ms)/1000;return String.format(Locale.ROOT,"%02d:%02d",s/60,s%60);}
    private final Runnable hide=()->{if(tracking)return;heading.setVisibility(View.GONE);center.setVisibility(View.GONE);bottom.setVisibility(View.GONE);tap.requestFocus();};
    private final Runnable updateLoop=new Runnable(){@Override public void run(){update();handler.postDelayed(this,600);}};
    private void showControls(){heading.setVisibility(View.VISIBLE);center.setVisibility(View.VISIBLE);bottom.setVisibility(View.VISIBLE);handler.removeCallbacks(hide);handler.postDelayed(hide,4500);}
    private void update(){if(player==null)return;long pos=player.getCurrentPosition(),dur=player.getDuration();playPause.setText(player.isPlaying()?"❚❚":"▶");
        if(!tracking){time.setText(format(pos)+" / "+format(dur));progress.setProgress(dur>0?(int)Math.min(1000,pos*1000/dur):0);}
    }
    @Override protected void onStart(){super.onStart();if(url==null||player!=null)return;
        player=new ExoPlayer.Builder(this).build();video.setPlayer(player);
        player.addListener(new Player.Listener(){@Override public void onPlayerError(PlaybackException e){Toast.makeText(PlayerActivity.this,"Não foi possível reproduzir: "+e.getErrorCodeName(),Toast.LENGTH_LONG).show();}
            @Override public void onPlaybackStateChanged(int state){if(state==Player.STATE_ENDED){save(true);showControls();}update();}});
        player.setMediaItem(MediaItem.fromUri(url));player.prepare();if(startPosition>0)player.seekTo(startPosition);player.play();
    }
    private void seekBy(long ms){if(player!=null)seekTo(player.getCurrentPosition()+ms);}
    private void seekTo(long ms){if(player==null)return;long d=player.getDuration();player.seekTo(Math.max(0,d>0?Math.min(d,ms):ms));update();}
    private void save(boolean ended){if(player==null||animeId==null)return;long d=Math.max(0,player.getDuration());long pos=ended&&d>0?d:Math.max(0,player.getCurrentPosition());startPosition=pos;
        WatchHistory.save(this,animeId,title==null?"Anime":title,poster==null?"":poster,episode==null?"Episódio":episode,url,pos,d);}
    private void playNext(){if(nextUrl==null||!nextUrl.startsWith("https://"))return;save(false);url=nextUrl;episode=nextLabel;nextUrl="";next.setVisibility(View.GONE);startPosition=0;
        heading.setText(episode+"\n"+title);player.setMediaItem(MediaItem.fromUri(url));player.prepare();player.play();showControls();}
    private void showQuality(){if(player==null)return;ArrayList<String> labels=new ArrayList<>();ArrayList<TrackSelectionOverride> choices=new ArrayList<>();labels.add("Automática");choices.add(null);
        Set<String> seen=new HashSet<>();for(Tracks.Group group:player.getCurrentTracks().getGroups()){
            if(group.getType()!=C.TRACK_TYPE_VIDEO)continue;
            for(int i=0;i<group.length;i++){if(!group.isTrackSupported(i))continue;Format f=group.getTrackFormat(i);String name=f.height>0?f.height+"p":f.bitrate>0?f.bitrate/1000+" kbps":"Faixa "+(i+1);
                if(seen.add(name)){labels.add(name);choices.add(new TrackSelectionOverride(group.getMediaTrackGroup(),i));}}
        }
        if(labels.size()==1){new AlertDialog.Builder(this).setMessage("Este vídeo oferece apenas a qualidade original.").setPositiveButton("OK",null).show();return;}
        new AlertDialog.Builder(this).setTitle("Resolução").setItems(labels.toArray(new String[0]),(dialog,index)->{
            TrackSelectionParameters.Builder p=player.getTrackSelectionParameters().buildUpon();p.clearOverridesOfType(C.TRACK_TYPE_VIDEO);if(index>0)p.setOverrideForType(choices.get(index));player.setTrackSelectionParameters(p.build());quality.setText("⚙\nQualidade: "+labels.get(index));showControls();
        }).show();
    }
    @Override public boolean onKeyDown(int key,KeyEvent event){if(key==KeyEvent.KEYCODE_MEDIA_REWIND){seekBy(-10000);showControls();return true;}if(key==KeyEvent.KEYCODE_MEDIA_FAST_FORWARD){seekBy(10000);showControls();return true;}
        if(key==KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE && player!=null){if(player.isPlaying())player.pause();else player.play();showControls();return true;}
        if((key==KeyEvent.KEYCODE_DPAD_CENTER||key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_DOWN||key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT)&&center.getVisibility()!=View.VISIBLE){showControls();playPause.requestFocus();return true;}
        if(key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_DOWN||key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT)showControls();
        return super.onKeyDown(key,event);}
    @Override protected void onStop(){save(false);if(player!=null){player.release();player=null;video.setPlayer(null);}super.onStop();}
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);super.onDestroy();}
}

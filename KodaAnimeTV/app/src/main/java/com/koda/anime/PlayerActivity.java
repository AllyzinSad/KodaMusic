package com.koda.anime;

import android.app.*;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.media3.common.*;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import java.util.*;

public final class PlayerActivity extends Activity {
    private static final int WHITE=0xFFFFFFFF, MUTED=0xFFB8B8C0, RED=0xFFFF1635, DARK=0xD908080A;
    private final Handler handler=new Handler(Looper.getMainLooper());

    private ExoPlayer player;
    private PlayerView video;
    private FrameLayout overlay;
    private TextView playPause,quality,next,skip,elapsed,duration,titleView,episodeView;
    private SeekBar progress;
    private View tap;
    private boolean tracking;
    private long startPosition,openingEndMs=-1;
    private String url,title,animeId,poster,episode,nextUrl,nextLabel;
    private ScreenFit screenFit;

    private int dp(float n){return screenFit.px(n);}
    private GradientDrawable shape(int fill,int stroke,float radius,float sw){
        GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(radius));
        if(sw>0)g.setStroke(dp(sw),stroke);return g;
    }
    private TextView text(String value,int size,int color,boolean bold){
        TextView v=new TextView(this);v.setText(value);v.setTextColor(color);v.setTextSize(size*screenFit.textScale(this));
        if(bold)v.setTypeface(null,Typeface.BOLD);return v;
    }
    private TextView pill(String value,int size,Runnable action){
        TextView v=text(value,size,WHITE,true);v.setGravity(Gravity.CENTER);v.setPadding(dp(16),dp(8),dp(16),dp(8));v.setFocusable(true);
        v.setBackground(shape(0xB31A1A20,0xFF4A4A54,12,1));
        v.setOnFocusChangeListener((x,f)->{v.setBackground(f?shape(0xE62A0710,RED,12,2):shape(0xB31A1A20,0xFF4A4A54,12,1));v.setScaleX(f?1.035f:1f);v.setScaleY(f?1.035f:1f);if(f)showControls();});
        v.setOnClickListener(x->{action.run();showControls();});return v;
    }
    private TextView round(String value,int size,Runnable action){
        TextView v=text(value,size,WHITE,true);v.setGravity(Gravity.CENTER);v.setFocusable(true);
        v.setBackground(shape(0xA914141A,0xFF555560,100,1));
        v.setOnFocusChangeListener((x,f)->{v.setBackground(f?shape(0xEEA8001E,RED,100,3):shape(0xA914141A,0xFF555560,100,1));v.setScaleX(f?1.08f:1f);v.setScaleY(f?1.08f:1f);if(f)showControls();});
        v.setOnClickListener(x->{action.run();showControls();});return v;
    }

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        Intent i=getIntent();
        url=i.getStringExtra("url");title=i.getStringExtra("title");animeId=i.getStringExtra("animeId");
        poster=i.getStringExtra("poster");episode=i.getStringExtra("episode");nextUrl=i.getStringExtra("nextUrl");
        nextLabel=i.getStringExtra("nextLabel");startPosition=i.getLongExtra("startPosition",0);
        openingEndMs=i.getLongExtra("openingEndMs",-1);

        if(url==null||!url.startsWith("https://")){
            Toast.makeText(this,"URL HTTPS inválida",Toast.LENGTH_LONG).show();finish();return;
        }

        ScreenFit.immersive(this);
        screenFit=new ScreenFit(this);

        FrameLayout root=new FrameLayout(this);root.setBackgroundColor(Color.BLACK);setContentView(root);
        video=new PlayerView(this);video.setUseController(false);root.addView(video,new FrameLayout.LayoutParams(-1,-1));

        tap=new View(this);tap.setFocusable(true);tap.setOnClickListener(v->showControls());
        root.addView(tap,new FrameLayout.LayoutParams(-1,-1));

        overlay=new FrameLayout(this);
        root.addView(overlay,screenFit.centered());

        View topShade=new View(this);
        GradientDrawable topGrad=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0xE608080A,0xA008080A,0x0008080A});
        topShade.setBackground(topGrad);
        overlay.addView(topShade,new FrameLayout.LayoutParams(-1,dp(175),Gravity.TOP));

        View bottomShade=new View(this);
        GradientDrawable bottomGrad=new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,new int[]{0xF008080A,0xB008080A,0x0008080A});
        bottomShade.setBackground(bottomGrad);
        overlay.addView(bottomShade,new FrameLayout.LayoutParams(-1,dp(235),Gravity.BOTTOM));

        TextView back=round("‹",27,this::finish);
        FrameLayout.LayoutParams backLp=new FrameLayout.LayoutParams(dp(44),dp(44),Gravity.TOP|Gravity.LEFT);
        backLp.leftMargin=dp(18);backLp.topMargin=dp(14);overlay.addView(back,backLp);

        ImageView logo=new ImageView(this);logo.setImageResource(R.mipmap.ic_launcher);logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        FrameLayout.LayoutParams logoLp=new FrameLayout.LayoutParams(dp(118),dp(58),Gravity.TOP|Gravity.LEFT);
        logoLp.leftMargin=dp(72);logoLp.topMargin=dp(8);overlay.addView(logo,logoLp);

        LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);
        episodeView=text(title==null?"KODA ANIME":title.toUpperCase(Locale.ROOT),11,RED,true);episodeView.setLetterSpacing(.16f);info.addView(episodeView);
        titleView=text(episode==null?"Episódio":episode,27,WHITE,true);titleView.setTypeface(Typeface.SERIF,Typeface.BOLD);info.addView(titleView);
        FrameLayout.LayoutParams infoLp=new FrameLayout.LayoutParams(dp(520),-2,Gravity.TOP|Gravity.LEFT);
        infoLp.leftMargin=dp(24);infoLp.topMargin=dp(82);overlay.addView(info,infoLp);

        LinearLayout topActions=new LinearLayout(this);topActions.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        TextView audio=pill("Áudio\nOriginal",11,()->Toast.makeText(this,"Áudio conforme a fonte do episódio.",Toast.LENGTH_SHORT).show());
        TextView subtitles=pill("Legendas\nPortuguês (BR)",11,()->Toast.makeText(this,"Legendas conforme a fonte do episódio.",Toast.LENGTH_SHORT).show());
        quality=pill("Qualidade\nAutomática",11,this::showQuality);
        LinearLayout.LayoutParams small=new LinearLayout.LayoutParams(dp(118),dp(52));small.leftMargin=dp(8);topActions.addView(audio,small);
        LinearLayout.LayoutParams small2=new LinearLayout.LayoutParams(dp(132),dp(52));small2.leftMargin=dp(8);topActions.addView(subtitles,small2);
        LinearLayout.LayoutParams small3=new LinearLayout.LayoutParams(dp(126),dp(52));small3.leftMargin=dp(8);topActions.addView(quality,small3);
        FrameLayout.LayoutParams topLp=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.RIGHT);topLp.topMargin=dp(16);topLp.rightMargin=dp(18);overlay.addView(topActions,topLp);

        skip=pill("≫  Pular abertura",13,()->{if(player!=null&&openingEndMs>0&&openingEndMs>player.getCurrentPosition())seekTo(openingEndMs);});
        FrameLayout.LayoutParams skipLp=new FrameLayout.LayoutParams(dp(168),dp(44),Gravity.RIGHT|Gravity.BOTTOM);
        skipLp.rightMargin=dp(24);skipLp.bottomMargin=dp(116);overlay.addView(skip,skipLp);
        skip.setVisibility(openingEndMs>0?View.VISIBLE:View.GONE);

        LinearLayout timeline=new LinearLayout(this);timeline.setGravity(Gravity.CENTER_VERTICAL);
        elapsed=text("00:00",12,WHITE,false);duration=text("00:00",12,WHITE,false);
        timeline.addView(elapsed,new LinearLayout.LayoutParams(dp(54),-2));
        progress=new SeekBar(this);progress.setMax(1000);progress.setProgressTintList(ColorStateList.valueOf(RED));progress.setProgressBackgroundTintList(ColorStateList.valueOf(0xFF77777D));progress.setThumbTintList(ColorStateList.valueOf(WHITE));
        timeline.addView(progress,new LinearLayout.LayoutParams(0,dp(38),1));
        TextView slash=text("  ",12,MUTED,false);timeline.addView(slash,new LinearLayout.LayoutParams(dp(10),-2));
        timeline.addView(duration,new LinearLayout.LayoutParams(dp(54),-2));
        FrameLayout.LayoutParams timeLp=new FrameLayout.LayoutParams(-1,dp(44),Gravity.BOTTOM);
        timeLp.leftMargin=dp(24);timeLp.rightMargin=dp(24);timeLp.bottomMargin=dp(70);overlay.addView(timeline,timeLp);

        progress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override public void onStartTrackingTouch(SeekBar s){tracking=true;handler.removeCallbacks(hide);}
            @Override public void onStopTrackingTouch(SeekBar s){tracking=false;if(player!=null)seekTo(s.getProgress()*Math.max(0,player.getDuration())/1000);showControls();}
            @Override public void onProgressChanged(SeekBar s,int p,boolean user){
                if(user&&player!=null)elapsed.setText(format(p*Math.max(0,player.getDuration())/1000));
            }
        });

        LinearLayout actions=new LinearLayout(this);actions.setGravity(Gravity.CENTER_VERTICAL);
        TextView episodes=pill("☰  Mais episódios",12,this::finish);
        TextView myList=pill("♡  Minha Lista",12,this::toggleList);
        LinearLayout.LayoutParams side=new LinearLayout.LayoutParams(dp(150),dp(46));side.rightMargin=dp(10);actions.addView(episodes,side);
        LinearLayout.LayoutParams side2=new LinearLayout.LayoutParams(dp(138),dp(46));side2.rightMargin=dp(26);actions.addView(myList,side2);

        TextView rewind=round("↶\n10",17,()->seekBy(-10000));actions.addView(rewind,new LinearLayout.LayoutParams(dp(54),dp(54)));
        playPause=round("❚❚",27,()->{if(player==null)return;if(player.isPlaying())player.pause();else player.play();update();});
        LinearLayout.LayoutParams playLp=new LinearLayout.LayoutParams(dp(66),dp(66));playLp.leftMargin=dp(10);playLp.rightMargin=dp(10);actions.addView(playPause,playLp);
        TextView forward=round("↷\n10",17,()->seekBy(10000));actions.addView(forward,new LinearLayout.LayoutParams(dp(54),dp(54)));

        next=pill("▶|  Próximo episódio",12,this::playNext);
        LinearLayout.LayoutParams nextLp=new LinearLayout.LayoutParams(dp(174),dp(46));nextLp.leftMargin=dp(26);actions.addView(next,nextLp);
        next.setVisibility(nextUrl!=null&&nextUrl.startsWith("https://")?View.VISIBLE:View.GONE);

        FrameLayout.LayoutParams actionsLp=new FrameLayout.LayoutParams(-2,dp(66),Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);
        actionsLp.bottomMargin=dp(10);overlay.addView(actions,actionsLp);

        showControls();handler.post(updateLoop);
    }

    private String format(long ms){long s=Math.max(0,ms)/1000;return String.format(Locale.ROOT,"%02d:%02d",s/60,s%60);}

    private final Runnable hide=()->{
        if(tracking)return;
        overlay.setVisibility(View.INVISIBLE);
        tap.requestFocus();
    };

    private final Runnable updateLoop=new Runnable(){
        @Override public void run(){update();handler.postDelayed(this,600);}
    };

    private void showControls(){
        overlay.setVisibility(View.VISIBLE);
        handler.removeCallbacks(hide);
        handler.postDelayed(hide,5000);
    }

    private void update(){
        if(player==null)return;
        long pos=player.getCurrentPosition(),dur=player.getDuration();
        playPause.setText(player.isPlaying()?"❚❚":"▶");
        if(skip!=null)skip.setVisibility(openingEndMs>0&&pos<openingEndMs?View.VISIBLE:View.GONE);
        if(!tracking){
            elapsed.setText(format(pos));duration.setText(format(dur));
            progress.setProgress(dur>0?(int)Math.min(1000,pos*1000/dur):0);
        }
    }

    @Override protected void onStart(){
        super.onStart();if(url==null||player!=null)return;
        player=new ExoPlayer.Builder(this).build();video.setPlayer(player);
        player.addListener(new Player.Listener(){
            @Override public void onPlayerError(PlaybackException e){
                Toast.makeText(PlayerActivity.this,"Não foi possível reproduzir: "+e.getErrorCodeName(),Toast.LENGTH_LONG).show();
            }
            @Override public void onPlaybackStateChanged(int state){
                if(state==Player.STATE_ENDED){save(true);showControls();}update();
            }
        });
        player.setMediaItem(MediaItem.fromUri(url));player.prepare();
        if(startPosition>0)player.seekTo(startPosition);player.play();
    }

    private void seekBy(long ms){if(player!=null)seekTo(player.getCurrentPosition()+ms);}
    private void seekTo(long ms){if(player==null)return;long d=player.getDuration();player.seekTo(Math.max(0,d>0?Math.min(d,ms):ms));update();}

    private void save(boolean ended){
        if(player==null||animeId==null)return;
        long d=Math.max(0,player.getDuration());long pos=ended&&d>0?d:Math.max(0,player.getCurrentPosition());startPosition=pos;
        WatchHistory.save(this,animeId,title==null?"Anime":title,poster==null?"":poster,episode==null?"Episódio":episode,url,pos,d);
    }

    private void toggleList(){
        String id=animeId==null||animeId.isEmpty()?"anime-"+Math.abs((title==null?"Anime":title).hashCode()):animeId;
        SharedPreferences auth=getSharedPreferences("auth",0);String sub=auth.getString("sub","guest");
        SharedPreferences prefs=getSharedPreferences("favorites",0);String key="fav_"+sub;
        Set<String> ids=new HashSet<>(prefs.getStringSet(key,new HashSet<>()));
        boolean added=ids.add(id);if(!added)ids.remove(id);
        prefs.edit().putStringSet(key,ids).putString("item_"+id,(title==null?"Anime":title)+"\n"+(poster==null?"":poster)+"\nAnime\n"+url).apply();
        Toast.makeText(this,added?"Adicionado à Minha Lista":"Removido da Minha Lista",Toast.LENGTH_SHORT).show();
    }

    private void playNext(){
        if(nextUrl==null||!nextUrl.startsWith("https://"))return;
        save(false);url=nextUrl;episode=nextLabel;nextUrl="";next.setVisibility(View.GONE);startPosition=0;
        titleView.setText(episode==null?"Próximo episódio":episode);
        player.setMediaItem(MediaItem.fromUri(url));player.prepare();player.play();showControls();
    }

    private void showQuality(){
        if(player==null)return;
        ArrayList<String> labels=new ArrayList<>();ArrayList<TrackSelectionOverride> choices=new ArrayList<>();
        labels.add("Automática");choices.add(null);Set<String> seen=new HashSet<>();
        for(Tracks.Group group:player.getCurrentTracks().getGroups()){
            if(group.getType()!=C.TRACK_TYPE_VIDEO)continue;
            for(int n=0;n<group.length;n++){
                if(!group.isTrackSupported(n))continue;
                Format f=group.getTrackFormat(n);
                String name=f.height>0?f.height+"p":f.bitrate>0?f.bitrate/1000+" kbps":"Faixa "+(n+1);
                if(seen.add(name)){labels.add(name);choices.add(new TrackSelectionOverride(group.getMediaTrackGroup(),n));}
            }
        }
        if(labels.size()==1){Toast.makeText(this,"Este vídeo oferece apenas a qualidade original.",Toast.LENGTH_SHORT).show();return;}
        new AlertDialog.Builder(this).setTitle("Qualidade").setItems(labels.toArray(new String[0]),(dialog,index)->{
            TrackSelectionParameters.Builder p=player.getTrackSelectionParameters().buildUpon();p.clearOverridesOfType(C.TRACK_TYPE_VIDEO);
            if(index>0)p.setOverrideForType(choices.get(index));player.setTrackSelectionParameters(p.build());
            quality.setText("Qualidade\n"+labels.get(index));showControls();
        }).show();
    }

    @Override public boolean onKeyDown(int key,KeyEvent event){
        if(key==KeyEvent.KEYCODE_MEDIA_REWIND){seekBy(-10000);showControls();return true;}
        if(key==KeyEvent.KEYCODE_MEDIA_FAST_FORWARD){seekBy(10000);showControls();return true;}
        if(key==KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE&&player!=null){if(player.isPlaying())player.pause();else player.play();showControls();return true;}
        if((key==KeyEvent.KEYCODE_DPAD_CENTER||key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_DOWN||key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT)&&overlay.getVisibility()!=View.VISIBLE){
            showControls();playPause.requestFocus();return true;
        }
        if(key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_DOWN||key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT)showControls();
        return super.onKeyDown(key,event);
    }

    @Override protected void onStop(){
        save(false);if(player!=null){player.release();player=null;video.setPlayer(null);}super.onStop();
    }
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);super.onDestroy();}
}
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
    private static final int WHITE=0xFFFFFFFF, MUTED=0xFFB8B8C0, RED=0xFFFF1635;

    private final Handler handler=new Handler(Looper.getMainLooper());
    private ExoPlayer player;
    private PlayerView video;
    private FrameLayout controls;
    private ImageButton playPause,next,skip;
    private TextView quality,elapsed,duration,episodeTitle,seriesTitle;
    private SeekBar progress;
    private View tap;
    private boolean tracking;
    private long startPosition,openingEndMs=-1;
    private String url,title,animeId,poster,episode,nextUrl,nextLabel;
    private ScreenFit screenFit;

    private int dp(float n){return screenFit.px(n);}
    private GradientDrawable bg(int fill,int stroke,int radius,int sw){
        GradientDrawable g=new GradientDrawable();g.setColor(fill);g.setCornerRadius(dp(radius));
        if(sw>0)g.setStroke(dp(sw),stroke);return g;
    }
    private TextView text(String value,int size,int color,boolean bold){
        TextView v=new TextView(this);v.setText(value);v.setTextColor(color);v.setTextSize(size*screenFit.textScale(this));
        if(bold)v.setTypeface(null,Typeface.BOLD);return v;
    }
    private TextView pill(String value,Runnable action){
        TextView v=text(value,11,WHITE,true);v.setGravity(Gravity.CENTER);v.setPadding(dp(13),dp(6),dp(13),dp(6));v.setFocusable(true);v.setClickable(true);
        v.setBackground(bg(0xAA111116,0xFF494951,10,1));
        v.setOnFocusChangeListener((x,f)->{
            v.setBackground(f?bg(0xD72B0711,RED,10,2):bg(0xAA111116,0xFF494951,10,1));
            v.animate().scaleX(f?1.04f:1f).scaleY(f?1.04f:1f).setDuration(100).start();
            if(f)showControls();
        });
        v.setOnClickListener(x->{action.run();showControls();});return v;
    }
    private ImageButton asset(int drawable,String desc,boolean mirrored,Runnable action){
        ImageButton v=new ImageButton(this);v.setImageResource(drawable);v.setScaleType(ImageView.ScaleType.FIT_CENTER);v.setAdjustViewBounds(true);
        v.setBackgroundColor(Color.TRANSPARENT);v.setPadding(0,0,0,0);v.setFocusable(true);v.setClickable(true);v.setContentDescription(desc);v.setAlpha(.80f);
        float normalX=mirrored?-1f:1f,focusX=mirrored?-1.08f:1.08f;v.setScaleX(normalX);
        v.setOnFocusChangeListener((x,f)->{
            v.setAlpha(f?1f:.80f);
            v.animate().scaleX(f?focusX:normalX).scaleY(f?1.08f:1f).setDuration(110).start();
            if(f)showControls();
        });
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

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(
                5894|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);

        screenFit=new ScreenFit(this);

        FrameLayout root=new FrameLayout(this);root.setBackgroundColor(Color.BLACK);setContentView(root);
        video=new PlayerView(this);video.setUseController(false);root.addView(video,new FrameLayout.LayoutParams(-1,-1));

        tap=new View(this);tap.setFocusable(true);tap.setOnClickListener(v->showControls());
        root.addView(tap,new FrameLayout.LayoutParams(-1,-1));

        controls=new FrameLayout(this);controls.setAlpha(1f);
        root.addView(controls,screenFit.centered());

        View topShade=new View(this);
        topShade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0xE808080A,0xA008080A,0x0008080A}));
        controls.addView(topShade,new FrameLayout.LayoutParams(-1,dp(175),Gravity.TOP));

        View bottomShade=new View(this);
        bottomShade.setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,new int[]{0xF508080A,0xB808080A,0x4808080A,0x0008080A}));
        controls.addView(bottomShade,new FrameLayout.LayoutParams(-1,dp(235),Gravity.BOTTOM));

        ImageView leaves=new ImageView(this);leaves.setImageResource(R.drawable.ui_leaves);leaves.setScaleType(ImageView.ScaleType.FIT_CENTER);leaves.setAlpha(.34f);
        FrameLayout.LayoutParams leavesLp=new FrameLayout.LayoutParams(dp(128),dp(220),Gravity.RIGHT|Gravity.BOTTOM);
        leavesLp.rightMargin=dp(2);leavesLp.bottomMargin=dp(0);controls.addView(leaves,leavesLp);

        ImageView logo=new ImageView(this);logo.setImageResource(R.mipmap.ic_launcher);logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        FrameLayout.LayoutParams logoLp=new FrameLayout.LayoutParams(dp(125),dp(62),Gravity.TOP|Gravity.LEFT);
        logoLp.leftMargin=dp(24);logoLp.topMargin=dp(12);controls.addView(logo,logoLp);

        LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);
        seriesTitle=text(title==null?"KODA ANIME":title.toUpperCase(Locale.ROOT),11,RED,true);seriesTitle.setLetterSpacing(.16f);info.addView(seriesTitle);
        episodeTitle=text(episode==null?"Episódio":episode,29,WHITE,true);episodeTitle.setTypeface(Typeface.SERIF,Typeface.BOLD);episodeTitle.setMaxLines(1);episodeTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);info.addView(episodeTitle);
        FrameLayout.LayoutParams infoLp=new FrameLayout.LayoutParams(dp(600),-2,Gravity.TOP|Gravity.LEFT);
        infoLp.leftMargin=dp(28);infoLp.topMargin=dp(82);controls.addView(info,infoLp);

        LinearLayout topActions=new LinearLayout(this);topActions.setGravity(Gravity.CENTER_VERTICAL);
        TextView audio=pill("Áudio\nOriginal",()->Toast.makeText(this,"Áudio conforme a fonte do episódio.",Toast.LENGTH_SHORT).show());
        TextView subs=pill("Legendas\nPortuguês (BR)",()->Toast.makeText(this,"Legendas conforme a fonte do episódio.",Toast.LENGTH_SHORT).show());
        quality=pill("Qualidade\nAutomática",this::showQuality);
        LinearLayout.LayoutParams ta=new LinearLayout.LayoutParams(dp(120),dp(50));ta.leftMargin=dp(8);topActions.addView(audio,ta);
        LinearLayout.LayoutParams ts=new LinearLayout.LayoutParams(dp(140),dp(50));ts.leftMargin=dp(8);topActions.addView(subs,ts);
        LinearLayout.LayoutParams tq=new LinearLayout.LayoutParams(dp(130),dp(50));tq.leftMargin=dp(8);topActions.addView(quality,tq);
        FrameLayout.LayoutParams topLp=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.RIGHT);
        topLp.rightMargin=dp(24);topLp.topMargin=dp(18);controls.addView(topActions,topLp);

        skip=asset(R.drawable.ui_btn_skip,"Pular abertura",false,()->{
            if(player!=null&&openingEndMs>0&&openingEndMs>player.getCurrentPosition())seekTo(openingEndMs);
        });
        FrameLayout.LayoutParams skipLp=new FrameLayout.LayoutParams(dp(180),dp(45),Gravity.RIGHT|Gravity.BOTTOM);
        skipLp.rightMargin=dp(28);skipLp.bottomMargin=dp(125);controls.addView(skip,skipLp);
        skip.setVisibility(openingEndMs>0?View.VISIBLE:View.GONE);

        LinearLayout timeline=new LinearLayout(this);timeline.setGravity(Gravity.CENTER_VERTICAL);
        elapsed=text("00:00",12,WHITE,false);duration=text("00:00",12,WHITE,false);
        timeline.addView(elapsed,new LinearLayout.LayoutParams(dp(62),-2));
        progress=new SeekBar(this);progress.setMax(1000);progress.setPadding(0,0,0,0);
        progress.setProgressTintList(ColorStateList.valueOf(RED));
        progress.setProgressBackgroundTintList(ColorStateList.valueOf(0xFF54545D));
        progress.setThumbTintList(ColorStateList.valueOf(WHITE));
        timeline.addView(progress,new LinearLayout.LayoutParams(0,dp(34),1));
        LinearLayout.LayoutParams durLp=new LinearLayout.LayoutParams(dp(62),-2);durLp.leftMargin=dp(10);timeline.addView(duration,durLp);
        FrameLayout.LayoutParams timelineLp=new FrameLayout.LayoutParams(-1,dp(38),Gravity.BOTTOM);
        timelineLp.leftMargin=dp(28);timelineLp.rightMargin=dp(28);timelineLp.bottomMargin=dp(79);controls.addView(timeline,timelineLp);

        progress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override public void onStartTrackingTouch(SeekBar s){tracking=true;handler.removeCallbacks(hide);}
            @Override public void onStopTrackingTouch(SeekBar s){
                tracking=false;if(player!=null)seekTo(s.getProgress()*Math.max(0,player.getDuration())/1000);showControls();
            }
            @Override public void onProgressChanged(SeekBar s,int p,boolean user){
                if(user&&player!=null)elapsed.setText(format(p*Math.max(0,player.getDuration())/1000));
            }
        });

        LinearLayout leftActions=new LinearLayout(this);leftActions.setGravity(Gravity.CENTER_VERTICAL);
        ImageButton episodes=asset(R.drawable.ui_btn_episodes,"Mais episódios",false,this::finish);
        ImageButton myList=asset(R.drawable.ui_btn_mylist,"Minha Lista",false,this::toggleList);
        LinearLayout.LayoutParams epLp=new LinearLayout.LayoutParams(dp(155),dp(46));epLp.rightMargin=dp(10);leftActions.addView(episodes,epLp);
        leftActions.addView(myList,new LinearLayout.LayoutParams(dp(143),dp(46)));
        FrameLayout.LayoutParams leftLp=new FrameLayout.LayoutParams(-2,dp(58),Gravity.BOTTOM|Gravity.LEFT);
        leftLp.leftMargin=dp(28);leftLp.bottomMargin=dp(13);controls.addView(leftActions,leftLp);

        LinearLayout centerActions=new LinearLayout(this);centerActions.setGravity(Gravity.CENTER);
        ImageButton rewind=asset(R.drawable.ui_player_rewind,"Voltar 10 segundos",false,()->seekBy(-10000));
        playPause=asset(R.drawable.ui_player_pause,"Pausar",false,()->{
            if(player==null)return;if(player.isPlaying())player.pause();else player.play();update();
        });
        ImageButton forward=asset(R.drawable.ui_player_rewind,"Avançar 10 segundos",true,()->seekBy(10000));
        centerActions.addView(rewind,new LinearLayout.LayoutParams(dp(58),dp(58)));
        LinearLayout.LayoutParams playLp=new LinearLayout.LayoutParams(dp(76),dp(72));playLp.leftMargin=dp(12);playLp.rightMargin=dp(12);centerActions.addView(playPause,playLp);
        centerActions.addView(forward,new LinearLayout.LayoutParams(dp(58),dp(58)));
        FrameLayout.LayoutParams centerLp=new FrameLayout.LayoutParams(-2,dp(72),Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);
        centerLp.bottomMargin=dp(7);controls.addView(centerActions,centerLp);

        next=asset(R.drawable.ui_player_next,"Próximo episódio",false,this::playNext);
        FrameLayout.LayoutParams nextLp=new FrameLayout.LayoutParams(dp(62),dp(60),Gravity.BOTTOM|Gravity.RIGHT);
        nextLp.rightMargin=dp(35);nextLp.bottomMargin=dp(12);controls.addView(next,nextLp);
        next.setVisibility(nextUrl!=null&&nextUrl.startsWith("https://")?View.VISIBLE:View.GONE);

        showControls();handler.post(updateLoop);
    }

    private String format(long ms){
        long s=Math.max(0,ms)/1000;return String.format(Locale.ROOT,"%02d:%02d",s/60,s%60);
    }

    private final Runnable hide=()->{
        if(tracking||controls==null)return;
        controls.animate().alpha(0f).setDuration(180).withEndAction(()->{
            controls.setVisibility(View.INVISIBLE);tap.requestFocus();
        }).start();
    };

    private final Runnable updateLoop=new Runnable(){
        @Override public void run(){update();handler.postDelayed(this,600);}
    };

    private void showControls(){
        if(controls==null)return;
        controls.animate().cancel();controls.setVisibility(View.VISIBLE);
        if(controls.getAlpha()<1f)controls.setAlpha(0f);
        controls.animate().alpha(1f).setDuration(140).start();
        handler.removeCallbacks(hide);handler.postDelayed(hide,5000);
    }

    private void update(){
        if(player==null)return;
        long pos=player.getCurrentPosition(),dur=player.getDuration();
        boolean playing=player.isPlaying();
        playPause.setImageResource(playing?R.drawable.ui_player_pause:R.drawable.ui_player_play);
        playPause.setContentDescription(playing?"Pausar":"Reproduzir");
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
    private void seekTo(long ms){
        if(player==null)return;long d=player.getDuration();
        player.seekTo(Math.max(0,d>0?Math.min(d,ms):ms));update();
    }

    private void save(boolean ended){
        if(player==null||animeId==null)return;
        long d=Math.max(0,player.getDuration());
        long pos=ended&&d>0?d:Math.max(0,player.getCurrentPosition());
        startPosition=pos;
        WatchHistory.save(this,animeId,title==null?"Anime":title,poster==null?"":poster,
                episode==null?"Episódio":episode,url,pos,d);
    }

    private void toggleList(){
        String id=animeId==null||animeId.isEmpty()?"anime-"+Math.abs((title==null?"Anime":title).hashCode()):animeId;
        String sub=getSharedPreferences("auth",0).getString("sub","guest");
        SharedPreferences prefs=getSharedPreferences("favorites",0);String key="fav_"+sub;
        Set<String> ids=new HashSet<>(prefs.getStringSet(key,new HashSet<>()));
        boolean added=ids.add(id);if(!added)ids.remove(id);
        prefs.edit().putStringSet(key,ids)
                .putString("item_"+id,(title==null?"Anime":title)+"\n"+(poster==null?"":poster)+"\nAnime\n"+url).apply();
        Toast.makeText(this,added?"Adicionado à Minha Lista":"Removido da Minha Lista",Toast.LENGTH_SHORT).show();
    }

    private void playNext(){
        if(nextUrl==null||!nextUrl.startsWith("https://"))return;
        save(false);url=nextUrl;episode=nextLabel;nextUrl="";startPosition=0;
        next.setVisibility(View.GONE);openingEndMs=-1;skip.setVisibility(View.GONE);
        episodeTitle.setText(episode==null?"Próximo episódio":episode);
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
        if(labels.size()==1){
            Toast.makeText(this,"Este vídeo oferece apenas a qualidade original.",Toast.LENGTH_SHORT).show();return;
        }
        new AlertDialog.Builder(this).setTitle("Qualidade").setItems(labels.toArray(new String[0]),(dialog,index)->{
            TrackSelectionParameters.Builder p=player.getTrackSelectionParameters().buildUpon();
            p.clearOverridesOfType(C.TRACK_TYPE_VIDEO);
            if(index>0)p.setOverrideForType(choices.get(index));
            player.setTrackSelectionParameters(p.build());
            quality.setText("Qualidade\n"+labels.get(index));showControls();
        }).show();
    }

    @Override public boolean onKeyDown(int key,KeyEvent event){
        if(key==KeyEvent.KEYCODE_MEDIA_REWIND){seekBy(-10000);showControls();return true;}
        if(key==KeyEvent.KEYCODE_MEDIA_FAST_FORWARD){seekBy(10000);showControls();return true;}
        if(key==KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE&&player!=null){
            if(player.isPlaying())player.pause();else player.play();showControls();return true;
        }
        if((key==KeyEvent.KEYCODE_DPAD_CENTER||key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_DOWN||
                key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT)&&controls.getVisibility()!=View.VISIBLE){
            showControls();playPause.requestFocus();return true;
        }
        if(key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_DOWN||
                key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT)showControls();
        return super.onKeyDown(key,event);
    }

    @Override protected void onStop(){
        save(false);
        if(player!=null){player.release();player=null;video.setPlayer(null);}
        super.onStop();
    }

    @Override protected void onDestroy(){
        handler.removeCallbacksAndMessages(null);super.onDestroy();
    }
}

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
    private static final int WHITE=0xFFFFFFFF, MUTED=0xFFB8B8C0, RED=0xFFFF1635;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private ExoPlayer player; private PlayerView video;
    private FrameLayout safeControls; private LinearLayout center,bottom;
    private TextView heading,quality,next,time,skip;
    private ImageButton playPause,rewind,forward;
    private ImageView topArt,bottomArt,leaves;
    private SeekBar progress; private View tap; private boolean tracking;
    private long startPosition,openingEndMs=-1;
    private String url,title,animeId,poster,episode,nextUrl,nextLabel;
    private ScreenFit screenFit;

    private int dp(float n){return screenFit.px(n);}
    private GradientDrawable bg(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
    private GradientDrawable outline(int fill,int stroke,int radius,int width){GradientDrawable g=bg(fill,radius);g.setStroke(dp(width),stroke);return g;}

    private TextView textControl(String text,int size,Runnable action){
        TextView v=new TextView(this);v.setText(text);v.setTextColor(WHITE);v.setTextSize(size*screenFit.textScale(this));v.setGravity(Gravity.CENTER);
        v.setPadding(dp(14),dp(6),dp(14),dp(6));v.setBackground(bg(0xB814141A,12));v.setFocusable(true);v.setClickable(true);
        v.setOnFocusChangeListener((view,focus)->{v.setBackground(focus?outline(0xE0280710,RED,12,2):bg(0xB814141A,12));v.setScaleX(focus?1.035f:1f);v.setScaleY(focus?1.035f:1f);if(focus)showControls();});
        v.setOnClickListener(view->{action.run();showControls();});return v;
    }

    private ImageButton imageControl(int drawable,Runnable action){
        ImageButton b=new ImageButton(this);b.setImageResource(drawable);b.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        b.setPadding(dp(7),dp(7),dp(7),dp(7));b.setBackground(bg(0x6608080A,44));b.setFocusable(true);b.setClickable(true);
        b.setOnFocusChangeListener((view,focus)->{b.setBackground(focus?outline(0xAA220610,RED,44,2):bg(0x6608080A,44));b.setScaleX(focus?1.08f:1f);b.setScaleY(focus?1.08f:1f);b.setElevation(focus?dp(10):0);if(focus)showControls();});
        b.setOnClickListener(view->{action.run();showControls();});return b;
    }

    private void addImage(LinearLayout row,View v,int size){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(size),dp(size));p.leftMargin=dp(8);p.rightMargin=dp(8);row.addView(v,p);}

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        Intent i=getIntent();
        url=i.getStringExtra("url");title=i.getStringExtra("title");animeId=i.getStringExtra("animeId");poster=i.getStringExtra("poster");episode=i.getStringExtra("episode");
        nextUrl=i.getStringExtra("nextUrl");nextLabel=i.getStringExtra("nextLabel");startPosition=i.getLongExtra("startPosition",0);openingEndMs=i.getLongExtra("openingEndMs",-1);
        if(url==null||!url.startsWith("https://")){Toast.makeText(this,"URL HTTPS inválida",Toast.LENGTH_LONG).show();finish();return;}

        ScreenFit.immersive(this);screenFit=new ScreenFit(this);
        FrameLayout root=new FrameLayout(this);root.setBackgroundColor(Color.BLACK);setContentView(root);

        video=new PlayerView(this);video.setUseController(false);video.setKeepScreenOn(true);root.addView(video,new FrameLayout.LayoutParams(-1,-1));
        tap=new View(this);tap.setFocusable(true);tap.setClickable(true);tap.setOnClickListener(v->showControls());root.addView(tap,new FrameLayout.LayoutParams(-1,-1));

        safeControls=new FrameLayout(this);root.addView(safeControls,screenFit.centered());

        topArt=new ImageView(this);topArt.setImageResource(R.drawable.koda_overlay_top);topArt.setScaleType(ImageView.ScaleType.FIT_XY);topArt.setAlpha(.84f);
        FrameLayout.LayoutParams ta=new FrameLayout.LayoutParams(dp(420),dp(86),Gravity.TOP|Gravity.RIGHT);ta.rightMargin=dp(4);safeControls.addView(topArt,ta);

        bottomArt=new ImageView(this);bottomArt.setImageResource(R.drawable.koda_overlay_bottom);bottomArt.setScaleType(ImageView.ScaleType.FIT_XY);bottomArt.setAlpha(.78f);
        FrameLayout.LayoutParams ba=new FrameLayout.LayoutParams(dp(430),dp(88),Gravity.BOTTOM|Gravity.LEFT);ba.leftMargin=dp(4);safeControls.addView(bottomArt,ba);

        leaves=new ImageView(this);leaves.setImageResource(R.drawable.koda_leaves);leaves.setScaleType(ImageView.ScaleType.FIT_CENTER);leaves.setAlpha(.58f);
        FrameLayout.LayoutParams la=new FrameLayout.LayoutParams(dp(105),dp(190),Gravity.TOP|Gravity.RIGHT);la.rightMargin=dp(6);la.topMargin=dp(32);safeControls.addView(leaves,la);

        ImageView logo=new ImageView(this);logo.setImageResource(R.mipmap.ic_launcher);logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        FrameLayout.LayoutParams logoLp=new FrameLayout.LayoutParams(dp(104),dp(66),Gravity.TOP|Gravity.LEFT);logoLp.leftMargin=dp(24);logoLp.topMargin=dp(14);safeControls.addView(logo,logoLp);

        heading=new TextView(this);heading.setText((episode==null?"":episode)+"\n"+(title==null?"Koda Anime":title));heading.setTextColor(WHITE);
        heading.setTextSize(16*screenFit.textScale(this));heading.setPadding(dp(20),dp(12),dp(20),dp(12));heading.setBackground(bg(0xA808080A,10));
        FrameLayout.LayoutParams headLp=new FrameLayout.LayoutParams(dp(500),-2,Gravity.TOP|Gravity.LEFT);headLp.leftMargin=dp(24);headLp.topMargin=dp(82);safeControls.addView(heading,headLp);

        center=new LinearLayout(this);center.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(-1,-2,Gravity.CENTER);safeControls.addView(center,cp);
        rewind=imageControl(R.drawable.koda_player_rewind,()->seekBy(-10000));addImage(center,rewind,76);
        playPause=imageControl(R.drawable.koda_player_pause,()->{if(player==null)return;if(player.isPlaying())player.pause();else player.play();update();});addImage(center,playPause,88);
        forward=imageControl(R.drawable.koda_player_rewind,()->seekBy(10000));forward.setScaleX(-1f);addImage(center,forward,76);

        bottom=new LinearLayout(this);bottom.setOrientation(LinearLayout.VERTICAL);bottom.setPadding(dp(24),dp(11),dp(24),dp(14));bottom.setBackground(bg(0xC608080A,12));
        FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(-1,dp(118),Gravity.BOTTOM);bp.leftMargin=dp(18);bp.rightMargin=dp(18);bp.bottomMargin=dp(16);safeControls.addView(bottom,bp);

        LinearLayout seekRow=new LinearLayout(this);seekRow.setGravity(Gravity.CENTER_VERTICAL);bottom.addView(seekRow,new LinearLayout.LayoutParams(-1,dp(36)));
        time=new TextView(this);time.setTextColor(WHITE);time.setTextSize(13*screenFit.textScale(this));time.setText("00:00 / 00:00");
        LinearLayout.LayoutParams timeLp=new LinearLayout.LayoutParams(dp(110),-2);seekRow.addView(time,timeLp);
        progress=new SeekBar(this);progress.setMax(1000);progress.setProgressTintList(android.content.res.ColorStateList.valueOf(RED));progress.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF55555E));progress.setThumbTintList(android.content.res.ColorStateList.valueOf(RED));
        seekRow.addView(progress,new LinearLayout.LayoutParams(0,dp(34),1));
        progress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override public void onStartTrackingTouch(SeekBar s){tracking=true;handler.removeCallbacks(hide);}
            @Override public void onStopTrackingTouch(SeekBar s){tracking=false;if(player!=null)seekTo(s.getProgress()*Math.max(0,player.getDuration())/1000);showControls();}
            @Override public void onProgressChanged(SeekBar s,int p,boolean user){if(user&&player!=null)time.setText(format(p*Math.max(0,player.getDuration())/1000)+" / "+format(player.getDuration()));}
        });

        LinearLayout actions=new LinearLayout(this);actions.setGravity(Gravity.CENTER_VERTICAL);bottom.addView(actions,new LinearLayout.LayoutParams(-1,dp(58)));
        quality=textControl("⚙  Qualidade: Auto",14,this::showQuality);LinearLayout.LayoutParams qlp=new LinearLayout.LayoutParams(0,dp(48),1);qlp.rightMargin=dp(10);actions.addView(quality,qlp);
        next=textControl("Próximo episódio  ⏭",14,this::playNext);LinearLayout.LayoutParams nlp=new LinearLayout.LayoutParams(0,dp(48),1);nlp.rightMargin=dp(10);actions.addView(next,nlp);
        next.setVisibility(nextUrl!=null&&nextUrl.startsWith("https://")?View.VISIBLE:View.GONE);
        skip=textControl("Pular abertura  ⏩",14,()->{if(player!=null&&openingEndMs>0&&openingEndMs>player.getCurrentPosition())seekTo(openingEndMs);});
        actions.addView(skip,new LinearLayout.LayoutParams(0,dp(48),1));skip.setVisibility(View.GONE);

        showControls();handler.post(updateLoop);
    }

    private String format(long ms){long s=Math.max(0,ms)/1000;return String.format(Locale.ROOT,"%02d:%02d",s/60,s%60);}
    private final Runnable hide=()->{if(tracking)return;heading.setVisibility(View.GONE);center.setVisibility(View.GONE);bottom.setVisibility(View.GONE);topArt.setVisibility(View.GONE);bottomArt.setVisibility(View.GONE);leaves.setVisibility(View.GONE);tap.requestFocus();};
    private final Runnable updateLoop=new Runnable(){@Override public void run(){update();handler.postDelayed(this,600);}};

    private void showControls(){
        heading.setVisibility(View.VISIBLE);center.setVisibility(View.VISIBLE);bottom.setVisibility(View.VISIBLE);topArt.setVisibility(View.VISIBLE);bottomArt.setVisibility(View.VISIBLE);leaves.setVisibility(View.VISIBLE);
        handler.removeCallbacks(hide);handler.postDelayed(hide,4500);
    }

    private void update(){
        if(player==null)return;long pos=player.getCurrentPosition(),dur=player.getDuration();
        playPause.setImageResource(player.isPlaying()?R.drawable.koda_player_pause:R.drawable.koda_player_play);
        if(skip!=null)skip.setVisibility(openingEndMs>0&&pos>=0&&pos<openingEndMs?View.VISIBLE:View.GONE);
        if(!tracking){time.setText(format(pos)+" / "+format(dur));progress.setProgress(dur>0?(int)Math.min(1000,pos*1000/dur):0);}
    }

    @Override protected void onStart(){
        super.onStart();if(url==null||player!=null)return;
        player=new ExoPlayer.Builder(this).build();video.setPlayer(player);
        player.addListener(new Player.Listener(){
            @Override public void onPlayerError(PlaybackException e){Toast.makeText(PlayerActivity.this,"Não foi possível reproduzir: "+e.getErrorCodeName(),Toast.LENGTH_LONG).show();}
            @Override public void onPlaybackStateChanged(int state){if(state==Player.STATE_ENDED){save(true);showControls();}update();}
        });
        player.setMediaItem(MediaItem.fromUri(url));player.prepare();if(startPosition>0)player.seekTo(startPosition);player.play();
    }

    private void seekBy(long ms){if(player!=null)seekTo(player.getCurrentPosition()+ms);}
    private void seekTo(long ms){if(player==null)return;long d=player.getDuration();player.seekTo(Math.max(0,d>0?Math.min(d,ms):ms));update();}

    private void save(boolean ended){
        if(player==null||animeId==null)return;long d=Math.max(0,player.getDuration());long pos=ended&&d>0?d:Math.max(0,player.getCurrentPosition());startPosition=pos;
        WatchHistory.save(this,animeId,title==null?"Anime":title,poster==null?"":poster,episode==null?"Episódio":episode,url,pos,d);
    }

    private void playNext(){
        if(nextUrl==null||!nextUrl.startsWith("https://"))return;save(false);url=nextUrl;episode=nextLabel;nextUrl="";next.setVisibility(View.GONE);startPosition=0;openingEndMs=-1;
        heading.setText(episode+"\n"+title);player.setMediaItem(MediaItem.fromUri(url));player.prepare();player.play();showControls();
    }

    private void showQuality(){
        if(player==null)return;ArrayList<String> labels=new ArrayList<>();ArrayList<TrackSelectionOverride> choices=new ArrayList<>();labels.add("Automática");choices.add(null);
        Set<String> seen=new HashSet<>();
        for(Tracks.Group group:player.getCurrentTracks().getGroups()){
            if(group.getType()!=C.TRACK_TYPE_VIDEO)continue;
            for(int i=0;i<group.length;i++){
                if(!group.isTrackSupported(i))continue;Format f=group.getTrackFormat(i);
                String name=f.height>0?f.height+"p":f.bitrate>0?f.bitrate/1000+" kbps":"Faixa "+(i+1);
                if(seen.add(name)){labels.add(name);choices.add(new TrackSelectionOverride(group.getMediaTrackGroup(),i));}
            }
        }
        if(labels.size()==1){new AlertDialog.Builder(this).setMessage("Este vídeo oferece apenas a qualidade original.").setPositiveButton("OK",null).show();return;}
        new AlertDialog.Builder(this).setTitle("Resolução").setItems(labels.toArray(new String[0]),(dialog,index)->{
            TrackSelectionParameters.Builder p=player.getTrackSelectionParameters().buildUpon();p.clearOverridesOfType(C.TRACK_TYPE_VIDEO);
            if(index>0)p.setOverrideForType(choices.get(index));player.setTrackSelectionParameters(p.build());quality.setText("⚙  Qualidade: "+labels.get(index));showControls();
        }).show();
    }

    @Override public boolean onKeyDown(int key,KeyEvent event){
        if(key==KeyEvent.KEYCODE_MEDIA_REWIND){seekBy(-10000);showControls();return true;}
        if(key==KeyEvent.KEYCODE_MEDIA_FAST_FORWARD){seekBy(10000);showControls();return true;}
        if(key==KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE&&player!=null){if(player.isPlaying())player.pause();else player.play();showControls();return true;}
        if((key==KeyEvent.KEYCODE_DPAD_CENTER||key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_DOWN||key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT)&&center.getVisibility()!=View.VISIBLE){showControls();playPause.requestFocus();return true;}
        if(key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_DOWN||key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT)showControls();
        return super.onKeyDown(key,event);
    }

    @Override protected void onStop(){save(false);if(player!=null){player.release();player=null;video.setPlayer(null);}super.onStop();}
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);super.onDestroy();}
}

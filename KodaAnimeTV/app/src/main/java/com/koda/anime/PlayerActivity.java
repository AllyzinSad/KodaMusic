package com.koda.anime;

import android.app.*;
import android.content.*;
import android.content.res.ColorStateList;
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

    private ExoPlayer player;
    private PlayerView video;
    private ScreenFit screenFit;

    private FrameLayout controls;
    private LinearLayout titleArea,centerControls,bottomPanel;
    private TextView episodeView,titleView,elapsedView,durationView,quality,next,skip,forward;
    private ImageButton rewind,playPause;
    private SeekBar progress;
    private View tap;
    private boolean tracking;

    private long startPosition,openingEndMs=-1;
    private String url,title,animeId,poster,episode,nextUrl,nextLabel;

    private int dp(float n){return screenFit.px(n);}

    private GradientDrawable rounded(int color,int radius){
        GradientDrawable d=new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private GradientDrawable outline(int fill,int stroke,int radius,int width){
        GradientDrawable d=rounded(fill,radius);
        d.setStroke(dp(width),stroke);
        return d;
    }

    private GradientDrawable circle(int fill,int stroke,int width){
        GradientDrawable d=new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(fill);
        if(width>0)d.setStroke(dp(width),stroke);
        return d;
    }

    private TextView actionButton(String text,Runnable action){
        TextView v=new TextView(this);
        v.setText(text);
        v.setTextColor(WHITE);
        v.setTextSize(13*screenFit.textScale(this));
        v.setGravity(Gravity.CENTER);
        v.setSingleLine(true);
        v.setPadding(dp(14),0,dp(14),0);
        v.setBackground(rounded(0xB317171C,12));
        v.setFocusable(true);
        v.setClickable(true);
        v.setOnFocusChangeListener((view,focus)->{
            v.setBackground(focus?outline(0xE0260710,RED,12,2):rounded(0xB317171C,12));
            v.setScaleX(focus?1.045f:1f);
            v.setScaleY(focus?1.045f:1f);
            v.setElevation(focus?dp(8):0);
            if(focus)showControls();
        });
        v.setOnClickListener(view->{action.run();showControls();});
        return v;
    }

    private ImageButton imageCircle(int drawable,Runnable action){
        ImageButton b=new ImageButton(this);
        b.setImageResource(drawable);
        b.setScaleType(ImageView.ScaleType.CENTER_CROP);
        b.setBackground(circle(0xC70B0B0E,0,0));
        b.setClipToOutline(true);
        b.setPadding(0,0,0,0);
        b.setFocusable(true);
        b.setClickable(true);
        b.setOnFocusChangeListener((view,focus)->{
            b.setBackground(focus?circle(0xE015080D,RED,2):circle(0xC70B0B0E,0,0));
            b.setScaleX(focus?1.085f:1f);
            b.setScaleY(focus?1.085f:1f);
            b.setElevation(focus?dp(12):0);
            if(focus)showControls();
        });
        b.setOnClickListener(view->{action.run();showControls();});
        return b;
    }

    private TextView forwardButton(Runnable action){
        TextView b=new TextView(this);
        b.setText("↻\n10");
        b.setGravity(Gravity.CENTER);
        b.setTextColor(WHITE);
        b.setTextSize(16*screenFit.textScale(this));
        b.setTypeface(null,android.graphics.Typeface.BOLD);
        b.setBackground(circle(0xE00B0B0E,0,0));
        b.setFocusable(true);
        b.setClickable(true);
        b.setOnFocusChangeListener((view,focus)->{
            b.setBackground(focus?circle(0xEA17080D,RED,2):circle(0xE00B0B0E,0,0));
            b.setScaleX(focus?1.085f:1f);
            b.setScaleY(focus?1.085f:1f);
            b.setElevation(focus?dp(12):0);
            if(focus)showControls();
        });
        b.setOnClickListener(view->{action.run();showControls();});
        return b;
    }

    private void addCenter(View v,int size){
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(size),dp(size));
        lp.leftMargin=dp(10);
        lp.rightMargin=dp(10);
        centerControls.addView(v,lp);
    }

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        Intent i=getIntent();
        url=i.getStringExtra("url");
        title=i.getStringExtra("title");
        animeId=i.getStringExtra("animeId");
        poster=i.getStringExtra("poster");
        episode=i.getStringExtra("episode");
        nextUrl=i.getStringExtra("nextUrl");
        nextLabel=i.getStringExtra("nextLabel");
        startPosition=i.getLongExtra("startPosition",0);
        openingEndMs=i.getLongExtra("openingEndMs",-1);

        if(url==null||!url.startsWith("https://")){
            Toast.makeText(this,"URL HTTPS inválida",Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        ScreenFit.immersive(this);
        screenFit=new ScreenFit(this);

        FrameLayout root=new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        setContentView(root);

        video=new PlayerView(this);
        video.setUseController(false);
        video.setKeepScreenOn(true);
        root.addView(video,new FrameLayout.LayoutParams(-1,-1));

        tap=new View(this);
        tap.setFocusable(true);
        tap.setClickable(true);
        tap.setOnClickListener(v->{showControls();playPause.requestFocus();});
        root.addView(tap,new FrameLayout.LayoutParams(-1,-1));

        controls=new FrameLayout(this);
        controls.setClipChildren(false);
        controls.setClipToPadding(false);
        root.addView(controls,screenFit.centered());

        titleArea=new LinearLayout(this);
        titleArea.setOrientation(LinearLayout.VERTICAL);
        titleArea.setPadding(dp(24),dp(18),dp(24),dp(12));
        titleArea.setBackground(new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{0xB8000000,0x70000000,0x00000000}
        ));
        FrameLayout.LayoutParams titleLp=new FrameLayout.LayoutParams(dp(610),dp(92),Gravity.TOP|Gravity.LEFT);
        titleLp.leftMargin=dp(12);
        titleLp.topMargin=dp(10);
        controls.addView(titleArea,titleLp);

        episodeView=new TextView(this);
        episodeView.setText(episode==null?"":episode);
        episodeView.setTextColor(RED);
        episodeView.setTextSize(12*screenFit.textScale(this));
        episodeView.setTypeface(null,android.graphics.Typeface.BOLD);
        episodeView.setLetterSpacing(.06f);
        titleArea.addView(episodeView,new LinearLayout.LayoutParams(-1,-2));

        titleView=new TextView(this);
        titleView.setText(title==null?"Koda Anime":title);
        titleView.setTextColor(WHITE);
        titleView.setTextSize(18*screenFit.textScale(this));
        titleView.setTypeface(null,android.graphics.Typeface.BOLD);
        titleView.setSingleLine(true);
        titleView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams tvp=new LinearLayout.LayoutParams(-1,-2);
        tvp.topMargin=dp(4);
        titleArea.addView(titleView,tvp);

        centerControls=new LinearLayout(this);
        centerControls.setGravity(Gravity.CENTER);
        centerControls.setClipChildren(false);
        FrameLayout.LayoutParams centerLp=new FrameLayout.LayoutParams(-1,-2,Gravity.CENTER);
        controls.addView(centerControls,centerLp);

        rewind=imageCircle(R.drawable.koda_player_rewind,()->seekBy(-10000));
        addCenter(rewind,70);

        playPause=imageCircle(R.drawable.koda_player_pause,()->{
            if(player==null)return;
            if(player.isPlaying())player.pause(); else player.play();
            update();
        });
        addCenter(playPause,84);

        forward=forwardButton(()->seekBy(10000));
        addCenter(forward,70);

        bottomPanel=new LinearLayout(this);
        bottomPanel.setOrientation(LinearLayout.VERTICAL);
        bottomPanel.setPadding(dp(24),dp(22),dp(24),dp(12));
        bottomPanel.setBackground(new GradientDrawable(
            GradientDrawable.Orientation.BOTTOM_TOP,
            new int[]{0xF0000000,0xB0000000,0x00000000}
        ));
        FrameLayout.LayoutParams bottomLp=new FrameLayout.LayoutParams(-1,dp(150),Gravity.BOTTOM);
        controls.addView(bottomPanel,bottomLp);

        LinearLayout seekRow=new LinearLayout(this);
        seekRow.setGravity(Gravity.CENTER_VERTICAL);
        bottomPanel.addView(seekRow,new LinearLayout.LayoutParams(-1,dp(46)));

        elapsedView=new TextView(this);
        elapsedView.setText("00:00");
        elapsedView.setTextColor(WHITE);
        elapsedView.setTextSize(12*screenFit.textScale(this));
        elapsedView.setGravity(Gravity.CENTER_VERTICAL|Gravity.LEFT);
        seekRow.addView(elapsedView,new LinearLayout.LayoutParams(dp(58),-1));

        progress=new SeekBar(this);
        progress.setMax(1000);
        progress.setProgressTintList(ColorStateList.valueOf(RED));
        progress.setProgressBackgroundTintList(ColorStateList.valueOf(0xFF4A4A52));
        progress.setThumbTintList(ColorStateList.valueOf(RED));
        LinearLayout.LayoutParams plp=new LinearLayout.LayoutParams(0,dp(34),1);
        plp.leftMargin=dp(8);
        plp.rightMargin=dp(8);
        seekRow.addView(progress,plp);

        durationView=new TextView(this);
        durationView.setText("00:00");
        durationView.setTextColor(MUTED);
        durationView.setTextSize(12*screenFit.textScale(this));
        durationView.setGravity(Gravity.CENTER_VERTICAL|Gravity.RIGHT);
        seekRow.addView(durationView,new LinearLayout.LayoutParams(dp(58),-1));

        progress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override public void onStartTrackingTouch(SeekBar s){tracking=true;handler.removeCallbacks(hide);}
            @Override public void onStopTrackingTouch(SeekBar s){
                tracking=false;
                if(player!=null)seekTo(s.getProgress()*Math.max(0,player.getDuration())/1000);
                showControls();
            }
            @Override public void onProgressChanged(SeekBar s,int p,boolean fromUser){
                if(fromUser&&player!=null){
                    long dur=Math.max(0,player.getDuration());
                    elapsedView.setText(format(p*dur/1000));
                }
            }
        });

        LinearLayout actions=new LinearLayout(this);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        bottomPanel.addView(actions,new LinearLayout.LayoutParams(-1,dp(58)));

        quality=actionButton("⚙  Qualidade: Auto",this::showQuality);
        LinearLayout.LayoutParams qlp=new LinearLayout.LayoutParams(dp(190),dp(44));
        qlp.rightMargin=dp(10);
        actions.addView(quality,qlp);

        next=actionButton("Próximo episódio  ›",this::playNext);
        LinearLayout.LayoutParams nlp=new LinearLayout.LayoutParams(dp(200),dp(44));
        nlp.rightMargin=dp(10);
        actions.addView(next,nlp);
        next.setVisibility(hasNext()?View.VISIBLE:View.GONE);

        skip=actionButton("Pular abertura  »",()->{
            if(player!=null && openingEndMs>0 && openingEndMs>player.getCurrentPosition())seekTo(openingEndMs);
        });
        actions.addView(skip,new LinearLayout.LayoutParams(dp(185),dp(44)));
        skip.setVisibility(View.GONE);

        showControls();
        handler.post(updateLoop);
    }

    private boolean hasNext(){return nextUrl!=null&&nextUrl.startsWith("https://");}
    private String format(long ms){long s=Math.max(0,ms)/1000;return String.format(Locale.ROOT,"%02d:%02d",s/60,s%60);}

    private final Runnable hide=()->{
        if(tracking)return;
        titleArea.setVisibility(View.GONE);
        centerControls.setVisibility(View.GONE);
        bottomPanel.setVisibility(View.GONE);
        tap.requestFocus();
    };

    private final Runnable updateLoop=new Runnable(){
        @Override public void run(){
            update();
            handler.postDelayed(this,500);
        }
    };

    private void showControls(){
        titleArea.setVisibility(View.VISIBLE);
        centerControls.setVisibility(View.VISIBLE);
        bottomPanel.setVisibility(View.VISIBLE);
        handler.removeCallbacks(hide);
        handler.postDelayed(hide,4200);
    }

    private void update(){
        if(player==null)return;
        long pos=Math.max(0,player.getCurrentPosition());
        long dur=Math.max(0,player.getDuration());
        playPause.setImageResource(player.isPlaying()?R.drawable.koda_player_pause:R.drawable.koda_player_play);

        boolean canSkip=openingEndMs>0 && pos<openingEndMs;
        skip.setVisibility(canSkip?View.VISIBLE:View.GONE);

        if(!tracking){
            elapsedView.setText(format(pos));
            durationView.setText(format(dur));
            progress.setProgress(dur>0?(int)Math.min(1000,pos*1000/dur):0);
        }
    }

    @Override protected void onStart(){
        super.onStart();
        if(url==null||player!=null)return;

        player=new ExoPlayer.Builder(this).build();
        video.setPlayer(player);
        player.addListener(new Player.Listener(){
            @Override public void onPlayerError(PlaybackException e){
                Toast.makeText(PlayerActivity.this,"Não foi possível reproduzir: "+e.getErrorCodeName(),Toast.LENGTH_LONG).show();
                showControls();
            }
            @Override public void onPlaybackStateChanged(int state){
                if(state==Player.STATE_ENDED){save(true);showControls();}
                update();
            }
        });

        player.setMediaItem(MediaItem.fromUri(url));
        player.prepare();
        if(startPosition>0)player.seekTo(startPosition);
        player.play();
    }

    private void seekBy(long ms){if(player!=null)seekTo(player.getCurrentPosition()+ms);}

    private void seekTo(long ms){
        if(player==null)return;
        long d=player.getDuration();
        player.seekTo(Math.max(0,d>0?Math.min(d,ms):ms));
        update();
    }

    private void save(boolean ended){
        if(player==null||animeId==null)return;
        long d=Math.max(0,player.getDuration());
        long pos=ended&&d>0?d:Math.max(0,player.getCurrentPosition());
        startPosition=pos;
        WatchHistory.save(this,animeId,title==null?"Anime":title,poster==null?"":poster,episode==null?"Episódio":episode,url,pos,d);
    }

    private void playNext(){
        if(!hasNext())return;
        save(false);
        url=nextUrl;
        episode=nextLabel;
        nextUrl="";
        startPosition=0;
        openingEndMs=-1;
        next.setVisibility(View.GONE);
        skip.setVisibility(View.GONE);
        episodeView.setText(episode==null?"":episode);
        player.setMediaItem(MediaItem.fromUri(url));
        player.prepare();
        player.play();
        showControls();
    }

    private void showQuality(){
        if(player==null)return;

        ArrayList<String> labels=new ArrayList<>();
        ArrayList<TrackSelectionOverride> choices=new ArrayList<>();
        labels.add("Automática");
        choices.add(null);

        Set<String> seen=new HashSet<>();
        for(Tracks.Group group:player.getCurrentTracks().getGroups()){
            if(group.getType()!=C.TRACK_TYPE_VIDEO)continue;
            for(int i=0;i<group.length;i++){
                if(!group.isTrackSupported(i))continue;
                Format f=group.getTrackFormat(i);
                String name=f.height>0?f.height+"p":f.bitrate>0?f.bitrate/1000+" kbps":"Faixa "+(i+1);
                if(seen.add(name)){
                    labels.add(name);
                    choices.add(new TrackSelectionOverride(group.getMediaTrackGroup(),i));
                }
            }
        }

        if(labels.size()==1){
            new AlertDialog.Builder(this)
                .setMessage("Este vídeo oferece apenas a qualidade original.")
                .setPositiveButton("OK",null)
                .show();
            return;
        }

        new AlertDialog.Builder(this)
            .setTitle("Resolução")
            .setItems(labels.toArray(new String[0]),(dialog,index)->{
                TrackSelectionParameters.Builder p=player.getTrackSelectionParameters().buildUpon();
                p.clearOverridesOfType(C.TRACK_TYPE_VIDEO);
                if(index>0)p.setOverrideForType(choices.get(index));
                player.setTrackSelectionParameters(p.build());
                quality.setText("⚙  Qualidade: "+labels.get(index));
                showControls();
            })
            .show();
    }

    @Override public boolean onKeyDown(int key,KeyEvent event){
        if(key==KeyEvent.KEYCODE_MEDIA_REWIND){seekBy(-10000);showControls();return true;}
        if(key==KeyEvent.KEYCODE_MEDIA_FAST_FORWARD){seekBy(10000);showControls();return true;}
        if(key==KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE&&player!=null){
            if(player.isPlaying())player.pause(); else player.play();
            showControls();
            update();
            return true;
        }

        boolean navKey=key==KeyEvent.KEYCODE_DPAD_CENTER||key==KeyEvent.KEYCODE_DPAD_UP||
            key==KeyEvent.KEYCODE_DPAD_DOWN||key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT;

        if(navKey && centerControls.getVisibility()!=View.VISIBLE){
            showControls();
            playPause.requestFocus();
            return true;
        }

        if(navKey)showControls();
        return super.onKeyDown(key,event);
    }

    @Override protected void onStop(){
        save(false);
        if(player!=null){
            player.release();
            player=null;
            video.setPlayer(null);
        }
        super.onStop();
    }

    @Override protected void onDestroy(){
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}

package com.koda.anime;

import android.app.*;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.media3.common.*;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;
import java.util.*;

/**
 * Koda Anime TV player.
 *
 * 0.18 rebuild goals:
 * - the video is the only thing left on screen when controls are hidden;
 * - no atlas/sprite-sheet or decorative overlay is used by the player;
 * - controls are lightweight, TV/D-pad friendly and rendered over subtle scrims;
 * - playback/history/quality/next episode behavior stays compatible with 0.17.1.
 */
public final class PlayerActivity extends Activity {
    private static final int WHITE=0xFFFFFFFF;
    private static final int MUTED=0xFFB8B8C0;
    private static final int RED=0xFFFF1635;
    private static final int CONTROL_BG=0xC20A0A0D;
    private static final long CONTROLS_TIMEOUT_MS=4200L;

    private final Handler handler=new Handler(Looper.getMainLooper());

    private ExoPlayer player;
    private PlayerView video;
    private ScreenFit screenFit;

    private FrameLayout controlsLayer;
    private LinearLayout centerControls;
    private LinearLayout bottomActions;
    private TextView episodeView,titleView,elapsedView,durationView;
    private TextView quality,next,skip;
    private ImageButton rewind,playPause,forward;
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

    private void animateFocus(View v,boolean focused){
        v.animate().cancel();
        v.animate().scaleX(focused?1.075f:1f).scaleY(focused?1.075f:1f).setDuration(110).start();
        v.setElevation(focused?dp(11):0);
    }

    private TextView actionButton(int iconRes,String text,Runnable action){
        TextView v=new TextView(this);
        v.setText(text);
        v.setTextColor(WHITE);
        v.setTextSize(13*screenFit.textScale(this));
        v.setGravity(Gravity.CENTER);
        v.setSingleLine(true);
        v.setTypeface(null,Typeface.BOLD);
        v.setPadding(dp(14),0,dp(16),0);
        v.setBackground(rounded(0xB5121217,13));
        v.setFocusable(true);
        v.setClickable(true);

        Drawable icon=getDrawable(iconRes);
        if(icon!=null){
            icon.setBounds(0,0,dp(21),dp(21));
            v.setCompoundDrawables(icon,null,null,null);
            v.setCompoundDrawablePadding(dp(9));
        }

        v.setOnFocusChangeListener((view,focus)->{
            v.setBackground(focus?outline(0xD9270710,RED,13,2):rounded(0xB5121217,13));
            animateFocus(v,focus);
            if(focus)showControls();
        });
        v.setOnClickListener(view->{action.run();showControls();});
        return v;
    }

    private ImageButton imageCircle(int drawable,Runnable action,int iconPadding){
        ImageButton b=new ImageButton(this);
        b.setImageResource(drawable);
        b.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        b.setBackground(circle(CONTROL_BG,0,0));
        b.setPadding(dp(iconPadding),dp(iconPadding),dp(iconPadding),dp(iconPadding));
        b.setFocusable(true);
        b.setClickable(true);
        b.setOnFocusChangeListener((view,focus)->{
            b.setBackground(focus?circle(0xE318080D,RED,2):circle(CONTROL_BG,0,0));
            animateFocus(b,focus);
            if(focus)showControls();
        });
        b.setOnClickListener(view->{action.run();showControls();});
        return b;
    }

    private void addCenter(View v,int size){
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(size),dp(size));
        lp.leftMargin=dp(11);
        lp.rightMargin=dp(11);
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

        // Keep the 0.17.1 ScreenFit hotfix. ScreenFit itself waits for DecorView
        // before requesting WindowInsetsController on Android 30+.
        ScreenFit.immersive(this);
        screenFit=new ScreenFit(this);

        FrameLayout root=new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        setContentView(root);

        video=new PlayerView(this);
        video.setUseController(false);
        video.setKeepScreenOn(true);
        video.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
        root.addView(video,new FrameLayout.LayoutParams(-1,-1));

        // The tap/focus surface is deliberately empty. When controls are hidden,
        // this plus the PlayerView means the screen contains only the video.
        tap=new View(this);
        tap.setFocusable(true);
        tap.setClickable(true);
        tap.setBackgroundColor(Color.TRANSPARENT);
        tap.setOnClickListener(v->{showControls();playPause.requestFocus();});
        root.addView(tap,new FrameLayout.LayoutParams(-1,-1));

        controlsLayer=new FrameLayout(this);
        controlsLayer.setClipChildren(false);
        controlsLayer.setClipToPadding(false);
        root.addView(controlsLayer,screenFit.centered());

        View topScrim=new View(this);
        topScrim.setBackground(new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{0xC8000000,0x76000000,0x00000000}
        ));
        controlsLayer.addView(topScrim,new FrameLayout.LayoutParams(-1,dp(128),Gravity.TOP));

        View bottomScrim=new View(this);
        bottomScrim.setBackground(new GradientDrawable(
            GradientDrawable.Orientation.BOTTOM_TOP,
            new int[]{0xDD000000,0x8C000000,0x00000000}
        ));
        controlsLayer.addView(bottomScrim,new FrameLayout.LayoutParams(-1,dp(162),Gravity.BOTTOM));

        LinearLayout titleArea=new LinearLayout(this);
        titleArea.setOrientation(LinearLayout.VERTICAL);
        FrameLayout.LayoutParams titleLp=new FrameLayout.LayoutParams(dp(620),-2,Gravity.TOP|Gravity.LEFT);
        titleLp.leftMargin=dp(28);
        titleLp.topMargin=dp(21);
        controlsLayer.addView(titleArea,titleLp);

        titleView=new TextView(this);
        titleView.setText(title==null?"Koda Anime":title);
        titleView.setTextColor(WHITE);
        titleView.setTextSize(18*screenFit.textScale(this));
        titleView.setTypeface(null,Typeface.BOLD);
        titleView.setSingleLine(true);
        titleView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        titleView.setShadowLayer(dp(5),0,dp(1),0xFF000000);
        titleArea.addView(titleView,new LinearLayout.LayoutParams(-1,-2));

        episodeView=new TextView(this);
        episodeView.setText(episode==null?"":episode);
        episodeView.setTextColor(MUTED);
        episodeView.setTextSize(12*screenFit.textScale(this));
        episodeView.setSingleLine(true);
        episodeView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        episodeView.setShadowLayer(dp(4),0,dp(1),0xFF000000);
        LinearLayout.LayoutParams evp=new LinearLayout.LayoutParams(-1,-2);
        evp.topMargin=dp(3);
        titleArea.addView(episodeView,evp);

        centerControls=new LinearLayout(this);
        centerControls.setGravity(Gravity.CENTER);
        centerControls.setClipChildren(false);
        FrameLayout.LayoutParams centerLp=new FrameLayout.LayoutParams(-1,-2,Gravity.CENTER);
        controlsLayer.addView(centerControls,centerLp);

        rewind=imageCircle(R.drawable.koda_player_rewind,()->seekBy(-10000),11);
        addCenter(rewind,72);

        playPause=imageCircle(R.drawable.koda_player_pause,()->{
            if(player==null)return;
            if(player.isPlaying())player.pause(); else player.play();
            update();
        },18);
        addCenter(playPause,88);

        forward=imageCircle(R.drawable.koda_player_forward,()->seekBy(10000),11);
        addCenter(forward,72);

        LinearLayout seekRow=new LinearLayout(this);
        seekRow.setGravity(Gravity.CENTER_VERTICAL);
        FrameLayout.LayoutParams seekLp=new FrameLayout.LayoutParams(-1,dp(42),Gravity.BOTTOM);
        seekLp.leftMargin=dp(28);
        seekLp.rightMargin=dp(28);
        seekLp.bottomMargin=dp(58);
        controlsLayer.addView(seekRow,seekLp);

        elapsedView=new TextView(this);
        elapsedView.setText("00:00");
        elapsedView.setTextColor(WHITE);
        elapsedView.setTextSize(12*screenFit.textScale(this));
        elapsedView.setGravity(Gravity.CENTER_VERTICAL|Gravity.LEFT);
        seekRow.addView(elapsedView,new LinearLayout.LayoutParams(dp(62),-1));

        progress=new SeekBar(this);
        progress.setMax(1000);
        progress.setFocusable(true);
        progress.setProgressTintList(ColorStateList.valueOf(RED));
        progress.setProgressBackgroundTintList(ColorStateList.valueOf(0xFF55555D));
        progress.setThumbTintList(ColorStateList.valueOf(WHITE));
        progress.setPadding(0,0,0,0);
        LinearLayout.LayoutParams progressLp=new LinearLayout.LayoutParams(0,dp(30),1);
        progressLp.leftMargin=dp(8);
        progressLp.rightMargin=dp(8);
        seekRow.addView(progress,progressLp);

        durationView=new TextView(this);
        durationView.setText("00:00");
        durationView.setTextColor(MUTED);
        durationView.setTextSize(12*screenFit.textScale(this));
        durationView.setGravity(Gravity.CENTER_VERTICAL|Gravity.RIGHT);
        seekRow.addView(durationView,new LinearLayout.LayoutParams(dp(62),-1));

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
        progress.setOnKeyListener((v,key,event)->{
            if(event.getAction()!=KeyEvent.ACTION_DOWN)return false;
            if(key==KeyEvent.KEYCODE_DPAD_LEFT){seekBy(-10000);showControls();return true;}
            if(key==KeyEvent.KEYCODE_DPAD_RIGHT){seekBy(10000);showControls();return true;}
            return false;
        });
        progress.setOnFocusChangeListener((v,focus)->{
            progress.setThumbTintList(ColorStateList.valueOf(focus?RED:WHITE));
            if(focus)showControls();
        });

        bottomActions=new LinearLayout(this);
        bottomActions.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);
        bottomActions.setClipChildren(false);
        FrameLayout.LayoutParams actionsLp=new FrameLayout.LayoutParams(-2,dp(48),Gravity.BOTTOM|Gravity.LEFT);
        actionsLp.leftMargin=dp(28);
        actionsLp.bottomMargin=dp(10);
        controlsLayer.addView(bottomActions,actionsLp);

        quality=actionButton(R.drawable.koda_player_quality,"Qualidade",this::showQuality);
        LinearLayout.LayoutParams qualityLp=new LinearLayout.LayoutParams(dp(158),dp(44));
        qualityLp.rightMargin=dp(10);
        bottomActions.addView(quality,qualityLp);

        next=actionButton(R.drawable.koda_player_next,"Próximo episódio",this::playNext);
        LinearLayout.LayoutParams nextLp=new LinearLayout.LayoutParams(dp(194),dp(44));
        nextLp.rightMargin=dp(10);
        bottomActions.addView(next,nextLp);
        next.setVisibility(hasNext()?View.VISIBLE:View.GONE);

        skip=actionButton(R.drawable.koda_player_skip,"Pular abertura",()->{
            if(player!=null&&openingEndMs>0&&openingEndMs>player.getCurrentPosition())seekTo(openingEndMs);
        });
        bottomActions.addView(skip,new LinearLayout.LayoutParams(dp(175),dp(44)));
        skip.setVisibility(View.GONE);

        showControls();
        playPause.requestFocus();
        handler.post(updateLoop);
    }

    private boolean hasNext(){return nextUrl!=null&&nextUrl.startsWith("https://");}

    private String format(long ms){
        long total=Math.max(0,ms)/1000;
        long hours=total/3600;
        long minutes=(total%3600)/60;
        long seconds=total%60;
        return hours>0
            ?String.format(Locale.ROOT,"%d:%02d:%02d",hours,minutes,seconds)
            :String.format(Locale.ROOT,"%02d:%02d",minutes,seconds);
    }

    private final Runnable hide=()->hideControls();

    private final Runnable updateLoop=new Runnable(){
        @Override public void run(){
            update();
            handler.postDelayed(this,500);
        }
    };

    private void hideControls(){
        if(tracking||controlsLayer==null)return;
        controlsLayer.setVisibility(View.GONE);
        tap.requestFocus();
    }

    private void showControls(){
        if(controlsLayer==null)return;
        controlsLayer.setVisibility(View.VISIBLE);
        handler.removeCallbacks(hide);
        handler.postDelayed(hide,CONTROLS_TIMEOUT_MS);
    }

    private void update(){
        if(player==null)return;
        long pos=Math.max(0,player.getCurrentPosition());
        long dur=Math.max(0,player.getDuration());
        playPause.setImageResource(player.isPlaying()?R.drawable.koda_player_pause:R.drawable.koda_player_play);

        // Never invent or hard-code an opening duration. The button only exists
        // when a real timestamp was supplied by the caller.
        boolean canSkip=openingEndMs>0&&pos<openingEndMs;
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
            @Override public void onIsPlayingChanged(boolean isPlaying){update();}
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
        nextLabel="";
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

        Set<String> seen=new LinkedHashSet<>();
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
            .setTitle("Qualidade")
            .setItems(labels.toArray(new String[0]),(dialog,index)->{
                TrackSelectionParameters.Builder p=player.getTrackSelectionParameters().buildUpon();
                p.clearOverridesOfType(C.TRACK_TYPE_VIDEO);
                if(index>0)p.setOverrideForType(choices.get(index));
                player.setTrackSelectionParameters(p.build());
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

        if(navKey&&controlsLayer!=null&&controlsLayer.getVisibility()!=View.VISIBLE){
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

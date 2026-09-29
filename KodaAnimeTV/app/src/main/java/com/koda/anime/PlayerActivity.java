package com.koda.anime;

import android.app.*;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.*;
import android.graphics.drawable.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.media3.common.*;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;
import java.util.*;

public final class PlayerActivity extends Activity {
    private static final int WHITE=0xFFFFFFFF,MUTED=0xFFB8B8C0,RED=0xFFFF1635;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private ExoPlayer player; private PlayerView video;
    private FrameLayout controlsLayer; private LinearLayout centerRow,topActions,bottomActions;
    private ImageButton playPause,rewind,forward;
    private TextView next,skip,quality,audio,subtitles,heading,time;
    private SeekBar progress; private View tap;
    private boolean tracking;
    private long startPosition,openingEndMs=-1;
    private String url,title,animeId,poster,episode,nextUrl,nextLabel;
    private ScreenFit screenFit;

    private int dp(float n){return screenFit.px(n);}
    private GradientDrawable bg(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
    private GradientDrawable outline(int fill,int stroke,int radius,int width){GradientDrawable g=bg(fill,radius);g.setStroke(dp(width),stroke);return g;}
    private GradientDrawable circle(int fill,int stroke,int width){GradientDrawable g=new GradientDrawable();g.setShape(GradientDrawable.OVAL);g.setColor(fill);if(width>0)g.setStroke(dp(width),stroke);return g;}

    private void focusAnim(View v,boolean f){v.animate().cancel();v.animate().scaleX(f?1.08f:1f).scaleY(f?1.08f:1f).setDuration(110).start();v.setElevation(f?dp(12):0);}

    private ImageButton circleButton(String kind,Runnable action,int iconPad){
        ImageButton b=new ImageButton(this);b.setImageDrawable(new KodaIcon(kind,WHITE));b.setScaleType(ImageView.ScaleType.FIT_CENTER);
        b.setPadding(dp(iconPad),dp(iconPad),dp(iconPad),dp(iconPad));b.setBackground(circle(0xD9101014,0,0));b.setFocusable(true);b.setClickable(true);
        b.setOnFocusChangeListener((v,f)->{b.setBackground(f?circle(0xE62A0710,RED,2):circle(0xD9101014,0,0));focusAnim(b,f);if(f)showControls();});
        b.setOnClickListener(v->{action.run();showControls();});return b;
    }

    private TextView pill(String kind,String label,Runnable action){
        TextView b=new TextView(this);b.setText(label);b.setTextColor(WHITE);b.setTextSize(13*screenFit.textScale(this));b.setTypeface(null,Typeface.BOLD);
        b.setGravity(Gravity.CENTER);b.setSingleLine(true);b.setPadding(dp(14),0,dp(16),0);b.setBackground(outline(0xD915151A,0xFF34343C,14,1));b.setFocusable(true);b.setClickable(true);
        KodaIcon icon=new KodaIcon(kind,WHITE);icon.setBounds(0,0,dp(22),dp(22));b.setCompoundDrawables(icon,null,null,null);b.setCompoundDrawablePadding(dp(9));
        b.setOnFocusChangeListener((v,f)->{b.setBackground(f?outline(0xE63A0713,RED,14,2):outline(0xD915151A,0xFF34343C,14,1));focusAnim(b,f);if(f)showControls();});
        b.setOnClickListener(v->{action.run();showControls();});return b;
    }

    private void addCircle(View v,int size){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(size),dp(size));p.leftMargin=dp(12);p.rightMargin=dp(12);centerRow.addView(v,p);}
    private void addPill(LinearLayout row,View v,int width){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(width),dp(48));p.leftMargin=dp(6);p.rightMargin=dp(6);row.addView(v,p);}

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        Intent i=getIntent();url=i.getStringExtra("url");title=i.getStringExtra("title");animeId=i.getStringExtra("animeId");poster=i.getStringExtra("poster");episode=i.getStringExtra("episode");
        nextUrl=i.getStringExtra("nextUrl");nextLabel=i.getStringExtra("nextLabel");startPosition=i.getLongExtra("startPosition",0);openingEndMs=i.getLongExtra("openingEndMs",-1);
        if(url==null||!url.startsWith("https://")){Toast.makeText(this,"URL HTTPS inválida",Toast.LENGTH_LONG).show();finish();return;}

        ScreenFit.immersive(this);screenFit=new ScreenFit(this);
        FrameLayout root=new FrameLayout(this);root.setBackgroundColor(Color.BLACK);setContentView(root);

        video=new PlayerView(this);video.setUseController(false);video.setKeepScreenOn(true);video.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);root.addView(video,new FrameLayout.LayoutParams(-1,-1));
        tap=new View(this);tap.setFocusable(true);tap.setClickable(true);tap.setBackgroundColor(Color.TRANSPARENT);tap.setOnClickListener(v->{showControls();playPause.requestFocus();});root.addView(tap,new FrameLayout.LayoutParams(-1,-1));

        controlsLayer=new FrameLayout(this);controlsLayer.setClipChildren(false);root.addView(controlsLayer,screenFit.centered());

        View topShade=new View(this);topShade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0xCC000000,0x62000000,0x00000000}));
        controlsLayer.addView(topShade,new FrameLayout.LayoutParams(-1,dp(145),Gravity.TOP));
        View bottomShade=new View(this);bottomShade.setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,new int[]{0xE6000000,0x82000000,0x00000000}));
        controlsLayer.addView(bottomShade,new FrameLayout.LayoutParams(-1,dp(172),Gravity.BOTTOM));

        heading=new TextView(this);heading.setText((episode==null?"":episode)+"\n"+(title==null?"Koda Anime":title));heading.setTextColor(WHITE);
        heading.setTextSize(17*screenFit.textScale(this));heading.setTypeface(null,Typeface.BOLD);heading.setShadowLayer(dp(6),0,dp(1),0xFF000000);heading.setMaxLines(2);
        FrameLayout.LayoutParams hp=new FrameLayout.LayoutParams(dp(520),-2,Gravity.TOP|Gravity.LEFT);hp.leftMargin=dp(28);hp.topMargin=dp(22);controlsLayer.addView(heading,hp);

        audio=pill(KodaIcon.AUDIO,"Áudio",()->showTrackPicker(C.TRACK_TYPE_AUDIO,"Áudio"));
        subtitles=pill(KodaIcon.SUBTITLES,"Legendas",this::showSubtitlePicker);
        quality=pill(KodaIcon.QUALITY,"Qualidade",this::showQuality);
        topActions=new LinearLayout(this);topActions.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        addPill(topActions,audio,126);addPill(topActions,subtitles,144);addPill(topActions,quality,150);
        FrameLayout.LayoutParams topLp=new FrameLayout.LayoutParams(-2,dp(52),Gravity.TOP|Gravity.RIGHT);topLp.rightMargin=dp(25);topLp.topMargin=dp(22);controlsLayer.addView(topActions,topLp);

        centerRow=new LinearLayout(this);centerRow.setGravity(Gravity.CENTER);centerRow.setClipChildren(false);
        rewind=circleButton(KodaIcon.REWIND10,()->seekBy(-10000),14);
        playPause=circleButton(KodaIcon.PAUSE,()->{if(player==null)return;if(player.isPlaying())player.pause();else player.play();update();},22);
        forward=circleButton(KodaIcon.FORWARD10,()->seekBy(10000),14);
        addCircle(rewind,94);addCircle(playPause,112);addCircle(forward,94);
        controlsLayer.addView(centerRow,new FrameLayout.LayoutParams(-1,-2,Gravity.CENTER));

        skip=pill(KodaIcon.SKIP,"Pular abertura",()->{if(player!=null&&openingEndMs>0&&openingEndMs>player.getCurrentPosition())seekTo(openingEndMs);});
        FrameLayout.LayoutParams skp=new FrameLayout.LayoutParams(dp(186),dp(48),Gravity.BOTTOM|Gravity.RIGHT);skp.rightMargin=dp(28);skp.bottomMargin=dp(106);controlsLayer.addView(skip,skp);skip.setVisibility(View.GONE);

        LinearLayout seekWrap=new LinearLayout(this);seekWrap.setGravity(Gravity.CENTER_VERTICAL);
        time=new TextView(this);time.setTextColor(WHITE);time.setTextSize(12*screenFit.textScale(this));time.setText("00:00 / 00:00");seekWrap.addView(time,new LinearLayout.LayoutParams(dp(112),-2));
        progress=new SeekBar(this);progress.setMax(1000);progress.setProgressTintList(ColorStateList.valueOf(RED));progress.setProgressBackgroundTintList(ColorStateList.valueOf(0xFF3F3F46));progress.setThumbTintList(ColorStateList.valueOf(WHITE));
        seekWrap.addView(progress,new LinearLayout.LayoutParams(0,dp(30),1));
        FrameLayout.LayoutParams seekLp=new FrameLayout.LayoutParams(-1,dp(38),Gravity.BOTTOM);seekLp.leftMargin=dp(28);seekLp.rightMargin=dp(28);seekLp.bottomMargin=dp(60);controlsLayer.addView(seekWrap,seekLp);

        progress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override public void onStartTrackingTouch(SeekBar s){tracking=true;handler.removeCallbacks(hide);}
            @Override public void onStopTrackingTouch(SeekBar s){tracking=false;if(player!=null)seekTo(s.getProgress()*Math.max(0,player.getDuration())/1000);showControls();}
            @Override public void onProgressChanged(SeekBar s,int p,boolean user){if(user&&player!=null)time.setText(format(p*Math.max(0,player.getDuration())/1000)+" / "+format(player.getDuration()));}
        });
        progress.setOnKeyListener((v,key,e)->{if(e.getAction()!=KeyEvent.ACTION_DOWN)return false;if(key==KeyEvent.KEYCODE_DPAD_LEFT){seekBy(-10000);showControls();return true;}if(key==KeyEvent.KEYCODE_DPAD_RIGHT){seekBy(10000);showControls();return true;}return false;});

        next=pill(KodaIcon.NEXT,"Próximo episódio",this::playNext);
        bottomActions=new LinearLayout(this);bottomActions.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);addPill(bottomActions,next,190);
        FrameLayout.LayoutParams blp=new FrameLayout.LayoutParams(-2,dp(50),Gravity.BOTTOM|Gravity.LEFT);blp.leftMargin=dp(28);blp.bottomMargin=dp(12);controlsLayer.addView(bottomActions,blp);
        next.setVisibility(nextUrl!=null&&nextUrl.startsWith("https://")?View.VISIBLE:View.GONE);

        showControls();playPause.requestFocus();handler.post(updateLoop);
    }

    private String format(long ms){long s=Math.max(0,ms)/1000;return s>=3600?String.format(Locale.ROOT,"%d:%02d:%02d",s/3600,(s%3600)/60,s%60):String.format(Locale.ROOT,"%02d:%02d",s/60,s%60);}
    private final Runnable hide=this::hideControls;
    private final Runnable updateLoop=new Runnable(){@Override public void run(){update();handler.postDelayed(this,500);}};

    private void hideControls(){if(tracking||controlsLayer==null)return;controlsLayer.setVisibility(View.GONE);tap.requestFocus();}
    private void showControls(){if(controlsLayer==null)return;controlsLayer.setVisibility(View.VISIBLE);handler.removeCallbacks(hide);handler.postDelayed(hide,4200);}

    private void update(){
        if(player==null)return;long pos=Math.max(0,player.getCurrentPosition()),dur=Math.max(0,player.getDuration());
        playPause.setImageDrawable(new KodaIcon(player.isPlaying()?KodaIcon.PAUSE:KodaIcon.PLAY,WHITE));
        skip.setVisibility(openingEndMs>0&&pos<openingEndMs?View.VISIBLE:View.GONE);
        if(!tracking){time.setText(format(pos)+" / "+format(dur));progress.setProgress(dur>0?(int)Math.min(1000,pos*1000/dur):0);}
    }

    @Override protected void onStart(){
        super.onStart();if(url==null||player!=null)return;
        player=new ExoPlayer.Builder(this).build();video.setPlayer(player);
        player.addListener(new Player.Listener(){
            @Override public void onPlayerError(PlaybackException e){Toast.makeText(PlayerActivity.this,"Não foi possível reproduzir: "+e.getErrorCodeName(),Toast.LENGTH_LONG).show();showControls();}
            @Override public void onPlaybackStateChanged(int state){if(state==Player.STATE_ENDED){save(true);showControls();}update();}
            @Override public void onIsPlayingChanged(boolean isPlaying){update();}
        });
        player.setMediaItem(MediaItem.fromUri(url));player.prepare();if(startPosition>0)player.seekTo(startPosition);player.play();
    }

    private void seekBy(long ms){if(player!=null)seekTo(player.getCurrentPosition()+ms);}
    private void seekTo(long ms){if(player==null)return;long d=player.getDuration();player.seekTo(Math.max(0,d>0?Math.min(d,ms):ms));update();}

    private void save(boolean ended){if(player==null||animeId==null)return;long d=Math.max(0,player.getDuration());long pos=ended&&d>0?d:Math.max(0,player.getCurrentPosition());startPosition=pos;WatchHistory.save(this,animeId,title==null?"Anime":title,poster==null?"":poster,episode==null?"Episódio":episode,url,pos,d);}

    private void playNext(){
        if(nextUrl==null||!nextUrl.startsWith("https://"))return;save(false);url=nextUrl;episode=nextLabel;nextUrl="";nextLabel="";next.setVisibility(View.GONE);startPosition=0;openingEndMs=-1;
        heading.setText((episode==null?"Episódio":episode)+"\n"+title);player.setMediaItem(MediaItem.fromUri(url));player.prepare();player.play();showControls();
    }

    private String trackName(Format f,int index){if(f.label!=null&&!f.label.trim().isEmpty())return f.label;if(f.language!=null&&!f.language.trim().isEmpty())return f.language.toUpperCase(Locale.ROOT);if(f.height>0)return f.height+"p";if(f.bitrate>0)return (f.bitrate/1000)+" kbps";return "Faixa "+(index+1);}

    private void showQuality(){
        if(player==null)return;ArrayList<String> labels=new ArrayList<>();ArrayList<TrackSelectionOverride> choices=new ArrayList<>();labels.add("Automática");choices.add(null);Set<String> seen=new LinkedHashSet<>();
        for(Tracks.Group group:player.getCurrentTracks().getGroups())if(group.getType()==C.TRACK_TYPE_VIDEO)for(int i=0;i<group.length;i++)if(group.isTrackSupported(i)){Format f=group.getTrackFormat(i);String name=f.height>0?f.height+"p":trackName(f,i);if(seen.add(name)){labels.add(name);choices.add(new TrackSelectionOverride(group.getMediaTrackGroup(),i));}}
        if(labels.size()==1){Toast.makeText(this,"Este vídeo oferece apenas a qualidade original.",Toast.LENGTH_SHORT).show();return;}
        new AlertDialog.Builder(this).setTitle("Qualidade").setItems(labels.toArray(new String[0]),(d,index)->{TrackSelectionParameters.Builder p=player.getTrackSelectionParameters().buildUpon();p.clearOverridesOfType(C.TRACK_TYPE_VIDEO);if(index>0)p.setOverrideForType(choices.get(index));player.setTrackSelectionParameters(p.build());showControls();}).show();
    }

    private void showTrackPicker(int type,String titleText){
        if(player==null)return;ArrayList<String> labels=new ArrayList<>();ArrayList<TrackSelectionOverride> choices=new ArrayList<>();
        for(Tracks.Group group:player.getCurrentTracks().getGroups())if(group.getType()==type)for(int i=0;i<group.length;i++)if(group.isTrackSupported(i)){labels.add(trackName(group.getTrackFormat(i),i));choices.add(new TrackSelectionOverride(group.getMediaTrackGroup(),i));}
        if(labels.isEmpty()){Toast.makeText(this,titleText+" indisponível neste vídeo.",Toast.LENGTH_SHORT).show();return;}
        new AlertDialog.Builder(this).setTitle(titleText).setItems(labels.toArray(new String[0]),(d,index)->{TrackSelectionParameters.Builder p=player.getTrackSelectionParameters().buildUpon();p.clearOverridesOfType(type);p.setTrackTypeDisabled(type,false);p.setOverrideForType(choices.get(index));player.setTrackSelectionParameters(p.build());showControls();}).show();
    }

    private void showSubtitlePicker(){
        if(player==null)return;ArrayList<String> labels=new ArrayList<>();ArrayList<TrackSelectionOverride> choices=new ArrayList<>();labels.add("Desativadas");choices.add(null);
        for(Tracks.Group group:player.getCurrentTracks().getGroups())if(group.getType()==C.TRACK_TYPE_TEXT)for(int i=0;i<group.length;i++)if(group.isTrackSupported(i)){labels.add(trackName(group.getTrackFormat(i),i));choices.add(new TrackSelectionOverride(group.getMediaTrackGroup(),i));}
        if(labels.size()==1){Toast.makeText(this,"Legendas indisponíveis neste vídeo.",Toast.LENGTH_SHORT).show();return;}
        new AlertDialog.Builder(this).setTitle("Legendas").setItems(labels.toArray(new String[0]),(d,index)->{TrackSelectionParameters.Builder p=player.getTrackSelectionParameters().buildUpon();p.clearOverridesOfType(C.TRACK_TYPE_TEXT);p.setTrackTypeDisabled(C.TRACK_TYPE_TEXT,index==0);if(index>0)p.setOverrideForType(choices.get(index));player.setTrackSelectionParameters(p.build());showControls();}).show();
    }

    @Override public boolean onKeyDown(int key,KeyEvent event){
        if(key==KeyEvent.KEYCODE_MEDIA_REWIND){seekBy(-10000);showControls();return true;}if(key==KeyEvent.KEYCODE_MEDIA_FAST_FORWARD){seekBy(10000);showControls();return true;}
        if(key==KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE&&player!=null){if(player.isPlaying())player.pause();else player.play();showControls();update();return true;}
        boolean dpad=key==KeyEvent.KEYCODE_DPAD_CENTER||key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_DOWN||key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT;
        if(dpad&&controlsLayer.getVisibility()!=View.VISIBLE){showControls();playPause.requestFocus();return true;}if(dpad)showControls();return super.onKeyDown(key,event);
    }

    @Override protected void onStop(){save(false);if(player!=null){player.release();player=null;video.setPlayer(null);}super.onStop();}
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);super.onDestroy();}
}

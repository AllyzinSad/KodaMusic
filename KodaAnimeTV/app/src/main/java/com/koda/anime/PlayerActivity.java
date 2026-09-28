package com.koda.anime;

import android.app.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.Tracks;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import java.util.*;

public final class PlayerActivity extends Activity {
    private ExoPlayer player; private TextView quality;
    private long openingEndMs=-1;
    private int dp(int v){return (int)(getResources().getDisplayMetrics().density*v+.5f);}
    private GradientDrawable shape(int fill,int stroke){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(10));d.setStroke(dp(2),stroke);return d;}
    private TextView control(String title,Runnable action){TextView v=new TextView(this);v.setText(title);v.setTextColor(Color.WHITE);v.setTextSize(16);v.setGravity(Gravity.CENTER);v.setPadding(dp(16),dp(10),dp(16),dp(10));v.setBackground(shape(0xDD151519,0xFF4A4A50));v.setFocusable(true);
        v.setOnFocusChangeListener((view,focus)->v.setBackground(focus?shape(0xFF471A22,0xFFE7333E):shape(0xDD151519,0xFF4A4A50)));
        v.setOnClickListener(x->action.run());return v;
    }
    @Override public void onCreate(Bundle b){super.onCreate(b);
        String url=getIntent().getStringExtra("url"),title=getIntent().getStringExtra("title");
        openingEndMs=getIntent().getLongExtra("openingEndMs",-1);
        if(url==null||!url.startsWith("https://")){Toast.makeText(this,"URL HTTPS inválida",Toast.LENGTH_LONG).show();finish();return;}
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        FrameLayout root=new FrameLayout(this);root.setBackgroundColor(Color.BLACK);setContentView(root);
        PlayerView view=new PlayerView(this);view.setUseController(true);view.setControllerShowTimeoutMs(4500);root.addView(view,new FrameLayout.LayoutParams(-1,-1));
        TextView heading=new TextView(this);heading.setText(title==null?"Koda Anime":title);heading.setTextColor(-1);heading.setTextSize(17);heading.setPadding(dp(27),dp(14),dp(27),dp(14));heading.setBackgroundColor(0x88000000);
        root.addView(heading,new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.LEFT));
        LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER);bar.setPadding(dp(12),dp(8),dp(12),dp(16));bar.setBackgroundColor(0xAA09090C);
        FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);root.addView(bar,bp);
        add(bar,control("↶ 10 s",()->seekBy(-10000)));
        add(bar,control("▶ / ❚❚",()->{if(player==null)return;if(player.isPlaying())player.pause();else player.play();}));
        add(bar,control(openingEndMs>=0?"Pular abertura":"Pular abertura (+90 s)",()->{
            if(player==null)return;long now=player.getCurrentPosition();
            long to=openingEndMs>now?openingEndMs:now+90000;
            seekTo(to);
        }));
        add(bar,control("10 s ↷",()->seekBy(10000)));
        quality=control("Qualidade: Auto",this::showQuality);add(bar,quality);
        player=new ExoPlayer.Builder(this).build();view.setPlayer(player);
        player.addListener(new Player.Listener(){
            @Override public void onPlayerError(PlaybackException e){Toast.makeText(PlayerActivity.this,"Não foi possível reproduzir: "+e.getErrorCodeName(),Toast.LENGTH_LONG).show();}
            @Override public void onTracksChanged(Tracks tracks){quality.setEnabled(true);quality.setAlpha(1f);}
        });
        player.setMediaItem(MediaItem.fromUri(url));player.prepare();player.play();
    }
    private void add(LinearLayout row,TextView v){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,dp(46));p.rightMargin=dp(9);row.addView(v,p);}
    private void seekBy(long delta){if(player!=null)seekTo(player.getCurrentPosition()+delta);}
    private void seekTo(long pos){if(player==null)return;long duration=player.getDuration();if(duration>0)pos=Math.min(pos,duration);player.seekTo(Math.max(0,pos));}
    private void showQuality(){if(player==null)return;
        ArrayList<String> labels=new ArrayList<>();ArrayList<TrackSelectionOverride> overrides=new ArrayList<>();labels.add("Automática");overrides.add(null);
        Set<String> seen=new HashSet<>();
        for(Tracks.Group group:player.getCurrentTracks().getGroups()){
            if(group.getType()!=C.TRACK_TYPE_VIDEO)continue;
            for(int i=0;i<group.length;i++){
                if(!group.isTrackSupported(i))continue;Format f=group.getTrackFormat(i);
                String label=f.height>0?f.height+"p":(f.bitrate>0?(f.bitrate/1000)+" kbps":"Faixa de vídeo "+(i+1));
                if(seen.add(label)){labels.add(label);overrides.add(new TrackSelectionOverride(group.getMediaTrackGroup(),i));}
            }
        }
        if(labels.size()==1){new AlertDialog.Builder(this).setMessage("Este vídeo oferece apenas a qualidade original.").setPositiveButton("OK",null).show();return;}
        new AlertDialog.Builder(this).setTitle("Resolução do vídeo").setItems(labels.toArray(new String[0]),(d,which)->{
            androidx.media3.common.TrackSelectionParameters.Builder params=player.getTrackSelectionParameters().buildUpon();
            params.clearOverridesOfType(C.TRACK_TYPE_VIDEO);
            if(which>0)params.setOverrideForType(overrides.get(which));
            player.setTrackSelectionParameters(params.build());quality.setText("Qualidade: "+labels.get(which));
        }).show();
    }
    @Override public boolean onKeyDown(int key,KeyEvent event){
        if(key==KeyEvent.KEYCODE_MEDIA_REWIND){seekBy(-10000);return true;}
        if(key==KeyEvent.KEYCODE_MEDIA_FAST_FORWARD){seekBy(10000);return true;}
        return super.onKeyDown(key,event);
    }
    @Override protected void onStop(){super.onStop();if(player!=null){player.release();player=null;}}
}

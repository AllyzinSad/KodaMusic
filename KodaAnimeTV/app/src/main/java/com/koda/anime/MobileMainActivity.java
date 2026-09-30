package com.koda.anime;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.*;

public final class MobileMainActivity extends AppCompatActivity {
    private static final int BG=0xFF08080A,SURFACE=0xFF141419,RED=0xFFFF1635,WHITE=0xFFFFFFFF,MUTED=0xFFAAAAAF;
    private FrameLayout root; private LinearLayout content,bottom; private String page="Início"; private int generation;
    private Anime selectedAnime;

    private int dp(float v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private TextView text(String s,int sp,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(color);if(bold)t.setTypeface(null,Typeface.BOLD);return t;}
    private GradientDrawable rounded(int color,float radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
    private GradientDrawable stroke(int fill,int line,float radius,float width){GradientDrawable g=rounded(fill,radius);g.setStroke(dp(width),line);return g;}

    @Override public void onCreate(Bundle b){super.onCreate(b);Net.initialize(this);showHome();}
    @Override protected void onResume(){super.onResume();if(page.equals("Downloads"))showDownloads();}

    private void shell(String target){
        page=target;generation++;
        root=new FrameLayout(this);root.setBackgroundColor(BG);

        LinearLayout vertical=new LinearLayout(this);vertical.setOrientation(LinearLayout.VERTICAL);root.addView(vertical,new FrameLayout.LayoutParams(-1,-1));
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);vertical.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(16),dp(18),dp(16),dp(24));scroll.addView(content);

        bottom=new LinearLayout(this);bottom.setGravity(Gravity.CENTER);bottom.setPadding(dp(6),dp(6),dp(6),dp(6));bottom.setBackgroundColor(0xFF0E0E12);
        vertical.addView(bottom,new LinearLayout.LayoutParams(-1,dp(64)));
        nav("Início","⌂",this::showHome);nav("Pesquisa","⌕",this::showSearch);nav("Downloads","↓",this::showDownloads);nav("Favoritos","♥",this::showFavorites);nav("Histórico","◷",this::showHistory);

        KodaDecorView decor=new KodaDecorView(this);decor.setAlpha(.20f);root.addView(decor,new FrameLayout.LayoutParams(-1,-1));
        setContentView(root);
    }

    private void nav(String name,String icon,Runnable action){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setGravity(Gravity.CENTER);box.setClickable(true);box.setFocusable(true);
        TextView i=text(icon,20,page.equals(name)?RED:MUTED,true);i.setGravity(Gravity.CENTER);
        TextView l=text(name,10,page.equals(name)?WHITE:MUTED,page.equals(name));l.setGravity(Gravity.CENTER);
        box.addView(i,new LinearLayout.LayoutParams(-1,dp(28)));box.addView(l,new LinearLayout.LayoutParams(-1,dp(18)));
        box.setOnClickListener(v->action.run());
        bottom.addView(box,new LinearLayout.LayoutParams(0,-1,1));
    }

    private void title(String eyebrow,String name){
        TextView e=text(eyebrow,11,RED,true);e.setLetterSpacing(.14f);content.addView(e);
        TextView t=text(name,30,WHITE,true);t.setTypeface(Typeface.SERIF,Typeface.BOLD);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(14);content.addView(t,lp);
    }

    private TextView action(String label,boolean primary,Runnable run){
        TextView b=text(label,14,WHITE,true);b.setGravity(Gravity.CENTER);b.setClickable(true);b.setFocusable(true);b.setPadding(dp(14),0,dp(14),0);
        b.setBackground(primary?rounded(RED,10):stroke(0xFF202027,0xFF44444E,10,1));b.setOnClickListener(v->run.run());return b;
    }

    private View animeCard(Anime a){
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setClickable(true);card.setFocusable(true);card.setBackground(rounded(SURFACE,12));
        ImageView img=new ImageView(this);img.setScaleType(ImageView.ScaleType.CENTER_CROP);img.setBackgroundColor(0xFF202027);card.addView(img,new LinearLayout.LayoutParams(dp(126),dp(178)));Net.image(a.poster,img);
        TextView name=text(a.title,13,WHITE,true);name.setMaxLines(2);name.setEllipsize(android.text.TextUtils.TruncateAt.END);name.setPadding(dp(8),dp(7),dp(8),dp(8));card.addView(name,new LinearLayout.LayoutParams(dp(126),dp(52)));
        card.setOnClickListener(v->showDetails(a));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(126),dp(230));lp.rightMargin=dp(12);card.setLayoutParams(lp);return card;
    }

    private LinearLayout horizontalRail(){
        HorizontalScrollView hsv=new HorizontalScrollView(this);hsv.setHorizontalScrollBarEnabled(false);LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);hsv.addView(row);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(244));lp.bottomMargin=dp(18);content.addView(hsv,lp);return row;
    }

    private void showHome(){
        shell("Início");title("KODA ANIME","O que vamos assistir?");
        TextView sub=text("Lançamentos",19,WHITE,true);content.addView(sub);LinearLayout recent=horizontalRail();
        TextView recTitle=text("Recomendados",19,WHITE,true);content.addView(recTitle);LinearLayout rec=horizontalRail();
        final int g=generation;
        Catalog.recent((list,error)->{if(g!=generation)return;for(Anime a:list)recent.addView(animeCard(a));});
        Catalog.recommendations((list,error)->{if(g!=generation)return;for(Anime a:list)rec.addView(animeCard(a));});
    }

    private void showSearch(){
        shell("Pesquisa");title("PESQUISAR","Encontre um anime");
        EditText q=new EditText(this);q.setSingleLine(true);q.setHint("Digite o nome do anime");q.setTextColor(WHITE);q.setHintTextColor(MUTED);q.setTextSize(16);q.setBackground(stroke(0xFF16161C,0xFF3E3E48,11,1));q.setPadding(dp(14),0,dp(14),0);
        content.addView(q,new LinearLayout.LayoutParams(-1,dp(54)));
        LinearLayout results=new LinearLayout(this);results.setOrientation(LinearLayout.VERTICAL);LinearLayout.LayoutParams rlp=new LinearLayout.LayoutParams(-1,-2);rlp.topMargin=dp(14);content.addView(results,rlp);
        q.setOnEditorActionListener((v,action,event)->{searchNow(q.getText().toString(),results);return true;});
        q.setOnFocusChangeListener((v,f)->{if(!f&&!q.getText().toString().trim().isEmpty())searchNow(q.getText().toString(),results);});
        q.requestFocus();q.postDelayed(()->((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(q,InputMethodManager.SHOW_IMPLICIT),200);
    }

    private void searchNow(String query,LinearLayout results){
        results.removeAllViews();if(query.trim().isEmpty())return;
        results.addView(text("Buscando…",14,MUTED,false));
        Catalog.search(query,"",(list,error)->{results.removeAllViews();for(Anime a:list)results.addView(searchRow(a));if(list.isEmpty())results.addView(text(error==null?"Nada encontrado":error,14,MUTED,false));});
    }

    private View searchRow(Anime a){
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,dp(8),0,dp(8));row.setClickable(true);
        ImageView img=new ImageView(this);img.setScaleType(ImageView.ScaleType.CENTER_CROP);row.addView(img,new LinearLayout.LayoutParams(dp(64),dp(88)));Net.image(a.poster,img);
        LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.setPadding(dp(12),0,0,0);row.addView(info,new LinearLayout.LayoutParams(0,-2,1));
        info.addView(text(a.title,15,WHITE,true));TextView d=text(a.description,12,MUTED,false);d.setMaxLines(2);info.addView(d);
        row.setOnClickListener(v->showDetails(a));return row;
    }

    private void showDetails(Anime a){
        selectedAnime=a;shell("Detalhes");
        ImageView poster=new ImageView(this);poster.setScaleType(ImageView.ScaleType.CENTER_CROP);content.addView(poster,new LinearLayout.LayoutParams(-1,dp(310)));Net.image(a.poster,poster);
        TextView t=text(a.title,28,WHITE,true);t.setTypeface(Typeface.SERIF,Typeface.BOLD);LinearLayout.LayoutParams tlp=new LinearLayout.LayoutParams(-1,-2);tlp.topMargin=dp(14);content.addView(t,tlp);
        TextView d=text(a.description.isEmpty()?"Selecione um episódio para assistir.":a.description,14,MUTED,false);LinearLayout.LayoutParams dlp=new LinearLayout.LayoutParams(-1,-2);dlp.topMargin=dp(8);content.addView(d,dlp);

        LinearLayout buttons=new LinearLayout(this);LinearLayout.LayoutParams blp=new LinearLayout.LayoutParams(-1,dp(50));blp.topMargin=dp(12);content.addView(buttons,blp);
        TextView fav=action(isFavorite(a)?"♥ Favorito":"♡ Favoritar",false,()->{toggleFavorite(a);showDetails(a);});buttons.addView(fav,new LinearLayout.LayoutParams(0,-1,1));
        View gap=new View(this);buttons.addView(gap,new LinearLayout.LayoutParams(dp(10),1));
        TextView first=action("▶ Assistir",true,()->playFirst(a));buttons.addView(first,new LinearLayout.LayoutParams(0,-1,1));

        TextView epsTitle=text("Episódios",20,WHITE,true);LinearLayout.LayoutParams elp=new LinearLayout.LayoutParams(-1,-2);elp.topMargin=dp(20);content.addView(epsTitle,elp);
        LinearLayout controls=new LinearLayout(this);controls.setGravity(Gravity.CENTER_VERTICAL);content.addView(controls,new LinearLayout.LayoutParams(-1,dp(50)));
        TextView jump=action("Ir para episódio",false,()->{});controls.addView(jump,new LinearLayout.LayoutParams(0,dp(42),1));
        LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);content.addView(list);
        list.addView(text("Carregando episódios…",14,MUTED,false));

        Catalog.episodes(a,(eps,error)->{
            list.removeAllViews();
            if(eps.isEmpty()){list.addView(text(error==null?"Nenhum episódio disponível":error,14,MUTED,false));return;}
            jump.setOnClickListener(v->jumpDialog(a,eps,list));
            renderEpisodeRange(a,eps,list,0);
        });
    }

    private void jumpDialog(Anime a,List<Anime> eps,LinearLayout list){
        EditText input=new EditText(this);input.setInputType(InputType.TYPE_CLASS_NUMBER);input.setHint("Ex.: 742");
        new AlertDialog.Builder(this).setTitle("Ir para episódio").setView(input).setNegativeButton("Cancelar",null).setPositiveButton("Ir",(d,w)->{
            int n;try{n=Integer.parseInt(input.getText().toString());}catch(Exception e){n=1;}
            int idx=Math.max(0,n-1);for(int i=0;i<eps.size();i++)if(Catalog.episodeNumber(eps.get(i).description)==n){idx=i;break;}
            renderEpisodeRange(a,eps,list,(idx/50)*50);
        }).show();
    }

    private void renderEpisodeRange(Anime a,List<Anime> eps,LinearLayout list,int start){
        list.removeAllViews();int end=Math.min(start+50,eps.size());
        if(eps.size()>50){
            TextView range=text("Episódios "+(start+1)+"–"+end+" de "+eps.size(),13,RED,true);LinearLayout.LayoutParams r=new LinearLayout.LayoutParams(-1,-2);r.bottomMargin=dp(8);list.addView(range,r);
        }
        for(int i=start;i<end;i++){
            final int index=i;Anime ep=eps.get(i);WatchHistory.Entry hist=WatchHistory.find(this,historyId(a),ep.description);
            LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(12),dp(9),dp(8),dp(9));row.setBackground(rounded(SURFACE,10));
            LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);row.addView(info,new LinearLayout.LayoutParams(0,-2,1));
            int number=Catalog.episodeNumber(ep.description);if(number<=0)number=i+1;
            info.addView(text("Episódio "+number,15,WHITE,true));
            info.addView(text(hist==null?"Não assistido":hist.watched()?"✓ Assistido":"Continuar",12,hist!=null&&hist.watched()?0xFF66D38A:MUTED,false));
            TextView dl=action("↓",false,()->AnimeDownloads.enqueue(this,a,ep));row.addView(dl,new LinearLayout.LayoutParams(dp(48),dp(42)));
            View gap=new View(this);row.addView(gap,new LinearLayout.LayoutParams(dp(8),1));
            TextView play=action("▶",true,()->playEpisode(a,eps,index));row.addView(play,new LinearLayout.LayoutParams(dp(48),dp(42)));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(74));lp.bottomMargin=dp(8);list.addView(row,lp);
        }
        if(start>0){TextView prev=action("← "+Math.max(1,start-49)+"–"+start,false,()->renderEpisodeRange(a,eps,list,Math.max(0,start-50)));list.addView(prev,new LinearLayout.LayoutParams(-1,dp(44)));}
        if(end<eps.size()){TextView next=action((end+1)+"–"+Math.min(end+50,eps.size())+" →",false,()->renderEpisodeRange(a,eps,list,end));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(44));lp.topMargin=dp(8);list.addView(next,lp);}
    }

    private void playFirst(Anime a){Catalog.episodes(a,(eps,error)->{if(eps.isEmpty())return;playEpisode(a,eps,0);});}
    private void playEpisode(Anime a,List<Anime> eps,int index){
        Anime ep=eps.get(index);WatchHistory.Entry h=WatchHistory.find(this,historyId(a),ep.description);
        Intent i=new Intent(this,PlayerActivity.class);i.putExtra("url",ep.playUrl);i.putExtra("title",a.title);i.putExtra("animeId",historyId(a));i.putExtra("poster",a.poster);i.putExtra("episode",ep.description);
        i.putExtra("startPosition",h==null||h.watched()?0:h.position);if(index+1<eps.size()){i.putExtra("nextUrl",eps.get(index+1).playUrl);i.putExtra("nextLabel",eps.get(index+1).description);}startActivity(i);
    }

    private String historyId(Anime a){return Catalog.normalize(a.title.replaceAll("(?i)\\s*\\(dublado\\)\\s*$",""));}
    private android.content.SharedPreferences favs(){return getSharedPreferences("mobile_favorites",0);}
    private boolean isFavorite(Anime a){return favs().contains(historyId(a));}
    private void toggleFavorite(Anime a){String id=historyId(a);if(isFavorite(a))favs().edit().remove(id).apply();else favs().edit().putString(id,a.id+"\n"+a.title+"\n"+a.poster).apply();}

    private void showFavorites(){
        shell("Favoritos");title("SUA LISTA","Favoritos");
        Map<String,?> all=favs().getAll();if(all.isEmpty()){content.addView(text("Você ainda não adicionou favoritos.",14,MUTED,false));return;}
        for(Object raw:all.values()){String[] p=String.valueOf(raw).split("\n",-1);if(p.length<3)continue;Anime a=new Anime(p[0],p[1],p[2],"","",0);content.addView(searchRow(a));}
    }

    private void showHistory(){
        shell("Histórico");title("CONTINUAR ASSISTINDO","Histórico");
        List<WatchHistory.Entry> entries=WatchHistory.all(this);if(entries.isEmpty()){content.addView(text("Nenhum episódio assistido ainda.",14,MUTED,false));return;}
        for(WatchHistory.Entry e:entries){
            LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(12),dp(10),dp(12),dp(10));row.setBackground(rounded(SURFACE,10));
            LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);row.addView(info,new LinearLayout.LayoutParams(0,-2,1));info.addView(text(e.title,15,WHITE,true));info.addView(text(e.episode,12,MUTED,false));
            TextView play=action("▶",true,()->{Intent i=new Intent(this,PlayerActivity.class);i.putExtra("url",e.url);i.putExtra("title",e.title);i.putExtra("animeId",e.animeId);i.putExtra("poster",e.poster);i.putExtra("episode",e.episode);i.putExtra("startPosition",e.watched()?0:e.position);startActivity(i);});row.addView(play,new LinearLayout.LayoutParams(dp(48),dp(42)));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(72));lp.bottomMargin=dp(8);content.addView(row,lp);
        }
    }

    private void showDownloads(){
        shell("Downloads");title("OFFLINE","Downloads");
        List<AnimeDownloads.Item> items=AnimeDownloads.items(this);
        if(items.isEmpty()){content.addView(text("Seus episódios baixados aparecerão aqui.",14,MUTED,false));return;}
        for(AnimeDownloads.Item item:items){
            LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(12),dp(10),dp(12),dp(10));row.setBackground(rounded(SURFACE,10));
            LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);row.addView(info,new LinearLayout.LayoutParams(0,-2,1));info.addView(text(item.title,15,WHITE,true));info.addView(text(item.episode+" · "+AnimeDownloads.statusLabel(this,item.id),12,MUTED,false));
            TextView del=action("Excluir",false,()->{AnimeDownloads.remove(this,item.id);showDownloads();});row.addView(del,new LinearLayout.LayoutParams(dp(82),dp(42)));
            View gap=new View(this);row.addView(gap,new LinearLayout.LayoutParams(dp(8),1));
            TextView play=action("▶",true,()->{Intent i=new Intent(this,PlayerActivity.class);i.putExtra("url",item.url);i.putExtra("title",item.title);i.putExtra("animeId",item.animeId);i.putExtra("episode",item.episode);startActivity(i);});row.addView(play,new LinearLayout.LayoutParams(dp(48),dp(42)));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(76));lp.bottomMargin=dp(8);content.addView(row,lp);
        }
    }

    @Override public void onBackPressed(){if(page.equals("Detalhes"))showHome();else if(!page.equals("Início"))showHome();else super.onBackPressed();}
}

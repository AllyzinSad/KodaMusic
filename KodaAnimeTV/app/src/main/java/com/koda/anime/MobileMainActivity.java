package com.koda.anime;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.*;

public final class MobileMainActivity extends AppCompatActivity {
    private static final int BG=0xFF070709,SURFACE=0xFF121217,SURFACE2=0xFF1A1A20,RED=0xFFFF1635,WHITE=0xFFFFFFFF,MUTED=0xFFA7A7B0;
    private FrameLayout root; private LinearLayout content,bottom; private String page="Início"; private int generation;
    private Anime selectedAnime;

    private int dp(float v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private TextView text(String s,int sp,int color,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(sp);t.setTextColor(color);if(bold)t.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));return t;}
    private GradientDrawable rounded(int color,float radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
    private GradientDrawable stroke(int fill,int line,float radius,float width){GradientDrawable g=rounded(fill,radius);g.setStroke(dp(width),line);return g;}
    private void gap(LinearLayout p,int h){p.addView(new Space(this),new LinearLayout.LayoutParams(1,dp(h)));}

    @Override public void onCreate(Bundle b){super.onCreate(b);Net.initialize(this);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);showHome();}
    @Override protected void onResume(){super.onResume();if(page.equals("Downloads"))showDownloads();}

    private void shell(String target){
        page=target;generation++;
        root=new FrameLayout(this);root.setBackgroundColor(BG);

        LinearLayout vertical=new LinearLayout(this);vertical.setOrientation(LinearLayout.VERTICAL);root.addView(vertical,new FrameLayout.LayoutParams(-1,-1));
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setClipToPadding(false);scroll.setVerticalScrollBarEnabled(false);
        vertical.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(18),dp(10),dp(18),dp(28));scroll.addView(content);

        bottom=new LinearLayout(this);bottom.setGravity(Gravity.CENTER);bottom.setPadding(dp(4),dp(4),dp(4),dp(4));bottom.setBackgroundColor(0xFF0D0D12);
        vertical.addView(bottom,new LinearLayout.LayoutParams(-1,dp(68)));
        nav("Início","⌂",this::showHome);nav("Pesquisa","⌕",this::showSearch);nav("Downloads","↓",this::showDownloads);nav("Favoritos","♥",this::showFavorites);nav("Histórico","◷",this::showHistory);

        root.setOnApplyWindowInsetsListener((v,insets)->{
            int top,bottomInset;
            if(android.os.Build.VERSION.SDK_INT>=30){
                android.graphics.Insets sys=insets.getInsets(WindowInsets.Type.systemBars());
                top=sys.top;bottomInset=sys.bottom;
            }else{
                top=insets.getSystemWindowInsetTop();bottomInset=insets.getSystemWindowInsetBottom();
            }
            vertical.setPadding(0,top,0,bottomInset);
            return insets;
        });

        KodaDecorView decor=new KodaDecorView(this);decor.setAlpha(.12f);decor.setClickable(false);decor.setFocusable(false);root.addView(decor,new FrameLayout.LayoutParams(-1,-1));
        setContentView(root);root.requestApplyInsets();
    }

    private void nav(String name,String icon,Runnable action){
        boolean active=page.equals(name);
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setGravity(Gravity.CENTER);box.setClickable(true);box.setFocusable(true);
        TextView i=text(icon,21,active?RED:0xFF8E8E98,true);i.setGravity(Gravity.CENTER);
        TextView l=text(name,10,active?WHITE:0xFF8E8E98,active);l.setGravity(Gravity.CENTER);
        box.addView(i,new LinearLayout.LayoutParams(-1,dp(30)));box.addView(l,new LinearLayout.LayoutParams(-1,dp(19)));
        if(active){View mark=new View(this);mark.setBackgroundColor(RED);LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(dp(22),dp(2));mp.topMargin=dp(2);box.addView(mark,mp);}
        box.setOnClickListener(v->action.run());bottom.addView(box,new LinearLayout.LayoutParams(0,-1,1));
    }

    private void brand(){
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);
        ImageView logo=new ImageView(this);logo.setImageResource(R.mipmap.ic_launcher);logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);row.addView(logo,new LinearLayout.LayoutParams(dp(42),dp(42)));
        LinearLayout words=new LinearLayout(this);words.setOrientation(LinearLayout.VERTICAL);words.setPadding(dp(10),0,0,0);row.addView(words,new LinearLayout.LayoutParams(0,dp(44),1));
        TextView k=text("KODA",20,WHITE,true);k.setLetterSpacing(.08f);words.addView(k);
        TextView a=text("ANIME",10,RED,true);a.setLetterSpacing(.28f);words.addView(a);
        TextView alpha=text("ALPHA",9,0xFF686873,true);alpha.setGravity(Gravity.CENTER);alpha.setBackground(rounded(0xFF17171D,20));row.addView(alpha,new LinearLayout.LayoutParams(dp(54),dp(26)));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(48));lp.bottomMargin=dp(14);content.addView(row,lp);
    }

    private void pageTitle(String eyebrow,String name,String subtitle){
        TextView e=text(eyebrow,10,RED,true);e.setLetterSpacing(.18f);content.addView(e);
        TextView t=text(name,29,WHITE,true);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(2);content.addView(t,lp);
        if(subtitle!=null&&!subtitle.isEmpty()){TextView s=text(subtitle,13,MUTED,false);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);sp.topMargin=dp(4);sp.bottomMargin=dp(15);content.addView(s,sp);}
        else gap(content,12);
    }

    private TextView section(String name){
        TextView t=text(name,19,WHITE,true);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(7);lp.bottomMargin=dp(10);content.addView(t,lp);return t;
    }

    private TextView action(String label,boolean primary,Runnable run){
        TextView b=text(label,14,WHITE,true);b.setGravity(Gravity.CENTER);b.setClickable(true);b.setFocusable(true);b.setPadding(dp(14),0,dp(14),0);
        b.setBackground(primary?new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{0xFFFF1635,0xFFD6002A}):stroke(0xEB1B1B21,0xFF3A3A44,11,1));
        ((GradientDrawable)b.getBackground()).setCornerRadius(dp(11));b.setOnClickListener(v->run.run());return b;
    }

    private View animeCard(Anime a){
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setClickable(true);card.setFocusable(true);card.setBackground(rounded(SURFACE,13));card.setClipToOutline(true);
        FrameLayout art=new FrameLayout(this);card.addView(art,new LinearLayout.LayoutParams(dp(138),dp(184)));
        ImageView img=new ImageView(this);img.setScaleType(ImageView.ScaleType.CENTER_CROP);img.setBackgroundColor(0xFF202027);art.addView(img,new FrameLayout.LayoutParams(-1,-1));loadPoster(a.poster,img);
        TextView badge=text(a.title.toLowerCase(Locale.ROOT).contains("dublado")||a.genre.toLowerCase(Locale.ROOT).contains("dubl")?"DUB":"LEG",9,WHITE,true);badge.setGravity(Gravity.CENTER);badge.setBackground(rounded(0xE0C90025,20));
        FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(dp(42),dp(24),Gravity.TOP|Gravity.RIGHT);bp.topMargin=dp(8);bp.rightMargin=dp(8);art.addView(badge,bp);
        LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.setPadding(dp(9),dp(8),dp(8),dp(8));card.addView(info,new LinearLayout.LayoutParams(dp(138),dp(62)));
        TextView name=text(a.title,13,WHITE,true);name.setMaxLines(2);name.setEllipsize(android.text.TextUtils.TruncateAt.END);info.addView(name);
        if(a.description!=null&&!a.description.isEmpty()){TextView ep=text(a.description,10,MUTED,false);ep.setSingleLine(true);ep.setEllipsize(android.text.TextUtils.TruncateAt.END);info.addView(ep);}
        card.setOnClickListener(v->showDetails(a));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(138),dp(246));lp.rightMargin=dp(12);card.setLayoutParams(lp);return card;
    }

    private LinearLayout horizontalRail(int height){
        HorizontalScrollView hsv=new HorizontalScrollView(this);hsv.setHorizontalScrollBarEnabled(false);hsv.setClipToPadding(false);
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);hsv.addView(row);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(height));lp.bottomMargin=dp(18);content.addView(hsv,lp);return row;
    }

    private void loadPoster(String url,ImageView image){
        image.setImageDrawable(null);image.setBackgroundColor(0xFF202027);
        if(url!=null&&url.startsWith("https://"))Net.image(url,image);
    }

    private FrameLayout heroShell(){
        FrameLayout hero=new FrameLayout(this);hero.setBackground(rounded(SURFACE,16));hero.setClipToOutline(true);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(220));lp.bottomMargin=dp(18);content.addView(hero,lp);
        TextView loading=text("Carregando destaque…",14,MUTED,false);loading.setGravity(Gravity.CENTER);hero.addView(loading,new FrameLayout.LayoutParams(-1,-1));return hero;
    }

    private void renderHero(FrameLayout hero,Anime a){
        hero.removeAllViews();
        ImageView art=new ImageView(this);art.setScaleType(ImageView.ScaleType.CENTER_CROP);hero.addView(art,new FrameLayout.LayoutParams(-1,-1));loadPoster(a.poster,art);
        View shade=new View(this);shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,new int[]{0xF3070709,0xA8070709,0x10070709}));hero.addView(shade,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.setPadding(dp(16),0,dp(16),dp(14));
        FrameLayout.LayoutParams ip=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);hero.addView(info,ip);
        TextView label=text("EM DESTAQUE",9,RED,true);label.setLetterSpacing(.18f);info.addView(label);
        TextView title=text(a.title,24,WHITE,true);title.setMaxLines(2);info.addView(title);
        LinearLayout actions=new LinearLayout(this);LinearLayout.LayoutParams alp=new LinearLayout.LayoutParams(-1,dp(42));alp.topMargin=dp(9);info.addView(actions,alp);
        TextView watch=action("▶ Assistir",true,()->playFirst(a));actions.addView(watch,new LinearLayout.LayoutParams(dp(128),-1));
        View g=new View(this);actions.addView(g,new LinearLayout.LayoutParams(dp(9),1));
        TextView details=action("Detalhes",false,()->showDetails(a));actions.addView(details,new LinearLayout.LayoutParams(dp(110),-1));
    }

    private void showHome(){
        shell("Início");brand();
        TextView welcome=text("Descubra seu próximo anime",25,WHITE,true);LinearLayout.LayoutParams wlp=new LinearLayout.LayoutParams(-1,-2);wlp.bottomMargin=dp(12);content.addView(welcome,wlp);
        FrameLayout hero=heroShell();

        List<WatchHistory.Entry> history=WatchHistory.all(this);
        if(!history.isEmpty()){
            section("Continuar assistindo");
            LinearLayout continueRow=horizontalRail(104);
            int shown=0;HashSet<String> keys=new HashSet<>();
            for(WatchHistory.Entry e:history){
                if(e.watched()||!keys.add(e.animeId)||shown++>=8)continue;
                continueRow.addView(historyCard(e));
            }
        }

        section("Lançamentos recentes");LinearLayout recent=horizontalRail(258);
        TextView recentLoading=text("Carregando lançamentos…",13,MUTED,false);recent.addView(recentLoading,new LinearLayout.LayoutParams(-2,dp(40)));
        section("Em alta");LinearLayout recommended=horizontalRail(258);
        TextView recLoading=text("Carregando recomendações…",13,MUTED,false);recommended.addView(recLoading,new LinearLayout.LayoutParams(-2,dp(40)));
        section("Dublados para você");LinearLayout dubbed=horizontalRail(258);

        final int g=generation;
        Catalog.recommendations((list,error)->{
            if(g!=generation)return;recommended.removeAllViews();
            List<Anime> grouped=Catalog.collapseFranchises(list);
            if(!grouped.isEmpty())renderHero(hero,grouped.get(0));
            for(Anime a:grouped)recommended.addView(animeCard(a));
            if(list.isEmpty())recommended.addView(text(error==null?"Nada disponível agora.":error,13,MUTED,false));
        });
        Catalog.recent((list,error)->{
            if(g!=generation)return;recent.removeAllViews();
            for(Anime a:list)recent.addView(animeCard(a));
            if(list.isEmpty())recent.addView(text(error==null?"Nenhum lançamento disponível.":error,13,MUTED,false));
        });
        Catalog.page("","",0,80,(list,error)->{
            if(g!=generation)return;dubbed.removeAllViews();int added=0;
            for(Anime a:list)if(a.title.toLowerCase(Locale.ROOT).contains("dublado")&&added++<16)dubbed.addView(animeCard(a));
            if(added==0)dubbed.addView(text("Nenhum título dublado disponível agora.",13,MUTED,false));
        });
    }

    private View historyCard(WatchHistory.Entry e){
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(10),dp(8),dp(10),dp(8));row.setBackground(rounded(SURFACE,12));row.setClickable(true);
        ImageView img=new ImageView(this);img.setScaleType(ImageView.ScaleType.CENTER_CROP);row.addView(img,new LinearLayout.LayoutParams(dp(58),dp(78)));loadPoster(e.poster,img);
        LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.setPadding(dp(10),0,0,0);row.addView(info,new LinearLayout.LayoutParams(dp(160),-2));
        TextView t=text(e.title,13,WHITE,true);t.setMaxLines(2);info.addView(t);info.addView(text(e.episode+" · Continuar",11,MUTED,false));
        row.setOnClickListener(v->{Intent i=new Intent(this,PlayerActivity.class);i.putExtra("url",e.url);i.putExtra("title",e.title);i.putExtra("animeId",e.animeId);i.putExtra("poster",e.poster);i.putExtra("episode",e.episode);i.putExtra("startPosition",e.position);startActivity(i);});
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(238),dp(94));lp.rightMargin=dp(10);row.setLayoutParams(lp);return row;
    }

    private void showSearch(){
        shell("Pesquisa");brand();pageTitle("PESQUISA","Encontre um anime","Busque por nome e abra diretamente a série que quiser.");
        LinearLayout box=new LinearLayout(this);box.setGravity(Gravity.CENTER_VERTICAL);box.setPadding(dp(14),0,dp(12),0);box.setBackground(stroke(0xFF15151B,0xFF34343F,13,1));
        TextView icon=text("⌕",23,MUTED,true);box.addView(icon,new LinearLayout.LayoutParams(dp(34),-1));
        EditText q=new EditText(this);q.setSingleLine(true);q.setHint("Nome do anime");q.setTextColor(WHITE);q.setHintTextColor(0xFF777781);q.setTextSize(16);q.setBackgroundColor(Color.TRANSPARENT);box.addView(q,new LinearLayout.LayoutParams(0,dp(56),1));
        content.addView(box,new LinearLayout.LayoutParams(-1,dp(56)));
        LinearLayout results=new LinearLayout(this);results.setOrientation(LinearLayout.VERTICAL);LinearLayout.LayoutParams rlp=new LinearLayout.LayoutParams(-1,-2);rlp.topMargin=dp(14);content.addView(results,rlp);
        q.setOnEditorActionListener((v,action,event)->{searchNow(q.getText().toString(),results);return true;});
        q.setOnFocusChangeListener((v,f)->{if(!f&&!q.getText().toString().trim().isEmpty())searchNow(q.getText().toString(),results);});
    }

    private void searchNow(String query,LinearLayout results){
        results.removeAllViews();if(query.trim().isEmpty())return;results.addView(text("Buscando…",14,MUTED,false));
        Catalog.search(query,"",(list,error)->{
            results.removeAllViews();List<Anime> grouped=Catalog.collapseFranchises(list);
            for(Anime a:grouped)results.addView(searchRow(a));
            if(grouped.isEmpty())emptyInto(results,"⌕","Nada encontrado","Tente outro nome ou uma parte do título.");
        });
    }

    private View searchRow(Anime a){
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(10),dp(9),dp(10),dp(9));row.setClickable(true);row.setBackground(rounded(SURFACE,12));
        ImageView img=new ImageView(this);img.setScaleType(ImageView.ScaleType.CENTER_CROP);row.addView(img,new LinearLayout.LayoutParams(dp(66),dp(92)));loadPoster(a.poster,img);
        LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.setPadding(dp(12),0,0,0);row.addView(info,new LinearLayout.LayoutParams(0,-2,1));
        info.addView(text(Catalog.seriesTitle(a.title),15,WHITE,true));TextView d=text(a.description,12,MUTED,false);d.setMaxLines(2);info.addView(d);
        TextView chevron=text("›",26,MUTED,false);chevron.setGravity(Gravity.CENTER);row.addView(chevron,new LinearLayout.LayoutParams(dp(32),-1));
        row.setOnClickListener(v->showDetails(a));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(110));lp.bottomMargin=dp(9);row.setLayoutParams(lp);return row;
    }

    private void showDetails(Anime a){
        selectedAnime=a;shell("Detalhes");brand();
        FrameLayout hero=new FrameLayout(this);hero.setBackground(rounded(SURFACE,15));hero.setClipToOutline(true);content.addView(hero,new LinearLayout.LayoutParams(-1,dp(245)));
        ImageView poster=new ImageView(this);poster.setScaleType(ImageView.ScaleType.CENTER_CROP);hero.addView(poster,new FrameLayout.LayoutParams(-1,-1));loadPoster(a.poster,poster);
        View shade=new View(this);shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,new int[]{0xFA070709,0xB8070709,0x20070709}));hero.addView(shade,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout heroText=new LinearLayout(this);heroText.setOrientation(LinearLayout.VERTICAL);heroText.setPadding(dp(15),0,dp(15),dp(14));FrameLayout.LayoutParams htp=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);hero.addView(heroText,htp);
        TextView t=text(Catalog.seriesTitle(a.title),24,WHITE,true);t.setMaxLines(2);heroText.addView(t);
        TextView meta=text(Catalog.seasonLabel(a.title)+"  •  "+(a.title.toLowerCase(Locale.ROOT).contains("dublado")?"Dublado":"Legendado"),11,0xFFD0D0D6,false);heroText.addView(meta);

        TextView d=text(a.description.isEmpty()?"Selecione um episódio para assistir.":a.description,13,MUTED,false);d.setMaxLines(4);LinearLayout.LayoutParams dlp=new LinearLayout.LayoutParams(-1,-2);dlp.topMargin=dp(12);content.addView(d,dlp);
        LinearLayout buttons=new LinearLayout(this);LinearLayout.LayoutParams blp=new LinearLayout.LayoutParams(-1,dp(48));blp.topMargin=dp(12);content.addView(buttons,blp);
        TextView first=action("▶ Assistir",true,()->playFirst(a));buttons.addView(first,new LinearLayout.LayoutParams(0,-1,1));
        View g=new View(this);buttons.addView(g,new LinearLayout.LayoutParams(dp(9),1));
        TextView fav=action(isFavorite(a)?"♥ Favorito":"♡ Favoritar",false,()->{toggleFavorite(a);showDetails(a);});buttons.addView(fav,new LinearLayout.LayoutParams(0,-1,1));

        TextView seasonHeading=section("Temporadas");
        HorizontalScrollView seasonScroll=new HorizontalScrollView(this);seasonScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout seasonRow=new LinearLayout(this);seasonRow.setOrientation(LinearLayout.HORIZONTAL);seasonScroll.addView(seasonRow);
        content.addView(seasonScroll,new LinearLayout.LayoutParams(-1,dp(48)));

        TextView audioHeading=section("Versão");
        HorizontalScrollView audioScroll=new HorizontalScrollView(this);audioScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout audioRow=new LinearLayout(this);audioRow.setOrientation(LinearLayout.HORIZONTAL);audioScroll.addView(audioRow);
        content.addView(audioScroll,new LinearLayout.LayoutParams(-1,dp(48)));

        Catalog.related(a,(related,relatedError)->{
            if(!page.equals("Detalhes")||selectedAnime!=a)return;
            populateSeasonControls(a,related,seasonRow,audioRow);
            seasonHeading.setVisibility(seasonRow.getChildCount()>1?View.VISIBLE:View.GONE);
            seasonScroll.setVisibility(seasonRow.getChildCount()>1?View.VISIBLE:View.GONE);
            audioHeading.setVisibility(audioRow.getChildCount()>1?View.VISIBLE:View.GONE);
            audioScroll.setVisibility(audioRow.getChildCount()>1?View.VISIBLE:View.GONE);
        });

        section("Episódios");
        TextView jump=action("Ir para episódio",false,()->{});content.addView(jump,new LinearLayout.LayoutParams(-1,dp(44)));
        gap(content,10);
        LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);content.addView(list);
        list.addView(text("Carregando episódios…",14,MUTED,false));

        Catalog.episodes(a,(eps,error)->{
            list.removeAllViews();
            if(eps.isEmpty()){emptyInto(list,"!","Nenhum episódio disponível",error==null?"Tente novamente mais tarde.":error);return;}
            jump.setOnClickListener(v->jumpDialog(a,eps,list));
            int initial=0;for(WatchHistory.Entry e:WatchHistory.all(this))if(e.animeId.equals(historyId(a))){int n=Catalog.episodeNumber(e.episode);if(n>0)initial=((n-1)/50)*50;break;}
            renderEpisodeRange(a,eps,list,initial);
        });
    }

    private void populateSeasonControls(Anime current,List<Anime> related,LinearLayout seasonRow,LinearLayout audioRow){
        seasonRow.removeAllViews();audioRow.removeAllViews();
        ArrayList<Anime> ordered=new ArrayList<>(related);
        ordered.sort((x,y)->{
            int sx=Catalog.seasonNumber(x.title),sy=Catalog.seasonNumber(y.title);
            if(sx!=sy)return sx-sy;
            boolean xd=x.title.toLowerCase(Locale.ROOT).contains("dublado"),yd=y.title.toLowerCase(Locale.ROOT).contains("dublado");
            return xd==yd?x.title.compareToIgnoreCase(y.title):(xd?-1:1);
        });

        String selectedSeason=Catalog.seasonLabel(current.title);
        boolean selectedDub=current.title.toLowerCase(Locale.ROOT).contains("dublado");
        LinkedHashSet<String> seasons=new LinkedHashSet<>();
        for(Anime x:ordered)seasons.add(Catalog.seasonLabel(x.title));

        for(String season:seasons){
            Anime target=findVersion(ordered,season,selectedDub,true);
            if(target==null)continue;
            TextView chip=action(season,season.equals(selectedSeason),()->showDetails(target));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(126),dp(42));lp.rightMargin=dp(8);seasonRow.addView(chip,lp);
        }

        for(boolean wantDub:new boolean[]{true,false}){
            Anime target=findVersion(ordered,selectedSeason,wantDub,false);
            if(target==null)continue;
            String label=wantDub?"Dublado":"Legendado";
            TextView chip=action(label,wantDub==selectedDub,()->showDetails(target));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(126),dp(42));lp.rightMargin=dp(8);audioRow.addView(chip,lp);
        }
    }

    private Anime findVersion(List<Anime> related,String season,boolean dubbed,boolean fallback){
        for(Anime variant:related){
            boolean dub=variant.title.toLowerCase(Locale.ROOT).contains("dublado");
            if(Catalog.seasonLabel(variant.title).equals(season)&&dub==dubbed)return variant;
        }
        if(fallback)for(Anime variant:related)if(Catalog.seasonLabel(variant.title).equals(season))return variant;
        return null;
    }

    private void jumpDialog(Anime a,List<Anime> eps,LinearLayout list){
        EditText input=new EditText(this);input.setInputType(InputType.TYPE_CLASS_NUMBER);input.setHint("Ex.: 742");input.setTextColor(WHITE);input.setHintTextColor(MUTED);
        new AlertDialog.Builder(this).setTitle("Ir para episódio").setView(input).setNegativeButton("Cancelar",null).setPositiveButton("Ir",(d,w)->{
            int n;try{n=Integer.parseInt(input.getText().toString());}catch(Exception e){n=1;}
            int idx=Math.max(0,n-1);for(int i=0;i<eps.size();i++)if(Catalog.episodeNumber(eps.get(i).description)==n){idx=i;break;}
            renderEpisodeRange(a,eps,list,(idx/50)*50);
        }).show();
    }

    private void renderEpisodeRange(Anime a,List<Anime> eps,LinearLayout list,int start){
        list.removeAllViews();int safe=Math.max(0,Math.min(start,Math.max(0,eps.size()-1)));safe=(safe/50)*50;final int rangeStart=safe;int end=Math.min(rangeStart+50,eps.size());
        if(eps.size()>50){
            LinearLayout ranges=new LinearLayout(this);ranges.setGravity(Gravity.CENTER_VERTICAL);
            TextView prev=action("‹",false,()->renderEpisodeRange(a,eps,list,Math.max(0,rangeStart-50)));prev.setEnabled(rangeStart>0);ranges.addView(prev,new LinearLayout.LayoutParams(dp(46),dp(40)));
            TextView range=text((rangeStart+1)+"–"+end+" de "+eps.size(),13,WHITE,true);range.setGravity(Gravity.CENTER);ranges.addView(range,new LinearLayout.LayoutParams(0,dp(40),1));
            TextView next=action("›",false,()->renderEpisodeRange(a,eps,list,end));next.setEnabled(end<eps.size());ranges.addView(next,new LinearLayout.LayoutParams(dp(46),dp(40)));
            LinearLayout.LayoutParams rlp=new LinearLayout.LayoutParams(-1,dp(44));rlp.bottomMargin=dp(10);list.addView(ranges,rlp);
        }
        for(int i=rangeStart;i<end;i++){
            final int index=i;Anime ep=eps.get(i);WatchHistory.Entry hist=WatchHistory.find(this,historyId(a),ep.description);
            LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(12),dp(9),dp(8),dp(9));row.setBackground(rounded(SURFACE,11));
            LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);row.addView(info,new LinearLayout.LayoutParams(0,-2,1));
            int number=Catalog.episodeNumber(ep.description);if(number<=0)number=i+1;
            info.addView(text("Episódio "+number,15,WHITE,true));
            info.addView(text(hist==null?"Não assistido":hist.watched()?"✓ Assistido":"Continuar de onde parou",11,hist!=null&&hist.watched()?0xFF66D38A:MUTED,false));
            TextView dl=action("↓",false,()->AnimeDownloads.enqueue(this,a,ep));row.addView(dl,new LinearLayout.LayoutParams(dp(46),dp(42)));
            View gg=new View(this);row.addView(gg,new LinearLayout.LayoutParams(dp(7),1));
            TextView play=action("▶",true,()->playEpisode(a,eps,index));row.addView(play,new LinearLayout.LayoutParams(dp(46),dp(42)));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(74));lp.bottomMargin=dp(8);list.addView(row,lp);
        }
    }

    private void playFirst(Anime a){Catalog.episodes(a,(eps,error)->{if(eps.isEmpty()){Toast.makeText(this,"Nenhum episódio disponível.",Toast.LENGTH_SHORT).show();return;}playEpisode(a,eps,0);});}
    private void playEpisode(Anime a,List<Anime> eps,int index){
        Anime ep=eps.get(index);WatchHistory.Entry h=WatchHistory.find(this,historyId(a),ep.description);
        Intent i=new Intent(this,PlayerActivity.class);i.putExtra("url",ep.playUrl);i.putExtra("title",a.title);i.putExtra("animeId",historyId(a));i.putExtra("poster",a.poster);i.putExtra("episode",ep.description);
        i.putExtra("startPosition",h==null||h.watched()?0:h.position);if(index+1<eps.size()){i.putExtra("nextUrl",eps.get(index+1).playUrl);i.putExtra("nextLabel",eps.get(index+1).description);}startActivity(i);
    }

    private String historyId(Anime a){return Catalog.normalize(a.title.replaceAll("(?i)\\s*\\(dublado\\)\\s*$",""));}
    private android.content.SharedPreferences favs(){return getSharedPreferences("mobile_favorites",0);}
    private boolean isFavorite(Anime a){return favs().contains(historyId(a));}
    private void toggleFavorite(Anime a){String id=historyId(a);if(isFavorite(a))favs().edit().remove(id).apply();else favs().edit().putString(id,a.id+"\n"+a.title+"\n"+a.poster).apply();}

    private void emptyInto(LinearLayout parent,String icon,String title,String desc){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setGravity(Gravity.CENTER);box.setPadding(dp(20),dp(34),dp(20),dp(34));box.setBackground(rounded(0x66141419,14));
        TextView i=text(icon,34,RED,true);i.setGravity(Gravity.CENTER);box.addView(i,new LinearLayout.LayoutParams(-1,dp(48)));
        TextView t=text(title,17,WHITE,true);t.setGravity(Gravity.CENTER);box.addView(t);
        TextView d=text(desc,13,MUTED,false);d.setGravity(Gravity.CENTER);LinearLayout.LayoutParams dpv=new LinearLayout.LayoutParams(-1,-2);dpv.topMargin=dp(5);box.addView(d,dpv);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(18);parent.addView(box,lp);
    }

    private void showFavorites(){
        shell("Favoritos");brand();pageTitle("SUA LISTA","Favoritos","Tudo que você marcou para assistir depois.");
        Map<String,?> all=favs().getAll();if(all.isEmpty()){emptyInto(content,"♥","Sua lista está vazia","Toque em Favoritar na página de um anime.");return;}
        for(Object raw:all.values()){String[] p=String.valueOf(raw).split("\n",-1);if(p.length<3)continue;Anime a=new Anime(p[0],p[1],p[2],"","",0);content.addView(searchRow(a));}
    }

    private void showHistory(){
        shell("Histórico");brand();pageTitle("CONTINUAR","Histórico","Retome rapidamente de onde parou.");
        List<WatchHistory.Entry> entries=WatchHistory.all(this);if(entries.isEmpty()){emptyInto(content,"◷","Nada assistido ainda","Seu progresso vai aparecer aqui automaticamente.");return;}
        for(WatchHistory.Entry e:entries)content.addView(historyListRow(e));
    }

    private View historyListRow(WatchHistory.Entry e){
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(10),dp(9),dp(10),dp(9));row.setBackground(rounded(SURFACE,11));
        ImageView img=new ImageView(this);img.setScaleType(ImageView.ScaleType.CENTER_CROP);row.addView(img,new LinearLayout.LayoutParams(dp(58),dp(78)));loadPoster(e.poster,img);
        LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);info.setPadding(dp(10),0,0,0);row.addView(info,new LinearLayout.LayoutParams(0,-2,1));info.addView(text(e.title,14,WHITE,true));info.addView(text(e.episode+(e.watched()?" · Assistido":" · Continuar"),11,MUTED,false));
        TextView play=action("▶",true,()->{Intent i=new Intent(this,PlayerActivity.class);i.putExtra("url",e.url);i.putExtra("title",e.title);i.putExtra("animeId",e.animeId);i.putExtra("poster",e.poster);i.putExtra("episode",e.episode);i.putExtra("startPosition",e.watched()?0:e.position);startActivity(i);});row.addView(play,new LinearLayout.LayoutParams(dp(46),dp(42)));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(96));lp.bottomMargin=dp(8);row.setLayoutParams(lp);return row;
    }

    private void showDownloads(){
        shell("Downloads");brand();pageTitle("OFFLINE","Downloads","Baixe episódios e assista mesmo sem internet.");
        List<AnimeDownloads.Item> items=AnimeDownloads.items(this);
        if(items.isEmpty()){emptyInto(content,"↓","Nenhum download","Abra um anime e toque na seta ao lado do episódio.");return;}
        for(AnimeDownloads.Item item:items){
            LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(12),dp(10),dp(10),dp(10));row.setBackground(rounded(SURFACE,11));
            LinearLayout info=new LinearLayout(this);info.setOrientation(LinearLayout.VERTICAL);row.addView(info,new LinearLayout.LayoutParams(0,-2,1));TextView tt=text(item.title,14,WHITE,true);tt.setMaxLines(2);info.addView(tt);info.addView(text(item.episode+" · "+AnimeDownloads.statusLabel(this,item.id),11,MUTED,false));
            TextView del=action("Excluir",false,()->{AnimeDownloads.remove(this,item.id);showDownloads();});row.addView(del,new LinearLayout.LayoutParams(dp(78),dp(42)));
            View gg=new View(this);row.addView(gg,new LinearLayout.LayoutParams(dp(7),1));
            TextView play=action("▶",true,()->{Intent i=new Intent(this,PlayerActivity.class);i.putExtra("url",item.url);i.putExtra("title",item.title);i.putExtra("animeId",item.animeId);i.putExtra("episode",item.episode);startActivity(i);});row.addView(play,new LinearLayout.LayoutParams(dp(46),dp(42)));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(78));lp.bottomMargin=dp(8);content.addView(row,lp);
        }
    }

    @Override public void onBackPressed(){if(page.equals("Detalhes"))showHome();else if(!page.equals("Início"))showHome();else super.onBackPressed();}
}

package com.koda.anime;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.util.*;

public final class MainActivity extends Activity {
    private static final int BLACK=0xFF0A0A0D, SIDE=0xFF101013, RED=0xFFE7333E, WHITE=0xFFF7F7F9, MUTED=0xFFB9B9C2;
    private LinearLayout content,nav; private String page="Início"; private boolean playerOpened; private Anime selectedAnime; private String returnPage="Início",detailFocus=""; private int searchGeneration; private final Map<String,Anime> seen=new LinkedHashMap<>();
    private final ArrayList<TextView> navButtons=new ArrayList<>();
    @Override public void onCreate(Bundle state){super.onCreate(state);getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);showHome();}
    private float uiScale(){return getResources().getDisplayMetrics().widthPixels/(1000f*getResources().getDisplayMetrics().density);}
    private int dp(float v){return (int)(getResources().getDisplayMetrics().density*uiScale()*v+.5f);}
    private GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private TextView label(String text,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(text);t.setTextSize(size*uiScale());t.setTextColor(color);if(bold)t.setTypeface(null,Typeface.BOLD);return t;}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
    private void pad(View v,int a,int b,int c,int d){v.setPadding(dp(a),dp(b),dp(c),dp(d));}
    private void shell(String target){page=target;navButtons.clear();LinearLayout frame=new LinearLayout(this);frame.setBackground(new GradientDrawable(GradientDrawable.Orientation.TR_BL,new int[]{0xFF211015,BLACK,BLACK}));setContentView(frame);
        nav=column();nav.setBackgroundColor(SIDE);pad(nav,18,23,14,22);frame.addView(nav,new LinearLayout.LayoutParams(dp(190),-1));
        LinearLayout brand=new LinearLayout(this);brand.setGravity(Gravity.CENTER_VERTICAL);
        ImageView mark=new ImageView(this);mark.setImageResource(com.koda.anime.R.mipmap.ic_launcher);brand.addView(mark,new LinearLayout.LayoutParams(dp(36),dp(36)));
        TextView name=label("  Koda",19,WHITE,true);brand.addView(name);brand.addView(label(" Anime",19,RED,true));
        nav.addView(brand,new LinearLayout.LayoutParams(-1,dp(62)));
        navItem("Início","⌂",()->showHome());navItem("Pesquisa","⌕",()->showSearch());navItem("Favoritos","♡",()->showFavorites());navItem("Histórico","◷",()->showHistory());
        View spacer=new View(this);nav.addView(spacer,new LinearLayout.LayoutParams(1,0,1));
        navItem("Configurações","⚙",()->showSettings());
        ScrollView sc=new ScrollView(this);sc.setFillViewport(true);frame.addView(sc,new LinearLayout.LayoutParams(0,-1,1));
        content=column();pad(content,22,30,12,24);sc.addView(content);
    }
    private void navItem(String name,String icon,Runnable action){TextView v=label(name,14,page.equals(name)?WHITE:MUTED,page.equals(name));v.setGravity(Gravity.CENTER_VERTICAL);pad(v,13,0,6,0);
        NavIcon glyph=new NavIcon(name,page.equals(name)?WHITE:MUTED);glyph.setBounds(0,0,dp(21),dp(21));v.setCompoundDrawables(glyph,null,null,null);v.setCompoundDrawablePadding(dp(13));
        v.setBackground(page.equals(name)?new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{0xFFFF263D,0xFF9B142A}):bg(SIDE,8));if(v.getBackground() instanceof GradientDrawable)((GradientDrawable)v.getBackground()).setCornerRadius(dp(8));v.setFocusable(true);
        v.setOnFocusChangeListener((view,focus)->{v.setBackground(bg(focus?0xFFBF1B32:page.equals(name)?0xFFB7182D:SIDE,8));glyph.setTintColor(focus?WHITE:page.equals(name)?WHITE:MUTED);});
        v.setOnClickListener(x->action.run());LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(38));p.bottomMargin=dp(12);nav.addView(v,p);navButtons.add(v);
    }
    private GradientDrawable outline(int fill,int stroke,int radius,int width){GradientDrawable d=bg(fill,radius);d.setStroke(dp(width),stroke);return d;}
    private TextView heading(String title){TextView t=label(title,20,WHITE,true);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(25);p.bottomMargin=dp(10);content.addView(t,p);return t;}
    private void showHome(){shell("Início");content.addView(label("Descubra sua próxima história",21,MUTED,false));
        heading("Recomendados para você");LinearLayout recommended=row();TextView status=label("Carregando recomendações...",16,MUTED,false);recommended.addView(status);
        Catalog.recommendations((list,error)->{recommended.removeAllViews();if(error!=null){recommended.addView(label("Catálogo indisponível: "+error,16,MUTED,false));return;}cards(recommended,list,true);});
        heading("Lançados recentemente");LinearLayout recent=row();recent.addView(label("Carregando lançamentos...",16,MUTED,false));
        Net.UI.postDelayed(()->Catalog.recent((list,error)->{if(!"Início".equals(page))return;recent.removeAllViews();if(error!=null){recent.addView(label("Não foi possível carregar lançamentos.",16,MUTED,false));return;}cards(recent,list,false);}),500);
        List<WatchHistory.Entry> history=WatchHistory.all(this);if(!history.isEmpty()){
            heading("Continuar assistindo");LinearLayout continueRow=row();HashSet<String> shown=new HashSet<>();
            for(WatchHistory.Entry e:history){if(e.watched()||!shown.add(e.animeId)||shown.size()>12)continue;
                Anime a=new Anime(e.animeId,e.title,e.poster,e.episode+" · "+formatTime(e.position),"Continuar",0,e.url);
                continueRow.addView(card(a,false));}
        }

    }
    private LinearLayout row(){HorizontalScrollView scroll=new HorizontalScrollView(this);scroll.setHorizontalScrollBarEnabled(false);LinearLayout row=new LinearLayout(this);row.setPadding(dp(2),dp(4),dp(2),dp(12));scroll.addView(row);content.addView(scroll,new LinearLayout.LayoutParams(-1,-2));return row;}
    private void cards(LinearLayout row,List<Anime> items,boolean large){for(Anime a:items){seen.put(a.id,a);row.addView(card(a,large));}}
    private View card(Anime a,boolean large){int width=large?190:148,height=large?225:114;
        FrameLayout tile=new FrameLayout(this);tile.setFocusable(true);tile.setBackground(bg(0xFF17171B,8));tile.setClipToOutline(true);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(width),dp(height));lp.rightMargin=dp(10);tile.setLayoutParams(lp);
        ImageView image=new ImageView(this);image.setScaleType(ImageView.ScaleType.CENTER_CROP);tile.addView(image,new FrameLayout.LayoutParams(-1,-1));Net.image(a.poster,image);
        View shade=new View(this);shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0x00000000,0xA508090C,0xFA08090C}));
        tile.addView(shade,new FrameLayout.LayoutParams(-1,dp(large?87:42),Gravity.BOTTOM));
        LinearLayout caption=column();pad(caption,9,5,8,8);tile.addView(caption,new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM));
        TextView title=label(a.title,large?15:11,WHITE,true);title.setSingleLine(true);title.setEllipsize(android.text.TextUtils.TruncateAt.END);caption.addView(title);
        String subtitle=large?(a.description.isEmpty()?a.genre:a.description.replaceAll("\\s+"," ")):a.id.startsWith("kitsu-")?"Informações · sem vídeo":a.id.startsWith("episode-")?Catalog.episodeLabel(a):a.genre;
        TextView meta=label(subtitle,large?11:9,MUTED,false);meta.setSingleLine(true);meta.setEllipsize(android.text.TextUtils.TruncateAt.END);caption.addView(meta);
        if(large){TextView badge=label(a.id.startsWith("kitsu-")?"CATÁLOGO":a.title.toLowerCase(Locale.ROOT).contains("dublado")?"DUBLADO":"LEGENDADO",8,WHITE,true);pad(badge,7,4,7,4);badge.setBackground(bg(a.title.toLowerCase(Locale.ROOT).contains("dublado")?0xFFEB1530:0xCC262B35,4));FrameLayout.LayoutParams bp=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.LEFT);bp.leftMargin=dp(7);bp.topMargin=dp(7);tile.addView(badge,bp);
            if(a.episodes>0){TextView count=label(a.episodes+" episódios",8,WHITE,false);pad(count,6,4,6,4);count.setBackground(bg(0xCC15151A,4));FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(-2,-2,Gravity.TOP|Gravity.RIGHT);cp.rightMargin=dp(7);cp.topMargin=dp(7);tile.addView(count,cp);}}
        View focusFrame=new View(this);focusFrame.setBackground(outline(Color.TRANSPARENT,RED,8,3));focusFrame.setVisibility(View.GONE);tile.addView(focusFrame,new FrameLayout.LayoutParams(-1,-1));
        tile.setOnFocusChangeListener((v,focused)->{focusFrame.setVisibility(focused?View.VISIBLE:View.GONE);tile.setScaleX(focused?1.025f:1f);tile.setScaleY(focused?1.025f:1f);});tile.setOnClickListener(v->details(a));return tile;
    }
    private TextView button(String title,Runnable action){TextView b=label(title,16,WHITE,true);b.setGravity(Gravity.CENTER);pad(b,18,12,18,12);b.setBackground(bg(0xFF292930,10));b.setFocusable(true);
        b.setOnFocusChangeListener((v,f)->b.setBackground(bg(f?0xFF8C2333:0xFF292930,10)));b.setOnClickListener(v->action.run());return b;}
    private void showSearch(){shell("Pesquisa");content.addView(label("Pesquisa",30,WHITE,true));
        EditText query=new EditText(this);query.setSingleLine(true);query.setHint("Buscar anime...");query.setTextColor(WHITE);query.setHintTextColor(MUTED);query.setTextSize(19*uiScale());query.setBackground(bg(0xFF242428,12));pad(query,18,10,18,10);
        LinearLayout.LayoutParams qp=new LinearLayout.LayoutParams(-1,dp(58));qp.topMargin=dp(26);content.addView(query,qp);
        heading("Categorias");HorizontalScrollView sc=new HorizontalScrollView(this);sc.setHorizontalScrollBarEnabled(false);LinearLayout chips=new LinearLayout(this);sc.addView(chips);content.addView(sc);
        heading("Catálogo de animes");LinearLayout results=column();content.addView(results);
        heading("Catálogo mundial · informações dos animes");LinearLayout kitsu=row();
        String[][] options={{"Todos",""},{"Ação","Ação"},{"Aventura","Aventura"},{"Fantasia","Fantasia"},{"Romance","Romance"},{"Comédia","Comédia"},{"Suspense","Suspense"},{"Drama","Drama"},{"Esporte","Esporte"},{"Terror","Terror"},{"Mistério","Mistério"},{"Sci-Fi","Sci-Fi"},{"Slice of Life","Slice of Life"},{"Sobrenatural","Sobrenatural"}};
        final String[] genre={""};
        Runnable search=()->{String q=query.getText().toString().trim();int generation=++searchGeneration;
            results.removeAllViews();results.addView(label("Carregando catálogo...",16,MUTED,false));appendResults(results,q,genre[0],0,generation);
            kitsu.removeAllViews();appendKitsu(kitsu,q,0,generation);};
        for(String[] item:options){TextView chip=button(item[0],()->{genre[0]=item[1];search.run();});LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,dp(47));p.rightMargin=dp(10);chips.addView(chip,p);}
        TextView go=button("⌕  Buscar",search);LinearLayout.LayoutParams gp=new LinearLayout.LayoutParams(dp(170),dp(48));gp.topMargin=dp(12);content.addView(go,2,gp);
        query.setOnEditorActionListener((v,id,event)->{search.run();((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(query.getWindowToken(),0);return true;});
        search.run();
    }
    private void appendKitsu(LinearLayout host,String query,int offset,int generation){
        TextView loading=label("Carregando mais títulos...",15,MUTED,false);host.addView(loading);
        Catalog.kitsuPage(query,offset,(items,error)->{if(!page.equals("Pesquisa")||generation!=searchGeneration)return;host.removeView(loading);
            if(error!=null){host.addView(button("Tentar novamente",()->{host.removeViewAt(host.getChildCount()-1);appendKitsu(host,query,offset,generation);}));return;}
            if(items.isEmpty()){if(offset==0)host.addView(label("Nenhum título adicional encontrado.",15,MUTED,false));return;}
            cards(host,items,false);
            if(items.size()==20){TextView more=button("Mais títulos ›",()->{host.removeViewAt(host.getChildCount()-1);appendKitsu(host,query,offset+20,generation);});host.addView(more,new LinearLayout.LayoutParams(dp(148),dp(114)));}
        });
    }
    private void appendResults(LinearLayout host,String query,String genre,int offset,int generation){
        Catalog.page(query,genre,offset,40,(items,error)->{if(!page.equals("Pesquisa")||generation!=searchGeneration)return;
            if(offset==0)host.removeAllViews();if(error!=null){host.addView(label("Falha ao carregar catálogo: "+error,16,MUTED,false));return;}
            if(items.isEmpty()){if(offset==0)host.addView(label("Nada encontrado nesta categoria.",16,MUTED,false));return;}
            int columns=4;
            LinearLayout line=null;for(int i=0;i<items.size();i++){if(i%columns==0){line=new LinearLayout(this);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(12);host.addView(line,lp);}
                Anime a=items.get(i);seen.put(a.id,a);line.addView(card(a,false));}
            if(items.size()==40){TextView more=button("Mostrar mais animes  ↓",()->{host.removeViewAt(host.getChildCount()-1);appendResults(host,query,genre,offset+40,generation);});
                LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(dp(300),dp(52));mp.topMargin=dp(10);host.addView(more,mp);}
        });
    }
    private String favKey(){String sub=getSharedPreferences("auth",0).getString("sub","guest");return "fav_"+sub;}
    private Set<String> favorites(){return new HashSet<>(getSharedPreferences("favorites",0).getStringSet(favKey(),new HashSet<>()));}
    private boolean isFavorite(Anime a){return favorites().contains(a.id);}
    private void toggleFavorite(Anime a){Set<String> ids=favorites();if(!ids.add(a.id))ids.remove(a.id);
        getSharedPreferences("favorites",0).edit().putStringSet(favKey(),ids).putString("item_"+a.id,a.title+"\n"+a.poster+"\n"+a.genre+"\n"+a.playUrl).apply();}
    private void showFavorites(){shell("Favoritos");content.addView(label("Minha lista",30,WHITE,true));heading("Animes salvos");LinearLayout list=row();
        Set<String> ids=favorites();if(ids.isEmpty()){list.addView(label("Seus favoritos aparecem aqui.",18,MUTED,false));return;}
        for(String id:ids){Anime a=seen.get(id);if(a==null){String[] bits=getSharedPreferences("favorites",0).getString("item_"+id,"Anime\n\n").split("\n",-1);a=new Anime(id,bits[0],bits.length>1?bits[1]:"","",bits.length>2?bits[2]:"",0,bits.length>3?bits[3]:"");}list.addView(card(a,true));}
    }
    private static String historyId(Anime a){return "anime-"+Catalog.normalize(a.title);}
    private static String formatTime(long ms){long s=ms/1000;return String.format(Locale.ROOT,"%02d:%02d",s/60,s%60);}
    private void showHistory(){shell("Histórico");content.addView(label("Histórico",30,WHITE,true));heading("Continuar de onde parou");LinearLayout list=row();
        List<WatchHistory.Entry> entries=WatchHistory.all(this);if(entries.isEmpty()){list.addView(label("Seus episódios assistidos aparecem aqui.",18,MUTED,false));return;}
        for(WatchHistory.Entry e:entries){Anime a=new Anime(e.animeId,e.title,e.poster,e.episode+(e.watched()?" · Assistido":" · "+formatTime(e.position)),"Histórico",0,e.url);list.addView(card(a,false));}
    }
    private void details(Anime a){
        if(!page.equals("Detalhes"))returnPage=page;
        detailFocus="";
        showDetail(a);
        if(a.description.isEmpty()||a.id.startsWith("episode-"))Catalog.search(a.title,"",(list,error)->{
            if(!page.equals("Detalhes")||selectedAnime!=a)return;
            for(Anime item:list)if(item.title.equalsIgnoreCase(a.title)&&!item.description.isEmpty()){showDetail(item);break;}
        });
    }
    private void showDetail(Anime a){selectedAnime=a;shell("Detalhes");
        TextView back=button("‹  Voltar",this::backFromDetail);content.addView(back,new LinearLayout.LayoutParams(dp(140),dp(44)));if(detailFocus.isEmpty())back.requestFocus();
        LinearLayout hero=new LinearLayout(this);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2);hp.topMargin=dp(16);hp.bottomMargin=dp(20);content.addView(hero,hp);
        ImageView cover=new ImageView(this);cover.setScaleType(ImageView.ScaleType.CENTER_CROP);cover.setBackground(bg(0xFF29212A,12));cover.setClipToOutline(true);
        hero.addView(cover,new LinearLayout.LayoutParams(dp(178),dp(248)));Net.image(a.poster,cover);
        LinearLayout info=column();pad(info,26,0,0,0);hero.addView(info,new LinearLayout.LayoutParams(0,-2,1));
        TextView name=label(a.title,28,WHITE,true);name.setMaxLines(2);info.addView(name);
        TextView meta=label(Catalog.seasonLabel(a.title)+"  ·  "+(a.genre.isEmpty()?"Anime":a.genre)+(a.episodes>0?"  ·  "+a.episodes+" episódios":""),15,RED,true);
        LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(-1,-2);mp.topMargin=dp(9);info.addView(meta,mp);
        TextView synopsis=label(a.description.isEmpty()?"Sinopse não disponível nesta fonte.":a.description,16,MUTED,false);synopsis.setMaxLines(6);synopsis.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);sp.topMargin=dp(13);info.addView(synopsis,sp);
        LinearLayout heroActions=new LinearLayout(this);LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,-2);ap.topMargin=dp(15);info.addView(heroActions,ap);
        addDetailAction(heroActions,button(isFavorite(a)?"♥  Favorito":"♡  Favoritar",()->{toggleFavorite(a);showDetail(a);}));
        WatchHistory.Entry previous=null;for(WatchHistory.Entry e:WatchHistory.all(this))if(e.animeId.equals(historyId(a))){previous=e;break;}
        if(previous!=null){WatchHistory.Entry h=previous;addDetailAction(heroActions,button("▶  Continuar "+h.episode,()->play(a,h.episode,h.url,"","",h.position)));}
        LinearLayout panel=column();pad(panel,20,18,20,20);panel.setBackground(bg(0xFF17171C,13));content.addView(panel,new LinearLayout.LayoutParams(-1,-2));
        panel.addView(label("Episódios",24,WHITE,true));
        TextView versionsTitle=label("TEMPORADAS",13,MUTED,true);LinearLayout.LayoutParams vtp=new LinearLayout.LayoutParams(-1,-2);vtp.topMargin=dp(18);vtp.bottomMargin=dp(8);panel.addView(versionsTitle,vtp);
        HorizontalScrollView versionScroll=new HorizontalScrollView(this);versionScroll.setHorizontalScrollBarEnabled(false);panel.addView(versionScroll);
        LinearLayout versions=new LinearLayout(this);versionScroll.addView(versions);versions.addView(label("Carregando temporadas...",15,MUTED,false));
        TextView audioTitle=label("ÁUDIO",13,MUTED,true);LinearLayout.LayoutParams atp=new LinearLayout.LayoutParams(-1,-2);atp.topMargin=dp(18);atp.bottomMargin=dp(8);panel.addView(audioTitle,atp);
        LinearLayout audio=new LinearLayout(this);panel.addView(audio);
        View divider=new View(this);divider.setBackgroundColor(0xFF34343A);LinearLayout.LayoutParams dl=new LinearLayout.LayoutParams(-1,dp(1));dl.topMargin=dp(18);dl.bottomMargin=dp(16);panel.addView(divider,dl);
        TextView count=label("Carregando episódios...",15,MUTED,false);panel.addView(count);
        LinearLayout episodeList=column();LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(-1,-2);ep.topMargin=dp(10);panel.addView(episodeList,ep);
        Catalog.related(a,(related,error)->{if(!page.equals("Detalhes")||selectedAnime!=a)return;versions.removeAllViews();audio.removeAllViews();
            ArrayList<Anime> ordered=new ArrayList<>(related);ordered.sort((x,y)->{
                int ax=Catalog.seasonLabel(x.title).startsWith("Temporada")?Catalog.seasonNumber(x.title):100;
                int ay=Catalog.seasonLabel(y.title).startsWith("Temporada")?Catalog.seasonNumber(y.title):100;
                return ax!=ay?ax-ay:x.title.compareToIgnoreCase(y.title);
            });
            LinkedHashSet<String> labels=new LinkedHashSet<>();for(Anime variant:ordered)labels.add(Catalog.seasonLabel(variant.title));
            String selected=Catalog.seasonLabel(a.title);boolean dubbed=a.title.toLowerCase(Locale.ROOT).contains("dublado");
            for(String season:labels){TextView tab=button(season,()->{Anime variant=findVersion(related,season,dubbed,true);if(variant!=null){detailFocus="season";showDetail(variant);}});
                tab.setBackground(bg(season.equals(selected)?0xFF9B2437:0xFF303037,9));addDetailAction(versions,tab);if(season.equals(selected)&&detailFocus.equals("season"))tab.requestFocus();}
            for(boolean wantDub:new boolean[]{true,false}){Anime variant=findVersion(related,selected,wantDub,false);if(variant==null)continue;
                TextView tab=button(wantDub?"Dublado":"Legendado",()->{detailFocus="audio";showDetail(variant);});tab.setBackground(bg(wantDub==dubbed?0xFF9B2437:0xFF303037,9));addDetailAction(audio,tab);if(wantDub==dubbed&&detailFocus.equals("audio"))tab.requestFocus();}
            detailFocus="";
            if(audio.getChildCount()==0)audio.addView(label("Versão original",15,MUTED,false));
        });
        Catalog.episodes(a,(eps,error)->{if(!page.equals("Detalhes")||selectedAnime!=a)return;episodeList.removeAllViews();
            if(!eps.isEmpty()){count.setText(eps.size()+" episódios · "+Catalog.seasonLabel(a.title));appendEpisodes(episodeList,a,eps,0);return;}
            count.setText("Nenhum episódio direto disponível nesta versão");
            Sources.find(this,a,(sources,sourceError)->{if(!page.equals("Detalhes")||selectedAnime!=a)return;
                for(Sources.Episode source:sources){TextView item=button(source.label,()->Sources.resolve(source,(url,err)->{
                    if(err==null)play(a,source.label,url,"","",0);else Toast.makeText(this,"Falha na fonte",Toast.LENGTH_SHORT).show();}));
                    LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(-1,dp(56));ip.topMargin=dp(8);episodeList.addView(item,ip);}
            });
        });
    }
    private Anime findVersion(List<Anime> related,String season,boolean dubbed,boolean fallback){
        for(Anime variant:related)if(Catalog.seasonLabel(variant.title).equals(season) && variant.title.toLowerCase(Locale.ROOT).contains("dublado")==dubbed)return variant;
        if(fallback)for(Anime variant:related)if(Catalog.seasonLabel(variant.title).equals(season))return variant;
        return null;
    }
    private void addDetailAction(LinearLayout row,View v){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,dp(46));lp.rightMargin=dp(10);row.addView(v,lp);}
    private void appendEpisodes(LinearLayout host,Anime anime,List<Anime> eps,int start){
        int end=Math.min(start+24,eps.size());for(int i=start;i<end;i++){final int index=i;Anime current=eps.get(i);WatchHistory.Entry watched=WatchHistory.find(this,historyId(anime),current.description);
            LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setFocusable(true);row.setBackground(bg(0xFF24242A,10));
            LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,dp(106));rp.bottomMargin=dp(8);host.addView(row,rp);
            ImageView thumb=new ImageView(this);thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);row.addView(thumb,new LinearLayout.LayoutParams(dp(174),-1));Net.image(current.poster.isEmpty()?anime.poster:current.poster,thumb);
            LinearLayout description=column();pad(description,16,5,5,5);row.addView(description,new LinearLayout.LayoutParams(0,-2,1));
            description.addView(label(current.description,18,WHITE,true));
            String state=watched==null?"Não assistido":watched.watched()?"✓ Assistido":"◐ Continuar em "+formatTime(watched.position);
            TextView status=label(state,14,watched!=null&&watched.watched()?0xFF60C88A:MUTED,false);LinearLayout.LayoutParams st=new LinearLayout.LayoutParams(-1,-2);st.topMargin=dp(7);description.addView(status,st);
            TextView arrow=label("▶",22,RED,true);pad(arrow,10,0,20,0);row.addView(arrow);
            row.setOnFocusChangeListener((v,f)->row.setBackground(bg(f?0xFF823040:0xFF24242A,10)));
            row.setOnClickListener(v->{WatchHistory.Entry progress=WatchHistory.find(this,historyId(anime),current.description);
                String nextUrl=index+1<eps.size()?eps.get(index+1).playUrl:"";String nextLabel=index+1<eps.size()?eps.get(index+1).description:"";
                play(anime,current.description,current.playUrl,nextUrl,nextLabel,progress==null||progress.watched()?0:progress.position);});
        }
        if(end<eps.size()){TextView more=button("Mostrar mais episódios  ↓",()->{host.removeViewAt(host.getChildCount()-1);appendEpisodes(host,anime,eps,end);});
            host.addView(more,new LinearLayout.LayoutParams(dp(300),dp(50)));}
    }
    private void backFromDetail(){String target=returnPage;selectedAnime=null;if(target.equals("Pesquisa"))showSearch();else if(target.equals("Favoritos"))showFavorites();else if(target.equals("Histórico"))showHistory();else showHome();}
    private void play(Anime a,String episode,String url,String nextUrl,String nextLabel,long position){Intent i=new Intent(this,PlayerActivity.class);
        i.putExtra("url",url);i.putExtra("title",a.title);i.putExtra("animeId",historyId(a));i.putExtra("poster",a.poster);i.putExtra("episode",episode);
        i.putExtra("nextUrl",nextUrl);i.putExtra("nextLabel",nextLabel);i.putExtra("startPosition",position);playerOpened=true;startActivity(i);}
    private void play(String url,String title){play(new Anime("sample",title,"","","",0),"Vídeo de teste",url,"","",0);}
    private void showSettings(){shell("Configurações");content.addView(label("Configurações",30,WHITE,true));
        String name=getSharedPreferences("auth",0).getString("name","");heading("Conta Google");
        content.addView(label(name.isEmpty()?"Nenhuma conta vinculada":"Vinculado: "+name,17,MUTED,false));
        TextView link=button("Vincular conta por código",()->GoogleLink.show(this,()->showSettings()));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(290),-2);p.topMargin=dp(13);content.addView(link,p);
        TextView unlink=button("Desvincular",()->{getSharedPreferences("auth",0).edit().remove("sub").remove("name").apply();showSettings();});content.addView(unlink,new LinearLayout.LayoutParams(dp(190),-2));
        heading("Fontes de episódios para teste");content.addView(label("Insira a URL HTTPS da sua instância das APIs. O catálogo principal usa animestvs.org, e o Kitsu complementa os metadados.",16,MUTED,false));
        android.content.SharedPreferences pref=getSharedPreferences("sources",0);
        EditText h=sourceField("URL da api-animesonline-cc",pref.getString("hallan",""));
        EditText s=sourceField("URL da SugoiAPI",pref.getString("sugoi",""));
        content.addView(button("Salvar fontes",()->{pref.edit().putString("hallan",h.getText().toString().trim()).putString("sugoi",s.getText().toString().trim()).apply();Toast.makeText(this,"Fontes salvas",Toast.LENGTH_SHORT).show();}));
        heading("Verificar player");content.addView(button("▶ Reproduzir vídeo de teste",()->play("https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4","Teste do player · Big Buck Bunny")));
        EditText manual=sourceField("URL HTTPS autorizada (.mp4 ou .m3u8)","");
        content.addView(button("▶ Reproduzir minha URL",()->{
            String url=manual.getText().toString().trim();
            if(!url.startsWith("https://")){Toast.makeText(this,"Use uma URL HTTPS",Toast.LENGTH_SHORT).show();return;}
            play(url,"Teste de URL própria");
        }));
    }
    private EditText sourceField(String hint,String value){EditText field=new EditText(this);field.setSingleLine(true);field.setHint(hint);field.setText(value);field.setTextColor(WHITE);field.setHintTextColor(MUTED);field.setTextSize(16*uiScale());field.setBackground(bg(0xFF26262C,9));pad(field,14,8,14,8);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(48));p.topMargin=dp(14);content.addView(field,p);return field;}
    @Override public void onBackPressed(){if(page.equals("Detalhes"))backFromDetail();else if(!page.equals("Início"))showHome();else super.onBackPressed();}
    @Override protected void onResume(){super.onResume();if(playerOpened){playerOpened=false;if(page.equals("Histórico"))showHistory();else if(page.equals("Início"))showHome();else if(page.equals("Detalhes")&&selectedAnime!=null)showDetail(selectedAnime);}}
}

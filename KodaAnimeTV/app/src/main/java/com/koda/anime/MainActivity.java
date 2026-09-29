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

public final class MainActivity extends androidx.appcompat.app.AppCompatActivity {
    private static final int BLACK=0xFF08080A, SIDE=0xFF0E0E12, RED=0xFFFF1635, RED_DARK=0xFF9E001B, WHITE=0xFFFFFFFF, MUTED=0xFFB8B8C0; private static final String VERSION="0.17-assets";
    private LinearLayout content,nav; private String page="Início"; private boolean playerOpened; private Anime selectedAnime; private String returnPage="Início",detailFocus=""; private int searchGeneration; private final Map<String,Anime> seen=new LinkedHashMap<>();
    private Anime heroAnime; private ImageView homeHeroImage; private TextView homeHeroTitle,homeHeroDescription,homeHeroWatch;
    private final ArrayList<TextView> navButtons=new ArrayList<>();
    @Override public void onCreate(Bundle state){super.onCreate(state);Net.initialize(this);ScreenFit.immersive(this);showHome();}
    private int screenGeneration;
    private ScreenFit screenFit;
    private float uiScale(){if(screenFit==null)screenFit=new ScreenFit(this);return screenFit.textScale(this);}
    private int dp(float v){return (int)(getResources().getDisplayMetrics().density*uiScale()*v+.5f);}
    private GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private TextView label(String text,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(text);t.setTextSize(size*uiScale());t.setTextColor(color);if(bold)t.setTypeface(null,Typeface.BOLD);return t;}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
    private void pad(View v,int a,int b,int c,int d){v.setPadding(dp(a),dp(b),dp(c),dp(d));}
    private void shell(String target){
        screenGeneration++; Net.clearImageQueue(); page=target; navButtons.clear();
        FrameLayout viewport=new FrameLayout(this); viewport.setBackgroundColor(BLACK); screenFit=new ScreenFit(this);

        ImageView backdrop=new ImageView(this); backdrop.setScaleType(ImageView.ScaleType.CENTER_CROP);
        backdrop.setImageResource(R.drawable.tv_banner); backdrop.setAlpha(.22f);
        viewport.addView(backdrop,screenFit.centered());

        View shade=new View(this);
        shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{0xF708080A,0xE608080A,0xB008080A,0xD908080A}));
        viewport.addView(shade,screenFit.centered());

        LinearLayout frame=new LinearLayout(this); frame.setOrientation(LinearLayout.HORIZONTAL); frame.setBackgroundColor(Color.TRANSPARENT);
        viewport.addView(frame,screenFit.centered());

        nav=column(); nav.setBackgroundColor(0xF20B0B0E); pad(nav,16,18,12,18);
        frame.addView(nav,new LinearLayout.LayoutParams(dp(190),-1));

        ImageView brand=new ImageView(this); brand.setImageResource(R.mipmap.ic_launcher); brand.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        LinearLayout.LayoutParams brandLp=new LinearLayout.LayoutParams(-1,dp(82)); brandLp.bottomMargin=dp(8); nav.addView(brand,brandLp);

        navItem("Início","",this::showHome);
        navItem("Lançados","",this::showLaunches);
        navItem("Recomendados","",this::showRecommended);
        navItem("Histórico","",this::showHistory);

        View divider=new View(this); divider.setBackgroundColor(0xFF2A2A30);
        LinearLayout.LayoutParams divLp=new LinearLayout.LayoutParams(-1,dp(1)); divLp.topMargin=dp(3); divLp.bottomMargin=dp(10); nav.addView(divider,divLp);

        navItem("Pesquisa","",this::showSearch);
        navItem("Favoritos","",this::showFavorites);
        navItem("Configurações","",this::showSettings);

        ScrollView sc=new ScrollView(this); sc.setFillViewport(true); sc.setClipToPadding(false); sc.setVerticalScrollBarEnabled(false);
        frame.addView(sc,new LinearLayout.LayoutParams(0,-1,1));
        content=column(); content.setClipChildren(false); content.setClipToPadding(false); pad(content,18,18,18,22); sc.addView(content);

        ImageView topArt=new ImageView(this);topArt.setImageResource(R.drawable.koda_overlay_top);topArt.setScaleType(ImageView.ScaleType.FIT_XY);topArt.setAlpha(target.equals("Início")?.82f:.44f);
        FrameLayout.LayoutParams topArtLp=new FrameLayout.LayoutParams(dp(390),dp(76),Gravity.TOP|Gravity.RIGHT);topArtLp.rightMargin=dp(10);topArtLp.topMargin=dp(2);viewport.addView(topArt,topArtLp);

        ImageView bottomArt=new ImageView(this);bottomArt.setImageResource(R.drawable.koda_overlay_bottom);bottomArt.setScaleType(ImageView.ScaleType.FIT_XY);bottomArt.setAlpha(target.equals("Início")?.72f:.36f);
        FrameLayout.LayoutParams bottomArtLp=new FrameLayout.LayoutParams(dp(340),dp(68),Gravity.BOTTOM|Gravity.LEFT);bottomArtLp.leftMargin=dp(184);bottomArtLp.bottomMargin=dp(2);viewport.addView(bottomArt,bottomArtLp);

        ImageView leaves=new ImageView(this);leaves.setImageResource(R.drawable.koda_leaves);leaves.setScaleType(ImageView.ScaleType.FIT_CENTER);leaves.setAlpha(target.equals("Início")?.62f:.28f);
        FrameLayout.LayoutParams leavesLp=new FrameLayout.LayoutParams(dp(96),dp(182),Gravity.TOP|Gravity.RIGHT);leavesLp.rightMargin=dp(8);leavesLp.topMargin=dp(38);viewport.addView(leaves,leavesLp);

        viewport.addView(new PetalOverlay(this),screenFit.centered());
        setContentView(viewport);
    }

    private void navItem(String name,String icon,Runnable action){
        boolean selected=page.equals(name);
        TextView v=label(name,13,selected?WHITE:MUTED,selected); v.setGravity(Gravity.CENTER_VERTICAL); v.setSingleLine(true); v.setEllipsize(android.text.TextUtils.TruncateAt.END); pad(v,12,0,8,0);
        NavIcon glyph=new NavIcon(name,selected?WHITE:MUTED); glyph.setBounds(0,0,dp(21),dp(21));
        v.setCompoundDrawables(glyph,null,null,null); v.setCompoundDrawablePadding(dp(12)); v.setFocusable(true);
        v.setBackground(selected?outline(0xB3210710,RED,8,1):bg(Color.TRANSPARENT,8));
        v.setOnFocusChangeListener((view,focus)->{
            v.setTextColor(focus||selected?WHITE:MUTED);
            v.setBackground(focus?outline(0xD02D0611,RED,8,2):selected?outline(0xB3210710,RED,8,1):bg(Color.TRANSPARENT,8));
            glyph.setTintColor(focus||selected?WHITE:MUTED);
            if(focus){v.setScaleX(1.025f);v.setScaleY(1.025f);}else{v.setScaleX(1f);v.setScaleY(1f);}
        });
        v.setOnClickListener(x->action.run());
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(42)); p.bottomMargin=dp(7); nav.addView(v,p); navButtons.add(v);
    }

    private GradientDrawable outline(int fill,int stroke,int radius,int width){GradientDrawable d=bg(fill,radius);d.setStroke(dp(width),stroke);return d;}
    private TextView heading(String title){TextView t=label(title,20,WHITE,true);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(18);p.bottomMargin=dp(9);content.addView(t,p);return t;}
    private TextView sectionTitle(String title){LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);View accent=new View(this);accent.setBackgroundColor(RED);row.addView(accent,new LinearLayout.LayoutParams(dp(4),dp(22)));TextView t=label(title,18,WHITE,true);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,-2,1);tp.leftMargin=dp(10);row.addView(t,tp);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.topMargin=dp(22);rp.bottomMargin=dp(11);content.addView(row,rp);return t;}
    private int railAvailableWidth(){return Math.max(dp(560),screenFit.width-dp(232));}
    private int railCardWidth(){return screenFit.recommendedCardWidth(railAvailableWidth());}
    private androidx.recyclerview.widget.RecyclerView rail(){
        androidx.recyclerview.widget.RecyclerView row=new androidx.recyclerview.widget.RecyclerView(this);
        row.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this,androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL,false));
        row.setNestedScrollingEnabled(false);row.setItemAnimator(null);row.setClipToPadding(false);row.setClipChildren(false);
        row.setOverScrollMode(View.OVER_SCROLL_NEVER);
        row.setPadding(dp(2),dp(5),dp(8),dp(12));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,screenFit.railHeight(railAvailableWidth()));
        lp.topMargin=dp(3);lp.bottomMargin=dp(24);content.addView(row,lp);return row;
    }
    private AnimeRowAdapter attachRail(androidx.recyclerview.widget.RecyclerView row,AnimeRowAdapter.OnAnimeSelectedListener listener){
        AnimeRowAdapter adapter=new AnimeRowAdapter(new ArrayList<>(),listener,railCardWidth());row.setAdapter(adapter);return adapter;
    }
    private void showHome(){
        shell("Início"); heroAnime=null; final int generation=screenGeneration;

        FrameLayout hero=new FrameLayout(this); hero.setClipToOutline(true); hero.setBackground(bg(0xFF111116,14));
        LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,dp(245)); hp.bottomMargin=dp(5); content.addView(hero,hp);

        homeHeroImage=new ImageView(this); homeHeroImage.setScaleType(ImageView.ScaleType.CENTER_CROP); homeHeroImage.setImageResource(R.drawable.tv_banner);
        hero.addView(homeHeroImage,new FrameLayout.LayoutParams(-1,-1));
        View scrim=new View(this); scrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{0xF508080A,0xD908080A,0x4008080A,0x2208080A}));
        hero.addView(scrim,new FrameLayout.LayoutParams(-1,-1));

        LinearLayout info=column(); pad(info,22,18,18,16);
        FrameLayout.LayoutParams ip=new FrameLayout.LayoutParams(dp(500),-1,Gravity.LEFT); hero.addView(info,ip);
        TextView eyebrow=label("SÉRIE EM DESTAQUE",11,RED,true); eyebrow.setLetterSpacing(.18f); info.addView(eyebrow);
        homeHeroTitle=label("Koda Anime",38,WHITE,true); homeHeroTitle.setTypeface(Typeface.SERIF,Typeface.BOLD); homeHeroTitle.setMaxLines(2);
        LinearLayout.LayoutParams titleLp=new LinearLayout.LayoutParams(-1,-2); titleLp.topMargin=dp(3); info.addView(homeHeroTitle,titleLp);
        TextView meta=label("HD   |   Anime   |   Dublado e Legendado",12,0xFFE1E1E6,false); LinearLayout.LayoutParams mlp=new LinearLayout.LayoutParams(-1,-2);mlp.topMargin=dp(4);info.addView(meta,mlp);
        homeHeroDescription=label("Carregando catálogo…",14,0xFFD0D0D6,false); homeHeroDescription.setMaxLines(3); homeHeroDescription.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams dlp=new LinearLayout.LayoutParams(-1,-2); dlp.topMargin=dp(8); info.addView(homeHeroDescription,dlp);
        LinearLayout actions=new LinearLayout(this); LinearLayout.LayoutParams alp=new LinearLayout.LayoutParams(-1,-2);alp.topMargin=dp(12);info.addView(actions,alp);
        homeHeroWatch=button("▶  Assistir",()->watchHeroDirect(generation)); homeHeroWatch.setEnabled(false);
        TextView more=button("ⓘ  Mais informações",()->{if(heroAnime!=null)details(heroAnime);});
        LinearLayout.LayoutParams a1=new LinearLayout.LayoutParams(dp(150),dp(46));a1.rightMargin=dp(10);actions.addView(homeHeroWatch,a1);
        actions.addView(more,new LinearLayout.LayoutParams(dp(190),dp(46)));

        sectionTitle("Lançados");
        androidx.recyclerview.widget.RecyclerView launched=rail();
        sectionTitle("Recomendados para você");
        androidx.recyclerview.widget.RecyclerView recommended=rail();

        AnimeRowAdapter.OnAnimeSelectedListener listener=new AnimeRowAdapter.OnAnimeSelectedListener(){
            @Override public void onSelected(Anime anime){details(anime);}
            @Override public void onAnimeFocused(Anime anime){if(screenGeneration==generation)updateHomeHero(anime);}
        };
        AnimeRowAdapter launchedAdapter=attachRail(launched,listener);
        AnimeRowAdapter recommendedAdapter=attachRail(recommended,listener);

        Catalog.recent((list,error)->{
            if(generation!=screenGeneration)return;
            launchedAdapter.submit(list); for(Anime a:list)seen.put(a.id,a);
            if(heroAnime==null&&!list.isEmpty())updateHomeHero(list.get(0));
        });
        Catalog.recommendations((list,error)->{
            if(generation!=screenGeneration)return;
            recommendedAdapter.submit(list); for(Anime a:list)seen.put(a.id,a);
            if(heroAnime==null&&!list.isEmpty())updateHomeHero(list.get(0));
        });
        if(!navButtons.isEmpty())navButtons.get(0).requestFocus();
    }

    private void updateHomeHero(Anime anime){
        if(anime==null)return; heroAnime=anime;
        homeHeroTitle.setText(anime.title);
        homeHeroDescription.setText(anime.description.isEmpty()?"Descubra temporadas, episódios e versões disponíveis.":anime.description);
        homeHeroImage.setTag(null); homeHeroImage.setImageResource(R.drawable.tv_banner); Net.image(anime.poster,homeHeroImage);
        homeHeroWatch.setEnabled(true); homeHeroWatch.setText("▶  Assistir");
        for(WatchHistory.Entry entry:WatchHistory.all(this))if(entry.animeId.equals(historyId(anime))&&!entry.watched()){homeHeroWatch.setText("▶  Continuar");break;}
    }

    private void watchHeroDirect(int generation){
        final Anime anime=heroAnime;if(anime==null)return;homeHeroWatch.setEnabled(false);homeHeroWatch.setText("Carregando…");
        Catalog.episodes(anime,(episodes,error)->{
            if(generation!=screenGeneration||heroAnime!=anime)return;
            homeHeroWatch.setEnabled(true);homeHeroWatch.setText("▶  Assistir");
            if(episodes.isEmpty()){Toast.makeText(this,"Escolha uma versão disponível na página do anime.",Toast.LENGTH_LONG).show();details(anime);return;}
            int index=0;long resume=0;
            for(WatchHistory.Entry entry:WatchHistory.all(this)){if(!entry.animeId.equals(historyId(anime))||entry.watched())continue;for(int n=0;n<episodes.size();n++)if(episodes.get(n).description.equals(entry.episode)){index=n;resume=entry.position;break;}break;}
            Anime ep=episodes.get(index);play(anime,ep.description,ep.playUrl,index+1<episodes.size()?episodes.get(index+1).playUrl:"",index+1<episodes.size()?episodes.get(index+1).description:"",resume);
        });
    }

    private void showLaunches(){showCollection("Lançados","LANÇADOS","Novos episódios e títulos adicionados recentemente.",true);}
    private void showRecommended(){showCollection("Recomendados","RECOMENDADOS","Seleção em destaque para descobrir o que assistir agora.",false);}
    private void showCollection(String pageName,String eyebrow,String subtitle,boolean recent){
        shell(pageName);TextView e=label(eyebrow,12,RED,true);e.setLetterSpacing(.18f);content.addView(e);
        TextView title=label(pageName,38,WHITE,true);title.setTypeface(Typeface.SERIF,Typeface.BOLD);content.addView(title);
        TextView sub=label(subtitle,15,MUTED,false);LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);sp.bottomMargin=dp(14);content.addView(sub,sp);
        sectionTitle(pageName);
        androidx.recyclerview.widget.RecyclerView list=rail();
        AnimeRowAdapter adapter=attachRail(list,new AnimeRowAdapter.OnAnimeSelectedListener(){@Override public void onSelected(Anime anime){details(anime);}@Override public void onAnimeFocused(Anime anime){}});
        Catalog.Result done=(items,error)->{adapter.submit(items);for(Anime a:items)seen.put(a.id,a);if(error!=null&&items.isEmpty())Toast.makeText(this,error,Toast.LENGTH_LONG).show();};
        if(recent)Catalog.recent(done);else Catalog.recommendations(done);
    }

    private LinearLayout row(){HorizontalScrollView scroll=new HorizontalScrollView(this);scroll.setHorizontalScrollBarEnabled(false);LinearLayout row=new LinearLayout(this);row.setPadding(dp(2),dp(4),dp(2),dp(12));scroll.addView(row);content.addView(scroll,new LinearLayout.LayoutParams(-1,-2));return row;}
    private void cards(LinearLayout row,List<Anime> items,boolean large){for(Anime a:items){seen.put(a.id,a);row.addView(card(a,large));}}
    private View card(Anime a,boolean large){
        int available=Math.max(dp(620),screenFit.width-dp(228));
        int widthPx=screenFit.recommendedCardWidth(available);
        if(!large)widthPx=Math.round(widthPx*.92f);
        int heightPx=Math.round(widthPx*1.50f);
        FrameLayout tile=new FrameLayout(this);tile.setFocusable(true);tile.setBackground(bg(0xFF121216,9));tile.setClipToOutline(false);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(widthPx,heightPx);lp.rightMargin=dp(14);tile.setLayoutParams(lp);
        ImageView image=new ImageView(this);image.setScaleType(ImageView.ScaleType.CENTER_CROP);image.setBackground(bg(0xFF18181D,8));tile.addView(image,new FrameLayout.LayoutParams(-1,-1));Net.image(a.poster,image);
        View shade=new View(this);shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0x00000000,0x25000000,0xF0000000}));tile.addView(shade,new FrameLayout.LayoutParams(-1,dp(70),Gravity.BOTTOM));
        TextView title=label(a.title,12,WHITE,true);title.setTypeface(Typeface.SERIF,Typeface.BOLD);title.setMaxLines(2);title.setEllipsize(android.text.TextUtils.TruncateAt.END);pad(title,9,0,8,8);tile.addView(title,new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM));
        View focus=new View(this);focus.setBackground(outline(Color.TRANSPARENT,RED,9,2));focus.setVisibility(View.GONE);tile.addView(focus,new FrameLayout.LayoutParams(-1,-1));
        tile.setOnFocusChangeListener((v,foc)->{focus.setVisibility(foc?View.VISIBLE:View.GONE);v.setScaleX(foc?1.035f:1f);v.setScaleY(foc?1.035f:1f);v.setElevation(foc?dp(7):0);});
        tile.setOnClickListener(v->details(a));return tile;
    }
    private TextView button(String title,Runnable action){
        TextView b=label(title,14,WHITE,true);b.setGravity(Gravity.CENTER);pad(b,16,8,16,8);b.setFocusable(true);b.setClickable(true);
        b.setBackground(bg(0xD91A1A20,8));
        b.setOnFocusChangeListener((v,foc)->{b.setBackground(foc?outline(0xE62B0710,RED,8,2):bg(0xD91A1A20,8));b.setScaleX(foc?1.03f:1f);b.setScaleY(foc?1.03f:1f);});
        b.setOnClickListener(v->action.run());return b;
    }
    private void showSearch(){
        shell("Pesquisa");
        TextView eyebrow=label("PESQUISAR",12,RED,true);eyebrow.setLetterSpacing(.18f);content.addView(eyebrow);
        TextView title=label("Pesquisar",40,WHITE,true);title.setTypeface(Typeface.SERIF,Typeface.BOLD);content.addView(title);
        TextView subtitle=label("Encontre seus animes favoritos, descubra novos títulos e explore um universo de histórias incríveis.",15,MUTED,false);content.addView(subtitle);

        EditText query=new EditText(this);query.setSingleLine(true);query.setHint("Digite o nome do anime, personagem ou gênero…");query.setTextColor(WHITE);query.setHintTextColor(0xFF9B9BA5);query.setTextSize(17*uiScale());query.setBackground(outline(0xE816161C,0xFF5A5A66,10,1));pad(query,18,8,18,8);
        query.setOnFocusChangeListener((v,foc)->query.setBackground(outline(foc?0xE62A0710:0xE816161C,foc?RED:0xFF5A5A66,10,foc?2:1)));
        LinearLayout.LayoutParams qp=new LinearLayout.LayoutParams(-1,dp(54));qp.topMargin=dp(12);qp.bottomMargin=dp(8);content.addView(query,qp);

        sectionTitle("Buscas populares");
        HorizontalScrollView chipScroll=new HorizontalScrollView(this);chipScroll.setHorizontalScrollBarEnabled(false);LinearLayout chips=new LinearLayout(this);chipScroll.addView(chips);content.addView(chipScroll);
        String[][] options={{"Todos",""},{"Ação","Ação"},{"Fantasia","Fantasia"},{"Isekai","Isekai"},{"Romance","Romance"},{"Escolar","Escolar"},{"Sobrenatural","Sobrenatural"},{"Mecha","Mecha"},{"Aventura","Aventura"}};
        final String[] genre={""};

        sectionTitle("Resultados");
        androidx.recyclerview.widget.RecyclerView primary=rail();
        sectionTitle("Mais resultados");
        androidx.recyclerview.widget.RecyclerView secondary=rail();
        AnimeRowAdapter.OnAnimeSelectedListener listener=new AnimeRowAdapter.OnAnimeSelectedListener(){@Override public void onSelected(Anime a){details(a);}@Override public void onAnimeFocused(Anime a){}};
        AnimeRowAdapter pAdapter=attachRail(primary,listener),sAdapter=attachRail(secondary,listener);

        Runnable search=()->{
            String q=query.getText().toString().trim();int generation=++searchGeneration;
            Catalog.page(q,genre[0],0,18,(items,error)->{if(!page.equals("Pesquisa")||generation!=searchGeneration)return;pAdapter.submit(items);for(Anime a:items)seen.put(a.id,a);});
            Catalog.kitsuPage(q,0,(items,error)->{if(!page.equals("Pesquisa")||generation!=searchGeneration)return;sAdapter.submit(items);for(Anime a:items)seen.put(a.id,a);});
        };
        for(String[] item:options){TextView chip=button("⌕  "+item[0],()->{genre[0]=item[1];search.run();});LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-2,dp(42));cp.rightMargin=dp(8);chips.addView(chip,cp);}
        query.setOnEditorActionListener((v,id,event)->{search.run();((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(query.getWindowToken(),0);return true;});
        search.run();query.requestFocus();
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
    private void showHistory(){
        shell("Histórico");
        TextView eyebrow=label("HISTÓRICO",12,RED,true);eyebrow.setLetterSpacing(.18f);content.addView(eyebrow);
        TextView title=label("Continue de onde parou",36,WHITE,true);title.setTypeface(Typeface.SERIF,Typeface.BOLD);content.addView(title);
        TextView sub=label("Seus episódios recentes ficam organizados aqui.",15,MUTED,false);content.addView(sub);
        sectionTitle("Assistidos recentemente");
        LinearLayout list=row();
        List<WatchHistory.Entry> entries=WatchHistory.all(this);
        if(entries.isEmpty()){list.addView(label("Seu histórico aparecerá aqui depois que você assistir a um episódio.",17,MUTED,false));return;}
        for(WatchHistory.Entry e:entries)list.addView(historyCard(e));
    }
    private View historyCard(WatchHistory.Entry e){
        FrameLayout tile=new FrameLayout(this);tile.setFocusable(true);tile.setBackground(bg(0xE6121217,9));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(260),dp(112));lp.rightMargin=dp(14);tile.setLayoutParams(lp);
        ImageView image=new ImageView(this);image.setScaleType(ImageView.ScaleType.CENTER_CROP);tile.addView(image,new FrameLayout.LayoutParams(-1,-1));Net.image(e.poster,image);
        View shade=new View(this);shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{0x22000000,0xCC08080A,0xF008080A}));tile.addView(shade,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout info=column();pad(info,105,14,12,10);tile.addView(info,new FrameLayout.LayoutParams(-1,-1));
        TextView name=label(e.title,14,WHITE,true);name.setMaxLines(1);name.setEllipsize(android.text.TextUtils.TruncateAt.END);info.addView(name);
        TextView ep=label(e.episode+"  ·  "+(e.watched()?"Assistido":formatTime(e.position)),12,MUTED,false);info.addView(ep);
        ProgressBar bar=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);bar.setMax(1000);bar.setProgress(e.duration>0?(int)Math.min(1000,e.position*1000/e.duration):0);bar.setProgressTintList(android.content.res.ColorStateList.valueOf(RED));bar.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF3B3B43));
        LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,dp(5));bp.topMargin=dp(12);info.addView(bar,bp);
        View focus=new View(this);focus.setBackground(outline(Color.TRANSPARENT,RED,9,2));focus.setVisibility(View.GONE);tile.addView(focus,new FrameLayout.LayoutParams(-1,-1));
        tile.setOnFocusChangeListener((v,foc)->{focus.setVisibility(foc?View.VISIBLE:View.GONE);v.setScaleX(foc?1.025f:1f);v.setScaleY(foc?1.025f:1f);});
        tile.setOnClickListener(v->{Anime a=new Anime(e.animeId,e.title,e.poster,"","Histórico",0,e.url);play(a,e.episode,e.url,"","",e.watched()?0:e.position);});
        return tile;
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
        ImageView cover=new ImageView(this);cover.setScaleType(ImageView.ScaleType.CENTER_CROP);cover.setBackground(bg(0xFF151519,12));cover.setClipToOutline(true);
        hero.addView(cover,new LinearLayout.LayoutParams(dp(178),dp(248)));Net.image(a.poster,cover);
        LinearLayout info=column();pad(info,26,0,0,0);hero.addView(info,new LinearLayout.LayoutParams(0,-2,1));
        TextView badge=label("SÉRIE EM DESTAQUE",11,RED,true);badge.setLetterSpacing(.18f);info.addView(badge);
        TextView name=label(a.title,34,WHITE,true);name.setTypeface(Typeface.SERIF,Typeface.BOLD);name.setMaxLines(2);info.addView(name);
        TextView meta=label(Catalog.seasonLabel(a.title)+"  ·  "+(a.genre.isEmpty()?"Anime":a.genre)+(a.episodes>0?"  ·  "+a.episodes+" episódios":""),15,RED,true);
        LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(-1,-2);mp.topMargin=dp(9);info.addView(meta,mp);
        TextView synopsis=label(a.description.isEmpty()?"Sinopse não disponível nesta fonte.":a.description,16,MUTED,false);synopsis.setMaxLines(6);synopsis.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);sp.topMargin=dp(13);info.addView(synopsis,sp);
        LinearLayout heroActions=new LinearLayout(this);LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,-2);ap.topMargin=dp(15);info.addView(heroActions,ap);
        TextView watchNow=button("▶  Assistir agora",()->playFirstEpisode(a));watchNow.setBackground(bg(RED,8));addDetailAction(heroActions,watchNow);
        addDetailAction(heroActions,button(isFavorite(a)?"♥  Favorito":"♡  Favoritar",()->{toggleFavorite(a);showDetail(a);}));
        WatchHistory.Entry previous=null;for(WatchHistory.Entry e:WatchHistory.all(this))if(e.animeId.equals(historyId(a))){previous=e;break;}
        if(previous!=null){WatchHistory.Entry h=previous;addDetailAction(heroActions,button("▶  Continuar "+h.episode,()->play(a,h.episode,h.url,"","",h.position)));}
        LinearLayout panel=column();pad(panel,20,18,20,20);panel.setBackground(bg(0xFF111116,13));content.addView(panel,new LinearLayout.LayoutParams(-1,-2));
        panel.addView(label("Episódios",24,WHITE,true));
        TextView versionsTitle=label("TEMPORADAS",13,MUTED,true);LinearLayout.LayoutParams vtp=new LinearLayout.LayoutParams(-1,-2);vtp.topMargin=dp(18);vtp.bottomMargin=dp(8);panel.addView(versionsTitle,vtp);
        HorizontalScrollView versionScroll=new HorizontalScrollView(this);versionScroll.setHorizontalScrollBarEnabled(false);panel.addView(versionScroll);
        LinearLayout versions=new LinearLayout(this);versionScroll.addView(versions);versions.addView(label("Carregando temporadas...",15,MUTED,false));
        TextView audioTitle=label("ÁUDIO",13,MUTED,true);LinearLayout.LayoutParams atp=new LinearLayout.LayoutParams(-1,-2);atp.topMargin=dp(18);atp.bottomMargin=dp(8);panel.addView(audioTitle,atp);
        LinearLayout audio=new LinearLayout(this);panel.addView(audio);
        View divider=new View(this);divider.setBackgroundColor(0xFF34343A);LinearLayout.LayoutParams dl=new LinearLayout.LayoutParams(-1,dp(1));dl.topMargin=dp(18);dl.bottomMargin=dp(16);panel.addView(divider,dl);
        TextView count=label("Carregando episódios...",15,MUTED,false);panel.addView(count);
        HorizontalScrollView episodeScroll=new HorizontalScrollView(this);episodeScroll.setHorizontalScrollBarEnabled(false);episodeScroll.setClipToPadding(false);
        LinearLayout episodeList=new LinearLayout(this);episodeList.setOrientation(LinearLayout.HORIZONTAL);episodeScroll.addView(episodeList);
        LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(-1,dp(126));ep.topMargin=dp(10);panel.addView(episodeScroll,ep);
        TextView relatedTitle=label("RELACIONADOS",13,MUTED,true);LinearLayout.LayoutParams rtp=new LinearLayout.LayoutParams(-1,-2);rtp.topMargin=dp(18);rtp.bottomMargin=dp(8);panel.addView(relatedTitle,rtp);
        HorizontalScrollView relatedScroll=new HorizontalScrollView(this);relatedScroll.setHorizontalScrollBarEnabled(false);relatedScroll.setClipToPadding(false);panel.addView(relatedScroll,new LinearLayout.LayoutParams(-1,dp(205)));
        LinearLayout relatedRow=new LinearLayout(this);relatedRow.setOrientation(LinearLayout.HORIZONTAL);relatedScroll.addView(relatedRow);
        Catalog.related(a,(related,error)->{if(!page.equals("Detalhes")||selectedAnime!=a)return;versions.removeAllViews();audio.removeAllViews();
            ArrayList<Anime> ordered=new ArrayList<>(related);ordered.sort((x,y)->{
                int ax=Catalog.seasonLabel(x.title).startsWith("Temporada")?Catalog.seasonNumber(x.title):100;
                int ay=Catalog.seasonLabel(y.title).startsWith("Temporada")?Catalog.seasonNumber(y.title):100;
                return ax!=ay?ax-ay:x.title.compareToIgnoreCase(y.title);
            });
            LinkedHashSet<String> labels=new LinkedHashSet<>();for(Anime variant:ordered)labels.add(Catalog.seasonLabel(variant.title));
            String selected=Catalog.seasonLabel(a.title);boolean dubbed=a.title.toLowerCase(Locale.ROOT).contains("dublado");
            for(String season:labels){TextView tab=button(season,()->{Anime variant=findVersion(related,season,dubbed,true);if(variant!=null){detailFocus="season";showDetail(variant);}});
                tab.setBackground(season.equals(selected)?outline(0xD92A0710,RED,9,2):bg(0xFF202026,9));addDetailAction(versions,tab);if(season.equals(selected)&&detailFocus.equals("season"))tab.requestFocus();}
            for(boolean wantDub:new boolean[]{true,false}){Anime variant=findVersion(related,selected,wantDub,false);if(variant==null)continue;
                TextView tab=button(wantDub?"Dublado":"Legendado",()->{detailFocus="audio";showDetail(variant);});tab.setBackground(wantDub==dubbed?outline(0xD92A0710,RED,9,2):bg(0xFF202026,9));addDetailAction(audio,tab);if(wantDub==dubbed&&detailFocus.equals("audio"))tab.requestFocus();}
            detailFocus="";
            if(audio.getChildCount()==0)audio.addView(label("Versão original",15,MUTED,false));
            relatedRow.removeAllViews();
            for(Anime variant:ordered)if(!variant.id.equals(a.id))relatedRow.addView(card(variant,false));
            if(relatedRow.getChildCount()==0)relatedRow.addView(label("Nenhum título relacionado disponível.",14,MUTED,false));
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
        int end=Math.min(start+24,eps.size());
        for(int i=start;i<end;i++){
            final int index=i;Anime current=eps.get(i);WatchHistory.Entry watched=WatchHistory.find(this,historyId(anime),current.description);
            FrameLayout tile=new FrameLayout(this);tile.setFocusable(true);tile.setBackground(bg(0xFF15151A,9));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(205),dp(112));lp.rightMargin=dp(12);host.addView(tile,lp);
            ImageView thumb=new ImageView(this);thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);tile.addView(thumb,new FrameLayout.LayoutParams(-1,-1));Net.image(current.poster.isEmpty()?anime.poster:current.poster,thumb);
            View shade=new View(this);shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0x10000000,0x50000000,0xEE08080A}));tile.addView(shade,new FrameLayout.LayoutParams(-1,-1));
            LinearLayout caption=column();pad(caption,12,0,10,9);FrameLayout.LayoutParams cp=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);tile.addView(caption,cp);
            TextView title=label((index+1)+". "+current.description,13,WHITE,true);title.setSingleLine(true);title.setEllipsize(android.text.TextUtils.TruncateAt.END);caption.addView(title);
            String state=watched==null?"Não assistido":watched.watched()?"✓ Assistido":"Continuar em "+formatTime(watched.position);
            caption.addView(label(state,11,watched!=null&&watched.watched()?0xFF66D38A:MUTED,false));
            View focus=new View(this);focus.setBackground(outline(Color.TRANSPARENT,RED,9,2));focus.setVisibility(View.GONE);tile.addView(focus,new FrameLayout.LayoutParams(-1,-1));
            tile.setOnFocusChangeListener((v,foc)->{focus.setVisibility(foc?View.VISIBLE:View.GONE);v.setScaleX(foc?1.025f:1f);v.setScaleY(foc?1.025f:1f);});
            tile.setOnClickListener(v->{WatchHistory.Entry progress=WatchHistory.find(this,historyId(anime),current.description);String nextUrl=index+1<eps.size()?eps.get(index+1).playUrl:"";String nextLabel=index+1<eps.size()?eps.get(index+1).description:"";play(anime,current.description,current.playUrl,nextUrl,nextLabel,progress==null||progress.watched()?0:progress.position);});
        }
        if(end<eps.size()){TextView more=button("Mais episódios  ›",()->{host.removeViewAt(host.getChildCount()-1);appendEpisodes(host,anime,eps,end);});host.addView(more,new LinearLayout.LayoutParams(dp(180),dp(112)));}
    }
    private void playFirstEpisode(Anime anime){
        Catalog.episodes(anime,(episodes,error)->{if(episodes.isEmpty()){Toast.makeText(this,"Nenhum episódio disponível nesta versão.",Toast.LENGTH_LONG).show();return;}Anime ep=episodes.get(0);play(anime,ep.description,ep.playUrl,episodes.size()>1?episodes.get(1).playUrl:"",episodes.size()>1?episodes.get(1).description:"",0);});
    }
    private void backFromDetail(){String target=returnPage;selectedAnime=null;if(target.equals("Pesquisa"))showSearch();else if(target.equals("Favoritos"))showFavorites();else if(target.equals("Histórico"))showHistory();else showHome();}
    private void play(Anime a,String episode,String url,String nextUrl,String nextLabel,long position){Intent i=new Intent(this,PlayerActivity.class);
        i.putExtra("url",url);i.putExtra("title",a.title);i.putExtra("animeId",historyId(a));i.putExtra("poster",a.poster);i.putExtra("episode",episode);
        i.putExtra("nextUrl",nextUrl);i.putExtra("nextLabel",nextLabel);i.putExtra("startPosition",position);playerOpened=true;startActivity(i);}
    private void play(String url,String title){play(new Anime("sample",title,"","","",0),"Vídeo de teste",url,"","",0);}
    private void showSettings(){
        shell("Configurações");
        TextView eyebrow=label("CONFIGURAÇÕES",12,RED,true);eyebrow.setLetterSpacing(.18f);content.addView(eyebrow);
        TextView title=label("Personalize sua experiência",34,WHITE,true);title.setTypeface(Typeface.SERIF,Typeface.BOLD);content.addView(title);
        TextView sub=label("Ajuste reprodução, qualidade, idioma e outros recursos do Koda Anime.",15,MUTED,false);LinearLayout.LayoutParams slp=new LinearLayout.LayoutParams(-1,-2);slp.bottomMargin=dp(14);content.addView(sub,slp);

        android.content.SharedPreferences prefs=getSharedPreferences("ui_settings",0);
        settingsToggle("▶","Reprodução automática","Reproduzir o próximo episódio automaticamente.","autoplay",true,prefs);
        settingsToggle("▷","Iniciar próximo episódio","Retomar automaticamente de onde parou.","resume",true,prefs);
        settingsToggle("◉","Prévia automática","Reproduzir trailers ao navegar pelos títulos.","preview",false,prefs);
        settingsChoice("▣","Qualidade","Qualidade de vídeo padrão","Automática (Recomendada)");
        settingsChoice("◎","Idioma","Idioma da interface","Português (Brasil)");
        settingsChoice("◖","Áudio","Idioma de áudio padrão","Japonês (Original)");
        settingsChoice("▤","Legendas","Idioma das legendas padrão","Português (Brasil)");
        settingsAction("◷","Histórico","Gerencie seu histórico de reprodução.","Limpar",()->{getSharedPreferences("watch_history",0).edit().clear().apply();Toast.makeText(this,"Histórico limpo.",Toast.LENGTH_SHORT).show();});
        settingsAction("ⓘ","Sobre o aplicativo","Koda Anime TV · versão "+VERSION,"Detalhes",()->new AlertDialog.Builder(this).setTitle("Koda Anime TV").setMessage("Versão "+VERSION+"\nInterface para Android TV.").setPositiveButton("OK",null).show());
    }
    private void settingsToggle(String icon,String title,String desc,String key,boolean def,android.content.SharedPreferences prefs){
        LinearLayout card=settingsCard(icon,title,desc);Switch toggle=new Switch(this);toggle.setChecked(prefs.getBoolean(key,def));toggle.setOnCheckedChangeListener((b,on)->prefs.edit().putBoolean(key,on).apply());card.addView(toggle,new LinearLayout.LayoutParams(-2,-2));
    }
    private void settingsChoice(String icon,String title,String desc,String value){
        LinearLayout card=settingsCard(icon,title,desc);TextView choice=button(value+"  ›",()->{});card.addView(choice,new LinearLayout.LayoutParams(dp(225),dp(42)));
    }
    private void settingsAction(String icon,String title,String desc,String action,Runnable run){
        LinearLayout card=settingsCard(icon,title,desc);TextView b=button(action,run);card.addView(b,new LinearLayout.LayoutParams(dp(130),dp(42)));
    }
    private LinearLayout settingsCard(String icon,String title,String desc){
        LinearLayout card=new LinearLayout(this);card.setGravity(Gravity.CENTER_VERTICAL);pad(card,14,10,14,10);card.setBackground(outline(0xE6111116,0xFF34343C,10,1));
        TextView glyph=label(icon,22,RED,true);glyph.setGravity(Gravity.CENTER);card.addView(glyph,new LinearLayout.LayoutParams(dp(52),dp(48)));
        LinearLayout text=column();TextView h=label(title,18,WHITE,true);TextView d=label(desc,13,MUTED,false);text.addView(h);text.addView(d);card.addView(text,new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(68));lp.bottomMargin=dp(8);content.addView(card,lp);return card;
    }
    private EditText sourceField(String hint,String value){EditText field=new EditText(this);field.setSingleLine(true);field.setHint(hint);field.setText(value);field.setTextColor(WHITE);field.setHintTextColor(MUTED);field.setTextSize(16*uiScale());field.setBackground(bg(0xFF26262C,9));pad(field,14,8,14,8);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(48));p.topMargin=dp(14);content.addView(field,p);return field;}
    @Override public void onBackPressed(){if(page.equals("Detalhes"))backFromDetail();else if(!page.equals("Início"))showHome();else super.onBackPressed();}
    @Override protected void onResume(){super.onResume();if(playerOpened){playerOpened=false;if(page.equals("Histórico"))showHistory();else if(page.equals("Início"))showHome();else if(page.equals("Detalhes")&&selectedAnime!=null)showDetail(selectedAnime);}}
}

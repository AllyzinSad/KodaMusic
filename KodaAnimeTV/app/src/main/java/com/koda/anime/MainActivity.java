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
    private LinearLayout content,nav; private String page="Início"; private boolean playerOpened; private Anime selectedAnime; private String returnPage="Início"; private int searchGeneration; private final Map<String,Anime> seen=new LinkedHashMap<>();
    private final ArrayList<TextView> navButtons=new ArrayList<>();
    @Override public void onCreate(Bundle state){super.onCreate(state);getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);showHome();}
    private int dp(float v){return (int)(getResources().getDisplayMetrics().density*v+.5f);}
    private GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private TextView label(String text,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(text);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(null,Typeface.BOLD);return t;}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(1);return l;}
    private void pad(View v,int a,int b,int c,int d){v.setPadding(dp(a),dp(b),dp(c),dp(d));}
    private void shell(String target){page=target;navButtons.clear();LinearLayout frame=new LinearLayout(this);frame.setBackgroundColor(BLACK);setContentView(frame);
        nav=column();nav.setBackgroundColor(SIDE);pad(nav,20,28,18,22);frame.addView(nav,new LinearLayout.LayoutParams(dp(220),-1));
        LinearLayout brand=new LinearLayout(this);brand.setGravity(Gravity.CENTER_VERTICAL);
        ImageView mark=new ImageView(this);mark.setImageResource(com.koda.anime.R.mipmap.ic_launcher);brand.addView(mark,new LinearLayout.LayoutParams(dp(40),dp(40)));
        TextView name=label("  Koda",23,WHITE,true);brand.addView(name);brand.addView(label(" Anime",23,RED,true));
        nav.addView(brand,new LinearLayout.LayoutParams(-1,dp(82)));
        navItem("Início","⌂",()->showHome());navItem("Pesquisa","⌕",()->showSearch());navItem("Favoritos","♡",()->showFavorites());navItem("Histórico","◷",()->showHistory());
        View spacer=new View(this);nav.addView(spacer,new LinearLayout.LayoutParams(1,0,1));
        navItem("Configurações","⚙",()->showSettings());
        ScrollView sc=new ScrollView(this);sc.setFillViewport(true);frame.addView(sc,new LinearLayout.LayoutParams(0,-1,1));
        content=column();pad(content,29,35,24,30);sc.addView(content);
    }
    private void navItem(String name,String icon,Runnable action){TextView v=label(icon+"   "+name,17,page.equals(name)?WHITE:MUTED,page.equals(name));v.setGravity(Gravity.CENTER_VERTICAL);pad(v,17,0,8,0);
        v.setBackground(bg(page.equals(name)?0xFF7C1525:SIDE,11));v.setFocusable(true);
        v.setOnFocusChangeListener((view,focus)->v.setBackground(bg(focus?0xFF70202B:(page.equals(name)?0xFF7C1525:SIDE),11)));
        v.setOnClickListener(x->action.run());LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(50));p.bottomMargin=dp(10);nav.addView(v,p);navButtons.add(v);
    }
    private GradientDrawable outline(int fill,int stroke,int radius,int width){GradientDrawable d=bg(fill,radius);d.setStroke(dp(width),stroke);return d;}
    private TextView heading(String title){TextView t=label(title,25,WHITE,true);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(28);p.bottomMargin=dp(15);content.addView(t,p);return t;}
    private void showHome(){shell("Início");content.addView(label("Descubra sua próxima história",27,MUTED,false));
        List<WatchHistory.Entry> history=WatchHistory.all(this);if(!history.isEmpty()){
            heading("Continuar assistindo");LinearLayout continueRow=row();HashSet<String> shown=new HashSet<>();
            for(WatchHistory.Entry e:history){if(e.watched()||!shown.add(e.animeId)||shown.size()>12)continue;
                Anime a=new Anime(e.animeId,e.title,e.poster,e.episode+" · "+formatTime(e.position),"Continuar",0,e.url);
                continueRow.addView(card(a,false));}
        }
        heading("Recomendados para você");LinearLayout recommended=row();TextView status=label("Carregando recomendações...",16,MUTED,false);recommended.addView(status);
        Catalog.recommendations((list,error)->{recommended.removeAllViews();if(error!=null){recommended.addView(label("Catálogo indisponível: "+error,16,MUTED,false));return;}cards(recommended,list,true);});
        heading("Lançados recentemente");LinearLayout recent=row();recent.addView(label("Carregando lançamentos...",16,MUTED,false));
        Net.UI.postDelayed(()->Catalog.recent((list,error)->{if(!"Início".equals(page))return;recent.removeAllViews();if(error!=null){recent.addView(label("Não foi possível carregar lançamentos.",16,MUTED,false));return;}cards(recent,list,false);}),1300);
    }
    private LinearLayout row(){HorizontalScrollView scroll=new HorizontalScrollView(this);scroll.setHorizontalScrollBarEnabled(false);LinearLayout row=new LinearLayout(this);row.setPadding(dp(2),dp(4),dp(2),dp(12));scroll.addView(row);content.addView(scroll,new LinearLayout.LayoutParams(-1,-2));return row;}
    private void cards(LinearLayout row,List<Anime> items,boolean large){for(Anime a:items){seen.put(a.id,a);row.addView(card(a,large));}}
    private View card(Anime a,boolean large){int width=large?224:196,height=large?294:178;
        LinearLayout outer=column();outer.setFocusable(true);outer.setBackground(bg(0xFF16161B,10));outer.setClipToOutline(true);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(width),dp(height));lp.rightMargin=dp(13);
        ImageView image=new ImageView(this);image.setScaleType(ImageView.ScaleType.CENTER_CROP);image.setBackground(bg(0xFF401D27,10));outer.addView(image,new LinearLayout.LayoutParams(-1,0,1));Net.image(a.poster,image);
        LinearLayout caption=column();caption.setBackgroundColor(0xFF17171B);pad(caption,10,8,8,8);outer.addView(caption,new LinearLayout.LayoutParams(-1,dp(large?69:57)));
        TextView title=label(a.title,large?16:14,WHITE,true);title.setMaxLines(1);title.setEllipsize(android.text.TextUtils.TruncateAt.END);caption.addView(title);
        String metadata=a.id.startsWith("episode-")?Catalog.episodeLabel(a)+" · "+a.genre:(a.genre.isEmpty()?"Anime":a.genre)+(a.episodes>0?" · "+a.episodes+" episódios":"");
        TextView meta=label(metadata,12,MUTED,false);meta.setSingleLine(true);meta.setEllipsize(android.text.TextUtils.TruncateAt.END);caption.addView(meta);
        outer.setOnFocusChangeListener((v,focus)->{outer.setBackground(bg(focus?0xFF5B1B27:0xFF16161B,10));outer.setScaleX(focus?1.025f:1f);outer.setScaleY(focus?1.025f:1f);});
        outer.setOnClickListener(v->details(a));outer.setLayoutParams(lp);return outer;
    }
    private TextView button(String title,Runnable action){TextView b=label(title,16,WHITE,true);b.setGravity(Gravity.CENTER);pad(b,18,12,18,12);b.setBackground(bg(0xFF292930,10));b.setFocusable(true);
        b.setOnFocusChangeListener((v,f)->b.setBackground(bg(f?0xFF8C2333:0xFF292930,10)));b.setOnClickListener(v->action.run());return b;}
    private void showSearch(){shell("Pesquisa");content.addView(label("Pesquisa",30,WHITE,true));
        EditText query=new EditText(this);query.setSingleLine(true);query.setHint("Buscar anime...");query.setTextColor(WHITE);query.setHintTextColor(MUTED);query.setTextSize(19);query.setBackground(bg(0xFF242428,12));pad(query,18,10,18,10);
        LinearLayout.LayoutParams qp=new LinearLayout.LayoutParams(-1,dp(58));qp.topMargin=dp(26);content.addView(query,qp);
        heading("Categorias");HorizontalScrollView sc=new HorizontalScrollView(this);sc.setHorizontalScrollBarEnabled(false);LinearLayout chips=new LinearLayout(this);sc.addView(chips);content.addView(sc);
        heading("Catálogo de animes");LinearLayout results=column();content.addView(results);
        heading("Mais títulos · Kitsu (metadados)");LinearLayout kitsu=row();kitsu.addView(label("Busque um título para explorar mais animes.",15,MUTED,false));
        String[][] options={{"Todos",""},{"Ação","Ação"},{"Aventura","Aventura"},{"Fantasia","Fantasia"},{"Romance","Romance"},{"Comédia","Comédia"},{"Suspense","Suspense"}};
        final String[] genre={""};
        Runnable search=()->{String q=query.getText().toString().trim();int generation=++searchGeneration;
            results.removeAllViews();results.addView(label("Carregando catálogo...",16,MUTED,false));appendResults(results,q,genre[0],0,generation);
            kitsu.removeAllViews();if(q.isEmpty()){kitsu.addView(label("Busque um título para explorar mais animes.",15,MUTED,false));return;}
            kitsu.addView(label("Buscando no Kitsu...",15,MUTED,false));Catalog.kitsuSearch(q,(items,error)->{
                if(!page.equals("Pesquisa")||generation!=searchGeneration)return;kitsu.removeAllViews();
                if(items.isEmpty()){kitsu.addView(label("Nenhum título adicional encontrado.",15,MUTED,false));return;}cards(kitsu,items,false);
            });};
        for(String[] item:options){TextView chip=button(item[0],()->{genre[0]=item[1];search.run();});LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,dp(47));p.rightMargin=dp(10);chips.addView(chip,p);}
        TextView go=button("⌕  Buscar",search);LinearLayout.LayoutParams gp=new LinearLayout.LayoutParams(dp(170),dp(48));gp.topMargin=dp(12);content.addView(go,2,gp);
        query.setOnEditorActionListener((v,id,event)->{search.run();((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(query.getWindowToken(),0);return true;});
        search.run();
    }
    private void appendResults(LinearLayout host,String query,String genre,int offset,int generation){
        Catalog.page(query,genre,offset,40,(items,error)->{if(!page.equals("Pesquisa")||generation!=searchGeneration)return;
            if(offset==0)host.removeAllViews();if(error!=null){host.addView(label("Falha ao carregar catálogo: "+error,16,MUTED,false));return;}
            if(items.isEmpty()){if(offset==0)host.addView(label("Nada encontrado nesta categoria.",16,MUTED,false));return;}
            int columns=getResources().getDisplayMetrics().widthPixels/getResources().getDisplayMetrics().density>950?3:2;
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
        showDetail(a);
        if(a.description.isEmpty()||a.id.startsWith("episode-"))Catalog.search(a.title,"",(list,error)->{
            if(!page.equals("Detalhes")||selectedAnime!=a)return;
            for(Anime item:list)if(item.title.equalsIgnoreCase(a.title)&&!item.description.isEmpty()){showDetail(item);break;}
        });
    }
    private void showDetail(Anime a){selectedAnime=a;shell("Detalhes");
        TextView back=button("‹  Voltar",()->backFromDetail());content.addView(back,new LinearLayout.LayoutParams(dp(140),dp(45)));
        LinearLayout hero=new LinearLayout(this);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2);hp.topMargin=dp(17);content.addView(hero,hp);
        ImageView cover=new ImageView(this);cover.setScaleType(ImageView.ScaleType.CENTER_CROP);cover.setBackground(bg(0xFF29212A,12));cover.setClipToOutline(true);
        hero.addView(cover,new LinearLayout.LayoutParams(dp(186),dp(260)));Net.image(a.poster,cover);
        LinearLayout info=column();pad(info,27,0,0,0);hero.addView(info,new LinearLayout.LayoutParams(0,-2,1));
        TextView name=label(a.title,29,WHITE,true);name.setMaxLines(2);info.addView(name);
        TextView meta=label(Catalog.seasonLabel(a.title)+"  ·  "+(a.genre.isEmpty()?"Anime":a.genre)+(a.episodes>0?"  ·  "+a.episodes+" episódios":""),15,RED,true);
        LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(-1,-2);mp.topMargin=dp(11);info.addView(meta,mp);
        TextView synopsis=label(a.description.isEmpty()?"Sinopse não disponível nesta fonte.":a.description,16,MUTED,false);synopsis.setMaxLines(6);synopsis.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2);sp.topMargin=dp(15);info.addView(synopsis,sp);
        LinearLayout actions=new LinearLayout(this);LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,-2);ap.topMargin=dp(18);info.addView(actions,ap);
        TextView fav=button(isFavorite(a)?"♥  Favorito":"♡  Favoritar",()->{toggleFavorite(a);showDetail(a);});addDetailAction(actions,fav);
        WatchHistory.Entry previous=null;for(WatchHistory.Entry e:WatchHistory.all(this))if(e.animeId.equals(historyId(a))){previous=e;break;}
        if(previous!=null){WatchHistory.Entry h=previous;addDetailAction(actions,button("▶  Continuar "+h.episode,()->play(a,h.episode,h.url,"","",h.position)));}
        heading("Temporadas e versões");HorizontalScrollView seasonScroll=new HorizontalScrollView(this);seasonScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout seasons=new LinearLayout(this);seasons.addView(label("Carregando...",15,MUTED,false));seasonScroll.addView(seasons);content.addView(seasonScroll);
        Catalog.related(a,(related,error)->{if(!page.equals("Detalhes")||selectedAnime!=a)return;seasons.removeAllViews();
            for(Anime variant:related){String title=Catalog.seasonLabel(variant.title)+(variant.title.toLowerCase(Locale.ROOT).contains("dublado")?" · Dublado":" · Legendado");
                TextView chip=button(title,()->showDetail(variant));chip.setBackground(bg(variant.title.equals(a.title)?0xFF9A2133:0xFF292930,10));addDetailAction(seasons,chip);}
        });
        heading("Episódios · "+Catalog.seasonLabel(a.title));LinearLayout episodeList=column();content.addView(episodeList);
        episodeList.addView(label("Carregando episódios...",16,MUTED,false));
        Catalog.episodes(a,(eps,error)->{if(!page.equals("Detalhes")||selectedAnime!=a)return;episodeList.removeAllViews();
            if(!eps.isEmpty()){appendEpisodes(episodeList,a,eps,0);return;}
            episodeList.addView(label("Nenhum episódio direto encontrado nesta versão.",16,MUTED,false));
            Sources.find(this,a,(sources,sourceError)->{if(!page.equals("Detalhes")||selectedAnime!=a)return;
                for(Sources.Episode ep:sources){TextView item=button(ep.label,()->Sources.resolve(ep,(url,err)->{
                    if(err==null)play(a,ep.label,url,"","",0);else Toast.makeText(this,"Falha na fonte",Toast.LENGTH_SHORT).show();}));
                    LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(-1,dp(58));ip.topMargin=dp(8);episodeList.addView(item,ip);}
            });
        });
    }
    private void addDetailAction(LinearLayout row,View v){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,dp(48));lp.rightMargin=dp(12);row.addView(v,lp);}
    private void appendEpisodes(LinearLayout host,Anime anime,List<Anime> eps,int start){
        int end=Math.min(start+24,eps.size());for(int i=start;i<end;i+=2){LinearLayout line=new LinearLayout(this);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(13);host.addView(line,lp);
            for(int j=i;j<Math.min(i+2,end);j++){final int index=j;Anime ep=eps.get(j);WatchHistory.Entry h=WatchHistory.find(this,historyId(anime),ep.description);
                LinearLayout tile=column();tile.setFocusable(true);tile.setBackground(bg(0xFF1B1B20,11));tile.setClipToOutline(true);
                LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(dp(240),dp(185));tp.rightMargin=dp(14);line.addView(tile,tp);
                ImageView thumb=new ImageView(this);thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);tile.addView(thumb,new LinearLayout.LayoutParams(-1,dp(125)));Net.image(ep.poster,thumb);
                TextView title=label("▶  "+ep.description,17,WHITE,true);pad(title,11,5,5,0);tile.addView(title);
                TextView status=label(h==null?"Não assistido":h.watched()?"✓ Assistido":"◐ Continuar em "+formatTime(h.position),12,h!=null&&h.watched()?RED:MUTED,false);pad(status,11,1,5,0);tile.addView(status);
                tile.setOnFocusChangeListener((v,f)->{tile.setBackground(bg(f?0xFF902435:0xFF1B1B20,11));tile.setScaleX(f?1.02f:1f);tile.setScaleY(f?1.02f:1f);});
                tile.setOnClickListener(v->{WatchHistory.Entry progress=WatchHistory.find(this,historyId(anime),ep.description);
                    String nextUrl=index+1<eps.size()?eps.get(index+1).playUrl:"";String nextLabel=index+1<eps.size()?eps.get(index+1).description:"";
                    play(anime,ep.description,ep.playUrl,nextUrl,nextLabel,progress==null||progress.watched()?0:progress.position);});
            }
        }
        if(end<eps.size()){TextView more=button("Mostrar mais episódios  ↓",()->{host.removeViewAt(host.getChildCount()-1);appendEpisodes(host,anime,eps,end);});
            host.addView(more,new LinearLayout.LayoutParams(dp(300),dp(52)));}
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
        heading("Fontes de episódios para teste");content.addView(label("Insira a URL HTTPS da sua instância das APIs. O catálogo é fornecido pelo Jikan; as fontes são opcionais.",16,MUTED,false));
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
    private EditText sourceField(String hint,String value){EditText field=new EditText(this);field.setSingleLine(true);field.setHint(hint);field.setText(value);field.setTextColor(WHITE);field.setHintTextColor(MUTED);field.setTextSize(16);field.setBackground(bg(0xFF26262C,9));pad(field,14,8,14,8);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(48));p.topMargin=dp(14);content.addView(field,p);return field;}
    @Override public void onBackPressed(){if(page.equals("Detalhes"))backFromDetail();else if(!page.equals("Início"))showHome();else super.onBackPressed();}
    @Override protected void onResume(){super.onResume();if(playerOpened){playerOpened=false;if(page.equals("Histórico"))showHistory();else if(page.equals("Início"))showHome();else if(page.equals("Detalhes")&&selectedAnime!=null)showDetail(selectedAnime);}}
}

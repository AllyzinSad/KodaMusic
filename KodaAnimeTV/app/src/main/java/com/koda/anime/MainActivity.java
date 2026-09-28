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
    private LinearLayout content,nav; private String page="Início"; private final Map<String,Anime> seen=new LinkedHashMap<>();
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
        navItem("Início","⌂",()->showHome());navItem("Pesquisa","⌕",()->showSearch());navItem("Favoritos","♡",()->showFavorites());
        View spacer=new View(this);nav.addView(spacer,new LinearLayout.LayoutParams(1,0,1));
        navItem("Configurações","⚙",()->showSettings());
        ScrollView sc=new ScrollView(this);sc.setFillViewport(true);frame.addView(sc,new LinearLayout.LayoutParams(0,-1,1));
        content=column();pad(content,29,35,24,30);sc.addView(content);
    }
    private void navItem(String name,String icon,Runnable action){TextView v=label(icon+"   "+name,17,page.equals(name)?WHITE:MUTED,page.equals(name));v.setGravity(Gravity.CENTER_VERTICAL);pad(v,17,0,8,0);
        v.setBackground(bg(page.equals(name)?0xFF7C1525:SIDE,11));v.setFocusable(true);
        v.setOnFocusChangeListener((view,focus)->{if(focus) v.setBackground(outline(0xFF35151D,RED,10,2));else v.setBackground(bg(page.equals(name)?0xFF7C1525:SIDE,11));});
        v.setOnClickListener(x->action.run());LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(50));p.bottomMargin=dp(10);nav.addView(v,p);navButtons.add(v);
    }
    private GradientDrawable outline(int fill,int stroke,int radius,int width){GradientDrawable d=bg(fill,radius);d.setStroke(dp(width),stroke);return d;}
    private TextView heading(String title){TextView t=label(title,25,WHITE,true);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(28);p.bottomMargin=dp(15);content.addView(t,p);return t;}
    private void showHome(){shell("Início");content.addView(label("Descubra sua próxima história",27,MUTED,false));
        heading("Recomendados para você");LinearLayout recommended=row();TextView status=label("Carregando recomendações...",16,MUTED,false);recommended.addView(status);
        Catalog.recommendations((list,error)->{recommended.removeAllViews();if(error!=null){recommended.addView(label("Catálogo indisponível: "+error,16,MUTED,false));return;}cards(recommended,list,true);});
        heading("Lançados recentemente");LinearLayout recent=row();recent.addView(label("Carregando lançamentos...",16,MUTED,false));
        Net.UI.postDelayed(()->Catalog.recent((list,error)->{if(!"Início".equals(page))return;recent.removeAllViews();if(error!=null){recent.addView(label("Não foi possível carregar lançamentos.",16,MUTED,false));return;}cards(recent,list,false);}),1300);
    }
    private LinearLayout row(){HorizontalScrollView scroll=new HorizontalScrollView(this);scroll.setHorizontalScrollBarEnabled(false);LinearLayout row=new LinearLayout(this);row.setPadding(dp(2),dp(4),dp(2),dp(12));scroll.addView(row);content.addView(scroll,new LinearLayout.LayoutParams(-1,-2));return row;}
    private void cards(LinearLayout row,List<Anime> items,boolean large){for(Anime a:items){seen.put(a.id,a);row.addView(card(a,large));}}
    private View card(Anime a,boolean large){int width=large?224:196,height=large?294:178;
        LinearLayout outer=column();outer.setFocusable(true);outer.setBackground(outline(0xFF16161B,0xFF303038,10,1));outer.setClipToOutline(true);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(width),dp(height));lp.rightMargin=dp(13);
        ImageView image=new ImageView(this);image.setScaleType(ImageView.ScaleType.CENTER_CROP);image.setBackground(bg(0xFF401D27,10));outer.addView(image,new LinearLayout.LayoutParams(-1,0,1));Net.image(a.poster,image);
        LinearLayout caption=column();caption.setBackgroundColor(0xFF17171B);pad(caption,10,8,8,8);outer.addView(caption,new LinearLayout.LayoutParams(-1,dp(large?69:57)));
        TextView title=label(a.title,large?16:14,WHITE,true);title.setMaxLines(1);title.setEllipsize(android.text.TextUtils.TruncateAt.END);caption.addView(title);
        TextView meta=label((a.genre.isEmpty()?"Anime":a.genre)+(a.episodes>0?" · "+a.episodes+" episódios":""),12,MUTED,false);meta.setSingleLine(true);meta.setEllipsize(android.text.TextUtils.TruncateAt.END);caption.addView(meta);
        outer.setOnFocusChangeListener((v,focus)->{outer.setBackground(focus?outline(0xFF221014,RED,10,3):outline(0xFF16161B,0xFF303038,10,1));outer.setScaleX(focus?1.025f:1f);outer.setScaleY(focus?1.025f:1f);});
        outer.setOnClickListener(v->details(a));outer.setLayoutParams(lp);return outer;
    }
    private TextView button(String title,Runnable action){TextView b=label(title,16,WHITE,true);b.setGravity(Gravity.CENTER);pad(b,18,12,18,12);b.setBackground(bg(0xFF292930,10));b.setFocusable(true);
        b.setOnFocusChangeListener((v,f)->b.setBackground(f?outline(0xFF451A22,RED,10,2):bg(0xFF292930,10)));b.setOnClickListener(v->action.run());return b;}
    private void showSearch(){shell("Pesquisa");content.addView(label("Pesquisa",30,WHITE,true));
        EditText query=new EditText(this);query.setSingleLine(true);query.setHint("Buscar anime...");query.setTextColor(WHITE);query.setHintTextColor(MUTED);query.setTextSize(19);query.setBackground(outline(0xFF242428,0xFF4A4A50,12,1));pad(query,18,10,18,10);
        LinearLayout.LayoutParams qp=new LinearLayout.LayoutParams(-1,dp(58));qp.topMargin=dp(26);content.addView(query,qp);
        heading("Categorias");HorizontalScrollView sc=new HorizontalScrollView(this);sc.setHorizontalScrollBarEnabled(false);LinearLayout chips=new LinearLayout(this);sc.addView(chips);content.addView(sc);
        heading("Resultados");LinearLayout results=row();TextView note=label("Digite um título ou escolha uma categoria.",17,MUTED,false);results.addView(note);
        String[][] options={{"Todos",""},{"Ação","1"},{"Aventura","2"},{"Fantasia","10"},{"Romance","22"},{"Comédia","4"},{"Suspense","41"}};
        final String[] genre={""};
        Runnable search=()->{String q=query.getText().toString().trim();if(q.isEmpty()&&genre[0].isEmpty())return;results.removeAllViews();results.addView(label("Buscando...",16,MUTED,false));
            String url="https://api.jikan.moe/v4/anime?"+(q.isEmpty()?"":"q="+Net.enc(q)+"&")+(genre[0].isEmpty()?"":"genres="+genre[0]+"&")+"limit=20";
            Net.json(url,(j,e)->{if(!"Pesquisa".equals(page))return;results.removeAllViews();if(e!=null||j==null){results.addView(label("Falha na busca.",16,MUTED,false));return;}
                org.json.JSONArray data=j.optJSONArray("data");if(data==null||data.length()==0){results.addView(label("Nada encontrado.",16,MUTED,false));return;}
                for(int i=0;i<data.length();i++){org.json.JSONObject o=data.optJSONObject(i);if(o!=null){Anime a=Anime.fromJikan(o);seen.put(a.id,a);results.addView(card(a,true));}}
            });};
        for(String[] item:options){TextView chip=button(item[0],()->{genre[0]=item[1];search.run();});LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,dp(47));p.rightMargin=dp(10);chips.addView(chip,p);}
        query.setOnEditorActionListener((v,id,event)->{search.run();((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(query.getWindowToken(),0);return true;});
    }
    private String favKey(){String sub=getSharedPreferences("auth",0).getString("sub","guest");return "fav_"+sub;}
    private Set<String> favorites(){return new HashSet<>(getSharedPreferences("favorites",0).getStringSet(favKey(),new HashSet<>()));}
    private boolean isFavorite(Anime a){return favorites().contains(a.id);}
    private void toggleFavorite(Anime a){Set<String> ids=favorites();if(!ids.add(a.id))ids.remove(a.id);
        getSharedPreferences("favorites",0).edit().putStringSet(favKey(),ids).putString("item_"+a.id,a.title+"\n"+a.poster+"\n"+a.genre).apply();}
    private void showFavorites(){shell("Favoritos");content.addView(label("Minha lista",30,WHITE,true));heading("Animes salvos");LinearLayout list=row();
        Set<String> ids=favorites();if(ids.isEmpty()){list.addView(label("Seus favoritos aparecem aqui.",18,MUTED,false));return;}
        for(String id:ids){Anime a=seen.get(id);if(a==null){String[] bits=getSharedPreferences("favorites",0).getString("item_"+id,"Anime\n\n").split("\n",-1);a=new Anime(id,bits[0],bits.length>1?bits[1]:"","",bits.length>2?bits[2]:"",0);}list.addView(card(a,true));}
    }
    private void details(Anime a){LinearLayout box=column();pad(box,30,12,30,12);TextView text=label(a.description.isEmpty()?"Escolha uma fonte de episódios para testar a reprodução.":a.description,16,WHITE,false);box.addView(text);
        TextView info=label("\n"+a.genre+(a.episodes>0?" · "+a.episodes+" episódios":""),14,MUTED,false);box.addView(info);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(a.title).setView(box).setNegativeButton("Fechar",null).create();
        box.addView(button(isFavorite(a)?"♥ Remover dos favoritos":"♡ Adicionar aos favoritos",()->{toggleFavorite(a);dialog.dismiss();if(page.equals("Favoritos"))showFavorites();}));
        box.addView(button("▶ Buscar episódios nas fontes",()->{dialog.dismiss();loadEpisodes(a);}));dialog.show();
    }
    private void loadEpisodes(Anime a){AlertDialog loading=new AlertDialog.Builder(this).setMessage("Consultando fontes de episódios...").setCancelable(true).create();loading.show();
        Sources.find(this,a,(eps,error)->{loading.dismiss();if(isFinishing())return;
            if(eps.isEmpty()){new AlertDialog.Builder(this).setTitle("Sem episódio disponível").setMessage(error==null?"As fontes não retornaram vídeo.":error).setPositiveButton("Configurar fontes",(d,w)->showSettings()).setNegativeButton("Fechar",null).show();return;}
            String[] labels=new String[eps.size()];for(int i=0;i<eps.size();i++)labels[i]=eps.get(i).label;
            new AlertDialog.Builder(this).setTitle(a.title+" · Escolha o episódio").setItems(labels,(d,w)->{
                AlertDialog resolving=new AlertDialog.Builder(this).setMessage("Preparando episódio...").create();resolving.show();
                Sources.resolve(eps.get(w),(url,err)->{resolving.dismiss();if(err!=null){new AlertDialog.Builder(this).setMessage("Falha na fonte: "+err.getMessage()).setPositiveButton("OK",null).show();return;}play(url,a.title+" · "+eps.get(w).label);});
            }).setNegativeButton("Cancelar",null).show();
        });}
    private void play(String url,String title){Intent i=new Intent(this,PlayerActivity.class);i.putExtra("url",url);i.putExtra("title",title);startActivity(i);}
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
    @Override public void onBackPressed(){if(!page.equals("Início"))showHome();else super.onBackPressed();}
}

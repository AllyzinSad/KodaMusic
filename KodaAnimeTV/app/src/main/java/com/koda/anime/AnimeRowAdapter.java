package com.koda.anime;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AnimeRowAdapter extends RecyclerView.Adapter<AnimeRowAdapter.Holder> {
    public interface OnAnimeSelectedListener { default void onSelected(Anime anime) {} void onAnimeFocused(Anime anime); }
    private final List<Anime> items=new ArrayList<>();
    private final OnAnimeSelectedListener listener;
    private final int cardWidthPx;
    public AnimeRowAdapter(List<Anime> animeList,OnAnimeSelectedListener listener){this(animeList,listener,0);}
    public AnimeRowAdapter(List<Anime> animeList,OnAnimeSelectedListener listener,int cardWidthPx){this.listener=listener;this.cardWidthPx=cardWidthPx;this.items.addAll(animeList);}
    void submit(List<Anime> data){items.clear();items.addAll(data);notifyDataSetChanged();}
    @Override public int getItemCount(){return items.size();}
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent,int type){
        View view=LayoutInflater.from(parent.getContext()).inflate(R.layout.item_anime,parent,false);
        if(cardWidthPx>0){
            ViewGroup.LayoutParams lp=view.getLayoutParams();
            lp.width=cardWidthPx;
            lp.height=ViewGroup.LayoutParams.WRAP_CONTENT;
            view.setLayoutParams(lp);
        }
        return new Holder(view);
    }
    @Override public void onBindViewHolder(@NonNull Holder holder,int position){
        Anime anime=items.get(position);
        holder.image.setTag(null);holder.image.setImageDrawable(null);
        holder.title.setText(anime.getName());
        holder.title.setTextColor(holder.itemView.hasFocus()?0xFFFF1635:0xFFFFFFFF);
        holder.itemView.setTranslationZ(holder.itemView.hasFocus()?8f:0f);
        String audio=anime.isDubbed()?"DUB":"LEG";
        holder.audio.setText(audio);holder.itemView.setContentDescription(anime.title+", "+audio);
        Net.image(anime.poster,holder.image);
        holder.itemView.setOnClickListener(v->listener.onSelected(anime));
        holder.itemView.setOnFocusChangeListener((v,focused)->{
            holder.title.setTextColor(focused?0xFFFF1635:0xFFFFFFFF);
            v.setTranslationZ(focused?8f:0f);
            if(focused)listener.onAnimeFocused(anime);
        });
    }
    @Override public void onViewRecycled(@NonNull Holder holder){
        holder.image.setTag(null);holder.image.setImageDrawable(null);
        holder.itemView.setOnFocusChangeListener(null);super.onViewRecycled(holder);
    }
    static final class Holder extends RecyclerView.ViewHolder {
        final ImageView image;final TextView title,audio;
        Holder(View view){super(view);image=view.findViewById(R.id.anime_image);title=view.findViewById(R.id.anime_name);audio=view.findViewById(R.id.anime_audio_tag);}
    }
}

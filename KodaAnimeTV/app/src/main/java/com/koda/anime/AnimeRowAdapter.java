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

final class AnimeRowAdapter extends RecyclerView.Adapter<AnimeRowAdapter.Holder> {
    interface Listener { void onSelected(Anime anime); void onFocused(Anime anime); }
    private final List<Anime> items=new ArrayList<>();
    private final Listener listener;
    AnimeRowAdapter(Listener listener){this.listener=listener;}
    void submit(List<Anime> data){items.clear();items.addAll(data);notifyDataSetChanged();}
    @Override public int getItemCount(){return items.size();}
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent,int type){
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_anime,parent,false));
    }
    @Override public void onBindViewHolder(@NonNull Holder holder,int position){
        Anime anime=items.get(position);
        holder.image.setTag(null);holder.image.setImageDrawable(null);
        holder.title.setText(anime.title);
        String audio=anime.title.toLowerCase(Locale.ROOT).contains("dublado")?"DUB":"LEG";
        holder.audio.setText(audio);holder.itemView.setContentDescription(anime.title+", "+audio);
        Net.image(anime.poster,holder.image);
        holder.itemView.setOnClickListener(v->listener.onSelected(anime));
        holder.itemView.setOnFocusChangeListener((v,focused)->{
            holder.title.setTextColor(focused?0xFFFF6600:0xFFFFFFFF);
            v.setTranslationZ(focused?8f:0f);
            if(focused)listener.onFocused(anime);
        });
    }
    @Override public void onViewRecycled(@NonNull Holder holder){
        holder.image.setTag(null);holder.image.setImageDrawable(null);
        holder.itemView.setOnFocusChangeListener(null);super.onViewRecycled(holder);
    }
    static final class Holder extends RecyclerView.ViewHolder {
        final ImageView image;final TextView title,audio;
        Holder(View view){super(view);image=view.findViewById(R.id.anime_poster);title=view.findViewById(R.id.anime_title);audio=view.findViewById(R.id.anime_audio);}
    }
}

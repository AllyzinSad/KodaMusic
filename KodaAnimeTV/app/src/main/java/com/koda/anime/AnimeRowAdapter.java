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

public final class AnimeRowAdapter extends RecyclerView.Adapter<AnimeRowAdapter.Holder> {
    public interface OnAnimeSelectedListener { default void onSelected(Anime anime) {} void onAnimeFocused(Anime anime); }
    private final List<Anime> items=new ArrayList<>();
    private final OnAnimeSelectedListener listener;

    public AnimeRowAdapter(List<Anime> animeList,OnAnimeSelectedListener listener){
        this.listener=listener;this.items.addAll(animeList);
    }

    void submit(List<Anime> data){items.clear();items.addAll(data);notifyDataSetChanged();}
    @Override public int getItemCount(){return items.size();}

    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent,int type){
        View view=LayoutInflater.from(parent.getContext()).inflate(R.layout.item_anime,parent,false);

        int available=parent.getMeasuredWidth();
        if(available<=0)available=(int)(parent.getResources().getDisplayMetrics().widthPixels*.76f);
        float density=parent.getResources().getDisplayMetrics().density;
        int gap=Math.max(10,(int)(12*density));

        // ~5–6 cards on a TV row, automatically adapting to the real available width.
        float visible=available>=1800?6.2f:available>=1150?5.6f:4.8f;
        int width=Math.max((int)(94*density),Math.round((available-gap*(visible-1))/visible));

        RecyclerView.LayoutParams lp=new RecyclerView.LayoutParams(width,ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.rightMargin=gap;
        view.setLayoutParams(lp);
        return new Holder(view);
    }

    @Override public void onBindViewHolder(@NonNull Holder holder,int position){
        Anime anime=items.get(position);
        holder.image.setTag(null);holder.image.setImageDrawable(null);
        holder.title.setText(anime.getName());
        holder.title.setTextColor(holder.itemView.hasFocus()?0xFFFF1635:0xFFFFFFFF);
        holder.itemView.setTranslationZ(holder.itemView.hasFocus()?8f:0f);
        String audio=anime.isDubbed()?"DUB":"LEG";
        holder.audio.setText(audio);
        holder.itemView.setContentDescription(anime.title+", "+audio);
        Net.image(anime.poster,holder.image);

        holder.itemView.setOnClickListener(v->listener.onSelected(anime));
        holder.itemView.setOnFocusChangeListener((v,focused)->{
            holder.title.setTextColor(focused?0xFFFF1635:0xFFFFFFFF);
            v.setTranslationZ(focused?8f:0f);
            v.setScaleX(focused?1.035f:1f);
            v.setScaleY(focused?1.035f:1f);
            if(focused)listener.onAnimeFocused(anime);
        });
    }

    @Override public void onViewRecycled(@NonNull Holder holder){
        holder.image.setTag(null);holder.image.setImageDrawable(null);
        holder.itemView.setOnFocusChangeListener(null);
        super.onViewRecycled(holder);
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ImageView image;final TextView title,audio;
        Holder(View view){
            super(view);
            image=view.findViewById(R.id.anime_image);
            title=view.findViewById(R.id.anime_name);
            audio=view.findViewById(R.id.anime_audio_tag);
        }
    }
}
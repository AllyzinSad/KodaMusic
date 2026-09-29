package com.koda.anime;

import android.app.Activity;
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
    public interface OnAnimeSelectedListener {
        default void onSelected(Anime anime){}
        void onAnimeFocused(Anime anime);
    }

    private final List<Anime> items=new ArrayList<>();
    private final OnAnimeSelectedListener listener;

    public AnimeRowAdapter(List<Anime> animeList,OnAnimeSelectedListener listener){
        this.listener=listener;items.addAll(animeList);
    }

    void submit(List<Anime> data){
        items.clear();items.addAll(data);notifyDataSetChanged();
    }

    @Override public int getItemCount(){return items.size();}

    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent,int type){
        View view=LayoutInflater.from(parent.getContext()).inflate(R.layout.item_anime,parent,false);
        int available=parent.getMeasuredWidth();
        if(available<=0)available=(int)(parent.getResources().getDisplayMetrics().widthPixels*.72f);

        int width;
        if(parent.getContext() instanceof Activity){
            ScreenFit fit=new ScreenFit((Activity)parent.getContext());
            width=fit.posterWidth(available);
        }else{
            float d=parent.getResources().getDisplayMetrics().density;
            width=Math.max((int)(112*d),Math.round(available/5.5f));
        }

        int gap=Math.max(8,(int)(parent.getResources().getDisplayMetrics().density*10));
        RecyclerView.LayoutParams lp=new RecyclerView.LayoutParams(width,ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.rightMargin=gap;
        view.setLayoutParams(lp);
        return new Holder(view);
    }

    @Override public void onBindViewHolder(@NonNull Holder holder,int position){
        Anime anime=items.get(position);
        holder.image.setTag(null);holder.image.setImageDrawable(null);
        holder.title.setText(anime.getName());
        String audio=anime.isDubbed()?"DUB":"LEG";
        holder.audio.setText(audio);
        holder.itemView.setContentDescription(anime.title+", "+audio);
        Net.image(anime.poster,holder.image);

        holder.itemView.setOnClickListener(v->listener.onSelected(anime));
        holder.itemView.setOnFocusChangeListener((v,focused)->{
            holder.title.setTextColor(focused?0xFFFF1635:0xFFFFFFFF);
            v.setTranslationZ(focused?12f:0f);
            v.animate().scaleX(focused?1.035f:1f).scaleY(focused?1.035f:1f).setDuration(120).start();
            if(focused)listener.onAnimeFocused(anime);
        });
    }

    @Override public void onViewRecycled(@NonNull Holder holder){
        holder.image.setTag(null);holder.image.setImageDrawable(null);
        holder.itemView.setOnFocusChangeListener(null);
        holder.itemView.setScaleX(1f);holder.itemView.setScaleY(1f);
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
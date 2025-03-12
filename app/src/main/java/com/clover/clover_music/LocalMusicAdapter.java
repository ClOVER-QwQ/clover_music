package com.clover.clover_music;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class LocalMusicAdapter extends RecyclerView.Adapter<LocalMusicAdapter.LocalMusicViewHolder> {
    private final Context context;
    private final List<LocalMusicBean> mDatas;
    private OnItemClickListener onItemClickListener;

    public LocalMusicAdapter(Context context, List<LocalMusicBean> mDatas) {
        this.context = context;
        this.mDatas = mDatas;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    @NonNull
    @Override
    public LocalMusicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_clover_music, parent, false);
        return new LocalMusicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LocalMusicViewHolder holder, int position) {
        LocalMusicBean musicBean = mDatas.get(position);
        holder.idTv.setText(musicBean.getId());
        holder.songTv.setText(musicBean.getSong());
        holder.singerTv.setText(musicBean.getSinger());
        holder.albumTv.setText(musicBean.getAlbum());
        holder.timeTv.setText(musicBean.getDuration());

        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                int adapterPosition = holder.getAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onItemClickListener.onItemClick(v, adapterPosition);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mDatas.size();
    }

    public static class LocalMusicViewHolder extends RecyclerView.ViewHolder {
        private final TextView idTv;
        private final TextView songTv;
        private final TextView singerTv;
        private final TextView albumTv;
        private final TextView timeTv;

        public LocalMusicViewHolder(View itemView) {
            super(itemView);
            idTv = itemView.findViewById(R.id.item_local_music_num);
            songTv = itemView.findViewById(R.id.item_local_music_song);
            singerTv = itemView.findViewById(R.id.item_local_music_singer);
            albumTv = itemView.findViewById(R.id.item_local_music_album);
            timeTv = itemView.findViewById(R.id.item_local_music_durtion);
        }
    }
}
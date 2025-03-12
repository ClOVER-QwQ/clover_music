package com.clover.clover_music;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.icu.text.SimpleDateFormat;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int REQUEST_CODE_PERMISSION = 1;
    private ImageView playIv;
    private ImageView albumIv;
    private TextView singerTv, songTv;
    private RecyclerView musicRv;

    private List<LocalMusicBean> mDatas;
    private LocalMusicAdapter adapter;

    private int currentPlayPosition = -1;
    private int currentPausePositionInSong = 0;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        mediaPlayer = new MediaPlayer();
        mDatas = new ArrayList<>();
        adapter = new LocalMusicAdapter(this, mDatas);
        musicRv.setAdapter(adapter);
        musicRv.setLayoutManager(new LinearLayoutManager(this));
        checkAndRequestStoragePermission();
        setEventListener();
    }

    private void checkAndRequestStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_PERMISSION);
        } else {
            loadLocalMusicData();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadLocalMusicData();
            } else {
                Toast.makeText(this, "需要存储权限才能加载音乐！", Toast.LENGTH_SHORT).show();
                Log.e("Permission", "Storage permission denied");
            }
        }
    }

    private void initView() {
        ImageView nextIv = findViewById(R.id.clover_music_bottom_iv_next);
        playIv = findViewById(R.id.clover_music_bottom_iv_play);
        ImageView lastIv = findViewById(R.id.clover_music_bottom_iv_last);
        albumIv = findViewById(R.id.clover_music_bottom_iv_icon);
        singerTv = findViewById(R.id.clover_music_bottom_tv_singer);
        songTv = findViewById(R.id.clover_music_bottom_tv_song);
        musicRv = findViewById(R.id.clover_music_rv);
        nextIv.setOnClickListener(this);
        lastIv.setOnClickListener(this);
        playIv.setOnClickListener(this);
    }

    private void setEventListener() {
        adapter.setOnItemClickListener((view, position) -> {
            currentPlayPosition = position;
            playSelectedMusic();
        });
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.clover_music_bottom_iv_last) {
            if (currentPlayPosition == 0) {
                Toast.makeText(this, "已经是第一首了，没有上一曲！", Toast.LENGTH_SHORT).show();
                return;
            }
            currentPlayPosition--;
            playSelectedMusic();
        } else if (id == R.id.clover_music_bottom_iv_next) {
            if (currentPlayPosition == mDatas.size() - 1) {
                Toast.makeText(this, "已经是最后一首了，没有下一曲！", Toast.LENGTH_SHORT).show();
                return;
            }
            currentPlayPosition++;
            playSelectedMusic();
        } else if (id == R.id.clover_music_bottom_iv_play) {
            if (currentPlayPosition == -1) {
                Toast.makeText(this, "请选择想要播放的音乐", Toast.LENGTH_SHORT).show();
                return;
            }
            if (mediaPlayer.isPlaying()) {
                pauseMusic();
            } else {
                playMusic();
            }
        }
    }

    private void playSelectedMusic() {
        LocalMusicBean musicBean = mDatas.get(currentPlayPosition);
        try {
            playMusicInMusicBean(musicBean);
            updateBottomLayout(musicBean);
        } catch (Exception e) {
            Log.e("MusicPlayer", "Error playing music", e);
            Toast.makeText(this, "无法播放音乐", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateBottomLayout(LocalMusicBean musicBean) {
        songTv.setText(musicBean.getSong());
        singerTv.setText(musicBean.getSinger());
        String albumArt = musicBean.getAlbumArt();
        if (albumArt != null && new File(albumArt).exists()) {
            albumIv.setImageBitmap(BitmapFactory.decodeFile(albumArt));
        } else {
            albumIv.setImageResource(R.mipmap.ic_launcher); // 默认专辑封面
        }
    }

    public void playMusicInMusicBean(LocalMusicBean musicBean) throws Exception {
        stopMusic();
        mediaPlayer.reset();
        mediaPlayer.setDataSource(musicBean.getPath());
        mediaPlayer.prepare();
        mediaPlayer.start();
        playIv.setImageResource(R.mipmap.icon_pause);
    }

    private void playMusic() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.seekTo(currentPausePositionInSong);
            mediaPlayer.start();
            playIv.setImageResource(R.mipmap.icon_pause);
        }
    }

    private void pauseMusic() {
        if (mediaPlayer.isPlaying()) {
            currentPausePositionInSong = mediaPlayer.getCurrentPosition();
            mediaPlayer.pause();
            playIv.setImageResource(R.mipmap.icon_play);
        }
    }

    private void stopMusic() {
        if (mediaPlayer != null) {
            currentPausePositionInSong = 0;
            mediaPlayer.pause();
            mediaPlayer.seekTo(0);
            mediaPlayer.stop();
            playIv.setImageResource(R.mipmap.icon_play);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void loadLocalMusicData() {
        File musicDir = new File(Environment.getExternalStorageDirectory(), "Music");
        if (!musicDir.exists() || !musicDir.isDirectory()) {
            Log.d("MusicLoader", "Music folder does not exist or is not a directory: " + musicDir.getAbsolutePath());
            return;
        }

        File[] files = musicDir.listFiles();
        if (files == null || files.length == 0) {
            Log.d("MusicLoader", "Music folder is empty: " + musicDir.getAbsolutePath());
            return;
        }

        for (File file : files) {
            if (file.isFile() && isAudioFile(file.getName())) {
                String id = String.valueOf(mDatas.size() + 1);
                String song = file.getName();
                String singer = "Unknown Artist";
                String album = "Unknown Album";
                String path = file.getAbsolutePath();
                long duration = getDuration(path);
                String albumArt = getAlbumArt(path);

                SimpleDateFormat sdf = new SimpleDateFormat("mm:ss", Locale.getDefault());
                String time = sdf.format(new Date(duration));

                LocalMusicBean bean = new LocalMusicBean(id, song, singer, album, time, path, albumArt);
                mDatas.add(bean);

                Log.d("MusicLoader", "Loaded music: " +
                        "\nTitle: " + song +
                        "\nArtist: " + singer +
                        "\nAlbum: " + album +
                        "\nPath: " + path +
                        "\nDuration: " + time +
                        "\nAlbum Art: " + albumArt);

                adapter.notifyItemInserted(mDatas.size() - 1);
            }
        }
    }

    private boolean isAudioFile(String fileName) {
        return fileName.toLowerCase().endsWith(".mp3") || fileName.toLowerCase().endsWith(".wav") || fileName.toLowerCase().endsWith(".aac");
    }

    private long getDuration(String filePath) {
        MediaPlayer mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(filePath);
            mediaPlayer.prepare();
            return mediaPlayer.getDuration();
        } catch (Exception e) {
            Log.e("MusicLoader", "Error getting duration for file: " + filePath, e);
            return 0;
        } finally {
            mediaPlayer.release();
        }
    }

    private String getAlbumArt(String filePath) {
        return null;
    }
}
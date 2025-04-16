package com.clover.clover_music;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.icu.text.SimpleDateFormat;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
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

import com.bumptech.glide.Glide;

import java.io.File;
import java.sql.Date;
import java.util.ArrayList;
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

        if (albumArt != null) {
            // 使用 Glide 加载专辑封面
            Glide.with(MainActivity.this)
                    .load(albumArt)
                    .placeholder(R.mipmap.ic_launcher) // 设置占位图
                    .error(R.mipmap.ic_launcher) // 设置错误图
                    .into(albumIv);
        } else {
            // 默认专辑封面
            Glide.with(MainActivity.this)
                    .load(R.mipmap.ic_launcher)
                    .into(albumIv);
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

                SimpleDateFormat sdf = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    sdf = new SimpleDateFormat("mm:ss", Locale.getDefault());
                }
                String time = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    time = sdf.format(new Date(duration));
                }

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
        ContentResolver contentResolver = getContentResolver();
        Uri uri = Uri.fromFile(new File(filePath));
        String albumArtPath = null;

        // 查询音乐文件获取专辑ID
        Cursor cursor = contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Audio.Media.ALBUM_ID},
                MediaStore.Audio.Media.DATA + "=?",
                new String[]{filePath},
                null);

        if (cursor != null && cursor.moveToFirst()) {
            @SuppressLint("Range") long albumId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
            cursor.close();

            // 根据专辑ID获取专辑封面
            Uri albumArtUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId);
            albumArtPath = albumArtUri.toString();
        }

        return albumArtPath;
    }
}
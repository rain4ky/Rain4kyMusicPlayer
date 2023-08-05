package com.example.rain4kymusicplayer;

import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;

import java.io.IOException;
import java.util.Timer;

public class MusicPlayService extends Service {
    private final IBinder musicBinder = new MusicBinder();
    static private boolean isPlay;
    Timer timer;
    MediaPlayer mediaPlayer=new MediaPlayer();
    public MusicPlayService() {}
    @Override
    public void onCreate(){
        super.onCreate();
    }
    @Override
    public IBinder onBind(Intent intent) {
        return musicBinder;
    }
    public boolean getIsPlay(){
        return isPlay;
    }
    public void prepareMediaPlayer(String path){
        mediaPlayer.reset();
        mediaPlayer.setAudioAttributes(new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
        isPlay=false;
        try {
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public void playMusic(){
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mediaPlayer.start();
            }
        });
        isPlay=true;
    }
    public void pauseMusic(){
        mediaPlayer.pause();
        isPlay=false;
    }
    public class MusicBinder extends Binder{
        public MusicPlayService getService(){
            return MusicPlayService.this;
        }
    }
}
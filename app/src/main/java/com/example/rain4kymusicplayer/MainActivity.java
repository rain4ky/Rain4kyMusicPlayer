package com.example.rain4kymusicplayer;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.loader.content.CursorLoader;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    String[] permissions={Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.MOUNT_FORMAT_FILESYSTEMS,
            Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
            Manifest.permission.READ_MEDIA_AUDIO};
    TextView sing_name_now,sing_time_end,sing_time_now;
    SeekBar music_seekbar;
    ImageButton input_button,ssl_button,last_song,play_pause,next_song;
    RecyclerView sing_list;MyAdapter myAdapter;SwipeRefreshLayout refresher;
    List<Sing> R_sing_list = new ArrayList<>();
    SQLiteOpenHelper My_sql_helper;SQLiteDatabase sqliteDatabase;
    MediaPlayer song_player=new MediaPlayer();
    Timer timer;
    int ssl_button_i=1,pp_i=1,song_play_i=0;
    boolean seek_on_change=false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        if (ContextCompat.checkSelfPermission(this, permissions[0]) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, permissions[1]) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, permissions[2]) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, permissions[3]) == PackageManager.PERMISSION_GRANTED) {
            main_function();
        } else {
            ActivityCompat.requestPermissions(this, permissions, 1);
        }
    }
    @Override
    protected void onDestroy(){super.onDestroy();sqliteDatabase.close();if (song_player.isPlaying()){song_player.stop();}song_player.release();song_player=null;}
    @Override
    public void onClick(View view) {
        if(view.getId()==R.id.input_button){
            get_music();
        } else if (view.getId()==R.id.ssl_button) {
            ssl_button_i=(ssl_button_i+1)%3;
            switch (ssl_button_i) {
                case 0 -> {
                    ssl_button.setImageResource(R.drawable.ic_play_btn_single);
                    song_player.setLooping(true);
                }
                case 1 -> {
                    ssl_button.setImageResource(R.drawable.ic_play_btn_loop);
                    song_player.setLooping(false);
                }
                default -> {
                    ssl_button.setImageResource(R.drawable.ic_play_btn_shuffle);
                    song_player.setLooping(false);
                }
            }
        } else if (view.getId()==R.id.last_song) {
            if(ssl_button_i!=2){
                if(song_play_i==0){
                    play_music(R_sing_list.size()-1);
                } else {
                    play_music(song_play_i-1);
                }
            }else {
                Random random = new Random();
                int r = random.nextInt(R_sing_list.size());
                play_music(r);
            }
        } else if (view.getId()==R.id.play_pause) {
            pp_i=1-pp_i;
            if(pp_i==0){
                play_pause.setImageResource(R.drawable.ic_play_bar_btn_play);
                song_player.pause();
            } else {
                play_pause.setImageResource(R.drawable.ic_play_bar_btn_pause);
                song_player.start();
            }
        } else if (view.getId()==R.id.next_song) {
            if(ssl_button_i==2){
                Random random = new Random();
                int r = random.nextInt(R_sing_list.size());
                play_music(r);
            } else {
                song_player.stop();
                play_pause.setImageResource(R.drawable.ic_play_bar_btn_play);
                if ((song_play_i + 1) == R_sing_list.size()) {
                    play_music(0);
                } else {
                    play_music(song_play_i + 1);
                }
            }
        }
    }
    public void init(){
        sing_time_now=findViewById(R.id.sing_time_now);
        sing_time_end = findViewById(R.id.sing_time_end);
        sing_name_now=findViewById(R.id.sing_name_now);
        music_seekbar=findViewById(R.id.music_seekbar);
        music_seekbar.setOnSeekBarChangeListener(new My_seekbar());
        sing_list=findViewById(R.id.sing_list);
        input_button=findViewById(R.id.input_button);
        ssl_button=findViewById(R.id.ssl_button);
        last_song=findViewById(R.id.last_song);
        play_pause=findViewById(R.id.play_pause);
        next_song=findViewById(R.id.next_song);
        refresher=findViewById(R.id.refresher);
        input_button.setOnClickListener(this);
        ssl_button.setOnClickListener(this);
        last_song.setOnClickListener(this);
        play_pause.setOnClickListener(this);
        next_song.setOnClickListener(this);
    }
    public void main_function(){
        read_db();
        myAdapter=new MyAdapter();
        myAdapter.setOnItemClickListener(this::play_music);
        sing_list.setAdapter(myAdapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(MainActivity.this,RecyclerView.VERTICAL,false);
        sing_list.setLayoutManager(layoutManager);
        refresher.setOnRefreshListener(() -> {
            main_function();
            refresher.setRefreshing(false);
        });
    }
    public void play_music(int p){
        seek_on_change=true;
        song_play_i=p;
        Sing sing = R_sing_list.get(p);
        sing_name_now.setText(sing.getSing_name());
        sing_time_end.setText(sing.getSing_length_txt());
        song_player.reset();
        song_player.setAudioAttributes(new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
        try {
            song_player.setDataSource(sing.getPath());
            song_player.prepare();
            song_player.start();
            seek_on_change=false;
            song_player.setOnCompletionListener(mediaPlayer -> {
                seek_on_change=true;
                end_of_song(p);
            });
            music_seekbar.setMax(song_player.getDuration());
            play_pause.setImageResource(R.drawable.ic_play_bar_btn_pause);
            timer =new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (!seek_on_change){music_seekbar.setProgress(song_player.getCurrentPosition());}
                }
            },0,40);
        } catch (IOException e) {
            Toast.makeText(MainActivity.this,"qwq出问题了！",Toast.LENGTH_SHORT).show();
            throw new RuntimeException(e);
        }
    }
    public void end_of_song(int p){
        song_player.stop();
        play_pause.setImageResource(R.drawable.ic_play_bar_btn_play);
        switch (ssl_button_i) {
            case 0 -> play_music(p);
            case 1 -> {
                if ((p + 1) == R_sing_list.size()) {
                    play_music(0);
                } else {
                    play_music(p + 1);
                }
            }
            case 2 -> {
                Random random = new Random();
                int r = random.nextInt(R_sing_list.size());
                play_music(r);
            }
        }
    }
    public class My_seekbar implements SeekBar.OnSeekBarChangeListener{
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            String time_now = song_time_into_txt((long) seekBar.getProgress());
            sing_time_now.setText(time_now);
        }
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            seek_on_change=true;
        }
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            seek_on_change=false;
            song_player.seekTo(music_seekbar.getProgress());
        }
    }
    public void read_db(){
        My_sql_helper = new MySqlHelper(MainActivity.this,"music_db",null,1);
        sqliteDatabase = My_sql_helper.getReadableDatabase();
        R_sing_list = new ArrayList<>();
        Cursor cursor=sqliteDatabase.rawQuery("select * from sing_db ",null);
        if ((cursor != null) && cursor.moveToFirst()) {
            do {
                @SuppressLint("Range") String songName= cursor.getString(cursor.getColumnIndex("song_name"));
                @SuppressLint("Range") String time = cursor.getString(cursor.getColumnIndex("sing_length_txt"));
                @SuppressLint("Range") String singerName = cursor.getString(cursor.getColumnIndex("singer_name"));
                @SuppressLint("Range") String path = cursor.getString(cursor.getColumnIndex("path"));
                Sing sing = new Sing();
                sing.setSing_name(songName);
                sing.setPath(path);
                sing.setSinger_name(singerName);
                sing.setSing_length_txt(time);
                R_sing_list.add(sing);
            } while (cursor.moveToNext());
        }
        if (cursor != null) {cursor.close();}
    }
    public void get_music(){
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("audio/*"); // 只选择音频文件
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); // 允许选择多个文件
        filePickerLauncher.launch(intent);
    }
    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(),
        result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                Intent data = result.getData();
                if (data != null) {
                    ClipData clipData = data.getClipData();
                    if (clipData != null) {
                        int itemCount = clipData.getItemCount();
                        for (int i = 0; i < itemCount; i++) {
                            Uri uri = clipData.getItemAt(i).getUri();
                            String song_name = getSongName(uri);
                            String singer_name=getSingerName(uri);
                            String path = Environment.getExternalStorageDirectory().getPath()+"/自己的文件夹/歌/"+song_name;
                            String time = song_time_into_txt(getMusicDuration(uri));
                            Write_into_sql(song_name,singer_name,path,time);
                        }
                    }else {
                        Uri uri = data.getData();
                        String song_name = getSongName(uri);
                        String singer_name=getSingerName(uri);
                        String path = getSongPath(uri);
                        String time = song_time_into_txt(getMusicDuration(uri));
                        Write_into_sql(song_name,singer_name,path,time);
                    }
                }
            }
            read_db();
        }
    );
    public void Write_into_sql(String songName, String singerName, String path, String time){
        Cursor cursor=sqliteDatabase.rawQuery("select * from sing_db where song_name=?",new String[]{songName});
        ContentValues values = new ContentValues();
        values.put("song_name", songName);
        values.put("singer_name", singerName);
        values.put("sing_length_txt", time);
        values.put("path", path);
        if (cursor.getCount() == 0) {
            sqliteDatabase.insert("sing_db", null, values);
        }else {
            sqliteDatabase.update("sing_db",values,"song_name=?",new String[]{songName});
        }
        cursor.close();
    }
    public String song_time_into_txt(Long time_long){
        String time = "" ;
        long minute = time_long / 60000 ;
        long seconds = time_long % 60000 ;
        long second = Math.round((float)seconds/1000) ;
        if( minute < 10 ){time += "0" ;}
        time += minute+":" ;
        if( second < 10 ){time += "0" ;}
        time += second ;
        return time ;
    }
    private String getSongName(Uri uri) {
        String fileName = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if ((cursor != null) && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                fileName = cursor.getString(nameIndex);
                cursor.close();
            }
        } else if (uri.getScheme().equals("file")) {
            fileName = uri.getLastPathSegment();
        }
        return fileName;
    }
    private String getSongPath(Uri uri){
        return Environment.getExternalStorageDirectory().getPath()+"/自己的文件夹/歌/"+getSongName(uri);
    }
    private String getSingerName(Uri uri) {
        String[] projection = { MediaStore.Images.Media.ARTIST };
        String artist="null!";
        CursorLoader loader = new CursorLoader(MainActivity.this,uri, projection, null, null, null);
        try (Cursor cursor = loader.loadInBackground()) {
            if (cursor != null) {
                int column_index = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
                cursor.moveToFirst();
                artist = cursor.getString(column_index);
            }
            return artist;
        } catch (Exception e) {
            e.printStackTrace();
        }
        Toast.makeText(MainActivity.this,artist,Toast.LENGTH_SHORT).show();
        return artist;
    }
    private long getMusicDuration(Uri uri) {
        MediaPlayer mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(getApplicationContext(), uri);
            mediaPlayer.prepare();
            return mediaPlayer.getDuration();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            mediaPlayer.reset();
            mediaPlayer.release();
        }
        return 0;
    }
    class MyAdapter extends RecyclerView.Adapter<MyViewHolder> {
        private OnItemClickListener mItemClickListener;
        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.music_model, parent,false);
            return new MyViewHolder(view);
        }
        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
            Sing sing = R_sing_list.get(position);
            holder.sing_name.setText(sing.getSing_name());
            holder.singer_name.setText(sing.getSinger_name());
            holder.sing_length.setText(sing.getSing_length_txt());
            holder.itemView.setOnClickListener(v -> {
                if (mItemClickListener != null) {
                    mItemClickListener.onItemClick(position);
                }
            });
        }
        @Override
        public int getItemCount() {
            return R_sing_list.size();
        }
        public interface OnItemClickListener {
            void onItemClick(int position);
        }
        public void setOnItemClickListener(OnItemClickListener listener) {
            mItemClickListener = listener;
        }
    }
    static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView sing_name,singer_name,sing_length;
        LinearLayout sing_list;
        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            sing_name = itemView.findViewById(R.id.sing_name);
            singer_name = itemView.findViewById(R.id.singer_name);
            sing_length = itemView.findViewById(R.id.sing_length);
            sing_list = itemView.findViewById(R.id.sing_list);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                main_function();
            } else {
                Toast.makeText(MainActivity.this,"不给权限就别用这个软件！",Toast.LENGTH_SHORT).show();
                openAppSettings();
            }
        }
    }
    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }//打开设置
}
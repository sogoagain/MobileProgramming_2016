package io.github.sogoesagain.simple_mp3player;

import android.app.*;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

/**
 * Created by sogoesagain on 2016. 11. 27..
 */

public class MusicService extends Service {

    // Binder 클래스를 상속 받는 클래스를 정의
    // getService() 메소드에서 현재 서비스 객체를 반환
    class LocalBinder extends Binder {
        // 클라이언트가 호출할 수 있는 공개 메소드가 있는 현재 Service 객체 반환
        MusicService getService() {
            return MusicService.this;
        }
    }
    // 위에서 정의한 Binder 클래스의 객체 생성
    // Binder 클래스는 Interface인 IBinder를 구현한 클래스
    private final IBinder m_binder = new LocalBinder();

    // < 콜백 인터페이스 ICallback 선언 >
    // 엑티비티에서 ICallback 객체를 생성해 액티비티와 서비스가 바운딩 되었을 때,
    // MusicService의 registerCallback()메소드를 이용해 액티비티에서 생성된 ICallback객체를 받아온다.
    // 이를 통해 실행중인 서비스에서 액티비티의 내용을 변경할 수 있다.
    // 현재 프로그램에서는 updateTitleTextView() 메소드를 구현해 서비스에서 한 노래가 끝나고
    // 자동으로 다음 노래로 넘어가면 해당 노래 인덱스를 엑티비티에 넘겨준 뒤, 액티비티에 표시되는 노래 제목을 변경한다.
    interface ICallback {
        void updateTitleTextView(int index); //액티비티에서 선언한 콜백 함수.
    }
    // 위에서 정의한 ICallback의 참조변수 선언.
    // 향후 액티비티와 바인딩 되었을 때, 엑티비티에서 생성된 객체와 연결한다.
    private ICallback m_callback;


    // 디버깅을 위한 태그
    private static final String TAG = "MusicService";

    // 음악을 재생하기위한 MediaPlayer 객체
    private MediaPlayer m_musicPlayer = null;
    private File[] m_musicFiles;        // .mp3 파일들의 File 객체
    private int m_playingMusicIndex;    // 현재 재생중인 음원의 인덱스

    // < onCreate() >
    // 1. MediaPlayer() 객체 m_musicPlayer을 생성한다.
    // 2. m_musicPlayer에 노래가 끝나면 호출되는 이벤트에 대한 리스너 객체를 선언 및 등록한다.
    //    이 이벤트 리스너는 노래가 재생이 완료되면 다음 노래를 재생한다.
    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate()");
        // MediaPlayer 객체 생성
        m_musicPlayer = new MediaPlayer();
        m_musicPlayer.setLooping(false); // 반복재생 여부 설정

        // 노래 재생이 완료되면 호출되는 이벤트에 대한 리스너 객체를 선언 및 등록한다.
        m_musicPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                // 노래 재생이 완료되면 다음 노래를 재생한다.
                // 마지막곡의 다음곡은 처음곡이다.
                m_playingMusicIndex++;
                if(m_playingMusicIndex > m_musicFiles.length - 1) {
                    m_playingMusicIndex = 0;
                }

                // 해당하는 노래 인덱스에 맞춰 m_musicPlayer을 설정한다.
                setMusic(m_playingMusicIndex);
                // 노래를 재생한다.
                startMusic();

                // 액티비티로부터 m_callback 객체를 받았다면
                // 자동으로 다음 노래를 재생했을 때, 재생되는 음원의 인덱스를 액티비티에 보내준다.
                if(m_callback != null)
                    m_callback.updateTitleTextView(m_playingMusicIndex);
            }
        });
    }

    // < onDestroy() >
    // m_musicPlayer의 리소스를 정리한다.
    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");

        if(m_musicPlayer.isPlaying())
            m_musicPlayer.stop();
        m_musicPlayer.reset();
        m_musicPlayer.release();
        m_musicPlayer = null;
    }

    // < onStartCommand() >
    // 1. 액티비티에 의해 서비스가 실행됐을 경우 엑티비티로부터 재생할 노래의 인덱스와 mp3파일들의 File객체 배열을 받아온다.
    // 2. 재생해야할 노래를 재생한다.
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // intent: startService() 호출 시 넘기는 intent 객체
        // flags: service start 요청에 대한 부가 정보. 0, START_FLAG_REDELIVERY, START_FLAG_RETRY
        // startId: start 요청을 나타내는 unique integer id
        Log.d(TAG, "onStartCommand()");

        // 액티비티로부터 전달받은 인텐트에서 재생할 노래의 인덱스와 mp3파일들의 File객체 배열을 받아온다.
        try {
            m_musicFiles = (File[]) intent.getExtras().get("MUSIC_FILES");
            m_playingMusicIndex = intent.getIntExtra("MUSIC_INDEX", 0);
        } catch(Exception e) {
            Toast.makeText(this, "음원 리스트를 전달받는 중 오류가 생겼습니다.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            this.stopSelf();
        }

        // 전달받은 인덱스에 해당하는 음원을 재생한다.
        setMusic(m_playingMusicIndex);
        startMusic();

        // 인텐트로부터 mp3파일들의 File객체 배열을 받아오는데, 이는 음악 재생에 있어 중요하다.
        // 따라서 MusicService는 생존 유지가 중요할 뿐더러 intent정보가 서비스 동작에 중요한 역할을 한다.
        // 따라서 START_REDELIVER_INTENT값을 반환한다.
        return START_REDELIVER_INTENT;
    }

    // < 액티비티와 서비스가 바운딩되면 호출되는 onBind() >
    // m_binder 객체를 반환한다.
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind()");
        // 위에서 생성한 LocalBinder 객체를 반환
        return m_binder;
    }

    // < registerCallback() >
    // 액티비티에서 생성한 ICallback 객체를 받아온다.
    void registerCallback(ICallback cb) {
        m_callback = cb;
    }

    // < setMusic() >
    // 노래를 재생하기에 앞서 MediaPlayer객체인 m_musicPlayer를 노래 재생 이전단계까지 설정한다.
    // 1. 매개변수로 재생할 음원의 인덱스를 받아온다.
    // 2. MediaPlayer의 reset() 함수를 호출해 m_musicPlayer의 상태를 Idle 상태로 설정한다.
    // 3. 현재 재생할 음원의 파일 경로를 바탕으로 m_musicPlayer의 음원 소스를 등록한다.
    // 4. m_musicPlayer을 prepare 상태로 설정한다.
    // 5. 현재 재생중인 노래 정보를 보여주기 위해 Notification객체를 생성한다.
    // 6. 현재 실행중인 서비스를 Foreground로 실행하기 위해 startForeground()를 호출한다.
    void setMusic(int musicIndex) {
        m_playingMusicIndex = musicIndex;
        try {
            Log.i(TAG, "재생할 음원 파일 경로: " + m_musicFiles[m_playingMusicIndex].getAbsolutePath());

            String filepath = m_musicFiles[m_playingMusicIndex].getAbsolutePath();

            m_musicPlayer.reset();
            m_musicPlayer.setDataSource(filepath);
            m_musicPlayer.prepare();

            //***************************************
            // Service를 Foreground로 실행하기 위한 과정

            // 1. Notification 객체 생성
            // 1-1. Intent 객체 생성 - MainActivity 클래스를 실행하기 위한 Intent 객체
            Intent sintent = new Intent(this, MainActivity.class);
            // 1-2. Intent 객체를 이용하여 PendingIntent 객체를 생성 - Activity를 실행하기 위한 PendingIntent
            PendingIntent pIntent = PendingIntent.getActivity(this, 0, sintent, PendingIntent.FLAG_UPDATE_CURRENT);

            // 1-3. Notification 객체 생성
            Notification noti = new Notification.Builder(this)
                    .setContentTitle("Simple_MP3Player")
                    .setContentText(m_musicFiles[m_playingMusicIndex].getName().replace(".mp3", ""))
                    .setSmallIcon(R.drawable.ic_music_note_black_24dp)
                    .setContentIntent(pIntent)
                    .build();

            // 2. foregound service 설정 - startForeground() 메소드 호출, 위에서 생성한 nofication 객체 넘겨줌
            startForeground(123, noti);
            //****************************************

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // < startMusic() >
    // 현재 m_musicPlayer에 설정된 음원을 재생한다.
    void startMusic() {
        m_musicPlayer.start();
    }

    // < pauseMusic() >
    // 현재 m_musicPlayer에서 재생중인 음원을 일시정지한다.
    void pauseMusic() {
        m_musicPlayer.pause();
    }

    // < getPlayMusicTitle() >
    // 현재재생중인 음원의 제목을 반환한다.
    String getPlayMusicTitle() {
        return m_musicFiles[m_playingMusicIndex].getName().replace(".mp3","");
    }

    // < getIsPlaying() >
    // 1. 반환값 true: 현재 노래가 재생중임.
    // 2. 반환값 false: 현재 노래가 재생중이지 않음.
    boolean getIsPlaying() {
        return m_musicPlayer.isPlaying();
    }

    // < getMusicIndex() >
    // 현재 재생중인 노래의 인덱스를 반환.
    int getMusicIndex() {
        return m_playingMusicIndex;
    }
}

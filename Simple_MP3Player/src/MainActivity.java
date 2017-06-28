package io.github.sogoagain.simple_mp3player;

import android.Manifest;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.*;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";    // 디버깅을 위한 태그
    private final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1; // MUSIC 디렉토리 접근 권한 확인에 사용되는 상수

    private TextView m_musicTitle;    // 현재 재생중인 음악 제목을 표시해주는 텍스트 뷰
    private ImageButton m_startButton, m_pauseButton;   // 재생, 일시정지 버튼

    private ArrayList<String> m_listMusicTitle; // 노래파일명(확장자제외)들을 담고있는 ArrayList
    private File[] m_musicFiles;    // 공용 MUSIC 디렉터리 안에 있는 mp3 파일들
    private int m_numOfMusic;       // 음원 갯수
    private int m_selectedMusicIndex = 0;   // 현재 선택된 음원의 인덱스

    private MusicService m_service;         // 바운딩된 MusicService 객체
    private boolean m_bound = false;        // MusicService와 바운딩 여부

    // MusicService에서 아래의 updateTitleTextView 콜백 함수를 호출 할 수 있다.
    // updateTitleTextView는 MusicService에서 자동으로 다음 노래가 재생되면 현재 표시되고있는 음악 제목을 재생중인 음악의 제목으로 변경한다.
    private MusicService.ICallback m_callback = new MusicService.ICallback() {
        public void updateTitleTextView(int index) {
            // 현재 재생중인 노래 인덱스를 받아와 동기화하며 현재 재생중인 음악 제목을 변경한다.
            m_selectedMusicIndex = index;
            m_musicTitle.setText(m_listMusicTitle.get(m_selectedMusicIndex));
        }
    };

    // < ServiceConnection 인터페이스를 구현한 ServiceConnection 객체 생성 >
    private ServiceConnection m_connection = new ServiceConnection() {
        // Service에 연결(bound)되었을 때 호출되는 callback 메소드
        // 1. m_service 참조변수에 현재 실행중인 MusicService 객체를 연결한다.
        // 2. MusicService에서 자동으로 다음 노래가 재생되었을 때 MainActivity에 적절한 처리를 하기위해
        //    MusicService에서 정의한 MusicService.ICallback 객체를 등록한다.
        // 3. 엑티비티에 표시되고 있는 현재 재생중인 노래 제목을 서비스에서 재생하고있는 노래 제목으로 바꾼다.
        // 4. 재생되고 있는 노래의 인덱스를 받아와 엑티비티와 동기화한다.
        // 5. 현재 노래가 재생중이면 재생버튼을 일시정지 버튼으로 변경한다.
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected()");

            // 바운딩이 된것이므로 바운딩 여부를 나타내는 변수 m_Bound를 true로 설정한다.
            m_bound = true;

            // m_service 참조변수에 현재 실행중인 MusicService 객체 연결
            MusicService.LocalBinder binder = (MusicService.LocalBinder)service;
            m_service = binder.getService();

            // MusicService.ICallback 객체 등록
            m_service.registerCallback(m_callback); //콜백 등록

            // 엑티비티에서 재생중이라고 표시되고있는 제목을 MusicService에서 재생중인 제목으로 변경한다.
            // 재생중인 노래의 인덱스를 받아온다.
            m_musicTitle.setText(m_service.getPlayMusicTitle());
            m_selectedMusicIndex = m_service.getMusicIndex();

            // 현재 노래가 재생중이라면 재생버튼을 일시정지 버튼으로 변경한다.
            changeButtonStartToPause(m_service.getIsPlaying());
        }

        // Service 연결 해제되었을 때 호출되는 callback 메소드
        // 바운딩이 해제되는 경우는 정지버튼을 누르거나, 액티비티가 onStop()이 호출되었을 경우 2가지다.
        // 1. m_Bound를 false로 변경한다.
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected()");
            m_bound = false;
        }
    };


    // < musicListView 아이템 터치 이벤트 리스너 객체 생성 >
    // 1. 기본적인 동작은 리스트 뷰에서 노래가 하나 선택되면 그 노래를 바로 재생한다.
    // 2. 따라서 선택된 노래 인덱스를 저장하고 서비스 상태를 체크해 실행중이지 않거나 바운딩 되지 않았다면 실행하거나 바운딩한다.
    // 3. 바운딩이 되어있다면 현재 인덱스에 있는 노래를 재생한다.
    // 4. 재생되는 노래 제목이 표시되는 뷰의 내용을 현재 노래로 변경한다.
    // 5. 노래가 재생되므로 재생버튼은 일시정지 버튼으로 변경한다.
    private AdapterView.OnItemClickListener onClickListItem = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Log.d(TAG, "onItemClick()");

            m_selectedMusicIndex =  position;   // 선택된 노래 인덱스를 저장
            startAndBindService();  // 서비스 상태를 체크하여 필요시 서비스를 실행시키거나 바운딩한다.
            if (m_bound) {          // 바운딩 되어 있다면 바로 MusicService의 메소드를 호출해 노래를 재생한다.
                m_service.setMusic(m_selectedMusicIndex);
                m_service.startMusic();
            }

            // 노래 제목과 재생버튼을 적절히 변경한다.
            m_musicTitle.setText(m_listMusicTitle.get(m_selectedMusicIndex));
            changeButtonStartToPause(true);
        }
    };

    // < onCreate() >
    // 1. 현재 재생중인 노래 제목을 표시해주는 m_musicTitle, 시작 버튼 m_startButton,
    //    일시정지 버튼 m_pauseButton을 실제 객체와 연결후 상태를 적절히 설정한다.
    // 2. Runtime permission check를 통해 MUSIC 디렉토리의 접근 권한을 사용자에게 요청한뒤,
    //    접근 권한이 주어지면 노래 리스트를 받아오고, 권한이 거부되면 권한이 필요하다는 토스트메세지를 띄운다.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 각 뷰들의 참조 변수와 실제 객체를 연결하고 기본적인 설정을 한다.
        m_musicTitle = (TextView) findViewById(R.id.tvMusicTitle);
        m_musicTitle.setSelected(true);
        m_startButton = (ImageButton) findViewById(R.id.btStart);
        m_pauseButton = (ImageButton) findViewById(R.id.btPause);

        // 초기에 시작버튼은 보이고, 일시정지버튼은 숨겨지도록 한다.
        changeButtonStartToPause(false);

        //*******************************************************************
        // Runtime permission check
        // 1. 권한이 주어지면 MUSIC 디렉토리에서 음원 목록을 읽어온다.
        // 2. 권한이 거부되면 권한이 필요하다는 토스트메세지를 띄운다.
        //*******************************************************************
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                Toast.makeText(this, "노래 재생을 위해 파일 접근 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
                ImageButton tmpBtn = (ImageButton) findViewById(R.id.btNext);
                tmpBtn.setClickable(false);
                tmpBtn = (ImageButton) findViewById(R.id.btPrevious);
                tmpBtn.setClickable(false);
                tmpBtn = (ImageButton) findViewById(R.id.btStop);
                tmpBtn.setClickable(false);
                m_startButton.setClickable(false);
                m_pauseButton.setClickable(false);

            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
            }
        } else {
            // READ_EXTERNAL_STORAGE 권한이 있는 것이므로
            // Public Directory에 접근할 수 있고 거기에 있는 파일을 읽을 수 있다
            // MUSIC 저장소에 있는 노래 리스트를 getMusicList() 메소드를 통해 가져온다.
            this.getMusicList();
        }
        //*********************************************************************
    }

    // < onResume() >
    // 엑티비티가 다시 살아날 경우 적절한 처리를 한다.
    // 엑티비티가 다시 살아났을 때 MusicService가 실행되고 있다면 바로 바인딩을 시도한다.
    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume()");
        // 만약 서비스가 살아있다면 바로 바인딩시도
        if(isAliveService(MainActivity.this)) {
            if(!m_bound) {
                // Service에 연결하기 위해 bindService 호출, 생성한 intent 객체와 구현한 ServiceConnection의 객체를 전달
                bindService(new Intent(MainActivity.this, MusicService.class), m_connection, Context.BIND_AUTO_CREATE);
            }
        }
    }

    // < onStop() >
    // 서비스와 바운딩을 끊는다.
    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "onStop()");
        if(m_bound) {
            unbindService(m_connection);
            m_bound = false;
        }
    }

    // < onDestroy() >
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestory()");
    }

    // MUSIC 디렉터리 접근 권한 설정을 확인한뒤 적절한 처리를 한다.
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // read_external_storage-related task you need to do.

                    // READ_EXTERNAL_STORAGE 권한을 얻었으므로
                    // 관련 작업을 수행할 수 있다
                    // MUSIC 저장소에 있는 노래 리스트를 getMusicList() 메소드를 통해 가져온다.
                    this.getMusicList();
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.

                    // 권한을 얻지 못 하였으므로 파일 읽기를 할 수 없다
                    // 적절히 대처한다
                    Toast.makeText(this, "노래 재생을 위해 파일 접근 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
                    ImageButton tmpBtn = (ImageButton) findViewById(R.id.btNext);
                    tmpBtn.setClickable(false);
                    tmpBtn = (ImageButton) findViewById(R.id.btPrevious);
                    tmpBtn.setClickable(false);
                    tmpBtn = (ImageButton) findViewById(R.id.btStop);
                    tmpBtn.setClickable(false);
                    m_startButton.setClickable(false);
                    m_pauseButton.setClickable(false);
                }
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }


    // < getMusicList() >
    // MUSIC 디렉터리에 저장되어 있는 노래 들을 읽어 오는 메소드
    // 1. MUSIC 저장소에 있는 파일들 중 확장자가 .mp3인 파일들을 읽어들인다.
    // 2. 그 후, 파일명에서 확장자 .mp3를 노래 리스트를 보여주는 ListView에 목록을 뿌린다.
    private void getMusicList() {
        // .mp3 파일을 추출할 때 사용하는 FileFilter 객체
        // 뒤에 붙은 확장자가 .mp3인 것만 걸러낼때 사용한다.
        FilenameFilter mp3FileFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.endsWith(".mp3");
            }
        };

        // MUSIC 디렉토리를 읽어 리스트 뷰를 설정하고 .mp3파일들의 File객체 배열을 생성한다.
        try {
            // Public Directory 중에 Music 디렉토리에 대한 File 객체를 반환한다
            File musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);

            // .mp3파일만을 추출한다
            String[] strAryMP3File = musicDir.list(mp3FileFilter);

            // Music 디렉토리에서 .mp3 파일만을 추출해 m_musicFiles에 각 파일의 경로를 저장한다.
            m_musicFiles = musicDir.listFiles(mp3FileFilter);
            m_numOfMusic = m_musicFiles.length;

            // 아래 코드는 File 객체 배열의 길이만큼 for 루프를 돌면서 파일(혹은 디렉토리)의 이름을 로그로 출력한다
            for (int i = 0; i < m_numOfMusic; i++) {
                Log.i(TAG, "music directory file " + (i) + " : " + m_musicFiles[i].getName());
            }

            // 추출된 strAryMP3File 배열 항목들의 ".mp3" 확장자를 제거하여
            // ArrayList인 m_listMusicTitle에 모두 추가한뒤 반환한다.
            m_listMusicTitle = new ArrayList<>();

            for (String filename : strAryMP3File) {
                filename = filename.replace(".mp3", "");
                m_listMusicTitle.add(filename);
            }

            // simple_list_item_1을 layout으로 하고 musicListView 항목으로하는 어댑터 생성
            ArrayAdapter<String> listViewAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, m_listMusicTitle);
            // MUSIC 디렉터리에 있는 노래 파일들을 보여주는 ListView
            // musicListView 리스트 뷰에 어댑터를 연결한고 리스너를 설정한다.
            ListView musicListView = (ListView)findViewById(R.id.lvMusicList);
            try {
                musicListView.setAdapter(listViewAdapter);
                musicListView.setOnItemClickListener(onClickListItem);
            } catch(NullPointerException e) {
                e.printStackTrace();
            }
        }
        catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    // < onClick() >
    // 재생, 일시정지, 다음곡, 이전곡, 정지 버튼들에 대한 onClick 메소드
    // 1. 재생버튼: 먼저 서비스의 실행 여부, 바인딩 여부를 확인한 뒤 필요시 서비스를 실행 혹은 바인딩한다.
    //            ( 만일 서비스가 실행되지 않았다면 서비스 실행만 시키고 노래 재생은 하지 않는다. )
    //            ( startService()를 하면 서비스의 onStartCommand()에서 노래가 재생되기 때문이다. )
    //            서비스가 바인딩이 되어있다면 서비스의 메소드를 이용해 노래를 재생한다.
    //            이 버튼이 눌리면 노래가 재생이 되는 것이므로 일시정지버튼을 화면에 띄운다.
    // 2. 정지버튼: 1. 만일 서비스가 살아있다면 서비스를 종료시킨다.
    //            2. 재생되고있는 노래가 없으므로 초기 화면과 같게 만든다.
    // 3. 일시정지버튼: 1. 서비스가 바인딩 되어있다면 서비스의 메소드를 이용해 노래를 일시정지한다.
    //               2. 노래가 정지된 것이므로 재생버튼을 화면에 띄운다.
    // 4. 이전곡버튼: 1. 현재 재생중인 노래 인덱스보다 한단계 이전인 노래를 재생한다. (처음노래의 이전은 마지막노래다.)
    //             2. 노래가 재생중인 것이므로 일시정지버튼을 화면에 띄운다.
    // 5. 다음곡버튼: 1. 현재 재생중인 노래 인덱스보다 한단계 이후인 노래를 재생한다. (마지막노래 이후는 처음노래다.)
    //             2. 노래가 재생중인 것이므로 일시정지버튼을 화면에 띄운다.
    public void onClick(View view) {
        switch(view.getId()) {
            // 재생버튼
            case R.id.btStart:
                Log.d(TAG, "onClick() Start");

                // MusicService의 상태를 확인해 필요시 서비스를 실행 시키거나 서비스와 바운딩한다.
                startAndBindService();
                // 바운딩이 되어있다면 노래를 재생한다.
                if(m_bound) {
                    m_service.startMusic();
                }

                // 표시되는 노래 제목을 현재 재생중인 노래로 설정한다.
                m_musicTitle.setText(m_listMusicTitle.get(m_selectedMusicIndex));
                // 일시정지버튼을 화면에 띄운다.
                changeButtonStartToPause(true);
                break;
            // 정지버튼
            case R.id.btStop:
                Log.d(TAG, "onClick() Stop");

                // 서비스가 살아있다면 바운딩을 끊고 서비스를 종료한다.
                if(isAliveService(MainActivity.this)) {
                    if(m_bound) {
                        unbindService(m_connection);
                        m_bound = false;
                    }
                    stopService(new Intent(this, MusicService.class));
                }

                // 초기 화면으로 설정한다.
                m_musicTitle.setText("Shall we dance?");
                changeButtonStartToPause(false);
                break;
            // 일시정지버튼
            case R.id.btPause:
                Log.d(TAG, "onClick() Pause");
                // 바운딩 되어있으면 재생중인 노래를 일시정지한다.
                if(m_bound)
                    m_service.pauseMusic();
                changeButtonStartToPause(false);
                break;
            // 이전곡버튼
            case R.id.btPrevious:
                // 바운딩 되어 있다면
                if(m_bound) {
                    // 지금 재생중인 노래 바로 이전의 노래를 재생한다.
                    m_selectedMusicIndex--;
                    if(m_selectedMusicIndex < 0)    // 처음곡의 이전은 마지막곡이다.
                        m_selectedMusicIndex = m_numOfMusic - 1;
                    m_musicTitle.setText(m_listMusicTitle.get(m_selectedMusicIndex));
                    m_service.setMusic(m_selectedMusicIndex);
                    m_service.startMusic();

                    // 일시정지 버튼을 화면에 띄운다.
                    changeButtonStartToPause(true);
                }
                break;
            // 다음곡버튼
            case R.id.btNext:
                // 바운딩 되어 있다면
                if(m_bound) {
                    // 지금 재생중인 노래 바로 이후의 노래를 재생한다.
                    m_selectedMusicIndex++;
                    if(m_selectedMusicIndex > m_numOfMusic - 1)
                        m_selectedMusicIndex = 0;   // 마지막곡의 이후는 처음곡이다.
                    m_musicTitle.setText(m_listMusicTitle.get(m_selectedMusicIndex));
                    m_service.setMusic(m_selectedMusicIndex);
                    m_service.startMusic();

                    // 일시정지 버튼을 화면에 띄운다.
                    changeButtonStartToPause(true);
                }
                break;
        }
    }

    // < MusicService가 실행중인지 확인하는 isAliceService() 메소드 >
    /* 참고링크 : http://jhb.kr/324 */
    // MusicService가 실행중이면 true, 그렇지 못하면 false를 반환한다.
    private Boolean isAliveService(Context context) {
        // ActivityManager 객체를 이용해 현재 시스템에서 돌고있는 서비스들의 정보를 가져온다.
        ActivityManager activityManager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);

        // 현재 시스템에서 돌고있는 서비스들 중에 MusicService가 있다면 true를 반환한다.
        for (ActivityManager.RunningServiceInfo rsi : activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (MusicService.class.getName().equals(rsi.service.getClassName()))
                return true;
        }
        // 그렇지 않다면 false를 반환한다.
        return false;
    }

    // < startAndBindService() >
    // 1. MusicService가 실행중이지 않다면 서비스를 실행한다.
    // 2. MusicService와 바인딩 되어있지 않다면 바운딩을 시도한다.
    private void startAndBindService() {
        // 서비스가 실행중이지 않다면 현재 선택된 음악의 인덱스와 음원 파일들의 File객체를 인텐트에 담아 서비스를 실행한다.
        if(!isAliveService(MainActivity.this)) {
            Intent intent = new Intent(MainActivity.this, MusicService.class);
            intent.putExtra("MUSIC_INDEX", m_selectedMusicIndex);
            intent.putExtra("MUSIC_FILES", m_musicFiles);
            setResult(RESULT_OK, intent);
            startService(intent);
        }
        // 서비스와 바운딩되어있지 않다면 바운딩을 시도한다.
        if(!m_bound) {
            // Service에 연결하기 위해 bindService 호출, 생성한 intent 객체와 구현한 ServiceConnection의 객체를 전달
            bindService(new Intent(MainActivity.this, MusicService.class), m_connection, Context.BIND_AUTO_CREATE);
        }
    }

    // < changeButtonStartToPause() >
    // 1. 매개변수로 true를 주면 일시정지 버튼을 화면에 뿌리고
    // 2. 매개변수로 false를 주면 시작 버튼을 화면에 뿌린다.
    private void changeButtonStartToPause(boolean isPlaying) {
        if(isPlaying) {
            m_pauseButton.setVisibility(View.VISIBLE);
            m_startButton.setVisibility(View.INVISIBLE);
        } else {
            m_pauseButton.setVisibility(View.INVISIBLE);
            m_startButton.setVisibility(View.VISIBLE);
        }
    }
}

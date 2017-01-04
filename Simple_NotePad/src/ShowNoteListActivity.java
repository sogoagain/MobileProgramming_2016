package io.github.sogoesagain.simple_notepad;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

public class ShowNoteListActivity extends AppCompatActivity {
    static final int GET_NEW_NOTE_TITLE = 1;
    static final int GET_DELETE_NOTE_TITLE = 2;

    private ListView m_lvNoteList;              // 메모들을 순차적으로 보여주는 ListView
    private ArrayAdapter<String> m_adapter;     // ListView에 쓰일 ArrayAdapter
    private ArrayList<String> m_listNoteTitle;  // 메모 제목들을 담고있는 ArrayList


    /*
                < m_lvNoteList 리스트 뷰 아이템 터치 이벤트 리스너 >
    리스트 뷰에 있는 아이템 하나를 클릭하면 아이템의 이름을 ViewNoteActivity에 전달한다.
    그 후, ViewNoteActivity를 실행한다.
    */
    private AdapterView.OnItemClickListener onClickListItem = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            // m_lvNoteList의 한 아이템이 클릭되었을 때,
            // 클릭한 아이템의 이름을 Intent객체를 이용해
            // ViewNoteActivity로 전달하고 ViewNoteActivity 실행
            Intent intent = new Intent(ShowNoteListActivity.this, ViewNoteActivity.class);
            intent.putExtra("READ_NOTE_TITLE",m_adapter.getItem(position).toString());
            setResult(RESULT_OK, intent);

            // ViewNoteActivity를 실행할 때, 메모가 삭제되었을 때
            // 메모제목을 GET_DELETE_NOTE_TITLE을 request code로 받아온다.
            startActivityForResult(intent, GET_DELETE_NOTE_TITLE);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_note_list);

        // 내부 저장소에 저장된 메모 리스트를 getSavedNoteList() 메소드를 통해 m_listNoteTitle로 가져온다.
        m_listNoteTitle = this.getSavedNoteList();

        // simple_list_item_1을 layout으로 하고 m_listNoteTitle을 항목으로하는 어댑터 생성
        m_adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, m_listNoteTitle);

        // lvNoteList 리스트 뷰에 어댑터를 연결한고 리스너를 설정한다.
        m_lvNoteList = (ListView)findViewById(R.id.lvNoteList);
        m_lvNoteList.setAdapter(m_adapter);
        m_lvNoteList.setOnItemClickListener(onClickListItem);
    }

    /*
                < 메모 작성 액션 항목 추가 메소드 >
    */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // 메뉴 리소스를 팽찰하고 액션바에 메모 작성 항목을 추가한다.
        getMenuInflater().inflate(R.menu.list_action_bar, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /*
                < 메모 작성 액션 항목이 클릭되었을때 이벤트 처리 메소드 >
    메모 작성 액션 항목이 클릭되면 EditNoteActivity를 실행한다.
    EditNoteActivity로 부터 'GET_NOTE_TITLE'이라는 요청 코드를 통해 작성된 메모 제목을 전달받는다.
    */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_addNote:
                Intent intent = new Intent(ShowNoteListActivity.this, EditNoteActivity.class);
                startActivityForResult(intent, GET_NEW_NOTE_TITLE);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /*
                < 다른 엑티비티로부터 전달된 결과를 처리하는 메소드 >
     1. EditNoteActivity로 부터 전달된 결과
        - requestCode가 GET_NEW_NOTE_TITLE 일때, 전달받은 메모 제목을 리스트뷰 어댑터에 추가한다.
     2. ViewNoteActivity로 부터 전달된 결과
        - requestCode가 GET_DELETE_NOTE_TITLE 일때, 전달받은 삭제된 메모 제목을 리스트뷰 어댑터에서 삭제한다.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // EditNoteActivity를 통해 전달받은 메모 제목을 리스트뷰 어댑터에 추가한다
        if(requestCode == GET_NEW_NOTE_TITLE) {
            if(resultCode == RESULT_OK) {
                m_adapter.add(data.getStringExtra("NEW_NOTE_TITLE"));
            }
        }
        // ViewNoteActivity를 통해 전달받은 메모 제목을 리스트뷰 어댑터에서 삭제한다.
        else if(requestCode == GET_DELETE_NOTE_TITLE) {
            if(resultCode == RESULT_OK) {
                m_adapter.remove(data.getStringExtra("DELETE_NOTE_TITLE"));
            }
        }
        return;
    }

    /*
                < 내부 저장소에 저장되어 있는 메모 제목들을 가져오는 메소드 >
     내부 저장소에 있는 파일들 중 확장자가 .txt인 파일들을 읽어들인다.
     그 후, 파일명에서 확장자 .txt를 지워 ArrayList에 담은 후 반환한다.
     */
    private ArrayList<String> getSavedNoteList() {
        // getFilesDir()은 내부 저장소의 디렉터리 파일 객체를 반환한다
        File fileInternalDir = getFilesDir();

        // FilenameFilter 인터페이스를 사용하여 .txt 파일을 추출한다
        String[] strAryTxtFile = fileInternalDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.endsWith(".txt");
            }
        });

        // 추출된 strAryTxtFile 배열 항목들의 ".txt" 확장자를 제거하여
        // ArrayList인 listNoteTitle에 모두 추가한뒤 반환한다.
        ArrayList<String> listNoteTitle = new ArrayList<>();

        for(String filename : strAryTxtFile) {
            filename = filename.replace(".txt","");
            listNoteTitle.add(filename);
        }
        return listNoteTitle;
    }
}

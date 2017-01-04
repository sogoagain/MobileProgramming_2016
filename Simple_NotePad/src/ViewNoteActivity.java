package io.github.sogoesagain.simple_notepad;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created by sogoesagain on 2016. 10. 29..
 */
public class ViewNoteActivity extends AppCompatActivity {

    private String m_strNoteTitle;  // ShowNoteListActivity로 부터 전달받은 메모 제목
    private String m_strFilename;   // 읽어야할 텍스트 파일의 파일 명 (m_strNoteTitle + .txt)
    private TextView m_tvTitle;     // 메모 제목을 보여주는 텍스트 뷰
    private TextView m_tvContent;   // 메모 내용을 보여주는 텍스트 뷰

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_note);

        // 각 텍스트 뷰 객체 연결
        m_tvTitle = (TextView) findViewById(R.id.tvViewTitle);
        m_tvContent = (TextView) findViewById(R.id.tvViewContent);
        m_tvContent.setMovementMethod(new ScrollingMovementMethod());

        // ShowNoteListActivity로 부터 읽어야할 메모 제목을 전달 받는다.
        // 전달 받은 메모 제목뒤에 .txt를 붙여 m_strFilename에 저장한다.
        // 파일 제목을 전달받지 못했을 경우 사용자에게 오류 내용을 토스트메세지로 알려주고 엑티비티를 종료한다.
        try {
            Intent intent = getIntent();
            m_strNoteTitle = intent.getStringExtra("READ_NOTE_TITLE");
            m_strFilename = m_strNoteTitle + ".txt";
        } catch(Exception e) {
            Toast.makeText(this, "메모 제목을 읽지 못하였습니다.",Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            finish();
        }

        // 메모 제목을 보여준다.
        m_tvTitle.setText(m_strNoteTitle);

        // 내부저장소의 m_strFilename 파일을 읽어온다.
        // 읽어온 내용을 m_tvContent 텍스트뷰를 통해 보여준다.
        // 파일을 읽는데 실패하면 토스트메세지로 사용자에게 알려주고 Activity를 종료한다.
        try {
            // 파일을 열고 FileInputStream 객체를 연결
            // available() 메소드를 이용하여 파일의 사이즈를 얻어 byte[] buffer 생성
            FileInputStream fisNoteStream = openFileInput(m_strFilename);
            byte[] buffer = new byte[fisNoteStream.available()];
            fisNoteStream.read(buffer);

            m_tvContent.setText(new String(buffer));
            fisNoteStream.close();
        } catch (IOException e) {
            Toast.makeText(this, m_strFilename+"을 읽는데 실패했습니다.",Toast.LENGTH_SHORT).show();
            finish();
            e.printStackTrace();
        }
    }

    /*
                < 메모 삭제 액션 항목 추가 메소드 >
    */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // 메뉴 리소스를 팽창하고 액션바에 메모 삭제 항목을 추가한다.
        getMenuInflater().inflate(R.menu.view_action_bar, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /*
                < 메모 삭제 액션 항목이 클릭되었을때 이벤트 처리 메소드 >
    메모 삭제 액션 항목이 클릭되면 삭제를 확인하는 대화상자를 띄운다.
    */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            // 메모 삭제 액션 항목이 클릭되면 경고 대화상자를 띄운다.
            case R.id.action_deleteNote:
                DialogFragment myFragment = new DeleteDialogFragment();
                myFragment.show(getSupportFragmentManager(), "Confirm Delete");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /*
                < 해당 메모를 삭제하는 메소드 >
    삭제할 메모의 파일명을 받아와 deleteFile() 메소드를 이용해 삭제한다.
    삭제에 성공하면 토스트 메세지로 삭제 성공을 알리고 인텐트 객체를 통해 삭제한 메모 제목을 ShowNoteListActivity에 전달한다.
    삭제에 실패하면 토스트 메세지로 삭제 실패를 알린다.
    삭제 시도 후 엑티비티를 종료한다.
    */
    private void deleteCurrentNote() {
        if(deleteFile(m_strFilename)) {
            Toast.makeText(this, m_strNoteTitle + " 삭제", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent();
            intent.putExtra("DELETE_NOTE_TITLE",m_strNoteTitle);
            setResult(RESULT_OK, intent);
        }
        else {
            Toast.makeText(this, m_strNoteTitle + " 삭제 실패", Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    /*
                < DialogFragment를 이용하여 AletDialog 생성 >
    Builder 클래스를 이용하여 대화상자 구성한다.
    사용자가 삭제버튼을 누르면 deleteCurrentNote()메소드를 호출해 메모를 삭제한다.
    취소버튼을 누르면 Dialog를 종료한다.
    */
    public static class DeleteDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Builder 클래스를 이용하여 대화상자 구성
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("메모 삭제 확인")
                    .setMessage("정말로 해당 메모를 삭제하시겠습니까?")
                    .setPositiveButton("삭제", new DialogInterface.OnClickListener() {
                        // 대화상자에서 삭제 버튼을 눌렀을 때, 해당 메모 파일을 삭제하고 Activity를 종료한다.
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ((ViewNoteActivity) getActivity()).deleteCurrentNote();
                        }
                    })
                    .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                        // 대화상자에서 취소 버튼을 눌렀을 때
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
            // AlerDialog 생성 및 반환
            return builder.create();
        }
    }
}

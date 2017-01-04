package io.github.sogoesagain.simple_notepad;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by sogoesagain on 2016. 10. 29..
 */
public class EditNoteActivity extends AppCompatActivity {

    private String m_strFilename;               // 저장할 메모의 파일 이름
    private String m_strNoteTitle;              // 메모 제목
    private EditText m_etTitle, m_etContent;    // 사용자가 메모 제목과 내용을 작성하는 EditText 참조변수

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_note);

        // 각 EditText 참조변수에 객체 연결
        m_etTitle = (EditText)findViewById(R.id.etEditTitle);
        m_etContent = (EditText)findViewById(R.id.etEditContent);
    }

    /*
                < 저장 버튼의 onClick 메소드 >
    renameDuplicatFile()메소드를 통해 저장할 파일의 이름을 만든다.
    그 후 해당 파일이름으로 파일스트림 객체를 생성해 메모내용을 저장한다.
    메모의 작성 성공과 실패를 토스트메세지를 통해 사용자에게 알려준다.
    작성 성공시 메모제목은 MainActivity인 ShowNoteListActivity에 넘겨준다.
     */
    public void onClickSaveBtn(View v) {
        try {
            // 중복검사를 마친 메모제목을 받아오고, 메모제목에 ".txt"를 더한 메모파일명을 만든다.
            m_strNoteTitle = renameDuplicateFile();
            m_strFilename = m_strNoteTitle + ".txt";

            // 메모파일을 작성할 FileOutputStream 객체를 생성한다.
            FileOutputStream fosNoteStream = openFileOutput(m_strFilename, Context.MODE_PRIVATE);
            fosNoteStream.write(m_etContent.getText().toString().getBytes());
            fosNoteStream.close();

            // Intent객체를 통해 생성한 메모 제목을 ShowNoteListActivity에 넘겨준다.
            Intent intent = new Intent();
            intent.putExtra("NEW_NOTE_TITLE", m_strNoteTitle);
            setResult(RESULT_OK, intent);

            Toast.makeText(this, m_strNoteTitle+" 작성", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(EditNoteActivity.this, e.getMessage().toString(), Toast.LENGTH_SHORT).show();
            Toast.makeText(this, m_strNoteTitle+" 작성 실패", Toast.LENGTH_SHORT).show();
        }

        finish();
    }

    /*
                < 취소 버튼의 onClick 메소드 >
    취소 버튼을 누르면 사용자가 입력한 메모 제목과 내용을 지워 초기화한다.
     */
    public void onClickCancelBtn(View v) {
        m_etContent.setText("");
        m_etTitle.setText("");
    }

    /*
                < 메모 파일명이 중복되었을 경우 renaming하는 메소드 >
     저장된 메모 파일들의 이름과 현재 메모제목.txt를 비교해 일치하는 파일이 있으면
     "메모제목 (i)" 형식으로 새로운 메모제목을 할당한다.
     중복으로 인해 메모제목을 변경한 경우 토스트메세지로 사용자에게 알린다.
     새로운 메모제목을 반환한다.
     */
    private String renameDuplicateFile() {
        String[] savedFiles = fileList();
        String tmpNoteTitle;
        int i = 1;
        boolean isDuplicateFile = true;
        boolean isChangedName = false;

        // 사용자가 입력한 초기 메모제목을 받아온다.
        m_strNoteTitle = m_etTitle.getText().toString();
        tmpNoteTitle = m_strNoteTitle;

        // 중복검사를 하는 부분
        // 중복을 검사하고 중복되었을 경우에는 메모제목을 새롭게 할당한다.
        // 새롭게 할당된 메모제목에 대해서 중복검사를 다시 실시한다.
        while(isDuplicateFile) {
            for (String tmp : savedFiles) {
                if (tmp.equals(tmpNoteTitle + ".txt")) {
                    tmpNoteTitle = m_strNoteTitle + " (" + i + ")";
                    i++;
                    isDuplicateFile = true;
                    isChangedName = true;
                    break;
                }
                isDuplicateFile = false;
            }
        }

        // 중복으로 메모제목이 변경되었을 경우 토스트메세지로 사용자에게 알린다.
        if(isChangedName)
            Toast.makeText(EditNoteActivity.this, "메모 제목이 중복되어 제목을 "
                    + tmpNoteTitle + "으로 변경했습니다.", Toast.LENGTH_SHORT).show();

        // 중복검사를 거친 메모제목을 반환한다.
        return tmpNoteTitle;
    }
}

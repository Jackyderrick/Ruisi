package me.yluo.ruisiapp.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.util.Pair;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import me.yluo.ruisiapp.R;
import me.yluo.ruisiapp.myhttp.HttpUtil;
import me.yluo.ruisiapp.myhttp.ResponseHandler;
import me.yluo.ruisiapp.utils.RuisUtils;
import me.yluo.ruisiapp.widget.MyColorPicker;
import me.yluo.ruisiapp.widget.MySmileyPicker;
import me.yluo.ruisiapp.widget.MySpinner;
import me.yluo.ruisiapp.widget.emotioninput.EmotionInputHandler;

/**
 * Created by free2 on 16-8-4.
 * 编辑activity
 */
public class EditActivity extends BaseActivity implements View.OnClickListener {

    private EditText ed_title, ed_content;
    private ProgressDialog dialog;
    private MySpinner typeid_spinner;
    private MyColorPicker myColorPicker;
    private MySmileyPicker smileyPicker;
    private TextView tv_select_type;
    private List<Pair<String, String>> typeiddatas;
    private View type_id_container;
    private String typeId = "";
    private String pid, tid;
    private Map<String, String> params;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_topic);

        Bundle b = getIntent().getExtras();
        if (b != null) {
            pid = b.getString("PID");
            tid = b.getString("TID");
        } else {
            showToast("参数异常无法编辑");
        }

        myColorPicker = new MyColorPicker(this);
        smileyPicker = new MySmileyPicker(this);
        initToolBar(true, "编辑帖子");
        typeid_spinner = new MySpinner(this);
        typeiddatas = new ArrayList<>();


        View btnDone = addToolbarMenu(R.drawable.ic_done_black_24dp);
        btnDone.setOnClickListener(view -> {
            if (checkPostInput()) {
                dialog = new ProgressDialog(EditActivity.this);
                dialog.setMessage("提交中,请稍后......");
                dialog.show();
                start_post();
            }
        });

        findViewById(R.id.forum_container).setVisibility(View.GONE);
        type_id_container = findViewById(R.id.type_id_container);
        type_id_container.setVisibility(View.GONE);
        tv_select_type = findViewById(R.id.tv_select_type);
        tv_select_type.setOnClickListener(this);
        ed_title = findViewById(R.id.ed_title);
        ed_content = findViewById(R.id.ed_content);
        typeid_spinner.setListener((pos, v) -> {
            if (pos > typeiddatas.size()) {
                return;
            } else {
                typeId = typeiddatas.get(pos).first;
                tv_select_type.setText(typeiddatas.get(pos).second);
            }
        });
        final LinearLayout edit_bar = findViewById(R.id.edit_bar);
        for (int i = 0; i < edit_bar.getChildCount(); i++) {
            View c = edit_bar.getChildAt(i);
            if (c instanceof ImageView) {
                c.setOnClickListener(this);
            }
        }

        Spinner setSize = findViewById(R.id.action_text_size);
        setSize.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                //[size=7][/size]
                if (ed_content == null || (ed_content.getText().length() <= 0 && i == 0)) {
                    return;
                }
                handleInsert("[size=" + (i + 1) + "][/size]");
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });


        myColorPicker.setListener((pos, v, color) -> handleInsert("[color=" + color + "][/color]"));

        EmotionInputHandler handler = new EmotionInputHandler(ed_content, (enable, s) -> {
            if (enable) {
                btnDone.setVisibility(View.VISIBLE);
            } else {
                btnDone.setVisibility(View.INVISIBLE);
            }
        });

        smileyPicker.setListener(handler::insertSmiley);
        start_edit();
    }

    private void start_edit() {
        String url = "forum.php?mod=post&action=edit&tid=" + tid + "&pid=" + pid + "&mobile=2";
        HttpUtil.get(this, url, new ResponseHandler() {
            @Override
            public void onSuccess(byte[] response) {
                Document document = Jsoup.parse(new String(response));
                params = RuisUtils.getForms(document, "postform");
                String title = params.get("subject");
                if (TextUtils.isEmpty(title)) {
                    ed_title.setVisibility(View.GONE);
                } else {
                    ed_title.setText(title);
                }

                if (TextUtils.isEmpty(params.get("message"))) {
                    showToast("本贴不支持编辑！");
                    finish();
                }
                ed_content.setText(params.get("message"));

                Elements types = document.select("#typeid").select("option");
                for (Element e : types) {
                    typeiddatas.add(new Pair<>(e.attr("value"), e.text()));
                }
                if (typeiddatas.size() > 0) {
                    type_id_container.setVisibility(View.VISIBLE);
                    tv_select_type.setText(typeiddatas.get(0).second);
                    typeId = typeiddatas.get(0).first;
                } else {
                    type_id_container.setVisibility(View.GONE);
                }
            }

            @Override
            public void onFailure(Throwable e) {
                super.onFailure(e);
                showToast("网络错误");
            }
        });
    }

    private void start_post() {
        String url = "forum.php?mod=post&action=edit&extra=&editsubmit=yes&mobile=2&handlekey=postform&inajax=1";
        params.put("editsubmit", "yes");
        if (!TextUtils.isEmpty(typeId) && !typeId.equals("0")) {
            params.put("typeid", typeId);
        }

        params.put("subject", ed_title.getText().toString());
        params.put("message", ed_content.getText().toString());

        HttpUtil.post(this, url, params, new ResponseHandler() {
            @Override
            public void onSuccess(byte[] response) {
                String res = new String(response);
                Log.e("resoult", res);
                if (res.contains("帖子编辑成功")) {
                    dialog.dismiss();
                    showToast("帖子编辑成功");
                    Intent i = new Intent();
                    if (ed_title.getVisibility() == View.VISIBLE) {
                        i.putExtra("TITLE", ed_title.getText().toString());
                    }
                    i.putExtra("CONTENT", ed_content.getText().toString());
                    i.putExtra("PID", pid);
                    setResult(RESULT_OK, i);
                    EditActivity.this.finish();
                } else {
                    int start = res.indexOf("<p>");
                    String ss = res.substring(start + 3, start + 20);
                    postFail(ss);
                }
            }

            @Override
            public void onFailure(Throwable e) {
                super.onFailure(e);
                dialog.dismiss();
                showToast("网络错误");
            }
        });
    }

    private void handleInsert(String s) {
        int start = ed_content.getSelectionStart();
        Editable edit = ed_content.getEditableText();//获取EditText的文字
        if (start < 0 || start >= edit.length()) {
            edit.append(s);
        } else {
            edit.insert(start, s);//光标所在位置插入文字
        }
        //[size=7][/size]
        int a = s.indexOf("[/");
        if (a > 0) {
            ed_content.setSelection(start + a);
        }
    }

    @Override
    public void onClick(final View view) {
        switch (view.getId()) {
            case R.id.action_bold:
                handleInsert("[b][/b]");
                break;
            case R.id.action_italic:
                handleInsert("[i][/i]");
                break;
            case R.id.action_quote:
                handleInsert("[quote][/quote]");
                break;
            case R.id.action_color_text:
                myColorPicker.showAsDropDown(view, 0, 10);
                break;
            case R.id.action_emotion:
                ((ImageView) view).setImageResource(R.drawable.ic_edit_emoticon_accent_24dp);
                smileyPicker.showAsDropDown(view, 0, 10);
                smileyPicker.setOnDismissListener(() -> ((ImageView) view).setImageResource(R.drawable.ic_edit_emoticon_24dp));
                break;
            case R.id.action_backspace:
                int start = ed_content.getSelectionStart();
                int end = ed_content.getSelectionEnd();
                if (start == 0) {
                    return;
                }
                if ((start == end) && start > 0) {
                    start = start - 1;
                }
                ed_content.getText().delete(start, end);
                break;
            case R.id.tv_select_type:
                String[] names = new String[typeiddatas.size()];
                for (int i = 0; i < typeiddatas.size(); i++) {
                    names[i] = typeiddatas.get(i).second;
                }
                typeid_spinner.setData(names);
                typeid_spinner.setWidth(view.getWidth());
                typeid_spinner.showAsDropDown(view, 0, 15);
        }
    }

    private boolean checkPostInput() {
        if (!TextUtils.isEmpty(typeId) && typeId.equals("0")) {
            Toast.makeText(this, "请选择主题分类", Toast.LENGTH_SHORT).show();
            return false;
        } else if ((ed_title.getVisibility() == View.VISIBLE) && TextUtils.isEmpty(ed_title.getText().toString().trim())) {
            Toast.makeText(this, "标题不能为空", Toast.LENGTH_SHORT).show();
            return false;
        } else if (TextUtils.isEmpty(ed_content.getText().toString().trim())) {
            Toast.makeText(this, "内容不能为空", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    //发帖失败执行
    private void postFail(String str) {
        dialog.dismiss();
        showToast(str);
    }
}

package com.sunmi.printerservice.ui.widget;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.text.method.KeyListener;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import woyou.aidlservice.jiuiv5.R;


/**
 * Created by Administrator on 2016/6/14.
 */
public class EditTextDialog extends BaseEditDialog {

    private View clear;
    private TextView textline;

    public EditTextDialog(Context context, String textLeft, String textRight, String title, View.OnClickListener left, View.OnClickListener right, OnShowListener onShow){
    	dialog = new Dialog(context, R.style.defaultDialogStyle);
    	dialog.setContentView(R.layout.dialog_edit_text_9_16);
    	
        TextView tvMsg = (TextView) dialog.findViewById(R.id.title);
        TextView btnLeft = (TextView) dialog.findViewById(R.id.left);
        TextView btnRight = (TextView) dialog.findViewById(R.id.right);
        tvMsg.setText(title);
        btnLeft.setText(textLeft);
        btnRight.setText(textRight);
        btnLeft.setOnClickListener(left);
        btnRight.setOnClickListener(right);
        dialog.setOnShowListener(onShow);
        dialog.findViewById(R.id.clear).setVisibility(View.INVISIBLE);
        setDialog();
    }

    @Override
    protected void init() {
    	textline = (TextView) dialog.findViewById(R.id.linetext);
        editText = (EditText) dialog.findViewById(R.id.edit);
        clear = dialog.findViewById(R.id.clear);
        editText.addTextChangedListener(textWatcher);
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editText.setText("");
            }
        });
    }

    @Override
    protected void onDialogCancel() {
        editText.setText("");
    }


    TextWatcher textWatcher = new TextWatcher(){
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int start, int count, int after) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            if(editable.length()>0){
                clear.setVisibility(View.VISIBLE);
            }else{
                clear.setVisibility(View.INVISIBLE);
            }
        }
    };

    public void showKeyboard() {
        if(editText!=null){
            //设置可获得焦点
            editText.setFocusable(true);
            editText.setFocusableInTouchMode(true);
            //请求获得焦点
            editText.requestFocus();
            //调用系统输入法
            InputMethodManager inputManager = (InputMethodManager) editText
                    .getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.showSoftInput(editText, 0);
        }
    }

    /**
     * 设置输入框的文字
     * @param content
     */
    public void setText(String content){
        editText.setText(content);
        editText.setSelection(content.length());
    }

    public EditText getEditText(){
        return this.editText;
    }

    /**
     * 设置输入框的最大长度
     * @param num
     */
    public void setMaxLength(int num){
        editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(num)});
    }

    /**
     * 设置输入框提示文字
     * @param text
     */
    public void setHintText(String text){
        editText.setHint(text);
    }

    public void setKeyListen(KeyListener listen){
        editText.setKeyListener(listen);
    }
    
    /*
     * 展示警告
     */
    public void showAlert(boolean show){
    	if(show){
    		textline.setVisibility(View.VISIBLE);
    	}else{
    		textline.setVisibility(View.INVISIBLE);
    	}
    }
}
package com.sunmi.printerservice.ui.widget;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import woyou.aidlservice.jiuiv5.R;



/**
 * @author   by lj on 2016/6/14.
 */
public class ListDialog extends BaseDialog{

    private LinearLayout list;

    private ScrollView scrollView;

    private ItemClickListener itemClickListener;

    public ListDialog(Context context, String title, String[] items) {
    	dialog = new Dialog(context, R.style.defaultDialogStyle);
    	dialog.setContentView(R.layout.dialog_item_select_9_16);
    	dialog.getWindow().getAttributes().gravity = Gravity.CENTER; //居中
    	dialog.setCanceledOnTouchOutside(false); //点击空白不取消
    	dialog.setCancelable(false); //点击返回按钮不取消
        TextView tvMsg = (TextView) dialog.findViewById(R.id.title);
        tvMsg.setText(title);
        setDialog();
        setData(items);
    }


    public void setItemClickListener(ItemClickListener listener){
        this.itemClickListener = listener;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void init() {
        scrollView = (ScrollView) dialog.findViewById(R.id.scrollView);
        list = (LinearLayout) dialog.findViewById(R.id.list);
        View cancel =  dialog.findViewById(R.id.btn_cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cancel();
            }
        });
    }

    @Override
    protected void onDialogCancel() {
        dialog = null;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void setData(String[] items){
        list.removeAllViews();
        float itemHeight = getHeight();
        ViewGroup.LayoutParams layoutParams = (ViewGroup.LayoutParams) scrollView.getLayoutParams();
        int height = 0;
        int j = 0;
        for(int i=0;i<items.length;i++){
            String itemStr = items[i];
            View view = inflatView();
            TextView text = (TextView) view.findViewById(R.id.text);
            text.setText(itemStr);
            view.setTag(i);
            view.setOnClickListener(new ItemClick());
            list.addView(view);
            if(i<=3){
                height+=itemHeight;
                j = i;
            }

        }
        layoutParams.height = height+j+1;
        scrollView.setLayoutParams(layoutParams);
    }

    class ItemClick implements View.OnClickListener{

        @Override
        public void onClick(View view) {
            int pos = (int) view.getTag();
            if(itemClickListener!=null){
                itemClickListener.OnItemClick(pos);
            }
        }
    }

    public interface ItemClickListener{
        void OnItemClick(int position);
    }

    private  float getHeight() {
    	return resources.getDimension(R.dimen.itemHeight);
    }

    private View inflatView(){
        return View.inflate(dialog.getContext(), R.layout.item_text_9_16, null);
    }
}
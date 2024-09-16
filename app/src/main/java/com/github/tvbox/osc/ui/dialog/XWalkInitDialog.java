package com.github.tvbox.osc.ui.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.github.tvbox.osc.R;
import com.github.tvbox.osc.util.FastClickCheckUtil;
import com.github.tvbox.osc.util.XWalkUtils;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.FileCallback;
import com.lzy.okgo.model.Progress;
import com.lzy.okgo.model.Response;

import org.jetbrains.annotations.NotNull;

import java.io.File;

public class XWalkInitDialog extends BaseDialog {
    private OnListener listener;
    TextView downText;
    TextView downTip;

    public XWalkInitDialog(@NonNull @NotNull Context context) {
        super(context);
        setCanceledOnTouchOutside(false);
        setCancelable(true);
        setContentView(R.layout.dialog_xwalk);
        setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                OkGo.getInstance().cancelTag("down_xwalk");
            }
        });
        downText = findViewById(R.id.downXWalk);
        downTip = findViewById(R.id.downXWalkArch);

        downTip.setText("下载XWalkView运行组件\nArch:" + XWalkUtils.getRuntimeAbi());

        if (XWalkUtils.xWalkLibExist(context)) {
            downTip.setText("已安装XWalkView运行组件\nArch:" + XWalkUtils.getRuntimeAbi());
            downText.setText("重新下载");
        }else if(XWalkUtils.isXWalkZipExistsOnExternal(context)){
            downTip.setText("发现本地存储XWalkView运行组件\nArch:" + XWalkUtils.getRuntimeAbi());
            downText.setText("本地安装");
        }

        downText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FastClickCheckUtil.check(v);
                setTextEnable(false);
                if("重新下载".contentEquals(downText.getText())) {
                    downloadAndSetup(context);
                }else if("本地安装".contentEquals(downText.getText())){
                    externalStoreSetup(context);
                    downText.setText("安装完成");
                }
            }
        });
    }

    void externalStoreSetup(Context context){
        //如果存在sdcard的zip文件，则解开
        if(!XWalkUtils.xWalkLibExist(context) && XWalkUtils.isXWalkZipExistsOnExternal(context)) {
            Toast.makeText(context, "发现XWalk文件，正在安装...", Toast.LENGTH_LONG).show();
            XWalkUtils.extractXWalkZipOnExternal(context);
        }
    }

    void downloadAndSetup(Context context){
        OkGo.<File>get(XWalkUtils.downUrl()).tag("down_xwalk").execute(new FileCallback(context.getCacheDir().getAbsolutePath(), XWalkUtils.saveZipFile()) {
            @Override
            public void onSuccess(Response<File> response) {
                try {
                    XWalkUtils.unzipXWalkZip(context, response.body().getAbsolutePath());
                    XWalkUtils.extractXWalkLib(context);
                    XWalkUtils.deleteZipFile(response.body().getAbsolutePath());
                    downText.setText("重新下载");
                    if (listener != null)
                        listener.onchange();
                    dismiss();
                } catch (Throwable e) {
                    e.printStackTrace();
                    Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
                    setTextEnable(true);
                }
            }

            @Override
            public void onError(Response<File> response) {
                super.onError(response);
                Toast.makeText(context, response.getException().getMessage(), Toast.LENGTH_LONG).show();
                setTextEnable(true);
            }

            @Override
            public void downloadProgress(Progress progress) {
                super.downloadProgress(progress);
                downText.setText(String.format("%.2f%%", progress.fraction * 100));
            }
        });
    }

    private void setTextEnable(boolean enable) {
        downText.setEnabled(enable);
        downText.setTextColor(enable ? Color.BLACK : Color.GRAY);
    }

    public XWalkInitDialog setOnListener(OnListener listener) {
        this.listener = listener;
        return this;
    }

    public interface OnListener {
        void onchange();
    }
}
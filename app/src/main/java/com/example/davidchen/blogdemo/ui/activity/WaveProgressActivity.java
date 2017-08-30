package com.example.davidchen.blogdemo.ui.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.example.davidchen.blogdemo.R;
import com.example.davidchen.blogdemo.view.WaveProgressView;

/**
 * 测试波纹进度条
 * Created by DavidChen on 2017/8/30.
 */

public class WaveProgressActivity extends AppCompatActivity{

    private WaveProgressView wpv_git;

    final Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            float progress = wpv_git.getProgress();
            if (progress >= 100) {
                progress = 0;
            }
            wpv_git.setProgress(++progress);
            wpv_git.setText(progress + "%");
            handler.sendEmptyMessageDelayed(0, 100);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wave);
        wpv_git = (WaveProgressView) findViewById(R.id.wpv_git);
        handler.sendEmptyMessage(0);
    }
}

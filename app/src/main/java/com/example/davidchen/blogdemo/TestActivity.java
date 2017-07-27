package com.example.davidchen.blogdemo;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.BindView;
import com.example.OnClick;
import com.example.api.ButterKnife;

/**
 * 测试activity
 * Created by DavidChen on 2017/7/25.
 */

public class TestActivity extends AppCompatActivity {

    @BindView(R.id.btn_enter)
    public Button btn_enter;

    @BindView(R.id.tv_result)
    public TextView tv_result;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        ButterKnife.bind(this);
    }

    @OnClick({R.id.btn_enter, R.id.tv_result})
    public void click(View view) {
        switch (view.getId()) {
            case R.id.btn_enter:
                tv_result.setText("注入成功");
                break;
            case R.id.tv_result:
                Toast.makeText(TestActivity.this, "guin", Toast.LENGTH_SHORT).show();
                break;
        }
    }
    @OnClick({R.id.btn_test})
    public void click2(View view) {
        switch (view.getId()) {
            case R.id.btn_test:
                Toast.makeText(TestActivity.this, "test2", Toast.LENGTH_SHORT).show();
                break;
        }
    }
}

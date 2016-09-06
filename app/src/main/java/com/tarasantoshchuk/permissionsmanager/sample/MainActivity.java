package com.tarasantoshchuk.permissionsmanager.sample;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.tarasantoshchuk.permissionsmanager.R;

public class MainActivity extends Activity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_activity_with_request_on_create).setOnClickListener(this);
        findViewById(R.id.btn_activity_with_trigger).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Class<? extends Activity> targetActivity;
        switch (v.getId()) {
            case R.id.btn_activity_with_request_on_create:
                targetActivity = ActivityWithStartRequest.class;
                break;
            case R.id.btn_activity_with_trigger:
                targetActivity = ActivityWithTrigger.class;
                break;
            default:
                return;
        }
        Intent intent = new Intent(MainActivity.this, targetActivity);
        startActivity(intent);
    }
}

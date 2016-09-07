package com.tarasantoshchuk.permissionsmanager.sample;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.tarasantoshchuk.permissionsmanager.PermissionsManager;
import com.tarasantoshchuk.permissionsmanager.R;
import com.tarasantoshchuk.permissionsmanager.Request;

public class ActivityWithTrigger extends Activity {
    private Request mPermissionRequest;
    private boolean isRestored;
    private AlertDialog mRationale;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_with_trigger);

        findViewById(R.id.request_trigger).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPermissionRequest.run(ActivityWithTrigger.this);
            }
        });

        isRestored = savedInstanceState != null;
    }

    @Override
    protected void onStop() {
        super.onStop();
        mPermissionRequest.stop();

        if (mRationale != null && mRationale.isShowing()) {
            mRationale.dismiss();

            //uncomment if you want rationale request to be restored
            //mPermissionRequest.reset();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mPermissionRequest = PermissionsManager.init(this).createRequestAll(R.id.request_on_button_click, isRestored, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        mPermissionRequest.setListener(new Request.Listener() {
            @Override
            public boolean onShowRationale() {
                mRationale = new AlertDialog.Builder(ActivityWithTrigger.this)
                        .setTitle("Rationale title")
                        .setMessage("Rationale message")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                mPermissionRequest.proceed();
                            }
                        })
                        .setCancelable(true)
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                mPermissionRequest.reset();
                            }
                        })
                        .show();
                return false;
            }

            @Override
            public void onRequestResult(@Request.Result int result, int requestCode, String... permissions) {
                String message;
                switch(result) {
                    case Request.RESULT_GRANTED:
                        message = "permission granted";
                        break;
                    case Request.RESULT_DENIED:
                        message = "permission denied";
                        break;
                    case Request.RESULT_DENIED_FOREVER:
                        message = "permission denied forever";
                        break;
                    default:
                        throw new RuntimeException("unexpected");
                }

                Toast.makeText(ActivityWithTrigger.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}

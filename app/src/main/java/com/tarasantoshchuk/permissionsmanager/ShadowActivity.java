package com.tarasantoshchuk.permissionsmanager;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;

@TargetApi(Build.VERSION_CODES.M)
public class ShadowActivity extends Activity {
    private static final String KEY_REQUEST_CODE = "KEY_REQUEST_CODE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            int requestCode = retrieveRequestCode(getIntent().getExtras());
            requestPermissions(requestCode);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void requestPermissions(int requestCode) {
        Request request = PermissionsManager.getInstance().getRequest(requestCode);

        requestPermissions(request.requestedPermissions.toArray(new String[0]), requestCode);

        request.setState(Request.STATE_REQUESTED);
    }

    private int retrieveRequestCode(Bundle extras) {
        if (!extras.containsKey(KEY_REQUEST_CODE)) {
            throw new RuntimeException("unexpected");
        }

        return extras.getInt(KEY_REQUEST_CODE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        PermissionsManager.getInstance().saveState();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        boolean[] shouldShowRationale = new boolean[permissions.length];

        for (int i = 0; i < permissions.length; i++) {
            shouldShowRationale[i] = shouldShowRequestPermissionRationale(permissions[i]);
        }

        PermissionsManager.getInstance().handleRequestResult(requestCode, permissions, grantResults, shouldShowRationale);
        finish();
    }

    public static Intent getStartIntent(Context mContext, int requestCode) {
        Intent intent = new Intent(mContext, ShadowActivity.class);
        Bundle bundle = new Bundle();
        bundle.putInt(KEY_REQUEST_CODE, requestCode);
        intent.putExtras(bundle);
        return intent;
    }
}

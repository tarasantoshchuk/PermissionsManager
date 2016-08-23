package com.tarasantoshchuk.permissionsmanager;

import android.app.Activity;
import android.support.annotation.IntDef;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Request implements Serializable {
    final int requestCode;

    @RequestMode
    final int requestMode;

    final List<String> requestedPermissions;

    @State
    private int state;

    @Result
    private int result;

    private Listener mListener;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({REQUEST_MODE_ALL, REQUEST_MODE_EACH})
    public @interface RequestMode {}
    public static final int REQUEST_MODE_ALL = 0;
    public static final int REQUEST_MODE_EACH = 1;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({RESULT_GRANTED, RESULT_DENIED, RESULT_DENIED_FOREVER})
    public @interface Result {}
    public static final int RESULT_GRANTED = 0;
    public static final int RESULT_DENIED = 1;
    public static final int RESULT_DENIED_FOREVER = 2;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({STATE_BEFORE_REQUEST, STATE_FINISHED, STATE_INIT, STATE_RATIONALE, STATE_REQUESTED, STATE_STARTED})
    public @interface State {}
    public static final int STATE_INIT = 0;
    public static final int STATE_STARTED = 1;
    public static final int STATE_RATIONALE = 2;
    public static final int STATE_BEFORE_REQUEST = 3;
    public static final int STATE_REQUESTED = 4;
    public static final int STATE_FINISHED = 5;

    Request(int requestCode, @RequestMode int requestMode, String[] permissions) {
        this.requestCode = requestCode;
        this.requestMode = requestMode;
        ArrayList<String> array = new ArrayList<>();

        for (int i = 0; i < permissions.length; i++) {
            array.add(i, permissions[i]);
        }

        requestedPermissions = Collections.unmodifiableList(array);
    }

    public boolean isRunning() {
        return state >= STATE_STARTED;
    }

    public boolean isFinished() {
        return state == STATE_FINISHED;
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public void stop() {
        if (mListener != null) {
            mListener = null;
        }
    }

    public void run(Activity activity) {
        PermissionsManager.getInstance().runRequest(this, activity);
    }

    void setState(@State int state) {
        this.state = state;

        switch(state) {
            case STATE_RATIONALE:
                if(mListener.onShowRationale()) {
                    proceed();
                }
                break;
            case STATE_FINISHED:
                mListener.onRequestResult(result, requestCode, requestedPermissions.toArray(new String[requestedPermissions.size()]));
                break;
            case STATE_BEFORE_REQUEST:
            case STATE_INIT:
            case STATE_STARTED:
            case STATE_REQUESTED:
                //ok, just do nothing
                break;
            default:
                throw new RuntimeException("unexpected");
        }
    }

    public void proceed() {
        PermissionsManager.getInstance().proceedRequest(this);
    }

    void setResult(@Result int result) {
        this.result = result;
    }

    public interface Listener {
        boolean onShowRationale();
        void onRequestResult(@Result int result, int requestCode, String... permissions);
    }

    public static String toJson(Request request) {
        JSONObject json = new JSONObject();
        try {
            json.put("requestCode", request.requestCode);
            json.put("requestMode", request.requestMode);

            for (String permission: request.requestedPermissions) {
                json.accumulate("permissions", permission);
            }
            return json.toString();
        } catch (JSONException e) {
            return null;
        }
    }

    @SuppressWarnings("WrongConstant")
    public static Request fromJson(String jsonStr) {
        JSONObject json = null;
        try {
            json = new JSONObject(jsonStr);
            int requestCode = json.getInt("requestCode");
            int requestMode = json.getInt("requestMode");
            JSONArray permissionsJson = json.getJSONArray("permissions");
            String[] permissions = new String[permissionsJson.length()];

            for (int i = 0; i < permissionsJson.length(); i++) {
                permissions[i] = permissionsJson.get(i).toString();
            }

            return new Request(requestCode, requestMode, permissions);
        } catch (JSONException e) {
            return null;
        }
    }
}

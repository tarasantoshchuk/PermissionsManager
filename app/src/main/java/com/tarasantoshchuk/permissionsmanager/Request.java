package com.tarasantoshchuk.permissionsmanager;

import android.app.Activity;
import android.support.annotation.IntDef;

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

    private transient Listener mListener;

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

    Request(int requestCode, @RequestMode int requestMode, String... permissions) {
        this.requestCode = requestCode;
        this.requestMode = requestMode;
        ArrayList<String> array = new ArrayList<>();

        for (int i = 0; i < permissions.length; i++) {
            array.add(i, permissions[i]);
        }

        requestedPermissions = Collections.unmodifiableList(array);

        state = STATE_INIT;
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
        setState(STATE_STARTED);
        PermissionsManager.getInstance().runRequest(this, activity);
    }

    void setState(@State int state) {
        this.state = state;
    }

    public void proceed() {
        PermissionsManager.getInstance().proceedRequest(this);
    }

    void setResult(@Result int result) {

    }

    public interface Listener {
        boolean onShowRationale();
        void onRequestResult(@Result int result, String... permissions);
    }
}

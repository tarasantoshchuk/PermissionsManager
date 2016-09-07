package com.tarasantoshchuk.permissionsmanager;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class PermissionsManager {
    private static final String TAG = PermissionsManager.class.getSimpleName();

    static final String SHARED_PREFS_FILE = "com.tarasnantoshchuk.permissionsmanager.PermissionsManager";
    private static final String PREFS_KEY_REQUEST_JSONS = "PREFS_KEY_REQUEST_JSONS";

    Request getRequest(int requestCode) {
        if (!mPendingRequests.containsKey(requestCode)) {
            throw new RuntimeException("unexpected");
        }

        return mPendingRequests.get(requestCode);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void saveState() {
        mContext
                .getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE)
                .edit()
                .putStringSet(PREFS_KEY_REQUEST_JSONS, getRequestsJsons())
                .commit();
    }

    private Set<String> getRequestsJsons() {
        Set<String> result = new HashSet<>();
        for (Request request: mPendingRequests.values()) {
            result.add(Request.toJson(request));
        }

        Log.v(TAG, "saveState, result " + result);
        return result;
    }

    public void handleRequestResult(int requestCode, String[] permissions, int[] grantResults, boolean[] shouldShowRationale) {
        Request request = mPendingRequests.get(requestCode);

        if (request == null) {
            throw new RuntimeException("unexpected");
        }

        @Request.Result int result;

        switch (request.requestMode) {
            case Request.REQUEST_MODE_ALL:
                result = Request.RESULT_GRANTED;
                for (int i = 0; i < grantResults.length; i++) {
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        result = Request.RESULT_DENIED;
                        if (!shouldShowRationale[i]) {
                            result = Request.RESULT_DENIED_FOREVER;
                            break;
                        }
                    }
                }
                break;
            case Request.REQUEST_MODE_EACH:
                throw new RuntimeException("not supported yet");
            default:
                throw new RuntimeException("unexpected");
        }

        request.setResult(result);
        request.setState(Request.STATE_FINISHED);
        request.reset();

        saveState();
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({REQUEST_STATUS_DENIED_FOREVER, REQUEST_STATUS_GRANTED, REQUEST_STATUS_UNKNOWN, REQUEST_STATUS_UNKNOWN_SHOW_RATIONALE})
    private @interface RequestStatus {}
    private static final int REQUEST_STATUS_GRANTED = 0;
    private static final int REQUEST_STATUS_DENIED_FOREVER = 1;
    private static final int REQUEST_STATUS_UNKNOWN = 2;
    private static final int REQUEST_STATUS_UNKNOWN_SHOW_RATIONALE = 3;


    static PermissionsManager sInstance;

    private Context mContext;

    HashMap<Integer, Request> mPendingRequests = new HashMap<>();

    public static synchronized PermissionsManager init(Context context) {
        if (sInstance == null) {
            sInstance = new PermissionsManager(context);
        }

        return sInstance;
    }

    static PermissionsManager getInstance() {
        return sInstance;
    }

    private PermissionsManager(Context context) {
        mContext = context.getApplicationContext();

        if (isMarshmallow()) {
            retrieveRequests();
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void retrieveRequests() {
        Set<String> requestJsons = mContext
                .getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE)
                .getStringSet(PREFS_KEY_REQUEST_JSONS, new HashSet<String>());

        Log.v(TAG, "retrieveRequests, requestJsons " + requestJsons);

        for (String requestJson : requestJsons) {
            Request request = Request.fromJson(requestJson);
            if (request != null) {
                mPendingRequests.put(request.requestCode, request);
            }
        }
    }

    public Request createRequestEach(int requestCode, String... permissions) {
        throw new RuntimeException("Not supported yet");
//      return createRequest(requestCode, Request.REQUEST_MODE_EACH, permissions);
    }

    public Request createRequestAll(int requestCode, boolean isRestored, String... permissions) {
        return createRequest(requestCode, isRestored, Request.REQUEST_MODE_ALL, permissions);
    }

    private Request createRequest(int requestCode, boolean isRestored, @Request.RequestMode int requestMode, String... permissions) {
        if (isRestored && mPendingRequests.containsKey(requestCode)) {
            return mPendingRequests.get(requestCode);
        } else {
            return createAndCacheRequest(requestCode, requestMode, permissions);
        }
    }

    @NonNull
    private Request createAndCacheRequest(int requestCode, @Request.RequestMode int requestMode, String[] permissions) {
        Request newRequest = new Request(requestCode, requestMode, permissions);
        newRequest.setState(Request.STATE_INIT);
        mPendingRequests.put(requestCode, newRequest);
        return newRequest;
    }

    void runRequest(Request request, Activity activity) {
        Log.v(TAG, "runRequest, request " + request);
        if (request.isRunning()) {
            return;
        }

        request.setState(Request.STATE_STARTED);
        @RequestStatus int requestStatus = getRequestStatus(request, activity);

        switch(requestStatus) {
            case REQUEST_STATUS_DENIED_FOREVER:
                request.setResult(Request.RESULT_DENIED_FOREVER);
                request.setState(Request.STATE_FINISHED);
                request.reset();
                break;
            case REQUEST_STATUS_GRANTED:
                request.setResult(Request.RESULT_GRANTED);
                request.setState(Request.STATE_FINISHED);
                request.reset();
                break;
            case REQUEST_STATUS_UNKNOWN_SHOW_RATIONALE:
                request.setState(Request.STATE_RATIONALE);
                break;
            case REQUEST_STATUS_UNKNOWN:
                request.setState(Request.STATE_BEFORE_REQUEST);
                launchRequest(request);
                break;
            default:
                throw new RuntimeException("unexpected");
        }
    }

    void proceedRequest(Request request) {
        request.setState(Request.STATE_BEFORE_REQUEST);
        launchRequest(request);
    }

    private void launchRequest(Request request) {
        Intent intent = ShadowActivity.getStartIntent(mContext, request.requestCode);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
    }

    @RequestStatus
    private int getRequestStatus(Request request, Activity activity) {
        if (request.requestMode == Request.REQUEST_MODE_ALL) {
            return getRequestStatusForAll(request, activity);
        } else {
            return getRequestStatusForEach(request, activity);
        }
    }

    @RequestStatus
    private int getRequestStatusForAll(Request request, Activity activity) {
        boolean hasDenied = false;
        boolean shouldShowRationale = false;

        for (String permission: request.requestedPermissions) {
            if (!isGranted(permission)){
                hasDenied = true;
            }

            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                shouldShowRationale = true;
            }
        }

        if (!hasDenied) {
            return REQUEST_STATUS_GRANTED;
        } else {
            if (shouldShowRationale) {
                return REQUEST_STATUS_UNKNOWN_SHOW_RATIONALE;
            } else {
                return REQUEST_STATUS_UNKNOWN;
            }
        }
    }

    @RequestStatus
    private int getRequestStatusForEach(Request request, Activity activity) {
        boolean allGranted = true;
        boolean shouldShowRationale = false;

        for (String permission: request.requestedPermissions) {
            if (!isGranted(permission)){
                allGranted = false;
            }

            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                shouldShowRationale = true;
            }
        }

        if (allGranted) {
            return REQUEST_STATUS_GRANTED;
        } else {
            if (shouldShowRationale) {
                return REQUEST_STATUS_UNKNOWN_SHOW_RATIONALE;
            } else {
                return REQUEST_STATUS_UNKNOWN;
            }
        }
    }

    private boolean isMarshmallow() {
        return Build.VERSION_CODES.M <= Build.VERSION.SDK_INT;
    }

    private boolean isGranted(String permission) {
        return !isMarshmallow() || isGranted_(permission);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean isGranted_(String permission) {
        return mContext.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean isDeniedForever(String permission, Activity activity) {
        return isMarshmallow() && isDeniedForever_(permission, activity);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean isDeniedForever_(String permission, Activity activity) {
        return activity.shouldShowRequestPermissionRationale(permission);
    }
}

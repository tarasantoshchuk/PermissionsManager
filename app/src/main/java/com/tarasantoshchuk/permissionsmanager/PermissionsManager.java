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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class PermissionsManager {
    static final String SHARED_PREFS_FILE = "com.tarasnantoshchuk.permissionsmanager.PermissionsManager";
    private static final String PREFS_KEY_REQUEST_JSONS = "PREFS_KEY_REQUEST_JSONS";

    public Request getRequest(int requestCode) {
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
        return result;
    }

    public void handleRequestResult(int requestCode, String[] permissions, int[] grantResults) {
        Request request = mPendingRequests.get(requestCode);

        if (request == null) {
            throw new RuntimeException("unexpected");
        }


        //compute request result

        mPendingRequests.remove(request.requestCode);
        request.setResult(Request.RESULT_DENIED_FOREVER);
        request.setState(Request.STATE_FINISHED);

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
        mContext = context;

        if (isMarshmallow()) {
            retrieveRequests();
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void retrieveRequests() {
        Set<String> requestJsons = mContext
                .getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE)
                .getStringSet(PREFS_KEY_REQUEST_JSONS, new HashSet<String>());

        for (String requestJson : requestJsons) {
            Request request = Request.fromJson(requestJson);
            mPendingRequests.put(request.requestCode, request);
        }
    }

    public Request createRequestEach(int requestCode, String... permissions) {
        throw new RuntimeException("Not supported yet");
//      return createRequest(requestCode, Request.REQUEST_MODE_EACH, permissions);
    }

    public Request createRequestAll(int requestCode, String... permissions) {
        return createRequest(requestCode, Request.REQUEST_MODE_ALL, permissions);
    }

    private Request createRequest(int requestCode, @Request.RequestMode int requestMode, String... permissions) {

        if (mPendingRequests.containsKey(requestCode)) {
            return mPendingRequests.get(requestCode);
        } else {
            return createAndCacheRequest(requestCode, requestMode, permissions);
        }
    }

    @NonNull
    private Request createAndCacheRequest(int requestCode, @Request.RequestMode int requestMode, String[] permissions) {
        Request newRequest = new Request(requestCode, requestMode, permissions);
        newRequest.setState(Request.STATE_STARTED);
        mPendingRequests.put(requestCode, newRequest);
        return newRequest;
    }

    void runRequest(Request request, Activity activity) {
        request.setState(Request.STATE_STARTED);
        @RequestStatus int requestStatus = getRequestStatus(request, activity);

        switch(requestStatus) {
            case REQUEST_STATUS_DENIED_FOREVER:
                request.setResult(Request.RESULT_DENIED_FOREVER);
                break;
            case REQUEST_STATUS_GRANTED:
                request.setResult(Request.RESULT_GRANTED);
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
        boolean hasDeniedForever = false;

        for (String permission: request.requestedPermissions) {
            if (isDeniedForever(permission)) {
                hasDenied = true;
                hasDeniedForever = true;
            } else if (!isGranted(permission)){
                hasDenied = true;
            }

            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                shouldShowRationale = true;
            }
        }

        if (hasDeniedForever) {
            return REQUEST_STATUS_DENIED_FOREVER;
        } else if (!hasDenied) {
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
        boolean allDeniedForever = true;

        for (String permission: request.requestedPermissions) {
            if (!isDeniedForever(permission)) {
                allDeniedForever = false;
            } else if (!isGranted(permission)){
                allGranted = false;
            }

            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                shouldShowRationale = true;
            }
        }

        if (allDeniedForever) {
            return REQUEST_STATUS_DENIED_FOREVER;
        } else if (allGranted) {
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

    private boolean isDeniedForever(String permission) {
        return isMarshmallow() && isDeniedForever_(permission);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean isDeniedForever_(String permission) {
        return mContext.getPackageManager().isPermissionRevokedByPolicy(permission, mContext.getPackageName());
    }
}

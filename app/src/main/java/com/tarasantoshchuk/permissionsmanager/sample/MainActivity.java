package com.tarasantoshchuk.permissionsmanager.sample;

        import android.app.Activity;
        import android.os.Bundle;

        import com.tarasantoshchuk.permissionsmanager.PermissionsManager;
        import com.tarasantoshchuk.permissionsmanager.R;
        import com.tarasantoshchuk.permissionsmanager.Request;

        import org.json.JSONObject;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Request request = Request.fromJson(Request.toJson(PermissionsManager.init(this).createRequestAll(42, "permission2", "permission3")));
        request = null;
    }
}

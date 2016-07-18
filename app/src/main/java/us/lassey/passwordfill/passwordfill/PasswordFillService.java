package us.lassey.passwordfill.passwordfill;

import android.accessibilityservice.AccessibilityService;
import android.app.AlertDialog;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.content.CursorLoader;
import android.database.CursorWrapper;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

public class PasswordFillService extends AccessibilityService {
    String PASSWORD_URI = "content://org.mozilla.fennec_blassey.db.passwords/passwords";
    public PasswordFillService() {

    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // get the source node of the event
        AccessibilityNodeInfo nodeInfo = event.getSource();
        if (nodeInfo.isPassword()) {
            Log.d("PasswordFill", "Got a password for: " + event);
            String packagename = event.getPackageName().toString();
            String[] nameParts = packagename.split("\\.");
            String[] host = {"%" + nameParts[1] + "." + nameParts[0] + "%"};
            Log.i("PasswordFill", "looking for " + host[0]);
            Uri uri = Uri.parse(PASSWORD_URI);
            String[] proj = {"hostname", "encryptedUsername", "encryptedPassword", "id"};
            Cursor c = this.getContentResolver().query(uri, proj, "hostname LIKE ?", host, null);
            if (c != null) {
                Log.i("PasswordFill", "got " + c.getCount() + " rows");
                if (c.getCount() > 0) {
                    c.moveToFirst();
                    String[] cols = c.getColumnNames();
                    for (int i = 0; i < cols.length; i++) {
                        Log.i("PasswordFill", cols[i] + " : " + c.getString(i));
                    }
                    //CursorLoader loader = new CursorLoader(this, uri, null, "hostname LIKE ?", host, null);
                    int[] views = {R.id.hostname, R.id.username};
                    CursorWrapper cw = new CursorWrapper(c) {
                        @Override
                        public int getColumnIndex (String columnName) {
                            if (columnName.equals("_id")) {
                                return getWrappedCursor().getColumnIndex("id");
                            }
                            return getWrappedCursor().getColumnIndex(columnName);
                        }

                        @Override
                        public int getColumnIndexOrThrow (String columnName) {
                            if (columnName.equals("_id")) {
                                return getWrappedCursor().getColumnIndexOrThrow("id");
                            }
                            return getWrappedCursor().getColumnIndexOrThrow(columnName);
                        }
                    };
                    SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.layout_login, cw, proj, views, 0 );
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);

                    builder.setView(R.layout.password_fill_dialog_layout);
                    AlertDialog alert = builder.create();
                    alert.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);

                    alert.show();
                    ListView listView = (ListView)alert.findViewById(R.id.login_listView);
                    listView.setAdapter(adapter);

                }
            }
        }
    }

    @Override
    public void onInterrupt() {
        Log.d("PasswordFill", "interupted");
    }
}

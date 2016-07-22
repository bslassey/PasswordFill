package us.lassey.passwordfill.passwordfill;

import android.accessibilityservice.AccessibilityService;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.content.CursorLoader;
import android.database.CursorWrapper;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import java.util.Iterator;
import java.util.List;

public class PasswordFillService extends AccessibilityService {
    String PASSWORD_URI = "content://org.mozilla.fennec_blassey.db.passwords/passwords";
    public PasswordFillService() {

    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // get the source node of the event
        final AccessibilityNodeInfo nodeInfo = event.getSource();
        if (nodeInfo.isPassword()) {
            String packagename = event.getPackageName().toString();
            String[] nameParts = packagename.split("\\.");
            String[] host = {"%" + nameParts[1] + "." + nameParts[0] + "%"};
            Uri uri = Uri.parse(PASSWORD_URI);
            String[] proj = {"hostname", "encryptedUsername", "encryptedPassword", "id"};
            Cursor cursor = this.getContentResolver().query(uri, proj, "hostname LIKE ?", host, null);
            if (cursor != null) {
                if (cursor.getCount() > 0) {
                    final ClipboardManager clipboard = (ClipboardManager)this.getSystemService(Context.CLIPBOARD_SERVICE);
                    int[] views = {R.id.hostname, R.id.username};
                    CursorWrapper cw = new CursorWrapper(cursor) {
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
                    final SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.layout_login, cw, proj, views, 0 );
                    AlertDialog.Builder builderSingle = new AlertDialog.Builder(this);
                    builderSingle.setTitle("Login with your saved Firefox password?");
                    builderSingle.setNegativeButton(
                            "cancel",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });

                    builderSingle.setPositiveButton(
                            "OK",
                            new DialogInterface.OnClickListener() {
                                private void setText(AccessibilityNodeInfo node, String text) {
                                    Bundle arguments = new Bundle();
                                    arguments.putCharSequence(AccessibilityNodeInfo
                                            .ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
                                    boolean success = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
                                    if (!success) {
                                        ClipData oldClip = clipboard.getPrimaryClip();
                                        ClipData clip = ClipData.newPlainText("password", "testing");
                                        clipboard.setPrimaryClip(clip);
                                        success = node.performAction(AccessibilityNodeInfo.ACTION_PASTE);
                                        clipboard.setPrimaryClip(oldClip);
                                    }
                                }
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    AlertDialog alert = (AlertDialog)dialog;
                                    Integer tag = (Integer)alert.getButton(DialogInterface.BUTTON_POSITIVE).getTag();
                                    Cursor c = (Cursor)adapter.getItem(tag.intValue());
                                    final String password = c.getString(c.getColumnIndex("encryptedPassword"));
                                    final String username =  c.getString(c.getColumnIndex("encryptedUsername"));
                                    Handler handler = new Handler();
                                    // Need to let the dialog dismiss before we can insert text
                                    final Runnable runnable = new Runnable() {
                                        @Override
                                        public void run() {
                                            setText(nodeInfo, password);
                                            AccessibilityNodeInfo user = nodeInfo.focusSearch(View.FOCUS_UP);
                                            setText(user,username);
                                        }
                                    };
                                    handler.postDelayed(runnable,100);
                                }
                            });

                    //builderSingle.setAdapter(
                    builderSingle.setSingleChoiceItems(
                            adapter, 0,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Button ok = ((AlertDialog)dialog).getButton(DialogInterface.BUTTON_POSITIVE);
                                    ok.setTag(new Integer(which));
                                    ok.setEnabled(true);
                                    Log.i("PAsswordFill", " you selected " + which);
                                }
                            });

                    final AlertDialog alert = builderSingle.create();
                    alert.getListView().setSelector(new ColorDrawable(Color.argb(63,255,195,63)));

                    alert.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                    alert.show();
                    alert.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
                }
            }
        }
    }

    @Override
    public void onInterrupt() {
        Log.d("PasswordFill", "interupted");
    }
}

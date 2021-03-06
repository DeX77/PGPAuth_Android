package org.lf_net.pgpunlocker;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.IntentSender.SendIntentException;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends Activity {
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ServerManager.loadFromFile(this);

        Uri serverConfig = getIntent().getData();

        if (serverConfig != null) {
            Intent intent = new Intent(this, ServerEditActivity.class);
            intent.setData(serverConfig);
            startActivity(intent);
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        setContentView(R.layout.activity_main);

        ListView listViewServers = (ListView) findViewById(R.id.listViewServers);
        ServerAdapter adapter = new ServerAdapter(this, R.layout.listview_item_server);
        listViewServers.setAdapter(adapter);

        registerForContextMenu(listViewServers);

        try {
            Logic.Logic = new Logic(this, prefs.getBoolean("pref_forceapg", false));

            Logic.GuiHelper guiHelper = Logic.Logic.new GuiHelper() {
                public void startActivityForResult(Intent intent, int requestCode) {
                    MainActivity.this.startActivityForResult(intent, requestCode + 0x0000B000);
                }

                public void startIntentSenderForResult(IntentSender intentSender, int requestCode) {
                    try {
                        MainActivity.this.startIntentSenderForResult(intentSender, requestCode + 0x0000B000, null, 0, 0, 0);
                    } catch (SendIntentException e) {
                        // just fuck you
                    }
                }

                public void showUserFeedback(final String feedback) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(MainActivity.this, feedback, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            };

            Logic.Logic.setGuiHelper(guiHelper);
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivityForResult(intent, 0x0000A001);

            Toast.makeText(this, R.string.invalid_settings_detected, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroy() {
        if (Logic.Logic != null) {
            Logic.Logic.close();
        }

        ServerManager.saveToFile(this);

        super.onDestroy();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo info) {
        super.onCreateContextMenu(menu, v, info);

        if (v == findViewById(R.id.listViewServers)) {
            AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) info;
            int index = menuInfo.position;

            Intent editIntent = new Intent(this, ServerEditActivity.class);
            editIntent.putExtra("ServerIndex", index);

            Intent deleteIntent = new Intent(this, ServerDeleteActivity.class);
            deleteIntent.putExtra("ServerIndex", index);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_TEXT, ServerManager.serverAtIndex(index).serializeForURL());
            shareIntent.setType("text/plain");

            menu.setHeaderTitle(R.string.contextmenu_title);

            MenuItem editItem = menu.add(0, v.getId(), 0, R.string.action_edit);
            editItem.setIntent(editIntent);

            MenuItem deleteItem = menu.add(0, v.getId(), 1, R.string.action_delete);
            deleteItem.setIntent(deleteIntent);

            MenuItem shareItem = menu.add(0, v.getId(), 2, R.string.action_share);
            shareItem.setIntent(Intent.createChooser(shareIntent, getText(R.string.action_share)));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuItemAbout: {
                Intent intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
                break;
            }
            case R.id.menuItemAddServer: {
                ServerManager.addServer();

                Intent intent = new Intent(this, ServerEditActivity.class);
                intent.putExtra("ServerIndex", ServerManager.count() - 1);
                startActivity(intent);
                break;
            }
            case R.id.menuItemSettings: {
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivityForResult(intent, 0x0000A001);
                break;
            }
            default:
                return false;
        }

        return true;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == 0x0000A001) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

            if (Logic.Logic != null) {
                Logic.Logic.close();
            }

            try {
                Logic.Logic = new Logic(this, prefs.getBoolean("pref_forceapg", false));
            } catch (Exception e) {
                Logic.Logic = null;
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivityForResult(intent, 0x0000A001);

                Toast.makeText(this, R.string.invalid_settings_detected, Toast.LENGTH_LONG).show();
            }
        } else if (requestCode > 0x0000B000 && requestCode < 0x0000C0000) {
            Logic.Logic.postResult(requestCode - 0x0000B000, resultCode, data);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}

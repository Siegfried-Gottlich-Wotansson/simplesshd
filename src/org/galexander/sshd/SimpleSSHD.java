package org.galexander.sshd;

import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Button;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.net.Uri;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;

public class SimpleSSHD extends Activity
{
	private static final Object lock = new Object();
	private EditText log_view;
	private Button startstop_view;
	public static SimpleSSHD curr = null;
	private UpdaterThread updater = null;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Prefs.init(this);
		setContentView(R.layout.main);
		log_view = (EditText)findViewById(R.id.log);
		startstop_view = (Button)findViewById(R.id.startstop);
	}

	public void onResume() {
		super.onResume();
		synchronized (lock) {
			curr = this;
		}
		update_startstop_prime();
		updater = new UpdaterThread();
		updater.start();
	}

	public void onPause() {
		synchronized (lock) {
			curr = null;
		}
		updater.interrupt();
		super.onPause();
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main_menu, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.settings:
				startActivity(new Intent(this, Settings.class));
				return true;
			case R.id.about: {
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setData(Uri.parse("http://www.galexander.org/software/simplesshd"));
				startActivity(i);
			}	return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private void update_startstop_prime() {
		if (SimpleSSHDService.is_started()) {
			startstop_view.setText("STOP");
			startstop_view.setTextColor(0xFF881111);
		} else {
			startstop_view.setText("START");
			startstop_view.setTextColor(0xFF118811);
		}
	}

	public static void update_startstop() {
		Thread t = new Thread() {
			public void run() {
				synchronized (lock) {
					if (curr != null) {
						curr.update_startstop_prime();
					}
				}
			}
		};
		synchronized (lock) {
			if (curr != null) {
				curr.runOnUiThread(t);
			}
		}
	}

	public void startstop_clicked(View v) {
		Intent i = new Intent(this, SimpleSSHDService.class);
		if (SimpleSSHDService.is_started()) {
			i.putExtra("stop", true);
		}
		startService(i);
	}

	private void update_log_prime() {
		String[] lines = new String[50];
		int curr_line = 0;
		boolean wrapped = false;
		try {
			File f = new File(Prefs.get_path(), "dropbear.err");
			if (f.exists()) {
				BufferedReader r = new BufferedReader(
							new FileReader(f));
				try {
					String l;
					while ((l = r.readLine()) != null) {
						lines[curr_line++] = l;
						if (curr_line >= lines.length) {
							curr_line = 0;
							wrapped = true;
						}
					}
				} finally {
					r.close();
				}
			}
		} catch (Exception e) { }
		int i;
		i = (wrapped ? curr_line : 0);
		String output = "";
		do {
			output = output + lines[i] + "\n";
			i++;
			i %= lines.length;
		} while (i != curr_line);
		log_view.setText(output);
		log_view.setSelection(output.length());
	}

	public static void update_log() {
		Thread t = new Thread() {
			public void run() {
				synchronized (lock) {
					if (curr != null) {
						curr.update_log_prime();
					}
				}
			}
		};
		synchronized (lock) {
			if (curr != null) {
				curr.runOnUiThread(t);
			}
		}
	}
}

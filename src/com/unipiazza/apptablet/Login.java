package com.unipiazza.apptablet;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.google.gson.JsonObject;

public class Login extends Activity implements OnClickListener {

	private EditText user, pass;
	private Button mSubmit;
	private ProgressDialog pDialog;
	JSONParser jsonParser = new JSONParser();
	public static final String MIME_TEXT_PLAIN = "text/plain";
	private NfcAdapter mNfcAdapter;
	private SharedPreferences sp;
	private String email;
	private String password;

	// Classe di controllo connessione
	public class ConnectionDetector {
		private Context _context;

		public ConnectionDetector(Context context) {
			this._context = context;
		}

		public boolean isConnectingToInternet() {
			ConnectivityManager connectivity = (ConnectivityManager) _context.getSystemService(Context.CONNECTIVITY_SERVICE);
			if (connectivity != null) {
				NetworkInfo[] info = connectivity.getAllNetworkInfo();
				if (info != null)
					for (int i = 0; i < info.length; i++)
						if (info[i].getState() == NetworkInfo.State.CONNECTED)
							return true;
			}
			return false;
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login);
		user = (EditText) findViewById(R.id.username);
		pass = (EditText) findViewById(R.id.password);

		mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

		sp = PreferenceManager.getDefaultSharedPreferences(Login.this);
		email = sp.getString("email", "");
		password = sp.getString("password", "");

		pass.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					email = user.getText().toString();
					password = pass.getText().toString();
					postAuthenticate();
				}
				return false;
			}
		});
		mSubmit = (Button) findViewById(R.id.login);
		mSubmit.setOnClickListener(this);
	}

	public static void setupForegroundDispatch(final Activity activity, NfcAdapter adapter) {
		final Intent intent = new Intent(activity.getApplicationContext(), activity.getClass());
		intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		final PendingIntent pendingIntent = PendingIntent.getActivity(activity.getApplicationContext(), 0, intent, 0);
		IntentFilter[] filters = new IntentFilter[1];
		String[][] techList = new String[][] {};
		filters[0] = new IntentFilter();
		filters[0].addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
		filters[0].addCategory(Intent.CATEGORY_DEFAULT);
		try {
			filters[0].addDataType(MIME_TEXT_PLAIN);
		} catch (MalformedMimeTypeException e) {
			throw new RuntimeException("Check your mime type.");
		}
		adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList);
	}

	public static void stopForegroundDispatch(final Activity activity, NfcAdapter adapter) {
		adapter.disableForegroundDispatch(activity);
	}

	@Override
	protected void onResume() {
		super.onResume();
		setupForegroundDispatch(this, mNfcAdapter);
	}

	@Override
	protected void onPause() {
		stopForegroundDispatch(this, mNfcAdapter);
		super.onPause();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		handleIntent(intent);
	}

	private void handleIntent(Intent intent) {
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.login:
			email = user.getText().toString();
			password = pass.getText().toString();
			postAuthenticate();
			break;
		default:
			break;
		}
	}

	private void postAuthenticate() {
		pDialog = new ProgressDialog(Login.this);
		pDialog.setMessage("Login in corso...");
		pDialog.setIndeterminate(false);
		pDialog.setCancelable(true);
		pDialog.show();

		//Controllo connessione ad Internet
		ConnectionDetector cd = new ConnectionDetector(getApplicationContext());
		Boolean internet = cd.isConnectingToInternet();

		Log.d("value", "Internet:  " + internet);
		Log.d("request!", "starting");
		if (internet == true) {
			//Internet presente quindi:
			// Building Parameters
			Log.v("UNIPIAZZA", "username=" + email + " password=" + password);

			AttivitAppRESTClient.getInstance(Login.this).postAuthenticate(Login.this, email, password, new HttpCallback() {

				@Override
				public void onSuccess(JsonObject result) {
					Log.d("Login avvenuto con successo!", result.toString());
					// save user data
					pDialog.dismiss();
					Intent i = new Intent(Login.this, WaitingTap.class);
					finish();
					startActivity(i);
				}

				@Override
				public void onFail(JsonObject result, Throwable e) {
					if (result != null && result.get("msg") != null)
						Toast.makeText(Login.this, result.get("msg").getAsString(), Toast.LENGTH_LONG).show();
					else if (e != null)
						Toast.makeText(Login.this, "Errore con l'autenticazione", Toast.LENGTH_LONG).show();
					pDialog.dismiss();
				}
			});
		} else {
			Toast.makeText(Login.this, "Nessuna connessione a internet", Toast.LENGTH_LONG).show();
			pDialog.dismiss();
		}
	}

}

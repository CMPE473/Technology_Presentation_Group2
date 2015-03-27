package com.example.gamifica;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.games.Games;
import com.google.android.gms.plus.Plus;

public class MainActivity extends ActionBarActivity implements ConnectionCallbacks, OnConnectionFailedListener {
	final int RC_RESOLVE = 5000, RC_UNUSED = 5001;

	// Request code to use when launching the resolution activity
	private static final int REQUEST_RESOLVE_ERROR = 1001;
	// Unique tag for the error dialog fragment
	private static final String DIALOG_ERROR = "dialog_error";
	// Bool to track whether the app is already resolving an error
	private boolean mResolvingError = false;

	// State variable for resolving error state.
	private static final String STATE_RESOLVING_ERROR = "resolving_error";

	private int drinkCounter;

	private GoogleApiClient googleApiClient;
	private Button openLeaderboardButton;
	private Button openAchievementsButton;
	private Button drinkWaterButton;
	private TextView drinkCounterView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mResolvingError = savedInstanceState != null && savedInstanceState.getBoolean(STATE_RESOLVING_ERROR, false);

		googleApiClient = new GoogleApiClient.Builder(this).addApi(Plus.API).addScope(Plus.SCOPE_PLUS_LOGIN).addApi(Games.API).addScope(Games.SCOPE_GAMES)
				.addConnectionCallbacks(this).addOnConnectionFailedListener(this).build();

		openLeaderboardButton = (Button) findViewById(R.id.openLeaderBoardBtn);
		openLeaderboardButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (googleApiClient.isConnected()) {
					startActivityForResult(Games.Leaderboards.getAllLeaderboardsIntent(googleApiClient), RC_UNUSED);
				} else {
					// TODO handle not connected state.
				}
			}
		});

		drinkWaterButton = (Button) findViewById(R.id.drinkWaterBtn);
		drinkWaterButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				incrementDrinkCounter();
			}
		});

		openAchievementsButton = (Button) findViewById(R.id.openAchievementsBtn);
		openAchievementsButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (googleApiClient.isConnected()) {
					startActivityForResult(Games.Achievements.getAchievementsIntent(googleApiClient), RC_UNUSED);
				} else {
					// TODO handle not connected state.
				}
			}
		});

		drinkCounterView = (TextView) findViewById(R.id.drinkCounterView);

		drinkCounter = 0;
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (!mResolvingError) {
			googleApiClient.connect();
		}
	}

	@Override
	protected void onStop() {
		googleApiClient.disconnect();
		super.onStop();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_RESOLVE_ERROR) {
			mResolvingError = false;
			if (resultCode == RESULT_OK) {
				// Make sure the app is not already connected or attempting to
				// connect
				if (!googleApiClient.isConnecting() && !googleApiClient.isConnected()) {
					googleApiClient.connect();
				}
			}
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(STATE_RESOLVING_ERROR, mResolvingError);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onConnected(Bundle connectionHint) {
		// Connected to Google Play services!
		// The good stuff goes here.
		Games.setViewForPopups(googleApiClient, findViewById(R.id.mainView));
		Log.v("sedrik", "client connected: " + googleApiClient.isConnected());
	}

	@Override
	public void onConnectionSuspended(int cause) {
		// The connection has been interrupted.
		// Disable any UI components that depend on Google APIs
		// until onConnected() is called.
		Log.v("sedrik", "connection suspended. cause is " + cause + ". client connected: " + googleApiClient.isConnected());
	}

	@Override
	public void onConnectionFailed(ConnectionResult result) {
		// This callback is important for handling errors that
		// may occur while attempting to connect with Google.
		if (mResolvingError) {
			// Already attempting to resolve an error.
			return;
		} else if (result.hasResolution()) {
			try {
				mResolvingError = true;
				result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
			} catch (SendIntentException e) {
				// There was an error with the resolution intent. Try again.
				googleApiClient.connect();
			}
		} else {
			// Show dialog using GooglePlayServicesUtil.getErrorDialog()
			showErrorDialog(result.getErrorCode());
			mResolvingError = true;
		}
	}

	public void incrementDrinkCounter() {
		drinkCounter++;
		drinkCounterView.setText(String.valueOf(drinkCounter));

		Games.Leaderboards.submitScore(googleApiClient, Constants.LEADERBOARD_TOP_DRINKERS, drinkCounter);
		Log.v("sedrik", "Drink counter is now " + drinkCounter);

		if (drinkCounter == 5) {
			Games.Achievements.unlock(googleApiClient, Constants.ACH_NEWBIE);
		} else if (drinkCounter == 8) {
			Games.Achievements.unlock(googleApiClient, Constants.ACH_PRE_PROFESSIONAL);
		} else if (drinkCounter == 10) {
			Games.Achievements.unlock(googleApiClient, Constants.ACH_PROFESSIONAL);
		} else if (drinkCounter == 12) {
			Games.Achievements.unlock(googleApiClient, Constants.ACH_MASTER);
		} else if (drinkCounter == 14) {
			Games.Achievements.unlock(googleApiClient, Constants.ACH_POST_MASTER);
		}
	}

	/* Creates a dialog for an error message */
	private void showErrorDialog(int errorCode) {
		// Create a fragment for the error dialog
		ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
		// Pass the error that should be displayed
		Bundle args = new Bundle();
		args.putInt(DIALOG_ERROR, errorCode);
		dialogFragment.setArguments(args);
		dialogFragment.show(getSupportFragmentManager(), "errordialog");
	}

	/* Called from ErrorDialogFragment when the dialog is dismissed. */
	public void onDialogDismissed() {
		mResolvingError = false;
	}

	/* A fragment to display an error dialog */
	public static class ErrorDialogFragment extends DialogFragment {
		public ErrorDialogFragment() {
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			// Get the error code and retrieve the appropriate dialog
			int errorCode = this.getArguments().getInt(DIALOG_ERROR);
			return GooglePlayServicesUtil.getErrorDialog(errorCode, this.getActivity(), REQUEST_RESOLVE_ERROR);
		}

		@Override
		public void onDismiss(DialogInterface dialog) {
			((MainActivity) getActivity()).onDialogDismissed();
		}
	}

}

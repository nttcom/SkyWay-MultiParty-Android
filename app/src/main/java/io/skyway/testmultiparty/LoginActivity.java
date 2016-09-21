package io.skyway.testmultiparty;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.Button;

/**
 * A login screen
 */
public class LoginActivity
		extends AppCompatActivity{


	// UI references.
	private AutoCompleteTextView mRoomView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);

		// Set up the login form.
		mRoomView = (AutoCompleteTextView) findViewById(R.id.room);
		{
			Button button = (Button) findViewById(R.id.sign_in_button);
			button.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					String strRoomId = mRoomView.getEditableText().toString();

					Context context = getApplicationContext();
					Intent intent = new Intent();
					intent.setClass(context, MultiPartyActivity.class);
					intent.putExtra(MultiPartyActivity.EXTRA_ROOM_NAME, strRoomId);

					startActivity(intent);
				}
			});
		}

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		android.os.Process.killProcess(android.os.Process.myPid());
	}

}
package io.skyway.testmultiparty;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.skyway.Peer.Browser.Canvas;
import io.skyway.Peer.Browser.MediaStream;
import io.skyway.Peer.PeerError;
import io.skyway.multiparty.MultiParty;
import io.skyway.multiparty.MultiPartyOptions;
import io.skyway.multiparty.OnCallback;


public class MultiPartyActivity
		extends AppCompatActivity
{
	private static final String TAG = MultiPartyActivity.class.getSimpleName();

	public static final String EXTRA_ROOM_NAME = "room_id";

	private DateFormat			_fmtDate;

	private MultiParty			_multiparty;
	private String				_peerOwnId;
	private MediaStream			_msLocal;
	private int					_iDatas;

	private int					_iVolumeControlStream;

	private List<MediaStream> _msRemote;
	private MediaStream       _msCurrent;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_multi_party);


		String roomId = "";

		Intent intent = getIntent();
		if (null != intent)
		{
			roomId = intent.getStringExtra(EXTRA_ROOM_NAME);
		}


		Context context = getApplicationContext();

		_fmtDate = DateFormat.getTimeInstance();
		_iDatas = 0;
		_msRemote = new ArrayList<>();


		{
			TextView tv = (TextView) findViewById(R.id.tvMsg);
			tv.setSingleLine(false);
			tv.setMaxLines(Integer.MAX_VALUE);
			tv.setVerticalScrollBarEnabled(true);
			tv.setHorizontallyScrolling(false);
			tv.setTextColor(Color.WHITE);
			tv.setMovementMethod(new ScrollingMovementMethod());
		}

		{
			Button button = (Button) findViewById(R.id.btnPrev);
			button.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View view)
				{
					prevStream();
				}
			});
		}

		{
			Button button = (Button) findViewById(R.id.btnNext);
			button.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View view)
				{
					nextStream();
				}
			});
		}

		{
			Button button = (Button) findViewById(R.id.btnSendMsg);
			button.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View view)
				{
					sendMessage();
				}
			});
		}

		{
			ToggleButton button = (ToggleButton)findViewById(R.id.btnVideo);
			button.setChecked(true);
			button.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View view)
				{
					updateMuteState();
				}
			});
		}

		{
			ToggleButton button = (ToggleButton)findViewById(R.id.btnAudio);
			button.setChecked(true);
			button.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View view)
				{
					updateMuteState();
				}
			});
		}

		// create MultiParty Object
		_multiparty = createMultiParty(context,roomId);
		setMultipartyEvents(_multiparty);

		_multiparty.start();

		updateUI();
	}



	private MultiParty createMultiParty(Context context, String roomId)
	{
		MultiPartyOptions options = new MultiPartyOptions();

		// Enter your APIkey and Domain
		// Please check this page. >> https://skyway.io/ds/
		options.key = "";
		options.domain = "";
		options.locationHost = "/";

		options.room = roomId;
		options.debug = MultiPartyOptions.DebugLevelEnum.ALL_LOGS;

		return new MultiParty(context, options);
	}


	private void setMultipartyEvents(MultiParty multiParty)
	{
		// TODO: OPEN
		multiParty.on(MultiParty.MultiPartyEventEnum.OPEN, new OnCallback()
		{
			@Override
			public void onCallback(JSONObject object)
			{
				Log.d(TAG, "[MP/OPEN]" + object.toString());

				_peerOwnId = "";

				try
				{
					_peerOwnId = object.getString("peer-id");
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}

				String strLog = "open:" + _peerOwnId;
				appendLog(strLog);
			}
		});

		// TODO: MY_MS
		multiParty.on(MultiParty.MultiPartyEventEnum.MY_MS, new OnCallback()
		{
			@Override
			public void onCallback(JSONObject object)
			{
				Log.d(TAG, "[MP/MY_MS]" + object.toString());

				MediaStream stream = null;
				try
				{
					stream = (MediaStream) object.get("src");

					_msLocal = stream;

					if (null != _msLocal)
					{
						Canvas canvas = (Canvas)findViewById(R.id.cvsLocal);
						if (null != canvas)
						{
							canvas.addSrc(_msLocal, 0);
						}
					}
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}
			}
		});

		// TODO: PEER_MS
		multiParty.on(MultiParty.MultiPartyEventEnum.PEER_MS, new OnCallback()
		{
			@Override
			public void onCallback(JSONObject object)
			{
				Log.d(TAG, "[MP/PEER_MS]" + object.toString());

				String peerId = null;
				MediaStream stream = null;

				try
				{
					peerId = object.getString("id");
					stream = (MediaStream)object.get("src");

					if (null != stream)
					{
						if (0 == _msRemote.size())
						{
							changeMediaStream(null, stream);
						}

						_msRemote.add(stream);
					}

					String strLog = "In:" + peerId;
					appendLog(strLog);

					updateUI();
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}
			}
		});

		// TODO: MS_CLOSE
		multiParty.on(MultiParty.MultiPartyEventEnum.MS_CLOSE, new OnCallback()
		{
			@Override
			public void onCallback(JSONObject object)
			{
				Log.d(TAG, "[MP/MS_CLOSE]" + object.toString());

				String peerId = null;
				MediaStream stream = null;

				try
				{
					peerId = object.getString("id");
					stream = (MediaStream)object.get("src");
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}

				if (null != stream)
				{
					Canvas canvas = (Canvas)findViewById(R.id.cvsRemote);
					if (null != canvas)
					{
						if (_msCurrent == stream)
						{
							canvas.removeSrc(stream, 0);

							_msCurrent = null;
						}

						if (null != _msRemote)
						{
							_msRemote.remove(stream);
						}

						if (null == _msCurrent)
						{
							nextStream();
						}
					}
				}

				String strLog = "Out:" + peerId;
				appendLog(strLog);

				updateUI();
			}
		});

		// TODO: PEER_SS
		multiParty.on(MultiParty.MultiPartyEventEnum.PEER_SS, new OnCallback()
		{
			@Override
			public void onCallback(JSONObject object)
			{
				Log.d(TAG, "[MP/PEER_SS]" + object.toString());
			}
		});

		// TODO: SS_CLOSE
		multiParty.on(MultiParty.MultiPartyEventEnum.SS_CLOSE, new OnCallback()
		{
			@Override
			public void onCallback(JSONObject object)
			{
				Log.d(TAG, "[MP/SS_CLOSE]" + object.toString());
			}
		});

		// TODO: DC_OPEN
		multiParty.on(MultiParty.MultiPartyEventEnum.DC_OPEN, new OnCallback()
		{
			@Override
			public void onCallback(JSONObject object)
			{
				Log.d(TAG, "[MP/DC_OPEN]" + object.toString());

				_iDatas++;

				updateUI();
			}
		});

		// TODO: MESSAGE
		multiParty.on(MultiParty.MultiPartyEventEnum.MESSAGE, new OnCallback()
		{
			@Override
			public void onCallback(JSONObject object)
			{
				String strValue = "";

				Log.d(TAG, "[MP/MESSAGE]");
				strValue = object.toString();

				appendLog(strValue);
			}
		});

		// TODO: DC_CLOSE
		multiParty.on(MultiParty.MultiPartyEventEnum.DC_CLOSE, new OnCallback()
		{
			@Override
			public void onCallback(JSONObject object)
			{
				Log.d(TAG, "[MP/DC_CLOSE]" + object.toString());

				_iDatas--;

				updateUI();
			}
		});

		// TODO: ERROR
		multiParty.on(MultiParty.MultiPartyEventEnum.ERROR, new OnCallback()
		{
			@Override
			public void onCallback(JSONObject object)
			{
				Log.d(TAG, "[MP/ERROR]" + object.toString());

				PeerError peerError = null;

				try
				{
					peerError = (PeerError)object.get("peerError");
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}
			}
		});
	}

	private void appendLog(final String strLog)
	{
		Handler handler = new Handler(Looper.getMainLooper());
		handler.post(new Runnable()
		{
			@Override
			public void run()
			{
				TextView tv = (TextView) findViewById(R.id.tvMsg);

				if (null != tv)
				{
					Date date = new Date();
					String strTime = _fmtDate.format(date);
					tv.append("[");
					tv.append(strTime);
					tv.append("]");
					tv.append(strLog);
					tv.append("\n");

					final int scrollAmount = tv.getLayout().getLineTop(
							tv.getLineCount()) - tv.getHeight();
					if (scrollAmount > 0)
					{
						tv.scrollTo(0, scrollAmount);
					} else
					{
						tv.scrollTo(0, 0);
					}
				}
			}
		});
	}

	private void updateUI()
	{
		Handler handler = new Handler(Looper.getMainLooper());
		handler.post(new Runnable()
		{
			@Override
			public void run()
			{
				int iVisible = View.GONE;

				if ((null != _msRemote) && (1 < _msRemote.size()))
				{
					iVisible = View.VISIBLE;
				}

				{
					Button button = (Button) findViewById(R.id.btnVideo);
					button.bringToFront();
				}

				{
					Button button = (Button) findViewById(R.id.btnAudio);
					button.bringToFront();
				}

				{
					Button button = (Button) findViewById(R.id.btnPrev);
					button.setVisibility(iVisible);
					if (View.VISIBLE == iVisible)
					{
						button.bringToFront();
					}
				}

				{
					Button button = (Button) findViewById(R.id.btnNext);
					button.setVisibility(iVisible);
					if (View.VISIBLE == iVisible)
					{
						button.bringToFront();
					}
				}

				{
					TextView tv = (TextView) findViewById(R.id.tvMsg);
					tv.bringToFront();
				}

				{
					Button button = (Button) findViewById(R.id.btnSendMsg);
					boolean bEnable = false;
					if (0 < _iDatas)
					{
						bEnable = true;
					}
					button.setEnabled(bEnable);
					button.bringToFront();
				}

				View view = findViewById(android.R.id.content);
				view.requestLayout();
			}
		});
	}

	private void updateMuteState()
	{
		boolean bMuteVideo = false;
		boolean bMuteAudio = false;

		{
			ToggleButton button = (ToggleButton)findViewById(R.id.btnVideo);
			if (!button.isChecked())
			{
				bMuteVideo = true;
			}
		}

		{
			ToggleButton button = (ToggleButton)findViewById(R.id.btnAudio);
			if (!button.isChecked())
			{
				bMuteAudio = true;
			}
		}

		_multiparty.mute(bMuteVideo, bMuteAudio);
	}

	private void changeMediaStream(MediaStream prev, MediaStream next)
	{
		Canvas canvas = (Canvas)findViewById(R.id.cvsRemote);
		if (null == canvas)
		{
			return;
		}

		int iCount = 0;
		if (null != prev)
		{
			iCount = prev.getVideoTracks();
			for (int i = 0 ; iCount > i ; i++)
			{
				prev.setEnableVideoTrack(i, false);
			}

			canvas.removeSrc(prev, 0);
		}

		_msCurrent = next;

		Handler handler = new Handler(Looper.getMainLooper());
		handler.post(new Runnable()
		{
			@Override
			public void run()
			{
				if (null == _msCurrent)
				{
					return;
				}

				Canvas canvas = (Canvas) findViewById(R.id.cvsRemote);
				canvas.addSrc(_msCurrent, 0);

				int iCount = _msCurrent.getVideoTracks();
				for (int i = 0; iCount > i; i++)
				{
					_msCurrent.setEnableVideoTrack(i, true);
				}
			}
		});
	}

	private void prevStream()
	{
		if (null == _msRemote)
		{
			return;
		}

		int iIndex = 0;

		if (0 == _msRemote.size())
		{
			return;
		}
		else if (1 == _msRemote.size())
		{
		}
		else if (1 < _msRemote.size())
		{
			iIndex = _msRemote.indexOf(_msCurrent);
			iIndex--;
			if (0 > iIndex)
			{
				iIndex = _msRemote.size() - 1;
			}
		}

		MediaStream msNew = null;

		if (0 <= iIndex)
		{
			msNew = _msRemote.get(iIndex);
		}

		if (null != msNew)
		{
			changeMediaStream(_msCurrent, msNew);
		}
	}

	private void nextStream()
	{
		if (null == _msRemote)
		{
			return;
		}

		int iIndex = 0;

		if (0 == _msRemote.size())
		{
			return;
		}
		else if (1 == _msRemote.size())
		{
			//
		}
		else if (1 < _msRemote.size())
		{
			iIndex = _msRemote.indexOf(_msCurrent);
			iIndex++;
			if (_msRemote.size() <= iIndex)
			{
				iIndex = 0;
			}
		}

		MediaStream msNew = null;

		if (0 <= iIndex)
		{
			msNew = _msRemote.get(iIndex);
		}

		if (null != msNew)
		{
			changeMediaStream(_msCurrent, msNew);
		}
	}

	private void sendMessage()
	{
		String strMsg = "Hello";
		_multiparty.send(strMsg);
		appendLog(strMsg);
	}


	@Override
	protected void onStart()
	{
		super.onStart();

		// Disable Sleep and Screen Lock
		Window wnd = getWindow();
		wnd.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		wnd.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		// Set volume control stream type to WebRTC audio.
		_iVolumeControlStream = getVolumeControlStream();

		setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
	}

	@Override
	protected void onPause()
	{
		// Set default volume control stream type.
		setVolumeControlStream(_iVolumeControlStream);

		super.onPause();
	}

	@Override
	protected void onStop()
	{
		// Enable Sleep and Screen Lock
		Window wnd = getWindow();
		wnd.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		wnd.clearFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

		super.onStop();
	}

	@Override
	protected void onDestroy()
	{
		_multiparty.close();
		_multiparty = null;

		while (0 < _msRemote.size())
		{
			MediaStream stream = _msRemote.get(0);
			if (null == stream)
			{
				_msRemote.remove(0);
				continue;
			}

			stream.close();

			_msRemote.remove(0);
		}
		_msRemote = null;

		super.onDestroy();
	}

}

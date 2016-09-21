package io.skyway.multiparty;


import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import io.skyway.Peer.Browser.MediaConstraints;
import io.skyway.Peer.Browser.MediaStream;
import io.skyway.Peer.Browser.Navigator;
import io.skyway.Peer.CallOption;
import io.skyway.Peer.ConnectOption;
import io.skyway.Peer.DataConnection;
import io.skyway.Peer.MediaConnection;
import io.skyway.Peer.Peer;
import io.skyway.Peer.PeerError;
import io.skyway.Peer.PeerOption;

/**
 * MultiParty
 */
public class MultiParty extends MultiPartyImpl
{

	/// Event callback type
	public enum MultiPartyEventEnum
	{
		OPEN,		/// Connect to signaling server.

		MY_MS,		/// Open local media stream.

		PEER_MS,	/// Open remote media stream.
		MS_CLOSE,	/// Close remote media stream.

		PEER_SS,	/// Open screen cast stream.
		SS_CLOSE,	/// Close screen cast stream.

		DC_OPEN,	/// Open data connection.
		MESSAGE,	/// Received data.
		DC_CLOSE,	/// Close data connection.

		ERROR,		/// Rise error.
	}

	/**
	 * Constructor
	 * @param context Application context
	 * @param options MultiParty setting
	 */
	public MultiParty (Context context, MultiPartyOptions options)
	{
		super(context, options);
	}

}

/**
 * Internal
 */
class MultiPartyImpl
{
	private static final String TAG = MultiParty.class.getSimpleName();

	private static final int MD5_DIGEST_LENGTH = 16;
	private static final int MD5_BLOCK_BYTES   = 64;

	static final String KEY_TYPE    = "type";
	static final String TYPE_SCREEN = "screen";

	// Public properties
	public boolean opened;        	/// Open status
	public boolean reconnecting;	/// Reconnecting status

	// Local static
	private static final Map<String, MultiPartyConnection> connections = Collections.synchronizedMap(new HashMap<String, MultiPartyConnection>());	/// Connections

	private static final ReentrantLock _lckPolling = new ReentrantLock();
	private static final Condition _cndPolling = _lckPolling.newCondition();

	private static final ReentrantLock _lck  = new ReentrantLock();
	private static final Condition _cnd  = _lck.newCondition();

	// Local variables
	private Context _context;
	private Handler _handler;

	private boolean _bPolling;

	// MultiParty local variables
	private MultiPartyImpl		_multipartyimpl;

	private MultiPartyOptions	_options;
	private HandlerThread		_ht;

	// WebRTC
	private Peer        _peer;
	private MediaStream _msLocal;

	// Callbacks
	private OnCallback _cbOpen;
	private OnCallback _cbMyMs;
	///
	private OnCallback _cbPeerMs;
	private OnCallback _cbMsClose;
	//
	private OnCallback _cbPeerSs;
	private OnCallback _cbSsClose;
	//
	private OnCallback _cbDcOpen;
	private OnCallback _cbMessage;
	private OnCallback _cbDcClose;
	//
	private OnCallback _cbError;

	/**
	 * Constructor
	 * @param context Application context
	 * @param options setting information
	 */
	public MultiPartyImpl(Context context, MultiPartyOptions options)
	{
		if (MultiPartyOptions.DebugLevelEnum.ALL_LOGS.ordinal() <= options.debug.ordinal())
		{
			Log.d(TAG, options.toString());
		}

		// Public properties
		opened = false;
		reconnecting = false;

		_multipartyimpl = this;

		_context = context;
		_ht = new HandlerThread("io.skyway.multiparty.handler");
		_ht.start();
		_handler = new Handler(_ht.getLooper());

		_options = options;

		PeerOption peerOption = new PeerOption();
		peerOption.type = Peer.PeerTypeEnum.SKYWAY;
		peerOption.key = options.key;
		peerOption.domain = options.domain;

		Peer.DebugLevelEnum debug = Peer.DebugLevelEnum.NO_LOGS;
		if (MultiPartyOptions.DebugLevelEnum.NO_LOGS == options.debug)
		{
			debug = Peer.DebugLevelEnum.NO_LOGS;
		}
		else if (MultiPartyOptions.DebugLevelEnum.ONLY_ERROR == options.debug)
		{
			debug = Peer.DebugLevelEnum.ONLY_ERROR;
		}
		else if (MultiPartyOptions.DebugLevelEnum.ERROR_AND_WARNING == options.debug)
		{
			debug = Peer.DebugLevelEnum.ERROR_AND_WARNING;
		}
		else if (MultiPartyOptions.DebugLevelEnum.ALL_LOGS == options.debug)
		{
			debug = Peer.DebugLevelEnum.ALL_LOGS;
		}
		peerOption.debug = debug;

		peerOption.host = options.host;
		peerOption.port = options.port;
		peerOption.secure = options.secure;
		peerOption.config = options.config;
		peerOption.turn = options.useSkyWayTurn;
		peerOption.vp8hwcodec = options.vp8hwcodec;

		_options.setPeerOption(peerOption);

		_options.setUseStream(false);
		if ((options.constraints.videoFlag) || (options.constraints.audioFlag))
		{
			_options.setUseStream(true);
		}

		String strSeed = "";
		if ((null != options.room) && (0 < options.room.length()))
		{
			strSeed = options.room;
		}

		String strResult = strSeed.replaceAll("/^[0-9a-zA-Z\\\\-\\\\_]{4,32}$/", "");

		_options.setRoomName(strResult);

		String strId = makeRoomName(strSeed);
		_options.setRoomId(strId);

		String peerId = "";
		if ((null == _options.identity) || (0 == _options.identity.length()))
		{
			String strGenId = makeID();
			peerId = String.format("%s%s", strId, strGenId);
		}
		else
		{
			peerId = String.format("%s%s", strId, _options.identity);
		}
		_options.identity = peerId;
	}

	/**
	 * Starting multiparty
	 */
	public void start()
	{
		if (opened)
		{
			return;
		}

		opened = true;

		// Start media connection
		if (_options.getUseStream())
		{
			_handler.post(new Runnable()
			{
				@Override
				public void run()
				{
					startMyStream();

					connectToSkyWay(_context);
				}
			});
		}
		else
		{
			_handler.post(new Runnable()
			{
				@Override
				public void run()
				{
					// Connecting
					connectToSkyWay(_context);
				}
			});
		}
	}

	/**
	 * Set event callback
	 * @param event Event type
	 * @param callback callback
	 */
	public void on(MultiParty.MultiPartyEventEnum event, OnCallback callback)
	{
		if (MultiParty.MultiPartyEventEnum.OPEN == event)
		{
			// Open
			_cbOpen = callback;
		}
		else if (MultiParty.MultiPartyEventEnum.MY_MS == event)
		{
			// Opened own media stream
			_cbMyMs = callback;
		}
		else if (MultiParty.MultiPartyEventEnum.PEER_MS == event)
		{
			// Opened remote media stream
			_cbPeerMs = callback;
		}
		else if (MultiParty.MultiPartyEventEnum.MS_CLOSE == event)
		{
			// Closed remote media stream
			_cbMsClose = callback;
		}
		else if (MultiParty.MultiPartyEventEnum.PEER_SS == event)
		{
			// Opened remote screen cast stream
			_cbPeerSs = callback;
		}
		else if (MultiParty.MultiPartyEventEnum.SS_CLOSE == event)
		{
			// Closed remote screen cast stream
			_cbSsClose = callback;
		}
		else if (MultiParty.MultiPartyEventEnum.DC_OPEN == event)
		{
			// Opened remote data channel
			_cbDcOpen = callback;
		}
		else if (MultiParty.MultiPartyEventEnum.MESSAGE == event)
		{
			// Received remote message
			_cbMessage = callback;
		}
		else if (MultiParty.MultiPartyEventEnum.DC_CLOSE == event)
		{
			// Closed remote data channel
			_cbDcClose = callback;
		}
		else if (MultiParty.MultiPartyEventEnum.ERROR == event)
		{
			// Error
			_cbError = callback;
		}
	}

	/**
	 * Muting video and audio track
	 * @param video Mute video track
	 * @param audio Mute audio track
	 */
	public void mute(boolean video, boolean audio)
	{
		if (null == _msLocal)
		{
			return;
		}

		// Audio tracks
		if (false == audio)
		{
			audio = true;
		}
		else
		{
			audio = false;
		}

		int iTracks = _msLocal.getAudioTracks();
		if (0 < iTracks)
		{
			for (int i = 0 ; iTracks > i ; i++)
			{
				_msLocal.setEnableAudioTrack(i, audio);
			}
		}

		// Video tracks
		if (false == video)
		{
			video = true;
		}
		else
		{
			video = false;
		}

		iTracks = _msLocal.getVideoTracks();
		if (0 < iTracks)
		{
			for (int i = 0 ; iTracks > i ; i++)
			{
				_msLocal.setEnableVideoTrack(i, video);
			}
		}
	}

	/**
	 * Disconnect remote peer
	 * @param peerId Remote peer ID
	 * @return result
	 */
	public boolean removePeer(String peerId)
	{
		if (null == peerId)
		{
			return false;
		}

		synchronized (connections)
		{
			MultiPartyConnection connection = connections.get(peerId);
			if (null == connection)
			{
				return false;
			}

			connection.close();

			connections.remove(peerId);
		}

		return true;
	}

	/**
	 * Send data to remote peers
	 * @param data Send data
	 * @return result
	 */
	public boolean send(Object data)
	{
		if (null == data)
		{
			return false;
		}

		boolean bResultCode = true;

		Set keys = connections.keySet();
		for (Iterator iterator = keys.iterator() ; iterator.hasNext() ;)
		{
			String peerId = (String)iterator.next();
			// Get connection
			MultiPartyConnection connection = connections.get(peerId);
			if (null == connection)
			{
				continue;
			}

			if (0 < connection.datas.size())
			{
				// Send data to remote
				for (DataConnection dat : connection.datas)
				{
					bResultCode = dat.send(data);
					if (!bResultCode)
					{
						break;
					}
				}
			}

			if (!bResultCode)
			{
				break;
			}
		}

		return bResultCode;
	}

	/**
	 * Closing remote connections and destroy peer
	 * @return result
	 */
	public boolean close()
	{
		if (!opened)
		{
			return false;
		}

		opened = false;
		reconnecting = false;

		if (null != _lck)
		{
			_lck.lock();
			_cnd.signal();
			_lck.unlock();
		}

		stopPollingConnections();

		while (0 < connections.size())
		{
			Set keys = connections.keySet();
			Iterator iterator = keys.iterator();
			if (!iterator.hasNext())
			{
				continue;
			}

			String peerId = (String)iterator.next();

			removePeer(peerId);
		}

		if (null != _msLocal)
		{
			_msLocal.close();
			_msLocal = null;
		}

		if (null != _peer)
		{
			clearPeerEvents(_peer);

			_peer.destroy();
			_peer = null;
		}

		if (null != _handler)
		{
			_handler = null;
		}

		if (null != _ht)
		{
			_ht.quit();
			_ht = null;
		}

		return true;
	}

	/**
	 * Listing peers
	 * @param listing
	 * @return Executing result
	 */
	public boolean listAllPeers(OnListing listing)
	{
		if ((null == _peer) || (_peer.isDestroyed))
		{
			return false;
		}

		RunnableListing runListing = new RunnableListing();
		runListing.peer = _peer;
		runListing.listing = listing;
		runListing.ownId = _options.identity;
		runListing.head = _options.getRoomId();

		_handler.post(runListing);

		return true;
	}

	/**
	 * Reconnecting lost connection in room peers
	 * @param video Media stream
	 * @param screen Screen share media stream
	 * @param data Data stream
	 */
	public void reconnect(boolean video, boolean screen, boolean data)
	{
		if (reconnecting)
		{
			return;
		}

		reconnecting = true;

		// Stop polling
		stopPollingConnections();

		final boolean bMedia = video;
		final boolean bData = data;
		final boolean bScreen = screen;

		listAllPeers(new OnListing()
		{
			@Override
			public void onListing(List<String> list)
			{
				final List<String> peers = list;

				_handler.post(new Runnable()
				{
					@Override
					public void run()
					{
						boolean bConnecting = false;

						for (String peerId : peers)
						{
							if (false == reconnecting)
							{
								break;
							}

							if (bData)
							{
								bConnecting = startData(peerId);
								if (bConnecting)
								{
									try
									{
										_lck.lock();
										_cnd.await();
										_lck.unlock();
									}
									catch (Exception e)
									{
										e.printStackTrace();
									}
								}
							}

							if (false == reconnecting)
							{
								break;
							}

							if (bMedia)
							{
								bConnecting = startCall(peerId, true);
								if (bConnecting)
								{
									try
									{
										_lck.lock();
										_cnd.await();
										_lck.unlock();
									}
									catch (Exception e)
									{
										e.printStackTrace();
									}
								}
							}

							if (false == reconnecting)
							{
								break;
							}

							if (bScreen)
							{
								bConnecting = startCall(peerId, false);
								if (bConnecting)
								{
									try
									{
										_lck.lock();
										_cnd.await();
										_lck.unlock();
									}
									catch (Exception e)
									{
										e.printStackTrace();
									}
								}
							}
						}

						reconnecting = false;

						// Restart polling
						if (_options.polling)
						{
							startPollingConnections();
						}
					}
				});
			}
		});
	}

	/**
	 * Connecting to SkyWay signaling server
	 * @param context Application context
	 */
	private void connectToSkyWay(Context context)
	{
		PeerOption option = _options.getPeerOption();
		_peer = new Peer(context, _options.identity, option);

		setPeerEvents(_peer);

		if (_options.getUseStream())
		{
			startMediaStream();
		}
	}

	/**
	 * Set Peer event methods
	 * @param peer Peer object
	 */
	private void setPeerEvents(Peer peer)
	{
		if (null == peer)
		{
			return;
		}

		// OPEN
		peer.on(Peer.PeerEventEnum.OPEN, new io.skyway.Peer.OnCallback()
		{
			@Override
			public void onCallback(Object o)
			{
				if (!(o instanceof String))
				{
					return;
				}

				String strPeerId = (String)o;

				opened = true;

				if (0 == _options.identity.compareTo(strPeerId))
				{
					JSONObject json = new JSONObject();

					try
					{
						json.put("peer-id", strPeerId);
					}
					catch (JSONException e)
					{
						e.printStackTrace();
					}

					raiseEvent(MultiParty.MultiPartyEventEnum.OPEN, json);

					//
					_handler.post(new Runnable()
					{
						@Override
						public void run()
						{
							if (null == _options)
							{
								return;
							}

							if ((null == _msLocal) && (_options.getUseStream()))
							{
								// Waiting local media stream
								try
								{
									_lck.lock();
									_cnd.await();
									_lck.unlock();
								}
								catch (InterruptedException e)
								{
									e.printStackTrace();
								}
							}

							getIDs();
						}
					});
				}
			}
		});

		// CALL
		peer.on(Peer.PeerEventEnum.CALL, new io.skyway.Peer.OnCallback()
		{
			@Override
			public void onCallback(Object o)
			{
				// CALL from remote
				if (!(o instanceof MediaConnection))
				{
					return;
				}

				MediaConnection media = (MediaConnection)o;

				String peerId = media.peer;

				MultiPartyConnection connection = null;

				synchronized (connections)
				{
					connection = connections.get(peerId);
					if (null == connection)
					{
						connection = new MultiPartyConnection();
						connection.peerId = peerId;
						connection.media = false;
						connection.multipartyimpl = _multipartyimpl;

						connections.put(peerId, connection);
					}
				}

				connection.addMediaConnection(media);

				String value = getValueFromJSONString(media.metadata, KEY_TYPE);
				if (0 == value.compareTo(TYPE_SCREEN))
				{
					media.answer();

					return;
				}

				if (!_options.getUseStream())
				{
					media.answer();
				}
				else
				{
					media.answer(_msLocal);
				}
			}
		});

		// CONNECTION
		peer.on(Peer.PeerEventEnum.CONNECTION, new io.skyway.Peer.OnCallback()
		{
			@Override
			public void onCallback(Object o)
			{
				// CONNECTION from remote
				if (!(o instanceof DataConnection))
				{
					return;
				}

				DataConnection data = (DataConnection)o;

				String peerId = data.peer;

				MultiPartyConnection connection = null;

				synchronized (connections)
				{
					connection = connections.get(peerId);
					if (null == connection)
					{
						connection = new MultiPartyConnection();
						connection.peerId = peerId;
						connection.media = false;
						connection.multipartyimpl = _multipartyimpl;

						connections.put(peerId, connection);
					}
				}

				connection.addDataConnection(data);
			}
		});

		// CLOSE
		peer.on(Peer.PeerEventEnum.CLOSE, new io.skyway.Peer.OnCallback()
		{
			@Override
			public void onCallback(Object o)
			{
				// TODO: Code
			}
		});

		peer.on(Peer.PeerEventEnum.DISCONNECTED, new io.skyway.Peer.OnCallback()
		{
			@Override
			public void onCallback(Object o)
			{
				// TODO: Code
			}
		});

		peer.on(Peer.PeerEventEnum.ERROR, new io.skyway.Peer.OnCallback()
		{
			@Override
			public void onCallback(Object o)
			{
				// Error
				String peerId = _peer.identity;
				if (null == peerId)
				{
					peerId = "";
				}

				PeerError peerError = null;
				if (o instanceof PeerError)
				{
					peerError = (PeerError) o;
				}

				JSONObject json = new JSONObject();

				try
				{
					json.put("id", peerId);
					if (null != peerError)
					{
						json.put("PeerError", peerError);
					}
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}

			}
		});
	}

	/**
	 * Clear peer events
	 * @param peer Peer
	 */
	private void clearPeerEvents(Peer peer)
	{
		if (null == peer)
		{
			return;
		}

		peer.on(Peer.PeerEventEnum.ERROR, null);
		peer.on(Peer.PeerEventEnum.DISCONNECTED, null);
		peer.on(Peer.PeerEventEnum.CLOSE, null);
		peer.on(Peer.PeerEventEnum.CONNECTION, null);
		peer.on(Peer.PeerEventEnum.CALL, null);
		peer.on(Peer.PeerEventEnum.OPEN, null);
	}

	/**
	 * Clear callback
	 */
	private void clearCallbacks()
	{
		_cbError = null;

		_cbDcClose = null;
		_cbMessage = null;
		_cbDcOpen = null;

		_cbPeerSs = null;
		_cbSsClose = null;

		_cbPeerSs = null;
		_cbSsClose = null;

		_cbMyMs = null;

		_cbOpen = null;
	}

	/**
	 * Get room peers
	 */
	void getIDs()
	{
		listAllPeers(new OnListing()
		{
			@Override
			public void onListing(List<String> list)
			{
				final ArrayList<String> lstPeers = new ArrayList<String>();

				for (String strPeerId : list)
				{
					MultiPartyConnection connection = null;
					connection = connections.get(strPeerId);
					if (null != connection)
					{
						continue;
					}

					connection = new MultiPartyConnection();
					connection.peerId = strPeerId;
					connection.media = _options.getUseStream();
					connection.multipartyimpl = _multipartyimpl;

					connections.put(strPeerId, connection);

					lstPeers.add(strPeerId);
				}

				// Start connection
				_handler.post(new Runnable()
				{
					@Override
					public void run()
					{
						if ((null != lstPeers) && (0 < lstPeers.size()))
						{
							// Media connection
							startCall(lstPeers);

							// Data connection
							startData(lstPeers);
						}

						if (_options.polling)
						{
							startPollingConnections();
						}
					}
				});
			}
		});
	}

	/**
	 * Start polling remote peers
	 */
	void startPollingConnections()
	{
		if (_bPolling)
		{
			return;
		}

		_bPolling = true;

		Thread thrPolling = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				while (_bPolling)
				{
					long lInterval = _options.polling_interval;

					if ((null == _lckPolling) || (null == _cndPolling))
					{
						break;
					}

					try
					{
						_lckPolling.lock();
						_cndPolling.await(lInterval, TimeUnit.MILLISECONDS);
						_lckPolling.unlock();
					}
					catch (InterruptedException e)
					{
						e.printStackTrace();
					}

					//
					if (!_bPolling)
					{
						break;
					}

					listAllPeers(new OnListing()
					{
						@Override
						public void onListing(List<String> list)
						{
							// Remove list
							List<String> lstRemove = new ArrayList<>();

							Set<String> keys = connections.keySet();

							for (Iterator iterator = keys.iterator() ; iterator.hasNext() ; )
							{
								String strPeer = (String) iterator.next();

								if (0 <= list.indexOf(strPeer))
								{
									continue;
								}

								lstRemove.add(strPeer);
							}

							for (String strPeer : lstRemove)
							{
								removePeer(strPeer);
							}

							try
							{
								_lckPolling.lock();
								_cndPolling.signal();
								_lckPolling.unlock();
							}
							catch (Exception e)
							{
								e.printStackTrace();
							}
						}
					});

					try
					{
						_lckPolling.lock();
						_cndPolling.await();
						_lckPolling.unlock();
					}
					catch (InterruptedException e)
					{
						e.printStackTrace();
					}
				}
			}
		}, "polling");

		thrPolling.start();
	}

	/**
	 * Stopping polling connection
	 */
	void stopPollingConnections()
	{
		if (!_bPolling)
		{
			return;
		}

		_bPolling = false;

		if (null != _cndPolling)
		{
			try
			{
				_lckPolling.lock();
				_cndPolling.signal();
				_lckPolling.unlock();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	/**
	 * Get local media stream
	 */
	private void startMyStream()
	{
		MediaConstraints constraints = null;

		if ((null != _options) && (null != _options.constraints))
		{
			constraints = _options.constraints;
		}

		if (null == constraints)
		{
			constraints = new MediaConstraints();
		}

		Navigator.initialize(_peer);

		_msLocal = Navigator.getUserMedia(constraints);

		if (null == _msLocal)
		{
			return;
		}

		String peerId = _peer.identity;

		JSONObject json = new JSONObject();

		try
		{
			json.put("id", peerId);
			json.put("src", _msLocal);
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}

		raiseEvent(MultiParty.MultiPartyEventEnum.MY_MS, json);

		try
		{
			_lck.lock();
			_cnd.signal();
			_lck.unlock();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Starting local media stream
	 */
	private void startMediaStream()
	{
		if (null != _msLocal)
		{
			return;
		}

		Handler handler = new Handler(Looper.getMainLooper());
		handler.post(new Runnable()
		{
			@Override
			public void run()
			{
				startMyStream();
			}
		});
	}

	/**
	 * Connecting data conenction
	 * @param peerId Remote peer
	 */
	boolean startData(String peerId)
	{
		if ((null == _peer) || (null == connections))
		{
			return false;
		}

		MultiPartyConnection connection = null;
		synchronized (connections)
		{
			connection = connections.get(peerId);
		}

		if (null == connection)
		{
			connection = new MultiPartyConnection();
			connection.peerId = peerId;
			connection.media = _options.getUseStream();
			connection.multipartyimpl = _multipartyimpl;

			connections.put(peerId, connection);
		}
		else
		{
			if (0 < connection.datas.size())
			{
				if (MultiPartyOptions.DebugLevelEnum.ALL_LOGS.ordinal() <= _options.debug.ordinal())
				{
					Log.i(TAG, "Already connected to " + peerId);
				}

				return false;
			}
		}

		if (MultiPartyOptions.DebugLevelEnum.ALL_LOGS.ordinal() <= _options.debug.ordinal())
		{
			Log.i(TAG, "Connecting to " + peerId);
		}

		// Apply to connecting option
		DataConnection.SerializationEnum serialization = DataConnection.SerializationEnum.BINARY;
		if (MultiPartyOptions.SerializationEnum.BINARY == _options.serialization)
		{
			serialization = DataConnection.SerializationEnum.BINARY;
		}
		else if (MultiPartyOptions.SerializationEnum.BINARY_UTF8 == _options.serialization)
		{
			serialization = DataConnection.SerializationEnum.BINARY_UTF8;
		}
		else if (MultiPartyOptions.SerializationEnum.JSON == _options.serialization)
		{
			serialization = DataConnection.SerializationEnum.JSON;
		}
		else if (MultiPartyOptions.SerializationEnum.NONE == _options.serialization)
		{
			serialization = DataConnection.SerializationEnum.NONE;
		}

		ConnectOption option = new ConnectOption();
		option.serialization = serialization;
		option.reliable = _options.reliable;

		// Connecting data
		DataConnection data = _peer.connect(peerId, option);
		if (null != data)
		{
			connection.addDataConnection(data);
		}

		return true;
	}

	/**
	 * Begin connecting data connection
	 */
	private void startData(List<String> peers)
	{
		// Connecting to remotes

		boolean bConnecting = false;

		for (String peerId : peers)
		{
			bConnecting = startData(peerId);
			if (bConnecting)
			{
				try
				{
					_lck.lock();
					_cnd.await();
					_lck.unlock();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 *
	 * @param peerId
	 */
	boolean startCall(String peerId, boolean useLocalMedia)
	{
		if ((null == _peer) || (null == connections))
		{
			return false;
		}

		CallOption option = new CallOption();

		MultiPartyConnection connection = null;
		synchronized (connections)
		{
			connection = connections.get(peerId);
		}

		if (null == connection)
		{
			connection = new MultiPartyConnection();
			connection.peerId = peerId;
			connection.media = _options.getUseStream();
			connection.multipartyimpl = _multipartyimpl;

			connections.put(peerId, connection);
		}
		else
		{
			// Already connected
			if (0 < connection.medias.size())
			{
				return false;
			}
		}

		if (MultiPartyOptions.DebugLevelEnum.ALL_LOGS.ordinal() <= _options.debug.ordinal())
		{
			Log.i(TAG, "Calling to" + peerId);
		}

		MediaConnection media = null;
		if (useLocalMedia)
		{
			media = _peer.call(peerId, _msLocal, option);
		}
		else
		{
			// Screen share metadata
			StringBuilder sb = new StringBuilder();
			sb.append("{");
			sb.append(KEY_TYPE);
			sb.append(":");
			sb.append(TYPE_SCREEN);
			sb.append("}");
			option.metadata = sb.toString();

			media = _peer.call(peerId, null, option);
		}

		if (null != media)
		{
			connection.addMediaConnection(media);
		}

		return true;
	}

	/**
	 * Begin connecting media connection
	 */
	private void startCall(List<String> peers)
	{
		boolean bConnecting = false;

		for (String peerId : peers)
		{
			bConnecting = startCall(peerId, true);
			if (bConnecting)
			{
				try
				{
					_lck.lock();
					_cnd.await();
					_lck.unlock();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Raise event
	 * @param event Event type
	 * @param json Result JSON object
	 */
	void raiseEvent(MultiParty.MultiPartyEventEnum event, JSONObject json)
	{
		if (null == _handler)
		{
			return;
		}

		RunnableCallback rc = new RunnableCallback();

		// Set callback
		if (MultiParty.MultiPartyEventEnum.OPEN == event)
		{
			// Open
			rc.callback = _cbOpen;
		}
		else if (MultiParty.MultiPartyEventEnum.MY_MS == event)
		{
			// Opened own media stream
			rc.callback = _cbMyMs;
		}
		else if (MultiParty.MultiPartyEventEnum.PEER_MS == event)
		{
			// Opened remote media stream
			rc.callback = _cbPeerMs;
		}
		else if (MultiParty.MultiPartyEventEnum.MS_CLOSE == event)
		{
			// Closed remote media stream
			rc.callback = _cbMsClose;
		}
		else if (MultiParty.MultiPartyEventEnum.PEER_SS == event)
		{
			// Opened remote screen cast stream
			rc.callback = _cbPeerSs;
		}
		else if (MultiParty.MultiPartyEventEnum.SS_CLOSE == event)
		{
			// Closed remote screen cast stream
			rc.callback = _cbSsClose;
		}
		else if (MultiParty.MultiPartyEventEnum.DC_OPEN == event)
		{
			// Opened remote data channel
			rc.callback = _cbDcOpen;
		}
		else if (MultiParty.MultiPartyEventEnum.MESSAGE == event)
		{
			// Received remote data
			rc.callback = _cbMessage;
		}
		else if (MultiParty.MultiPartyEventEnum.DC_CLOSE == event)
		{
			// Closed remote data channel
			rc.callback = _cbDcClose;
		}
		else if (MultiParty.MultiPartyEventEnum.ERROR == event)
		{
			// Error
			rc.callback = _cbError;
		}

		if ((null != _lck) && (null != _cnd))
		{
			try
			{
				_lck.lock();
				_cnd.signal();
				_lck.unlock();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}

		if (null == rc.callback)
		{
			return;
		}

		rc.eventData = json;

		_handler.post(rc);
	}

	/**
	 * Get MultiParty handler
	 * @return
	 */
	Handler getHandler()
	{
		return _handler;
	}

	/**
	 * Get value from JSONString
	 * @param inValue JSONString
	 * @param key key String
	 * @return value
	 */
	private String getValueFromJSONString(String inValue, String key)
	{
		String resultValue = "";

		if ((null == inValue) || (0 == inValue.length()))
		{
			return resultValue;
		}

		try
		{
			JSONObject jsonMetadata = new JSONObject(inValue);
			Iterator<String> keys = jsonMetadata.keys();
			while (keys.hasNext())
			{
				String strKey = keys.next();
				if (0 == key.compareTo(strKey))
				{
					resultValue = jsonMetadata.getString(key);
					break;
				}
			}
		}
		catch (JSONException e)
		{
			e.printStackTrace();
		}

		if (null == resultValue)
		{
			resultValue = "";
		}

		return resultValue;
	}

	/**
	 * Make random string
	 * @param length Length
	 * @return Random string
	 */
	private String randomToken(int length)
	{
		final String strLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
		int iLetters = strLetters.length();

		StringBuilder sb = new StringBuilder();

		int iPos = 0;
		char chValue = 0;

		Random rnd = new Random();

		while (length > sb.length())
		{
			iPos = rnd.nextInt(iLetters);
			chValue = strLetters.charAt(iPos);
			sb.append(chValue);
		}

		return sb.toString();
	}

	/**
	 * Calc MD5
	 * @param seed Seed string
	 * @return MD5 string
	 */
	private String calcMD5(String seed)
	{
		String strResult = "";

		try
		{
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(seed.getBytes());

			byte digest[] = md.digest();

			StringBuilder sb = new StringBuilder();

			for (int i = 0 ; MD5_DIGEST_LENGTH > i ; i++)
			{
				String strValue = String.format("%02x", digest[i]);
				sb.append(strValue);
			}

			strResult = sb.toString();
		}
		catch (NoSuchAlgorithmException e)
		{
			e.printStackTrace();
		}

		return strResult;
	}

	/**
	 * Make ID string (32 chars)
	 * @return ID string
	 */
	private String makeID()
	{
		return randomToken(32);
	}

	/**
	 * Make room name
	 * @param seed seed string
	 * @return Room name
	 */
	private String makeRoomName(String seed)
	{
		if (null == seed)
		{
			seed = "";
		}

		StringBuilder sb = new StringBuilder(seed);
		String strHost = _options.locationHost;
		if (null != strHost)
		{
			sb.append(strHost);
		}
		String strPath = _options.locationPath;
		if (null != strPath)
		{
			sb.append(strPath);
		}

		String strMD5 = calcMD5(sb.toString());

		String strHead = strMD5.substring(0, 6);

		return String.format("%sR_", strHead);
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();

		sb.append("opened=");
		sb.append(opened);
		sb.append(" connections=");
		sb.append(connections.toString());

		return sb.toString();
	}
}
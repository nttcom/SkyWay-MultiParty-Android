package io.skyway.multiparty;


import java.util.ArrayList;

import io.skyway.Peer.Browser.MediaConstraints;
import io.skyway.Peer.IceConfig;
import io.skyway.Peer.PeerOption;


/**
 * MultiParty option
 */
public class MultiPartyOptions
{
	private static final String TAG = MultiPartyOptions.class.getSimpleName();

	/// Serialization
	public enum SerializationEnum
	{
		BINARY,				/// binary
		BINARY_UTF8,		/// binary-utf8
		JSON,				/// json
		NONE,				/// none
	}

	/// Debug output level
	public enum DebugLevelEnum
	{
		NO_LOGS,			/// No logs (Default)
		ONLY_ERROR,			/// Only error
		ERROR_AND_WARNING,	/// Error and warning
		ALL_LOGS,			/// All logs
	}

	public static final int DEFAULT_POLLING_INTERVAL = 3000;

	public String						key;				/// SkyWay API key.
	public String						domain;				/// Registered domain URL.
	public String						room;				/// Room name
	public String						identity;			/// User ID
	public boolean						reliable;			/// Reliability data
	public SerializationEnum			serialization;		/// Data serialization type.
	public MediaConstraints				constraints;		/// Local media constraints
	public boolean						polling;			/// Polling peers
	public int							polling_interval;	/// Polling interval
	public DebugLevelEnum				debug;				/// Output debug level.
	public String						locationHost;		/// location.host
	public String						locationPath;		/// location.pathname
	public String						host;				/// Host name.
	public int							port;				/// Port number.
	public boolean						secure;				/// Data using TLS connection.
	public ArrayList<IceConfig>			config;				/// ICE servers.
	public boolean						useSkyWayTurn;		/// Using SkyWay TURN server.
	public boolean						vp8hwcodec;			/// Using hardware codec VP8.

	private PeerOption					_peerOption;
	private boolean						_useStream;
	private String						_strRoomName;
	private String						_strRoomId;

	/**
	 * Constructor
	 */
	public MultiPartyOptions()
	{
		PeerOption defOption = new PeerOption();

		key = null;
		domain = defOption.domain;
		room = null;
		identity = null;
		reliable = false;
		serialization = SerializationEnum.BINARY;
		constraints = new MediaConstraints();
		polling = false;
		polling_interval = DEFAULT_POLLING_INTERVAL;
		debug = DebugLevelEnum.NO_LOGS;
		locationHost = "";
		locationPath = "";
		host = defOption.host;
		port = defOption.port;
		secure = defOption.secure;
		config = defOption.config;
		useSkyWayTurn = defOption.turn;
		vp8hwcodec = defOption.vp8hwcodec;

		_peerOption = null;
	}

	/**
	 * Set Peer option setting
	 * @param option Setting
	 */
	void setPeerOption(PeerOption option)
	{
		_peerOption = option;
	}

	/**
	 * Get Peer option setting
	 * @return Setting
	 */
	PeerOption getPeerOption()
	{
		return _peerOption;
	}

	/**
	 * Set using stream flag
	 * @param using Using stream flag
	 */
	void setUseStream(boolean using)
	{
		_useStream = using;
	}

	/**
	 * Get using stream flag
	 * @return Using stream flag
	 */
	boolean getUseStream()
	{
		return _useStream;
	}

	void setRoomName(String value)
	{
		_strRoomName = value;
	}

	String getRoomName()
	{
		return _strRoomName;
	}

	void setRoomId(String value)
	{
		_strRoomId = value;
	}

	String getRoomId()
	{
		return _strRoomId;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();

		sb.append("key:");
		sb.append(key);
		sb.append(" domain:");
		sb.append(domain);
		sb.append(" room:");
		sb.append(room);
		sb.append(" identity:");
		sb.append(identity);
		sb.append(" reliable:");
		sb.append(reliable);
		sb.append(" serialization:");
		sb.append(serialization);
		sb.append(" debug:");
		sb.append(debug);
		sb.append(" locationHost:");
		sb.append(locationHost);
		sb.append(" locationPath:");
		sb.append(locationPath);
		sb.append(" host:");
		sb.append(host);
		sb.append(" port:");
		sb.append(port);
		sb.append(" secure:");
		sb.append(secure);
		sb.append(" config:");
		sb.append(config);
		sb.append(" useSkyWayTurn:");
		sb.append(useSkyWayTurn);
		sb.append(" vp8hwcodec:");
		sb.append(vp8hwcodec);

		return sb.toString();
	}
}

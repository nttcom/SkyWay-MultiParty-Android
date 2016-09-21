package io.skyway.multiparty;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.skyway.Peer.Browser.MediaStream;
import io.skyway.Peer.DataConnection;
import io.skyway.Peer.MediaConnection;
import io.skyway.Peer.OnCallback;

/**
 * Connection
 */
class MultiPartyConnection
{
	public MultiPartyImpl			multipartyimpl;
	public String					peerId;
	public boolean					media;
	public List<MediaConnection>	medias;
	public List<DataConnection>		datas;
	public boolean					usedMedia;
	public boolean					usedData;
	public boolean					usedScreenShare;

	private static final String TYPE_ID        = "id";
	private static final String TYPE_SRC       = "src";
	private static final String TYPE_DATA      = "data";
	private static final String TYPE_RECONNECT = "reconnect";

	/**
	 * Constructor
	 */
	public MultiPartyConnection()
	{
		medias = new ArrayList<>();
		datas = new ArrayList<>();

		media = true;

		usedMedia = false;
		usedData = false;
		usedScreenShare = false;
	}

	/**
	 * Closing media connections
	 */
	public void closeMedia()
	{
		// Media connection
		if (null == medias)
		{
			return;
		}

		while (true)
		{
			if ((null == medias) || (0 == medias.size()))
			{
				break;
			}

			MediaConnection media = medias.get(0);
			removeMediaConnection(media);
		}
	}

	/**
	 * Closing screen share media connection
	 */
	public void closingScreenShare()
	{
		if (null == medias)
		{
			return;
		}

		while (true)
		{
			if ((null == medias) || (0 == medias.size()))
			{
				break;
			}

			MediaConnection media = medias.get(0);
			String value = getValueFromJSONString(media.metadata, MultiParty.KEY_TYPE);
			if (0 == value.compareTo(MultiParty.TYPE_SCREEN))
			{
				removeMediaConnection(media);
			}
		}
	}

	/**
	 * Closing data connections
	 */
	public void closeData()
	{
		// Data connection
		if (null == datas)
		{
			return;
		}

		while (true)
		{
			if ((null == datas) || (0 == datas.size()))
			{
				break;
			}

			DataConnection data = datas.get(0);
			removeDataConnection(data);
		}
	}

	/**
	 * Close remote connections
	 */
	public void close()
	{
		// Closing media connection
		closeMedia();

		medias = null;

		// Closing data connection
		closeData();

		datas = null;

		multipartyimpl = null;
	}

	/**
	 * Add media connection to media list
	 * @param media Media connection
	 */
	public void addMediaConnection(MediaConnection media)
	{
		if (null == media)
		{
			return;
		}

		if (null == medias)
		{
			medias = new ArrayList<>();
		}

		medias.add(media);

		// Add remote media stream
		media.on(MediaConnection.MediaEventEnum.STREAM, new OnCallback()
		{
			@Override
			public void onCallback(Object o)
			{
				if (null == multipartyimpl)
				{
					return;
				}

				boolean bReconnect = multipartyimpl.reconnecting;

				JSONObject json = new JSONObject();

				try
				{
					json.put(TYPE_ID, peerId);
					json.put(TYPE_SRC, o);
					json.put(TYPE_RECONNECT, bReconnect);
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}

				// Detect connection
				if (o instanceof MediaStream)
				{
					MediaStream stream = (MediaStream)o;
					MediaConnection media = findMediaConnection(stream);

					if (null != media)
					{
						String value = getValueFromJSONString(media.metadata, MultiParty.KEY_TYPE);
						if (0 == value.compareTo(MultiParty.TYPE_SCREEN))
						{
							usedScreenShare = true;

							multipartyimpl.raiseEvent(MultiParty.MultiPartyEventEnum.PEER_SS, json);

							return;
						}
					}
				}

				usedMedia = true;

				multipartyimpl.raiseEvent(MultiParty.MultiPartyEventEnum.PEER_MS, json);
			}
		});

		// Remove remote media stream
		media.on(MediaConnection.MediaEventEnum.REMOVE_STREAM, new OnCallback()
		{
			@Override
			public void onCallback(Object o)
			{
				if (null == multipartyimpl)
				{
					return;
				}

				JSONObject json = new JSONObject();

				try
				{
					json.put(TYPE_ID, peerId);
					json.put(TYPE_SRC, o);
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}

				// Detect connection
				if (o instanceof MediaStream)
				{
					MediaStream stream = (MediaStream)o;
					MediaConnection media = findMediaConnection(stream);

					if (null != media)
					{
						String value = getValueFromJSONString(media.metadata, MultiParty.KEY_TYPE);
						if (0 == value.compareTo(MultiParty.TYPE_SCREEN))
						{
							multipartyimpl.raiseEvent(MultiParty.MultiPartyEventEnum.SS_CLOSE, json);

							return;
						}
					}
				}

				multipartyimpl.raiseEvent(MultiParty.MultiPartyEventEnum.MS_CLOSE, json);
			}
		});

		// Close remote media stream
		media.on(MediaConnection.MediaEventEnum.CLOSE, new OnCallback()
		{
			@Override
			public void onCallback(Object o)
			{
				MediaConnection media = null;

				if (null != o)
				{
					if (o instanceof MediaConnection)
					{
						media = (MediaConnection) o;
					}
				}

				removeMediaConnection(media);
			}
		});

		// Error on media stream
		media.on(MediaConnection.MediaEventEnum.ERROR, new OnCallback()
		{
			@Override
			public void onCallback(Object o)
			{
				if (null == multipartyimpl)
				{
					return;
				}

				JSONObject json = new JSONObject();

				try
				{
					json.putOpt(TYPE_ID, peerId);
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}

				multipartyimpl.raiseEvent(MultiParty.MultiPartyEventEnum.ERROR, json);
			}
		});
	}

	/**
	 * Remove media connection from media list
	 * @param media Media connection
	 */
	public void removeMediaConnection(MediaConnection media)
	{
		if (null == media)
		{
			return;
		}

		if (media.isOpen)
		{
			media.close();
		}

		media.on(MediaConnection.MediaEventEnum.STREAM, null);
		media.on(MediaConnection.MediaEventEnum.REMOVE_STREAM, null);
		media.on(MediaConnection.MediaEventEnum.CLOSE, null);
		media.on(MediaConnection.MediaEventEnum.ERROR, null);

		if (null != medias)
		{
			medias.remove(media);
		}

		if ((null != medias) && (0 == medias.size()) && (null != datas) && (0 == datas.size()))
		{
			if (null != multipartyimpl)
			{
				multipartyimpl.removePeer(peerId);
			}
		}
	}

	/**
	 * Get value from JSON string
	 * @param inValue JSON string
	 * @param key Key string
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
	 * Find media connection from mediastream
	 * @param stream MediaStream
	 * @return MediaConnecton object
	 */
	private MediaConnection findMediaConnection(MediaStream stream)
	{
		MediaConnection media = null;

		for (MediaConnection connection : medias)
		{
			if (null == connection.remote)
			{
				continue;
			}

			for (MediaStream remoteStream : connection.remote)
			{
				if (stream == remoteStream)
				{
					media = connection;
					break;
				}
			}

			if (null != media)
			{
				break;
			}
		}

		return media;
	}

	/**
	 * Add data connection to data list
	 * @param data Data connection
	 */
	public void addDataConnection(DataConnection data)
	{
		if (null == data)
		{
			return;
		}

		if (null == datas)
		{
			datas = new ArrayList<>();
		}

		datas.add(data);

		// Opened data channel
		data.on(DataConnection.DataEventEnum.OPEN, new OnCallback()
		{
			@Override
			public void onCallback(Object o)
			{
				JSONObject json = new JSONObject();

				try
				{
					json.put(TYPE_ID, peerId);
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}

				if (null == multipartyimpl)
				{
					return;
				}

				usedData = true;

				multipartyimpl.raiseEvent(MultiParty.MultiPartyEventEnum.DC_OPEN, json);
			}
		});

		// Received data
		data.on(DataConnection.DataEventEnum.DATA, new OnCallback()
		{
			@Override
			public void onCallback(Object o)
			{
				JSONObject json = new JSONObject();

				try
				{
					json.put(TYPE_ID, peerId);
					json.put(TYPE_DATA, o);
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}

				if (null == multipartyimpl)
				{
					return;
				}

				multipartyimpl.raiseEvent(MultiParty.MultiPartyEventEnum.MESSAGE, json);
			}
		});

		// Closed data channel
		data.on(DataConnection.DataEventEnum.CLOSE, new OnCallback()
		{
			@Override
			public void onCallback(Object o)
			{
				DataConnection data = null;

				if (null != o)
				{
					if (o instanceof DataConnection)
					{
						data = (DataConnection)o;
					}
				}

				JSONObject json = new JSONObject();

				try
				{
					json.put(TYPE_ID, peerId);
					json.put(TYPE_SRC, o);
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}

				if (null == multipartyimpl)
				{
					return;
				}

				multipartyimpl.raiseEvent(MultiParty.MultiPartyEventEnum.DC_CLOSE, json);

				removeDataConnection(data);
			}
		});

		// Error on data channel
		data.on(DataConnection.DataEventEnum.ERROR, new OnCallback() {
			@Override
			public void onCallback(Object o) {
				JSONObject json = new JSONObject();

				try
				{
					json.put(TYPE_ID, peerId);
				}
				catch (JSONException e)
				{
					e.printStackTrace();
				}

				if (null == multipartyimpl) {
					return;
				}

				multipartyimpl.raiseEvent(MultiParty.MultiPartyEventEnum.ERROR, json);
			}
		});
	}

	/**
	 * Remove data connection from data list
	 * @param data Data connection
	 */
	public void removeDataConnection(DataConnection data)
	{
		if (null == data)
		{
			return;
		}

		if (data.isOpen)
		{
			data.close();
		}

		data.on(DataConnection.DataEventEnum.OPEN, null);
		data.on(DataConnection.DataEventEnum.DATA, null);
		data.on(DataConnection.DataEventEnum.CLOSE, null);
		data.on(DataConnection.DataEventEnum.ERROR, null);

		if (null != datas)
		{
			datas.remove(data);

			if ((null != medias) && (0 == medias.size()) && (0 == datas.size()))
			{
				if (null != multipartyimpl)
				{
					multipartyimpl.removePeer(peerId);
				}
			}
		}
	}
}

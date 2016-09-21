package io.skyway.multiparty;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import io.skyway.Peer.OnCallback;
import io.skyway.Peer.Peer;

/**
 *
 */
class RunnableListing implements Runnable
{
	/// Listing callback
	public OnListing	listing;
	/// Peer
	public Peer			peer;
	/// Own Id
	public String		ownId;
	/// Head mask
	public String		head;

	@Override
	public void run()
	{
		if ((null == peer) || (null == listing))
		{
			return;
		}

		peer.listAllPeers(new OnCallback()
		{
			@Override
			public void onCallback(Object o)
			{
				List<String> list = new ArrayList<>();

				if (null == o)
				{
					// Nothing
				}
				else if (o instanceof JSONArray)
				{
					// JSON array object
					JSONArray ary = (JSONArray)o;
					for (int i = 0 ; ary.length() > i ; i++)
					{
						String strValue = "";
						try
						{
							strValue = ary.getString(i);
						}
						catch (JSONException e)
						{
							e.printStackTrace();
						}

						if ((null != ownId) && (0 < ownId.length()))
						{
							if (0 == strValue.compareTo(ownId))
							{
								continue;
							}
						}

						if ((null != head) && (0 < head.length()))
						{
							if (!strValue.startsWith(head))
							{
								continue;
							}
						}

						list.add(strValue);
					}
				}
				else if (o instanceof ArrayList)
				{
					// Array list object
					ArrayList ary = (ArrayList)o;
					for (int i = 0 ; ary.size() > i ; i++)
					{
						Object obj = ary.get(i);

						if (obj instanceof String)
						{
							String strValue = (String)obj;

							if ((null != ownId) && (0 < ownId.length()))
							{
								if (0 == strValue.compareTo(ownId))
								{
									continue;
								}
							}

							if ((null != head) && (0 < head.length()))
							{
								if (!strValue.startsWith(head))
								{
									continue;
								}
							}

							list.add(strValue);
						}
					}
				}
				else if (o instanceof List)
				{
					// List object
					List lst = (List)o;
					for (int i = 0 ; lst.size() > i ; i++)
					{
						Object obj = lst.get(i);

						if (obj instanceof String)
						{
							String strValue = (String)obj;

							if ((null != ownId) && (0 < ownId.length()))
							{
								if (0 == strValue.compareTo(ownId))
								{
									continue;
								}
							}

							if ((null != head) && (0 < head.length()))
							{
								if (!strValue.startsWith(head))
								{
									continue;
								}
							}

							list.add(strValue);
						}
					}
				}

				if (null != listing)
				{
					listing.onListing(list);
				}
			}
		});
	}
}

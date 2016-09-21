package io.skyway.multiparty;

import org.json.JSONObject;

/**
 * Callback runnable
 */
class RunnableCallback implements Runnable
{
	/// Callback
	public OnCallback	callback;
	/// Result JSON object
	public JSONObject	eventData;

	@Override
	public void run()
	{
		if (null == callback)
		{
			return;
		}

		callback.onCallback(eventData);
	}
}

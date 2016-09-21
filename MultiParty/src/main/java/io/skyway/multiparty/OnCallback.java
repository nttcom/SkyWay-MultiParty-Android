package io.skyway.multiparty;

import org.json.JSONObject;

/**
 * Callback interface.
 */
public interface OnCallback
{
	/**
	 * Callback method
	 * @param object Result JSON object.
	 */
	public void onCallback(JSONObject object);
}

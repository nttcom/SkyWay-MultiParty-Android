package io.skyway.multiparty;

import android.os.Handler;

/**
 * Connecting runnable
 */
public class RunnableConnect implements Runnable
{
	/// MultiParty
	public MultiPartyImpl multiparty;
	/// Remote peer Id
	public String peerId;
	/// media connection
	public boolean media;
	/// data connection
	public boolean data;
	/// screen share media connection
	public boolean screen;

	@Override
	public void run()
	{
		if (data)
		{
			multiparty.startData(peerId);
		}
		else if (media)
		{
			multiparty.startCall(peerId, true);
		}
		else if (screen)
		{
			multiparty.startCall(peerId, false);
		}
	}
}

/*
 * Copyright (c) 2019-2023 by David Gerber - https://zapek.com
 *
 * This file is part of Xeres.
 *
 * Xeres is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Xeres is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Xeres.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.xeres.app.xrs.service.turtle;

import io.xeres.app.xrs.service.turtle.item.TurtleSearchRequestItem;
import io.xeres.app.xrs.service.turtle.item.TurtleTunnelRequestItem;
import io.xeres.common.util.SecureRandomUtils;

import static io.xeres.app.xrs.service.turtle.TurtleRsService.MAX_TUNNEL_DEPTH;

class TunnelProbability
{
	private static final int TUNNEL_REQUEST_PACKET_SIZE = 50;

	private static final int MAX_TUNNEL_REQUEST_FORWARD_PER_SECOND = 20; // XXX: this one is settable in RS, decide what to do

	private static final int DISTANCE_SQUEEZING_POWER = 8;

	private static final double[] DEPTH_PEER_PROBABILITY = new double[]{1.0, 0.99, 0.9, 0.7, 0.6, 0.5, 0.4};

	private final int bias;

	public TunnelProbability()
	{
		bias = SecureRandomUtils.nextInt();
	}

	/**
	 * Finds out if a search request subclass is forwardable. Its depth has to be lower than MAX_TUNNEL_DEPTH. There's a random
	 * bias to let some packets pass to avoid a successful search by depth attack.
	 *
	 * @param item a TurtleSearchRequestItem
	 * @return true if forwardable
	 */
	public boolean isForwardable(TurtleSearchRequestItem item)
	{
		return isForwardable(item.getRequestId(), item.getDepth());
	}

	/**
	 * Finds out if a tunnel request is forwardable. Its depth has to be lower than MAX_TUNNEL_DEPTH. There's a random
	 * bias to let some packets pass to avoid a successful search by depth attack.
	 *
	 * @param item a TurtleTunnelRequestItem
	 * @return true if forwardable
	 */
	public boolean isForwardable(TurtleTunnelRequestItem item)
	{
		return isForwardable(item.getPartialTunnelId(), item.getDepth());
	}

	private boolean isForwardable(int id, int depth)
	{
		var randomBypass = depth >= MAX_TUNNEL_DEPTH && (((bias ^ id) & 0x7) == 2);

		return depth < MAX_TUNNEL_DEPTH || randomBypass;
	}

	public void incrementDepth(TurtleSearchRequestItem item)
	{
		item.setDepth(incrementDepth(item.getRequestId(), item.getDepth()));
	}

	public void incrementDepth(TurtleTunnelRequestItem item)
	{
		item.setDepth(incrementDepth(item.getPartialTunnelId(), item.getDepth()));
	}

	private short incrementDepth(int id, short depth)
	{
		var randomDepthSkipShift = depth == 1 && (((bias ^ id) & 0x7) == 6);

		if (!randomDepthSkipShift)
		{
			depth++;
		}
		return depth;
	}

	public int getBias()
	{
		return bias;
	}

	/**
	 * Gets the forwarding probability of a tunnel request. A particular care is taken to not flood the network:
	 * <ul>
	 *     <li>if the number of tunnel requests to forward per seconds is below {@code MAX_TUNNEL_REQUEST_FORWARD_PER_SECOND}, keep the traffic</li>
	 *     <li>if the limit is approached, start dropping with long tunnels first</li>
	 * </ul>
	 * Variables involved:
	 * <ul>
	 *     <li>distanceToMaximum: in [0,inf] is the proportion of the current up TR speed with respect to the maximum allowed speed. This is estimated
	 *     as an average between the average number of TR over the 60 last seconds and the current TR up speed</li>
	 *     <li>correctedDistance: in [0,inf] is a squeezed version of distance: small values become very small and large values become very large</li>
	 *     <li>{@code DEPTH_PEER_PROBABILITY}: basic probability of forwarding when the speed limit is reached</li>
	 *     <li>forwardProbability: final probability of forwarding the packet, per peer</li>
	 * </ul>
	 * When the number of peers increases, the speed limit is reached faster, but the behavior per peer is the same.
	 *
	 * @param item                   the tunnel request item
	 * @param tunnelRequestsUpload   the bandwidth of tunnel requests (up) in bytes per seconds
	 * @param tunnelRequestsDownload the bandwidth of tunnel requests (down) in bytes per seconds
	 * @param numberOfPeers          the number of connected peers
	 * @return a probability number between 0.0 and 1.0
	 */
	public double getForwardingProbability(TurtleTunnelRequestItem item, double tunnelRequestsUpload, double tunnelRequestsDownload, int numberOfPeers)
	{
		var distanceToMaximum = Math.min(100.0, tunnelRequestsUpload / (TUNNEL_REQUEST_PACKET_SIZE * MAX_TUNNEL_REQUEST_FORWARD_PER_SECOND));
		var correctedDistance = Math.pow(distanceToMaximum, DISTANCE_SQUEEZING_POWER);
		var forwardProbability = Math.pow(DEPTH_PEER_PROBABILITY[Math.min(DEPTH_PEER_PROBABILITY.length - 1, item.getDepth())], correctedDistance);

		if (forwardProbability * numberOfPeers < 1.0 && numberOfPeers > 0)
		{
			forwardProbability = 1.0 / numberOfPeers;

			if (tunnelRequestsDownload / TUNNEL_REQUEST_PACKET_SIZE > MAX_TUNNEL_REQUEST_FORWARD_PER_SECOND)
			{
				forwardProbability *= MAX_TUNNEL_REQUEST_FORWARD_PER_SECOND * TUNNEL_REQUEST_PACKET_SIZE / tunnelRequestsDownload;
			}
		}
		return forwardProbability;
	}
}

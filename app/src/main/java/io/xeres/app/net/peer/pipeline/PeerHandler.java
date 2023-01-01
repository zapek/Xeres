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

package io.xeres.app.net.peer.pipeline;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslHandshakeCompletionEvent;
import io.netty.util.ReferenceCountUtil;
import io.xeres.app.database.DatabaseSession;
import io.xeres.app.database.DatabaseSessionManager;
import io.xeres.app.database.model.location.Location;
import io.xeres.app.net.peer.ConnectionType;
import io.xeres.app.net.peer.PeerAttribute;
import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.net.peer.ssl.SSL;
import io.xeres.app.service.LocationService;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.item.RawItem;
import io.xeres.app.xrs.service.serviceinfo.ServiceInfoRsService;
import io.xeres.app.xrs.service.sliceprobe.item.SliceProbeItem;
import io.xeres.ui.support.tray.TrayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import javax.net.ssl.SSLPeerUnverifiedException;
import java.net.ProtocolException;
import java.security.cert.CertificateException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static io.xeres.app.net.peer.ConnectionType.TCP_INCOMING;

public class PeerHandler extends ChannelDuplexHandler
{
	private static final Logger log = LoggerFactory.getLogger(PeerHandler.class);

	private final ConnectionType connectionType;
	private final LocationService locationService;
	private final PeerConnectionManager peerConnectionManager;
	private final DatabaseSessionManager databaseSessionManager;
	private final ServiceInfoRsService serviceInfoRsService;
	private final TrayService trayService;

	public PeerHandler(LocationService locationService, PeerConnectionManager peerConnectionManager, DatabaseSessionManager databaseSessionManager, ServiceInfoRsService serviceInfoRsService, ConnectionType connectionType, TrayService trayService)
	{
		super();
		this.serviceInfoRsService = serviceInfoRsService;
		this.connectionType = connectionType;
		this.peerConnectionManager = peerConnectionManager;
		this.databaseSessionManager = databaseSessionManager;
		this.locationService = locationService;
		this.trayService = trayService;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg)
	{
		var peerConnection = ctx.channel().attr(PeerAttribute.PEER_CONNECTION).get();

		// Drop messages if SSL peer is not validated
		if (peerConnection == null)
		{
			log.debug("Dropping message as SSL not validated");
			ReferenceCountUtil.release(msg);
			return;
		}

		log.trace("Got message: {}", msg);
		var rawItem = (RawItem) msg;
		Item item = null;
		var sessionBound = false;

		try
		{
			item = rawItem.deserialize();

			var service = item.getService();
			if (service != null)
			{
				var handleItemMethod = service.getClass().getDeclaredMethod("handleItem", PeerConnection.class, Item.class);
				if (handleItemMethod.isAnnotationPresent(Transactional.class))
				{
					sessionBound = databaseSessionManager.bindSession();
				}
				service.handleItem(peerConnection, item);
			}
			else
			{
				log.warn("Unknown item. Ignoring.");
			}
		}
		catch (IllegalArgumentException | NoSuchMethodException e)
		{
			log.error("Failed to deserialize: {}, {}", e.getClass().getSimpleName(), e.getMessage(), e);
			rawItem.dispose();
			item = null; // Don't dispose twice
		}
		finally
		{
			if (sessionBound)
			{
				databaseSessionManager.unbindSession();
			}

			if (item != null)
			{
				item.dispose(); // Dispose the item
			}
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
	{
		if (cause instanceof TooLongFrameException || cause instanceof ProtocolException)
		{
			log.error("Protocol error: {}, {}, closing connection...", cause.getClass(), cause.getMessage());
			log.trace("Stacktrace: ", cause);
			ctx.close();
		}
		else
		{
			log.error("Exception in channel:", cause);
		}
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx)
	{
		log.info("{} connection with {}", connectionType == TCP_INCOMING ? "Incoming" : "Outgoing", ctx.channel().remoteAddress());
		ctx.channel().attr(PeerAttribute.MULTI_PACKET).set(false);
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt)
	{
		if (evt instanceof SslHandshakeCompletionEvent sslHandshakeCompletionEvent)
		{
			if (!sslHandshakeCompletionEvent.isSuccess())
			{
				log.error("SSL handshake failed: {}", sslHandshakeCompletionEvent.cause().getMessage());
				ctx.close();
				return;
			}

			try (var ignored = new DatabaseSession(databaseSessionManager))
			{
				Location location;

				synchronized (PeerHandler.class) // Make sure we cannot have an outgoing and incoming connection with the same peer at the same time
				{
					location = SSL.checkPeerCertificate(locationService, ctx.pipeline().get(SslHandler.class).engine().getSession().getPeerCertificates());
					var peerConnection = peerConnectionManager.addPeer(location, ctx);
					locationService.setConnected(location, ctx.channel().remoteAddress());
					peerConnection.schedule(() -> serviceInfoRsService.init(peerConnection), ThreadLocalRandom.current().nextInt(2, 9), TimeUnit.SECONDS);
				}

				var message = "Established " + connectionType.getDescription() + " connection with " + location.getProfile().getName() + " (" + location.getName() + ")";

				log.info(message);
				trayService.showNotification(message);

				sendSliceProbe(ctx);
			}
			catch (CertificateException | SSLPeerUnverifiedException e)
			{
				log.error("Certificate error: {}", e.getMessage());
				ctx.close();
			}
		}
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx)
	{
		log.debug("Closing connection with {}", ctx.channel().remoteAddress());
		var peerConnection = ctx.channel().attr(PeerAttribute.PEER_CONNECTION).get();
		if (peerConnection != null)
		{
			peerConnection.cleanup();
			locationService.setDisconnected(peerConnection.getLocation());
			peerConnectionManager.removePeer(peerConnection.getLocation());
		}
	}

	private void sendSliceProbe(ChannelHandlerContext ctx)
	{
		var sliceProbeItem = SliceProbeItem.from(ctx);
		PeerConnectionManager.writeItem(ctx, sliceProbeItem); // this makes the remote RS send packets in the new format
	}
}

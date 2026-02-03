/*
 * Copyright (c) 2019-2026 by David Gerber - https://zapek.com
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

package io.xeres.app.xrs.service;

import io.xeres.app.xrs.RsDeprecated;
import io.xeres.app.xrs.service.bandwidth.BandwidthRsService;
import io.xeres.app.xrs.service.board.BoardRsService;
import io.xeres.app.xrs.service.channel.ChannelRsService;
import io.xeres.app.xrs.service.chat.ChatRsService;
import io.xeres.app.xrs.service.discovery.DiscoveryRsService;
import io.xeres.app.xrs.service.filetransfer.FileTransferRsService;
import io.xeres.app.xrs.service.forum.ForumRsService;
import io.xeres.app.xrs.service.gxstunnel.GxsTunnelRsService;
import io.xeres.app.xrs.service.heartbeat.HeartbeatRsService;
import io.xeres.app.xrs.service.identity.IdentityRsService;
import io.xeres.app.xrs.service.rtt.RttRsService;
import io.xeres.app.xrs.service.serviceinfo.ServiceInfoRsService;
import io.xeres.app.xrs.service.status.StatusRsService;
import io.xeres.app.xrs.service.turtle.TurtleRsService;

/**
 * The registry of Retroshare service types. Do not change their names, as they're also checked for matches.
 */
public enum RsServiceType
{
	NONE(0, null, 0, 0, 0, 0),

	/**
	 * The {@link DiscoveryRsService}.
	 */
	DISCOVERY(0x11, "disc", 1, 0, 1, 0),

	/**
	 * The {@link ChatRsService}.
	 */
	CHAT(0x12, "chat", 1, 0, 1, 0),

	/**
	 * The messaging service (direct mail, etc...).
	 */
	MESSAGES(0x13, "msg", 1, 0, 1, 0),

	/**
	 * The {@link TurtleRsService}.
	 */
	TURTLE_ROUTER(0x14, "turtle", 1, 0, 1, 0),

	@RsDeprecated
	TUNNEL(0x15, null, 1, 0, 1, 0),

	/**
	 * The {@link HeartbeatRsService}.
	 */
	HEARTBEAT(0x16, "heartbeat", 1, 0, 1, 0),

	/**
	 * The {@link FileTransferRsService}.
	 */
	FILE_TRANSFER(0x17, "ft", 1, 0, 1, 0),

	/**
	 * The global router.
	 */
	GLOBAL_ROUTER(0x18, "Global Router", 1, 0, 1, 0),

	/**
	 * The file database transfer service.
	 */
	FILE_DATABASE(0x19, "file_database", 1, 0, 1, 0),

	/**
	 * The {@link ServiceInfoRsService}.
	 */
	SERVICE_INFO(0x20, "serviceinfo", 1, 0, 1, 0),

	/**
	 * The {@link BandwidthRsService}.
	 */
	BANDWIDTH_CONTROL(0x21, "bandwidth_ctrl", 1, 0, 1, 0),

	/**
	 * Claims to be new but was never used somehow.
	 */
	@RsDeprecated
	MAIL(0x22, null, 1, 0, 1, 0),

	/**
	 * Direct mail messages to a location ID.
	 */
	DIRECT_MAIL(0x23, "msgdirect", 1, 0, 1, 0),

	@RsDeprecated
	DISTANT_MAIL(0x24, null, 1, 0, 1, 0),

	@RsDeprecated
	GWEMAIL_MAIL(0x25, null, 1, 0, 1, 0),

	/**
	 * RS uses it internally for saving which services are permitted or not to other users.
	 */
	SERVICE_CONTROL(0x26, null, 1, 0, 1, 0),

	@RsDeprecated
	DISTANT_CHAT(0x27, null, 1, 0, 1, 0),

	/**
	 * The {@link GxsTunnelRsService}.
	 */
	GXS_TUNNELS(0x28, "GxsTunnels", 1, 0, 1, 0),

	/**
	 * IP filter list exchange.
	 */
	BANLIST(0x101, "banlist", 1, 0, 1, 0),

	/**
	 * The {@link StatusRsService}.
	 */
	STATUS(0x102, "status", 1, 0, 1, 0),

	/**
	 * RS has an optional standalone friend server, which dispatches friends on a Tor link.
	 */
	FRIEND_SERVER(0x103, null, 1, 0, 1, 0),

	/**
	 * Just a placeholder?
	 */
	NXS(0x200, null, 1, 0, 1, 0),

	/**
	 * The {@link IdentityRsService}.
	 */
	GXS_IDENTITY(0x211, "gxsid", 1, 0, 1, 0),

	/**
	 * Photo album, not finished.
	 */
	GXS_PHOTO(0x212, "gxsphoto", 1, 0, 1, 0),

	/**
	 * Wiki service.
	 */
	GXS_WIKI(0x213, "gxswiki", 1, 0, 1, 0),

	/**
	 * Twitter clone.
	 */
	GXS_WIRE(0x214, "gxswire", 1, 0, 1, 0),

	/**
	 * The {@link ForumRsService}.
	 */
	GXS_FORUMS(0x215, "gxsforums", 1, 0, 1, 0),

	/**
	 * The {@link BoardRsService}.
	 */
	GXS_BOARDS(0x216, "gxsposted", 1, 0, 1, 0),

	/**
	 * The {@link ChannelRsService}.
	 */
	GXS_CHANNELS(0x217, "gxschannels", 1, 0, 1, 0),

	/**
	 * The GXS circles.
	 */
	GXS_CIRCLES(0x218, "gxscircle", 1, 0, 1, 0),

	/**
	 * Identity reputation transfer.
	 */
	GXS_REPUTATION(0x219, "gxsreputation", 1, 0, 1, 0),

	@RsDeprecated
	GXS_RECOGN(0x220, null, 1, 0, 1, 0),

	/**
	 * Asynchronous mail delivery on top of GXS. Can be used to send messages when offline.
	 * In RS, was implemented by chat (was) and is implemented by distant mail.
	 */
	GXS_MAILS(0x230, "GXS Mails", 1, 0, 1, 0),

	/**
	 * Used internally by RS for serialization.
	 */
	JSONAPI(0x240, null, 1, 0, 1, 0),

	/**
	 * Used by RS for serialization.
	 */
	FORUMS_CONFIG(0x315, null, 1, 0, 1, 0),

	/**
	 * Used by RS for serialization.
	 */
	POSTED_CONFIG(0x316, null, 1, 0, 1, 0),

	/**
	 * Used by RS for serialization.
	 */
	CHANNELS_CONFIG(0x317, null, 1, 0, 1, 0),

	/**
	 * Experimental Destination-Sequenced Distance Vector routing in RS. Disabled.
	 */
	DSDV(0x1010, "dsdv", 1, 0, 1, 0),

	/**
	 * The {@link RttRsService}.
	 */
	RTT(0x1011, "rtt", 1, 0, 1, 0),

	// plugins
	ARADO_ID(0x2001, null, 1, 0, 1, 0),
	RETRO_CHESS(0x2002, "RetroChess", 1, 0, 1, 0),
	FEEDREADER(0x2003, "FEEDREADER", 1, 0, 1, 0),

	/**
	 * The VoIP service. Implemented as a plugin in RS, built-in in Xeres.
	 */
	VOIP(0xA021, "VOIP", 1, 0, 1, 0),

	/**
	 * GXS distant sync. Implemented for channels in RS.
	 */
	GXS_DISTANT_SYNC(0x2233, "GxsNetTunnel", 1, 0, 1, 0),

	// packet slicing
	PACKET_SLICING_PROBE(0xAABB, "SlicingProbe", 1, 0, 1, 0),

	// Nabu's experimental services
	ZERO_RESERVE(0xBEEF, null, 1, 0, 1, 0),
	FIDO_GW(0xF1D0, null, 1, 0, 1, 0);

	private final int type;
	private final String name;
	private final short versionMajor;
	private final short versionMinor;
	private final short minVersionMajor;
	private final short minVersionMinor;

	RsServiceType(int type, String name, int versionMajor, int versionMinor, int minVersionMajor, int minVersionMinor)
	{
		this.type = type;
		this.name = name;
		this.versionMajor = (short) versionMajor;
		this.versionMinor = (short) versionMinor;
		this.minVersionMajor = (short) minVersionMajor;
		this.minVersionMinor = (short) minVersionMinor;
	}

	public int getType()
	{
		return type;
	}

	public String getName()
	{
		return name;
	}

	public short getVersionMajor()
	{
		return versionMajor;
	}

	public short getVersionMinor()
	{
		return versionMinor;
	}

	public short getMinVersionMajor()
	{
		return minVersionMajor;
	}

	public short getMinVersionMinor()
	{
		return minVersionMinor;
	}
}

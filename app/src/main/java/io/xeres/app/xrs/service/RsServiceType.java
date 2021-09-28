/*
 * Copyright (c) 2019-2021 by David Gerber - https://zapek.com
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

public enum RsServiceType
{
	NONE(0, null, 0, 0, 0, 0),
	GOSSIP_DISCOVERY(0x11, "disc", 1, 0, 1, 0),
	CHAT(0x12, "chat", 1, 0, 1, 0),
	MSG(0x13, "msg", 1, 0, 1, 0),
	TURTLE(0x14, "turtle", 1, 0, 1, 0),
	TUNNEL(0x15, null, 1, 0, 1, 0),
	HEARTBEAT(0x16, "heartbeat", 1, 0, 1, 0),
	FILE_TRANSFER(0x17, "ft", 1, 0, 1, 0),
	GROUTER(0x18, "Global Router", 1, 0, 1, 0),
	FILE_DATABASE(0x19, "file_database", 1, 0, 1, 0),
	SERVICEINFO(0x20, "serviceinfo", 1, 0, 1, 0),
	BANDWIDTH_CONTROL(0x21, "bandwidth_ctrl", 1, 0, 1, 0),
	MAIL(0x22, null, 1, 0, 1, 0),
	DIRECT_MAIL(0x23, "msgdirect", 1, 0, 1, 0),
	DISTANT_MAIL(0x24, null, 1, 0, 1, 0),
	GWEMAIL_MAIL(0x25, null, 1, 0, 1, 0),
	SERVICE_CONTROL(0x26, null, 1, 0, 1, 0),
	DISTANT_CHAT(0x27, null, 1, 0, 1, 0),
	GXS_TUNNEL(0x28, "GxsTunnels", 1, 0, 1, 0),
	BANLIST(0x101, "banlist", 1, 0, 1, 0),
	STATUS(0x102, "status", 1, 0, 1, 0),
	NXS(0x200, null, 1, 0, 1, 0),
	GXSID(0x211, "gxsid", 1, 0, 1, 0),
	PHOTO(0x212, "gxsphoto", 1, 0, 1, 0),
	WIKI(0x213, "gxswiki", 1, 0, 1, 0),
	WIRE(0x214, "gxswire", 1, 0, 1, 0),
	FORUMS(0x215, "gxsforums", 1, 0, 1, 0),
	POSTED(0x216, "gxsposted", 1, 0, 1, 0),
	CHANNELS(0x217, "gxschannels", 1, 0, 1, 0),
	GXSCIRCLE(0x218, "gxscircle", 1, 0, 1, 0),
	REPUTATION(0x219, "gxsreputation", 1, 0, 1, 0),
	GXS_RECOGN(0x220, null, 1, 0, 1, 0),
	GXS_TRANS(0x230, "GXS Mails", 0, 1, 0, 1),
	JSONAPI(0x240, null, 1, 0, 1, 0),
	FORUMS_CONFIG(0x315, null, 1, 0, 1, 0),
	POSTED_CONFIG(0x316, null, 1, 0, 1, 0),
	CHANNELS_CONFIG(0x317, null, 1, 0, 1, 0),
	RTT(0x1011, "rtt", 1, 0, 1, 0),
	// plugins
	PLUGIN_ARADO_ID(0x2001, null, 1, 0, 1, 0),
	PLUGIN_QCHESS_ID(0x2002, "RetroChess", 1, 0, 1, 0),
	PLUGIN_FEEDREADER(0x2003, "FEEDREADER", 1, 0, 1, 0),
	PLUGIN_VOIP(0xA021, "VOIP", 1, 0, 1, 0),
	// packet slicing
	PACKET_SLICING_PROBE(0xAABB, "SlicingProbe", 1, 0, 1, 0),
	// Nabu's experimental services
	PLUGIN_ZERORESERVE(0xBEEF, null, 1, 0, 1, 0),
	PLUGIN_FIDO_GW(0xF1D0, null, 1, 0, 1, 0);

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

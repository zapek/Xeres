/*
 * Copyright (c) 2026 by David Gerber - https://zapek.com
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

// noinspection JSUnresolvedReference

// Called when receiving a private chat message
xeres.registerEventHandler("chatPrivateMessage", function (data)
{
	console.log(`Received private message from ${data.location} with content: ${data.content}`);

	switch (xeres.getAvailability())
	{
		case "AWAY":
			xeres.sendPrivateMessage(data.location, "Sorry but I'm away. I'll reply when I'm back.");
			break;

		case "BUSY":
			xeres.sendPrivateMessage(data.location, "Sorry but I'm busy right now. I'll reply when I'm available again.");
			break;
	}
});

// Called when receiving a distant chat message
xeres.registerEventHandler("chatDistantMessage", function (data)
{
	console.log(`Received distant message from ${data.gxsId} with content: ${data.content}`);

	switch (xeres.getAvailability())
	{
		case "AWAY":
			xeres.sendDistantMessage(data.gxsId, "Sorry but I'm away. I'll reply when I'm back.");
			break;

		case "BUSY":
			xeres.sendDistantMessage(data.gxsId, "Sorry but I'm busy right now. I'll reply when I'm available again.");
			break;
	}
});
/*
 * Copyright (c) 2025 by David Gerber - https://zapek.com
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

// This is an example user script for Xeres.
// All scripts use ECMA script version 2025 and up in strict mode.
// It has to be placed in %APPDATA%\Xeres\Scripts\user.js

// There are many events that you can register for below.

// Called when receiving a chat room message
xeresAPI.registerEventHandler("chatRoomMessage", function (data)
{
	console.log(`Received chat room message from ${data.nickname} with content: ${data.content}`);

	if (data.content === '!f1')
	{
		xeresAPI.sendChatRoomMessage(data.roomId, `${data.nickname}: ${getRandomDriver()} ${getRandomAction()} ${getRandomDriver()}`);
	}
});

// Called when receiving a private chat message
xeresAPI.registerEventHandler("chatPrivateMessage", function (data)
{
	console.log(`Received private message from ${data.location} with content: ${data.content}`);

	xeresAPI.sendPrivateMessage(data.location, "hey! I got your message");
});

// Called when receiving a distant chat message
xeresAPI.registerEventHandler("chatDistantMessage", function (data)
{
	console.log(`Received distant message from ${data.gxsId} with content: ${data.content}`);

	xeresAPI.sendDistantMessage(data.gxsId, "hey! I got your message");
});

// Called when someone joins a room
xeresAPI.registerEventHandler("chatRoomJoin", function (data)
{
	console.log(`User ${data.nickname} (${data.gxsId}) joined chat room ${data.roomId}`);

	xeresAPI.sendChatRoomMessage(data.roomId, `welcome ${data.nickname}!`);
});

// Called when getting a chat room invitation
xeresAPI.registerEventHandler("chatRoomInvite", function (data)
{
	console.log(`Location ${data.location} invited you to room id ${data.roomId}. Name: ${data.roomName}, topic: ${data.roomTopic}, public: ${data.roomIsPublic}, user count: ${data.roomUserCount}, signed: ${data.roomIsSigned}`);
});


// Initialization code
console.log(`User script loaded and ready.\nECMA Script version: ${Graal.versionECMAScript}\nGraal version: ${Graal.versionGraalVM}\nHotCode: ${Graal.isGraalRuntime()}`);

// Some helper functions
function getRandomDriver()
{
	const drivers = ['Verstappen', 'Hamilton', 'Leclerc', 'Alonso', 'Norris', 'Russell', 'Gasly', 'Albon', 'Hadjar', 'Hülkenberg', 'Ocon', 'Tsunoda', 'Piastri', 'Antonelli', 'Stroll', 'Colapinto', 'Sainz', 'Lawson', 'Bortoleto', 'Bearman'];
	return drivers[Math.floor(Math.random() * drivers.length)];
}

function getRandomAction()
{
	const actions = ['overtakes', 'crashes into', 'blocks', 'dive bombs', 'undercuts']
	return actions[Math.floor(Math.random() * actions.length)];
}

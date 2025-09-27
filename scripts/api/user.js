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
	else if (data.content === '!bullshit')
	{
		xeresAPI.sendChatRoomMessage(data.roomId, generateBullshit());
	}
	else if (/all your .+ are belong to .+$/i.test(data.content))
	{
		const ayb = [
			"What happen?",
			"Someone set up us the bomb",
			"We get signal",
			"Main screen turn on.",
			"How are you gentlemen!!",
			"You are on the way to destruction",
			"What you say?",
			"You have no chance to survive make your time",
			"Take off every 'ZIG'!!",
			"Move 'ZIG'.",
			"For great justice.",
			"It's you!!"
		];
		xeresAPI.sendChatRoomMessage(data.roomId, ayb[Math.floor(Math.random() * ayb.length)]);
	}

	console.log(`availability: ${xeresAPI.getAvailability()}`);
});

// Called when receiving a private chat message
xeresAPI.registerEventHandler("chatPrivateMessage", function (data)
{
	console.log(`Received private message from ${data.location} with content: ${data.content}`);

	switch (xeresAPI.getAvailability())
	{
		case "AWAY":
			xeresAPI.sendPrivateMessage(data.location, "Sorry but I'm away. I'll reply when I'm back.");
			break;

		case "BUSY":
			xeresAPI.sendPrivateMessage(data.location, "Sorry but I'm busy right now. I'll reply when I'm available again.");
			break;
	}
});

// Called when receiving a distant chat message
xeresAPI.registerEventHandler("chatDistantMessage", function (data)
{
	console.log(`Received distant message from ${data.gxsId} with content: ${data.content}`);

	switch (xeresAPI.getAvailability())
	{
		case "AWAY":
			xeresAPI.sendDistantMessage(data.gxsId, "Sorry but I'm away. I'll reply when I'm back.");
			break;

		case "BUSY":
			xeresAPI.sendDistantMessage(data.gxsId, "Sorry but I'm busy right now. I'll reply when I'm available again.");
			break;
	}
});

// Called when someone joins a room
xeresAPI.registerEventHandler("chatRoomJoin", function (data)
{
	console.log(`User ${data.nickname} (${data.gxsId}) joined chat room ${data.roomId}`);

	if (xeresAPI.getAvailability() === "AVAILABLE")
	{
		xeresAPI.sendChatRoomMessage(data.roomId, `welcome ${data.nickname}!`);
	}
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
	return getRandomString(drivers);
}

function getRandomAction()
{
	const actions = ['overtakes', 'crashes into', 'blocks', 'dive bombs', 'undercuts', 'swaps position with']
	return getRandomString(actions);
}

function getRandomString(array)
{
	return array[Math.floor(Math.random() * array.length)];
}

// Great function to help create powerpoint slides
function generateBullshit()
{
	const fle0 = [
		"aggregate",
		"architect",
		"benchmark",
		"brand",
		"cultivate",
		"deliver",
		"deploy",
		"disintermediate",
		"drive",
		"e-enable",
		"embrace",
		"empower",
		"enable",
		"engage",
		"engineer",
		"enhance",
		"envisioneer",
		"evolve",
		"expedite",
		"exploit",
		"extend",
		"facilitate",
		"generate",
		"grow",
		"harness",
		"implement",
		"incentivize",
		"incubate",
		"innovate",
		"integrate",
		"iterate",
		"leverage",
		"matrix",
		"maximize",
		"mesh",
		"monetize",
		"morph",
		"optimize",
		"orchestrate",
		"productize",
		"recontextualize",
		"reintermediate",
		"reinvent",
		"repurpose",
		"revolutionize",
		"scale",
		"seize",
		"strategize",
		"streamline",
		"syndicate",
		"synergize",
		"synthesize",
		"target",
		"transform",
		"transition",
		"unleash",
		"utilize",
		"visualize",
		"whiteboard"
	];

	const fle1 = [
		"24/365",
		"24/7",
		"B2B",
		"B2C",
		"back-end",
		"best-of-breed",
		"bleeding-edge",
		"bricks-and-clicks",
		"clicks-and-mortar",
		"collaborative",
		"compelling",
		"cross-platform",
		"cross-media",
		"customized",
		"cutting-edge",
		"distributed",
		"dot-com",
		"dynamic",
		"e-business",
		"efficient",
		"end-to-end",
		"enterprise",
		"extensible",
		"frictionless",
		"front-end",
		"global",
		"granular",
		"holistic",
		"impactful",
		"innovative",
		"integrated",
		"interactive",
		"intuitive",
		"killer",
		"leading-edge",
		"magnetic",
		"mission-critical",
		"next-generation",
		"one-to-one",
		"plug-and-play",
		"proactive",
		"real-time",
		"revolutionary",
		"robust",
		"scalable",
		"seamless",
		"sexy",
		"sticky",
		"strategic",
		"synergistic",
		"transparent",
		"turn-key",
		"ubiquitous",
		"user-centric",
		"value-added",
		"vertical",
		"viral",
		"virtual",
		"visionary",
		"web-enabled",
		"wireless",
		"world-class"
	];

	const fle2 = [
		"action-items",
		"AI",
		"applications",
		"architectures",
		"bandwidth",
		"channels",
		"cloud",
		"communities",
		"content",
		"convergence",
		"deliverables",
		"e-business",
		"e-commerce",
		"e-markets",
		"e-services",
		"e-tailers",
		"experiences",
		"eyeballs",
		"functionalities",
		"infomediaries",
		"infrastructures",
		"initiatives",
		"interfaces",
		"markets",
		"methodologies",
		"metrics",
		"mindshare",
		"models",
		"networks",
		"niches",
		"paradigms",
		"partnerships",
		"platforms",
		"portals",
		"relationships",
		"ROI",
		"synergies",
		"web-readiness",
		"schemas",
		"solutions",
		"supply-chains",
		"systems",
		"technologies",
		"users",
		"vortals",
		"web services"
	];

	return getRandomString(fle0) + " " + getRandomString(fle1) + " " + getRandomString(fle2);
}


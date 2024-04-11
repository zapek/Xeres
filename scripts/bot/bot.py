#!/usr/bin/env python3

#  Copyright (c) 2024 by David Gerber - https://zapek.com
#
#  This file is part of Xeres.
#
#  Xeres is free software: you can redistribute it and/or modify
#  it under the terms of the GNU General Public License as published by
#  the Free Software Foundation, either version 3 of the License, or
#  (at your option) any later version.
#
#  Xeres is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU General Public License for more details.
#
#  You should have received a copy of the GNU General Public License
#  along with Xeres.  If not, see <http://www.gnu.org/licenses/>.

#
#
# This file is part of Xeres.
#
# Xeres is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# Xeres is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with Xeres.  If not, see <http://www.gnu.org/licenses/>.

"""
# Xeres Bot

This is a simple python script demonstrating how to use a Xeres instance as a bot.

It is supposed to use a LLM running locally.

## Installation

You need the following:
- a running Xeres instance
- a running llamafile instance
- `pip install requests stomp.py cachetools`

## Running Xeres

Either run it standalone with the `--no-gui` option or with a docker compose like that:
```
version: '2.4'
services:
  xeres:
    image: zapek/xeres:0.6.3-rc1
    ports:
      - "1066:1066"
      - "3333:3333"
    environment:
      - SPRING_PROFILES_ACTIVE=cloud
      - XERES_SERVER_PORT=3333
      - XERES_DATA_DIR=/tmp
      - "JAVA_TOOL_OPTIONS=-Djava.net.preferIPv4Stack=true -Dfile.encoding=UTF-8"
    mem_limit: 1G
```

## Running llamafile

Get llamafile from here: https://github.com/mozilla-Ocho/llamafile

Run it with something like that (use the name of the llamafile you downloaded):

### Windows

`.\llamafile.exe --server --port 8080 --nobrowser`

### Linux

`llamafile --server --port 8080 --nobrowser`

### Docker

See https://github.com/iverly/llamafile-docker


## Writing the configuration file

You need a `config.json` file in the same directory which looks like the following:

```
{
    "xeres": {
        "api_url": "http://localhost:1066",
        "profile_name": "YourBotName",
        "location_name": "YourLocationName",
        "friend_ids": [
            "a Retroshare ID or Xeres ID of a friend's node"
        ],
        "room_names": [
            "the name of a chat room to join"
        ]
    },
    "openai": {
        "api_url": "http://localhost:8080/v1/chat/completions",
        "temperature": 0.7,
        "prompt": "You are an assistant and your name is {assistant}. You are helpful, kind, obedient, honest and know your own limits. You answer to {user}."
    },
    "context": {
        "max_users": 256,
        "max_time": 7200,
        "interactions": 6
    }
}
```

### Running the script

`python3 bot.py`

It will automatically configure the running Xeres instance and then take control of it. The bot will join the configured chat rooms and answer to users when being addressed directly.
It also answers to direct messages between nodes. If there's an `avatar.png` present in the same directory during configuration, it'll be used as the bot's avatar picture.

"""

import json
import os
import time
from urllib.parse import urlparse

import requests
import stomp
from cachetools import TTLCache

try:
	with open('config.json') as config_file:
		config = json.load(config_file)
except FileNotFoundError:
	print("Missing configuration file 'config.json' in the same directory. See the top of this python script file for more information.")
	exit(1)

XERES_API_URL = config['xeres']['api_url']
XERES_API_PREFIX = "/api/v1"
XERES_API_HOST = urlparse(XERES_API_URL).hostname
XERES_API_PORT = urlparse(XERES_API_URL).port
PROFILE_NAME = config['xeres']['profile_name']
LOCATION_NAME = config['xeres']['location_name']
FRIEND_IDS = config['xeres']['friend_ids']
ROOM_NAMES = config['xeres']['room_names']

OPENAI_URL = config['openai']['api_url']
TEMPERATURE = config['openai']['temperature']
PROMPT = config['openai']['prompt']

CHAT_CACHE = TTLCache(config['context']['max_users'], config['context']['max_time'])
INTERACTIONS = config['context']['interactions']


def has_profile():
	r = requests.get(XERES_API_URL + XERES_API_PREFIX + "/profiles/1")
	return r.status_code == 200


def create_profile():
	r = requests.post(XERES_API_URL + XERES_API_PREFIX + "/config/profile", json={'name': PROFILE_NAME})
	if r.status_code != 201:
		raise Exception(f"Couldn't create profile: {r.status_code}")


def create_location():
	r = requests.post(XERES_API_URL + XERES_API_PREFIX + "/config/location", json={'name': LOCATION_NAME})
	if r.status_code != 201:
		raise Exception(f"Couldn't create location: {r.status_code}")


def create_identity():
	r = requests.post(XERES_API_URL + XERES_API_PREFIX + "/config/identity", json={'name': PROFILE_NAME})
	if r.status_code != 201:
		raise Exception(f"Couldn't create identity: {r.status_code}")


def get_own_profile():
	r = requests.get(XERES_API_URL + XERES_API_PREFIX + "/profiles/1")
	if r.status_code != 200:
		raise Exception("Couldn't get own profile")
	return json.loads(r.text)


def get_own_identity():
	r = requests.get(XERES_API_URL + XERES_API_PREFIX + "/identities/1")
	if r.status_code != 200:
		raise Exception("Couldn't get own identity")
	return json.loads(r.text)


def get_own_location():
	r = requests.get(XERES_API_URL + XERES_API_PREFIX + "/locations/1")
	if r.status_code != 200:
		raise Exception("Couldn't get own location")
	return json.loads(r.text)


def get_own_rsid():
	r = requests.get(XERES_API_URL + XERES_API_PREFIX + "/locations/1/rsId")
	if r.status_code != 200:
		raise Exception(f"Couldn't get own RsId: {r.status_code}")
	return json.loads(r.text).get("rsId")


def add_friend(id):
	r = requests.post(XERES_API_URL + XERES_API_PREFIX + "/profiles?trust=FULL", json={'rsId': id})
	if r.status_code != 201:
		raise Exception(f"Couldn't add friend: {r.status_code}")


def synchronize_chatrooms(rooms):
	print(f"Syncing chatrooms...")
	remaining_rooms = rooms.copy()
	while len(remaining_rooms) > 0:
		for name in remaining_rooms:
			context = get_chat_rooms()
			id = find_chat_room(name, context['chatRooms']['subscribed'])
			if id != 0:
				remaining_rooms.remove(name)
				break

			id = find_chat_room(name, context['chatRooms']['available'])
			if id != 0:
				print(f"Subscribing to room {name} with id {id}", name, id)
				r = requests.put(XERES_API_URL + XERES_API_PREFIX + "/chat/rooms/" + str(id) + "/subscription")
				if r.status_code != 200:
					raise Exception(f"Couldn't subscribe to chatroom: {r.status_code}")
				remaining_rooms.remove(name)
				break
			time.sleep(10)

	context = get_chat_rooms()
	for room in context['chatRooms']['subscribed']:
		if room['name'] not in rooms:
			leave_room(room['name'])


def get_chat_rooms():
	r = requests.get(XERES_API_URL + XERES_API_PREFIX + "/chat/rooms")
	if r.status_code != 200:
		raise Exception(f"Couldn't get chatrooms: {r.status_code}")
	return json.loads(r.text)


def find_chat_room(name, room_array):
	for room in room_array:
		if room['name'] == name:
			return room['id']
	return 0


def leave_room(id):
	r = requests.delete(XERES_API_URL + XERES_API_PREFIX + "/chat/rooms/" + str(id) + "/subscription")
	if r.status_code != 204:
		raise Exception(f"Couldn't leave room: {r.status_code}")


def upload_avatar(path):
	with open(path, 'rb') as img:
		files = [('file', ("avatar.png", img, "image/png"))]
		r = requests.post(XERES_API_URL + XERES_API_PREFIX + "/identities/1/image", data={}, files=files)
		if r.status_code != 201:
			raise Exception(f"Couldn't upload avatar: {r.status_code}")


def connect_and_subscribe(conn):
	conn.connect(wait=True, with_connect_command=True)
	conn.subscribe(destination='/api/v1/chat', id=1, ack='auto')


class StompListener(stomp.ConnectionListener):
	def __init__(self, conn, own_profile, own_id):
		self.conn = conn
		self.own_profile = own_profile
		self.own_id = own_id

	def on_error(self, frame):
		print('received an error "%s"' % frame.body)

	def on_message(self, frame):
		# print(f'frame is {frame}')
		headers = frame.headers
		data = json.loads(frame.body)
		if headers['messageType'] == "CHAT_ROOM_MESSAGE":
			handle_incoming_room_message(self.conn,
			                             self.own_id,
			                             headers['destinationId'],
			                             data['roomId'],
			                             data['senderNickname'],
			                             data['gxsId']['bytes'],
			                             data['content'])
		elif headers['messageType'] == "CHAT_PRIVATE_MESSAGE":
			handle_incoming_private_message(self.conn,
			                                self.own_profile,
			                                self.own_id,
			                                headers['destinationId'],
			                                data['content'])

	def on_disconnected(self):
		print('disconnected')


def handle_chat(own_profile, own_id):
	conn = stomp.WSStompConnection([(XERES_API_HOST, XERES_API_PORT)], ws_path='/ws')
	conn.set_listener('', StompListener(conn, own_profile, own_id))
	connect_and_subscribe(conn)
	while True:
		time.sleep(60)


def handle_incoming_room_message(conn, own_id, destination_id, room_id, sender, gxs_sender, content):
	own_nickname = own_id['name'].lower()

	# We need to do that because what we say in the room is echoed back, obviously
	if gxs_sender == own_id['gxsId']['bytes']:
		return

	lower_content = content.lower()

	if not (lower_content.startswith("@" + own_nickname + " ") or lower_content.startswith(own_nickname + ": ") or lower_content.startswith("@" + own_nickname + ": ")):
		return

	content = content[(len(own_nickname) + (3 if lower_content.startswith("@" + own_nickname + ": ") else 2)):]

	# print(f"Handling message from {sender} in {room_id}: {content}")

	content = openai_api_send(content, own_id['name'], sender, gxs_sender)

	conn.send(destination="/app/api/v1/chat",
	          content_type="application/json",
	          headers={"messageType": "CHAT_ROOM_MESSAGE",
	                   "destinationId": f"{destination_id}"},
	          body=json.dumps({"roomId": room_id,
	                           "senderNickname": PROFILE_NAME,
	                           "gxsId": {"bytes": f"{own_id['gxsId']['bytes']}"},
	                           "content": f"{sender}: {content}",
	                           "empty": False})
	          )


def handle_incoming_private_message(conn, own_profile, own_id, destination_id, content):
	content = openai_api_send(content, own_id['name'], own_profile['name'], destination_id)

	conn.send(destination="/app/api/v1/chat",
	          content_type="application/json",
	          headers={"messageType": "CHAT_PRIVATE_MESSAGE",
	                   "destinationId": f"{destination_id}"},
	          body=json.dumps({"content": f"{content}",
	                           "empty": False})
	          )


def strip_nickname_prefix(message, nickname):
	if message.startswith(nickname + ": "):
		return message[(len(nickname) + 2):]
	return message


def evict_cache(messages):
	if (len(messages)) > INTERACTIONS * 2:
		messages.pop(0)
		messages.pop(0)


def get_cache_messages(user_id):
	messages = CHAT_CACHE.get(user_id, list())
	CHAT_CACHE[user_id] = messages
	return messages


def create_query_for_openai_api(prompt, messages):
	model = {
		"messages": [
			{
				"role": "system",
				"content": f"{prompt}"
			}
		],
		"temperature": TEMPERATURE
	}

	idx = 0

	for message in messages:
		model['messages'].append({
			"role": "user" if idx % 2 == 0 else "assistant",
			"content": f"{message}"
		})
		idx += 1

	return model


# user_id can be gxs or location_id, doesn't matter, it's for the cache
def openai_api_send(message, assistant, user, user_id):
	print(f"<{user}> {assistant}: {message}")

	messages = get_cache_messages(user_id)
	messages.append(message)

	prompt = PROMPT.format(assistant=assistant, user=user)

	query = create_query_for_openai_api(prompt, messages)
	# print(f"Query: {query}")

	r = requests.post(OPENAI_URL, json=query)
	if r.status_code != 200:
		raise Exception(f"Couldn't send message to GPT server")

	response = strip_nickname_prefix(json.loads(r.text)['choices'][0]['message']['content'], assistant)  # idiot AI sometimes inserts itself in the reply

	print(f"<{assistant}> {user}: {response}")

	messages.append(response)
	evict_cache(messages)

	return response


if not has_profile():
	create_profile()
	create_location()
	create_identity()
	if os.path.isfile("avatar.png"):
		upload_avatar("avatar.png")
	for friend in FRIEND_IDS:
		add_friend(friend)

print("Xeres Bot v1.0\n")

own_profile = get_own_profile()
own_id = get_own_identity()
print(f"I am {own_id.get('name')}")
print(f"This is my RS ID (paste it in friends I have to connect to):\n{get_own_rsid()}")

synchronize_chatrooms(ROOM_NAMES)
print(f"Ready and awaiting to be addressed to.")

handle_chat(own_profile, own_id)

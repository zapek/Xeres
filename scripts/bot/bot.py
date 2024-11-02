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


import json
import os
import requests
import stomp
import time
from cachetools import TTLCache
from urllib.parse import urlparse

try:
	with open('config.json') as config_file:
		config = json.load(config_file)
except FileNotFoundError:
	print("Missing configuration file 'config.json' in the same directory. See the README.md file for more information.")
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
MODEL = config['openai']['model'] if 'model' in config['openai'] else None
PROMPT = config['openai']['prompt']
AVATAR = "avatar.png"

CHAT_CACHE = TTLCache(config['context']['max_users'], config['context']['max_time'])
INTERACTIONS = config['context']['interactions']


def has_profile():
	r = requests.get(XERES_API_URL + XERES_API_PREFIX + "/profiles/1")
	return r.status_code == 200


def create_profile():
	r = requests.post(XERES_API_URL + XERES_API_PREFIX + "/config/profile", json={'name': PROFILE_NAME})
	if r.status_code != 201:
		raise RuntimeError(f"Couldn't create profile: {r.status_code}")


def create_location():
	r = requests.post(XERES_API_URL + XERES_API_PREFIX + "/config/location", json={'name': LOCATION_NAME})
	if r.status_code != 201:
		raise RuntimeError(f"Couldn't create location: {r.status_code}")


def create_identity():
	r = requests.post(XERES_API_URL + XERES_API_PREFIX + "/config/identity", json={'name': PROFILE_NAME})
	if r.status_code != 201:
		raise RuntimeError(f"Couldn't create identity: {r.status_code}")


def get_own_profile():
	r = requests.get(XERES_API_URL + XERES_API_PREFIX + "/profiles/1")
	if r.status_code != 200:
		raise RuntimeError("Couldn't get own profile")
	return json.loads(r.text)


def get_own_identity():
	r = requests.get(XERES_API_URL + XERES_API_PREFIX + "/identities/1")
	if r.status_code != 200:
		raise RuntimeError("Couldn't get own identity")
	return json.loads(r.text)


def get_own_location():
	r = requests.get(XERES_API_URL + XERES_API_PREFIX + "/locations/1")
	if r.status_code != 200:
		raise RuntimeError("Couldn't get own location")
	return json.loads(r.text)


def get_own_rsid():
	r = requests.get(XERES_API_URL + XERES_API_PREFIX + "/locations/1/rs-id")
	if r.status_code != 200:
		raise RuntimeError(f"Couldn't get own RsId: {r.status_code}")
	return json.loads(r.text).get("rsId")


def add_friend(id):
	r = requests.post(XERES_API_URL + XERES_API_PREFIX + "/profiles?trust=FULL", json={'rsId': id})
	if r.status_code != 201:
		raise RuntimeError(f"Couldn't add friend: {r.status_code}")


def synchronize_chatrooms(rooms):
	print("Syncing chatrooms...")
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
					raise RuntimeError(f"Couldn't subscribe to chatroom: {r.status_code}")
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
		raise RuntimeError(f"Couldn't get chatrooms: {r.status_code}")
	return json.loads(r.text)


def find_chat_room(name, room_array):
	for room in room_array:
		if room['name'] == name:
			return room['id']
	return 0


def leave_room(id):
	r = requests.delete(XERES_API_URL + XERES_API_PREFIX + "/chat/rooms/" + str(id) + "/subscription")
	if r.status_code != 204:
		raise RuntimeError(f"Couldn't leave room: {r.status_code}")


def upload_avatar(path):
	with open(path, 'rb') as img:
		files = [('file', (AVATAR, img, "image/png"))]
		r = requests.post(XERES_API_URL + XERES_API_PREFIX + "/identities/1/image", data={}, files=files)
		if r.status_code != 201:
			raise RuntimeError(f"Couldn't upload avatar: {r.status_code}")


def connect_and_subscribe(conn):
	conn.connect(wait=True, with_connect_command=True)
	conn.subscribe(destination='/api/v1/chat', id=1, ack='auto')


class StompListener(stomp.ConnectionListener):
	def __init__(self, conn, own_id):
		self.conn = conn
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
			                                self.own_id,
			                                headers['destinationId'],
			                                data['content'])

	def on_disconnected(self):
		print('disconnected')


def handle_chat(own_id):
	conn = stomp.WSStompConnection([(XERES_API_HOST, XERES_API_PORT)], ws_path='/ws')
	conn.set_listener('', StompListener(conn, own_id))
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

	content = openai_api_send(content, own_id['name'], sender, gxs_sender, lambda: send_chat_room_typing_notification(conn, own_id, destination_id, room_id))

	conn.send(destination="/app/api/v1/chat",
	          content_type="application/json",
	          headers={"messageType": "CHAT_ROOM_MESSAGE",
	                   "destinationId": f"{destination_id}"},
	          body=json.dumps({"roomId": room_id,
	                           "senderNickname": PROFILE_NAME,
	                           "gxsId": {"bytes": f"{own_id['gxsId']['bytes']}"},
	                           "content": f"{sender}: {content}"})
	          )


def handle_incoming_private_message(conn, own_id, destination_id, content):
	# user is not really the destination_id, we should fetch it
	content = openai_api_send(content, own_id['name'], destination_id, destination_id, lambda: send_private_typing_notification(conn, destination_id))

	conn.send(destination="/app/api/v1/chat",
	          content_type="application/json",
	          headers={"messageType": "CHAT_PRIVATE_MESSAGE",
	                   "destinationId": f"{destination_id}"},
	          body=json.dumps({"content": f"{content}"})
	          )


def send_chat_room_typing_notification(conn, own_id, destination_id, room_id):
	conn.send(destination="/app/api/v1/chat",
	          content_type="application/json",
	          headers={"messageType": "CHAT_ROOM_TYPING_NOTIFICATION",
	                   "destinationId": f"{destination_id}"},
	          body=json.dumps({"roomId": room_id,
	                           "senderNickname": PROFILE_NAME,
	                           "gxsId": {"bytes": f"{own_id['gxsId']['bytes']}"},
	                           "content": ""})
	          )


def send_private_typing_notification(conn, destination_id):
	conn.send(destination="/app/api/v1/chat",
	          content_type="application/json",
	          headers={"messageType": "CHAT_TYPING_NOTIFICATION",
	                   "destinationId": f"{destination_id}"},
	          body=json.dumps({"content": ""})
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
		"temperature": TEMPERATURE,
		"stream": True
	}

	if MODEL:
		model['model'] = MODEL

	idx = 0

	for message in messages:
		model['messages'].append({
			"role": "user" if idx % 2 == 0 else "assistant",
			"content": f"{message}"
		})
		idx += 1

	return model


# user_id can be gxs or location_id, doesn't matter, it's for the cache
def openai_api_send(message, assistant, user, user_id, _callback=None):
	print(f"<{user}> {assistant}: {message}")

	start = time.time()
	_callback()

	messages = get_cache_messages(user_id)
	messages.append(message)

	prompt = PROMPT.format(assistant=assistant, user=user)

	query = create_query_for_openai_api(prompt, messages)
	# print(f"Query: {query}")

	r = requests.post(OPENAI_URL, json=query, stream=True)
	if r.status_code != 200:
		raise RuntimeError(f"Couldn't send message to openai API server ({r.status_code}): {r.text}")

	output = ""

	for line in r.iter_lines():
		if line:
			line = line.decode("utf-8")
			if line == "data: [DONE]":
				break
			line = remove_prefix(line, "data: ")
			o = json.loads(line)
			if 'content' in o['choices'][0]['delta']:
				output += o['choices'][0]['delta']['content']
			if _callback and time.time() - start > 5.0:
				_callback()
				start = time.time()

	response = strip_nickname_prefix(output, assistant)  # idiot AI sometimes inserts itself in the reply

	print(f"<{assistant}> {user}: {response}")

	messages.append(response)
	evict_cache(messages)

	return response


def remove_prefix(text, prefix):
	if text.startswith(prefix):
		return text[len(prefix):]
	return text


if not has_profile():
	create_profile()
	create_location()
	create_identity()
	if os.path.isfile(AVATAR):
		upload_avatar(AVATAR)

for friend in FRIEND_IDS:
	add_friend(friend)

print("Xeres Bot v1.0\n")

own_id = get_own_identity()
print(f"I am {own_id.get('name')}")
print(f"This is my RS ID (paste it in friends I have to connect to):\n{get_own_rsid()}")

synchronize_chatrooms(ROOM_NAMES)
print("Ready and awaiting to be addressed to.")

handle_chat(own_id)

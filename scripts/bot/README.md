# Xeres Bot

This is a simple python script demonstrating how to use a Xeres instance as a bot.

It is supposed to use a LLM running locally.

## Installation

You need the following:

- a running Xeres instance
- a running llamafile instance (also works with ollama)
- `pip install requests stomp.py cachetools`

## Running Xeres

Either run it standalone with the `--no-gui` option or with a docker compose like that:

```
services:
  xeres:
    image: zapek/xeres:0.6.3-rc1
    user: 0:0
    environment:
      - SPRING_PROFILES_ACTIVE=cloud
      - XERES_SERVER_PORT=3333
      - XERES_DATA_DIR=/tmp/xeres
      - "JAVA_TOOL_OPTIONS=-Djava.net.preferIPv4Stack=true -Dfile.encoding=UTF-8"
    volumes:
      xeres-bot-data:
    mem_limit: 1G
    restart: always
    network_mode: host
volumes:
  xeres-bot-data:
```

## Running with llamafile

Get llamafile from here: https://github.com/mozilla-Ocho/llamafile

Run it with something like that (use the name of the llamafile you downloaded):

### Windows

`.\llamafile.exe --server --port 11434 --nobrowser`

### Linux

`llamafile --server --port 11434 --nobrowser`

### Docker

See https://github.com/iverly/llamafile-docker

## Running with ollama

Get ollama from here: https://ollama.com/

Then use `ollama run llama2`

## Writing the configuration file

You need a `config.json` file in the same directory which looks like the following:

```
{
    "xeres": {
        "api_url": "http://localhost:6232",
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
        "api_url": "http://localhost:11434/v1/chat/completions",
        "temperature": 0.7,
        "model": "llama2",
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

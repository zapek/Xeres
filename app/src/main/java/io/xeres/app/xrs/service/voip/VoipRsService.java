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

package io.xeres.app.xrs.service.voip;

import io.xeres.app.database.model.location.Location;
import io.xeres.app.net.peer.PeerConnection;
import io.xeres.app.net.peer.PeerConnectionManager;
import io.xeres.app.service.LocationService;
import io.xeres.app.service.MessageService;
import io.xeres.app.service.audio.AudioService;
import io.xeres.app.xrs.item.Item;
import io.xeres.app.xrs.service.RsService;
import io.xeres.app.xrs.service.RsServiceRegistry;
import io.xeres.app.xrs.service.RsServiceType;
import io.xeres.app.xrs.service.voip.item.VoipDataItem;
import io.xeres.app.xrs.service.voip.item.VoipProtocolItem;
import io.xeres.common.id.LocationIdentifier;
import io.xeres.common.message.MessageType;
import io.xeres.common.message.voip.VoipAction;
import io.xeres.common.message.voip.VoipMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xiph.speex.SpeexDecoder;
import org.xiph.speex.SpeexEncoder;

import java.io.StreamCorruptedException;

import static io.xeres.app.xrs.service.RsServiceType.PLUGIN_VOIP;
import static io.xeres.app.xrs.service.voip.item.VoipProtocolItem.Protocol.*;
import static io.xeres.common.message.MessagePath.voipPrivateDestination;

@Component
public class VoipRsService extends RsService
{
	private static final Logger log = LoggerFactory.getLogger(VoipRsService.class);

	public static final int FLAGS_VIDEO_DATA = 1;
	public static final int FLAGS_AUDIO_DATA = 2;

	private enum Status
	{
		OFF,
		CALLING,
		CALLED,
		ONGOING
	}

	private final PeerConnectionManager peerConnectionManager;
	private final AudioService audioService;
	private final MessageService messageService;
	private final LocationService locationService;

	private SpeexEncoder speexEncoder;
	private SpeexDecoder speexDecoder;

	private final LockBasedSingleEntrySupplier audioSupplier = new LockBasedSingleEntrySupplier();

	private LocationIdentifier remoteLocationIdentifier;
	private Status status = Status.OFF;

	VoipRsService(RsServiceRegistry rsServiceRegistry, PeerConnectionManager peerConnectionManager, AudioService audioService, MessageService messageService, LocationService locationService)
	{
		super(rsServiceRegistry);
		this.peerConnectionManager = peerConnectionManager;
		this.audioService = audioService;
		this.messageService = messageService;
		this.locationService = locationService;
	}

	@Override
	public RsServiceType getServiceType()
	{
		return PLUGIN_VOIP;
	}

	@Override
	public void handleItem(PeerConnection sender, Item item)
	{
		switch (item)
		{
			case VoipProtocolItem voipProtocolItem -> handleProtocolItem(sender, voipProtocolItem);
			case VoipDataItem voipDataItem -> handleDataItem(sender, voipDataItem);
			default -> log.debug("Unhandled item {}", item);
		}
	}

	public void call(LocationIdentifier locationIdentifier)
	{
		var location = locationService.findLocationByLocationIdentifier(locationIdentifier).orElseThrow();
		log.debug("Calling {}...", location);

		status = Status.CALLING;
		remoteLocationIdentifier = locationIdentifier;

		var item = new VoipProtocolItem(RING);
		peerConnectionManager.writeItem(location, item, this);
	}

	public void accept(LocationIdentifier locationIdentifier)
	{
		var location = locationService.findLocationByLocationIdentifier(locationIdentifier).orElseThrow();
		log.debug("Accepting call from {}", location);

		remoteLocationIdentifier = locationIdentifier;
		status = Status.ONGOING;

		var item = new VoipProtocolItem(ACKNOWLEDGE);
		peerConnectionManager.writeItem(location, item, this);

		openChannel(location);
	}

	public void hangup(LocationIdentifier locationIdentifier)
	{
		var location = locationService.findLocationByLocationIdentifier(locationIdentifier).orElseThrow();
		log.debug("Hanging up on {}", location);

		status = Status.OFF;
		remoteLocationIdentifier = null;

		var item = new VoipProtocolItem(CLOSE);
		peerConnectionManager.writeItem(location, item, this);

		closeChannel();
	}

	private void handleProtocolItem(PeerConnection sender, VoipProtocolItem item)
	{
		log.debug("Got protocol item {}, status: {}", item, status);
		switch (item.getProtocol())
		{
			case RING ->
			{
				if (remoteLocationIdentifier == null && status == Status.OFF)
				{
					log.debug("Got incoming call from {}", sender);
					remoteLocationIdentifier = sender.getLocation().getLocationIdentifier();
					status = Status.CALLED;
					messageService.sendToConsumers(voipPrivateDestination(), MessageType.NONE, sender.getLocation().getLocationIdentifier(), new VoipMessage(VoipAction.RING));
				}
				else
				{
					log.debug("Got incoming call from {}, but we're already in call, dropping...", sender);
					messageService.sendToConsumers(voipPrivateDestination(), MessageType.NONE, sender.getLocation().getLocationIdentifier(), new VoipMessage(VoipAction.CLOSE)); // XXX: not sure if this is understood by RS, check
				}
			}
			case ACKNOWLEDGE ->
			{
				if (sender.getLocation().getLocationIdentifier().equals(remoteLocationIdentifier) && status == Status.CALLING)
				{
					log.debug("Call acknowledged by {}", sender);
					status = Status.ONGOING;
					messageService.sendToConsumers(voipPrivateDestination(), MessageType.NONE, sender.getLocation().getLocationIdentifier(), new VoipMessage(VoipAction.ACKNOWLEDGE));
					openChannel(sender.getLocation());
				}
			}
			case CLOSE ->
			{
				if (sender.getLocation().getLocationIdentifier().equals(remoteLocationIdentifier) && (status == Status.ONGOING || status == Status.CALLED || status == Status.CALLING))
				{
					log.debug("Call closed by {}", sender);
					remoteLocationIdentifier = null;
					status = Status.OFF;
					messageService.sendToConsumers(voipPrivateDestination(), MessageType.NONE, sender.getLocation().getLocationIdentifier(), new VoipMessage(VoipAction.CLOSE));
					closeChannel();
				}
			}
			default -> log.debug("Unhandled protocol {}", item);
		}
	}

	private void handleDataItem(PeerConnection sender, VoipDataItem item)
	{
		if (status == Status.ONGOING && sender.getLocation().getLocationIdentifier().equals(remoteLocationIdentifier))
		{
			audioSupplier.put(decodeData(item.getData()));
		}
		else
		{
			log.debug("Ignoring data item {} because current peer is {}", item, remoteLocationIdentifier);
		}
	}

	private void openChannel(Location target)
	{
		speexEncoder = new SpeexEncoder();
		speexEncoder.init(audioService.getSpeexEncoderMode(), 9, audioService.getAudioSampleRate(), audioService.getAudioSampleChannels());

		speexEncoder.getEncoder().setVbr(true);
		speexEncoder.getEncoder().setVbrQuality(9.0f);
		speexEncoder.getEncoder().setComplexity(4);
		speexEncoder.getEncoder().setDtx(true);

		speexDecoder = new SpeexDecoder();
		speexDecoder.init(audioService.getSpeexEncoderMode(), audioService.getAudioSampleRate(), audioService.getAudioSampleChannels(), true);

		audioService.startPlayingAndRecording(speexEncoder.getFrameSize(),
				data -> peerConnectionManager.writeItem(target, new VoipDataItem(encodeData(data)), this),
				audioSupplier);
	}

	private void closeChannel()
	{
		audioService.stopRecordingAndPlaying();

		speexEncoder = null;
		speexDecoder = null;
	}

	private byte[] encodeData(byte[] input)
	{
		if (speexEncoder.processData(input, 0, input.length))
		{
			var encodedData = new byte[speexEncoder.getProcessedDataByteSize()];
			speexEncoder.getProcessedData(encodedData, 0);
			return encodedData;
		}
		log.error("Speex encoding failed");
		return new byte[0];
	}

	private byte[] decodeData(byte[] input)
	{
		try
		{
			speexDecoder.processData(input, 0, input.length);

			var decodedData = new byte[speexDecoder.getProcessedDataByteSize()];
			speexDecoder.getProcessedData(decodedData, 0);
			return decodedData;
		}
		catch (StreamCorruptedException e)
		{
			log.error("Speex decoding failed: {}", e.getMessage());
			return new byte[0];
		}
	}
}

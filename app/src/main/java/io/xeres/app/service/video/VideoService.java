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

package io.xeres.app.service.video;

import io.xeres.app.service.UiBridgeService;
import io.xeres.common.util.ThreadUtils;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;
import java.util.function.Supplier;

@Service
public class VideoService
{
	private volatile boolean isRecording;
	private volatile boolean isPlaying;
	private Thread recordThread;
	private Thread playThread;
	private Consumer<byte[]> videoConsumer;
	private Supplier<byte[]> videoSupplier;

	public void startPlayingAndRecording(int width, int height, int frameRate, Consumer<byte[]> videoConsumer, Supplier<byte[]> videoSupplier) // XXX: parameter needed?
	{
		startPlaying(videoSupplier);
		startRecording(width, height, frameRate, videoConsumer);
	}

	public void stopRecordingAndPlaying()
	{
		stopRecording();
		stopPlaying();
	}

	private void startPlaying(Supplier<byte[]> videoSupplier)
	{
		this.videoSupplier = videoSupplier;
		isPlaying = true;

		playThread = Thread.ofVirtual()
				.name("Video Playing Service")
				.start(this::playVideo);
	}

	private void startRecording(int width, int height, int frameRate, Consumer<byte[]> videoConsumer)
	{
		recordThread = Thread.ofVirtual()
				.name("Video Capture Service")
				.start(this::captureVideo);
	}

	private void stopPlaying()
	{
		isPlaying = false;
		ThreadUtils.waitForThread(playThread);
	}

	private void stopRecording()
	{
		isRecording = false;
		ThreadUtils.waitForThread(recordThread);
	}

	private void captureVideo()
	{
		while (isRecording)
		{
			var frame = UiBridgeService.getVideoFrame();
			videoConsumer.accept(frame);
		}
	}

	private void playVideo()
	{
		while (isPlaying)
		{
			var buffer = videoSupplier.get();
			UiBridgeService.sendVideoData(buffer);
		}
	}
}

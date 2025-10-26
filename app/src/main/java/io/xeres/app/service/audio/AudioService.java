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

package io.xeres.app.service.audio;

import org.springframework.stereotype.Service;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Service
public class AudioService
{
	private static final int AUDIO_SAMPLE_RATE = 16000; // in Hz, wideband
	private static final int AUDIO_SAMPLE_SIZE = 16; // in bits
	private static final int AUDIO_SAMPLE_CHANNELS = 1; // mono

	private TargetDataLine inputLine;
	private SourceDataLine outputLine;
	private boolean isRecording;
	private boolean isPlaying;
	private ByteArrayOutputStream audioBuffer;
	private Consumer<byte[]> audioConsumer;
	private Supplier<byte[]> audioSupplier;

	private AudioFormat audioFormat;
	private int frameSize;

	public int getAudioSampleRate()
	{
		return AUDIO_SAMPLE_RATE;
	}

	public int getAudioSampleSize()
	{
		return AUDIO_SAMPLE_SIZE;
	}

	public int getAudioSampleChannels()
	{
		return AUDIO_SAMPLE_CHANNELS;
	}

	@SuppressWarnings("DataFlowIssue")
	public int getSpeexEncoderMode()
	{
		return switch (AUDIO_SAMPLE_RATE)
		{
			case 8000 -> 0;
			case 16000 -> 1;
			case 32000 -> 2;
			default -> throw new IllegalStateException("Wrong sample rate " + AUDIO_SAMPLE_RATE + ", must be 8000, 16000 or 32000");
		};
	}

	public void startPlayingAndRecording(int frameSize, Consumer<byte[]> audioConsumer, Supplier<byte[]> audioSupplier)
	{
		startPlaying(audioSupplier);
		startRecording(frameSize, audioConsumer);
	}

	public void stopRecordingAndPlaying()
	{
		stopRecording();
		stopPlaying();
	}

	private void startRecording(int frameSize, Consumer<byte[]> audioConsumer)
	{
		createAudioFormatIfNeeded();

		try
		{
			inputLine = AudioSystem.getTargetDataLine(audioFormat);
			inputLine.open();

			audioBuffer = new ByteArrayOutputStream();
			this.frameSize = frameSize;
			this.audioConsumer = audioConsumer;
			isRecording = true;

			inputLine.start();

			Thread.ofVirtual()
					.name("Audio Capture Service")
					.start(this::captureAudio);
		}
		catch (LineUnavailableException | IllegalArgumentException e)
		{
			throw new IllegalStateException("Audio capture device not available: " + e.getMessage());
		}
	}

	private void stopRecording()
	{
		isRecording = false;

		if (inputLine != null)
		{
			inputLine.stop();
			inputLine.close();
		}
	}

	private void startPlaying(Supplier<byte[]> audioSupplier)
	{
		createAudioFormatIfNeeded();

		try
		{
			outputLine = AudioSystem.getSourceDataLine(audioFormat);
			outputLine.open();

			this.audioSupplier = audioSupplier;
			isPlaying = true;

			outputLine.start();

			Thread.ofVirtual()
					.name("Audio Playing Service")
					.start(this::playAudio);
		}
		catch (LineUnavailableException | IllegalArgumentException e)
		{
			throw new IllegalStateException("Audio playing device not available: " + e.getMessage());
		}
	}

	private void stopPlaying()
	{
		isPlaying = false;

		if (outputLine != null)
		{
			outputLine.stop();
			outputLine.close();
		}
	}

	private void createAudioFormatIfNeeded()
	{
		if (audioFormat == null)
		{
			audioFormat = new AudioFormat(AUDIO_SAMPLE_RATE, AUDIO_SAMPLE_SIZE, AUDIO_SAMPLE_CHANNELS, true, false);
		}
	}

	private void captureAudio()
	{
		var buffer = new byte[frameSize * AUDIO_SAMPLE_CHANNELS * (AUDIO_SAMPLE_SIZE / 8)];
		int bytesRead;

		while (isRecording)
		{
			bytesRead = inputLine.read(buffer, 0, buffer.length);
			if (bytesRead == buffer.length) // Only use full buffers, otherwise that's not enough to process a frame and the encoder will complain
			{
				audioBuffer.reset();
				audioBuffer.write(buffer, 0, bytesRead);

				audioConsumer.accept(audioBuffer.toByteArray());
			}
		}
	}

	private void playAudio()
	{
		while (isPlaying)
		{
			var buffer = audioSupplier.get();
			outputLine.write(buffer, 0, buffer.length);
		}
	}
}

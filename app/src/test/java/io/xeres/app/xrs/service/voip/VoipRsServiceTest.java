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

import io.xeres.common.util.ByteUnitUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.xiph.speex.SpeexDecoder;
import org.xiph.speex.SpeexEncoder;

import javax.sound.sampled.*;
import java.io.StreamCorruptedException;

class VoipRsServiceTest
{
	// XXX: remove once working
	@Test
	@Disabled
	void testSound() throws StreamCorruptedException, LineUnavailableException
	{
		int sampleRate = 16000;
		int sampleSize = 16;
		int channels = 1;
		AudioFormat format = new AudioFormat(sampleRate, sampleSize, channels, true, false);

		TargetDataLine lineIn = AudioSystem.getTargetDataLine(format);
		lineIn.open(format);

		SourceDataLine lineOut = AudioSystem.getSourceDataLine(format);
		lineOut.open(format);

		// for the parameters, see https://www.speex.org/docs/manual/speex-manual/
		SpeexEncoder encoder = new SpeexEncoder();
		SpeexDecoder decoder = new SpeexDecoder();
		encoder.init(1, 9, sampleRate, channels);
		encoder.getEncoder().setVbr(true);
		encoder.getEncoder().setVbrQuality(9.0f); // This overrides the "int" quality when VBR is true
		encoder.getEncoder().setComplexity(4);
		encoder.getEncoder().setDtx(true); // doesn't seem to work...

		decoder.init(1, sampleRate, channels, true); // Enhanced is better
		int rawBlockSize = encoder.getFrameSize() * channels * (sampleSize / 8);
		byte[] buffer = new byte[rawBlockSize];
		lineIn.start();
		lineOut.start();

		long lastPrintTime = System.currentTimeMillis();
		long currentTime;
		long totalIn = 0;
		long totalOut = 0;
		int secondIn = 0;
		int secondOut = 0;
		int seconds = 0;

		while (true)
		{
			int read = lineIn.read(buffer, 0, rawBlockSize);
			if (!encoder.processData(buffer, 0, rawBlockSize))
			{
				System.err.println("Could not encode data!");
				break;
			}
			int encoded = encoder.getProcessedData(buffer, 0);
			//System.out.printf("%4d <- %3d%n", encoded, read);
			byte[] encodedData = new byte[encoded];
			System.arraycopy(buffer, 0, encodedData, 0, encoded);
			decoder.processData(encodedData, 0, encoded);
			byte[] decodedData = new byte[decoder.getProcessedDataByteSize()];
			int decoded = decoder.getProcessedData(decodedData, 0);
			lineOut.write(decodedData, 0, decoded);

			totalIn += read;
			secondIn += read;
			totalOut += encoded;
			secondOut += encoded;

			currentTime = System.currentTimeMillis();
			if (currentTime - lastPrintTime >= 1000)
			{
				System.out.println(String.format("[%04d]", seconds) + " Speed: " + ByteUnitUtils.fromBytes(secondOut) + ", total: " + ByteUnitUtils.fromBytes(totalOut) + " / " + ByteUnitUtils.fromBytes(totalIn));
				lastPrintTime = currentTime;
				secondIn = 0;
				secondOut = 0;
				seconds++;
			}
		}
	}
}
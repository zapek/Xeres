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

package io.xeres.app.media.ffmpeg;

import org.bytedeco.ffmpeg.avcodec.AVCodec;
import org.bytedeco.ffmpeg.avcodec.AVCodecContext;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.ffmpeg.avutil.AVRational;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.PointerPointer;

import java.io.ByteArrayOutputStream;

import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avutil.*;

public class FFMPEGEncoder
{
	private final VideoCodec videoCodec;
	private final int width;
	private final int height;
	private final int frameRate;
	private final int bitRate;
	private AVCodec codec;
	private AVCodecContext context;
	private AVFrame frame;
	private final int format;

	public FFMPEGEncoder(VideoCodec videoCodec, int width, int height, int frameRate, int bitRate)
	{
		// XXX: do I need to open the libs? I think so...
		this.videoCodec = videoCodec;
		this.width = width;
		this.height = height;
		this.frameRate = frameRate;
		this.bitRate = bitRate;
		this.format = AV_PIX_FMT_YUV420P; // XXX: doesn't that depend on the input? and codec too...
	}

	private void initializeIfNeeded() // XXX: call!
	{
		if (codec != null) // XXX: better initialization maybe?
		{
			return;
		}
		codec = avcodec_find_encoder(videoCodec.getCodecId()); // XXX: or use find_encoder_by_name() ?
		if (codec != null)
		{
			context = avcodec_alloc_context3(codec);
			if (context != null)
			{
				context.width(width);
				context.height(height);
				context.time_base(getSupportedFrameRate(av_d2q(frameRate, 1001000)));
				context.pix_fmt(format);
				context.bit_rate(bitRate);
				// XXX: profile, quality, etc...
				context.profile(AV_PROFILE_H264_CONSTRAINED_BASELINE); // XXX: only for H264 obviously
				if (avcodec_open2(context, codec, (PointerPointer<?>) null) == 0)
				{
					frame = av_frame_alloc(); // XXX: check for failure. is it needed to call av_frame_get_buffer() afterwards?!
					frame.pts(0); // XXX: magic required by libx264... really?
					frame.width(width);
					frame.height(height);
					frame.format(format);
					// All OK
					return;
				}
			}
		}
		cleanup();
	}

	private AVRational getSupportedFrameRate(AVRational frame_rate)
	{
		AVRational supported_framerates = codec.supported_framerates();
		if (supported_framerates != null)
		{
			int idx = av_find_nearest_q_idx(frame_rate, supported_framerates);
			return supported_framerates.position(idx);
		}
		return frame_rate;
	}

	public byte[] encodeFrame(byte[] buffer)
	{
		initializeIfNeeded();

		if (buffer.length > 0)
		{
			av_image_fill_arrays(frame.data(), frame.linesize(), new BytePointer(buffer), format, context.width(), context.height(), 1);
			avcodec_send_frame(context, frame);
		}
		else
		{
			// Flush
			avcodec_send_frame(context, null);
		}

		var baos = new ByteArrayOutputStream();
		AVPacket packet = av_packet_alloc();

		while (avcodec_receive_packet(context, packet) == 0)
		{
			var packetData = new byte[packet.size()];
			packet.data().get(packetData);
			baos.write(packetData, 0, packet.size());
			av_packet_unref(packet);
		}

		av_packet_free(packet);

		return baos.toByteArray();
	}

	public void cleanup()
	{
		if (frame != null)
		{
			av_frame_free(frame);
			frame.close();
		}

		if (context != null)
		{
			avcodec_free_context(context);
			context.close();
		}

		if (codec != null)
		{
			codec.close();
		}
		PointerPointer.trimMemory();
	}
}

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
import org.bytedeco.ffmpeg.avcodec.AVCodecParserContext;
import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.avutil.AVFrame;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.PointerPointer;

import java.io.ByteArrayOutputStream;

import static org.bytedeco.ffmpeg.global.avcodec.*;
import static org.bytedeco.ffmpeg.global.avutil.*;

// XXX: this is all broken I think...
public class FFMPEGDecoder
{
	private final VideoCodec videoCodec;
	private AVCodec codec;
	private AVCodecContext context;
	private AVCodecParserContext parser;
	private AVFrame frame;
	private AVPacket packet;

	public FFMPEGDecoder(VideoCodec videoCodec)
	{
		this.videoCodec = videoCodec;
	}

	private void initializeIfNeeded()
	{
		if (codec != null)
		{
			return;
		}

		codec = avcodec_find_decoder(videoCodec.getCodecId());
		if (codec != null)
		{
			parser = av_parser_init(codec.id());
			if (parser != null)
			{
				context = avcodec_alloc_context3(codec);
				if (context != null)
				{
					if (avcodec_open2(context, codec, (PointerPointer<?>) null) == 0)
					{
						packet = av_packet_alloc();
						frame = av_frame_alloc();
						// All OK
						return;
					}
				}
			}
		}
	}

	private byte[] decode(byte[] buffer)
	{
		var sizePointer = new IntPointer();

		var ret = av_parser_parse2(parser, context, packet.data(), sizePointer, new BytePointer(buffer), buffer.length, AV_NOPTS_VALUE, AV_NOPTS_VALUE, 0);
		if (ret < 0)
		{
			// XXX: error!
		}

		if (sizePointer.get() > 0)
		{
			return decode();
		}
		// XXX: needs more buffers... how to signal that? return empty one?
		return new byte[0];
	}

	private byte[] decode()
	{
		int ret;
		ret = avcodec_send_packet(context, packet); // XXX: EAGAIN, etc...
		if (ret < 0)
		{
			// XXX: error!
		}

		while (ret >= 0)
		{
			ret = avcodec_receive_frame(context, frame);
			if (ret == AVERROR_EAGAIN() || ret == AVERROR_EOF())
			{
				return new byte[0]; // XXX
			}
			else if (ret < 0)
			{
				// XXX: error while decoding
			}

			var baos = new ByteArrayOutputStream();
			var frameData = new byte[frame.linesize().get() * frame.height()]; // XXX: I think...
			frame.data(0).get(frameData, 0, frame.linesize().get()); // XXX: mmm. doubtful...
			baos.write(frameData, 0, frameData.length);
			return baos.toByteArray();
		}
		return new byte[0];
	}

	public void cleanup()
	{
		if (packet != null)
		{
			av_packet_free(packet);
			packet.close();
		}

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

		if (parser != null)
		{
			av_parser_close(parser);
			parser.close();
		}

		if (codec != null)
		{
			codec.close();
		}
		PointerPointer.trimMemory();
	}
}

/*
 * Copyright (c) 2024 by David Gerber - https://zapek.com
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

package io.xeres.common.file;

import io.xeres.common.i18n.I18nEnum;
import io.xeres.common.i18n.I18nUtils;

import java.util.Locale;
import java.util.Set;

public enum FileType implements I18nEnum
{
	ANY(Set.of()),
	AUDIO(Set.of(
			"3ga", // Adaptive Multi-Rate Audio Codec
			"8svx", // Amiga IFF-8SVX File
			"aac", // Advanced Audio Coding
			"ac3", // Dolby Digital
			"aif", // Audio Interchange File Format
			"aifc", // Audio Interchange File Format
			"aiff", // Audio Interchange File Format
			"amr", // Adaptive Multi-Rate Audio Codec
			"ape", // Monkey's Audio Lossless Audio File
			"au", // Audio File (Sun Microsystems)
			"aud", // General Audio File
			"audio", // General Audio File
			"cda", // CD Audio Track
			"dmf", // D-Lusion Music Format Module
			"dsm", // Digital Sound Interface Kit Module
			"dts", // DTS Encoded Audio File
			"far", // Farandole Composer Module
			"flac", // Free Lossless Audio Codec File
			"it", // Impulse Tracker Module
			"m1a", // MPEG-1 Audio File
			"m2a", // MPEG-2 Audio File
			"m3u", // Multimedia Playlist File
			"m3u8", // Multimedia Playlist File (UTF-8)
			"m4a", // MPEG-4 Audio File
			"mdl", // DigiTrakker Module
			"med", // OctaMED Module
			"mid", // MIDI File
			"midi", // MIDI File
			"mka", // Matroska Audio File
			"mod", // Amiga Music Module (SoundTracker, ProTracker, etc...)
			"mp1", // MPEG Audio Layer I File
			"mp2", // MPEG Audio Layer II File
			"mp3", // MPEG Audio Layer III File
			"mpa", // MPEG Audio File
			"mpc", // Musepack
			"mtm", // MultiTracker Module
			"ogg", // Ogg Vorbis Audio File
			"psm", // ProTracker Studio Module
			"ptm", // PolyTracker Module
			"ra", // Real Audio File
			"ram", // Real Audio Meta File
			"rmi", // RIFF MIDI Music File
			"s3m", // ScreamTracker 3 Module
			"snd", // Audio File (Sun Microsystems)
			"stm", // ScreamTracker 2 Module
			"umx", // Unreal Engine 1 Music Format
			"wav", // Waveform Audio File Format
			"weba", // WebM Audio
			"wma", // Windows Media Audio
			"xm" // FastTracker 2 Extended Module
	)),
	ARCHIVE(Set.of(
			"7z", // 7-Zip
			"ace", // WinAce
			"adf", // Amiga Disk File
			"adz", // Amiga Disk File, GZipped
			"alz", // ALZip
			"arc", // ARC
			"arj", // ARJ
			"bin", // CD Image
			"br", // Brotli
			"bwa", // BlindWrite Disk Information File
			"bwi", // BlindWrite CD/DVD Disc Image
			"bws", // BlindWrite Sub Code File
			"bwt", // BlindWrite 4 Disk Image
			"bz2", // Bzip
			"cab", // Microsoft's Cabinet File
			"ccd", // CloneCD Disk Image
			"cif", // Easy CD Creator
			"cue", // CDWrite Cue Sheet File
			"dmg", // MacOS Disk Image
			"dms", // The DiskMasher System Amiga Disk Archiver
			"dsk", // Floppy Disk Archiving
			"gz", // GNU Zip
			"hqx", // BinHex 4.0
			"img", // Disk Image Data File
			"iso", // Disc Image File
			"lha", // LHA
			"lzh", // LZH
			"mdf", // Media Disc Image File
			"mds", // Daemon Tools
			"nrg", // Nero Burning Rom CD/DVD Image File
			"pak", // PAK
			"par", // Parchive Index
			"par2", // Parchive 2 Index
			"rar", // WinRAR
			"ratdvd", // RatDVD Disk Image
			"sea", // StuffIt Archive
			"sit", // StuffIt Archive
			"sitx", // Stuffit X Archive
			"tar", // Tape Archive (Unix)
			"tbz2", // BZip2-ed Tar File
			"tgz", // GZipped Tar File
			"toast", // Toast Disc Image
			"z", // Unix Compress
			"zip", // Zipped Archive
			"zst" // Zstandard
	)),
	DOCUMENT(Set.of(
			"adoc", // AsciiDoc
			"asc", // Text File
			"cbr", // Comic Book RAR Archive
			"cbz", // Comic Book ZIP Archive
			"chm", // Microsoft's Compiled HTML
			"css", // Cascading Style Sheet
			"csv", // Comma Separated Values
			"diz", // Description in Zip file
			"doc", // Microsoft Word Document
			"dot", // Microsoft Word Template
			"epub", // E-Books
			"hlp", // Microsoft Help File
			"htm", // HTML File
			"html", // HTML File
			"log", // Log File
			"md", // Markdown File
			"msg", // Outlook Mail Message File
			"nfo", // Warez Information File
			"ods", // Open Document Spreadsheet
			"odt", // Open Document Document
			"ott", // Open Document Template
			"pdf", // Portable Document Format
			"pps", // PowerPoint Slide Show
			"ppt", // PowerPoint Template
			"ps", // PostScript File
			"rtf", // Rich Text Format File
			"text", // Text File
			"txt", // Text File
			"wpd", // WordPerfect
			"wps", // Microsoft Works
			"wri", // Windows Write
			"xls", // Microsoft Excel Spreadsheet
			"xml" // eXtended Markup Language
	)),
	PICTURE(Set.of(
			"3dm", // OpenNURBS Initiative 3D Model
			"3dmf", // QuickDraw 3D Metafile
			"ai", // Adobe Illustrator
			"avif", // AV1 Image File Format
			"bmp", // Bitmap Image File
			"drw", // CADS Planner Drawing
			"dxf", // AutoCAD
			"emf", // Enhanced Windows Metafile
			"eps", // Encapsulated PostScript
			"gif", // Graphical Interchange Format File
			"heic", // High Efficiency Image Format
			"heif", // High Efficiency Image Format
			"ico", // Windows Icon file
			"iff", // Interchange File Format (Amiga)
			"indd", // Adobe InDesign
			"jfif", // JPEG File Interchange Format
			"jpe", // JPEG Image File
			"jpeg", // JPEG Image File
			"jpg", // JPEG Image File
			"lbm", // IFF Interleaved Bitmap
			"mng", // Multiple-Image Network Graphics Bitmap
			"pct", // PICT Picture File
			"pcx", // Paintbrush Bitmap Image File
			"pgm", // Portable GrayMap Bitmap File
			"pic", // PICT Picture File
			"pict", // PICT Picture File
			"pix", // Alias PIX Bitmap
			"png", // Portable Network Graphic
			"psd", // Photoshop Document
			"psp", // Paint Shop Pro Image File
			"qxd", // QuarkXPress
			"qxp", // QuarkXPress
			"rgb", // ColorViewSquash Bitmap
			"sgi", // Silicon Graphics Bitmap
			"svg", // Scalable Vector Graphics
			"tga", // Targa Graphic
			"tif", // Tagged Image File
			"tiff", // Tagged Image File
			"webp", // WebP Image File
			"wmf", // Windows Metafile
			"wmp", // Windows Media Photo File
			"xbm", // X Bitmap File
			"xcf", // GIMP Image
			"xif" // ScanSoft Pagis Extended Image Format File
	)),
	PROGRAM(Set.of(
			"apk", // Android Package
			"app", // MacOS Application Bundle
			"appimage", // AppImage
			"cmd", // Command File
			"com", // DOS executable
			"deb", // Debian Package
			"exe", // Executable File
			"flatpak", // Linux Flatpak Application Bundle
			"jar", // Java Archive
			"msi", // Microsoft Installer
			"pkg", // MacOS Installer
			"rpm", // RedHat Package
			"snap", // Canonical Snap Linux
			"xpi" // Mozilla Installer
	)),
	VIDEO(Set.of(
			"3g2", // 3GPP Multimedia File
			"3gp", // 3GPP Multimedia File
			"3gp2", // 3GPP Multimedia File
			"3gpp", // 3GPP Multimedia File
			"amv", // Anime Music Video File
			"asf", // Advanced Systems Format File
			"asx", // Advanced Stream Redirector File
			"avi", // Audio Video Interleave File
			"bik", // BINK Video File
			"divx", // DivX Movie File
			"dvr-ms", // Microsoft Digital Video Recording
			"flc", // FLIC Video File
			"fli", // FLIC Video File
			"flic", // FLIC Video File
			"flv", // Flash Video File
			"hdmov", // High-Definition QuickTime Movie
			"ifo", // DVD-Video Disc Information File
			"m1v", // MPEG-1 Video File
			"m2t", // MPEG-2 Video Transport Stream
			"m2ts", // MPEG-2 Video Transport Stream
			"m2v", // MPEG-2 Video File
			"m4b", // MPEG-4 Video File
			"m4v", // MPEG-4 Video File
			"mkv", // Matroska Video File
			"mov", // QuickTime Movie File
			"movie", // QuickTime Movie File
			"mp1v", // MPEG-1 Video File
			"mp2v", // MPEG-2 Video File
			"mp4", // MPEG-4 Video File
			"mpe", // MPEG Video File
			"mpeg", // MPEG Video File
			"mpg", // MPEG Video File
			"mpv", // MPEG Video File
			"mpv1", // MPEG-1 Video File
			"mpv2", // MPEG-2 Video File
			"ogm", // Ogg Media File
			"pva", // MPEG Video File
			"qt", // QuickTime Movie
			"rm", // Real Media File
			"rmm", // Real Media File
			"rmvb", // Real Video Variable Bit Rate File
			"rv", // Real Video File
			"smil", // SMIL Presentation File
			"smk", // Smacker Compressed Movie File
			"swf", // Macromedia Flash Movie
			"tp", // Video Transport Stream File
			"ts", // Video Transport Stream File
			"vid", // General Video File
			"video", // General Video File
			"vob", // DVD Video Object File
			"vp6", // TrueMotion VP6 Video File
			"webm", // WebM
			"wm", // Windows Media Video File"
			"wmv", // Windows Media Video File
			"xvid" // Xvid-Encoded Video File
	)),
	SUBTITLES(Set.of(
			"srt", // SubRip
			"sub"
	)),
	COLLECTION(Set.of(
			"emulecollection", // Emule
			"rscollection", // Retroshare
			"torrent" // Torrent
	)),
	DIRECTORY(Set.of());

	private final Set<String> extensions;

	FileType(Set<String> extensions)
	{
		this.extensions = extensions;
	}

	public Set<String> getExtensions()
	{
		return extensions;
	}

	@Override
	public String toString()
	{
		return I18nUtils.getString(getMessageKey(this));
	}

	public static FileType getTypeByExtension(String filename)
	{
		var index = filename.lastIndexOf(".");
		if (index == -1)
		{
			return ANY;
		}
		var extension = filename.substring(index + 1);
		if (extension.isEmpty())
		{
			return ANY;
		}
		for (var value : values())
		{
			if (value.getExtensions().contains(extension.toLowerCase(Locale.ROOT)))
			{
				return value;
			}
		}
		return ANY;
	}
}

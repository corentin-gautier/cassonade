"""Minimal PNG reading and writing for the texture scripts, so they need nothing beyond Python.

Pixels are rows of (r, g, b, a) tuples. Reads the non-interlaced PNGs Minecraft ships: greyscale,
RGB, palette, with or without alpha, 1 to 8 bits per sample.
"""

import struct
import zlib

SIGNATURE = b"\x89PNG\r\n\x1a\n"


def read_png(data):
	if data[:8] != SIGNATURE:
		raise ValueError("not a PNG")
	pos, idat, palette, transparency = 8, b"", None, None
	while pos < len(data):
		length, kind = struct.unpack(">I4s", data[pos:pos + 8])
		body = data[pos + 8:pos + 8 + length]
		if kind == b"IHDR":
			width, height, depth, color_type, _, _, interlace = struct.unpack(">IIBBBBB", body)
			if interlace:
				raise ValueError("interlaced PNGs are not supported")
		elif kind == b"PLTE":
			palette = [tuple(body[i:i + 3]) for i in range(0, len(body), 3)]
		elif kind == b"tRNS":
			transparency = body
		elif kind == b"IDAT":
			idat += body
		pos += 12 + length

	channels = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[color_type]
	bits_per_pixel = channels * depth
	stride = (width * bits_per_pixel + 7) // 8
	step = max(1, bits_per_pixel // 8)
	raw = zlib.decompress(idat)
	rows, previous = [], bytearray(stride)
	for y in range(height):
		start = y * (stride + 1)
		kind, line = raw[start], bytearray(raw[start + 1:start + 1 + stride])
		for i in range(stride):
			left = line[i - step] if i >= step else 0
			up = previous[i]
			up_left = previous[i - step] if i >= step else 0
			if kind == 1:
				line[i] = (line[i] + left) & 0xFF
			elif kind == 2:
				line[i] = (line[i] + up) & 0xFF
			elif kind == 3:
				line[i] = (line[i] + (left + up) // 2) & 0xFF
			elif kind == 4:
				p = left + up - up_left
				pa, pb, pc = abs(p - left), abs(p - up), abs(p - up_left)
				line[i] = (line[i] + (left if pa <= pb and pa <= pc else up if pb <= pc else up_left)) & 0xFF
		previous = line
		rows.append([_pixel(line, x, depth, color_type, palette, transparency) for x in range(width)])
	return rows


def _sample(line, index, depth):
	if depth == 8:
		return line[index]
	per_byte = 8 // depth
	byte = line[index // per_byte]
	shift = 8 - depth * (index % per_byte + 1)
	return (byte >> shift) & ((1 << depth) - 1)


def _pixel(line, x, depth, color_type, palette, transparency):
	if color_type == 3:
		index = _sample(line, x, depth)
		alpha = transparency[index] if transparency and index < len(transparency) else 255
		return palette[index] + (alpha,)
	scale = 255 // ((1 << depth) - 1)
	if color_type == 0:
		grey = _sample(line, x, depth) * scale
		return (grey, grey, grey, 255)
	if color_type == 4:
		return (line[x * 2], line[x * 2], line[x * 2], line[x * 2 + 1])
	if color_type == 2:
		return tuple(line[x * 3:x * 3 + 3]) + (255,)
	return tuple(line[x * 4:x * 4 + 4])


def write_png(path, pixels):
	height, width = len(pixels), len(pixels[0])
	raw = b"".join(b"\0" + bytes(c for px in row for c in px) for row in pixels)

	def chunk(kind, data):
		body = kind + data
		return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body))

	png = SIGNATURE
	png += chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
	png += chunk(b"IDAT", zlib.compress(raw, 9))
	png += chunk(b"IEND", b"")
	path.write_bytes(png)

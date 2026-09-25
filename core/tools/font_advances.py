"""Generate core/src/main/resources/font/default_advances.bin from a vanilla client.

The file holds one byte per BMP code point for the vanilla default font (the font every
unstyled action bar, chat line and title uses). The low 7 bits are the glyph's advance in
pixels. The high bit marks a unifont glyph, whose bold offset is half a pixel instead of one.
Code points no provider covers get the advance of the missing-glyph box the client draws.

Usage: python font_advances.py <minecraft-client.jar> <unifont.json> <unifont.zip>

Unifont is not in the jar. Both files are launcher assets, `minecraft/font/include/unifont.json`
and `minecraft/font/unifont.zip`, found by hash under .minecraft/assets/objects via the index.
"""
import io
import json
import sys
import zipfile
from pathlib import Path

from PIL import Image

UNIFONT_FLAG = 0x80
MISSING_ADVANCE = 6
OUTPUT = Path(__file__).resolve().parents[1] / "src/main/resources/font/default_advances.bin"


def main(client_jar, unifont_json, unifont_zip):
    advances = bytearray(0x10000)
    filled = [False] * 0x10000

    def put(code_point, advance, flag=0):
        if code_point < 0x10000 and not filled[code_point]:
            filled[code_point] = True
            advances[code_point] = min(advance, 0x7F) | flag

    with zipfile.ZipFile(client_jar) as jar:
        for char, advance in json.loads(jar.read("assets/minecraft/font/include/space.json"))["providers"][0]["advances"].items():
            put(ord(char), advance)
        for provider in json.loads(jar.read("assets/minecraft/font/include/default.json"))["providers"]:
            if provider["type"] == "bitmap":
                read_bitmap(jar, provider, put)

    unifont = json.loads(Path(unifont_json).read_text(encoding="utf-8"))["providers"]
    definition = next(p for p in unifont if p["hex_file"] == "minecraft:font/unifont.zip")
    read_unihex(unifont_zip, definition, put)

    for code_point in range(0x10000):
        if not filled[code_point]:
            advances[code_point] = MISSING_ADVANCE

    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_bytes(advances)
    print(f"{sum(filled)} glyphs -> {OUTPUT}")


def read_bitmap(jar, provider, put):
    namespace, path = provider["file"].split(":")
    image = Image.open(io.BytesIO(jar.read(f"assets/{namespace}/textures/{path}"))).convert("RGBA")
    alpha = image.getchannel("A").load()
    rows = provider["chars"]
    cell_w = image.width // len(rows[0])
    cell_h = image.height // len(rows)
    scale = provider.get("height", 8) / cell_h
    for r, row in enumerate(rows):
        for c, char in enumerate(row):
            if char in ("\0", " "):
                continue
            width = 0
            for x in range(cell_w - 1, -1, -1):
                if any(alpha[c * cell_w + x, r * cell_h + y] for y in range(cell_h)):
                    width = x + 1
                    break
            put(ord(char), int(0.5 + width * scale) + 1)


def read_unihex(unifont_zip, definition, put):
    overrides = [(ord(o["from"]), ord(o["to"]), o["left"], o["right"]) for o in definition.get("size_overrides", [])]
    with zipfile.ZipFile(unifont_zip) as archive:
        for name in archive.namelist():
            if not name.endswith(".hex"):
                continue
            for line in archive.read(name).decode("ascii").splitlines():
                if ":" not in line:
                    continue
                code, bits = line.split(":")
                code_point = int(code, 16)
                bit_width = len(bits) * 4 // 16
                override = next(((l, r) for f, t, l, r in overrides if f <= code_point <= t), None)
                if override:
                    left, right = override
                else:
                    left, right = measure(bits, bit_width)
                put(code_point, (right - left + 1) // 2 + 1, UNIFONT_FLAG)


def measure(bits, bit_width):
    row_len = bit_width // 4
    mask = 0
    for i in range(0, len(bits), row_len):
        mask |= int(bits[i:i + row_len], 16)
    if mask == 0:
        return 0, bit_width
    columns = [x for x in range(bit_width) if mask >> (bit_width - 1 - x) & 1]
    return columns[0], columns[-1]


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2], sys.argv[3])

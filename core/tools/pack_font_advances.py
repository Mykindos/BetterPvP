"""Generate an advance table for a resource pack font, in the same format as default_advances.bin.

Providers are read in order like the client does. Bitmap textures resolve from the pack's assets folder, and
`minecraft:` textures the pack does not override come from the client jar. A `reference` to `minecraft:default`
fills every code point not yet covered from default_advances.bin, unifont flags included.

Usage: python pack_font_advances.py <pack assets dir> <namespace:font> <minecraft-client.jar> <output name>
Example: python pack_font_advances.py ../Resourcepack/pack/assets betterpvp:rpg 1.21.11.jar rpg_advances.bin
"""
import io
import json
import sys
import zipfile
from pathlib import Path

from PIL import Image

from font_advances import MISSING_ADVANCE, OUTPUT as DEFAULT_TABLE, read_bitmap

FONT_DIR = DEFAULT_TABLE.parent


def main(assets_dir, font, client_jar, output_name):
    assets = Path(assets_dir)
    advances = bytearray(0x10000)
    filled = [False] * 0x10000
    default_table = DEFAULT_TABLE.read_bytes()

    def put(code_point, value):
        if code_point < 0x10000 and not filled[code_point]:
            filled[code_point] = True
            advances[code_point] = value

    with zipfile.ZipFile(client_jar) as jar:
        def texture(file):
            namespace, path = file.split(":")
            local = assets / namespace / "textures" / path
            if local.exists():
                return Image.open(local)
            return Image.open(io.BytesIO(jar.read(f"assets/{namespace}/textures/{path}")))

        namespace, name = font.split(":")
        providers = json.loads((assets / namespace / "font" / f"{name}.json").read_text(encoding="utf-8"))["providers"]
        for provider in providers:
            kind = provider["type"]
            if kind == "space":
                for char, advance in provider["advances"].items():
                    put(ord(char), min(advance, 0x7F))
            elif kind == "bitmap":
                read_bitmap(texture(provider["file"]).convert("RGBA"), provider, put)
            elif kind == "reference" and provider["id"] == "minecraft:default":
                for code_point in range(0x10000):
                    put(code_point, default_table[code_point])
            else:
                sys.exit(f"Unsupported provider {kind} {provider.get('id', '')}")

    for code_point in range(0x10000):
        if not filled[code_point]:
            advances[code_point] = MISSING_ADVANCE

    output = FONT_DIR / output_name
    output.write_bytes(advances)
    print(f"{font} -> {output}")


if __name__ == "__main__":
    main(*sys.argv[1:5])

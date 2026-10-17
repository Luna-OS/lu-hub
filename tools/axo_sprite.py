"""Erzeugt das Axo-Sprite-Sheet (assets/luhub/textures/gui/axo.png) und das Mod-Icon.

Axo ist hier als Pixel-Raster beschrieben. Farben/Pixel ändern und das Skript neu starten:
    python3 tools/axo_sprite.py
Benötigt Pillow (pip install pillow).
"""
from pathlib import Path
import math
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/luhub"

# . leer, o Umriss (lila), w Körper (weiß), p Kiemen (rosa), P Kiemenansatz/Mund (dunkleres Rosa),
# b Bäckchen, e Augen, l Pfötchen (lavendel), h Glanzpunkt
AXO = [
    ".p.......oooooooo.......p.",
    "..p....oowwwwwwwwoo....p..",
    "...p..owwwwwwwwwwwwo..p...",
    "....PPwwwwwwwwwwwwwwPP....",
    "pp...owwwwwwwwwwwwwwo...pp",
    "..ppPPwwwhewwwwhewwwPPpp..",
    ".....owwweewwwweewwwo.....",
    "....PPwwbwwwwwwwwbwwPP....",
    "...p.owwwwwwPPwwwwwwo.p...",
    "..p...owwwwwwwwwwwwo...p..",
    ".p.....oowwwwwwwwoo.....p.",
    ".........oooooooo.........",
    "........owwwwwwwwo.ooo....",
    ".......oowwlwwlwwooowo....",
    "......owowwwwwwwwwwwo.....",
    ".......o.owwwwwwoooo......",
    "..........oooooo..........",
]
COLORS = {
    "o": (92, 76, 160), "w": (250, 246, 240), "p": (242, 170, 206), "P": (226, 128, 178),
    "b": (244, 186, 152), "e": (24, 18, 54), "l": (218, 210, 250), "h": (255, 255, 255),
}
W, H = len(AXO[0]), len(AXO)
PAD = 1  # Platz für das Leuchten
FW, FH = W + PAD * 2, H + PAD * 2


def grid():
    return [list(r) for r in AXO]


def wiggle(g):
    """Kiemenspitzen eine Zeile nach oben – sieht aus, als würden sie flattern."""
    tips = [(y, x) for y in range(H) for x in range(W) if g[y][x] == "p" and (x <= 2 or x >= W - 3)]
    for y, x in tips:
        if y > 0 and g[y - 1][x] == ".":
            g[y - 1][x], g[y][x] = "p", "."
    return g


def blink(g):
    for y in range(H):
        for x in range(W):
            if g[y][x] in "eh" and y == 5:
                g[y][x] = "w"
    return g


def happy(g):
    g = blink(g)
    g[9][12] = g[9][13] = "p"  # Mund offen
    return g


def wag(g):
    """Schwanz eine Zeile hochwedeln."""
    for x in range(18, 22):
        g[11][x] = g[12][x] if g[12][x] in "ow" else g[11][x]
    g[12][19:22] = list("owo") if g[13][20] == "w" else g[12][19:22]
    g[13][19:22] = list("oo.")
    return g


FRAMES = [
    grid(),                 # 0 Ruhe
    wiggle(grid()),         # 1 Kiemen wackeln
    blink(grid()),          # 2 blinzeln
    happy(grid()),          # 3 fröhlich (Maus drüber)
    wag(wiggle(happy(grid()))),  # 4 fröhlich + Schwanz wedeln
]


def render(g):
    im = Image.new("RGBA", (FW, FH), (0, 0, 0, 0))
    solid = set()
    for y, row in enumerate(g):
        for x, ch in enumerate(row):
            if ch in COLORS:
                im.putpixel((x + PAD, y + PAD), COLORS[ch] + (255,))
                solid.add((x + PAD, y + PAD))
    # weiches Leuchten rund um den Umriss (nicht an den Kiemen)
    for (x, y) in list(solid):
        if g[y - PAD][x - PAD] not in "ow":
            continue
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            p = (x + dx, y + dy)
            if 0 <= p[0] < FW and 0 <= p[1] < FH and p not in solid:
                im.putpixel(p, (235, 228, 255, 46))
    return im


def main():
    sheet = Image.new("RGBA", (FW * len(FRAMES), FH), (0, 0, 0, 0))
    for i, f in enumerate(FRAMES):
        sheet.alpha_composite(render(f), (i * FW, 0))
    out = ASSETS / "textures/gui/axo.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(out)

    # Mod-Icon: Axo vor Nachthimmel mit Mond
    icon = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    for y in range(32):
        for x in range(32):
            if (x - 15.5) ** 2 + (y - 15.5) ** 2 <= 15.5 ** 2:
                t = y / 31
                icon.putpixel((x, y), (int(14 + 20 * t), int(12 + 14 * t), int(38 + 40 * t), 255))
    for (x, y) in [(5, 8), (26, 6), (9, 4), (23, 13), (4, 17), (28, 20), (15, 3), (19, 6)]:
        icon.putpixel((x, y), (255, 244, 194, 255))
    for y in range(32):
        for x in range(32):
            d = math.hypot(x + .5 - 24, y + .5 - 8)
            d2 = math.hypot(x + .5 - 26.5, y + .5 - 6.5)
            if d <= 5 and d2 > 4.2:
                icon.putpixel((x, y), (255, 241, 184, 255))
    icon.alpha_composite(render(FRAMES[0]), (2, 12))
    icon.resize((128, 128), Image.NEAREST).save(ASSETS / "icon.png")
    print(f"Sheet {sheet.size}, Frame {FW}x{FH}")


if __name__ == "__main__":
    main()

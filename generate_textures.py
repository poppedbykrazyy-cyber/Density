#!/usr/bin/env python3
"""Generate custom textures for the Density SMP resource pack."""
from PIL import Image, ImageDraw
import math
import os
import zipfile

BASE = "/home/user/Density/resourcepack"

def ensure_dir(path):
    os.makedirs(os.path.dirname(path), exist_ok=True)

def create_density_icon(color, filename, size=16):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    cx, cy = size // 2, size // 2
    for i in range(size // 2 - 1, 0, -1):
        alpha = int(255 * (i / (size // 2)))
        draw.ellipse([cx - i, cy - i, cx + i, cy + i], fill=(color[0], color[1], color[2], alpha))
    draw.ellipse([cx - 2, cy - 2, cx + 2, cy + 2], fill=(255, 255, 255, 255))
    path = os.path.join(BASE, "assets/densitysmp/textures/gui", filename)
    ensure_dir(path)
    img.save(path)

def create_orb(color, filename, size=16):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    cx, cy = size // 2, size // 2
    for i in range(7, 0, -1):
        alpha = int(180 * (i / 7))
        draw.ellipse([cx - i, cy - i, cx + i, cy + i], fill=(color[0], color[1], color[2], alpha))
    r2 = min(color[0] + 80, 255)
    g2 = min(color[1] + 80, 255)
    b2 = min(color[2] + 80, 255)
    draw.ellipse([cx - 3, cy - 3, cx + 3, cy + 3], fill=(r2, g2, b2, 255))
    draw.ellipse([cx - 2, cy - 3, cx, cy - 1], fill=(255, 255, 255, 200))
    path = os.path.join(BASE, "assets/densitysmp/textures/item", filename)
    ensure_dir(path)
    img.save(path)

def create_badge(primary, secondary, filename, size=32):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    cx, cy = size // 2, size // 2
    points = [(cx, 2), (size - 2, cy), (cx, size - 2), (2, cy)]
    draw.polygon(points, fill=primary, outline=secondary)
    inner = [(cx, 6), (size - 6, cy), (cx, size - 6), (6, cy)]
    draw.polygon(inner, fill=secondary)
    draw.ellipse([cx - 4, cy - 4, cx + 4, cy + 4], fill=primary)
    draw.ellipse([cx - 2, cy - 2, cx + 2, cy + 2], fill=(255, 255, 255, 230))
    path = os.path.join(BASE, "assets/densitysmp/textures/gui", filename)
    ensure_dir(path)
    img.save(path)

def create_particle(color, filename, size=8):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    cx, cy = size // 2, size // 2
    for i in range(3, 0, -1):
        alpha = int(255 * (i / 3))
        draw.ellipse([cx - i, cy - i, cx + i, cy + i], fill=(color[0], color[1], color[2], alpha))
    path = os.path.join(BASE, "assets/densitysmp/textures/misc", filename)
    ensure_dir(path)
    img.save(path)

def create_flow_bar(color, filled, filename, w=5, h=9):
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    if filled:
        draw.rectangle([0, 0, w - 1, h - 1], fill=color + (255,), outline=(0, 0, 0, 200))
    else:
        draw.rectangle([0, 0, w - 1, h - 1], fill=(40, 40, 40, 180), outline=(60, 60, 60, 200))
    path = os.path.join(BASE, "assets/densitysmp/textures/font", filename)
    ensure_dir(path)
    img.save(path)

def create_weapon_icon(color, accent, filename, size=16):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    # Sword-like shape
    draw.line([(3, 13), (13, 3)], fill=color + (255,), width=2)
    draw.line([(5, 11), (11, 5)], fill=accent + (255,), width=1)
    # Handle
    draw.rectangle([1, 13, 4, 15], fill=(100, 80, 60, 255))
    # Glow
    draw.ellipse([5, 5, 11, 11], fill=color + (80,))
    path = os.path.join(BASE, "assets/densitysmp/textures/item", filename)
    ensure_dir(path)
    img.save(path)

def create_pack_icon(size=64):
    img = Image.new("RGBA", (size, size), (20, 10, 30, 255))
    draw = ImageDraw.Draw(img)
    cx, cy = size // 2, size // 2
    for i in range(30, 0, -1):
        r = int(40 + (170 - 40) * ((30 - i) / 30))
        b = int(80 + (255 - 80) * ((30 - i) / 30))
        alpha = int(100 + 155 * (i / 30))
        draw.ellipse([cx - i, cy - i, cx + i, cy + i], fill=(r, 0, b, alpha))
    for i in range(15, 0, -1):
        alpha = int(200 * (i / 15))
        draw.ellipse([cx - i, cy - i, cx + i, cy + i], fill=(100, 0, 200, alpha))
    draw.ellipse([cx - 5, cy - 5, cx + 5, cy + 5], fill=(200, 150, 255, 255))
    draw.ellipse([cx - 3, cy - 3, cx + 3, cy + 3], fill=(255, 220, 255, 255))
    draw.ellipse([cx - 1, cy - 1, cx + 1, cy + 1], fill=(255, 255, 255, 255))
    for angle in range(0, 360, 45):
        rad = math.radians(angle)
        ox = int(cx + 22 * math.cos(rad))
        oy = int(cy + 22 * math.sin(rad))
        draw.ellipse([ox - 2, oy - 2, ox + 2, oy + 2], fill=(200, 150, 255, 200))
    img.save(os.path.join(BASE, "pack.png"))

print("Generating textures...")

# Density tiers
tiers = {
    "fragile":   (255, 255, 255),
    "lean":      (85, 255, 255),
    "balanced":  (85, 255, 85),
    "fortified": (255, 170, 0),
    "titan":     (255, 85, 85),
    "colossus":  (170, 0, 170),
}

for name, color in tiers.items():
    create_density_icon(color, f"tier_{name}.png")
    create_orb(color, f"orb_{name}.png")
    lighter = (min(color[0] + 50, 255), min(color[1] + 50, 255), min(color[2] + 50, 255), 255)
    create_badge(color + (255,), lighter, f"badge_{name}.png")
    create_particle(color, f"particle_{name}.png")
    create_flow_bar(color, True, f"bar_filled_{name}.png")
    create_flow_bar(color, False, f"bar_empty_{name}.png")

# Flow levels
flow_levels = {
    "warmed_up":    (255, 255, 85),
    "in_the_zone":  (255, 170, 0),
    "flowing":      (255, 85, 85),
    "unstoppable":  (170, 0, 0),
    "transcendent": (170, 0, 170),
}

for name, color in flow_levels.items():
    create_density_icon(color, f"flow_{name}.png")
    create_flow_bar(color, True, f"flow_bar_{name}.png")

# Weapon icons
weapons = {
    "density_blade": ((170, 0, 170), (220, 100, 220)),
    "void_axe":      ((50, 50, 50), (100, 100, 100)),
    "flow_scythe":   ((255, 85, 85), (255, 170, 170)),
    "siphon_bow":    ((255, 85, 255), (255, 170, 255)),
    "mass_shifter":  ((85, 255, 255), (170, 255, 255)),
}

for name, (color, accent) in weapons.items():
    create_weapon_icon(color, accent, f"weapon_{name}.png")

create_density_icon((170, 0, 0), "tier_eliminated.png")
create_pack_icon()

print("All textures generated!")

# Create ZIP
print("Creating resource pack ZIP...")
zip_path = "/home/user/Density/DensitySMP-ResourcePack.zip"
with zipfile.ZipFile(zip_path, 'w', zipfile.ZIP_DEFLATED) as zf:
    for root, dirs, files in os.walk(BASE):
        for file in files:
            file_path = os.path.join(root, file)
            arcname = os.path.relpath(file_path, BASE)
            zf.write(file_path, arcname)

print(f"Resource pack created: {zip_path}")

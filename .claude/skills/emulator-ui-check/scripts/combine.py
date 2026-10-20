"""Put several screenshots side by side so one image shows a whole flow.

Usage: python combine.py OUT.png SHOTS_DIR NAME[:x0,y0,x1,y1] [NAME ...]
Each NAME is a PNG in SHOTS_DIR (without extension); the optional box crops it (pixels of the 1080x2424 screen).
Prints the output path and the size of each cell.
"""
import sys

from PIL import Image

out, shots_dir, *specs = sys.argv[1:]
cell_width = 540 if len(specs) <= 2 else 360
images = []
for spec in specs:
    name, _, box = spec.partition(":")
    image = Image.open(f"{shots_dir}/{name}.png").convert("RGB")
    if box:
        image = image.crop(tuple(int(v) for v in box.split(",")))
    height = int(image.height * cell_width / image.width)
    images.append(image.resize((cell_width, height)))

sheet = Image.new("RGB", ((cell_width + 10) * len(images) - 10, max(i.height for i in images)), "white")
for index, image in enumerate(images):
    sheet.paste(image, (index * (cell_width + 10), 0))
sheet.save(out)
print(out, [image.size for image in images])

#!/usr/bin/env python3
"""Writes every texture, model and animation under src/main/resources/assets.

Rerun after changing anything here; the output is checked in, and nothing else writes it.

A cobweb is drawn by Pandorical's Strands provider with one of: its share of an orb web filling an
open rectangle of webs; a flat sheet through the block's middle, drawn whole for the sides holding
it, frayed along its lower edge when it hangs; a sheet across a corner at 45 degrees; a frayed edge
hanging from above; or a small web on a floor or wall with a low clump of silk over it.

Threads leave a block only at its ports, PORTS along each edge, and every sheet uses all of them on
each side holding it, so a sheet carried on into the next block meets it thread for thread
whichever variant either block drew. Every piece comes in a
few variants, the paths between the ports drawn differently, and the provider picks one per block
by position, so a big web does not repeat block to block.
"""

import json
import math
import random
import shutil
from pathlib import Path

from PIL import Image, ImageDraw

MOD_ID = "better-webs-justfatlard"
ROOT = Path(__file__).parent / "src/main/resources"
ASSETS = ROOT / "assets"

# Vanilla's cobweb is drawn in these three: silk, shadowed silk, and the thread furthest out.
SILK = (255, 255, 255, 255)
SHADE = (228, 233, 233, 255)
FAR = (196, 206, 210, 255)

# Where threads cross a block's edge. Every sheet uses every port on each side holding it, so every
# thread that reaches an edge carries on into the next block. A face only ever meets the same face
# of its neighbour, mirrored the same way, so the ports need not be symmetric.
PORTS = (3, 8, 12)

# The middle of a texture, which threads are drawn a little toward.
HUB = (7.5, 7.5)

OPPOSITE_SCREEN = {"up": "down", "down": "up", "left": "right", "right": "left"}

VARIANTS = 5

# Under the game's default UVs, which way across each face is right and which is up, as a
# direction in the world.
FACE_AXES = {
	"up": {"right": "east", "up": "north"},
	"down": {"right": "east", "up": "south"},
	"north": {"right": "west", "up": "up"},
	"south": {"right": "east", "up": "up"},
	"west": {"right": "south", "up": "up"},
	"east": {"right": "north", "up": "up"},
}

OPPOSITE = {"up": "down", "down": "up", "north": "south", "south": "north", "east": "west", "west": "east"}
AXIS_OF = {"up": "y", "down": "y", "north": "z", "south": "z", "east": "x", "west": "x"}
ORDER = ["down", "up", "north", "south", "west", "east"]
NEGATIVE = {"down", "north", "west"}

# A sheet facing along an axis: the flat element through the middle, and its two faces.
SHEETS = {
	"x": ({"from": [8, 0, 0], "to": [8, 16, 16]}, ("east", "west")),
	"y": ({"from": [0, 8, 0], "to": [16, 8, 16]}, ("up", "down")),
	"z": ({"from": [0, 0, 8], "to": [16, 16, 8]}, ("north", "south")),
}

# Held on one side only and that side not above, the web lies flat against what holds it, a hair
# off the face so it never fights it for depth.
HUGS = {
	"down": ({"from": [0, 0.2, 0], "to": [16, 0.2, 16]}, ("up", "down")),
	"north": ({"from": [0, 0, 0.2], "to": [16, 16, 0.2]}, ("south", "north")),
	"south": ({"from": [0, 0, 15.8], "to": [16, 16, 15.8]}, ("north", "south")),
	"west": ({"from": [0.2, 0, 0], "to": [0.2, 16, 16]}, ("east", "west")),
	"east": ({"from": [15.8, 0, 0], "to": [15.8, 16, 16]}, ("west", "east")),
}


def write_json(path, data):
	path.parent.mkdir(parents=True, exist_ok=True)
	path.write_text(json.dumps(data, indent=2) + "\n")


def save(image, path):
	path.parent.mkdir(parents=True, exist_ok=True)
	image.save(path)


# ---- drawing ----------------------------------------------------------------------------------

def blank():
	return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def curve(draw, start, bend, end, colour):
	"""A thread from start to end pulled toward bend, the way silk sags or is drawn aside."""
	points = []
	for i in range(17):
		t = i / 16
		x = (1 - t) ** 2 * start[0] + 2 * (1 - t) * t * bend[0] + t ** 2 * end[0]
		y = (1 - t) ** 2 * start[1] + 2 * (1 - t) * t * bend[1] + t ** 2 * end[1]
		points.append((round(x), round(y)))
	draw.line(points, fill=colour, width=1)


def jitter(rng, point, by=1.5):
	return (point[0] + rng.uniform(-by, by), point[1] + rng.uniform(-by, by))


def port(side, at):
	"""A point on an edge of the texture: top, bottom, left or right, at a port."""
	return {"up": (at, 0), "down": (at, 15), "left": (0, at), "right": (15, at)}[side]


def sheet(sides, rng, hanging=False):
	"""A web held on these sides of the texture: threads from each holding side's ports to another
	holding side's, never through one knot, so a big sheet is threads carried on block to block
	rather than a star in every block.

	Hanging, its threads keep to the upper half and its lower edge is frayed: loose strands falling
	from them, each to its own length, with no hem to gather them into a shape.
	"""
	image = blank()
	draw = ImageDraw.Draw(image)
	# Where each thread leaves its side varies piece to piece, so a big sheet is not ruled into a
	# grid: threads meet a neighbour's somewhere along the edge rather than at set points.
	def ports_on(side):
		# Hanging, the sides keep to their upper ports, leaving the lower edge free to fray; the
		# next hanging web along keeps to the same ones.
		return PORTS[:1] if hanging and side in ("left", "right") else PORTS

	ends = [(side, at) for side in sides for at in ports_on(side)]
	rng.shuffle(ends)
	unused = list(ends)
	threads = []
	while unused:
		side, at = unused.pop()
		others = [e for e in unused if e[0] != side] or [e for e in ends if e[0] != side]
		# As often to a neighbouring side as straight across: straight across every time rules a
		# big sheet into rows, where turning as often makes it one tangle zigzagging block to block.
		across = [e for e in others if e[0] == OPPOSITE_SCREEN[side]]
		other = rng.choice(across if across and rng.random() < 0.5 else others)
		if other in unused:
			unused.remove(other)
		threads.append((port(side, at), port(*other)))

	drawn = []
	for i, (start, end) in enumerate(threads):
		if hanging:
			# Kept to the upper half, so the lower edge is left to fray.
			start = (start[0], min(start[1], 8))
			end = (end[0], min(end[1], 8)) if end[1] != 0 else end
		# Straight, as silk pulled taut is. Held to the clean angles of pixel art a tangle turns into
		# wire mesh; a straight line at its own angle reads as a thread. One strong thread carries
		# the sheet, the rest are finer silk between.
		draw.line([start, end], fill=SILK if i == 0 else (SHADE if i % 3 == 1 else FAR), width=1)
		drawn.append((start, end))
	# A couple of short wisps off the threads, the fluff that makes silk read as silk.
	for start, end in rng.sample(drawn, min(2, len(drawn))):
		t = rng.uniform(0.3, 0.7)
		x, y = round(start[0] + (end[0] - start[0]) * t), round(start[1] + (end[1] - start[1]) * t)
		draw.line([(x, y), (min(15, max(0, x + rng.choice((-3, -2, 2, 3)))), min(15, max(0, y + rng.choice((-2, 2, 3)))))], fill=FAR, width=1)

	if hanging:
		fray(draw, rng, lambda x: lowest(image, x))
	return image


def lowest(image, x):
	"""The lowest drawn pixel in a column above the frayed edge, or None where none is."""
	for y in range(11, -1, -1):
		if image.getpixel((x, y))[3]:
			return y
	return None


def fray(draw, rng, top_at):
	"""Loose strands hanging from a web's lower edge: uneven gaps, uneven lengths, each drifting off
	plumb as it falls, now and then one leaning on its neighbour, fading at the tip. The edge of a
	web that tore, not a fringe cut straight."""
	x = rng.randint(0, 2)
	last = None
	while x < 16:
		top = top_at(x)
		if top is not None and top < 14:
			length = rng.choice((1, 2, 3, 3, 4, 5, 6, 8))
			bottom = min(15, top + length)
			lean = rng.choice((-2, -1, -1, 0, 1, 1, 2)) if length > 2 else 0
			tip = (min(15, max(0, x + lean)), bottom)
			curve(draw, (x, top), (x, (top + bottom) / 2), tip, SHADE)
			draw.point(tip, fill=FAR)
			# Now and then a strand tangles into the one before it.
			if last is not None and rng.random() < 0.3:
				draw.line([last, (x, min(15, top + rng.randint(1, 3)))], fill=FAR, width=1)
			last = (tip[0], max(top, bottom - 2))
		x += rng.randint(2, 5)


ORB_MOST = 4
ORB_VARIANTS = 2


def orb_canvas(width, height, rng):
	"""One orb web filling a rectangle of blocks, drawn whole and cut into a tile per block: spokes
	from a hub a little above the middle out to the edge, where they tie straight into whatever holds
	the web, and a spiral that keeps the rectangle's shape as it winds in, a few of its turns broken.
	No frame thread round the edge: drawn on every side it boxes the web in, even where it hangs free."""
	w, h = 16 * width, 16 * height
	image = Image.new("RGBA", (w, h), (0, 0, 0, 0))
	draw = ImageDraw.Draw(image)
	hub = (w / 2 + rng.uniform(-2, 2), h * 0.45 + rng.uniform(-1, 1))
	count = 8 + 2 * (max(width, height) - 1)
	spokes = []
	for i in range(count):
		angle = 2 * math.pi * (i + rng.uniform(-0.2, 0.2)) / count
		dx, dy = math.cos(angle), math.sin(angle)
		# Out to the frame: the nearest edge of the rectangle the spoke meets.
		reach = min(
			((w - 1 - hub[0]) / dx) if dx > 1e-6 else ((0 - hub[0]) / dx) if dx < -1e-6 else math.inf,
			((h - 1 - hub[1]) / dy) if dy > 1e-6 else ((0 - hub[1]) / dy) if dy < -1e-6 else math.inf)
		spokes.append((hub[0] + dx * reach, hub[1] + dy * reach))
	typical = sum(math.dist(hub, end) for end in spokes) / len(spokes)
	turns = max(2, int(typical / 3.2))
	for k in range(1, turns + 1):
		# An open ring round the hub before the spiral starts, as an orb has.
		t = 0.26 + 0.70 * k / (turns + 1)
		ring = [(hub[0] + (ex - hub[0]) * t, hub[1] + (ey - hub[1]) * t) for ex, ey in spokes]
		colour = SHADE if k <= turns / 2 else FAR
		for (x1, y1), (x2, y2) in zip(ring, ring[1:] + ring[:1]):
			if rng.random() < 0.08:
				continue
			# Sagging a little toward the hub between spokes.
			mid = ((x1 + x2) / 2 + (hub[0] - (x1 + x2) / 2) * 0.06, (y1 + y2) / 2 + (hub[1] - (y1 + y2) / 2) * 0.06)
			draw.line([(round(x1), round(y1)), (round(mid[0]), round(mid[1])), (round(x2), round(y2))], fill=colour, width=1)
	for end in spokes:
		draw.line([(round(hub[0]), round(hub[1])), (round(end[0]), round(end[1]))], fill=SILK, width=1)
	image.putpixel((round(hub[0]), round(hub[1])), SILK)
	return image


def tuft(rng):
	"""A web held only from above: the frayed lower edge of whatever holds it, loose strands spread
	across the width so they carry on a curtain's edge above, and nothing gathering them."""
	image = blank()
	draw = ImageDraw.Draw(image)
	fray(draw, rng, lambda x: 0)
	fray(draw, rng, lambda x: rng.choice((0, 1, 2)) if rng.random() < 0.5 else None)
	return image


def clump(rng):
	"""A few loose threads for the little mound a web held on one side makes, faint and low in the
	middle, the same from top or bottom since it is stood on its base either way up."""
	image = blank()
	draw = ImageDraw.Draw(image)
	for i in range(5):
		x1, x2 = rng.randint(0, 5), rng.randint(10, 15)
		draw.line([(x1, rng.randint(3, 12)), (x2, rng.randint(3, 12))], fill=(SHADE, SHADE, FAR, FAR, FAR)[i], width=1)
	return image


def hug(rng):
	"""A small web lying flat against a floor or wall: a few short spokes in the softer silk and a
	faint round, so it sits on the surface rather than shining off it."""
	image = blank()
	draw = ImageDraw.Draw(image)
	centre = (7.5 + rng.uniform(-1, 1), 7.5 + rng.uniform(-1, 1))
	spokes = []
	for i in range(6):
		angle = 2 * math.pi * i / 6 + rng.uniform(-0.35, 0.35)
		reach = rng.uniform(5.5, 7.5)
		spokes.append((centre[0] + reach * math.cos(angle), centre[1] + reach * math.sin(angle)))
	ring = [(centre[0] + (x - centre[0]) * 0.6, centre[1] + (y - centre[1]) * 0.6) for x, y in spokes]
	draw.line([(round(x), round(y)) for x, y in ring + ring[:1]], fill=FAR, width=1)
	for end in spokes:
		draw.line([(round(centre[0]), round(centre[1])), (round(end[0]), round(end[1]))], fill=SHADE, width=1)
	return image


def corner_strip(rng):
	"""The sheet across a corner, its texture laid so left to right runs along the corner and top and
	bottom are the two faces it is tied to: the same tangle as any other sheet, so a corner matches
	the webs round it. Its threads carry on along the corner through the side ports and tie off
	against the faces wherever they land."""
	return sheet(("up", "down", "left", "right"), rng)


def icon():
	"""A web strung across the corner of a dark room, the way a spider leaves one."""
	room = Image.new("RGBA", (32, 32), (34, 30, 38, 255))
	draw = ImageDraw.Draw(room)
	draw.rectangle([0, 0, 31, 3], fill=(58, 52, 62, 255))
	draw.rectangle([0, 0, 3, 31], fill=(58, 52, 62, 255))
	rng = random.Random(7)
	web = blank()
	web.alpha_composite(sheet(("up", "left"), rng))
	room.alpha_composite(web.resize((28, 28), Image.NEAREST), (4, 4))
	return room.resize((128, 128), Image.NEAREST)


# ---- models -----------------------------------------------------------------------------------

def screen(face, toward):
	"""Which way across this face's texture the given side lies."""
	axes = FACE_AXES[face]
	if toward == axes["right"]:
		return "right"
	if toward == OPPOSITE[axes["right"]]:
		return "left"
	if toward == axes["up"]:
		return "up"
	return "down"


def tex(name):
	return f"{MOD_ID}:block/strand/{name}"


def flat(element, textures, turn=0):
	"""One flat element, a texture on each of its two faces, lit alike from either side as vanilla's cross is."""
	return {
		"ambientocclusion": False,
		"textures": {"particle": "minecraft:block/cobweb", **{face: tex(name) for face, name in textures.items()}},
		"elements": [{
			**element,
			"shade_direction_override": "up",
			"faces": {face: {"uv": [0, 0, 16, 16], "texture": f"#{face}", **({"rotation": turn} if turn else {})} for face in textures},
		}],
	}


def corner_element(a, b):
	"""A flat sheet from the face beyond a, three quarters of a block out from b's, to the face beyond
	b, as far out from a's: across the corner at 45 degrees.

	Built standing square to b's axis through the middle of that diagonal, then turned about the
	corner's own axis into place.
	"""
	axes = "xyz"
	ia, ib = axes.index(AXIS_OF[a]), axes.index(AXIS_OF[b])
	ie = 3 - ia - ib
	# How far out along each face it reaches, in sixteenths; its middle is half that from each face.
	reach = 12
	ca = reach / 2 if a in NEGATIVE else 16 - reach / 2
	cb = reach / 2 if b in NEGATIVE else 16 - reach / 2
	half = reach / 2 * math.sqrt(2)
	lo, hi = [0.0] * 3, [0.0] * 3
	lo[ia], hi[ia] = ca - half, ca + half
	lo[ib] = hi[ib] = cb
	lo[ie], hi[ie] = 0, 16
	origin = [0.0] * 3
	origin[ia], origin[ib], origin[ie] = ca, cb, 8
	# Unturned, the sheet runs along a's axis. Turned, its end toward a's face has to move away
	# from b's face; which way that is depends on which way each face lies, and on whether turning
	# a's axis toward b's is positive about the corner's axis under the right-hand rule.
	sa = -1 if a in NEGATIVE else 1
	sb = -1 if b in NEGATIVE else 1
	handed = 1 if (ia, ib, ie) in ((0, 1, 2), (1, 2, 0), (2, 0, 1)) else -1
	angle = -45 * sa * sb * handed
	faces = tuple(f for f in ORDER if AXIS_OF[f] == axes[ib])
	# A face's texture runs left to right along x, except on the faces square to x, where it runs along z.
	across = "z" if axes[ib] == "x" else "x"
	turn = 0 if across == axes[ie] else 90
	return {
		"from": [round(v, 4) for v in lo],
		"to": [round(v, 4) for v in hi],
		"rotation": {"origin": origin, "axis": axes[ie], "angle": angle},
	}, faces, turn


def hug_model(side, element, faces, suffix):
	"""A web held on one side only: a flat web against that face, and over it a low mound of silk,
	two small sheets crossed and stood out from the face, so it has some body to it."""
	model = flat(element, {f: f"hug{suffix}" for f in faces})
	normal = AXIS_OF[side]
	tangents = [axis for axis in "xyz" if axis != normal]
	near = 0 if side in NEGATIVE else 16
	out = 6 if side in NEGATIVE else -6
	for along, across in (tangents, tangents[::-1]):
		lo, hi = {}, {}
		lo[normal], hi[normal] = sorted((near, near + out))
		lo[along], hi[along] = 1, 15
		lo[across] = hi[across] = 8
		model["elements"].append({
			"from": [lo[a] for a in "xyz"],
			"to": [hi[a] for a in "xyz"],
			"rotation": {"origin": [8, 8, 8], "axis": normal, "angle": 45},
			"shade_direction_override": "up",
			"faces": {f: {"uv": [0, 0, 16, 16], "texture": "#clump"} for f in ORDER if AXIS_OF[f] == across},
		})
	model["textures"]["clump"] = tex(f"clump{suffix}")
	return model


# Which faces show an orb's canvas mirrored: left to right on the canvas is east, or south facing x;
# its top is up, or north facing y.
ORB_FLIP = {"east": (True, False), "west": (False, False), "north": (True, False), "south": (False, False),
	"up": (False, False), "down": (False, True)}


def orb_model(element, faces, name):
	model = flat(element, {f: name for f in faces})
	for face in faces:
		flip_h, flip_v = ORB_FLIP[face]
		x1, x2 = (16, 0) if flip_h else (0, 16)
		y1, y2 = (16, 0) if flip_v else (0, 16)
		model["elements"][0]["faces"][face]["uv"] = [x1, y1, x2, y2]
	return model


# ---- animations -------------------------------------------------------------------------------

def track(target, keys):
	"""One channel of a bone: (time in seconds, [x, y, z]) pairs, eased between."""
	return {"target": target, "keyframes": [{"time": t, "value": v, "interpolation": "catmullrom"} for t, v in keys]}


def mirrored(keys):
	"""The same movement for the leg on the other side: a leg on the left lifts the other way about z."""
	return [(t, [x, -y, -z]) for t, (x, y, z) in keys]


def throw_animation():
	"""Rearing back to throw: the front legs lift high and the abdomen tips up over half a second,
	then the abdomen flicks forward as the web leaves at half a second, and everything settles.
	Degrees, layered over the spider's own pose; a right leg lifts with z turned positive."""
	front = [(0.0, [0, 0, 0]), (0.35, [-20, 0, 85]), (0.5, [-25, 0, 95]), (0.6, [10, 0, 15]), (0.8, [0, 0, 0])]
	middle = [(0.0, [0, 0, 0]), (0.35, [0, 0, 40]), (0.5, [0, 0, 45]), (0.7, [0, 0, 0]), (0.8, [0, 0, 0])]
	return {
		"length": 0.8,
		"looping": False,
		"bones": {
			"right_front_leg": [track("rotation", front)],
			"left_front_leg": [track("rotation", mirrored(front))],
			"right_middle_front_leg": [track("rotation", middle)],
			"left_middle_front_leg": [track("rotation", mirrored(middle))],
			"body1": [track("rotation", [(0.0, [0, 0, 0]), (0.45, [52, 0, 0]), (0.55, [-28, 0, 0]), (0.8, [0, 0, 0])])],
			"body0": [track("rotation", [(0.0, [0, 0, 0]), (0.45, [-10, 0, 0]), (0.55, [8, 0, 0]), (0.8, [0, 0, 0])])],
			"head": [track("rotation", [(0.0, [0, 0, 0]), (0.45, [-18, 0, 0]), (0.55, [12, 0, 0]), (0.8, [0, 0, 0])])],
		},
	}


def spin_animation():
	"""Spinning: the abdomen tipped up to draw silk, swaying side to side, the front legs working it
	in turn. Loops for as long as the spider spins."""
	front = [(0.0, [0, 0, 18]), (0.2, [0, 0, 42]), (0.4, [0, 0, 18]), (0.6, [0, 0, 42]), (0.8, [0, 0, 18])]
	other = [(0.0, [0, 0, -42]), (0.2, [0, 0, -18]), (0.4, [0, 0, -42]), (0.6, [0, 0, -18]), (0.8, [0, 0, -42])]
	middle = [(0.0, [0, 0, 0]), (0.4, [0, 0, 18]), (0.8, [0, 0, 0])]
	return {
		"length": 0.8,
		"looping": True,
		"bones": {
			"right_front_leg": [track("rotation", front)],
			"left_front_leg": [track("rotation", other)],
			"right_middle_front_leg": [track("rotation", middle)],
			"left_middle_front_leg": [track("rotation", mirrored(middle))],
			"body1": [track("rotation", [(0.0, [22, -14, 0]), (0.4, [22, 14, 0]), (0.8, [22, -14, 0])])],
		},
	}


def main():
	for generated in (ASSETS / "minecraft", ASSETS / MOD_ID / "textures", ASSETS / MOD_ID / "animations"):
		shutil.rmtree(generated, ignore_errors=True)

	textures = ASSETS / MOD_ID / "textures/block/strand"
	rng = random.Random(1993)

	def variants(name, draw):
		for n in range(1, VARIANTS + 1):
			save(draw(), textures / (f"{name}.png" if n == 1 else f"{name}_{n}.png"))

	# Every set of two or more sides of a texture, by name in a fixed order; hanging only where the
	# top holds and the bottom does not.
	screen_sides = ("up", "down", "left", "right")
	sets = [tuple(side for i, side in enumerate(screen_sides) if mask & 1 << i) for mask in range(16) if bin(mask).count("1") >= 2]
	for held in sets:
		variants("sheet_" + "_".join(held), lambda: sheet(held, rng))
		if "up" in held and "down" not in held:
			variants("sheet_" + "_".join(held) + "_hanging", lambda: sheet(held, rng, hanging=True))
	variants("tuft", lambda: tuft(rng))
	variants("hug", lambda: hug(rng))
	variants("clump", lambda: clump(rng))
	variants("corner", lambda: corner_strip(rng))

	models = ASSETS / "minecraft/models/block"

	def model(part, build):
		for n in range(1, VARIANTS + 1):
			suffix = "" if n == 1 else f"_{n}"
			write_json(models / f"cobweb_strand_{part}{suffix}.json", build(suffix))

	def screen_set(face, world):
		named = {screen(face, side) for side in world}
		return "_".join(side for side in screen_sides if side in named)

	for axis, (element, faces) in SHEETS.items():
		around = [side for side in ORDER if AXIS_OF[side] != axis]
		for mask in range(16):
			world = [side for i, side in enumerate(around) if mask & 1 << i]
			if len(world) < 2:
				continue
			name = f"sheet_{axis}_" + "_".join(world)
			model(name, lambda s: flat(element, {f: f"sheet_{screen_set(f, world)}{s}" for f in faces}))
			if axis != "y" and "up" in world and "down" not in world:
				model(name + "_hanging", lambda s: flat(element, {f: f"sheet_{screen_set(f, world)}_hanging{s}" for f in faces}))

	for i, a in enumerate(ORDER):
		for b in ORDER[i + 1:]:
			if AXIS_OF[a] == AXIS_OF[b]:
				continue
			corner, corner_faces, turn = corner_element(a, b)
			model(f"corner_{a}_{b}", lambda s: flat(corner, {f: f"corner{s}" for f in corner_faces}, turn))

	for axis in ("x", "z"):
		element, faces = SHEETS[axis]
		model(f"tuft_{axis}", lambda s: flat(element, {f: f"tuft{s}" for f in faces}))
	for side, (element, faces) in HUGS.items():
		model(f"hug_{side}", lambda s: hug_model(side, element, faces, s))

	# Orbs: a canvas per size and variant, cut into a tile per block. The tile a block takes depends on
	# where it is in the rectangle and which way its face looks, so the back of an orb shows the same
	# web from behind rather than a second one.
	for variant in range(1, ORB_VARIANTS + 1):
		suffix = "" if variant == 1 else f"_{variant}"
		for width in range(1, ORB_MOST + 1):
			for height in range(1, ORB_MOST + 1):
				canvas = orb_canvas(width, height, rng)
				for cx in range(width):
					for cy in range(height):
						tile = canvas.crop((cx * 16, cy * 16, cx * 16 + 16, cy * 16 + 16))
						save(tile, textures / f"orb_{width}x{height}_{cx}_{cy}{suffix}.png")
				for axis, (element, faces) in SHEETS.items():
					for across in range(width):
						for up in range(height):
							cy = up if axis == "y" else height - 1 - up
							name = f"orb_{width}x{height}_{across}_{cy}{suffix}"
							write_json(models / f"cobweb_strand_orb_{axis}_{width}x{height}_{across}_{up}{suffix}.json", orb_model(element, faces, name))

	write_json(ASSETS / MOD_ID / "animations/web_throw.json", throw_animation())
	write_json(ASSETS / MOD_ID / "animations/web_spin.json", spin_animation())

	save(icon(), ASSETS / MOD_ID / "icon.png")


if __name__ == "__main__":
	main()

#version 150

// Pass 1 of the exact separable distance transform: for every pixel the signed horizontal offset to the
// nearest mask seed on the same row, searched up to Radius pixels. With UseDepth, a mask pixel is a seed only
// where the item is visible (OutlineDepth.visible): item pixels hidden behind the scene are no seeds.

uniform sampler2D MaskSampler;
uniform sampler2D DepthSampler;
uniform int Radius;
uniform vec4 Region; // x0, y0, x1, y1 inclusive, framebuffer pixels (GL origin bottom-left)
uniform int UseDepth;
uniform float SeedEpsilon;

out vec4 fragColor;

float unpackDepth(vec3 gba) {
	ivec3 b = ivec3(gba * 255.0 + 0.5);
	return float((b.x << 16) | (b.y << 8) | b.z) / 16777215.0;
}

// Style id of the seed at q, or 0.0 if q is no seed.
float seedId(ivec2 q) {
	vec4 m = texelFetch(MaskSampler, q, 0);
	if (m.r > 0.0 && UseDepth == 1 && texelFetch(DepthSampler, q, 0).r < unpackDepth(m.gba) - SeedEpsilon) {
		return 0.0; // the scene is in front of the item here
	}
	return m.r;
}

void main() {
	ivec2 p = ivec2(gl_FragCoord.xy);
	int x0 = int(Region.x);
	int x1 = int(Region.z);
	for (int i = 0; i <= Radius; i++) {
		int xr = p.x + i;
		if (xr <= x1) {
			float id = seedId(ivec2(xr, p.y));
			if (id > 0.0) {
				fragColor = vec4((float(i) + 128.0) / 255.0, id, 0.0, 1.0);
				return;
			}
		}
		int xl = p.x - i;
		if (i > 0 && xl >= x0) {
			float id = seedId(ivec2(xl, p.y));
			if (id > 0.0) {
				fragColor = vec4((128.0 - float(i)) / 255.0, id, 0.0, 1.0);
				return;
			}
		}
	}
	// A = 0: no seed within Radius on this row.
	fragColor = vec4(0.0);
}

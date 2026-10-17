#version 150

// Pass 2: finishes the exact Euclidean distance transform vertically, looks up the style of the nearest
// seed, hides the outline where scene geometry is in front of the item and blends it onto the target.

uniform sampler2D MaskSampler;
uniform sampler2D EdtSampler;
uniform sampler2D DepthSampler;
uniform sampler2D StyleSampler;
uniform int Radius;
uniform vec4 Region;
uniform int UseDepth;
uniform vec2 NearFar;
uniform float DepthTolerance;
uniform vec2 DistanceFade; // (start, end) in blocks; end <= start disables

out vec4 fragColor;

float linearDepth(float z) {
	float n = NearFar.x;
	float f = NearFar.y;
	return (2.0 * n * f) / (f + n - (2.0 * z - 1.0) * (f - n));
}

void main() {
	ivec2 p = ivec2(gl_FragCoord.xy);
	if (texelFetch(MaskSampler, p, 0).r > 0.0) {
		discard; // inside a silhouette: no outline
	}
	float best = 1.0e9;
	float bestId = 0.0;
	ivec2 seed = p;
	int y0 = int(Region.y);
	int y1 = int(Region.w);
	for (int j = 0; j <= Radius; j++) {
		if (float(j * j) >= best) {
			break; // no closer seed possible
		}
		for (int s = -1; s <= 1; s += 2) {
			if (j == 0 && s > 0) {
				continue;
			}
			int y = p.y + s * j;
			if (y < y0 || y > y1) {
				continue; // outside the region the data is stale
			}
			vec4 h = texelFetch(EdtSampler, ivec2(p.x, y), 0);
			if (h.a < 0.5) {
				continue;
			}
			float dx = h.r * 255.0 - 128.0;
			float d2 = dx * dx + float(j * j);
			if (d2 < best) {
				best = d2;
				bestId = h.g;
				seed = ivec2(p.x + int(round(dx)), y);
			}
		}
	}
	if (bestId <= 0.0) {
		discard;
	}
	int idx = int(bestId * 255.0 + 0.5);
	vec4 col = texelFetch(StyleSampler, ivec2(idx, 0), 0);
	vec4 par = texelFetch(StyleSampler, ivec2(idx, 1), 0);
	float thickness = par.r * 255.0 / 8.0;
	float softness = par.g;
	float d = sqrt(best) - 0.5; // distance to the edge of the seed pixel
	float inner = thickness * (1.0 - softness);
	float outer = thickness * (1.0 + softness) + 0.5;
	float a = (1.0 - smoothstep(inner, outer, d)) * col.a;
	if (UseDepth == 1) {
		float zp = linearDepth(texelFetch(DepthSampler, p, 0).r);
		float zs = linearDepth(texelFetch(DepthSampler, seed, 0).r);
		if (zp < zs - max(DepthTolerance, zs * 0.02)) {
			discard; // something is in front of the item here
		}
		if (DistanceFade.y > DistanceFade.x) {
			a *= 1.0 - smoothstep(DistanceFade.x, DistanceFade.y, zs);
		}
	}
	if (a <= 0.003) {
		discard;
	}
	fragColor = vec4(col.rgb, a);
}

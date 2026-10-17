#version 150

// Pass 1 of the exact separable distance transform: for every pixel the signed horizontal offset to the
// nearest mask seed on the same row, searched up to Radius pixels.

uniform sampler2D MaskSampler;
uniform int Radius;
uniform vec4 Region; // x0, y0, x1, y1 inclusive, framebuffer pixels (GL origin bottom-left)

out vec4 fragColor;

void main() {
	ivec2 p = ivec2(gl_FragCoord.xy);
	int x0 = int(Region.x);
	int x1 = int(Region.z);
	for (int i = 0; i <= Radius; i++) {
		int xr = p.x + i;
		if (xr <= x1) {
			float id = texelFetch(MaskSampler, ivec2(xr, p.y), 0).r;
			if (id > 0.0) {
				fragColor = vec4((float(i) + 128.0) / 255.0, id, 0.0, 1.0);
				return;
			}
		}
		int xl = p.x - i;
		if (i > 0 && xl >= x0) {
			float id = texelFetch(MaskSampler, ivec2(xl, p.y), 0).r;
			if (id > 0.0) {
				fragColor = vec4((128.0 - float(i)) / 255.0, id, 0.0, 1.0);
				return;
			}
		}
	}
	// A = 0: no seed within Radius on this row.
	fragColor = vec4(0.0);
}

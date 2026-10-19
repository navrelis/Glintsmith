#version 150

uniform sampler2D Sampler0;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
	// Vanilla cutout threshold: only the item's own silhouette becomes mask, visible or hidden by the scene.
	if (texture(Sampler0, texCoord0).a < 0.1) {
		discard;
	}
	// R = style index / 255, written by the mask vertex consumer. G, B, A = window depth of this item surface,
	// 24 bits, most significant first (OutlineDepth.pack). The depth test against the mask's own depth keeps the
	// nearest item surface; the passes compare it with the scene depth to tell visible from hidden item pixels.
	uint z = min(uint(clamp(gl_FragCoord.z, 0.0, 1.0) * 16777215.0 + 0.5), 16777215u); // 1.0 must not wrap to 0
	fragColor = vec4(vertexColor.r, float((z >> 16u) & 255u) / 255.0, float((z >> 8u) & 255u) / 255.0,
		float(z & 255u) / 255.0);
}

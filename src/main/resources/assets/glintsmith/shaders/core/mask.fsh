#version 150

uniform sampler2D Sampler0;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
	// Vanilla cutout threshold: only the visible silhouette of the item becomes a seed.
	if (texture(Sampler0, texCoord0).a < 0.1) {
		discard;
	}
	// R = style index / 255, written by the mask vertex consumer.
	fragColor = vertexColor;
}

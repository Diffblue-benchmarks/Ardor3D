#version 330 core

layout (location = 0) in vec3 vertex;
layout (location = 1) in vec3 normal;
layout (location = 2) in vec4 color;
layout (location = 3) in vec2 uv0;

uniform mat4 model;
uniform mat4 viewProjection;

out vec4 DiffuseColor;
out vec2 TexCoord0;

void main()
{
    gl_Position = viewProjection * model * vec4(vertex, 1.0);
    DiffuseColor = color;
    TexCoord0 = uv0;
}

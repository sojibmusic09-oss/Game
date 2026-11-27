package com.example.graphics3d

import android.opengl.GLES30
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

object ShaderUtils {
    private const val TAG = "BdasShader"

    const val VERTEX_SHADER = """#version 300 es
        uniform mat4 uMvpMatrix;
        uniform mat4 uModelMatrix;
        layout(location = 0) in vec3 aPosition;
        layout(location = 1) in vec3 aNormal;
        out vec3 vNormal;
        out vec3 vWorldPos;
        void main() {
            vec4 worldPos = uModelMatrix * vec4(aPosition, 1.0);
            vWorldPos = worldPos.xyz;
            vNormal = normalize(mat3(uModelMatrix) * aNormal);
            gl_Position = uMvpMatrix * vec4(aPosition, 1.0);
        }
    """

    const val FRAGMENT_SHADER = """#version 300 es
        precision mediump float;
        uniform vec4 uColor;
        uniform vec3 uSunDir;
        uniform float uAmbient;
        uniform vec3 uFogColor;
        uniform float uFogDensity;
        uniform vec3 uCameraPos;
        uniform float uEmissive;
        in vec3 vNormal;
        in vec3 vWorldPos;
        out vec4 fragColor;
        void main() {
            vec3 n = normalize(vNormal);
            float diff = max(dot(n, normalize(uSunDir)), 0.0);
            float light = clamp(uAmbient + diff * 0.72 + uEmissive, 0.12, 1.4);
            vec3 litColor = uColor.rgb * light;
            float dist = length(vWorldPos - uCameraPos);
            float fogFactor = clamp(1.0 - exp(-dist * uFogDensity), 0.0, 0.92);
            vec3 finalRgb = mix(litColor, uFogColor, fogFactor);
            fragColor = vec4(finalRgb, uColor.a);
        }
    """

    fun compileProgram(vertexSrc: String = VERTEX_SHADER, fragmentSrc: String = FRAGMENT_SHADER): Int {
        val vs = compileShader(GLES30.GL_VERTEX_SHADER, vertexSrc)
        val fs = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentSrc)
        val program = GLES30.glCreateProgram()
        GLES30.glAttachShader(program, vs)
        GLES30.glAttachShader(program, fs)
        GLES30.glLinkProgram(program)
        val status = IntArray(1)
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, status, 0)
        if (status[0] == 0) {
            Log.e(TAG, "Program link error: ${GLES30.glGetProgramInfoLog(program)}")
        }
        GLES30.glDeleteShader(vs)
        GLES30.glDeleteShader(fs)
        return program
    }

    private fun compileShader(type: Int, src: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, src)
        GLES30.glCompileShader(shader)
        val status = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            Log.e(TAG, "Shader compile error: ${GLES30.glGetShaderInfoLog(shader)}")
        }
        return shader
    }

    fun toFloatBuffer(data: FloatArray): FloatBuffer =
        ByteBuffer.allocateDirect(data.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(data)
                position(0)
            }
}

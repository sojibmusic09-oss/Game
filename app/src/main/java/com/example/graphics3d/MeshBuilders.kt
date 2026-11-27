package com.example.graphics3d

import android.opengl.GLES30
import java.nio.FloatBuffer
import kotlin.math.cos
import kotlin.math.sin

class GlMesh(verticesAndNormals: FloatArray) {
    val vertexCount: Int = verticesAndNormals.size / 6
    private val buffer: FloatBuffer = ShaderUtils.toFloatBuffer(verticesAndNormals)
    private var vboId: Int = 0

    fun uploadIfNeeded() {
        if (vboId != 0) return
        val vbos = IntArray(1)
        GLES30.glGenBuffers(1, vbos, 0)
        vboId = vbos[0]
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId)
        GLES30.glBufferData(
            GLES30.GL_ARRAY_BUFFER,
            buffer.capacity() * 4,
            buffer,
            GLES30.GL_STATIC_DRAW
        )
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
    }

    fun draw() {
        uploadIfNeeded()
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboId)
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 24, 0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, 24, 12)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, vertexCount)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
    }
}

object MeshBuilders {
    fun buildBox(): GlMesh {
        val data = ArrayList<Float>(36 * 6)
        fun quad(
            x0: Float, y0: Float, z0: Float,
            x1: Float, y1: Float, z1: Float,
            x2: Float, y2: Float, z2: Float,
            x3: Float, y3: Float, z3: Float,
            nx: Float, ny: Float, nz: Float
        ) {
            fun v(x: Float, y: Float, z: Float) {
                data.add(x); data.add(y); data.add(z)
                data.add(nx); data.add(ny); data.add(nz)
            }
            v(x0, y0, z0); v(x1, y1, z1); v(x2, y2, z2)
            v(x0, y0, z0); v(x2, y2, z2); v(x3, y3, z3)
        }
        // Front (+Z), Back (-Z), Top (+Y), Bottom (-Y), Right (+X), Left (-X)
        quad(-0.5f, -0.5f, 0.5f, 0.5f, -0.5f, 0.5f, 0.5f, 0.5f, 0.5f, -0.5f, 0.5f, 0.5f, 0f, 0f, 1f)
        quad(0.5f, -0.5f, -0.5f, -0.5f, -0.5f, -0.5f, -0.5f, 0.5f, -0.5f, 0.5f, 0.5f, -0.5f, 0f, 0f, -1f)
        quad(-0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, -0.5f, -0.5f, 0.5f, -0.5f, 0f, 1f, 0f)
        quad(-0.5f, -0.5f, -0.5f, 0.5f, -0.5f, -0.5f, 0.5f, -0.5f, 0.5f, -0.5f, -0.5f, 0.5f, 0f, -1f, 0f)
        quad(0.5f, -0.5f, 0.5f, 0.5f, -0.5f, -0.5f, 0.5f, 0.5f, -0.5f, 0.5f, 0.5f, 0.5f, 1f, 0f, 0f)
        quad(-0.5f, -0.5f, -0.5f, -0.5f, -0.5f, 0.5f, -0.5f, 0.5f, 0.5f, -0.5f, 0.5f, -0.5f, -1f, 0f, 0f)
        return GlMesh(data.toFloatArray())
    }

    fun buildCylinderZ(segments: Int = 12): GlMesh {
        val data = ArrayList<Float>()
        fun addVert(x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float) {
            data.add(x); data.add(y); data.add(z)
            data.add(nx); data.add(ny); data.add(nz)
        }
        for (i in 0 until segments) {
            val a0 = (2.0 * Math.PI * i / segments).toFloat()
            val a1 = (2.0 * Math.PI * (i + 1) / segments).toFloat()
            val x0 = cos(a0) * 0.5f
            val y0 = sin(a0) * 0.5f
            val x1 = cos(a1) * 0.5f
            val y1 = sin(a1) * 0.5f
            // Side
            addVert(x0, y0, -0.5f, cos(a0), sin(a0), 0f)
            addVert(x1, y1, -0.5f, cos(a1), sin(a1), 0f)
            addVert(x1, y1, 0.5f, cos(a1), sin(a1), 0f)
            addVert(x0, y0, -0.5f, cos(a0), sin(a0), 0f)
            addVert(x1, y1, 0.5f, cos(a1), sin(a1), 0f)
            addVert(x0, y0, 0.5f, cos(a0), sin(a0), 0f)
            // Nose cap (+Z)
            addVert(0f, 0f, 0.68f, 0f, 0f, 1f)
            addVert(x0, y0, 0.5f, cos(a0) * 0.5f, sin(a0) * 0.5f, 0.86f)
            addVert(x1, y1, 0.5f, cos(a1) * 0.5f, sin(a1) * 0.5f, 0.86f)
            // Tail cap (-Z)
            addVert(0f, 0f, -0.62f, 0f, 0f, -1f)
            addVert(x1, y1, -0.5f, cos(a1) * 0.5f, sin(a1) * 0.5f, -0.86f)
            addVert(x0, y0, -0.5f, cos(a0) * 0.5f, sin(a0) * 0.5f, -0.86f)
        }
        return GlMesh(data.toFloatArray())
    }
}

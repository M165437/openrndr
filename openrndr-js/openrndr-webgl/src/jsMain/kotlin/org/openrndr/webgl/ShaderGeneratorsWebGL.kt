package org.openrndr.webgl

import org.openrndr.draw.ShadeStructure
import org.openrndr.draw.ShadeStyleGLSL
import org.openrndr.draw.ShadeStyleGLSL.Companion.drawerUniforms
import org.openrndr.draw.ShadeStyleGLSL.Companion.fragmentMainConstants
import org.openrndr.draw.ShadeStyleGLSL.Companion.primitiveTypes
import org.openrndr.draw.ShadeStyleGLSL.Companion.transformVaryingIn
import org.openrndr.internal.ShaderGenerators

private val rotate2 = """mat2 rotate2(float rotationInDegrees) {
    float r = radians(rotationInDegrees);
    float cr = cos(r);
    float sr = sin(r);
    return mat2(vec2(cr, sr), vec2(-sr, cr));
}
""".trimIndent()

class ShaderGeneratorsWebGL : ShaderGenerators {
    override fun vertexBufferFragmentShader(shadeStructure: ShadeStructure): String = """#version 300 es
        |precision highp float;
        |${primitiveTypes("d_vertex_buffer")}
        |${shadeStructure.buffers ?: ""}
        |${shadeStructure.uniforms ?: ""}
        |out vec4 o_color;
        |uniform sampler2D image;
        |${drawerUniforms()}
        |${shadeStructure.varyingIn ?: ""}
        |${shadeStructure.outputs ?: ""}
        |${transformVaryingIn}

        |${shadeStructure.fragmentPreamble ?: ""}
        |void main(void) {
        |   const int v_instance = 0;
        |    ${fragmentMainConstants(element = "0", instance = "0")}
        |    vec4 x_fill = u_fill;
        |    vec4 x_stroke = u_stroke;
        |    {
        |       ${shadeStructure.fragmentTransform ?: ""}
        |    }
        ${if (!shadeStructure.suppressDefaultOutput) """
            |    o_color = x_fill;
            |    o_color.rgb *= o_color.a;""".trimMargin() else ""}
|}""".trimMargin()

    override fun vertexBufferVertexShader(shadeStructure: ShadeStructure): String = """#version 300 es
        |precision highp float;
        |${primitiveTypes("d_vertex_buffer")}
        |${shadeStructure.buffers ?: ""}
        |${drawerUniforms()}
        |${shadeStructure.attributes ?: ""}
        |${shadeStructure.uniforms ?: ""}
        |${shadeStructure.varyingOut ?: ""}
        |${ShadeStyleGLSL.transformVaryingOut}
        |${shadeStructure.vertexPreamble ?: ""}
        |void main() {
        |${ShadeStyleGLSL.vertexMainConstants(instance = "0")}
        |${shadeStructure.varyingBridge ?: ""}
        |vec3 x_normal = vec3(0.0, 0.0, 0.0);
        |${if (shadeStructure.attributes?.contains("vec3 a_normal;") == true) "x_normal = a_normal;" else ""}
        |vec3 x_position = a_position;
        |${ShadeStyleGLSL.preVertexTransform}
        |{
        |   ${shadeStructure.vertexTransform ?: ""}
        |}
        |${ShadeStyleGLSL.postVertexTransform}
        |gl_Position = v_clipPosition;
        |}""".trimMargin()

    override fun imageFragmentShader(shadeStructure: ShadeStructure): String = """#version 300 es
        |precision highp float;
        |${primitiveTypes("d_image")}
        |${shadeStructure.buffers ?: ""}
        |${shadeStructure.uniforms ?: ""}
        |//layout(origin_upper_left) in vec4 gl_FragCoord;
        |uniform sampler2D image;
        |out vec4 o_color;
        |${drawerUniforms()}
        |${shadeStructure.varyingIn ?: ""}
        |${transformVaryingIn}
        |${shadeStructure.outputs ?: ""}
        |${shadeStructure.fragmentPreamble ?: ""}
        |in vec3 v_boundsPosition;
        |
        |vec4 colorTransform(vec4 color, float[25] matrix) {
        |   float r = color.r * matrix[0] + color.g * matrix[5] + color.b * matrix[10] + color.a * matrix[15] + matrix[20];
        |   float g = color.r * matrix[1] + color.g * matrix[6] + color.b * matrix[11] + color.a * matrix[16] + matrix[21];
        |   float b = color.r * matrix[2] + color.g * matrix[7] + color.b * matrix[12] + color.a * matrix[17] + matrix[22];
        |   float a = color.r * matrix[3] + color.g * matrix[8] + color.b * matrix[13] + color.a * matrix[18] + matrix[23];
        |   return vec4(r, g, b, a);
        |}
        |void main(void) {
        |   ${fragmentMainConstants(boundsPosition = "v_boundsPosition", instance = "0")}
        |   vec4 x_fill = texture(image, va_texCoord0);
        |   vec4 x_stroke = u_stroke;
        |   {
        |       ${shadeStructure.fragmentTransform ?: ""}
        |   }
        |   float div = x_fill.a != 0.0 ? x_fill.a : 1.0;
        |   x_fill.rgb /= div;
        |   //x_fill = colorTransform(x_fill, u_colorMatrix);
        |   x_fill.rgb *= x_fill.a;
        |   o_color = x_fill;
        |}""".trimMargin()

    override fun imageVertexShader(shadeStructure: ShadeStructure): String = """#version 300 es
        |precision highp float;
        |${primitiveTypes("d_image")}
        |${shadeStructure.buffers ?: ""}
        |${drawerUniforms()}
        |uniform int u_flipV;
        |${shadeStructure.attributes ?: ""}
        |${shadeStructure.uniforms ?: ""}
        |${shadeStructure.varyingOut ?: ""}
        |${ShadeStyleGLSL.transformVaryingOut}
        |${shadeStructure.vertexPreamble ?: ""}
        |out vec3 v_boundsPosition;
        |void main() {
        |   int v_instance = 0;
        |   ${ShadeStyleGLSL.vertexMainConstants(instance = "0")}
        |   ${shadeStructure.varyingBridge ?: ""}
        |   ${ShadeStyleGLSL.preVertexTransform}
        |   vec3 x_normal = a_normal;
        |   vec3 x_position = a_position;
        |   x_position.xy = a_position.xy * i_target.zw + i_target.xy;
        |   v_boundsPosition = vec3(a_texCoord0.xy, 1.0);
        |   va_texCoord0.xy = a_texCoord0.xy * i_source.zw + i_source.xy;
        |   if (u_flipV == 0) {
        |       va_texCoord0.y = 1.0 - va_texCoord0.y;
        |   }
        |   {
        |       ${shadeStructure.vertexTransform ?: ""}
        |   }
        |${ShadeStyleGLSL.postVertexTransform}
        |gl_Position = v_clipPosition;
        |}""".trimMargin()

    override fun imageArrayTextureFragmentShader(shadeStructure: ShadeStructure): String {
        error("not supported")
    }

    override fun imageArrayTextureVertexShader(shadeStructure: ShadeStructure): String {
        error("not supported")
    }

    override fun pointFragmentShader(shadeStructure: ShadeStructure): String = """#version 300 es
        |precision highp float;
        |${primitiveTypes("d_circle")}
        |${shadeStructure.buffers ?: ""}
        |${shadeStructure.uniforms ?: ""}
        |out vec4 o_color;
        |${drawerUniforms(styleBlock = false)}
        |${shadeStructure.varyingIn ?: ""}
        |${transformVaryingIn}

        |${shadeStructure.fragmentPreamble ?: ""}
        |in vec3 v_boundsSize;
        |void main(void) {
            ${
                fragmentMainConstants(boundsPosition = "vec3(0.0, 0.0, 0.0)",
                    boundsSize = "v_boundsSize")
            }

        |   vec4 x_fill = vi_fill;
        |   vec4 x_stroke = vi_stroke;
        |   {
        |       ${shadeStructure.fragmentTransform ?: ""}
        |   }
        |   x_fill.rgb *= x_fill.a;
        |   o_color = x_fill;
        |}""".trimMargin()

    override fun pointVertexShader(shadeStructure: ShadeStructure): String = """
${primitiveTypes("d_point")}
${shadeStructure.buffers ?: ""}
${drawerUniforms(styleBlock = false)}
${shadeStructure.attributes ?: ""}
${shadeStructure.uniforms ?: ""}
${shadeStructure.varyingOut ?: ""}
${ShadeStyleGLSL.transformVaryingOut}

${shadeStructure.vertexPreamble ?: ""}

//flat out int v_instance;
out vec3 v_boundsSize;
void main() {
    //v_instance = 0;
    ${ShadeStyleGLSL.vertexMainConstants()}
    ${shadeStructure.varyingBridge ?: ""}

    v_boundsSize = vec3(0, 0.0, 0.0);
    ${ShadeStyleGLSL.preVertexTransform}
    vec3 x_normal = vec3(0.0, 0.0, 1.0);
    vec3 x_position = a_position  + i_offset;
    {
        ${shadeStructure.vertexTransform ?: ""}
    }
    va_position = x_position;
    ${ShadeStyleGLSL.postVertexTransform}
    gl_Position = v_clipPosition;
}|        
        """.trimMargin()

    override fun circleFragmentShader(shadeStructure: ShadeStructure): String = """#version 300 es        
    precision highp float;
${primitiveTypes("d_circle")}
${shadeStructure.uniforms ?: ""}
${shadeStructure.buffers ?: ""}
//layout(origin_upper_left) in vec4 gl_FragCoord;

${drawerUniforms(styleBlock = false, contextBlock = true)}
${shadeStructure.varyingIn ?: ""}
${transformVaryingIn}


${shadeStructure.fragmentPreamble ?: ""}
in vec3 v_boundsSize;
out vec4 o_color;
void main(void) {
    int v_instance = 0;

    ${
        fragmentMainConstants(boundsPosition = "vec3(va_texCoord0, 0.0)",
            boundsSize = "v_boundsSize")
    }
    float smoothFactor = 3.0;

    vec4 x_fill = vi_fill;
    vec4 x_stroke = vi_stroke;
    float x_strokeWeight = vi_strokeWeight;
    
    {
        ${shadeStructure.fragmentTransform ?: ""}
    }
    float wd = fwidth(length(va_texCoord0 - vec2(0.0)));
    float d = length(va_texCoord0 - vec2(0.5)) * 2.0;

    float or = smoothstep(0.0, wd * smoothFactor, 1.0 - d);
    float b = x_strokeWeight / vi_radius.x;
    float ir = smoothstep(0.0, wd * smoothFactor, 1.0 - b - d);

    vec4 final = vec4(0.0);
    final.rgb =  x_stroke.rgb;
    final.a = or * (1.0 - ir) * x_stroke.a;
    final.rgb *= final.a;

    final.rgb += x_fill.rgb * ir * x_fill.a;
    final.a += ir * x_fill.a;
    o_color = final;
}
"""

    override fun circleVertexShader(shadeStructure: ShadeStructure): String = """#version 300 es        
precision highp float;        
// -- circle vertex shader        
${primitiveTypes("d_circle")}
${shadeStructure.buffers ?: ""}
${drawerUniforms(styleBlock = false)}
${shadeStructure.attributes ?: ""}
${shadeStructure.uniforms ?: ""}
${shadeStructure.varyingOut ?: ""}
${ShadeStyleGLSL.transformVaryingOut}

${shadeStructure.vertexPreamble ?: ""}


out vec3 v_boundsSize;
void main() {
    int v_instance = 0;
    ${ShadeStyleGLSL.vertexMainConstants(instance = "0")}
    ${shadeStructure.varyingBridge ?: ""}

    v_boundsSize = vec3(i_radius.xy, 0.0);
    ${ShadeStyleGLSL.preVertexTransform}
    vec3 x_normal = a_normal;
    vec3 x_position = vec3(a_position.xy * i_radius, 0.0) + i_offset;
    {
        ${shadeStructure.vertexTransform ?: ""}
    }
    va_position = x_position;
    ${ShadeStyleGLSL.postVertexTransform}
    gl_Position = v_clipPosition;
}
    """

    override fun fontImageMapFragmentShader(shadeStructure: ShadeStructure): String {
        TODO("Not yet implemented")
    }

    override fun fontImageMapVertexShader(shadeStructure: ShadeStructure): String {
        TODO("Not yet implemented")
    }

    override fun rectangleFragmentShader(shadeStructure: ShadeStructure): String = """#version 300 es
precision highp float;        
${primitiveTypes("d_rectangle")}
${shadeStructure.buffers ?: ""}
${shadeStructure.uniforms ?: ""}
//layout(origin_upper_left) in vec4 gl_FragCoord;

${drawerUniforms(styleBlock = false)}
${shadeStructure.varyingIn ?: ""}
${shadeStructure.outputs ?: ""}
${transformVaryingIn}


${shadeStructure.fragmentPreamble ?: ""}
//flat in int v_instance;
in vec3 v_boundsSize;
out vec4 o_color;

void main(void) {
    int v_instance = 0;

    ${
        fragmentMainConstants(
            boundsPosition = "vec3(va_texCoord0, 0.0)",
            boundsSize = "v_boundsSize")
    }
    vec4 x_fill = vi_fill;
    vec4 x_stroke = vi_stroke;
    {
        ${shadeStructure.fragmentTransform ?: ""}
    }
    vec2 wd = fwidth(va_texCoord0 - vec2(0.5));
    vec2 d = abs((va_texCoord0 - vec2(0.5)) * 2.0);

    float irx = smoothstep(0.0, wd.x * 2.5, 1.0-d.x - vi_strokeWeight*2.0/vi_dimensions.x);
    float iry = smoothstep(0.0, wd.y * 2.5, 1.0-d.y - vi_strokeWeight*2.0/vi_dimensions.y);
    float ir = irx*iry;

    vec4 final = vec4(1.0);
    final.rgb = x_fill.rgb * x_fill.a;
    final.a = x_fill.a;

    float sa = (1.0-ir) * x_stroke.a;
    final.rgb = final.rgb * (1.0-sa) + x_stroke.rgb * sa;
    final.a = final.a * (1.0-sa) + sa;

    o_color = final;
}
"""

    override fun rectangleVertexShader(shadeStructure: ShadeStructure): String = """#version 300 es
precision highp float;        
        
${primitiveTypes("d_rectangle")}
${shadeStructure.buffers ?: ""}
${drawerUniforms(styleBlock = false)}
${shadeStructure.attributes ?: ""}
${shadeStructure.uniforms ?: ""}
${shadeStructure.varyingOut ?: ""}
${ShadeStyleGLSL.transformVaryingOut}

${shadeStructure.vertexPreamble ?: ""}

//flat out int v_instance;
out vec3 v_boundsSize;
${rotate2}

void main() {
    //v_instance =  gl_InstanceID;
    ${ShadeStyleGLSL.vertexMainConstants(instance = "0")}
    ${shadeStructure.varyingBridge ?: ""}
    ${ShadeStyleGLSL.preVertexTransform}
    vec3 x_normal = vec3(0.0, 0.0, 1.0);
    vec2 rotatedPosition = rotate2(i_rotation) * (( a_position.xy - vec2(0.5) ) * i_dimensions) + vec2(0.5) * i_dimensions;
      
    vec3 x_position = vec3(rotatedPosition, 0.0) + i_offset;
    v_boundsSize = vec3(i_dimensions, 1.0);
    {
        ${shadeStructure.vertexTransform ?: ""}
    }
    ${ShadeStyleGLSL.postVertexTransform}
    gl_Position = v_clipPosition;
    }
    """

    override fun expansionFragmentShader(shadeStructure: ShadeStructure): String = """#version 300 es
precision highp float;        
${primitiveTypes("d_expansion")}
${shadeStructure.buffers ?: ""}
${shadeStructure.uniforms ?: ""}
//layout(origin_upper_left) in vec4 gl_FragCoord;
${drawerUniforms()}
${shadeStructure.varyingIn ?: ""}
${transformVaryingIn}
//flat in int v_instance;
uniform float strokeMult;
uniform float strokeThr;
uniform float strokeFillFactor;
uniform sampler2D tex;
uniform vec4 bounds;

out vec4 o_color;
in vec3 v_objectPosition;
in vec2 v_ftcoord;
//${if (!shadeStructure.suppressDefaultOutput) "out vec4 o_color;" else ""}

${shadeStructure.fragmentPreamble ?: ""}

float strokeMask() {
	//return pow(min(1.0, (1.0-abs(v_ftcoord.x*2.0-1.0)*strokeMult)) * min(1.0, v_ftcoord.y), 1.0);
    return smoothstep(0.0, 1.0, (1.0-abs(v_ftcoord.x*2.0-1.0))*strokeMult) * smoothstep(0.0, 1.0, v_ftcoord.y);
}

void main(void) {
    ${
        fragmentMainConstants(boundsPosition = "vec3(v_objectPosition.xy - bounds.xy, 0.0) / vec3(bounds.zw,1.0)",
            boundsSize = "vec3(bounds.zw, 0.0)",
            contourPosition = "va_vertexOffset",
            instance = "0"
        
        )
    }

	float strokeAlpha = strokeMask();

    vec4 x_stroke = u_stroke;
    vec4 x_fill = u_fill;

    { ${shadeStructure.fragmentTransform ?: ""} }

    vec4 color = mix(x_stroke, x_fill, strokeFillFactor)  * vec4(1, 1, 1, strokeAlpha);
    vec4 result = color;

    if (strokeAlpha < strokeThr) {
	    discard;
	}

    vec4 final = result;
	final = result;
	final.rgb *= final.a;
    ${if (!shadeStructure.suppressDefaultOutput) "o_color = final;" else ""}
}        
    """

    override fun expansionVertexShader(shadeStructure: ShadeStructure): String = """#version 300 es
precision highp float;        
        
${primitiveTypes("d_expansion")}
${shadeStructure.buffers ?: ""}
${drawerUniforms()}
${shadeStructure.uniforms ?: ""}
${shadeStructure.attributes}
${shadeStructure.varyingOut ?: ""}
${ShadeStyleGLSL.transformVaryingOut}

${shadeStructure.vertexPreamble ?: ""}

out vec2 v_ftcoord;
out float v_offset;

out vec3 v_objectPosition;
//flat out int v_instance;
float v_instance;

void main() {
    //v_instance = 0;
    ${ShadeStyleGLSL.vertexMainConstants(instance = "0")}
    ${shadeStructure.varyingBridge ?: ""}
    v_objectPosition = vec3(a_position, 0.0);
    v_ftcoord = a_texCoord0;

    vec3 x_position = vec3(a_position, 0.0);
    vec3 x_normal = vec3(0.0, 0.0, 1.0);
    ${ShadeStyleGLSL.preVertexTransform}
    {
        ${shadeStructure.vertexTransform ?: ""}
    }
    ${ShadeStyleGLSL.postVertexTransform}

    gl_Position = v_clipPosition;
}        
    """.trimIndent()



    override fun fastLineFragmentShader(shadeStructure: ShadeStructure): String {
        TODO("Not yet implemented")
    }

    override fun fastLineVertexShader(shadeStructure: ShadeStructure): String {
        TODO("Not yet implemented")
    }

    override fun meshLineFragmentShader(shadeStructure: ShadeStructure): String {
        TODO("Not yet implemented")
    }

    override fun meshLineVertexShader(shadeStructure: ShadeStructure): String {
        TODO("Not yet implemented")
    }

    override fun filterVertexShader(shadeStructure: ShadeStructure): String = """#version 300 es
        |// -- ShaderGeneratorsGL3.filterVertexShader
        |precision highp float;
        |${shadeStructure.buffers ?: ""}
        |in vec2 a_texCoord0;
        |in vec2 a_position;
        |// -- vertexPreamble
        |${shadeStructure.vertexPreamble ?: ""}
        |uniform vec2 targetSize;
        |uniform vec2 padding;
        |uniform mat4 projectionMatrix;
        |out vec2 v_texCoord0;
        |void main() {
        |   v_texCoord0 = a_texCoord0;
        |   vec2 transformed = a_position * (targetSize - 2*padding) + padding;
        |   gl_Position = projectionMatrix * vec4(transformed, 0.0, 1.0);
        |   ${shadeStructure.vertexTransform ?: ""}
        |}
    """.trimMargin()

    override fun filterFragmentShader(shadeStructure: ShadeStructure): String = """#version 300 es
        |// -- ShaderGeneratorsGL3.filterFragmentShader
        |precision highp float;
        |out vec4 o_color;
        |${shadeStructure.buffers ?: ""}
        |in vec2 v_texCoord0;
        |uniform sampler2D tex0;
        |uniform sampler2D tex1;
        |uniform sampler2D tex2;
        |uniform sampler2D tex3;
        |uniform sampler2D tex4;
        |// -- drawerUniforms
        |${drawerUniforms()}
        |// -- shadeStructure.outputs
        |${shadeStructure.outputs ?: ""}
        |${if (!shadeStructure.suppressDefaultOutput) "out vec4 o_color;" else ""}
        |// -- shadeStructure.uniforms
        |${shadeStructure.uniforms ?: ""}
        |// -- shadeStructure.fragmentPreamble
        |${shadeStructure.fragmentPreamble ?: ""}
        |void main() {
        |   ${fragmentMainConstants(instance = "0", screenPosition = "v_texCoord0")}
        |   vec4 x_fill = texture(tex0, v_texCoord0);
        |   vec4 x_stroke = vec4(0.0);
        |   {
        |       // -- shadeStructure.fragmentTransform
        |       ${shadeStructure.fragmentTransform ?: ""}
        |   }
        |${
        if (!shadeStructure.suppressDefaultOutput) """
            |o_color = x_fill;
            |o_color.rgb *= o_color.a;
            """.trimMargin() else ""
    }
        |}
        |
        
    """.trimMargin()

}
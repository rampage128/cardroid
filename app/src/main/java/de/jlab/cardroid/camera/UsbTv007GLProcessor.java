package de.jlab.cardroid.camera;

import android.opengl.GLES20;
import android.util.Log;

import androidx.annotation.NonNull;

import com.arksine.libusbtv.UsbTvFrame;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;


// This class is heavily inspired in the sample code from libusb007. Credit goes to arksine.
public class UsbTv007GLProcessor {


    final float[] positionVertices = {
            -1.0f, 1.0f,    // Position 0
            -1.0f, -1.0f,   // Position 1
            1.0f, -1.0f,    // Position 2
            1.0f, 1.0f      // Position 3
    };

    final float[] textureVertices = {
            0.0f, 0.0f,     // TexCoord 0
            0.0f, 1.0f,     // TexCoord 1
            1.0f, 1.0f,     // TexCoord 2
            1.0f, 0.0f      // TexCoord 3
    };

    final short[] indices = {0, 1, 2, 0, 2, 3};

    private FloatBuffer texVertexBuf;
    private FloatBuffer posVertexBuf;
    private ShortBuffer indicesBuf;

    private int shaderProgramId;
    private int positionAttr;
    private int textureAttr;
    private int yuvTextureId;

    private int attachedTexId = Integer.MIN_VALUE;



    public UsbTv007GLProcessor() {

        posVertexBuf = ByteBuffer.allocateDirect(positionVertices.length * 4)
               .order(ByteOrder.nativeOrder()).asFloatBuffer();
        posVertexBuf.put(positionVertices).position(0);

        texVertexBuf = ByteBuffer.allocateDirect(textureVertices.length * 4)
               .order(ByteOrder.nativeOrder()).asFloatBuffer();
        texVertexBuf.put(textureVertices).position(0);

        indicesBuf = ByteBuffer.allocateDirect(indices.length * 2)
               .order(ByteOrder.nativeOrder()).asShortBuffer();
        indicesBuf.put(indices).position(0);

    }

    public void attach(int textureId) {
        if (attachedTexId != Integer.MIN_VALUE) {
            detach();
        }
        attachedTexId = textureId;
        // Initialize Shaders
        loadShaders();

        // Get Vertex Shader Attributes
        positionAttr = GLES20.glGetAttribLocation(shaderProgramId, "a_position");
        textureAttr = GLES20.glGetAttribLocation(shaderProgramId, "a_texCoord");

        // Set up the yuv texture
        GLES20.glEnable(GLES20.GL_TEXTURE_2D);
        yuvTextureId = GLES20.glGetUniformLocation(shaderProgramId, "yuv_texture");
        int[] textureNames = new int[1];
        GLES20.glGenTextures(1, textureNames, 0);
        int yuvTextureName = textureNames[0];
        GLES20.glActiveTexture(attachedTexId);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, yuvTextureName);

        // Clear Background
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
    }

    public void detach() {

    }

    public void drawFrame(@NonNull UsbTvFrame frame) {
        // User Shader Program
        GLES20.glUseProgram(shaderProgramId);

        // Set up Vertex Buffers
        posVertexBuf.position(0);
        GLES20.glVertexAttribPointer(positionAttr, 2, GLES20.GL_FLOAT, false, 0,posVertexBuf);
        texVertexBuf.position(0);
        GLES20.glVertexAttribPointer(textureAttr, 2, GLES20.GL_FLOAT, false, 0, texVertexBuf);

        GLES20.glEnableVertexAttribArray(positionAttr);
        GLES20.glEnableVertexAttribArray(textureAttr);

        // Set up YUV texture
        GLES20.glActiveTexture(this.attachedTexId);
        GLES20.glUniform1i(yuvTextureId, 1);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, frame.getWidth()/2,
                frame.getHeight(), 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, frame.getFrameBuf());
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        // Draw
        indicesBuf.position(0);
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, 6, GLES20.GL_UNSIGNED_SHORT, indicesBuf);
    }

    private void loadShaders() {
        // TODO: Load these from raw resources
        // TODO: I have added a mask to the shader (I'm not sure the outcome has changed).
        // The swirling affect still takes place.  Could it be the renderer, or the render code itself?

        //Our vertex shader code; nothing special
        String vertexShader =
                "attribute vec4 a_position;                         \n" +
                        "attribute vec2 a_texCoord;                         \n" +
                        "varying vec2 v_texCoord;                           \n" +

                        "void main(){                                       \n" +
                        "   gl_Position = a_position;                       \n" +
                        "   v_texCoord = a_texCoord;                        \n" +
                        "}                                                  \n";

        //Our fragment shader code; takes Y,U,V values for each pixel and calculates R,G,B colors,
        //Effectively making YUV to RGB conversion
        String fragmentShader =
                "#ifdef GL_ES                                       \n" +
                        "precision highp float;                             \n" +
                        "#endif                                             \n" +

                        "varying vec2 v_texCoord;                           \n" +
                        "uniform sampler2D yuv_texture;                     \n" +

                        "void main (void){                                  \n" +
                        "   float r, g, b, y, u, v;                         \n" +
                        "   float yPosition;                                \n" +
                        "   vec4 yuvPixel;                                  \n"+

                        // Get the Pixel and Y-Value mask
                        "   yuvPixel = texture2D(yuv_texture, v_texCoord);  \n" +

                        // The Y-position is determined by its location in the viewport.
                        // Since FragCoords are centered, floor them to get the zeroed position.
                        // That coordinate mod 2 should give something close to a zero or a 1.
                        "   yPosition = mod(floor(gl_FragCoord.x), 2.0);      \n"+


                        // If the mask is zero (or thereabout), use the 1st y-value.
                        // Otherwise use the 2nd.
                        "   if (yPosition < 0.5) {                         \n"+
                        "       y = yuvPixel.x;                             \n"+
                        "   } else {                                        \n"+
                        "       y = yuvPixel.z;                             \n"+
                        "   }                                               \n"+

                        // U and V components are always the 2nd and 4th positions (not sure why subtracting .5)
                        "   u = yuvPixel.y - 0.5;                           \n" +
                        "   v = yuvPixel.w - 0.5;                           \n" +


                        //The numbers are just YUV to RGB conversion constants
                        "   r = y + 1.13983*v;                              \n" +
                        "   g = y - 0.39465*u - 0.58060*v;                  \n" +
                        "   b = y + 2.03211*u;                              \n" +

                        //We finally set the RGB color of our pixel
                        "   gl_FragColor = vec4(r, g, b, 1.0);              \n" +
                        "}                                                  \n";

        shaderProgramId = loadProgram(vertexShader, fragmentShader);
    }

    /**
     * Creates a shader
     * @param type          The Shader Type, being either vertex or fragment
     * @param shaderString  The String containing the shader source code
     * @return              The shader Id, or 0 if failed
     */
    private static int loadShader(int type, String shaderString) {
        int shaderId;
        int[] compiled = new int[1];

        shaderId = GLES20.glCreateShader(type);
        if (shaderId == 0) {
            Log.e("loadShader", "Error creating shader");
            return 0;
        }

        GLES20.glShaderSource(shaderId, shaderString);
        GLES20.glCompileShader(shaderId);
        GLES20.glGetShaderiv(shaderId, GLES20.GL_COMPILE_STATUS, compiled, 0);

        if (compiled[0] == 0) {
            Log.e("Load shader", "Error Compiling Shader: " + GLES20.glGetShaderInfoLog(shaderId));
            GLES20.glDeleteShader(shaderId);
            return 0;
        }
        return shaderId;
    }

    /**
     * Creates a Shader program.
     *
     * @param vertexString  String containting the Vertex Program Source
     * @param fragmentString    String containing the Fragment Program's source
     * @return  The Shader Program's Object ID
     */
    private static int loadProgram(String vertexString, String fragmentString) {
        int vertexShaderId;
        int fragmentShaderId;
        int programId;
        int[] linked = new int[1];

        vertexShaderId = loadShader(GLES20.GL_VERTEX_SHADER, vertexString);
        if (vertexShaderId == 0) {
            return 0;
        }

        fragmentShaderId = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentString);
        if (fragmentShaderId == 0) {
            return 0;
        }

        programId = GLES20.glCreateProgram();
        if (programId == 0) {
            return 0;
        }

        GLES20.glAttachShader(programId, vertexShaderId);
        GLES20.glAttachShader(programId, fragmentShaderId);
        GLES20.glLinkProgram(programId);
        GLES20.glGetProgramiv(programId, GLES20.GL_LINK_STATUS, linked, 0);

        if (linked[0] == 0) {
            Log.e("loadProgram", "Error Linking Program: " + GLES20.glGetProgramInfoLog(programId).toString());
            GLES20.glDeleteProgram(programId);
            return 0;
        }

        // Don't need Shader Objects anymore, delete them
        GLES20.glDeleteShader(vertexShaderId);
        GLES20.glDeleteShader(fragmentShaderId);

        return programId;
    }
}
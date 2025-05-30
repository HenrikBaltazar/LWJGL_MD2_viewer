import sun.security.provider.MD2;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import static org.lwjgl.opengl.GL11.*;

public class MD2Model {
    public int skinWidth, skinHeight, frameSize, numSkins, numVertices, numTexCoords, numTriangles, numGlCommands;
    public int numFrames, offsetSkins, offsetTexCoords, offsetTriangles, offsetFrames, offsetGlCmds, offsetEnd;
    public MD2Frame[] frames;
    short[][] triangles;
    int currentFrame = 0;
    int nextFrame = 1;
    float interpolacao = 0.0f;
    public short[][] texCoords;

    public void loadMD2(String file) {
        InputStream in;
        DataInputStream din;

        try{
            in = new FileInputStream(file);
            din = new DataInputStream(in);

            int ident = Integer.reverseBytes(din.readInt());
            int version = Integer.reverseBytes(din.readInt());

            if (ident != 844121161 || version != 8) {
                System.out.println("Arquivo não é um MD2 válido.");
                return;
            }

            skinWidth       = Integer.reverseBytes(din.readInt());
            skinHeight      = Integer.reverseBytes(din.readInt());
            frameSize       = Integer.reverseBytes(din.readInt());
            numSkins        = Integer.reverseBytes(din.readInt());
            numVertices     = Integer.reverseBytes(din.readInt());
            numTexCoords    = Integer.reverseBytes(din.readInt());
            numTriangles    = Integer.reverseBytes(din.readInt());
            numGlCommands   = Integer.reverseBytes(din.readInt());
            numFrames       = Integer.reverseBytes(din.readInt());
            offsetSkins     = Integer.reverseBytes(din.readInt());
            offsetTexCoords = Integer.reverseBytes(din.readInt());
            offsetTriangles = Integer.reverseBytes(din.readInt());
            offsetFrames    = Integer.reverseBytes(din.readInt());
            offsetGlCmds    = Integer.reverseBytes(din.readInt());
            offsetEnd       = Integer.reverseBytes(din.readInt());


            din.skip(offsetSkins - 68);
            byte[][] skins = new byte[numSkins][64];
            for (int i = 0; i < numSkins; i++) {
                din.readFully(skins[i]);
            }

            din.skip(offsetTexCoords - offsetSkins - (numSkins * 64));
            texCoords = new short[numTexCoords][2];
            for (int i = 0; i < numTexCoords; i++) {
                texCoords[i][0] = Short.reverseBytes(din.readShort());
                texCoords[i][1] = Short.reverseBytes(din.readShort());
            }

            din.skip(offsetTriangles - offsetTexCoords - (numTexCoords * 4));
            triangles = new short[numTriangles][6];
            for (int i = 0; i < numTriangles; i++) {
                for (int j = 0; j < 3; j++) {
                    triangles[i][j] = Short.reverseBytes(din.readShort());
                }
                for (int j = 3; j < 6; j++) {
                    triangles[i][j] = Short.reverseBytes(din.readShort());
                }
            }

            din.skip(offsetGlCmds - offsetTriangles - (numTriangles * 12));
            int[] glCommands = new int[numGlCommands];
            for (int i = 0; i < numGlCommands; i++) {
                glCommands[i] = Integer.reverseBytes(din.readInt());
            }

            frames = new MD2Frame[numFrames];
            din.skip(offsetFrames - offsetGlCmds - (numGlCommands * 4));
            for (int i = 0; i < numFrames; i++) {
                float[] scale = new float[3];
                float[] translate = new float[3];
                byte[] name = new byte[16];
                byte[][] verts = new byte[numVertices][4];

                for (int j = 0; j < 3; j++) {
                    scale[j] = Float.intBitsToFloat(Integer.reverseBytes(din.readInt()));
                }

                for (int j = 0; j < 3; j++) {
                    translate[j] = Float.intBitsToFloat(Integer.reverseBytes(din.readInt()));
                }

                din.readFully(name);

                for (int j = 0; j < numVertices; j++) {
                    verts[j][0] = din.readByte(); // x
                    verts[j][1] = din.readByte(); // y
                    verts[j][2] = din.readByte(); // z
                    verts[j][3] = din.readByte(); // lightnormalindex
                }

                MD2Frame frame = new MD2Frame();
                frame.scale = scale;
                frame.translate = translate;
                frame.vertices = verts;
                frame.name = new String(name).trim();
                frames[i] = frame;
            }

        }catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void renderFrame(int frameIndex) {
        if (frameIndex >= frames.length) return;

        float[] scale = frames[frameIndex].scale;
        float[] translate = frames[frameIndex].translate;
        byte[][] verts = frames[frameIndex].vertices;

        glBegin(GL_TRIANGLES);
        for (int i = 0; i < triangles.length; i++) {
            for (int j = 0; j < 3; j++) {
                int vertexIndex = triangles[i][j];
                float x = verts[vertexIndex][0] & 0xFF;
                float y = verts[vertexIndex][1] & 0xFF;
                float z = verts[vertexIndex][2] & 0xFF;
                x = x * scale[0] + translate[0];
                y = y * scale[1] + translate[1];
                z = z * scale[2] + translate[2];

                glVertex3f(x, y, z);
            }
        }
        glEnd();
    }


    public void updateAnimation(){
        interpolacao += 0.1f;
        if(interpolacao > 1.0f){
            currentFrame += nextFrame;
            nextFrame = (nextFrame + 1) % frames.length;
            interpolacao = 0.0f;
        }
    }

    public void Interpolated(int currentFrame, int nextFrame, float alpha) {
        if (currentFrame >= frames.length || nextFrame >= frames.length) return;

        MD2Frame frame1 = frames[currentFrame];
        MD2Frame frame2 = frames[nextFrame];

        glBegin(GL_TRIANGLES);
        for (int i = 0; i < triangles.length; i++) {
            for (int j = 0; j < 3; j++) {
                int vertexIndex = triangles[i][j];
                int texCoordIndex = triangles[i][j + 3];

                if (vertexIndex >= numVertices || texCoordIndex >= numTexCoords) continue;

                float s = (float)(texCoords[texCoordIndex][0] & 0xFFFF) / (float)skinWidth;
                float t = (float)(texCoords[texCoordIndex][1] & 0xFFFF) / (float)skinHeight;

                float x1 = (frame1.vertices[vertexIndex][0] & 0xFF) * frame1.scale[0] + frame1.translate[0];
                float y1 = (frame1.vertices[vertexIndex][1] & 0xFF) * frame1.scale[1] + frame1.translate[1];
                float z1 = (frame1.vertices[vertexIndex][2] & 0xFF) * frame1.scale[2] + frame1.translate[2];

                // Vértices do próximo frame
                float x2 = (frame2.vertices[vertexIndex][0] & 0xFF) * frame2.scale[0] + frame2.translate[0];
                float y2 = (frame2.vertices[vertexIndex][1] & 0xFF) * frame2.scale[1] + frame2.translate[1];
                float z2 = (frame2.vertices[vertexIndex][2] & 0xFF) * frame2.scale[2] + frame2.translate[2];
                
                float x = x1 + alpha * (x2 - x1);
                float y = y1 + alpha * (y2 - y1);
                float z = z1 + alpha * (z2 - z1);
                glTexCoord2f(s, t);
                glVertex3f(x, y, z);
            }
        }
        glEnd();
    }


}

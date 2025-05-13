
import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import obj.ObjModel;
import util.TextureLoader;

import java.awt.image.BufferedImage;

//import com.sun.org.apache.xerces.internal.dom.DeepNodeListImpl;

import java.nio.*;
import java.util.ArrayList;
import java.util.Random;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Main3D {

	// The window handle
	private long window;
	
	float angTankViewX = 0;
	float angTankViewY = 0;
	
	float angTowerViewY = 0;
	
	float angCanonViewZ = 0;
	
	public Random rnd = new Random();

	public void run() throws InterruptedException {
		System.out.println("Hello LWJGL " + Version.getVersion() + "!");

		init();
		loop();

		glfwFreeCallbacks(window);
		glfwDestroyWindow(window);

		glfwTerminate();
		glfwSetErrorCallback(null).free();
	}

	private void init() {
		GLFWErrorCallback.createPrint(System.err).set();
		if (!glfwInit())
			throw new IllegalStateException("Unable to initialize GLFW");
		glfwDefaultWindowHints();
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
		glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

		window = glfwCreateWindow(800, 600, "Hello World!", NULL, NULL);
		if (window == NULL)
			throw new RuntimeException("Failed to create the GLFW window");
		
		glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
			if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
				glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
		
			if ( key == GLFW_KEY_W) {
				angTankViewX+=5;
			}
			if ( key == GLFW_KEY_S) {
				angTankViewX-=5;
			}
			if ( key == GLFW_KEY_A) {
				angTankViewY+=5;
			}
			if ( key == GLFW_KEY_D) {
				angTankViewY-=5;
			}
			
			if ( key == GLFW_KEY_N) {
				angTowerViewY+=5;
			}
			if ( key == GLFW_KEY_M) {
				angTowerViewY-=5;
			}
			
			if ( key == GLFW_KEY_G) {
				angCanonViewZ+=5;
			}
			if ( key == GLFW_KEY_B) {
				angCanonViewZ-=5;
			}
		
		});

		try (MemoryStack stack = stackPush()) {
			IntBuffer pWidth = stack.mallocInt(1); // int*
			IntBuffer pHeight = stack.mallocInt(1); // int*

			glfwGetWindowSize(window, pWidth, pHeight);

			GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

			glfwSetWindowPos(window, (vidmode.width() - pWidth.get(0)) / 2, (vidmode.height() - pHeight.get(0)) / 2);
		}

		glfwMakeContextCurrent(window);
		glfwSwapInterval(1);

		glfwShowWindow(window);
	}

	private void loop() {
		GL.createCapabilities();

		BufferedImage imggato = TextureLoader.loadImage("x-35_obj_b.jpg");
		BufferedImage gatorgba = new BufferedImage(imggato.getWidth(), imggato.getHeight(), BufferedImage.TYPE_INT_ARGB);
		gatorgba.getGraphics().drawImage(imggato, 0, 0, null);
		int tgato = TextureLoader.loadTexture(imggato);

		glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

		MD2Model x35 = new MD2Model();
		x35.loadMD2("tris.md2");

		int currentFrame = 0;
		float frameTime = 0.2f; // Tempo entre frames (200 ms)
		float accumulator = 0f;
		long lastTime = System.nanoTime();

		glMatrixMode(GL_PROJECTION);
		glLoadIdentity();
		gluPerspective(45, 600f / 800f, 0.5f, 100);
		glMatrixMode(GL_MODELVIEW);
		glLoadIdentity();

		while (!glfwWindowShouldClose(window)) {
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

			glEnable(GL_LIGHTING);
			glShadeModel(GL_SMOOTH);
			glLoadIdentity();

			float[] lightAmbient = { 0.1f, 0.1f, 0.1f, 0.5f };
			float[] lightDiffuse = { 0.5f, 0.5f, 0.5f, 0.5f };
			float[] lightPosition = { 0.0f, 0.0f, 0.0f, 1.0f };
			glLightfv(GL_LIGHT0, GL_AMBIENT, lightAmbient);
			glLightfv(GL_LIGHT0, GL_DIFFUSE, lightDiffuse);
			glLightfv(GL_LIGHT0, GL_POSITION, lightPosition);

			glEnable(GL_LIGHT0);
			glEnable(GL_COLOR_MATERIAL);
			glColorMaterial(GL_FRONT, GL_AMBIENT_AND_DIFFUSE);
			glEnable(GL_DEPTH_TEST);
			glEnable(GL_TEXTURE_2D);
			glBindTexture(GL_TEXTURE_2D, tgato);

			glPushMatrix();
			glTranslatef(0, 0, -4);
			glScalef(0.01f, 0.01f, 0.01f);
			glRotatef(angTankViewX, 1.0f, 0.0f, 0.0f);
			glRotatef(angTankViewY, 0.0f, 1.0f, 0.0f);

			// ===== Interpolação entre frames =====
			long currentTime = System.nanoTime();
			float deltaTime = (currentTime - lastTime) / 1_000_000_000.0f;
			lastTime = currentTime;
			accumulator += deltaTime;

			int nextFrame = (currentFrame + 1) % x35.numFrames;
			float alpha = accumulator / frameTime;
			if (alpha > 1.0f) alpha = 1.0f;

			x35.renderInterpolatedFrame(currentFrame, nextFrame, alpha);

			if (accumulator >= frameTime) {
				accumulator -= frameTime;
				currentFrame = nextFrame;
			}
			// =====================================

			glPopMatrix();

			// Exemplo de desenho com textura
			glPushMatrix();
			glTranslatef(0, 2, -4);
			glColor3f(1f, 1f, 1f);
			glBegin(GL_TRIANGLE_STRIP);
			glTexCoord2f(1.0f, 1.0f); glVertex3f(-0.5f, -0.5f, -2.0f);
			glTexCoord2f(1.0f, 0.0f); glVertex3f(-0.5f,  0.5f, -2.0f);
			glTexCoord2f(0.0f, 1.0f); glVertex3f( 0.5f, -0.5f, -2.0f);
			glTexCoord2f(0.0f, 0.0f); glVertex3f( 0.5f,  0.5f, -2.0f);
			glEnd();
			glPopMatrix();

			glfwSwapBuffers(window);
			glfwPollEvents();
		}
	}



	public static void main(String[] args) throws InterruptedException {
		new Main3D().run();
	}

	public static void gluPerspective(float fovy, float aspect, float near, float far) {
		float bottom = -near * (float) Math.tan(fovy / 2);
		float top = -bottom;
		float left = aspect * bottom;
		float right = -left;
		glFrustum(left, right, bottom, top, near, far);
	}

}

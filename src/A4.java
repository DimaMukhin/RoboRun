import javax.swing.*;

import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import javax.imageio.ImageIO;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.*;
import com.jogamp.opengl.glu.*;

import com.jogamp.opengl.util.awt.ImageUtil;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;

public class A4 implements GLEventListener, KeyListener, MouseListener, MouseMotionListener {
	public static final boolean TRACE = false;

	public static final String WINDOW_TITLE = "A4: [Dima Mukhin]";
	public static final int INITIAL_WIDTH = 640;
	public static final int INITIAL_HEIGHT = 640;
	public static final int RAIN_PARTICLES = 120;

	private static final GLU glu = new GLU();

	private static final String TEXTURE_PATH = "resources/";
	
	// TODO: change this
	public static final String[] TEXTURE_FILES = { "circle.png", "ground.jpg", "metal.jpg", "danger.jpg", "skybox_left.png",
			"skybox_front.png", "skybox_right.png", "skybox_back.png", "skybox_top.png", "skybox_bottom.png", "rain.jpg"};

	public static void main(String[] args) {
		final JFrame frame = new JFrame(WINDOW_TITLE);

		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				if (TRACE)
					System.out.println("closing window '" + ((JFrame)e.getWindow()).getTitle() + "'");
				System.exit(0);
			}
		});

		final GLProfile profile = GLProfile.get(GLProfile.GL2);
		final GLCapabilities capabilities = new GLCapabilities(profile);
		final GLCanvas canvas = new GLCanvas(capabilities);
		try {
			Object self = self().getConstructor().newInstance();
			self.getClass().getMethod("setup", new Class[] { GLCanvas.class }).invoke(self, canvas);
			canvas.addGLEventListener((GLEventListener)self);
			canvas.addKeyListener((KeyListener)self);
			canvas.addMouseListener((MouseListener)self);
			canvas.addMouseMotionListener((MouseMotionListener)self);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		canvas.setSize(INITIAL_WIDTH, INITIAL_HEIGHT);

		frame.getContentPane().add(canvas);
		frame.pack();
		frame.setVisible(true);

		canvas.requestFocusInWindow();

		if (TRACE)
			System.out.println("-> end of main().");
	}

	private static Class<?> self() {
		// This ugly hack gives us the containing class of a static method 
		return new Object() { }.getClass().getEnclosingClass();
	}

	/*** Instance variables and methods ***/

	private String direction;
	static Texture[] textures;

	// TODO: Add instance variables here
	// Robot
	private Structure robot;	// Robot
	private Structure rightArm;	// Robot's right arm
	private Structure leftArm;	// Robot's left arm
	private Structure rightLeg;	// Robot's right leg
	private Structure leftLeg; 	// Robot's left leg
	
	// shapes
	private Shape rightHand;	// right hand
	private Shape leftHand;		// left hand
	private Shape rightFoot;	// right foot
	private Shape leftFoot;		// left foot
	
	// walking and motion states
	private boolean reverse = false; // reverse motion?
	private boolean jumping = false; // are you jumping?
	private float vy = 0; // speed in y axis
	
	// obstacles and hit detection
	private LinkedList<Obstacle> obstacles = new LinkedList<Obstacle>();
	private float jumpY;
	
	// camera
	private boolean firstPerson = true;
	private float[] lastDragPos;
	private float xangle = 0.0f;
	private float yangle = 0.0f;
	private float ztrans = -5.0f;

	// rain
	private LinkedList<RainDrop> rain = new LinkedList<RainDrop>();
	
	public void setup(final GLCanvas canvas) {
		// Called for one-time setup
		if (TRACE)
			System.out.println("-> executing setup()");

		new Timer().scheduleAtFixedRate(new TimerTask() {
			public void run() {
				canvas.repaint();
			}
		}, 1000, 1000/60);

		// TODO: Add code here
	}

	@Override
	public void init(GLAutoDrawable drawable) {
		// Called when the canvas is (re-)created - use it for initial GL setup
		if (TRACE)
			System.out.println("-> executing init()");

		final GL2 gl = drawable.getGL().getGL2();

		textures = new Texture[TEXTURE_FILES.length];
		try {
			for (int i = 0; i < TEXTURE_FILES.length; i++) {
				File infile = new File(TEXTURE_PATH + TEXTURE_FILES[i]); 
				BufferedImage image = ImageIO.read(infile);
				ImageUtil.flipImageVertically(image);
				textures[i] = TextureIO.newTexture(AWTTextureIO.newTextureData(gl.getGLProfile(), image, false));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		// TODO: Add code here
		constructRobot(); // construct the robot
		constructObstacles();

		// make it rain!
		for (int i = 0; i < RAIN_PARTICLES; i++) {
			rain.add(new RainDrop(robot));
		}
		
		// fog
		float[] colour = new float[] { 0.2f, 0.2f, 0.2f, 0.0f };
		gl.glEnable(GL2.GL_FOG);
		gl.glFogfv(GL2.GL_FOG_COLOR, colour, 0);
		gl.glFogi(GL2.GL_FOG_MODE, GL2.GL_LINEAR);
		gl.glFogf(GL2.GL_FOG_START, 1);
		gl.glFogf(GL2.GL_FOG_END, 10);
		
		gl.glClearColor(0.239f, 0.266f, 0.337f, 0.0f);
		gl.glEnable(GL2.GL_DEPTH_TEST);
		gl.glDepthFunc(GL2.GL_LEQUAL);
		gl.glEnable(GL2.GL_BLEND);
		gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
		gl.glEnable(GL2.GL_CULL_FACE);
	}
	
	@Override
	public void display(GLAutoDrawable drawable) {
		// Draws the display
		if (TRACE)
			System.out.println("-> executing display()");

		final GL2 gl = drawable.getGL().getGL2();

		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		glu.gluPerspective(25, 1, 2, 20); // [-2 to -20]
		
		gl.glMatrixMode(GL2.GL_MODELVIEW);

		// view
		if (firstPerson) {
			gl.glLoadIdentity();
			gl.glTranslatef(0, 0, -2.0f);							// move everything into the viewing volume
			gl.glTranslatef(-robot.posX, -robot.posY - 0.15f, -robot.posZ);	// move camera to the robot's head
			gl.glTranslatef(robot.posX, robot.posY, robot.posZ);	// put robot back in its original position
			gl.glRotatef(xangle, 1, 0, 0);							// rotate around the robot
			gl.glRotatef(yangle, 0, 1, 0);							// rotate around the robot
			gl.glRotatef(180 - robot.rotateY, 0, 1, 0);				// rotate around the robot
			gl.glTranslatef(-robot.posX, -robot.posY, -robot.posZ);	// set the robot at the origin
		} else {
			gl.glLoadIdentity();
			gl.glTranslatef(0, 0, ztrans);							// move everything into the viewing volume
			gl.glTranslatef(-robot.posX, -robot.posY, -robot.posZ);	// move camera to point at the robot
			gl.glTranslatef(robot.posX, robot.posY, robot.posZ);	// put robot back in its original position
			gl.glRotatef(20, 1, 0, 0);								// rotate around the robot
			gl.glRotatef(180 - robot.rotateY, 0, 1, 0);				// rotate around the robot
			gl.glTranslatef(-robot.posX, -robot.posY, -robot.posZ);	// set the robot at the origin
		}
		
		// TODO: Replace with your drawing code
		drawSurface(gl); // draw the surface
		
		walk();
		
		// hit detection:
		float[] hitbox = new float[] {robot.posX - 0.1f, robot.posZ - 0.1f, 0.2f, 0.2f, robot.posY + jumpY};
		
		for (int o = 0; o < obstacles.size(); o++) {
			Obstacle curr = obstacles.get(o);
			
			if (curr.hit(hitbox)) {
				robot.posX = 0;
				robot.posZ = 0;
			}
		}
		
		// making it rain!
		for (int i = 0; i < rain.size(); i++) {
			rain.get(i).draw(gl);
		}
		
		drawSkyBox(gl); // drawing the sky-box
		
		// drawing obstacles
		for (int i = 0; i < obstacles.size(); i++) {
			obstacles.get(i).draw(gl);
		}
		
		// draw the robot only in third person
		if (!firstPerson)
			robot.draw(gl); // drawing the robot
	}

	/*----------------------------------------------------------------------------------------drawSkyBox
	 * PURPOSE: draw the skybox
	 */
	private void drawSkyBox(GL2 gl) {
		float d = 9.5f;
		
		gl.glDisable(GL2.GL_FOG); // disable fog for the skybox
		
		gl.glColor3f(1, 1, 1);
		
		gl.glPushMatrix();
		gl.glTranslatef(robot.posX, robot.posY, robot.posZ);
		
		// left skybox
		textures[4].enable(gl);
		textures[4].bind(gl);
		
		gl.glBegin(GL2.GL_QUADS);
		
		gl.glTexCoord2f(0, 0);
		gl.glVertex3f(-d, -8, d);
		gl.glTexCoord2f(1, 0);
		gl.glVertex3f(-d, -8, -d);
		gl.glTexCoord2f(1, 1);
		gl.glVertex3f(-d, 8, -d);
		gl.glTexCoord2f(0, 1);
		gl.glVertex3f(-d, 8, d);
		
		gl.glEnd();
		
		textures[4].disable(gl);
		
		// front skybox
		textures[5].enable(gl);
		textures[5].bind(gl);
		
		gl.glBegin(GL2.GL_QUADS);
		
		gl.glTexCoord2f(0, 0);
		gl.glVertex3f(-d, -8, -d);
		gl.glTexCoord2f(1, 0);
		gl.glVertex3f(d, -8, -d);
		gl.glTexCoord2f(1, 1);
		gl.glVertex3f(d, 8, -d);
		gl.glTexCoord2f(0, 1);
		gl.glVertex3f(-d, 8, -d);
		
		gl.glEnd();
		
		textures[5].disable(gl);
		
		// right skybox
		textures[6].enable(gl);
		textures[6].bind(gl);
		
		gl.glBegin(GL2.GL_QUADS);
		
		gl.glTexCoord2f(0, 0);
		gl.glVertex3f(d, -8, -d);
		gl.glTexCoord2f(1, 0);
		gl.glVertex3f(d, -8, d);
		gl.glTexCoord2f(1, 1);
		gl.glVertex3f(d, 8, d);
		gl.glTexCoord2f(0, 1);
		gl.glVertex3f(d, 8, -d);
		
		gl.glEnd();
		
		textures[6].disable(gl);
		
		// back skybox
		textures[7].enable(gl);
		textures[7].bind(gl);
		
		gl.glBegin(GL2.GL_QUADS);
		
		gl.glTexCoord2f(0, 0);
		gl.glVertex3f(d, -8, d);
		gl.glTexCoord2f(1, 0);
		gl.glVertex3f(-d, -8, d);
		gl.glTexCoord2f(1, 1);
		gl.glVertex3f(-d, 8, d);
		gl.glTexCoord2f(0, 1);
		gl.glVertex3f(d, 8, d);
		
		gl.glEnd();
		
		textures[7].disable(gl);
		
		// top skybox
		textures[8].enable(gl);
		textures[8].bind(gl);
		
		gl.glBegin(GL2.GL_QUADS);
		
		gl.glTexCoord2f(0, 1);
		gl.glVertex3f(-d, 8, d);
		gl.glTexCoord2f(0, 0);
		gl.glVertex3f(-d, 8, -d);
		gl.glTexCoord2f(1, 0);
		gl.glVertex3f(d, 8, -d);
		gl.glTexCoord2f(1, 1);
		gl.glVertex3f(d, 8, d);
		
		gl.glEnd();
		
		textures[8].disable(gl);
		
		// bottom skybox
		textures[9].enable(gl);
		textures[9].bind(gl);
		
		gl.glBegin(GL2.GL_QUADS);
		
		gl.glTexCoord2f(0, 1);
		gl.glVertex3f(-d, -8, d);
		gl.glTexCoord2f(0, 0);
		gl.glVertex3f(d, -8, d);
		gl.glTexCoord2f(1, 0);
		gl.glVertex3f(d, -8, -d);
		gl.glTexCoord2f(1, 1);
		gl.glVertex3f(-d, -8, d);
		
		gl.glEnd();
		
		textures[9].disable(gl);
		
		gl.glPopMatrix();
		
		gl.glEnable(GL2.GL_FOG); // re enable fog
	}// END drawSkyBox

	/*----------------------------------------------------------------------------------------------walk
	 * PURPOSE: make the robot walk
	 * REMARKS: called every frame
	 */
	private void walk() {
		float v = 0.02f; // velocity x
		
		// move robot according to its facing direction
		robot.posZ += v * Math.cos(Math.toRadians(robot.rotateY)); // move robot in z direction
		robot.posX += v * Math.sin(Math.toRadians(robot.rotateY)); // move robot in x direction
		
		if (jumping) {
			robot.posY += vy;
			vy -= 0.001f;
			if (robot.posY <= 1.5f * robot.uniScale) {
				vy = 0;
				robot.posY = 1.5f * robot.uniScale;
				jumping = false;
			}
		}
		
		// move hands, legs, feet, arms
		if (!reverse) {
			rightLeg.rotateX -= 1;
			rightFoot.angleS -= 1;
			leftLeg.rotateX += 1;
			leftFoot.angleS += 1;
			rightArm.rotateX += 1;
			rightHand.angleS += 1;
			leftArm.rotateX -= 1;
			leftHand.angleS -= 1;
		} else {
			rightLeg.rotateX += 1;
			rightFoot.angleS += 1;
			leftLeg.rotateX -= 1;
			leftFoot.angleS -= 1;
			rightArm.rotateX -= 1;
			rightHand.angleS -= 1;
			leftArm.rotateX += 1;
			leftHand.angleS += 1;
		}
		
		// reverse conditions
		if ((rightLeg.rotateX <= -30.0f || rightLeg.rotateX >= 20.0f)
				&& (leftLeg.rotateX <= -30.0f || leftLeg.rotateX >= 20.0f)) {
			reverse = !reverse;
		}
		
		if (robot.posX >= 10) {
			robot.posX = -10;
		}
		
		if (robot.posX < -10) {
			robot.posX = 10;
		}
		
		if (robot.posZ >= 10) {
			robot.posZ = -10;
		}
		
		if (robot.posZ < -10) {
			robot.posZ = 10;
		}
	}// END walk
	
	/*--------------------------------------------------------------------------------constructObstacles
	 * PURPOSE: construct the obstacles
	 */
	private void constructObstacles() {
		// just a reminder: 0.5 0.5 0.5 is for a 1 by 1 by 1 box
		StaticObstacle sObs1 = new StaticObstacle(new float[] {1, 1}, textures[3]);
		StaticObstacle sObs2 = new StaticObstacle(new float[] {-5, -5}, textures[3]);
		StaticObstacle sObs3 = new StaticObstacle(new float[] {-2, 6}, textures[3]);
		StaticObstacle sObs4 = new StaticObstacle(new float[] {8, -6}, textures[3]);
		StaticObstacle sObs5 = new StaticObstacle(new float[] {2, 4}, textures[3]);
		obstacles.add(sObs1);
		obstacles.add(sObs2);
		obstacles.add(sObs3);
		obstacles.add(sObs4);
		obstacles.add(sObs5);
		
		Car car = new Car(new float[] {0, 2});
		obstacles.add(car);
		
		SmartObstacle smart = new SmartObstacle(robot, new float[] {5, -5});
		obstacles.add(smart);
	}// END constructObstacles
	
	/*------------------------------------------------------------------------------------constructRobot
	 * PURPOSE: construct the robot
	 */
	private void constructRobot() {
		float[] bodyS; 		// scale of the robot's body
		float[] headS; 		// scale of the robot's head
		float[] armS; 		// scale of the robot's arm
		float[] handS;		// scale of the robot's hand
		float[] legS;		// scale of the robot's leg
		float[] footS;		// scale of the robot's foot
		float[] headPos; 	// head's position relative to the robot
		float[] rArmPos;	// right arm's position relative to the robot
		float[] lArmPos;	// left arm's position relative to the robot
		float[] HandPos;	// hand's position relative to the arm
		float[] rLegPos; 	// right leg's position relative to the robot
		float[] lLegPos;	// left leg's position relative to the robot
		float[] footPos;	// foot's position relative to the leg
		
		// making the robot with just its body (no head, arms, legs)
		bodyS = new float[] {0.5f, 0.5f, 0.25f};
		robot = new Structure(bodyS, new Shape[] { }, new float[][] {}, textures[2]);
		
		// making the head of the robot
		headS = new float[] {0.25f, 0.25f, 0.25f};
		headPos = new float[] {0.0f, 3.5f, 0.0f};
		
		// making the right arm of the robot
		armS = new float[] {0.25f, 0.5f, 0.25f};
		rArmPos = new float[] {0.75f, 0.0f, 0.0f};
		rightArm = new Structure(armS, new Shape[] { }, new float[][] {}, textures[2]);
		rightArm.center = new float[] {0.0f, 0.5f, 0.0f};
		rightArm.rotateX = -30.0f;
		handS = new float[] {0.25f, 0.25f, 0.25f};
		HandPos = new float[] {0.0f, -0.25f, 0.25f};
		rightArm.add(handS, HandPos);
		
		// making the left arm of the robot
		lArmPos = new float[] {-0.75f, 0.0f, 0.0f};
		leftArm = new Structure(armS, new Shape[] { }, new float[][] {}, textures[2]);
		leftArm.center = new float[] {0.0f, 0.5f, 0.0f};
		leftArm.add(handS, HandPos);
		
		// making the right leg of the robot
		legS = new float[] {0.25f, 0.5f, 0.25f};
		rLegPos = new float[] {0.25f, -1.0f, 0.0f};
		rightLeg = new Structure(legS, new Shape[] { }, new float[][] {}, textures[2]);
		rightLeg.center = new float[] {0.0f, 0.5f, 0.0f};
		footS = new float[] {0.25f, 0.25f, 0.25f};
		footPos = new float[] {0.0f, -0.25f, 0.25f};
		rightLeg.add(footS, footPos);
		
		// making the left leg of the robot
		lLegPos = new float[] {-0.25f, -1.0f, 0.0f};
		leftLeg = new Structure(legS, new Shape[] { }, new float[][] {}, textures[2]);
		leftLeg.center = new float[] {0.0f, 0.5f, 0.0f};
		leftLeg.rotateX = -30.0f;
		leftLeg.add(footS, footPos);
		
		// adding all the parts to the robot
		robot.add(new Shape("resources/" + "sphere.obj"), headPos); // adding the head	
		robot.add(rightArm, rArmPos);	// adding the right arm
		robot.add(leftArm, lArmPos);	// adding the left arm
		robot.add(rightLeg, rLegPos);	// adding the right leg
		robot.add(leftLeg, lLegPos);	// adding the left leg
		
		// getting all sub-shapes
		rightHand = rightArm.contents.getFirst();
		leftHand = leftArm.contents.getFirst();
		rightFoot = rightLeg.contents.getFirst();
		leftFoot = leftLeg.contents.getFirst();
		
		rightHand.angleS = -30.0f;
		leftFoot.angleS = -30.0f;
		
		robot.uniScale = 0.25f;
		
		// rotating the robot (robot will walk along the x-axis)
		robot.rotateY = 90;
		
		// putting the robot on the surface
		robot.posY = 1.5f * robot.uniScale;
		jumpY = -robot.posY;
	}// END constructRobot

	/*---------------------------------------------------------------------------------------drawSurface
	 * PURPOSE: drwa the surface
	 */
	private void drawSurface(GL2 gl) {
		float minX, minZ, maxX, maxZ;
		minX = minZ = -10.0f;
		maxX = maxZ = 10.0f;
		
		textures[1].bind(gl);
		textures[1].enable(gl);
		
		for (float xoff = minX; xoff <= maxX; xoff++) {
			for (float zoff = minZ; zoff <= maxZ; zoff++) {
				gl.glPushMatrix();
				gl.glTranslatef(xoff, 0, zoff);
				gl.glColor3f(1, 1, 1);
				gl.glBegin(GL2.GL_QUADS);
				gl.glTexCoord2f(1, 1);
				gl.glVertex3f(0.5f, 0, 0.5f);
				gl.glTexCoord2f(0, 1);
				gl.glVertex3f(0.5f, 0, -0.5f);
				gl.glTexCoord2f(0, 0);
				gl.glVertex3f(-0.5f, 0, -0.5f);
				gl.glTexCoord2f(1, 0);
				gl.glVertex3f(-0.5f, 0, 0.5f);
				gl.glEnd();
				gl.glColor3f(0, 0, 1);
				gl.glBegin(GL2.GL_LINE_LOOP);
				gl.glVertex3f(0.5f, 0, 0.5f);
				gl.glVertex3f(-0.5f, 0, 0.5f);
				gl.glVertex3f(-0.5f, 0, -0.5f);
				gl.glVertex3f(0.5f, 0, -0.5f);
				gl.glEnd();
				gl.glPopMatrix();
			}
		}
		
		textures[1].disable(gl);
	}// END drawSurface

	@Override
	public void dispose(GLAutoDrawable drawable) {
		// Called when the canvas is destroyed (reverse anything from init) 
		if (TRACE)
			System.out.println("-> executing dispose()");
	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		// Called when the canvas has been resized
		// Note: glViewport(x, y, width, height) has already been called so don't bother if that's what you want
		if (TRACE)
			System.out.println("-> executing reshape(" + x + ", " + y + ", " + width + ", " + height + ")");

		final GL2 gl = drawable.getGL().getGL2();
		float ar = (float)width / (height == 0 ? 1 : height);

		gl.glMatrixMode(GL2.GL_PROJECTION);
		
		// TODO: use a perspective projection instead
		glu.gluPerspective(25, 1, 2, 20); // [-2 to -20]
	}

	@Override
	public void keyTyped(KeyEvent e) {
		if (e.getKeyChar() == 't') {
			xangle--;
		} else if (e.getKeyChar() == 'g') {
			xangle++;
		} else if (e.getKeyChar() == 'f') {
			yangle--;
		} else if (e.getKeyChar() == 'h') {
			yangle++;
		} else if (e.getKeyChar() == 'r') {
			xangle = 0;
			yangle = 0;
			ztrans = -5.0f;
		} else if (e.getKeyChar() == 'u') {
			ztrans += 0.01f;
		} else if (e.getKeyChar() == 'j') {
			ztrans -= 0.01f;
		}
		System.out.printf("Viewing angle x = %f y = %f | ztrans = %2.2f\n", xangle, yangle, ztrans);
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}

	@Override
	public void keyPressed(KeyEvent e) {
		// TODO Change this however you like
		direction = null;
		if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyChar() == 'a') {
			direction = "left";
			robot.rotateY += 5;
		}
		else if (e.getKeyCode() == KeyEvent.VK_RIGHT || e.getKeyChar() == 'd') {
			direction = "right";
			robot.rotateY -= 5;
		}
		else if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyChar() == 'w')
			direction = "up";
		else if (e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyChar() == 's')
			direction = "down";
		if (direction != null) {
			System.out.println("Direction key pressed: " + direction);
			((GLCanvas)e.getSource()).repaint();
		}
		if (e.getKeyChar() == ' ') {
			System.out.println("Space bar: jump!");
			if (!jumping) {
				jumping = true;
				vy = 0.03f;
			}
		} else if (e.getKeyChar() == '\n') {
			System.out.println("Enter: switch view");
			firstPerson = !firstPerson;
		}
		// TODO: add more keys as necessary
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		// TODO: use this or mouse moved for free look
		System.out.println("drag: (" + e.getX() + "," + e.getY() + ") at " + e.getWhen());
		float wx = e.getX();
		float wy = INITIAL_HEIGHT - e.getY() - 1;
		float dx = wx - lastDragPos[0];
		float dy = wy - lastDragPos[1];
		
		yangle += dx / 2.0f;
		xangle += dy / 2.0f;
		
		lastDragPos = new float[] {wx, wy};
	}

	@Override
	public void mouseMoved(MouseEvent e) {
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
		// TODO: you may need this
		System.out.println("press: (" + e.getX() + "," + e.getY() + ") at " + e.getWhen());
		float wx = e.getX();
		float wy = INITIAL_HEIGHT - e.getY() - 1;
		lastDragPos = new float[] {wx, wy};
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		yangle = 0;
		xangle = 0;
	}
	
	// bring in shape & structure (or whatever you used) from A3Q2 here
	// include the OBJ reading code if you are using it
}

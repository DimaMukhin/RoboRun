import java.util.ArrayList;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.texture.Texture;

public class Face {
	private int[] indices;
	private float[] colour;
	private Texture tex;
	
	public Face(int[] indices, float[] colour) {
		this.indices = new int[indices.length];
		this.colour = new float[colour.length];
		System.arraycopy(indices, 0, this.indices, 0, indices.length);
		System.arraycopy(colour, 0, this.colour, 0, colour.length);
		tex = null;
	}
	
	public Face(int[] indices, float[] colour, Texture tex) {
		this.indices = new int[indices.length];
		this.colour = new float[colour.length];
		System.arraycopy(indices, 0, this.indices, 0, indices.length);
		System.arraycopy(colour, 0, this.colour, 0, colour.length);
		this.tex = tex;
	}
	
	public void draw(GL2 gl, ArrayList<float[]> vertices, boolean useColour) {
		if (useColour) {
			if (colour.length == 3)
				gl.glColor3f(colour[0], colour[1], colour[2]);
			else
				gl.glColor4f(colour[0], colour[1], colour[2], colour[3]);
		}
		
		if (indices.length == 1) {
			gl.glBegin(GL2.GL_POINTS);
		} else if (indices.length == 2) {
			gl.glBegin(GL2.GL_LINES);
		} else if (indices.length == 3) {
			gl.glBegin(GL2.GL_TRIANGLES);
		} else if (indices.length == 4) {
			if (tex != null) {
				tex.enable(gl);
				tex.bind(gl);
				gl.glColor3f(1, 1, 1);
				
				gl.glBegin(GL2.GL_QUADS);
				gl.glTexCoord2f(0, 0);
				gl.glVertex3f(vertices.get(indices[0])[0], vertices.get(indices[0])[1], vertices.get(indices[0])[2]);
				gl.glTexCoord2f(1, 0);
				gl.glVertex3f(vertices.get(indices[1])[0], vertices.get(indices[1])[1], vertices.get(indices[1])[2]);
				gl.glTexCoord2f(1, 1);
				gl.glVertex3f(vertices.get(indices[2])[0], vertices.get(indices[2])[1], vertices.get(indices[2])[2]);
				gl.glTexCoord2f(0, 1);
				gl.glVertex3f(vertices.get(indices[3])[0], vertices.get(indices[3])[1], vertices.get(indices[3])[2]);
			} else {
				gl.glBegin(GL2.GL_QUADS);
			}
		} else {
			gl.glBegin(GL2.GL_POLYGON);
		}
		
		if (tex == null) {
			for (int i: indices) {
				gl.glVertex3f(vertices.get(i)[0], vertices.get(i)[1], vertices.get(i)[2]);
			}
		}
		
		gl.glEnd();
		
		if (tex != null) {
			tex.disable(gl);
		}
	}
}

//work in progress, artifact-prone. Small and degenerate faces are not being removed yet...

import wblut.math.*;
import wblut.processing.*;
import wblut.core.*;
import wblut.hemesh.*;
import wblut.geom.*;
HE_Mesh mesh;
WB_Render render;
HE_Selection sel;

void setup() {
  size(800, 800, OPENGL);
  smooth(8);
  HEC_Cube creator=new HEC_Cube(200,1,1,1);
  mesh=new HE_Mesh(creator); 
 HEM_Extrude ext=new HEM_Extrude().setDistance(200);
 mesh.modify(ext);
 

sel=ext.extruded;
mesh.delete(sel);

mesh.subdivide(new HES_Planar());
  render=new WB_Render(this);
}

void draw() {
  background(255);
  directionalLight(255, 255, 255, 1, 1, -1);
  directionalLight(127, 127, 127, -1, -1, 1);
  translate(400, 400, 0);
  rotateY(mouseX*1.0f/width*TWO_PI);
  rotateX(mouseY*1.0f/height*TWO_PI);
  stroke(0);
  render.drawEdges(mesh);
  noStroke();
  render.drawFaces(mesh);
  
}

void mousePressed(){
 
 mesh.modify(new HEM_Soapfilm().setIterations(10)); 
 mesh.collapseDegenerateEdges(0.01);
 
}

void keyPressed(){
 mesh.smooth(); 
}

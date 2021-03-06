package wblut.hemesh;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import wblut.geom.WB_Point;
import wblut.math.WB_Epsilon;

public class HEC_IsoSurfaceSNAP extends HEC_Creator {
	final static int ONVERTEX = 0;
	final static int ONEDGE = 1;
	final static int NEGATIVE = 0;
	final static int EQUAL = 1;
	final static int POSITIVE = 2;

	int[] digits = new int[8];
	/*
	 * VERTICES 000 ijk=0 100 Ijk=1 010 iJk=2 110 IJk=3 001 ijK=4 101 IjK=5 011
	 * iJK=6 111 IJK=7
	 */

	final static WB_Point[] gridvertices = new WB_Point[] {
		new WB_Point(0, 0, 0), new WB_Point(1, 0, 0),
		new WB_Point(0, 1, 0), new WB_Point(1, 1, 0),
		new WB_Point(0, 0, 1), new WB_Point(1, 0, 1),
		new WB_Point(0, 1, 1), new WB_Point(1, 1, 1), };

	// EDGES: 2 vertices per edge

	final static int[][] edges = { { 0, 1 }, // x ijk
		{ 0, 2 }, // y ijk
		{ 1, 3 }, // y Ijk
		{ 2, 3 }, // x iJk
		{ 0, 4 }, // z ijk
		{ 1, 5 }, // z Ijk
		{ 2, 6 }, // z iJk
		{ 3, 7 }, // z IJk
		{ 4, 5 }, // x ijK
		{ 4, 6 }, // y ijK
		{ 5, 7 }, // y IjK
		{ 6, 7 } // x iJK
	};

	// ISOVERTICES: 20
	// type=ONVERTEX iso vertex on vertex, index in vertex list
	// type=ONEDGE iso vertex on edge, index in edge list

	final static int[][] isovertices = new int[][] { { 0, 0 }, { 0, 1 },
		{ 0, 2 }, { 0, 3 }, { 0, 4 }, { 0, 5 }, { 0, 6 }, { 0, 7 },
		{ 1, 0 }, { 1, 1 }, { 1, 2 }, { 1, 3 }, { 1, 4 }, { 1, 5 },
		{ 1, 6 }, { 1, 7 }, { 1, 8 }, { 1, 9 }, { 1, 10 }, { 1, 11 } };

	int[][] entries;

	private double[][][] values;

	private int resx, resy, resz;

	private double cx, cy, cz;

	private double dx, dy, dz;

	private double isolevel;

	private double boundary;

	private TIntObjectMap<HE_Vertex> xedges;

	private TIntObjectMap<HE_Vertex> yedges;

	private TIntObjectMap<HE_Vertex> zedges;

	private TIntObjectMap<HE_Vertex> vertices;

	private TIntObjectMap<VertexRemap> vertexremaps;

	double gamma;

	HE_Mesh mesh;

	private boolean invert;

	public HEC_IsoSurfaceSNAP() {
		super();

		String line = "";
		final String cvsSplitBy = " ";

		BufferedReader br = null;
		InputStream is = null;
		InputStreamReader isr = null;
		entries = new int[6561][];
		try {
			is = this.getClass().getClassLoader()
					.getResourceAsStream("resources/isonepcube3D.txt");
			isr = new InputStreamReader(is);
			br = new BufferedReader(isr);
			int i = 0;
			while ((line = br.readLine()) != null) {
				final String[] cell = line.split(cvsSplitBy);
				final int[] indices = new int[cell.length];
				for (int j = 0; j < cell.length; j++) {
					indices[j] = Integer.parseInt(cell[j]);
				}
				entries[i] = indices;
				i++;
			}

		}
		catch (final FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (final IOException e) {
			e.printStackTrace();
		}
		finally {
			if (br != null) {
				try {
					br.close();
					isr.close();
					is.close();
				}
				catch (final IOException e) {
					e.printStackTrace();
				}
			}
		}
		gamma = 0;
		override = true;
		boundary = Double.NaN;
	}

	public HEC_IsoSurfaceSNAP setGamma(final double gamma) {
		this.gamma = gamma;
		return this;
	}

	/**
	 * Number of cells.
	 *
	 * @param resx
	 *            the resx
	 * @param resy
	 *            the resy
	 * @param resz
	 *            the resz
	 * @return self
	 */
	public HEC_IsoSurfaceSNAP setResolution(final int resx, final int resy,
			final int resz) {
		this.resx = resx;
		this.resy = resy;
		this.resz = resz;
		return this;
	}

	/**
	 * Size of cell.
	 *
	 * @param dx
	 * @param dy
	 * @param dz
	 * @return self
	 */
	public HEC_IsoSurfaceSNAP setSize(final double dx, final double dy,
			final double dz) {
		this.dx = dx;
		this.dy = dy;
		this.dz = dz;
		return this;
	}

	/**
	 * Values at grid points.
	 *
	 * @param values
	 *            double[resx+1][resy+1][resz+1]
	 * @return self
	 */
	public HEC_IsoSurfaceSNAP setValues(final double[][][] values) {
		this.values = new double[resx + 1][resy + 1][resz + 1];
		for (int i = 0; i <= resx; i++) {
			for (int j = 0; j <= resy; j++) {
				for (int k = 0; k <= resz; k++) {
					this.values[i][j][k] = values[i][j][k];
				}
			}
		}
		return this;
	}

	/**
	 * Sets the values.
	 *
	 * @param values
	 *            float[resx+1][resy+1][resz+1]
	 * @return self
	 */
	public HEC_IsoSurfaceSNAP setValues(final float[][][] values) {
		this.values = new double[resx + 1][resy + 1][resz + 1];
		for (int i = 0; i <= resx; i++) {
			for (int j = 0; j <= resy; j++) {
				for (int k = 0; k <= resz; k++) {
					this.values[i][j][k] = values[i][j][k];
				}
			}
		}

		return this;
	}

	/**
	 * Isolevel to render
	 *
	 * @param v
	 *            isolevel
	 * @return self
	 */
	public HEC_IsoSurfaceSNAP setIsolevel(final double v) {
		isolevel = v;
		return this;
	}

	/**
	 * Boundary level.
	 *
	 * @param v
	 *            boundary level
	 * @return self
	 */
	public HEC_IsoSurfaceSNAP setBoundary(final double v) {
		boundary = v;
		return this;
	}

	/**
	 * Clear boundary level.
	 *
	 * @return self
	 */
	public HEC_IsoSurfaceSNAP clearBoundary() {
		boundary = Double.NaN;
		return this;
	}

	/**
	 * Invert isosurface.
	 *
	 * @param invert
	 *            true/false
	 * @return self
	 */
	public HEC_IsoSurfaceSNAP setInvert(final boolean invert) {
		this.invert = invert;
		return this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.hemesh.HEC_Creator#setCenter(wblut.geom.WB_Point3d)
	 */
	@Override
	public HEC_IsoSurfaceSNAP setCenter(final WB_Point c) {
		cx = c.xd();
		cy = c.yd();
		cz = c.zd();
		return this;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see wblut.hemesh.creators.HEB_Creator#createBase()
	 */
	@Override
	protected HE_Mesh createBase() {
		mesh = new HE_Mesh();
		mapvertices();
		setvalues();
		polygonise();
		snapvertices();
		resetvalues();
		mesh.clean();

		int cleaner = 0;
		do {
			cleaner++;
		} while (mesh.fixNonManifoldVertices() && cleaner < 10);

		return mesh;

	}

	private void mapvertices() {
		vertices = new TIntObjectHashMap<HE_Vertex>(1024, 0.5f, -1);
		xedges = new TIntObjectHashMap<HE_Vertex>(1024, 0.5f, -1);
		yedges = new TIntObjectHashMap<HE_Vertex>(1024, 0.5f, -1);
		zedges = new TIntObjectHashMap<HE_Vertex>(1024, 0.5f, -1);
		vertexremaps = new TIntObjectHashMap<VertexRemap>(1024, 0.5f, -1);
		final WB_Point offset = new WB_Point(cx - 0.5 * resx * dx, cy - 0.5
				* resy * dy, cz - 0.5 * resz * dz);
		if (Double.isNaN(boundary)) {
			for (int i = 0; i < resx; i++) {
				// System.out.println("HEC_IsoSurface: " + (i + 1) + " of " +
				// resx);
				for (int j = 0; j < resy; j++) {
					for (int k = 0; k < resz; k++) {
						getPolygons(i, j, k, classifyCell(i, j, k), offset,
								true);

					}
				}
			}
		}
		else {
			for (int i = -1; i < resx + 1; i++) {
				// System.out.println("HEC_IsoSurface: " + (i + 1) + " of " +
				// resx);
				for (int j = -1; j < resy + 1; j++) {
					for (int k = -1; k < resz + 1; k++) {
						getPolygons(i, j, k, classifyCell(i, j, k), offset,
								true);

					}
				}
			}

		}

	}

	void setvalues() {
		VertexRemap vr;
		for (final Object o : vertexremaps.values()) {
			vr = (VertexRemap) o;
			vr.snapvertex.set(vr.p);
			values[vr.i][vr.j][vr.k] = isolevel;
		}
	}

	/**
	 * Polygonise.
	 */
	private void polygonise() {

		final WB_Point offset = new WB_Point(cx - 0.5 * resx * dx, cy - 0.5
				* resy * dy, cz - 0.5 * resz * dz);
		if (Double.isNaN(boundary)) {
			for (int i = 0; i < resx; i++) {
				// System.out.println("HEC_IsoSurface: " + (i + 1) + " of " +
				// resx);
				for (int j = 0; j < resy; j++) {
					for (int k = 0; k < resz; k++) {
						getPolygons(i, j, k, classifyCell(i, j, k), offset,
								false);

					}
				}
			}
		}
		else {
			for (int i = -1; i < resx + 1; i++) {
				// System.out.println("HEC_IsoSurface: " + (i + 1) + " of " +
				// resx);
				for (int j = -1; j < resy + 1; j++) {
					for (int k = -1; k < resz + 1; k++) {
						getPolygons(i, j, k, classifyCell(i, j, k), offset,
								false);

					}
				}
			}

		}

	}

	void snapvertices() {
		VertexRemap vr;
		for (final Object o : vertexremaps.values()) {
			vr = (VertexRemap) o;
			vr.snapvertex.set(vr.p);
		}
	}

	void resetvalues() {
		VertexRemap vr;
		for (final Object o : vertexremaps.values()) {
			vr = (VertexRemap) o;

			values[vr.i][vr.j][vr.k] = vr.originalvalue;
		}
	}

	/**
	 * Gets the polygons.
	 *
	 * @param i
	 *            the i
	 * @param j
	 *            the j
	 * @param k
	 *            the k
	 * @param cubeindex
	 *            the cubeindex
	 * @return the polygons
	 */
	private void getPolygons(final int i, final int j, final int k,
			final int cubeindex, final WB_Point offset, final boolean dummyrun) {
		final int[] indices = entries[cubeindex];
		final int numtris = indices[0];
		int currentindex = 1;

		for (int t = 0; t < numtris; t++) {

			final HE_Face f = new HE_Face();
			final HE_Vertex v1 = getIsoVertex(indices[currentindex++], i, j, k,
					offset, dummyrun);
			final HE_Vertex v2 = getIsoVertex(indices[currentindex++], i, j, k,
					offset, dummyrun);
			final HE_Vertex v3 = getIsoVertex(indices[currentindex++], i, j, k,
					offset, dummyrun);
			if (!dummyrun) {
				final HE_Halfedge he1 = new HE_Halfedge();
				final HE_Halfedge he2 = new HE_Halfedge();
				final HE_Halfedge he3 = new HE_Halfedge();

				he1.setNext(he2);
				he2.setNext(he3);
				he3.setNext(he1);

				he1.setFace(f);
				he2.setFace(f);
				he3.setFace(f);
				he1.setVertex(v1);
				v1.setHalfedge(he1);
				he2.setVertex(v2);
				v2.setHalfedge(he2);
				he3.setVertex(v3);
				v3.setHalfedge(he3);
				f.setHalfedge(he1);

				mesh.add(f);
				mesh.add(he1);
				mesh.add(he2);
				mesh.add(he3);
			}

		}
	}

	/**
	 * Classify cell.
	 *
	 * @param i
	 *            the i
	 * @param j
	 *            the j
	 * @param k
	 *            the k
	 * @return the int
	 */
	private int classifyCell(final int i, final int j, final int k) {
		if (Double.isNaN(boundary)) {
			if ((i < 0) || (j < 0) || (k < 0) || (i >= resx) || (j >= resy)
					|| (k >= resz)) {
				return -1;
			}
		}
		digits = new int[8];
		int cubeindex = 0;
		int offset = 1;
		if (invert) {
			if (value(i, j, k) < isolevel) {
				cubeindex += 2 * offset;
				digits[0] = POSITIVE;
			}
			else if (value(i, j, k) == isolevel) {
				cubeindex += offset;
				digits[0] = EQUAL;
			}
			offset *= 3;
			if (value(i + 1, j, k) < isolevel) {
				cubeindex += 2 * offset;
				digits[1] = POSITIVE;
			}
			else if (value(i + 1, j, k) == isolevel) {
				cubeindex += offset;
				digits[1] = EQUAL;
			}
			offset *= 3;
			if (value(i, j + 1, k) < isolevel) {
				cubeindex += 2 * offset;
				digits[2] = POSITIVE;
			}
			else if (value(i, j + 1, k) == isolevel) {
				cubeindex += offset;
				digits[2] = EQUAL;
			}
			offset *= 3;
			if (value(i + 1, j + 1, k) < isolevel) {
				cubeindex += 2 * offset;
				digits[3] = POSITIVE;
			}
			else if (value(i + 1, j + 1, k) == isolevel) {
				cubeindex += offset;
				digits[3] = EQUAL;
			}
			offset *= 3;
			if (value(i, j, k + 1) < isolevel) {
				cubeindex += 2 * offset;
				digits[4] = POSITIVE;
			}
			else if (value(i, j, k + 1) == isolevel) {
				cubeindex += offset;
				digits[4] = EQUAL;
			}
			offset *= 3;
			if (value(i + 1, j, k + 1) < isolevel) {
				cubeindex += 2 * offset;
				digits[5] = POSITIVE;
			}
			else if (value(i + 1, j, k + 1) == isolevel) {
				cubeindex += offset;
				digits[5] = EQUAL;
			}
			offset *= 3;
			if (value(i, j + 1, k + 1) < isolevel) {
				cubeindex += 2 * offset;
				digits[6] = POSITIVE;
			}
			else if (value(i, j + 1, k + 1) == isolevel) {
				cubeindex += offset;
				digits[6] = EQUAL;
			}
			offset *= 3;
			if (value(i + 1, j + 1, k + 1) < isolevel) {
				cubeindex += 2 * offset;
				digits[7] = POSITIVE;
			}
			else if (value(i + 1, j + 1, k + 1) == isolevel) {
				cubeindex += offset;
				digits[7] = EQUAL;
			}

		}
		else {
			if (value(i, j, k) > isolevel) {
				cubeindex += 2 * offset;
				digits[0] = POSITIVE;
			}
			else if (value(i, j, k) == isolevel) {
				cubeindex += offset;
				digits[0] = EQUAL;
			}
			offset *= 3;
			if (value(i + 1, j, k) > isolevel) {
				cubeindex += 2 * offset;
				digits[1] = POSITIVE;
			}
			else if (value(i + 1, j, k) == isolevel) {
				cubeindex += offset;
				digits[1] = EQUAL;
			}
			offset *= 3;
			if (value(i, j + 1, k) > isolevel) {
				cubeindex += 2 * offset;
				digits[2] = POSITIVE;
			}
			else if (value(i, j + 1, k) == isolevel) {
				cubeindex += offset;
				digits[2] = EQUAL;
			}
			offset *= 3;
			if (value(i + 1, j + 1, k) > isolevel) {
				cubeindex += 2 * offset;
				digits[3] = POSITIVE;
			}
			else if (value(i + 1, j + 1, k) == isolevel) {
				cubeindex += offset;
				digits[3] = EQUAL;
			}
			offset *= 3;
			if (value(i, j, k + 1) > isolevel) {
				cubeindex += 2 * offset;
				digits[4] = POSITIVE;
			}
			else if (value(i, j, k + 1) == isolevel) {
				cubeindex += offset;
				digits[4] = EQUAL;
			}
			offset *= 3;
			if (value(i + 1, j, k + 1) > isolevel) {
				cubeindex += 2 * offset;
				digits[5] = POSITIVE;
			}
			else if (value(i + 1, j, k + 1) == isolevel) {
				cubeindex += offset;
				digits[5] = EQUAL;
			}
			offset *= 3;
			if (value(i, j + 1, k + 1) > isolevel) {
				cubeindex += 2 * offset;
				digits[6] = POSITIVE;
			}
			else if (value(i, j + 1, k + 1) == isolevel) {
				cubeindex += offset;
				digits[6] = EQUAL;
			}
			offset *= 3;
			if (value(i + 1, j + 1, k + 1) > isolevel) {
				cubeindex += 2 * offset;
				digits[7] = POSITIVE;
			}
			else if (value(i + 1, j + 1, k + 1) == isolevel) {
				cubeindex += offset;
				digits[7] = EQUAL;
			}

		}
		return cubeindex;

	}

	HE_Vertex getIsoVertex(final int isopointindex, final int i, final int j,
			final int k, final WB_Point offset, final boolean dummyrun) {
		if (isovertices[isopointindex][0] == ONVERTEX) {
			switch (isovertices[isopointindex][1]) {
			case 0:
				return vertex(i, j, k, offset);
			case 1:
				return vertex(i + 1, j, k, offset);
			case 2:
				return vertex(i, j + 1, k, offset);
			case 3:
				return vertex(i + 1, j + 1, k, offset);
			case 4:
				return vertex(i, j, k + 1, offset);
			case 5:
				return vertex(i + 1, j, k + 1, offset);
			case 6:
				return vertex(i, j + 1, k + 1, offset);
			case 7:
				return vertex(i + 1, j + 1, k + 1, offset);
			default:
				return null;
			}
		}
		else if (isovertices[isopointindex][0] == ONEDGE) {
			switch (isovertices[isopointindex][1]) {

			case 0:
				return xedge(i, j, k, offset, dummyrun);
			case 1:
				return yedge(i, j, k, offset, dummyrun);
			case 2:
				return yedge(i + 1, j, k, offset, dummyrun);
			case 3:
				return xedge(i, j + 1, k, offset, dummyrun);
			case 4:
				return zedge(i, j, k, offset, dummyrun);
			case 5:
				return zedge(i + 1, j, k, offset, dummyrun);
			case 6:
				return zedge(i, j + 1, k, offset, dummyrun);
			case 7:
				return zedge(i + 1, j + 1, k, offset, dummyrun);
			case 8:
				return xedge(i, j, k + 1, offset, dummyrun);
			case 9:
				return yedge(i, j, k + 1, offset, dummyrun);
			case 10:
				return yedge(i + 1, j, k + 1, offset, dummyrun);
			case 11:
				return xedge(i, j + 1, k + 1, offset, dummyrun);
			default:
				return null;
			}
		}
		return null;
	}

	private HE_Vertex vertex(final int i, final int j, final int k,
			final WB_Point offset) {
		HE_Vertex vertex = vertices.get(index(i, j, k));
		if (vertex != null) {
			return vertex;
		}

		final WB_Point p0 = new WB_Point(i * dx, j * dy, k * dz);

		vertex = new HE_Vertex(p0.addSelf(offset));
		mesh.add(vertex);
		vertices.put(index(i, j, k), vertex);
		return vertex;
	}

	/**
	 * Xedge.
	 *
	 * @param i
	 *            i: -1 .. resx+1
	 * @param j
	 *            j: -1 .. resy+1
	 * @param k
	 *            k: -1 .. resz+1
	 * @return edge vertex
	 */
	private HE_Vertex xedge(final int i, final int j, final int k,
			final WB_Point offset, final boolean dummyrun) {

		final WB_Point p0 = new WB_Point(i * dx, j * dy, k * dz);
		final WB_Point p1 = new WB_Point(i * dx + dx, j * dy, k * dz);
		final double val0 = value(i, j, k);
		final double val1 = value(i + 1, j, k);
		double mu;
		if (dummyrun) {
			mu = (isolevel - val0) / (val1 - val0);
			if (mu < gamma) {
				VertexRemap vr = vertexremaps.get(index(i, j, k));
				if (vr == null) {
					vr = new VertexRemap();
					vr.closestd = mu * dx;
					vr.i = i;
					vr.j = j;
					vr.k = k;
					vr.originalvalue = values[i][j][k];
					vr.p = interp(isolevel, p0, p1, val0, val1).addSelf(offset);
					vr.snapvertex = vertex(i, j, k, offset);
					vertexremaps.put(index(i, j, k), vr);
				}
				else {
					if (vr.closestd > mu * dx) {
						vr.closestd = mu * dx;
						vr.i = i;
						vr.j = j;
						vr.k = k;
						vr.originalvalue = values[i][j][k];
						vr.p = interp(isolevel, p0, p1, val0, val1).addSelf(
								offset);
						vr.snapvertex = vertex(i, j, k, offset);
					}
				}
			}
			else if (mu > 1 - gamma) {
				VertexRemap vr = vertexremaps.get(index(i + 1, j, k));
				if (vr == null) {
					vr = new VertexRemap();
					vr.closestd = (1 - mu) * dx;
					vr.i = i + 1;
					vr.j = j;
					vr.k = k;
					vr.originalvalue = values[i + 1][j][k];
					vr.p = interp(isolevel, p0, p1, val0, val1).addSelf(offset);
					vr.snapvertex = vertex(i + 1, j, k, offset);
					vertexremaps.put(index(i + 1, j, k), vr);
				}
				else {
					if (vr.closestd > (1 - mu) * dx) {
						vr.closestd = (1 - mu) * dx;
						vr.i = i + 1;
						vr.j = j;
						vr.k = k;
						vr.originalvalue = values[i + 1][j][k];
						vr.p = interp(isolevel, p0, p1, val0, val1).addSelf(
								offset);
						vr.snapvertex = vertex(i + 1, j, k, offset);
					}
				}
			}
			return null;
		}

		HE_Vertex xedge = xedges.get(index(i, j, k));
		if (xedge != null) {
			return xedge;
		}
		xedge = new HE_Vertex(interp(isolevel, p0, p1, val0, val1));
		xedge.getPoint().addSelf(offset);
		if (!dummyrun) {
			mesh.add(xedge);
		}
		xedges.put(index(i, j, k), xedge);
		return xedge;
	}

	/**
	 * Yedge.
	 *
	 * @param i
	 *            i: -1 .. resx+1
	 * @param j
	 *            j: -1 .. resy+1
	 * @param k
	 *            k: -1 .. resz+1
	 * @return edge vertex
	 */
	private HE_Vertex yedge(final int i, final int j, final int k,
			final WB_Point offset, final boolean dummyrun) {
		HE_Vertex yedge = yedges.get(index(i, j, k));
		if (yedge != null) {
			return yedge;
		}

		final WB_Point p0 = new WB_Point(i * dx, j * dy, k * dz);
		final WB_Point p1 = new WB_Point(i * dx, j * dy + dy, k * dz);
		final double val0 = value(i, j, k);
		final double val1 = value(i, j + 1, k);
		double mu;
		if (dummyrun) {
			mu = (isolevel - val0) / (val1 - val0);
			if (mu < gamma) {
				VertexRemap vr = vertexremaps.get(index(i, j, k));
				if (vr == null) {
					vr = new VertexRemap();
					vr.closestd = mu * dy;
					vr.i = i;
					vr.j = j;
					vr.k = k;
					vr.originalvalue = values[i][j][k];
					vr.p = interp(isolevel, p0, p1, val0, val1).addSelf(offset);
					vr.snapvertex = vertex(i, j, k, offset);
					vertexremaps.put(index(i, j, k), vr);
				}
				else {
					if (vr.closestd > mu * dy) {
						vr.closestd = mu * dy;
						vr.i = i;
						vr.j = j;
						vr.k = k;
						vr.originalvalue = values[i][j][k];
						vr.p = interp(isolevel, p0, p1, val0, val1).addSelf(
								offset);
						vr.snapvertex = vertex(i, j, k, offset);
					}
				}
			}
			else if (mu > 1 - gamma) {
				VertexRemap vr = vertexremaps.get(index(i, j + 1, k));
				if (vr == null) {
					vr = new VertexRemap();
					vr.closestd = (1 - mu) * dy;
					vr.i = i;
					vr.j = j + 1;
					vr.k = k;
					vr.originalvalue = values[i][j + 1][k];
					vr.p = interp(isolevel, p0, p1, val0, val1).addSelf(offset);
					vr.snapvertex = vertex(i, j + 1, k, offset);
					vertexremaps.put(index(i, j + 1, k), vr);
				}
				else {
					if (vr.closestd > (1 - mu) * dy) {
						vr.closestd = (1 - mu) * dy;
						vr.i = i;
						vr.j = j + 1;
						vr.k = k;
						vr.originalvalue = values[i][j + 1][k];
						vr.p = interp(isolevel, p0, p1, val0, val1).addSelf(
								offset);
						vr.snapvertex = vertex(i, j + 1, k, offset);
					}
				}
			}
			return null;
		}

		yedge = new HE_Vertex(interp(isolevel, p0, p1, val0, val1));
		yedge.getPoint().addSelf(offset);
		if (!dummyrun) {
			mesh.add(yedge);
		}
		yedges.put(index(i, j, k), yedge);
		return yedge;
	}

	/**
	 * Zedge.
	 *
	 * @param i
	 *            i: -1 .. resx+1
	 * @param j
	 *            j: -1 .. resy+1
	 * @param k
	 *            k: -1 .. resz+1
	 * @return edge vertex
	 */
	private HE_Vertex zedge(final int i, final int j, final int k,
			final WB_Point offset, final boolean dummyrun) {
		HE_Vertex zedge = zedges.get(index(i, j, k));
		if (zedge != null) {
			return zedge;
		}

		final WB_Point p0 = new WB_Point(i * dx, j * dy, k * dz);
		final WB_Point p1 = new WB_Point(i * dx, j * dy, k * dz + dz);
		final double val0 = value(i, j, k);
		final double val1 = value(i, j, k + 1);

		double mu;
		if (dummyrun) {
			mu = (isolevel - val0) / (val1 - val0);
			if (mu < gamma) {
				VertexRemap vr = vertexremaps.get(index(i, j, k));
				if (vr == null) {
					vr = new VertexRemap();
					vr.closestd = mu * dz;
					vr.i = i;
					vr.j = j;
					vr.k = k;
					vr.originalvalue = values[i][j][k];
					vr.p = interp(isolevel, p0, p1, val0, val1).addSelf(offset);
					vr.snapvertex = vertex(i, j, k, offset);
					vertexremaps.put(index(i, j, k), vr);
				}
				else {
					if (vr.closestd > mu * dz) {
						vr.closestd = mu * dz;
						vr.i = i;
						vr.j = j;
						vr.k = k;
						vr.originalvalue = values[i][j][k];
						vr.p = interp(isolevel, p0, p1, val0, val1).addSelf(
								offset);
						vr.snapvertex = vertex(i, j, k, offset);
					}
				}
			}
			else if (mu > 1 - gamma) {
				VertexRemap vr = vertexremaps.get(index(i, j, k + 1));
				if (vr == null) {
					vr = new VertexRemap();
					vr.closestd = (1 - mu) * dz;
					vr.i = i;
					vr.j = j;
					vr.k = k + 1;
					vr.originalvalue = values[i][j][k + 1];
					vr.p = interp(isolevel, p0, p1, val0, val1).addSelf(offset);
					vr.snapvertex = vertex(i, j, k + 1, offset);
					vertexremaps.put(index(i, j, k + 1), vr);
				}
				else {
					if (vr.closestd > (1 - mu) * dz) {
						vr.closestd = (1 - mu) * dz;
						vr.i = i;
						vr.j = j;
						vr.k = k + 1;
						vr.originalvalue = values[i][j][k + 1];
						vr.p = interp(isolevel, p0, p1, val0, val1).addSelf(
								offset);
						vr.snapvertex = vertex(i, j, k + 1, offset);
					}
				}
			}
			return null;
		}
		zedge = new HE_Vertex(interp(isolevel, p0, p1, val0, val1));
		zedge.getPoint().addSelf(offset);
		if (!dummyrun) {
			mesh.add(zedge);
		}
		zedges.put(index(i, j, k), zedge);
		return zedge;

	}

	/*
	 * Linearly interpolate the position where an isosurface cuts an edge
	 * between two vertices, each with their own scalar value
	 */
	/**
	 * Interp.
	 *
	 * @param isolevel
	 *            the isolevel
	 * @param p1
	 *            the p1
	 * @param p2
	 *            the p2
	 * @param valp1
	 *            the valp1
	 * @param valp2
	 *            the valp2
	 * @return the h e_ vertex
	 */
	private WB_Point interp(final double isolevel, final WB_Point p1,
			final WB_Point p2, final double valp1, final double valp2) {

		double mu;

		if (WB_Epsilon.isEqualAbs(isolevel, valp1)) {
			return p1;
		}
		if (WB_Epsilon.isEqualAbs(isolevel, valp2)) {
			return p2;
		}
		if (WB_Epsilon.isEqualAbs(valp1, valp2)) {
			return p1;
		}
		mu = (isolevel - valp1) / (valp2 - valp1);

		return new WB_Point(p1.xd() + mu * (p2.xd() - p1.xd()), p1.yd() + mu
				* (p2.yd() - p1.yd()), p1.zd() + mu * (p2.zd() - p1.zd()));
	}

	private int index(final int i, final int j, final int k) {
		return ((i + 1) + (resx + 2) * (j + 1) + (resx + 2) * (resy + 2)
				* (k + 1));

	}

	/**
	 * Value.
	 *
	 * @param i
	 *            the i
	 * @param j
	 *            the j
	 * @param k
	 *            the k
	 * @return the double
	 */
	private double value(final int i, final int j, final int k) {
		if (Double.isNaN(boundary)) { // if no boundary is set i,j,k should
			// always be between o and resx,rey,resz
			return values[i][j][k];
		}
		if ((i < 0) || (j < 0) || (k < 0) || (i > resx) || (j > resy)
				|| (k > resz)) {
			return (invert) ? -boundary : boundary;
		}
		return values[i][j][k];

	}

	class VertexRemap {
		int i, j, k;
		double closestd;
		WB_Point p;
		double originalvalue;
		HE_Vertex snapvertex;
	}

}

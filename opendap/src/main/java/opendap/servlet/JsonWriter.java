package opendap.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Vector;

import opendap.dap.*;
import opendap.dap.parsers.ParseException;
import opendap.servers.ServerMethods;

public class JsonWriter {

	private PrintWriter pw = null;

	@SuppressWarnings("unchecked")
	public void asJSON(PrintWriter pw, DDS dds, Object specialO, ReqState rs)
			throws NoSuchVariableException, IOException,
			InvalidDimensionException {
		this.pw = pw;
		printConstrainedJSON(dds, (GuardedDataset) specialO, rs);
//		pw.println(";");
//		pw.println("}, \n \"variables\": { \n");
//		for (@SuppressWarnings("rawtypes")
//		Enumeration e = dds.getVariables(); e.hasMoreElements();) {
//			BaseType bt = (BaseType) e.nextElement();
//			// System.err.println("check: "+bt.getLongName()+" = "+((ServerMethods)
//			// bt).isProject());
//			ServerMethods sm = (ServerMethods) bt;
//			if (sm.isProject()) {
//				// bt.printJSON(os);
//			}
//		}
//		pw.print("} ");
//		pw.println(";");
		pw.print("{\"" + dds.getClearName() + "\": {\n");
		Enumeration<BaseType> e = (Enumeration<BaseType>) dds.getVariables();
		while (e.hasMoreElements()) {
			write(e.nextElement());
			if (e.hasMoreElements())
				pw.print(", \n");
		}
		pw.print("}\n}");
	}

	@SuppressWarnings("unchecked")
	private void write(BaseType type) throws NoSuchVariableException,
			IOException {
		String name = type.getClearName();

		if (type instanceof DSequence) {
			pw.print("\"" + name + "\": {\n");
			while (((ServerMethods) type).read(name, null)) {
				Enumeration<BaseType> e = (Enumeration<BaseType>) ((DSequence) type)
						.getVariables();
				while (e.hasMoreElements()) {
					write(e.nextElement());
					if (e.hasMoreElements())
						pw.print(", \n");
				}
			}
			pw.print("}\n");
		} else {
			if (!((ServerMethods) type).isRead())
				((ServerMethods) type).read(name, null);
			if (type instanceof DArray) {
				writeArray((DArray) type);
			} else if (type instanceof DGrid) {
				writeGrid((DGrid) type); // TODO: Copied from asciiWriter, but
											// not tested!
			} else if (type instanceof DSequence) {
				writeSequence((DSequence) type); // TODO: Copied from
													// asciiWriter, but not
													// tested!
			} else if (type instanceof DStructure) {
				writeStructure((DStructure) type); // TODO: Copied from
													// asciiWriter, but not
													// tested!
			} else {
				writePrimitive(type);
			}
		}
	}

	private void writePrimitive(BaseType data) {
		if (data instanceof DString) {
			String s = ((DString) data).getValue();
			if ((s.length() > 0) && s.charAt(s.length() - 1) == ((char) 0)) {
				char cArray[] = s.toCharArray();
				s = new String(cArray, 0, cArray.length - 1);
			}
			pw.print("\"" + s + "\"");
		} else if (data instanceof DFloat32)
			pw.print((new Float(((DFloat32) data).getValue())).toString());
		else if (data instanceof DFloat64)
			pw.print((new Double(((DFloat64) data).getValue())).toString());
		else if (data instanceof DUInt32)
			pw.print((new Long(((DUInt32) data).getValue()
					& ((long) 0xFFFFFFFF))).toString());
		else if (data instanceof DUInt16)
			pw.print((new Integer(((DUInt16) data).getValue() & 0xFFFF))
					.toString());
		else if (data instanceof DInt32)
			pw.print((new Integer(((DInt32) data).getValue())).toString());
		else if (data instanceof DInt16)
			pw.print((new Short(((DInt16) data).getValue())).toString());
		else if (data instanceof DByte)
			pw.print((new Integer(((DByte) data).getValue() & 0xFF)).toString());
		else
			pw.print("Not implemented type = " + data.getTypeName() + " "
					+ data.getEncodedName() + "\n");
	}

	private void writeArray(DArray data) {
		pw.print("\"" + data.getClearName() + "\": ");
		writeArrayDimension(data, data.numDimensions(), 0);
	}

	private int writeArrayDimension(DArray data, int dim, int offset) {
		int length = 0;
		try {
			length = data.getDimension(data.numDimensions() - dim).getSize();
		} catch (InvalidDimensionException e) {
			e.printStackTrace();
		}
		if (dim > 1) {
			for (int n = 0; n < length; n++) {
				if (n > 0)
					pw.print(", \n");
				pw.print("[");
				offset = writeArrayDimension(data, dim - 1, offset);
				pw.print("]");
			}
		} else {
			PrimitiveVector pv = data.getPrimitiveVector();
			for (int n = 0; n < length; n++) {
				if (n > 0)
					pw.print(", ");
				if (pv instanceof BaseTypePrimitiveVector) {
					BaseType bt = ((BaseTypePrimitiveVector) pv)
							.getValue(offset++);
					writePrimitive(bt);
				} else {
					pv.printSingleVal(pw, offset++);
				}
			}
		}
		return offset;
	}

	@SuppressWarnings("unchecked")
	private void writeGrid(DGrid data) throws NoSuchVariableException,
			IOException {
		pw.print("\"" + data.getClearName() + "\": ");
		writeEnumeration(data.getVariables());
	}

	@SuppressWarnings("unchecked")
	private void writeSequence(DSequence data) throws NoSuchVariableException,
			IOException {
		pw.print("\"" + data.getClearName() + "\": ");

		pw.print("{");
		for (int row = 0; row < data.getRowCount(); row++) {
			if (row > 0)
				pw.print(", ");
			Vector<BaseType> v = data.getRow(row);
			writeEnumeration(v.elements());
		}
		pw.print("}");
	}

	@SuppressWarnings("unchecked")
	private void writeStructure(DStructure data)
			throws NoSuchVariableException, IOException {
		pw.print("\"" + data.getClearName() + "\": ");
		writeEnumeration(data.getVariables());
	}

	private void writeEnumeration(Enumeration<BaseType> e)
			throws NoSuchVariableException, IOException {
		pw.print("[");
		while (e.hasMoreElements()) {
			BaseType datum = e.nextElement();
			write(datum);
			if (e.hasMoreElements())
				pw.print(", ");
		}
		pw.print("]\n");
	}

	public void printConstrainedJSON(DDS dds, GuardedDataset ds, ReqState rs) {
		pw.println("{");
		pw.println("\"data_url\":\"" + rs.getRequestURL().toString() + rs.getConstraintExpression() + "\",");
		if (dds.getEncodedName() != null)
			pw.println("\"dataset\":\"" + dds.getEncodedName() + "\",");
		pw.println("\"global_attributes\":{");
		try {
			DAS myDAS = ds.getDAS();
			myDAS.printGlobalJSON(pw);
		} catch (ParseException e1) {
			e1.printStackTrace();
		} catch (DAP2Exception e1) {
			e1.printStackTrace();
		}
	}
}

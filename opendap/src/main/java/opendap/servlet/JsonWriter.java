package opendap.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;

import opendap.dap.*;
import opendap.dap.parsers.ParseException;
import opendap.servers.ServerMethods;

public class JsonWriter {

	private PrintWriter pw = null;

	@SuppressWarnings("rawtypes")
	public void toJSON(PrintWriter pw, DDS dds, Object specialO, ReqState rs)
			throws NoSuchVariableException, IOException,
			InvalidDimensionException {
		this.pw = pw;
		printGlobalJSON(dds, (GuardedDataset) specialO, rs);
		Enumeration e = dds.getVariables();
		if (e.hasMoreElements()) {
			pw.println(",");
			pw.println("\"variables\":{");
		}		
		
		ArrayList<BaseType> projected = new ArrayList<BaseType>();
		while (e.hasMoreElements()) {
			BaseType bt = (BaseType) e.nextElement();
			if (!((ServerMethods) bt).isProject())
				continue;
			projected.add(bt);
		}
		for (BaseType bt : projected) {
			writeJSON(bt, dds, specialO);
			if (projected.indexOf(bt) != projected.size() - 1)
				pw.println(",");
		}
		pw.println("\n}\n}");
	}

	private void writeJSON(BaseType bt, DDS dds, Object specialO)
			throws NoSuchVariableException, IOException {
		String datasetName = dds.getEncodedName();
		bt.printJSON(pw, null, true, false);
		bt.printAttributesJSON(pw);
		pw.println("\"data\":[");
		if (bt instanceof DSequence) {
			DSequence dseq = (DSequence) bt;
			boolean moreToRead = true;
			while (moreToRead) {
				moreToRead = ((ServerMethods) bt).read(datasetName, specialO);
				for (Enumeration em = dseq.getVariables(); em.hasMoreElements();) {
					BaseType member = (BaseType) em.nextElement();
					writeJSON(member, dds, specialO);
				}
			}

		} else {
			if (!((ServerMethods) bt).isRead()) // make sure data is in memory,
												// but don't read it twice!
				((ServerMethods) bt).read(datasetName, specialO);
			try {
				toJSON(bt);
			} catch (NoSuchAttributeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		pw.println("]");
		pw.print("}");
	}

	public void toJSON(BaseType dtype) throws NoSuchAttributeException {
		toJSON(dtype, false, null, true);
	}

	private void toJSON(BaseType dtype, boolean addName, String rootName,
			boolean newLine) throws NoSuchAttributeException {
		if (dtype instanceof DArray)
			showArray((DArray) dtype, pw, addName, rootName, newLine);
//		else if (dtype instanceof DGrid)
//			showGrid((DGrid) dtype, pw, addName, rootName, newLine);
//		else if (dtype instanceof DSequence)
//			showSequence((DSequence) dtype, pw, addName, rootName, newLine);
//		else if (dtype instanceof DStructure)
//			showStructure((DStructure) dtype, pw, addName, rootName, newLine);
//		else
//			showPrimitive(dtype, pw, addName, rootName, newLine);
	}

	private void showArray(DArray data, PrintWriter pw, boolean addName,
			String rootName, boolean newLine) throws NoSuchAttributeException {

		if (addName) {
			pw.print(toJSONFlatName(data, rootName));
			pw.print("\n");
		}

		int dims = data.numDimensions();
		int shape[] = new int[dims];
		int i = 0;
		for (Enumeration e = data.getDimensions(); e.hasMoreElements();) {
			DArrayDimension d = (DArrayDimension) e.nextElement();
			shape[i++] = d.getSize();
		}
		jsonArray(data, pw, addName, "", 0, dims, shape, 0);

		if (newLine)
			pw.println();
	}

	private int jsonArray(DArray data, PrintWriter os, boolean addName,
			String label, int index, int dims, int shape[], int offset) throws NoSuchAttributeException {

        if (dims == 1) {

            if (addName)
                os.print(label);

            for (int i = 0; i < shape[offset]; i++) {
                PrimitiveVector pv = data.getPrimitiveVector();
                if (pv instanceof BaseTypePrimitiveVector) {
                    BaseType bt = ((BaseTypePrimitiveVector) pv).getValue(index++);
                    if ((i > 0) && (bt instanceof DString))
                        os.print(", ");
                    toJSON(bt, false, null, false);
                } else {
                    os.print("[");
                    pv.printSingleVal(os, index++);
                    os.print("]");
                    if (i >= 0 && i < shape[offset] - 1)
                    	os.println(", ");
                }

            }
            if (addName) os.print("\n");
            return index;

        } else {

            for (int i = 0; i < shape[offset]; i++) {
                String s = label + "[" + i + "]";
                if ((dims - 1) == 1)
                    s += ", ";
                index = jsonArray(data, os, addName, s, index, dims - 1, shape, offset + 1);
                if (i >= 0 && i < shape[offset] - 1)
                	os.println(",");
            }
            return index;
        }
	}

	@SuppressWarnings("rawtypes")
	private String toJSONFlatName(BaseType data, String rootName) throws NoSuchAttributeException {
		String result;
		boolean hasRootName = false;
		StringBuffer s = new StringBuffer();
		if (rootName != null) {
			s.append("\"").append(rootName).append(".");
			hasRootName = true;
		}
		if (hasRootName)
			s.append(data.getEncodedName());
		else 
			s.append("\"" + data.getEncodedName());

		if (data instanceof DArray) {
			DArray darray = (DArray) data;
			PrimitiveVector pv = darray.getPrimitiveVector();
			if (pv instanceof BaseTypePrimitiveVector) {
				BaseType bt = ((BaseTypePrimitiveVector) pv).getValue(0);
				if (bt instanceof DString) {
					for (Enumeration e = darray.getDimensions(); e
							.hasMoreElements();) {
						DArrayDimension d = (DArrayDimension) e.nextElement();
						s.append("[").append(d.getSize()).append("]");
					}
					result = s.toString();
				} else {
					result = toJSONFlatName(bt, s.toString());
				}

			} else {
				for (Enumeration e = darray.getDimensions(); e
						.hasMoreElements();) {
					DArrayDimension d = (DArrayDimension) e.nextElement();
					s.append("[").append(d.getSize()).append("]");
				}
				result = s.toString();
			}
			return result;

		} else if (data instanceof DSequence) {
			DSequence dseq = (DSequence) data;
			s.setLength(0);

			boolean firstPass = true;
			for (int row = 0; row < dseq.getRowCount(); row++) {
				Vector v = dseq.getRow(row);
				for (Enumeration e2 = v.elements(); e2.hasMoreElements();) {
					BaseType ta = (BaseType) e2.nextElement();
					if (!firstPass)
						s.append(", ");
					firstPass = false;

					s.append(toJSONFlatName(ta, rootName));
				}
				break;
			}

		} else if (data instanceof DConstructor) {
			DConstructor dcon = (DConstructor) data;
			s.setLength(0);

			boolean firstPass = true;
			Enumeration e = dcon.getVariables();
			while (e.hasMoreElements()) {
				BaseType ta = (BaseType) e.nextElement();
				if (!firstPass)
					s.append(", ");
				firstPass = false;

				s.append(toJSONFlatName(ta, rootName));
			}
		}

		return s.toString();
	}

//	private void writePrimitive(BaseType data) {
//		if (data instanceof DString) {
//			String s = ((DString) data).getValue();
//			if ((s.length() > 0) && s.charAt(s.length() - 1) == ((char) 0)) {
//				char cArray[] = s.toCharArray();
//				s = new String(cArray, 0, cArray.length - 1);
//			}
//			pw.print("\"" + s + "\"");
//		} else if (data instanceof DFloat32)
//			pw.print((new Float(((DFloat32) data).getValue())).toString());
//		else if (data instanceof DFloat64)
//			pw.print((new Double(((DFloat64) data).getValue())).toString());
//		else if (data instanceof DUInt32)
//			pw.print((new Long(((DUInt32) data).getValue()
//					& ((long) 0xFFFFFFFF))).toString());
//		else if (data instanceof DUInt16)
//			pw.print((new Integer(((DUInt16) data).getValue() & 0xFFFF))
//					.toString());
//		else if (data instanceof DInt32)
//			pw.print((new Integer(((DInt32) data).getValue())).toString());
//		else if (data instanceof DInt16)
//			pw.print((new Short(((DInt16) data).getValue())).toString());
//		else if (data instanceof DByte)
//			pw.print((new Integer(((DByte) data).getValue() & 0xFF)).toString());
//		else
//			pw.print("Not implemented type = " + data.getTypeName() + " "
//					+ data.getEncodedName() + "\n");
//	}
//
//	private void writeArray(DArray data) {
//
//		writeArrayDimension(data, data.numDimensions(), 0);
//	}
//
//	private int writeArrayDimension(DArray data, int dim, int offset) {
//		int length = 0;
//		try {
//			length = data.getDimension(data.numDimensions() - dim).getSize();
//		} catch (InvalidDimensionException e) {
//			e.printStackTrace();
//		}
//		if (dim > 1) {
//			for (int n = 0; n < length; n++) {
//				if (n > 0)
//					pw.print(", \n");
//				pw.print("[");
//				offset = writeArrayDimension(data, dim - 1, offset);
//				pw.print("]");
//			}
//		} else {
//			PrimitiveVector pv = data.getPrimitiveVector();
//			for (int n = 0; n < length; n++) {
//				if (n > 0)
//					pw.print(", ");
//				if (pv instanceof BaseTypePrimitiveVector) {
//					BaseType bt = ((BaseTypePrimitiveVector) pv)
//							.getValue(offset++);
//					writePrimitive(bt);
//				} else {
//					pv.printSingleVal(pw, offset++);
//				}
//			}
//		}
//		return offset;
//	}
//
//	@SuppressWarnings("unchecked")
//	private void writeGrid(DGrid data) throws NoSuchVariableException,
//			IOException {
//		pw.print("\"" + data.getClearName() + "\": ");
//		writeEnumeration(data.getVariables());
//	}
//
//	@SuppressWarnings("unchecked")
//	private void writeSequence(DSequence data) throws NoSuchVariableException,
//			IOException {
//		pw.print("\"" + data.getClearName() + "\": ");
//
//		pw.print("{");
//		for (int row = 0; row < data.getRowCount(); row++) {
//			if (row > 0)
//				pw.print(", ");
//			Vector<BaseType> v = data.getRow(row);
//			writeEnumeration(v.elements());
//		}
//		pw.print("}");
//	}
//
//	@SuppressWarnings("unchecked")
//	private void writeStructure(DStructure data)
//			throws NoSuchVariableException, IOException {
//		pw.print("\"" + data.getClearName() + "\": ");
//		writeEnumeration(data.getVariables());
//	}
//
//	private void writeEnumeration(Enumeration<BaseType> e)
//			throws NoSuchVariableException, IOException {
//		pw.print("[");
//		while (e.hasMoreElements()) {
//			BaseType datum = e.nextElement();
//			writeJSON(datum);
//			if (e.hasMoreElements())
//				pw.print(", ");
//		}
//		pw.print("]\n");
//	}

	public void printGlobalJSON(DDS dds, GuardedDataset ds, ReqState rs) {
		pw.println("{");
		pw.println("\"data_url\":\"" + rs.getRequestURL().toString() + "?"
				+ rs.getConstraintExpression() + "\",");
		if (dds.getEncodedName() != null)
			pw.println("\"dataset\":\"" + dds.getEncodedName() + "\",");
		pw.println("\"global_attributes\":");
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

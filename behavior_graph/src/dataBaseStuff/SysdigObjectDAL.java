package dataBaseStuff;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.StringJoiner;

import classes.*;
import exceptions.HighFieldNumberException;
import exceptions.LowFieldNumberException;
import helpers.Configurations;

public class SysdigObjectDAL {
	private Field[] ClassFields;
	private String InsertTemplate;

	public SysdigObjectDAL(boolean shortList) throws NoSuchFieldException, SecurityException {
		// region Set Class fields
		Class<?> c = new SysdigRecordObject().getClass();
		if (!shortList)
			ClassFields = c.getFields();
		else {
			ArrayList<Field> temp = new ArrayList<Field>();
			for (String pick : Configurations.getShortFieldList())
				temp.add(c.getField(pick));

			ClassFields = temp.toArray(new Field[temp.size()]);
		}  

		// endregion

		// region create insert template
		String Keys = " Insert into SysdigOutPut ( ";
		String Values = "";
		int FirstLen = Keys.length();
		for (Field pick : ClassFields) {
			if (Keys.length() != FirstLen)
				Keys += ",";

			Keys += pick.getName();
		}
		Keys += " ) values ( %1$s ) ;\r\n ";
		InsertTemplate = Keys;
		// endregion

	}

	private static String big_query = "";
	private static int big_query_counter = 0;
	private static StringJoiner items = new StringJoiner(" ");

	/**
	 * Insets the record into the Database
	 * 
	 * @param inp
	 *            the object to be inseted
	 */
	public void Insert(SysdigRecordObject inp) {
		String Query = "";
		try {
			String PickString = "";
			for (Field pick : ClassFields) {
				Object temp = pick.get(inp);
				if (temp == null)
					temp = "";

				if (PickString.length() != 0)
					PickString += " , ";

				PickString += "'" + temp.toString().replace("'", "''") + "'";
				// PickString +="\""+ temp.toString().replace("\"",
				// "\\\"")+"\"";
			}
			DataBaseLayer DL = new DataBaseLayer();
			Query = String.format(InsertTemplate, PickString);

			items.add(Query);
			// big_query += Query;
			big_query_counter++;

			if (big_query_counter % 1000 == 0) {
				// StringJoiner j = new StringJoiner(' ');
				DL.runUpdateQuery(items.toString());
				big_query = "";
				System.out.println("Running!");
			}
		} catch (Exception ex) {
			System.out.println(Query);
			System.out.println("Class Not found!");
		}
	}

	/**
	 * Loads the SysdigRecordObject from a sql resultset
	 * 
	 * @param input
	 *            the resultset to load the object from
	 * @return created sysdoigobject based on the row
	 * @throws SQLException
	 *             if row is not well formed
	 * @throws IllegalArgumentException
	 *             is value is not compatible with the record's expectation
	 * @throws IllegalAccessException
	 *             should not be thrown!
	 */
	public SysdigRecordObject LoadFromResultSet(ResultSet input)
			throws SQLException, IllegalArgumentException, IllegalAccessException {
		SysdigRecordObject ret = new SysdigRecordObject();

		for (Field pick : ClassFields) {

			String value = input.getString(pick.getName());
			if (!value.isEmpty())
				pick.set(ret, value);
		}

		return ret;
	}

	public void flushRows() {
		try {
			DataBaseLayer DL = new DataBaseLayer();
			DL.runUpdateQuery(items.toString());
		} catch (Exception ex) {
			System.out.println(big_query);
			System.out.println("Class Not found!");
		}
	}

	/**
	 * Reads the line of input and tryes to create a sysdig reciord based on the
	 * input text. number of fields in the row should match
	 * 
	 * @param inp
	 * @return
	 * @throws NumberFormatException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public SysdigRecordObject GetObjectFromTextLine(String inp)
			throws LowFieldNumberException, HighFieldNumberException, IllegalArgumentException, IllegalAccessException {
		SysdigRecordObject ret = new SysdigRecordObject();

		String tokens[] = inp.split("=&amin&=");

		if (tokens.length < ClassFields.length) {
			throw new LowFieldNumberException("Error! number of fields do not match!" + tokens.length + " instead of "
					+ ClassFields.length + " : " + inp);
			// System.out.println("Error! number of fields do not match!" +
			// tokens.length + " instead of "+ ClassFields.length);
		} else if (tokens.length > ClassFields.length) {
			throw new HighFieldNumberException("Error! number of fields do not match!" + tokens.length + " instead of "
					+ ClassFields.length + " : " + inp);

		}
		for (int index = 0; index < tokens.length; index++)
			ClassFields[index].set(ret, tokens[index].trim());

		// ret.fd_num = ret.fd_name + "|" + ret.fd_num;
		// ret.proc_pid = ret.proc_pid+"|" + ret.proc_name;
		return ret;
	}

	// public SysdigRecordObject GetObjectFromAndroidTextLine(String inp)
	// throws InvalidFormatException, IllegalArgumentException,
	// IllegalAccessException {
	// SysdigRecordObject ret = new SysdigRecordObject();
	//
	// String tokens[] = inp.split(",");
	//
	// if (tokens.length != ClassFields.length) {
	// throw new InvalidFormatException(
	// "Error! number of fields do not match!" + tokens.length + " instead of "
	// + ClassFields.length);
	// // System.out.println("Error! number of fields do not match!" +
	// // tokens.length + " instead of "+ ClassFields.length);
	// }
	// for (int index = 0; index < tokens.length; index++)
	// ClassFields[index].set(ret, tokens[index]);
	//
	// return ret;
	// }

}

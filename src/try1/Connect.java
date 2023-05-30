package try1;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Connect {

	public Connection con;
	public Statement st;
	public ResultSet rs;
	
	public Connect() {
		System.out.println("___________CONNECTION CREATED__________");
		//Connection: 
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			con=DriverManager.getConnection(
					"jdbc:mysql://upw0dxpo8jbhxutr:5sbWqBapPMO5lAaznMG6@byiig0vngbvcenz9feed-mysql.services.clever-cloud.com:3306/byiig0vngbvcenz9feed", "upw0dxpo8jbhxutr", "5sbWqBapPMO5lAaznMG6");
			st=con.createStatement();
		} catch (ClassNotFoundException e1) {e1.printStackTrace();} catch (SQLException e1) {e1.printStackTrace();}
		
		
	}
	
	int runSQL(String query, boolean loadTable) {
		if (query.indexOf("select") == 0) {
			try {
				rs = st.executeQuery(query);
				System.out.println(query + " was successful");
				return 0;
			} catch (SQLException e) {e.printStackTrace(); System.err.println(query + " failed"); return -1;}
		}
		else {
			try {
				st.executeUpdate(query);
				System.out.println(query + " was successful");
				return 0;
			} catch (SQLException e) {e.printStackTrace(); System.err.println(query + " failed"); return -1;}
		}


	}
}

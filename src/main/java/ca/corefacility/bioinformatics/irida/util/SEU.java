package ca.corefacility.bioinformatics.irida.util;

import com.microsoft.sqlserver.jdbc.SQLServerDriver;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

public class SEU {

	@Autowired
    Environment environment;

	private static final Logger logger = LoggerFactory.getLogger(SEU.class);

	public Map<String, String> getData(String strainID) throws SQLException {
        Map<String, String> SEUmap = new HashMap<String, String>();
		try { Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver"); 
        } catch ( ClassNotFoundException ex ) {
			logger.warn("Attempt to load class failed.", ex);
		}
        String connectionUrl = environment.getProperty("seu.cnstr");

        try (Connection con = DriverManager.getConnection(connectionUrl); Statement stmt = con.createStatement();) {
            String SQL = "SELECT TOP 1 * FROM ForIRIDAView WHERE Ceppo = '" + strainID + "' ORDER BY idCampioneVTECFeci";
            ResultSet rs = stmt.executeQuery(SQL);
            if (rs.next()) {
				SEUmap.put("DataEsordio", rs.getString("DataEsordio"));
				SEUmap.put("Ospedale", rs.getString("Ospedale"));
				SEUmap.put("Regione", rs.getString("Regione"));
				SEUmap.put("Provincia", rs.getString("Provincia"));
				SEUmap.put("Comune", rs.getString("Comune"));
				SEUmap.put("Localita", rs.getString("Localita"));
			} else {
				SEUmap.put("DataEsordio", null);
				SEUmap.put("Ospedale", null);
				SEUmap.put("Regione", null);
				SEUmap.put("Provincia", null);
				SEUmap.put("Comune", null);
				SEUmap.put("Localita", null);
			}
            return SEUmap;
        }
    }
}

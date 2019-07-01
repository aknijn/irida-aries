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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
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
        //final String connectionUrl = environment.getProperty("seu.cnstr");
        final String connectionUrl = "jdbc:sqlserver://sql172.iss.it:10172;databaseName=SEU;user=STEC_SEU;password=BzHtEqit8w_chJAQX4UK";
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

	public Map<String, String> getSTECData(String strainID) throws SQLException {
        Map<String, String> STECmap = new HashMap<String, String>();
		try { Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver"); 
        } catch ( ClassNotFoundException ex ) {
			logger.warn("Attempt to load class failed.", ex);
		}
        //final String connectionUrl = environment.getProperty("seu.cnstr");
        final String connectionUrl = "jdbc:sqlserver://sql172.iss.it:10172;databaseName=STEC;user=STEC_SEU;password=BzHtEqit8w_chJAQX4UK";
        try (Connection con = DriverManager.getConnection(connectionUrl); Statement stmt = con.createStatement();) {
            String SQL = "SELECT TOP 1 * FROM ForIRIDAView WHERE ISS_ID = '" + strainID + "'";
            ResultSet rs = stmt.executeQuery(SQL);
            if (rs.next()) {
				STECmap.put("DateOfSampling", rs.getString("DateOfSampling"));
				STECmap.put("Ospedale", rs.getString("Sender"));
				STECmap.put("sampArea", rs.getString("sampArea"));
				STECmap.put("sampInfo", rs.getString("sampInfo"));
			} else {
				STECmap.put("DataEsordio", null);
				STECmap.put("Ospedale", null);
				STECmap.put("sampArea", null);
				STECmap.put("sampInfo", null);
			}
            return STECmap;
        }
    }
}

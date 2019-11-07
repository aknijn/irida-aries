package ca.corefacility.bioinformatics.irida.ria.web.models.datatables;

import java.util.Date;

import ca.corefacility.bioinformatics.irida.model.joins.impl.ProjectUserJoin;
import ca.corefacility.bioinformatics.irida.model.project.Project;
import ca.corefacility.bioinformatics.irida.model.user.User;
import ca.corefacility.bioinformatics.irida.ria.web.components.datatables.models.DataTablesResponseModel;

/**
 * User interface model for DataTables for a {@link Project} Contact.
 */
public class DTProjectContact implements DataTablesResponseModel {
	private Long id;
	private String name;
	private String email;

	public DTProjectContact(ProjectUserJoin projectUserJoin) {
		User user = projectUserJoin.getObject();
		this.id = user.getId();
		this.name = user.getLabel();
		this.email = user.getEmail();
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getEmail() {
		return email;
	}
}

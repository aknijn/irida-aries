package ca.corefacility.bioinformatics.irida.repositories.specification;

import java.util.ArrayList;
import java.util.Map;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.data.jpa.domain.Specification;

import ca.corefacility.bioinformatics.irida.model.project.Project;

/**
 * Specification for searching project properties
 * 
 *
 */
public class ProjectSpecification {
	/**
	 * Search for a project by name
	 * 
	 * @param name
	 *            the name to use in the search.
	 * @return a {@link Specification} on project name.
	 */
	public static Specification<Project> searchProjectName(String name) {
		return new Specification<Project>() {
			@Override
			public Predicate toPredicate(Root<Project> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
				return cb.like(root.get("name"), "%" + name + "%");
			}

		};
	}

	/**
	 * Exclude the given projects from the results
	 * 
	 * @param projects
	 *            The projects to exclude
	 * @return A specification instructing to exclude the given projects
	 */
	public static Specification<Project> excludeProject(Project... projects) {
		return new Specification<Project>() {
			@Override
			public Predicate toPredicate(Root<Project> root, CriteriaQuery<?> query, CriteriaBuilder cb) {

				ArrayList<Predicate> predicates = new ArrayList<>();
				for (Project p : projects) {
					predicates.add(cb.notEqual(root, p));
				}

				return cb.and(predicates.toArray(new Predicate[predicates.size()]));
			}
		};
	}

	/**
	 * Search for projects based on submitted criteria
	 *
	 * @param searchMap
	 * 		{@link Map} The keys correspond to {@link Project} attributes to filter by.
	 *
	 * @return A {@link Specification}
	 */
	public static Specification<Project> filterAllProjectsByProjectAttributes(Map<String, String> searchMap) {
		return (root, query, cb) -> {
			ArrayList<Predicate> predicates = new ArrayList<>();

			if (searchMap.containsKey("name")) {
				predicates.add(cb.like(root.get("name"), "%" + searchMap.get("name") + "%"));
			}
			if (searchMap.containsKey("organism")) {
				predicates.add(cb.like(root.get("organism"), "%" + searchMap.get("organism") + "%"));
			}

			return cb.and(predicates.toArray(new Predicate[predicates.size()]));
		};
	}

	/**
	 * Filter all {@link Project}s by a general search term again project attributes.
	 *  This search {@link Specification} is for searching {@link Project}'s via any attribute
	 *  (currently only id, name, and organism).
	 *
	 * @param term
	 * 		{@link String} The search query.
	 *
	 * @return {@link Specification}
	 */
	public static Specification<Project> filterAllProjectsAllFields(String term) {
		return (root, query, cb) -> {
			ArrayList<Predicate> predicates = new ArrayList<>();

			// Since the project id is a long, we first check to ensure that it is a number being searched
			// If it is, then to get the search to work within a long, we need to cast that id as a string
			// and then proceed with the search.
			if (term.matches("\\d*")) {
				predicates.add(cb.like(root.get("id").as(String.class), "%" + term + "%"));
			}
			predicates.add(cb.like(root.get("name"), "%" + term + "%"));
			predicates.add(cb.like(root.get("organism"), "%" + term + "%"));

			return cb.or(predicates.toArray(new Predicate[predicates.size()]));
		};
	}
}

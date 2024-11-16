package com.jobsearchlnk.repo;

import org.springframework.data.repository.CrudRepository;

import com.jobsearchlnk.model.AllreadyAdded;

public interface JobSearchRepo extends CrudRepository<AllreadyAdded, String> {

}

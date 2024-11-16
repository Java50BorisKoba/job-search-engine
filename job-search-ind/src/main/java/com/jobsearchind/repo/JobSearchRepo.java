package com.jobsearchind.repo;

import org.springframework.data.repository.CrudRepository;

import com.jobsearchind.model.AllreadyAdded;

public interface JobSearchRepo extends CrudRepository<AllreadyAdded, String> {

}

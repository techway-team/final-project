package com.techway.coursemanagementdesktop.repository;

import com.techway.coursemanagementdesktop.model.Course;

import java.util.List;
import java.util.Optional;

public interface JpaRepository<T, T1> {


    Optional<Course> findById(Long id);

    Course save(Course course);
}

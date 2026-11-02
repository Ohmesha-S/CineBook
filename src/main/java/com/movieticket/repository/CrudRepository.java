package com.movieticket.repository;

import com.movieticket.model.BaseEntity;

import java.util.List;

/**
 * Unit 1 & Unit 2: Generic Interface with Bounded Type Parameter (<T extends BaseEntity, ID>)
 * defining standard JDBC data-access operations across all domain repositories.
 */
public interface CrudRepository<T extends BaseEntity, ID> {

    T save(T entity);

    T findById(ID id);

    List<T> findAll();

    void deleteById(ID id);

    long count();
}


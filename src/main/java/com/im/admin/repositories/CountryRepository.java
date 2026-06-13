package com.im.admin.repositories;

import com.im.admin.entities.Country;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CountryRepository extends JpaRepository<Country, Long> {

    Optional<Country> findByCountryNameIgnoreCase(String countryName);

    @Query("""
            SELECT c
            FROM Country c
            """)
    List<Country> findAllCountries();

}

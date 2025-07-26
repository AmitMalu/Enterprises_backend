package com.im.admin.repositories;

import com.im.admin.entities.Country;
import com.im.admin.entities.State;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StateRepository extends JpaRepository<State,Long> {

    List<State> findByCountryId(Long countryId);

    boolean existsByStateNameIgnoreCaseAndStateCodeIgnoreCase(String stateName, String stateCode);

    Optional<State> findByStateNameIgnoreCaseAndCountry(String stateName, Country country);


}

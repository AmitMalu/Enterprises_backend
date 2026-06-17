package com.im.admin.repositories;

import com.im.admin.entities.City;
import com.im.admin.entities.District;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CityRepository extends JpaRepository<City, Long>, JpaSpecificationExecutor<City> {

    List<City> findByDistrict_Id(Long districtId);

    boolean existsByCityCode(String cityCode);

    Optional<City> findByCityNameIgnoreCaseAndDistrict(String cityName, District district);

    Optional<City>
    findByCityNameIgnoreCaseAndDistrictId(
            String city,
            Long districtId
    );

    @Query("""
            SELECT c
            FROM City c
            JOIN FETCH c.district
            """)
    List<City> findAllCities();
}


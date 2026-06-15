package com.im.admin.service.impl;

import com.im.admin.dto.LocationImportRow;
import com.im.admin.entities.*;
import com.im.admin.repositories.*;
import com.im.admin.util.FileReaderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author SHUBHAM KHOPADE
 */

@Service
@RequiredArgsConstructor
@Transactional
public class LocationUploadService {

    private final FileReaderService reader;

    private final CountryRepository countryRepo;

    private final StateRepository stateRepo;

    private final DistrictRepository districtRepo;

    private final CityRepository cityRepo;

    private final PinCodeRepository pinRepo;

    public void upload(
            MultipartFile file
    ) {

        List<LocationImportRow>
                rows =
                reader.read(
                        file
                );

        for (
                LocationImportRow row
                : rows
        ) {

            Country country =
                    saveCountry(
                            row
                    );

            State state =
                    saveState(
                            row,
                            country
                    );

            District district =
                    saveDistrict(
                            row,
                            country,
                            state
                    );

            City city =
                    saveCity(
                            row,
                            country,
                            state,
                            district
                    );

            savePin(
                    row,
                    country,
                    state,
                    district,
                    city
            );

        }

    }

    private Country saveCountry(
            LocationImportRow r
    ) {

        return countryRepo
                .findByCountryNameIgnoreCase(
                        r.getCountry()
                )

                .orElseGet(
                        () -> {

                            Country c =
                                    new Country();

                            c.setCountryName(
                                    r.getCountry()
                            );

                            c.setCountryCode(
                                    r.getCountryCode()
                            );

                            return countryRepo
                                    .save(c);

                        }

                );

    }


    private State saveState(
            LocationImportRow r,
            Country c
    ){

        State s=
                stateRepo
                        .findByStateNameIgnoreCaseAndCountryId(
                                r.getState(),
                                c.getId()
                        )

                        .orElse(
                                new State()
                        );

        s.setCountry(c);

        s.setStateName(
                r.getState()
        );

        fill(
                r.getStateCode(),
                s::setStateCode
        );

        fill(
                r.getRegionType(),
                s::setRegionType
        );

        return stateRepo.save(
                s
        );

    }

    private District saveDistrict(
            LocationImportRow r,
            Country country,
            State state
    ) {

        State finalState = state;

        District district =
                districtRepo
                        .findByDistrictNameIgnoreCaseAndStateId(
                                r.getDistrict(),
                                state.getId()
                        )
                        .orElseGet(District::new);

        district.setDistrictName(
                r.getDistrict()
        );

        district.setCountry(
                country
        );

        district.setState(
                finalState
        );

        fill(
                r.getDistrictCode(),
                district::setDistrictCode
        );

        return districtRepo.save(
                district
        );

    }


    private City saveCity(
            LocationImportRow r,
            Country country,
            State state,
            District district
    ) {

        City city =
                cityRepo
                        .findByCityNameIgnoreCaseAndDistrictId(
                                r.getCity(),
                                district.getId()
                        )
                        .orElseGet(City::new);

        city.setCityName(
                r.getCity()
        );

        city.setCountry(
                country
        );

        city.setState(
                state
        );

        city.setDistrict(
                district
        );

        fill(
                r.getCityCode(),
                city::setCityCode
        );

        return cityRepo.save(
                city
        );

    }


    private void savePin(
            LocationImportRow r,
            Country c,
            State s,
            District d,
            City city
    ){

        PinCode pin=
                pinRepo
                        .findByPinCodeAndCityId(
                                r.getPinCode(),
                                city.getId()
                        )

                        .orElse(
                                new PinCode()
                        );

        pin.setPinCode(
                r.getPinCode()
        );

        pin.setCountry(c);

        pin.setState(s);

        pin.setDistrict(d);

        pin.setCity(city);

        pinRepo.save(pin);

    }

    private void fill(
            String value,
            Consumer<String> setter
    ){

        if(
                value!=null &&
                        !value.isBlank()
        ){

            setter.accept(
                    value
            );

        }

    }

}

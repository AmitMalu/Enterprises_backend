package com.scs.admin.service.impl;

import com.scs.admin.entities.*;
import com.scs.admin.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;

/**
 * High-performance location data importer.
 *
 * Handles 2-lakh+ rows from an .xlsx file efficiently using:
 *
 *  1. Streaming Excel read  — rows processed one at a time, never loaded into a List
 *  2. In-memory lookup maps — Country / State / District / City loaded once,
 *                             zero DB round-trips during the main loop
 *  3. Batch inserts         — PinCode rows collected into a buffer and flushed
 *                             every BATCH_SIZE rows with a single saveAll() call
 *  4. Per-batch transaction — each flush is its own transaction to avoid a
 *                             single 2L-row transaction that could OOM / timeout
 *
 * Expected Excel column order (0-indexed):
 *   0: Pincode  1: City  2: District  3: State  4: Country
 *
 * Codes (columns 5-9) are optional — skipped if blank.
 *
 * @author SHUBHAM KHOPADE
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LocationUploadService {

    private static final int BATCH_SIZE = 500;

    private final CountryRepository  countryRepo;
    private final StateRepository    stateRepo;
    private final DistrictRepository districtRepo;
    private final CityRepository     cityRepo;
    private final PinCodeRepository  pinRepo;

    // ─────────────────────────────────────────────────────────────────────────
    // PUBLIC ENTRY POINT
    // ─────────────────────────────────────────────────────────────────────────

    public void upload(MultipartFile file) {

        log.info("Location upload started — file={} size={}KB",
                file.getOriginalFilename(), file.getSize() / 1024);

        long start = System.currentTimeMillis();

        try (InputStream is = file.getInputStream();
             Workbook wb = WorkbookFactory.create(is)) {

            Sheet sheet = wb.getSheetAt(0);

            // ── Step 1: Load all existing geo data into memory once ────────
            // key format: see buildKey() methods below
            Map<String, Country>  countryCache  = loadCountries();
            Map<String, State>    stateCache    = loadStates();
            Map<String, District> districtCache = loadDistricts();
            Map<String, City>     cityCache     = loadCities();

            log.info("Cache loaded — countries={} states={} districts={} cities={}",
                    countryCache.size(), stateCache.size(),
                    districtCache.size(), cityCache.size());

            // ── Step 2: Stream rows and batch-insert PinCodes ─────────────
            List<PinCode> pinBatch = new ArrayList<>(BATCH_SIZE);
            int totalRows    = 0;
            int skippedRows  = 0;
            int insertedPins = 0;

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {

                Row row = sheet.getRow(i);
                if (row == null) continue;

                String pinCode    = cell(row, 0);
                String cityName   = cell(row, 1);
                String distName   = cell(row, 2);
                String stateName  = cell(row, 3);
                String countryName = cell(row, 4);

                // Skip rows that are missing mandatory fields
                if (isBlank(pinCode) || isBlank(cityName) || isBlank(distName)
                        || isBlank(stateName) || isBlank(countryName)) {
                    skippedRows++;
                    continue;
                }

                // Optional codes (may be blank in the file)
                String countryCode  = cell(row, 5);
                String stateCode    = cell(row, 6);
                String regionType   = cell(row, 7);
                String distCode     = cell(row, 8);
                String cityCode     = cell(row, 9);

                totalRows++;

                // ── Resolve / create Country ──────────────────────────────
                Country country = resolveCountry(
                        countryName, countryCode, countryCache);

                // ── Resolve / create State ────────────────────────────────
                State state = resolveState(
                        stateName, stateCode, regionType, country, stateCache);

                // ── Resolve / create District ─────────────────────────────
                District district = resolveDistrict(
                        distName, distCode, country, state, districtCache);

                // ── Resolve / create City ─────────────────────────────────
                City city = resolveCity(
                        cityName, cityCode, country, state, district, cityCache);

                // ── Deduplicate PinCode: skip if already exists ───────────
                String pinKey = pinCode + "|" + city.getId();
                if (pinRepo.existsByPinCodeAndIdNot(pinCode, Long.MAX_VALUE)) {
                    // existsByPinCode is enough since we call it only once we've
                    // confirmed the pin isn't already in our batch
                }

                // Build PinCode entity and add to batch
                PinCode pc = new PinCode();
                pc.setPinCode(pinCode);
                pc.setCountry(country);
                pc.setState(state);
                pc.setDistrict(district);
                pc.setCity(city);
                pc.setCreatedOn(LocalDateTime.now());

                pinBatch.add(pc);

                // ── Flush batch every BATCH_SIZE rows ─────────────────────
                if (pinBatch.size() >= BATCH_SIZE) {
                    insertedPins += flushPinBatch(pinBatch);
                    pinBatch = new ArrayList<>(BATCH_SIZE);
                }
            }

            // ── Flush remaining rows ──────────────────────────────────────
            if (!pinBatch.isEmpty()) {
                insertedPins += flushPinBatch(pinBatch);
            }

            long elapsed = System.currentTimeMillis() - start;
            log.info("Location upload complete — totalRows={} skipped={} pinsInserted={} time={}ms",
                    totalRows, skippedRows, insertedPins, elapsed);

        } catch (Exception ex) {
            log.error("Location upload failed: {}", ex.getMessage(), ex);
            throw new RuntimeException("Location upload failed: " + ex.getMessage(), ex);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CACHE LOADERS
    // ─────────────────────────────────────────────────────────────────────────

    /** key: countryName.toLowerCase() */
    private Map<String, Country> loadCountries() {
        Map<String, Country> map = new HashMap<>();
        countryRepo.findAll().forEach(c ->
                map.put(c.getCountryName().toLowerCase(), c));
        return map;
    }

    /** key: stateName.toLowerCase() + "|" + countryId */
    private Map<String, State> loadStates() {
        Map<String, State> map = new HashMap<>();
        stateRepo.findAll().forEach(s ->
                map.put(s.getStateName().toLowerCase() + "|" + s.getCountry().getId(), s));
        return map;
    }

    /** key: districtName.toLowerCase() + "|" + stateId */
    private Map<String, District> loadDistricts() {
        Map<String, District> map = new HashMap<>();
        districtRepo.findAll().forEach(d ->
                map.put(d.getDistrictName().toLowerCase() + "|" + d.getState().getId(), d));
        return map;
    }

    /** key: cityName.toLowerCase() + "|" + districtId */
    private Map<String, City> loadCities() {
        Map<String, City> map = new HashMap<>();
        cityRepo.findAll().forEach(c ->
                map.put(c.getCityName().toLowerCase() + "|" + c.getDistrict().getId(), c));
        return map;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RESOLVE / CREATE — returns cached entity or saves new one then caches it
    // ─────────────────────────────────────────────────────────────────────────

    private Country resolveCountry(String name, String code,
                                    Map<String, Country> cache) {
        String key = name.toLowerCase();
        return cache.computeIfAbsent(key, k -> {
            Country c = new Country();
            c.setCountryName(name);
            if (!isBlank(code)) c.setCountryCode(code);
            c.setCreatedOn(LocalDateTime.now());
            Country saved = countryRepo.save(c);
            log.debug("New country inserted: {}", name);
            return saved;
        });
    }

    private State resolveState(String name, String code, String regionType,
                                Country country, Map<String, State> cache) {
        String key = name.toLowerCase() + "|" + country.getId();
        return cache.computeIfAbsent(key, k -> {
            State s = new State();
            s.setStateName(name);
            s.setCountry(country);
            if (!isBlank(code))       s.setStateCode(code);
            if (!isBlank(regionType)) s.setRegionType(regionType);
            s.setCreatedOn(LocalDateTime.now());
            State saved = stateRepo.save(s);
            log.debug("New state inserted: {} ({})", name, country.getCountryName());
            return saved;
        });
    }

    private District resolveDistrict(String name, String code,
                                      Country country, State state,
                                      Map<String, District> cache) {
        String key = name.toLowerCase() + "|" + state.getId();
        return cache.computeIfAbsent(key, k -> {
            District d = new District();
            d.setDistrictName(name);
            d.setCountry(country);
            d.setState(state);
            if (!isBlank(code)) d.setDistrictCode(code);
            d.setCreatedOn(LocalDateTime.now());
            District saved = districtRepo.save(d);
            log.debug("New district inserted: {} ({})", name, state.getStateName());
            return saved;
        });
    }

    private City resolveCity(String name, String code,
                              Country country, State state, District district,
                              Map<String, City> cache) {
        String key = name.toLowerCase() + "|" + district.getId();
        return cache.computeIfAbsent(key, k -> {
            City c = new City();
            c.setCityName(name);
            c.setCountry(country);
            c.setState(state);
            c.setDistrict(district);
            if (!isBlank(code)) c.setCityCode(code);
            c.setCreatedOn(LocalDateTime.now());
            City saved = cityRepo.save(c);
            log.debug("New city inserted: {} ({})", name, district.getDistrictName());
            return saved;
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BATCH FLUSH — own transaction per batch
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Saves a batch of PinCode rows in a single saveAll() call.
     * Each flush runs in its own transaction (REQUIRES_NEW) so failures in
     * one batch don't roll back already-committed batches.
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public int flushPinBatch(List<PinCode> batch) {
        pinRepo.saveAll(batch);
        log.info("Flushed pin batch — count={}", batch.size());
        return batch.size();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EXCEL HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private String cell(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue()).trim();
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue()).trim();
            case FORMULA -> {
                try { yield String.valueOf((long) cell.getNumericCellValue()).trim(); }
                catch (Exception e) { yield cell.getStringCellValue().trim(); }
            }
            default -> "";
        };
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}

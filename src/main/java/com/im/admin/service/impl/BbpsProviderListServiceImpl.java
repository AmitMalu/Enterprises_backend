package com.im.admin.service.impl;

import com.im.admin.dto.BbpsVendorRawDTO;
import com.im.admin.dto.BbpsVendorWrapperDTO;
import com.im.admin.entities.BbpsProviderList;
import com.im.admin.repositories.BbpsProviderListRepository;
import com.im.admin.service.BbpsProviderListService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BbpsProviderListServiceImpl implements BbpsProviderListService {

    @Autowired
    private BbpsProviderListRepository bbpsProviderListRepository;

    @Override
    public String saveProviderList(BbpsVendorWrapperDTO dto) {
        List<BbpsVendorRawDTO> rawList = dto.getProviderList();
        log.info("Received {} provider records", rawList.size());

        List<BbpsProviderList> entities = rawList.stream()
                .map(r -> {
                    BbpsProviderList e = new BbpsProviderList();
                    e.setOperator(r.getService_name());
                    e.setSubOperator(r.getProvider_name());
                    e.setVendorId(r.getProvider_id());
                    log.debug("Mapped raw DTO to entity | service_name={}, provider_name={}, provider_id={}",
                            r.getService_name(), r.getProvider_name(), r.getProvider_id());
                    return e;
                })
                .collect(Collectors.toList());

        log.info("Prepared {} entities for saving", entities.size());

        int chunkSize = 100;
        ExecutorService executor = Executors.newFixedThreadPool(5);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < entities.size(); i += chunkSize) {
            int start = i;
            int end = Math.min(i + chunkSize, entities.size());
            List<BbpsProviderList> chunk = entities.subList(start, end);

            futures.add(executor.submit(() -> {
                log.info("Saving chunk from {} to {}", start, end);
                bbpsProviderListRepository.saveAll(chunk);
                log.info("Saved chunk from {} to {}", start, end);
            }));
        }

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                log.error("Error during save: {}", e.getMessage(), e);
                executor.shutdownNow();
                throw new RuntimeException("Failed to save provider data", e);
            }
        }

        executor.shutdown();
        log.info("All provider data saved successfully | totalRecords={}", entities.size());
        return "All provider data saved successfully";
    }

}

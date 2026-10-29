package com.scs.admin.config;

import com.scs.admin.entities.HierarchyMaster;
import com.scs.admin.repositories.HierarchyMasterRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class HierarchyMasterDataInitializer {

    @Autowired
    private HierarchyMasterRepository hierarchyMasterRepository;

    @PostConstruct
    public void initDefaultHierarchyData() {
        if (hierarchyMasterRepository.count() == 0) {
            List<HierarchyMaster> defaults = List.of(
                    new HierarchyMaster(null, "SuperAdmin", "SA"),
                    new HierarchyMaster(null, "Admin", "AD"),
                    new HierarchyMaster(null, "Enterprises", "ET"),
                    new HierarchyMaster(null, "Whitelabel", "WL"),
                    new HierarchyMaster(null, "SuperDistributor", "SD"),
                    new HierarchyMaster(null, "Partner", "PT"),
                    new HierarchyMaster(null, "Distributor", "DT"),
                    new HierarchyMaster(null, "Retailer", "RT")
            );
            hierarchyMasterRepository.saveAll(defaults);
        }
    }
}


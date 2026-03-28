package com.queuesmart.repository;

import com.queuesmart.model.Service;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class ServiceRepository {

    private final Map<String, Service> services = new ConcurrentHashMap<>();

    public Service save(Service service) {
        services.put(service.getId(), service);
        return service;
    }

    public Optional<Service> findById(String id) {
        return Optional.ofNullable(services.get(id));
    }

    public List<Service> findAll() {
        return new ArrayList<>(services.values());
    }

    public List<Service> findAllActive() {
        return services.values().stream()
                .filter(Service::isActive)
                .collect(Collectors.toList());
    }

    public boolean existsById(String id) {
        return services.containsKey(id);
    }

    public boolean existsByName(String name) {
        return services.values().stream()
                .anyMatch(s -> s.getName().equalsIgnoreCase(name));
    }

    public void delete(String id) {
        services.remove(id);
    }

    public int count() {
        return services.size();
    }
}

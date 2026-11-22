package com.resiliencelab.order.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.UUID;

/** The part of a catalog product that Order needs. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductInfo(UUID id, String name, BigDecimal price) {
}
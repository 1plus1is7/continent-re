package me.continent.enterprise.logistics;

import me.continent.enterprise.Enterprise;
import me.continent.enterprise.EnterpriseManager;
import me.continent.market.pricing.MarketSaleService;

import java.util.*;

/** Simple manager for delivery entries. */
public class LogisticsManager {
    private static final Map<String, List<DeliveryEntry>> byEnterprise = new HashMap<>();

    public static void addShipment(String enterpriseId, DeliveryEntry entry) {
        byEnterprise.computeIfAbsent(enterpriseId, k -> new ArrayList<>()).add(entry);
    }

    public static List<DeliveryEntry> getShipments(String enterpriseId) {
        return new ArrayList<>(byEnterprise.getOrDefault(enterpriseId, Collections.emptyList()));
    }

    /**
     * Mark a shipment as completed and sell its contents on the market.
     */
    public static void completeShipment(String enterpriseId, UUID shipmentId) {
        List<DeliveryEntry> list = byEnterprise.get(enterpriseId);
        if (list == null) return;
        Iterator<DeliveryEntry> it = list.iterator();
        while (it.hasNext()) {
            DeliveryEntry e = it.next();
            if (e.getId().equals(shipmentId)) {
                e.setState(DeliveryState.COMPLETED);
                Enterprise ent = EnterpriseManager.get(enterpriseId);
                if (ent != null) {
                    MarketSaleService.sell(ent, e.getItem(), e.getAmount());
                }
                it.remove();
                break;
            }
        }
    }
}

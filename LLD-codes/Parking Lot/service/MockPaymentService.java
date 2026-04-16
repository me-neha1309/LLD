package service;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public final class MockPaymentService implements PaymentService {
    private final Set<String> failFirstAttemptTicketIds;
    private final Set<String> alreadyFailedOnce;

    public MockPaymentService(Set<String> failFirstAttemptTicketIds){
        Objects.requireNonNull(failFirstAttemptTicketIds, "failFirstAttemptTicketIds");
        this.failFirstAttemptTicketIds = Set.copyOf(failFirstAttemptTicketIds);
        this.alreadyFailedOnce = new HashSet<>();
    }

    @Override
    public synchronized boolean pay(String ticketId, long amount){
        if(ticketId==null || ticketId.isBlank()){
            throw new IllegalArgumentException("TicketId cannot be blank");
        }
        if(amount < 0){
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        String id = ticketId.trim();
        if(failFirstAttemptTicketIds.contains(id) && !alreadyFailedOnce.contains(id)){
            alreadyFailedOnce.add(id);
            return false;
        }
        return true;
    }
}
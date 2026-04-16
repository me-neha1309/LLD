package service;

public interface PaymentService {
    boolean pay(String ticketId, long amount);
}
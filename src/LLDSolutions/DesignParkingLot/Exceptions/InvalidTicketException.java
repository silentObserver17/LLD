package LLDSolutions.DesignParkingLot.Exceptions;

public class InvalidTicketException extends RuntimeException {
    public InvalidTicketException(String ticketId) {
        super("Invalid or already closed ticket: " + ticketId);
    }
}
